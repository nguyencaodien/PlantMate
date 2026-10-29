package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.Message;
import com.example.ungdungchiasecaycanh.model.MessageType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseChatDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    /**
     * Opens the pair's one conversation about {@code listingId}. The first ask creates it; a later ask
     * about another plant of the same member switches the plant being discussed (listing, owner, asker).
     * Creating first and reading only when that is denied avoids reading a document that may not exist.
     */
    public Task<String> openPairConversation(String conversationId, String listingId,
                                             String ownerId, String requesterId,
                                             List<String> participantIds) {
        DocumentReference document = firestore.collection("conversations").document(conversationId);
        return document.set(conversationCreateFields(listingId, ownerId, requesterId, participantIds))
                .continueWithTask(write -> {
                    if (write.isSuccessful()) return Tasks.forResult(conversationId);
                    return document.get().continueWithTask(read -> {
                        if (!read.isSuccessful()) throw failure(write.getException());
                        DocumentSnapshot existing = read.getResult();
                        if (existing == null || !existing.exists()) throw failure(write.getException());
                        Conversation conversation = mapConversation(existing);
                        if (listingId.equals(conversation.getListingId())
                                && ownerId.equals(conversation.getOwnerId())
                                && requesterId.equals(conversation.getRequesterId())) {
                            return Tasks.forResult(conversationId);
                        }
                        Map<String, Object> switchTo = new HashMap<>();
                        switchTo.put("listingId", listingId);
                        switchTo.put("ownerId", ownerId);
                        switchTo.put("requesterId", requesterId);
                        return document.update(switchTo).continueWith(update -> {
                            if (!update.isSuccessful()) throw failure(update.getException());
                            return conversationId;
                        });
                    });
                });
    }

    /** Creates the plant-less pair chat; when the pair already has one the create is refused and it is reused. */
    public Task<String> openDirectConversation(String conversationId, String otherUid, String myUid,
                                               List<String> participantIds) {
        DocumentReference document = firestore.collection("conversations").document(conversationId);
        return document.set(conversationCreateFields("", otherUid, myUid, participantIds))
                .continueWithTask(write -> {
                    if (write.isSuccessful()) return Tasks.forResult(conversationId);
                    return document.get().continueWith(read -> {
                        if (read.isSuccessful() && read.getResult() != null && read.getResult().exists()) return conversationId;
                        throw failure(write.getException());
                    });
                });
    }

    /** A plant card in the chat; the text message that follows updates the inbox preview. */
    public Task<Void> sendListingMessage(String conversationId, String senderId, String listingId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("senderId", senderId);
        fields.put("type", MessageType.LISTING.name());
        fields.put("text", "");
        fields.put("listingId", listingId);
        fields.put("createdAt", FieldValue.serverTimestamp());
        return firestore.collection("conversations").document(conversationId)
                .collection("messages").document().set(fields);
    }

    public Task<Conversation> getConversation(String conversationId) {
        return firestore.collection("conversations").document(conversationId).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    DocumentSnapshot document = task.getResult();
                    if (document == null || !document.exists()) {
                        throw new IllegalStateException("Conversation does not exist");
                    }
                    return mapConversation(document);
                });
    }

    public Task<List<Conversation>> getInbox(String uid) {
        return firestore.collection("conversations")
                .whereArrayContains("participantIds", uid).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    List<Conversation> conversations = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        conversations.add(mapConversation(document));
                    }
                    Collections.sort(conversations, Comparator.comparing(
                            Conversation::getUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));
                    return conversations;
                });
    }

    /**
     * Live inbox for {@code uid}, newest first. Callbacks run on the main thread; a message
     * sent from this device shows at once with an estimated time.
     */
    public ListenerRegistration listenInbox(String uid, Consumer<List<Conversation>> onData,
                                            Consumer<Exception> onError) {
        return firestore.collection("conversations").whereArrayContains("participantIds", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        onError.accept(failure(error));
                        return;
                    }
                    List<Conversation> conversations = new ArrayList<>();
                    try {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            conversations.add(mapConversation(document));
                        }
                    } catch (RuntimeException invalid) {
                        onError.accept(invalid);
                        return;
                    }
                    Collections.sort(conversations, Comparator.comparing(
                            Conversation::getUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));
                    onData.accept(conversations);
                });
    }

    /** Live messages of one conversation, oldest first (same order as {@link #getMessages}). */
    public ListenerRegistration listenMessages(String conversationId, Consumer<List<Message>> onData,
                                               Consumer<Exception> onError) {
        return firestore.collection("conversations").document(conversationId)
                .collection("messages").orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        onError.accept(failure(error));
                        return;
                    }
                    List<Message> messages = new ArrayList<>();
                    try {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            messages.add(mapMessage(document));
                        }
                    } catch (RuntimeException invalid) {
                        onError.accept(invalid);
                        return;
                    }
                    onData.accept(messages);
                });
    }

    public Task<List<Message>> getMessages(String conversationId) {
        return firestore.collection("conversations").document(conversationId)
                .collection("messages").orderBy("createdAt", Query.Direction.ASCENDING)
                .get().continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    List<Message> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        messages.add(mapMessage(document));
                    }
                    return messages;
                });
    }

    public Task<Boolean> sendTextMessage(String conversationId, String senderId, String text) {
        DocumentReference conversation = firestore.collection("conversations").document(conversationId);
        DocumentReference message = conversation.collection("messages").document();
        return message.set(messageCreateFields(senderId, text))
                .continueWithTask(write -> {
                    if (!write.isSuccessful()) throw failure(write.getException());
                    return conversation.update(summaryUpdateFields(text))
                            .continueWith(summary -> summary.isSuccessful());
                });
    }

    /** Photo or voice message pointing at chatMedia; {@code summary} becomes the inbox preview. */
    public Task<Boolean> sendMediaMessage(String conversationId, String senderId, MessageType type,
                                          String caption, String mediaRef, long durationMs, String summary) {
        DocumentReference conversation = firestore.collection("conversations").document(conversationId);
        DocumentReference message = conversation.collection("messages").document();
        Map<String, Object> fields = new HashMap<>();
        fields.put("senderId", senderId);
        fields.put("type", type.name());
        fields.put("text", caption == null ? "" : caption);
        fields.put("mediaRef", mediaRef);
        fields.put("durationMs", durationMs);
        fields.put("createdAt", FieldValue.serverTimestamp());
        return message.set(fields).continueWithTask(write -> {
            if (!write.isSuccessful()) throw failure(write.getException());
            return conversation.update(summaryUpdateFields(summary)).continueWith(done -> done.isSuccessful());
        });
    }

    static Map<String, Object> conversationCreateFields(String listingId, String ownerId,
                                                         String requesterId, List<String> participantIds) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("listingId", listingId);
        fields.put("ownerId", ownerId);
        fields.put("requesterId", requesterId);
        fields.put("participantIds", participantIds);
        fields.put("lastMessage", "");
        fields.put("createdAt", FieldValue.serverTimestamp());
        fields.put("updatedAt", FieldValue.serverTimestamp());
        return fields;
    }

    static Map<String, Object> messageCreateFields(String senderId, String text) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("senderId", senderId);
        fields.put("type", MessageType.TEXT.name());
        fields.put("text", text);
        fields.put("createdAt", FieldValue.serverTimestamp());
        return fields;
    }

    private static Map<String, Object> summaryUpdateFields(String text) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("lastMessage", text);
        fields.put("updatedAt", FieldValue.serverTimestamp());
        return fields;
    }

    private static Conversation mapConversation(DocumentSnapshot document) {
        String listingId = requiredString(document, "listingId");
        String ownerId = requiredString(document, "ownerId");
        String requesterId = requiredString(document, "requesterId");
        Object rawParticipants = document.get("participantIds");
        if (!(rawParticipants instanceof List<?>)) {
            throw new IllegalStateException("Invalid conversation participants");
        }
        List<?> rawList = (List<?>) rawParticipants;
        if (rawList.size() != 2 || !(rawList.get(0) instanceof String)
                || !(rawList.get(1) instanceof String)) {
            throw new IllegalStateException("Invalid conversation participants");
        }
        List<String> participants = Arrays.asList((String) rawList.get(0), (String) rawList.get(1));
        if (ownerId.equals(requesterId) || !participants.contains(ownerId)
                || !participants.contains(requesterId)) {
            throw new IllegalStateException("Invalid conversation participants");
        }
        // ESTIMATE: a local write that has not reached the server yet still has a time.
        Timestamp createdAt = document.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        Timestamp updatedAt = document.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        if (createdAt == null || updatedAt == null) {
            throw new IllegalStateException("Invalid conversation timestamps");
        }
        return new Conversation(document.getId(), listingId, ownerId, requesterId,
                participants, requiredString(document, "lastMessage"), createdAt, updatedAt);
    }

    private static Message mapMessage(DocumentSnapshot document) {
        Timestamp createdAt = document.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        if (createdAt == null) throw new IllegalStateException("Invalid message timestamp");
        MessageType type;
        try {
            type = MessageType.valueOf(requiredString(document, "type"));
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("Unsupported message type", error);
        }
        String mediaRef = document.getString("mediaRef");
        Long duration = document.getLong("durationMs");
        String listingId = document.getString("listingId");
        return new Message(document.getId(), requiredString(document, "senderId"), type,
                requiredString(document, "text"), createdAt, mediaRef == null ? "" : mediaRef,
                duration == null ? 0 : duration, listingId == null ? "" : listingId);
    }

    private static String requiredString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        if (value == null) throw new IllegalStateException("Missing " + field);
        return value;
    }

    private static Exception failure(Exception cause) {
        return cause == null ? new IllegalStateException("Chat request failed") : cause;
    }
}
