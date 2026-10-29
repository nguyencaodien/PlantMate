package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseChatDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseChatMediaDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseImageDataSource;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.Message;
import com.example.ungdungchiasecaycanh.model.MessageType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

public class ChatRepository {
    private final FirebaseAuthDataSource authDataSource = new FirebaseAuthDataSource();
    private final FirebaseChatDataSource chatDataSource = new FirebaseChatDataSource();
    private final ListingRepository listingRepository = new ListingRepository();

    public String getCurrentUserId() { return authDataSource.getCurrentUserId(); }

    public Task<String> openOrCreateConversation(String listingId) {
        String requesterId = authDataSource.getCurrentUserId();
        if (requesterId == null) return unauthenticated();
        try {
            buildConversationId(listingId, requesterId);
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
        String id = listingId.trim();
        return listingRepository.getListing(id).continueWithTask(read -> {
            if (!read.isSuccessful()) throw failure(read.getException());
            String ownerId = read.getResult().getOwnerId();
            if (requesterId.equals(ownerId)) {
                throw new IllegalArgumentException("Cannot contact your own listing");
            }
            if (ownerId == null || ownerId.isEmpty()
                    || read.getResult().getStatus() != ListingStatus.ACTIVE) {
                throw new IllegalStateException("Listing is not open for contact");
            }
            return chatDataSource.openPairConversation(pairConversationId(ownerId, requesterId),
                    id, ownerId, requesterId, pairParticipants(ownerId, requesterId));
        });
    }

    /**
     * The chat with another member started from their profile: the pair's conversation, created without
     * a plant when it does not exist yet (an existing one keeps the plant it is about).
     */
    public Task<String> openDirectConversation(String otherUid) {
        String me = authDataSource.getCurrentUserId();
        if (me == null) return unauthenticated();
        if (otherUid == null || otherUid.trim().isEmpty() || otherUid.trim().equals(me)) {
            return Tasks.forException(new IllegalArgumentException("Choose another member"));
        }
        String other = otherUid.trim();
        return chatDataSource.openDirectConversation(pairConversationId(other, me), other, me,
                pairParticipants(other, me));
    }

    /** A plant card in the conversation, sent just before the member's question about it. */
    public Task<Void> sendListingMessage(String conversationId, String listingId) {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) return unauthenticated();
        if (conversationId == null || conversationId.trim().isEmpty()
                || listingId == null || listingId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Conversation and listing IDs are required"));
        }
        return chatDataSource.sendListingMessage(conversationId.trim(), uid, listingId.trim());
    }

