package net.osmand.plus.carlauncher;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Notification listener service.
 * MediaSession API icin gerekli - aktif muzik uygulamalarini tespit eder.
 */
public class MediaNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Muzik bildirimleri geldikçe burası çağrılır
        // MediaSessionManager bizim için gerekli bilgileri toplar
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Bildirim kaldırıldığında
    }
}
