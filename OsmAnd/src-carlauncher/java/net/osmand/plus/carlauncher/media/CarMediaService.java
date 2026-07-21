package net.osmand.plus.carlauncher.media;

import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import net.osmand.plus.carlauncher.music.MusicManager;
import net.osmand.plus.carlauncher.music.InternalMusicPlayer;

/**
 * Standard MediaBrowserService that bridges Android Auto / Steering wheel media controls
 * with OsmAnd CarLauncher internal music playback (MusicManager / InternalMusicPlayer).
 */
public class CarMediaService extends MediaBrowserService {

    private MediaSession mediaSession;
    private MusicManager musicManager;

    @Override
    public void onCreate() {
        super.onCreate();
        musicManager = MusicManager.getInstance(getApplicationContext());

        mediaSession = new MediaSession(this, "CarMediaService");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
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
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowser.MediaItem>> result) {
        result.sendResult(Collections.emptyList());
    }

    private boolean isInternalPlayerActive() {
        if (musicManager == null) return false;
        net.osmand.plus.carlauncher.music.BaseMediaAdapter activeAdapter = musicManager.getActiveAdapter();
        return activeAdapter instanceof net.osmand.plus.carlauncher.music.InternalPlayerAdapter;
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            if (!isInternalPlayerActive()) return;

            InternalMusicPlayer player = musicManager.getInternalPlayer();
            if (player != null) {
                player.play();
            }
            if (mediaSession != null) {
                mediaSession.setPlaybackState(new PlaybackState.Builder()
                        .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                        .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                        .build());
            }
        }

        @Override
        public void onPause() {
            if (!isInternalPlayerActive()) return;

            InternalMusicPlayer player = musicManager.getInternalPlayer();
            if (player != null) {
                player.pause();
            }
            if (mediaSession != null) {
                mediaSession.setPlaybackState(new PlaybackState.Builder()
                        .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                        .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0.0f)
                        .build());
            }
        }

        @Override
        public void onSkipToNext() {
            if (!isInternalPlayerActive()) return;

            InternalMusicPlayer player = musicManager.getInternalPlayer();
            if (player != null) {
                player.next();
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (!isInternalPlayerActive()) return;

            InternalMusicPlayer player = musicManager.getInternalPlayer();
            if (player != null) {
                player.previous();
            }
        }

        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            if (!isInternalPlayerActive()) return false;
            return super.onMediaButtonEvent(mediaButtonIntent);
        }
    }
}
