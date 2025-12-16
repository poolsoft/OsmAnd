package net.osmand.plus.carlauncher.music;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Merkezi Müzik Yoneticisi.
 * Hem yerel (Internal) hem de harici (External - Spotify, YouTube vs) muzik
 * kaynaklarini yonetir.
 * Oncelik her zaman aktif calan harici kaynaktadir. Harici kaynak durdugunda
 * veya yoksa, dahili player kontrol edilir.
 */
public class MusicManager implements InternalMusicPlayer.PlaybackListener {

    private static final String TAG = "MusicManager";
    private static MusicManager instance;

    private final Context context;
    private final MusicRepository repository;
    private final InternalMusicPlayer internalPlayer;
    private MediaSessionManager mediaSessionManager;
    private MediaController activeExternalController;

    private boolean isInternalPlaying = false;

    // UI Listeners
    private final List<MusicUIListener> listeners = new CopyOnWriteArrayList<>();

    public interface MusicUIListener {
        void onTrackChanged(String title, String artist, android.graphics.Bitmap albumArt, String packageName);

        void onPlaybackStateChanged(boolean isPlaying);

        void onSourceChanged(boolean isInternal); // internal vs external
    }

    private MusicManager(Context context) {
        this.context = context.getApplicationContext();
        this.repository = new MusicRepository(this.context);
        this.internalPlayer = new InternalMusicPlayer(this.context);
        this.internalPlayer.setListener(this);

        setupMediaSessionManager();

        // Scan music on start
        repository.scanMusic((tracks, folders) -> {
            Log.d(TAG, "Scan complete: " + tracks.size() + " tracks");
            if (!tracks.isEmpty()) {
                // Varsayilan liste
                internalPlayer.setPlaylist(tracks, 0);
            }
        });
    }

    public static synchronized MusicManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicManager(context);
        }
        return instance;
    }

    public MusicRepository getRepository() {
        return repository;
    }

    public InternalMusicPlayer getInternalPlayer() {
        return internalPlayer;
    }

    // --- Media Session (External) Setup ---

    private void setupMediaSessionManager() {
        mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (mediaSessionManager == null)
            return;

        mediaSessionManager.addOnActiveSessionsChangedListener(
                controllers -> updateActiveController(controllers),
                new ComponentName(context, "net.osmand.plus.carlauncher.MediaNotificationListener"));

        // Initial check
        try {
            updateActiveController(mediaSessionManager.getActiveSessions(
                    new ComponentName(context, "net.osmand.plus.carlauncher.MediaNotificationListener")));
        } catch (SecurityException e) {
            // Permission might not be granted yet
        }
    }

    private void updateActiveController(List<MediaController> controllers) {
        // Find a playing controller or just first one
        MediaController candidate = null;
        if (controllers != null) {
            for (MediaController controller : controllers) {
                PlaybackState state = controller.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    candidate = controller;
                    break;
                }
            }
            // Fallback to first if none playing
            if (candidate == null && controllers != null && !controllers.isEmpty()) {
                candidate = controllers.get(0);
            }
        }

        if (activeExternalController != candidate) {
            if (activeExternalController != null) {
                activeExternalController.unregisterCallback(externalCallback);
            }
            activeExternalController = candidate;
            if (activeExternalController != null) {
                activeExternalController.registerCallback(externalCallback);
                // Trigger update
                externalCallback.onMetadataChanged(activeExternalController.getMetadata());
                externalCallback.onPlaybackStateChanged(activeExternalController.getPlaybackState());
            }
        }
    }

    private final MediaController.Callback externalCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            boolean isExternalPlaying = state != null && state.getState() == PlaybackState.STATE_PLAYING;
            if (isExternalPlaying) {
                // Pause internal if external starts
                if (internalPlayer.isPlaying()) {
                    internalPlayer.pause();
                }
            }
            notifyStateChanged();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            notifyTrackChanged();
        }
    };

    // --- Controls ---

    public void playPause() {
        if (useExternal()) {
            MediaController.TransportControls controls = activeExternalController.getTransportControls();
            PlaybackState state = activeExternalController.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                controls.pause();
            } else {
                controls.play();
            }
        } else {
            internalPlayer.playPause();
        }
    }

    public void next() {
        if (useExternal()) {
            activeExternalController.getTransportControls().skipToNext();
        } else {
            internalPlayer.playNext();
        }
    }

    public void prev() {
        if (useExternal()) {
            activeExternalController.getTransportControls().skipToPrevious();
        } else {
            internalPlayer.playPrevious();
        }
    }

    // --- Logic ---

    private boolean useExternal() {
        if (activeExternalController != null) {
            PlaybackState state = activeExternalController.getPlaybackState();
            if (state != null) {
                // Eger external caliyorsa veya internal calmiyorsa external kullan
                boolean extPlaying = state.getState() == PlaybackState.STATE_PLAYING;
                boolean intPlaying = internalPlayer.isPlaying();

                if (extPlaying)
                    return true;
                if (intPlaying)
                    return false;

                // Ikisi de duruyorsa, son aktif olani tercih et (Basitce external)
                return true;
            }
        }
        return false;
    }

    // --- Internal Player Callbacks ---

    @Override
    public void onTrackChanged(MusicRepository.AudioTrack track) {
        if (!useExternal()) {
            notifyTrackChanged();
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        isInternalPlaying = isPlaying;
        if (!useExternal()) {
            notifyStateChanged();
        }
    }

    @Override
    public void onCompletion() {
        // Internal finished, next handled by InternalPlayer
    }

    // --- UI Notifications ---

    public void addListener(MusicUIListener listener) {
        listeners.add(listener);
        // Instant update
        new Handler(Looper.getMainLooper()).post(() -> {
            notifyTrackChangedForListener(listener);
            notifyStateChangedForListener(listener);
        });
    }

    public void removeListener(MusicUIListener listener) {
        listeners.remove(listener);
    }

    private void notifyTrackChanged() {
        for (MusicUIListener l : listeners) {
            notifyTrackChangedForListener(l);
        }
    }

    private void notifyStateChanged() {
        for (MusicUIListener l : listeners) {
            notifyStateChangedForListener(l);
        }
    }

    private void notifyTrackChangedForListener(MusicUIListener l) {
        if (useExternal() && activeExternalController != null) {
            MediaMetadata metadata = activeExternalController.getMetadata();
            if (metadata != null) {
                String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                android.graphics.Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                String pkg = activeExternalController.getPackageName();
                l.onTrackChanged(title, artist, art, pkg);
                l.onSourceChanged(false);
            }
        } else {
            MusicRepository.AudioTrack track = internalPlayer.getCurrentTrack();
            if (track != null) {
                // Load art uri to bitmap separately if needed, passing null for now or decoding
                // in UI
                // Load art uri to bitmap separately if needed, passing null for now or decoding
                // in UI
                l.onTrackChanged(track.getTitle(), track.getArtist(), null, context.getPackageName()); // Bitmap
                                                                                                       // decoding is
                                                                                                       // heavy, handle
                                                                                                       // in UI
                // using Uri
                // using Uri
                l.onSourceChanged(true);
            } else {
                l.onTrackChanged("Muzik Yok", "", null, context.getPackageName());
                l.onSourceChanged(true);
            }
        }
    }

    private void notifyStateChangedForListener(MusicUIListener l) {
        if (useExternal() && activeExternalController != null) {
            PlaybackState state = activeExternalController.getPlaybackState();
            boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
            l.onPlaybackStateChanged(playing);
        } else {
            l.onPlaybackStateChanged(internalPlayer.isPlaying());
        }
    }
}
