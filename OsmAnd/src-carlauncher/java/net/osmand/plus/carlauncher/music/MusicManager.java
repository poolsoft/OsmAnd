package net.osmand.plus.carlauncher.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
    private String preferredPackage;

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

        // Baslangicta muzikleri tara
        repository.scanMusic((tracks, folders) -> {
            Log.d(TAG, "Scan complete: " + tracks.size() + " tracks");
            if (!tracks.isEmpty()) {
                internalPlayer.setPlaylist(tracks, 0);
                
                // Auto Play Check
                net.osmand.plus.carlauncher.CarLauncherSettings settings = 
                     new net.osmand.plus.carlauncher.CarLauncherSettings(context);
                if (settings.isAutoPlayMusicEnabled()) {
                     internalPlayer.play();
                }
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

    public MediaController getActiveExternalController() {
        return activeExternalController;
    }

    public void setPreferredPackage(String packageName) {
        this.preferredPackage = packageName;

        // If switching to external app, pause internal player
        if (packageName != null && !packageName.equals("usage.internal.player")) {
            if (internalPlayer != null && internalPlayer.isPlaying()) {
                internalPlayer.pause();
            }
        }
        
        // State might have changed (Internal vs External), notify UI to refresh
        notifyTrackChanged();
        notifyStateChanged();

        // Tercih edilen paket değiştiğinde kontrolcüyü güncellemeye çalış
        if (mediaSessionManager != null) {
            try {
                ComponentName listenerComp = new ComponentName(context,
                        "net.osmand.plus.carlauncher.MediaNotificationListener");
                updateActiveController(mediaSessionManager.getActiveSessions(listenerComp));
            } catch (Exception e) {
                Log.w(TAG, "Failed to update controller for preferred package: " + e.getMessage());
            }
        }
    }

    public String getPreferredPackage() {
        return preferredPackage;
    }

    // --- Media Session (External) Setup ---

    private void setupMediaSessionManager() {
        mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (mediaSessionManager == null)
            return;

        ComponentName listenerComponent = new ComponentName(context,
                "net.osmand.plus.carlauncher.MediaNotificationListener");

        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    this::updateActiveController,
                    listenerComponent);

            // İlk kontrol
            updateActiveController(mediaSessionManager.getActiveSessions(listenerComponent));

        } catch (SecurityException e) {
            Log.e(TAG, "Media Control izni yok! Kullanıcı ayarlardan izin vermeli.", e);
        } catch (Exception e) {
            Log.e(TAG, "MediaSession setup hatası", e);
        }
    }

    private void updateActiveController(List<MediaController> controllers) {
        MediaController candidate = null;
        if (controllers != null) {
            // 1. Önce aktif çalanı bul
            for (MediaController controller : controllers) {
                PlaybackState state = controller.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    candidate = controller;
                    break;
                }
            }
            // 2. Çalan yoksa ve tercih edilen paket varsa onu bul
            if (candidate == null && preferredPackage != null) {
                for (MediaController controller : controllers) {
                    if (controller.getPackageName().equals(preferredPackage)) {
                        candidate = controller;
                        break;
                    }
                }
            }
            // 3. Hiçbiri yoksa listedeki ilk uygulamayı al
            if (candidate == null && !controllers.isEmpty()) {
                candidate = controllers.get(0);
            }
        }

        if (activeExternalController != candidate) {
            // Eskiyi temizle
            if (activeExternalController != null) {
                activeExternalController.unregisterCallback(externalCallback);
            }

            // Yeniyi ata
            activeExternalController = candidate;

            if (activeExternalController != null) {
                activeExternalController.registerCallback(externalCallback);
                // UI Güncelle
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
                // Harici kaynak çalmaya başlarsa dahiliyi durdur
                if (internalPlayer.isPlaying()) {
                    internalPlayer.pause();
                }
            }
            notifyStateChanged();
            updateVisualizerState();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            notifyTrackChanged();
        }

        @Override
        public void onSessionDestroyed() {
            // Oturum kapandıysa (örn: Spotify kapatıldı)
            activeExternalController = null;
            notifyTrackChanged(); // UI temizlensin
        }
    };

    // --- Controls (Widget ile uyumlu isimler) ---

    public void togglePlayPause() {
        // V15: Refresh active sessions to ensure we don't miss Spotify
        // Assume context is available (it is activeExternalController based)
        MediaSessionManager manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (manager != null) {
            try {
                // Determine component name for listener (required for getActiveSessions)
                // Using null often works for all, or use 'componentName' from constructor if saved.
                // Re-running updateActiveController logic manually:
                List<MediaController> controllers = manager.getActiveSessions(null);
                updateActiveController(controllers);
            } catch (Exception e) {
                 android.util.Log.e(TAG, "Failed to refresh sessions in togglePlayPause", e);
            }
        }
        
        // Prioritize controlling the ACTIVE external session if it exists
        if (activeExternalController != null) {
            PlaybackState state = activeExternalController.getPlaybackState();
            boolean isExternalActive = (state != null) && 
                (state.getState() == PlaybackState.STATE_PLAYING || 
                 state.getState() == PlaybackState.STATE_BUFFERING);

            if (isExternalActive) {
                // If it's playing, PAUSE it (Priority 1)
                activeExternalController.getTransportControls().pause();
                return;
            } else if (useExternal()) {
                // If we are in "External Mode" and it's paused, PLAY it
                activeExternalController.getTransportControls().play();
                return;
            }
        }
        
        // If we get here, either External is not active, or we are in Internal Mode
        if (activeExternalController == null && preferredPackage != null && !internalPlayer.isPlaying()) {
            // If no session exists but user has a preference, try to launch it
             startPreferredApplication(preferredPackage);
        } else {
            // Default to Internal Player
            internalPlayer.playPause();
        }
    }

    private void startPreferredApplication(String pkg) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch " + pkg, e);
        }
    }

    public void skipToNext() {
        if (useExternal()) {
            activeExternalController.getTransportControls().skipToNext();
        } else {
            internalPlayer.playNext();
        }
    }

    public void skipToPrevious() {
        if (useExternal()) {
            activeExternalController.getTransportControls().skipToPrevious();
        } else {
            internalPlayer.playPrevious();
        }
    }

    // --- Logic ---

    private boolean useExternal() {
        // 1. Dahili oynatıcı aktifse, kesinlikle dahili kullan (Focus bizde)
        if (internalPlayer.isPlaying()) {
            return false;
        }

        // 2. Harici bir kaynak varsa (Çalıyor VEYA Duraklatılmış), onu kullan.
        // Böylece Spotify durduğunda hemen Dahili'ye düşmeyiz. Resume edebiliriz.
        if (activeExternalController != null) {
            return true;
        }

        // 3. Kimse yoksa (activeExternalController == null), 
        // tercih edilen paket varsa belki bir gün lazım olur:
        if (preferredPackage != null && !preferredPackage.equals("usage.internal.player")) {
            return true;
        }

        // 4. Hiçbir şey yoksa Dahili varsayılan.
        return false;
    }

    // --- Internal Player Callbacks ---

    // --- Internal Player Callbacks ---

    @Override
    public void onTrackChanged(MusicRepository.AudioTrack track) {
        if (!useExternal()) {
            notifyTrackChanged();
            updateNotificationService();
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        isInternalPlaying = isPlaying;
        if (!useExternal()) {
            notifyStateChanged();
            updateNotificationService();
        } else {
             // If external takes over, internal notification should probably stop or pause?
             // If we switch to external, internal pauses automatically (see updateActiveController).
             // But valid to ensure service updates.
        }
        updateVisualizerState();
    }

    @Override
    public void onCompletion() {
        // Internal bittiğinde yapılacaklar
        // Maybe close service if playlist ended?
        // But player usually loops or goes to next.
        updateNotificationService();
    }
    
    private void updateNotificationService() {
        // Only managing notification for INTERNAL player.
        // External players manage their own notifications.
        
        Intent intent = new Intent(context, MusicPlaybackService.class);
        
        if (internalPlayer.isPlaying() || (internalPlayer.getCurrentTrack() != null && isInternalPlaying)) {
             intent.setAction(MusicPlaybackService.ACTION_UPDATE);
             // Start Service
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                 context.startForegroundService(intent);
             } else {
                 context.startService(intent);
             }
        } else {
            // Not playing. 
            // If just paused, we might want to keep notification for a while (to resume).
            // Logic: ACTION_UPDATE will update notification to "Paused" state.
            // If stopped fully or app closing, ACTION_CLOSE.
            // For now, let's keep it alive on Pause (so user can resume).
            // But if user explicitly closed app?
            // Let's send UPDATE. The Service handles self-stop if needed or user clicks X.
             intent.setAction(MusicPlaybackService.ACTION_UPDATE);
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                 context.startForegroundService(intent);
             } else {
                 context.startService(intent);
             }
        }
        
    }

    // --- UI Notifications ---

    public void addListener(MusicUIListener listener) {
        listeners.add(listener);
        // Yeni dinleyici eklendiğinde hemen mevcut durumu bildir
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
            String pkg = activeExternalController.getPackageName();

            if (metadata != null) {
                String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                // Not: Bitmap almak main thread'i yavaşlatabilir, dikkat edilmeli.
                android.graphics.Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);

                l.onTrackChanged(
                        title != null ? title : "Bilinmeyen",
                        artist != null ? artist : "",
                        art,
                        pkg);
                l.onSourceChanged(false);
            } else {
                // Metadata yok ama controller var
                l.onTrackChanged(null, null, null, pkg);
            }
        } else {
            MusicRepository.AudioTrack track = internalPlayer.getCurrentTrack();
            if (track != null) {
                l.onTrackChanged(track.getTitle(), track.getArtist(), null, context.getPackageName());
                l.onSourceChanged(true);
            } else {
                l.onTrackChanged("Muzik Secin", "", null, context.getPackageName());
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
            // Internal Player State
            l.onPlaybackStateChanged(internalPlayer.isPlaying());
        }
    }
    
    // Trigger Visualizer State Update
    private void checkVisualizerState() {
        if (isPlaying()) {
            if (!visualizerListeners.isEmpty()) startVisualizer();
        } else {
            stopVisualizer();
        }
    }
    // --- Visualizer Centralization ---

    private android.media.audiofx.Visualizer mVisualizer;
    public interface MusicVisualizerListener {
        void onFftDataCapture(byte[] fft);
    }
    private final List<MusicVisualizerListener> visualizerListeners = new CopyOnWriteArrayList<>();

    public void addVisualizerListener(MusicVisualizerListener listener) {
        visualizerListeners.add(listener);
        if (isPlaying()) {
            startVisualizer();
        }
    }

    public void removeVisualizerListener(MusicVisualizerListener listener) {
        visualizerListeners.remove(listener);
        if (visualizerListeners.isEmpty()) {
            stopVisualizer();
        }
    }

    private void startVisualizer() {
        if (mVisualizer != null) return;
        
        // 1. Check Permissions
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
             Log.w(TAG, "RECORD_AUDIO permission missing for Visualizer.");
             return;
        }

        try {
            // 2. Determine Session ID
            int sessionId = 0; // Global Mix (External)
            if (internalPlayer.isPlaying()) {
                sessionId = internalPlayer.getAudioSessionId();
            }
            
            // 3. Create Visualizer
            mVisualizer = new android.media.audiofx.Visualizer(sessionId);
            mVisualizer.setCaptureSize(android.media.audiofx.Visualizer.getCaptureSizeRange()[1]);
            mVisualizer.setDataCaptureListener(new android.media.audiofx.Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(android.media.audiofx.Visualizer visualizer, byte[] waveform, int samplingRate) {
                }

                @Override
                public void onFftDataCapture(android.media.audiofx.Visualizer visualizer, byte[] fft, int samplingRate) {
                    for (MusicVisualizerListener l : visualizerListeners) {
                         l.onFftDataCapture(fft);
                    }
                }
            }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 2, false, true);
            
            mVisualizer.setEnabled(true);
            Log.d(TAG, "Visualizer Started. Session: " + sessionId);

        } catch (Exception e) {
            Log.e(TAG, "Failed to start Visualizer", e);
            stopVisualizer();
        }
    }

    private void stopVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.release();
            mVisualizer = null;
            Log.d(TAG, "Visualizer Stopped");
        }
    }

    private boolean isPlaying() {
        return internalPlayer.isPlaying() || (activeExternalController != null && 
               activeExternalController.getPlaybackState() != null && 
               activeExternalController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING);
    }

    // Update Visualizer State based on Playback
    private void updateVisualizerState() {
        if (isPlaying() && !visualizerListeners.isEmpty()) {
            startVisualizer();
        } else {
            stopVisualizer();
        }
    }

}