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
                // internalPlayer.resumeLastSession(); // KALDIRILDI: Aggressive auto-play prevention
                // Sadece playlist'i hazirla ama baslatma.
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

    public void setPreferredPackage(String packageName) {
        this.preferredPackage = packageName;

        // If switching to external app, pause internal player
        if (packageName != null && !packageName.equals("usage.internal.player")) {
            if (internalPlayer != null && internalPlayer.isPlaying()) {
                internalPlayer.pause();
            }
        }

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
        if (useExternal()) {
            MediaController.TransportControls controls = activeExternalController.getTransportControls();
            PlaybackState state = activeExternalController.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                controls.pause();
            } else {
                controls.play();
            }
        } else if (preferredPackage != null && activeExternalController == null) {
            // Hiçbir session yoksa tercih edilen uygulamayı başlat
            startPreferredApplication(preferredPackage);
        } else {
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

        // 2. Harici bir kaynak aktif olarak oynuyorsa, onu kullan
        if (activeExternalController != null) {
            PlaybackState state = activeExternalController.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                return true;
            }
        }

        // 3. Kimse oynamıyorsa, tercih edilen pakete göre karar ver
        if (preferredPackage != null && !preferredPackage.equals("usage.internal.player")) {
            return true;
        }

        // 4. Varsayılan: Dahili
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
        // Internal bittiğinde yapılacaklar
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
            l.onPlaybackStateChanged(internalPlayer.isPlaying());
        }
    }
}