    public Task<Conversation> getConversation(String conversationId) {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) return unauthenticated();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Conversation ID is required"));
        }
        return chatDataSource.getConversation(conversationId.trim()).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            Conversation conversation = task.getResult();
            if (!conversation.getParticipantIds().contains(uid)) {
                throw new IllegalStateException("Conversation access denied");
            }
            return conversation;
        });
    }

    public Task<List<Conversation>> getConversationsForCurrentUser() {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) return unauthenticated();
        return chatDataSource.getInbox(uid).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            return withMessages(task.getResult());
        });
    }

    /**
     * Conversations someone has written in. Opening a chat from a listing creates its (deterministic)
     * conversation before the first message; until a message is sent it stays out of both inboxes.
     */
    public static List<Conversation> withMessages(List<Conversation> conversations) {
        List<Conversation> started = new ArrayList<>();
        if (conversations == null) return started;
        for (Conversation c : conversations) {
            if (c != null && c.getLastMessage() != null && !c.getLastMessage().trim().isEmpty()) started.add(c);
        }
        return started;
    }

    /** A live query; {@link #cancel()} stops it. Keeps Firebase types out of the UI layer. */
    public interface Subscription {
        void cancel();
    }

    private static final Subscription NONE = () -> { };

    /** Live inbox of the signed-in member (conversations newest first). */
    public Subscription listenInbox(Consumer<List<Conversation>> onData,
                                    Consumer<Exception> onError) {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            onError.accept(new IllegalStateException("Authentication required"));
            return NONE;
        }
        return chatDataSource.listenInbox(uid, list -> onData.accept(withMessages(list)), onError)::remove;
    }

    /** Live messages of a conversation; the rules only allow participants to read them. */
    public Subscription listenMessages(String conversationId,
                                       Consumer<List<Message>> onData,
                                       Consumer<Exception> onError) {
        if (authDataSource.getCurrentUserId() == null || conversationId == null
                || conversationId.trim().isEmpty()) {
            onError.accept(new IllegalStateException("Conversation unavailable"));
            return NONE;
        }
        return chatDataSource.listenMessages(conversationId.trim(), onData, onError)::remove;
    }

    /** Raw bytes of private chat media (blocking: call off the main thread). */
    public static byte[] loadMediaBlocking(String mediaRef) throws Exception {
        return FirebaseChatMediaDataSource.loadBlocking(mediaRef);
    }

    /** Stable id of a {@code pmchat://} ref, or null when the ref is not chat media. */
    public static String mediaIdOf(String mediaRef) {
        return FirebaseChatMediaDataSource.idOf(mediaRef);
    }

    public Task<List<Message>> getMessages(String conversationId) {
        if (authDataSource.getCurrentUserId() == null) return unauthenticated();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Conversation ID is required"));
        }
        return chatDataSource.getMessages(conversationId.trim());
    }

    // true means both Message and Inbox summary succeeded; false means Message succeeded
    // but the summary needs a later refresh. A failed Task means no Message was sent.
    public Task<Boolean> sendTextMessage(String conversationId, String text) {
        String senderId = authDataSource.getCurrentUserId();
        if (senderId == null) return unauthenticated();
        String normalized;
        try {
            normalized = normalizeMessageText(text);
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Conversation ID is required"));
        }
        String id = conversationId.trim();
        return getConversation(id).continueWithTask(read -> {
            if (!read.isSuccessful()) throw failure(read.getException());
            if (!read.getResult().getParticipantIds().contains(senderId)) {
                throw new IllegalStateException("Conversation access denied");
            }
            return chatDataSource.sendTextMessage(id, senderId, normalized);
        });
    }

    private final FirebaseChatMediaDataSource mediaDataSource =
            new FirebaseChatMediaDataSource();
    private static final Executor MEDIA_WORK = Executors.newSingleThreadExecutor();

    /** Compresses a photo on a background thread, stores it privately and sends an IMAGE message. */
    public Task<Boolean> sendImageMessage(Context context, String conversationId,
                                          String localUri, String summary) {
        String senderId = authDataSource.getCurrentUserId();
        if (senderId == null) return unauthenticated();
        Context app = context.getApplicationContext();
        return Tasks.call(MEDIA_WORK, () -> Base64.decode(
                        FirebaseImageDataSource
                                .encode(app, Uri.parse(localUri)).base64, Base64.DEFAULT))
                .continueWithTask(bytes -> {
                    if (!bytes.isSuccessful()) throw bytes.getException();
                    return mediaDataSource.save(senderId, conversationId, "IMAGE", "image/jpeg", bytes.getResult(), 0);
                })
                .continueWithTask(ref -> {
                    if (!ref.isSuccessful()) throw ref.getException();
                    return chatDataSource.sendMediaMessage(conversationId, senderId,
                            MessageType.IMAGE, "", ref.getResult(), 0, summary);
                });
    }

    /** Sends a recorded AAC voice note (0.5 to 60 s). */
    public Task<Boolean> sendVoiceMessage(String conversationId, File audio, long durationMs, String summary) {
        String senderId = authDataSource.getCurrentUserId();
        if (senderId == null) return unauthenticated();
        if (durationMs < 500 || durationMs > 60_000) {
            return Tasks.forException(new IllegalArgumentException("Voice note must be 0.5 to 60 seconds"));
        }
        return Tasks.call(MEDIA_WORK, () -> readAll(audio))
                .continueWithTask(bytes -> {
                    if (!bytes.isSuccessful()) throw bytes.getException();
                    return mediaDataSource.save(senderId, conversationId, "VOICE", "audio/mp4", bytes.getResult(), durationMs);
                })
                .continueWithTask(ref -> {
                    if (!ref.isSuccessful()) throw ref.getException();
                    return chatDataSource.sendMediaMessage(conversationId, senderId,
                            MessageType.VOICE, "", ref.getResult(), durationMs, summary);
                });
    }

    private static byte[] readAll(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }

    /** The one conversation of two members: both user IDs, sorted, joined by "__". */
    public static String pairConversationId(String uidA, String uidB) {
        List<String> pair = pairParticipants(uidA, uidB);
        return pair.get(0) + "__" + pair.get(1);
    }

    /** The two members in the order the pair conversation stores them (sorted). */
    public static List<String> pairParticipants(String uidA, String uidB) {
        String a = uidA == null ? "" : uidA.trim();
        String b = uidB == null ? "" : uidB.trim();
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            throw new IllegalArgumentException("Two different member IDs are required");
        }
        return a.compareTo(b) < 0 ? java.util.Arrays.asList(a, b) : java.util.Arrays.asList(b, a);
    }

    /** Conversations created before 2026-10-14: one per listing and asker. */
    public static String buildConversationId(String listingId, String requesterId) {
        if (listingId == null || listingId.trim().isEmpty()
                || requesterId == null || requesterId.trim().isEmpty()) {
            throw new IllegalArgumentException("Listing and requester IDs are required");
        }
        return listingId.trim() + "__" + requesterId.trim();
    }

    public static String normalizeMessageText(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty() || normalized.length() > 1000) {
            throw new IllegalArgumentException("Message must be 1 to 1000 characters");
        }
        return normalized;
    }

    private static <T> Task<T> unauthenticated() {
        return Tasks.forException(new IllegalStateException("Authentication required"));
    }

    private static Exception failure(Exception cause) {
        return cause == null ? new IllegalStateException("Chat request failed") : cause;
    }
}
