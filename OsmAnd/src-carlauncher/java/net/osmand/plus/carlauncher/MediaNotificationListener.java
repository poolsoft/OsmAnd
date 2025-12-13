package net.osmand.plus.carlauncher;

import android.service.notification.NotificationListenerService;

/**
 * Bu servis, sistemdeki medya session'larina erismek icin gereklidir.
 * Notification listener izni verilmelidir.
 */
public class MediaNotificationListener extends NotificationListenerService {
    // Sadece baglanti ve erisim icin bos bir servis yeterlidir.
    // MediaSessionManager.getActiveSessions() bu servisin komponenti uzerinden calisir.
}
