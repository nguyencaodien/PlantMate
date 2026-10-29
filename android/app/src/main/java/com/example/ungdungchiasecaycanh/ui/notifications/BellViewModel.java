package com.example.ungdungchiasecaycanh.ui.notifications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.data.repository.NotificationRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

/**
 * Count on the Home bell: unread community notifications + friend requests waiting + 1 for today's
 * watering until the Notifications screen is opened today.
 */
public class BellViewModel extends AndroidViewModel {
    private final NotificationRepository notifications = new NotificationRepository();
    private final FriendRepository friends = new FriendRepository();
    private final SettingsRepository settings;
    private final MediatorLiveData<Integer> count = new MediatorLiveData<>();
    private final androidx.lifecycle.MutableLiveData<Integer> chats = new androidx.lifecycle.MutableLiveData<>(0);
    private final com.example.ungdungchiasecaycanh.data.repository.ChatRepository chatRepository =
            new com.example.ungdungchiasecaycanh.data.repository.ChatRepository();
    private List<PlantEntity> plants;
    private int remote;

    public BellViewModel(@NonNull Application application) {
        super(application);
        settings = new SettingsRepository(application);
        count.addSource(new GardenRepository(application).observeCurrentUserPlants(), values -> {
            plants = values;
            publish();
        });
    }

    public LiveData<Integer> getCount() { return count; }

    /** Chats with new messages (the message button; notifications leave messages out). */
    public LiveData<Integer> getChats() { return chats; }

    public void refresh() {
        Task<Integer> unread = notifications.unreadCount();
        Task<Integer> requests = friends.getIncomingCount();
        Tasks.whenAllComplete(unread, requests).addOnCompleteListener(done -> {
            remote = value(unread) + value(requests);
            publish();
        });
        long since = settings.getChatSeenSince();
        chatRepository.getConversationsForCurrentUser().addOnCompleteListener(task -> chats.setValue(
                task.isSuccessful() && task.getResult() != null
                        ? com.example.ungdungchiasecaycanh.ui.inbox.ChatUnread.count(task.getResult(), settings.getChatSeen(), since)
                        : 0));
    }

    private void publish() {
        long now = System.currentTimeMillis();
        boolean water = plants != null && settings.getNotificationsSeenDay() < NotificationsViewModel.startOfDay(now)
                && NotificationsViewModel.wateringRow(plants, now, true) != null;
        count.setValue(remote + (water ? 1 : 0));
    }

    /** Red count on an icon button ("9+" above nine) and a spoken label that includes it. */
    public static void bindBadge(android.widget.TextView badge, android.view.View button, Integer n,
                                 int label, int unreadPlural) {
        int value = n == null ? 0 : n;
        badge.setVisibility(value > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        badge.setText(value > 9 ? "9+" : String.valueOf(value));
        android.content.res.Resources r = button.getResources();
        button.setContentDescription(value > 0 ? r.getQuantityString(unreadPlural, value, value) : r.getString(label));
    }

    private static int value(Task<Integer> task) {
        return task.isSuccessful() && task.getResult() != null ? task.getResult() : 0;
    }
}
