package net.osmand.plus.carlauncher.media;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.Collections;
import java.util.List;

import net.osmand.plus.carlauncher.music.MusicManager;
import net.osmand.plus.carlauncher.music.InternalMusicPlayer;

/**
 * MediaBrowserServiceCompat that bridges Android Auto / Steering wheel media controls
 * with OsmAnd CarLauncher internal music playback (MusicManager / InternalMusicPlayer).
 */
public class CarMediaService extends MediaBrowserServiceCompat {

    private MediaSessionCompat mediaSession;
    private MusicManager musicManager;

    @Override
    public void onCreate() {
        super.onCreate();
        musicManager = MusicManager.getInstance(getApplicationContext());

        mediaSession = new MediaSessionCompat(this, "CarMediaService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCallback());
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("CAR_ROOT", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(Collections.emptyList());
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            net.osmand.plus.carlauncher.music.BaseMediaAdapter adapter = musicManager != null ? musicManager.getActiveAdapter() : null;
            if (adapter != null) {
                adapter.play();
            }
            if (mediaSession != null) {
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                        .setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                        .build());
            }
        }

        @Override
        public void onPause() {
            net.osmand.plus.carlauncher.music.BaseMediaAdapter adapter = musicManager != null ? musicManager.getActiveAdapter() : null;
            if (adapter != null) {
                adapter.pause();
            }
            if (mediaSession != null) {
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                        .setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0.0f)
                        .build());
            }
        }

        @Override
        public void onSkipToNext() {
            net.osmand.plus.carlauncher.music.BaseMediaAdapter adapter = musicManager != null ? musicManager.getActiveAdapter() : null;
            if (adapter != null) {
                adapter.next();
            }
        }

        @Override
        public void onSkipToPrevious() {
            net.osmand.plus.carlauncher.music.BaseMediaAdapter adapter = musicManager != null ? musicManager.getActiveAdapter() : null;
            if (adapter != null) {
                adapter.prev();
            }
        }

        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            return MediaButtonReceiver.handleIntent(CarMediaService.this, mediaButtonIntent);
        }
    }
}
