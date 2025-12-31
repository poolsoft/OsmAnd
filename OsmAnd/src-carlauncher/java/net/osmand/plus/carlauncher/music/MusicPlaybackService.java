package net.osmand.plus.carlauncher.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class MusicPlaybackService extends Service {

    public static final String CHANNEL_ID = "car_launcher_music_channel";
    public static final int NOTIFICATION_ID = 888;

    public static final String ACTION_PLAY_PAUSE = "action_play_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREV = "action_prev";
    public static final String ACTION_CLOSE = "action_close";
    public static final String ACTION_UPDATE = "action_update";

    private MusicManager musicManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        musicManager = MusicManager.getInstance(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_NOT_STICKY;
    }

    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY_PAUSE:
                if (musicManager != null) musicManager.togglePlayPause();
                break;
            case ACTION_NEXT:
                if (musicManager != null) musicManager.skipToNext();
                break;
            case ACTION_PREV:
                if (musicManager != null) musicManager.skipToPrevious();
                break;
            case ACTION_CLOSE:
                if (musicManager != null && musicManager.getInternalPlayer().isPlaying()) {
                    musicManager.getInternalPlayer().pause();
                }
                stopForeground(true);
                stopSelf();
                return; // Service stops here
            case ACTION_UPDATE:
                // Just refresh notification
                break;
        }
        updateNotification();
    }

    private void updateNotification() {
        if (musicManager == null) return;

        MusicRepository.AudioTrack track = musicManager.getInternalPlayer().getCurrentTrack();
        if (track == null) {
            // Nothing to show, maybe stop?
            // If playing is false and no track, stop service
            if (!musicManager.getInternalPlayer().isPlaying()) {
                 stopForeground(true);
                 stopSelf();
            }
            return;
        }

        boolean isPlaying = musicManager.getInternalPlayer().isPlaying();

        // Bitmap art = null; // Can retrieve from track uri if needed, for now use default icon
        // Ideally we pass metadata via Intent or fetch active track details.
        // Getting Art is heavy, let's keep it simple for now or fetch if possible.
        // InternalMusicPlayer track has title/artist.

        Intent playPauseIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_PLAY_PAUSE);
        PendingIntent pPlayPause = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_NEXT);
        PendingIntent pNext = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_PREV);
        PendingIntent pPrev = PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent closeIntent = new Intent(this, MusicPlaybackService.class).setAction(ACTION_CLOSE);
        PendingIntent pClose = PendingIntent.getService(this, 0, closeIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // Open App Intent (Click on content)
        // We need to know which Activity to open. Assuming MapActivity or similar?
        // Let's use getLaunchIntent for self
        Intent contentIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pContent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setSmallIcon(R.drawable.ic_music_note) // Ensure this exists or use generic
                .setLargeIcon(null) // Todo: Load Album Art
                .setContentIntent(pContent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setDeleteIntent(pClose)
                // Add Media Style
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2) // Indices of actions
                        .setMediaSession(null)) // Pass session token if integrating MediaSession
                .addAction(R.drawable.ic_action_center_widget, "Prev", pPrev) // Need icons
                .addAction(isPlaying ? R.drawable.ic_action_video_dark : R.drawable.ic_action_play_dark, "Play/Pause", pPlayPause)
                .addAction(R.drawable.ic_action_center_widget, "Next", pNext) // Reusing icons temporarily if specific ones missing
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", pClose);

        // Icon Fixes:
        // Use R.drawable for existing icons.
        // Play: R.drawable.ic_play_arrow (if exists) or android.R.drawable.
        // We used custom buttons in widget. Let's use android defaults for safety or project icons.
        // Project has: ic_play_dark, ic_pause_dark? Widget used resource IDs directly.
        // Let's rely on standard Android icons for notification actions to be safe, or use what we saw in widget xmls?
        // Widget uses `widget_btn_play` etc. which are ImageButtons.
        
        // Let's try to use standard android drawable for notification actions to ensure they appear
        builder.mActions.clear();
        builder.addAction(android.R.drawable.ic_media_previous, "Prev", pPrev);
        if (isPlaying) {
             builder.addAction(android.R.drawable.ic_media_pause, "Pause", pPlayPause);
        } else {
             builder.addAction(android.R.drawable.ic_media_play, "Play", pPlayPause);
        }
        builder.addAction(android.R.drawable.ic_media_next, "Next", pNext);
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", pClose);
        
        // Update MediaStyle indices
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2));

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW 
            );
            channel.setDescription("Shows music controls");
            channel.setSound(null, null);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
