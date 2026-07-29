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
import net.osmand.plus.carlauncher.headunit.diagnostics.HardwareEventRecorder;

/**
 * Standard MediaBrowserService that bridges Android Auto / Steering wheel media controls
 * with OsmAnd CarLauncher internal music playback (MusicManager / InternalMusicPlayer).
 */
public class CarMediaService extends MediaBrowserService implements MusicManager.MusicUIListener {

    public static final String ACTION_DIAGNOSTIC_STATE_CHANGED =
            "net.osmand.plus.carlauncher.action.DIAGNOSTIC_STATE_CHANGED";

    private MediaSession mediaSession;
    private MusicManager musicManager;
    private HardwareEventRecorder eventRecorder;

    @Override
    public void onCreate() {
        super.onCreate();
        eventRecorder = HardwareEventRecorder.getInstance(this);
        musicManager = MusicManager.getInstance(getApplicationContext());

        mediaSession = new MediaSession(this, "CarMediaService");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setPlaybackState(buildPlaybackState(PlaybackState.STATE_PAUSED));
        setSessionToken(mediaSession.getSessionToken());
        musicManager.addListener(this);
        updateSessionActive();
        eventRecorder.record("MEDIA_SESSION", "created active=" + mediaSession.isActive());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DIAGNOSTIC_STATE_CHANGED.equals(intent.getAction())) {
            updateSessionActive();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        if (musicManager != null) {
            musicManager.removeListener(this);
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

    private PlaybackState buildPlaybackState(int state) {
        return new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                        state == PlaybackState.STATE_PLAYING ? 1.0f : 0.0f)
                .build();
    }

    private void updateSessionActive() {
        if (mediaSession != null) {
            mediaSession.setActive(isInternalPlayerActive()
                    || (eventRecorder != null && eventRecorder.isRecording()));
        }
    }

    @Override
    public void onTrackChanged(String title, String artist, android.graphics.Bitmap albumArt,
            String packageName) {
        // Playback state and input routing are the only responsibilities here.
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (mediaSession != null) {
            mediaSession.setPlaybackState(buildPlaybackState(isPlaying
                    ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED));
        }
        updateSessionActive();
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
        updateSessionActive();
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
                mediaSession.setPlaybackState(buildPlaybackState(PlaybackState.STATE_PLAYING));
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
                mediaSession.setPlaybackState(buildPlaybackState(PlaybackState.STATE_PAUSED));
            }
        }

        @Override
        public void onSkipToNext() {
            if (!isInternalPlayerActive()) return;

            InternalMusicPlayer player = musicManager.getInternalPlayer();
            if (player != null) {
                player.playNext();
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (!isInternalPlayerActive()) return;

            InternalMusicPlayer player = musicManager.getInternalPlayer();
            if (player != null) {
                player.playPrevious();
            }
        }


        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            eventRecorder.recordIntent("MEDIA_SESSION", mediaButtonIntent);
            android.view.KeyEvent keyEvent = (android.view.KeyEvent)
                    mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            eventRecorder.recordKeyEvent("MEDIA_SESSION_KEY", keyEvent);
            if (!isInternalPlayerActive()) return false;
            return super.onMediaButtonEvent(mediaButtonIntent);
        }
    }
}
