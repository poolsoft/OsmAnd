package net.osmand.plus.carlauncher.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Merkezi Müzik Yoneticisi.
 * Hem yerel (Internal) hem de harici (External - Spotify, YouTube vs) muzik
 * kaynaklarini yonetir.
 *
 * Permission Note: External control requires 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'
 * granted by user in System Settings.
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

    // XYAuto yerel muzik oynatici durum degiskenleri
    private String xyTrackTitle = null;
    private String xyTrackArtist = null;
    private String xyTrackAlbumArtPath = null;
    private boolean xyIsPlaying = false;
    private int xyDuration = 0;
    private int xyPosition = 0;

    // XYAuto yerel muzik yayinlarini dinleyen alici
    private final BroadcastReceiver xyAutoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            Log.d(TAG, "XYAuto yayini alindi: " + action);
            
            boolean changed = false;
            
            if ("update.widget.playbtnstate".equals(action)) {
                boolean oldPlaying = xyIsPlaying;
                xyIsPlaying = intent.getBooleanExtra("PlayState", false);
                if (xyIsPlaying != oldPlaying) {
                    changed = true;
                    if (xyIsPlaying) {
                        lastActiveSource = MusicSource.EXTERNAL;
                        if (internalPlayer.isPlaying()) {
                            internalPlayer.pause();
                        }
                        if (!"com.acloud.stub.localmusic".equals(preferredPackage)) {
                            setPreferredPackage("com.acloud.stub.localmusic");
                        }
                    }
                }
            } else if ("update.widget.songname".equals(action)) {
                String fullSongName = intent.getStringExtra("curplaysong");
                String oldTitle = xyTrackTitle;
                String oldArtist = xyTrackArtist;
                
                if (fullSongName != null) {
                    if (fullSongName.contains(" - ")) {
                        String[] parts = fullSongName.split(" - ", 2);
                        xyTrackTitle = parts[0].trim();
                        xyTrackArtist = parts[1].trim();
                    } else if (fullSongName.contains("-")) {
                        String[] parts = fullSongName.split("-", 2);
                        xyTrackTitle = parts[0].trim();
                        xyTrackArtist = parts[1].trim();
                    } else {
                        xyTrackTitle = fullSongName;
                        xyTrackArtist = "";
                    }
                } else {
                    xyTrackTitle = null;
                    xyTrackArtist = null;
                }
                
                if (intent.hasExtra("artistPicPath")) {
                    xyTrackAlbumArtPath = intent.getStringExtra("artistPicPath");
                    if (TextUtils.isEmpty(xyTrackArtist) && !TextUtils.isEmpty(xyTrackAlbumArtPath)) {
                        try {
                            java.io.File file = new java.io.File(xyTrackAlbumArtPath);
                            String name = file.getName();
                            int dot = name.lastIndexOf('.');
                            if (dot > 0) {
                                xyTrackArtist = name.substring(0, dot);
                            }
                        } catch (Exception e) {
                            // Hata durumunda yoksay
                        }
                    }
                }
                
                boolean playState = intent.getBooleanExtra("PlayState", false);
                if (playState) {
                    xyIsPlaying = true;
                    lastActiveSource = MusicSource.EXTERNAL;
                    if (internalPlayer.isPlaying()) {
                        internalPlayer.pause();
                    }
                    if (!"com.acloud.stub.localmusic".equals(preferredPackage)) {
                        setPreferredPackage("com.acloud.stub.localmusic");
                    }
                }
                
                if (!TextUtils.equals(oldTitle, xyTrackTitle) || !TextUtils.equals(oldArtist, xyTrackArtist)) {
                    changed = true;
                }
            } else if ("update.widget.update_proBar".equals(action)) {
                xyDuration = intent.getIntExtra("proBarmax", 0);
                xyPosition = intent.getIntExtra("proBarvalue", 0);
                
                String song = intent.getStringExtra("curplaysong");
                if (song != null && !TextUtils.equals(xyTrackTitle, song)) {
                    xyTrackTitle = song;
                    changed = true;
                }
                if (intent.hasExtra("artistPicPath")) {
                    xyTrackAlbumArtPath = intent.getStringExtra("artistPicPath");
                }
            }
            
            if (changed) {
                notifyTrackChanged();
            }
            notifyStateChanged();
        }
    };

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

        // XYAuto yerel muzik broadcast yayinlarini dinle
        IntentFilter filter = new IntentFilter();
        filter.addAction("update.widget.playbtnstate");
        filter.addAction("update.widget.update_proBar");
        filter.addAction("update.widget.songname");
        filter.addAction("update.widget.btnfun");
        filter.addAction("update.widget.dataError");
        filter.addAction("update.widget.musicinit");
        filter.addAction("update.widget.cdinit");
        filter.addAction("update.widget.albumpic");
        context.registerReceiver(xyAutoReceiver, filter);

        // Baslangicta muzikleri tara
        repository.scanMusic((tracks, folders) -> {
            Log.d(TAG, "Scan complete: " + tracks.size() + " tracks");
            if (!tracks.isEmpty()) {
                // Initialize playlist but DO NOT auto play yet
                // internalPlayer uses autoPlay=false here
                internalPlayer.setPlaylist(tracks, 0, false);
                
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

        // XYAuto yerel muzik baglantisi
        if ("com.acloud.stub.localmusic".equals(packageName)) {
            bindXyPlayService();
        } else {
            unbindXyPlayService();
        }

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

    public int getXyDuration() {
        if (xyPlayService != null && xyServiceBound) {
            try {
                return xyPlayService.getDuration();
            } catch (Exception e) {
                Log.e(TAG, "XYPlayService getDuration hatasi: " + e.getMessage());
            }
        }
        return 0;
    }

    public int getXyPosition() {
        if (xyPlayService != null && xyServiceBound) {
            try {
                return xyPlayService.getPosition();
            } catch (Exception e) {
                Log.e(TAG, "XYPlayService getPosition hatasi: " + e.getMessage());
            }
        }
        return 0;
    }

    public void seekXy(int position) {
        if (xyPlayService != null && xyServiceBound) {
            try {
                xyPlayService.seekTo(position);
                Log.d(TAG, "XYPlayService seekTo: " + position);
            } catch (Exception e) {
                Log.e(TAG, "XYPlayService seekTo hatasi: " + e.getMessage());
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

    public boolean checkNotificationAccess() {
        try {
            return androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
                    .contains(context.getPackageName());
        } catch (NoClassDefFoundError e) {
            // Fallback for older/custom androids if compat lib missing
             String enabledListeners = android.provider.Settings.Secure.getString(context.getContentResolver(), 
                "enabled_notification_listeners");
             return enabledListeners != null && enabledListeners.contains(context.getPackageName());
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
                lastActiveSource = MusicSource.EXTERNAL; // Mark External as source
                // Harici kaynak çalmaya başlarsa dahiliyi durdur
                if (internalPlayer.isPlaying()) {
                    internalPlayer.pause();
                }
                if (activeExternalController != null) {
                    String pkg = activeExternalController.getPackageName();
                    if (pkg != null && !pkg.equals(preferredPackage)) {
                        preferredPackage = pkg;
                        if ("com.acloud.stub.localmusic".equals(pkg)) {
                            bindXyPlayService();
                        } else {
                            unbindXyPlayService();
                        }
                    }
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

    private enum MusicSource {
        INTERNAL,
        EXTERNAL
    }
    
    private MusicSource lastActiveSource = MusicSource.INTERNAL; // Default

    private com.acloud.stub.service.aidl.IPlayService xyPlayService;
    private boolean xyServiceBound = false;

    private final android.content.ServiceConnection xyServiceConnection = new android.content.ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, android.os.IBinder service) {
            xyPlayService = com.acloud.stub.service.aidl.IPlayService.Stub.asInterface(service);
            xyServiceBound = true;
            Log.d(TAG, "XYPlayService baglantisi kuruldu.");
            notifyStateChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            xyPlayService = null;
            xyServiceBound = false;
            Log.d(TAG, "XYPlayService baglantisi kesildi.");
            notifyStateChanged();
        }
    };

    private void bindXyPlayService() {
        if (xyServiceBound) return;
        try {
            Intent intent = new Intent("com.acloud.stub.service.aidl.IPlayService");
            intent.setClassName("com.acloud.stub.localmusic", "com.acloud.stub.service.XYPlayerService");
            intent.setAction("init_widget");
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            context.bindService(intent, xyServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "XYPlayService baglaniyor...");
        } catch (Exception e) {
            Log.e(TAG, "XYPlayService baglanirken hata: " + e.getMessage());
        }
    }

    private void sendXyMusicServiceCommand(String action) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.acloud.stub.localmusic", "com.acloud.stub.service.XYPlayerService");
            intent.setAction(action);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            Log.d(TAG, "XYPlayerService komutu gonderildi: " + action);
        } catch (Exception e) {
            Log.e(TAG, "XYPlayerService komutu gonderilirken hata: " + action, e);
        }
    }

    private void unbindXyPlayService() {
        if (!xyServiceBound) return;
        try {
            context.unbindService(xyServiceConnection);
            xyPlayService = null;
            xyServiceBound = false;
            Log.d(TAG, "XYPlayService baglantisi koparildi.");
        } catch (Exception e) {
            Log.e(TAG, "XYPlayService baglantisi koparilirken hata: " + e.getMessage());
        }
    }

    private void sendXyMusicBroadcast(String action) {
        try {
            Intent intent = new Intent(action);
            context.sendBroadcast(intent);
            Log.d(TAG, "Sent XY music broadcast: " + action);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send XY music broadcast: " + action, e);
        }
    }

    private void sendMediaKey(int keycode) {
        try {
            android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                long eventtime = android.os.SystemClock.uptimeMillis();
                android.view.KeyEvent downEvent = new android.view.KeyEvent(eventtime, eventtime, android.view.KeyEvent.ACTION_DOWN, keycode, 0);
                android.view.KeyEvent upEvent = new android.view.KeyEvent(eventtime, eventtime, android.view.KeyEvent.ACTION_UP, keycode, 0);
                am.dispatchMediaKeyEvent(downEvent);
                am.dispatchMediaKeyEvent(upEvent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send media key: " + keycode, e);
        }
    }

    public void togglePlayPause() {
        // 1. Permission Check
        if (!checkNotificationAccess()) {
            android.widget.Toast.makeText(context, "Harici kontrol icin 'Bildirim Erisimi' izni gerekli!", android.widget.Toast.LENGTH_LONG).show();
        }

        // 2. Refresh External Sessions
        MediaSessionManager manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (manager != null) {
            try {
                ComponentName listenerComp = new ComponentName(context, "net.osmand.plus.carlauncher.MediaNotificationListener");
                List<MediaController> controllers = manager.getActiveSessions(listenerComp);
                updateActiveController(controllers);
            } catch (Exception e) {
                 // Session refresh failed (Normal if no permission)
            }
        }

        // 3. If Internal player has track and is active, toggle it
        if (internalPlayer.isPlaying() || (preferredPackage != null && preferredPackage.equals("usage.internal.player"))) {
            internalPlayer.playPause();
            lastActiveSource = MusicSource.INTERNAL;
            return;
        }

        // XYAuto yerel muzik entegrasyonu
        if ("com.acloud.stub.localmusic".equals(preferredPackage)) {
            boolean done = false;
            if (xyPlayService != null && xyServiceBound) {
                try {
                    int state = xyPlayService.getState();
                    if (state == 3 || xyIsPlaying) { // 3 = playing
                        xyPlayService.pause();
                    } else {
                        xyPlayService.start();
                    }
                    done = true;
                } catch (Exception e) {
                    Log.e(TAG, "XYPlayService togglePlayPause hatasi: " + e.getMessage());
                }
            }
            if (!done) {
                if (xyIsPlaying) {
                    sendXyMusicServiceCommand("xy.cdwidget.pause");
                } else {
                    sendXyMusicServiceCommand("xy.cdwidget.play");
                }
            }
            sendXyMusicBroadcast("xy.android.playpause");
            lastActiveSource = MusicSource.EXTERNAL;
            notifyStateChanged();
            return;
        }

        // 4. Preferred Package Priority with active controller
        if (preferredPackage != null && !preferredPackage.equals("usage.internal.player")) {
            if (activeExternalController != null && activeExternalController.getPackageName().equals(preferredPackage)) {
                try {
                    PlaybackState state = activeExternalController.getPlaybackState();
                    if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                        activeExternalController.getTransportControls().pause();
                    } else {
                        activeExternalController.getTransportControls().play();
                    }
                    lastActiveSource = MusicSource.EXTERNAL;
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to toggle via preferred controller, falling back", e);
                }
            }
        }

        // 5. Default Active controller fallback
        if (activeExternalController != null) {
            try {
                PlaybackState state = activeExternalController.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    activeExternalController.getTransportControls().pause();
                } else {
                    activeExternalController.getTransportControls().play();
                }
                lastActiveSource = MusicSource.EXTERNAL;
                return;
            } catch (Exception e) {
                Log.e(TAG, "Failed to toggle via active controller, falling back", e);
            }
        }

        // 6. Universal Fallback: Send media key event to active focus owner
        sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        lastActiveSource = MusicSource.EXTERNAL;

        // 7. Wake up preferred application if not running
        if (preferredPackage != null && !preferredPackage.equals("usage.internal.player")) {
            if (activeExternalController == null || !activeExternalController.getPackageName().equals(preferredPackage)) {
                startPreferredApplication(preferredPackage);
            }
        }
    }

    public void skipToNext() {
        if (internalPlayer.isPlaying() || (preferredPackage != null && preferredPackage.equals("usage.internal.player"))) {
            internalPlayer.playNext();
            return;
        }
        if ("com.acloud.stub.localmusic".equals(preferredPackage)) {
            sendXyMusicServiceCommand("xy.cdwidget.next");
            sendXyMusicBroadcast("xy.android.nextmedia");
            return;
        }
        if (activeExternalController != null) {
            try {
                activeExternalController.getTransportControls().skipToNext();
                return;
            } catch (Exception e) {
                Log.e(TAG, "Failed to skipToNext via controller, falling back", e);
            }
        }
        sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    public void skipToPrevious() {
        if (internalPlayer.isPlaying() || (preferredPackage != null && preferredPackage.equals("usage.internal.player"))) {
            internalPlayer.playPrevious();
            return;
        }
        if ("com.acloud.stub.localmusic".equals(preferredPackage)) {
            sendXyMusicServiceCommand("xy.cdwidget.prev");
            sendXyMusicBroadcast("xy.android.previousmedia");
            return;
        }
        if (activeExternalController != null) {
            try {
                activeExternalController.getTransportControls().skipToPrevious();
                return;
            } catch (Exception e) {
                Log.e(TAG, "Failed to skipToPrevious via controller, falling back", e);
            }
        }
        sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS);
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



    // --- Logic ---

    /**
     * UTF-8 karakter bozulmalarini temizleyen yardimci metot.
     */
    private String sanitizeEncoding(String text) {
        if (text == null) return null;
        try {
            return text
                .replace("Ä\u00b0", "\u0130") // Buyuk I
                .replace("Ä\u00b1", "\u0131") // Kucuk i
                .replace("Å\u009f", "\u015f") // Kucuk s
                .replace("Å\u009e", "\u015e") // Buyuk S
                .replace("Ä\u009f", "\u011f") // Kucuk g
                .replace("Ä\u009e", "\u011e") // Buyuk G
                .replace("Ã\u00bc", "\u00fc") // Kucuk u
                .replace("Ã\u009c", "\u00dc") // Buyuk U
                .replace("Ã\u00b6", "\u00f6") // Kucuk o
                .replace("Ã\u0096", "\u00d6") // Buyuk O
                .replace("Ã\u00a7", "\u00e7") // Kucuk c
                .replace("Ã\u0087", "\u00c7"); // Buyuk C
        } catch (Exception e) {
            return text;
        }
    }

    public boolean isShuffleOn() {
        return internalPlayer.isShuffleOn();
    }

    public void setShuffleOn(boolean shuffleOn) {
        internalPlayer.setShuffleOn(shuffleOn);
        notifyStateChanged();
    }

    public int getRepeatMode() {
        return internalPlayer.getRepeatMode();
    }

    public void setRepeatMode(int repeatMode) {
        internalPlayer.setRepeatMode(repeatMode);
        notifyStateChanged();
    }

    public boolean useExternal() {
        // 1. Eger tercih edilen paket "usage.internal.player" ise, kesinlikle dahili kullan (Turkce karakter yok)
        if ("usage.internal.player".equals(preferredPackage)) {
            return false;
        }

        // 2. Tercih edilen paket harici bir uygulama ise, kesinlikle harici kullan (Turkce karakter yok)
        if (preferredPackage != null && !preferredPackage.equals("usage.internal.player")) {
            return true;
        }

        // 3. Eger tercih edilen paket null ise (Kullanici listeden sarkiya tiklayip baslatmissa):
        // En son aktif olan kaynaga gore karar ver (Turkce karakter yok)
        if (lastActiveSource == MusicSource.INTERNAL) {
            return false;
        }

        // 4. Eger en son harici aktifse ve harici kontrolcumuz varsa harici kullan (Turkce karakter yok)
        if (activeExternalController != null) {
            return true;
        }

        // Varsayilan olarak dahili
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
        if (isPlaying) {
             lastActiveSource = MusicSource.INTERNAL; // Mark Internal as source
             preferredPackage = "usage.internal.player";
             unbindXyPlayService();
        }
        
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
        if (useExternal()) {
            if ("com.acloud.stub.localmusic".equals(preferredPackage)) {
                android.graphics.Bitmap art = null;
                if (xyTrackAlbumArtPath != null && !xyTrackAlbumArtPath.isEmpty()) {
                    try {
                        art = BitmapFactory.decodeFile(xyTrackAlbumArtPath);
                    } catch (Exception e) {
                        Log.e(TAG, "XYAuto cover art decode hatasi", e);
                    }
                }
                l.onTrackChanged(
                        sanitizeEncoding(xyTrackTitle != null ? xyTrackTitle : "Bilinmeyen"),
                        sanitizeEncoding(xyTrackArtist != null ? xyTrackArtist : ""),
                        art,
                        preferredPackage);
                l.onSourceChanged(false);
            } else if (activeExternalController != null) {
                MediaMetadata metadata = activeExternalController.getMetadata();
                String pkg = activeExternalController.getPackageName();

                if (metadata != null) {
                    String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                    String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                    android.graphics.Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);

                    l.onTrackChanged(
                            sanitizeEncoding(title != null ? title : "Bilinmeyen"),
                            sanitizeEncoding(artist != null ? artist : ""),
                            art,
                            pkg);
                    l.onSourceChanged(false);
                } else {
                    l.onTrackChanged(null, null, null, pkg);
                }
            } else {
                l.onTrackChanged(null, null, null, preferredPackage);
                l.onSourceChanged(false);
            }
        } else {
            MusicRepository.AudioTrack track = internalPlayer.getCurrentTrack();
            if (track != null) {
                l.onTrackChanged(
                        sanitizeEncoding(track.getTitle()),
                        sanitizeEncoding(track.getArtist()),
                        null,
                        context.getPackageName());
                l.onSourceChanged(true);
            } else {
                l.onTrackChanged("Muzik Secin", "", null, context.getPackageName());
                l.onSourceChanged(true);
            }
        }
    }

    private void notifyStateChangedForListener(MusicUIListener l) {
        if (useExternal()) {
            if ("com.acloud.stub.localmusic".equals(preferredPackage)) {
                l.onPlaybackStateChanged(xyIsPlaying);
            } else if (activeExternalController != null) {
                PlaybackState state = activeExternalController.getPlaybackState();
                boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
                l.onPlaybackStateChanged(playing);
            } else {
                l.onPlaybackStateChanged(false);
            }
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
        if (useExternal()) {
            if ("com.acloud.stub.localmusic".equals(preferredPackage)) {
                return xyIsPlaying;
            }
            return activeExternalController != null && 
                   activeExternalController.getPlaybackState() != null && 
                   activeExternalController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
        }
        return internalPlayer.isPlaying();
    }

    // Update Visualizer State based on Playback
    private void updateVisualizerState() {
        if (isPlaying() && !visualizerListeners.isEmpty()) {
            startVisualizer();
        } else {
            stopVisualizer();
        }
    }

    // --- Smart Player Selector (NEW) ---
    
    /**
     * Data class for media session info (used by player selector UI)
     */
    public static class MediaSessionInfo {
        public String packageName;
        public String appName;
        public boolean isPlaying;
        public boolean isPaused;
        public String currentTrack;
        public boolean isActive; // Is this the currently controlled session?
    }
    
    /**
     * Get all active media sessions (for player selection UI)
     */
    public List<MediaSessionInfo> getActiveMediaSessions() {
        List<MediaSessionInfo> sessions = new ArrayList<>();
        
        if (mediaSessionManager != null) {
            try {
                ComponentName listener = new ComponentName(context, 
                    "net.osmand.plus.carlauncher.MediaNotificationListener");
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(listener);
                
                for (MediaController controller : controllers) {
                    String packageName = controller.getPackageName();
                    String appName = getAppName(packageName);
                    PlaybackState state = controller.getPlaybackState();
                    MediaMetadata metadata = controller.getMetadata();
                    
                    // Create info object
                    MediaSessionInfo info = new MediaSessionInfo();
                    info.packageName = packageName;
                    info.appName = appName;
                    info.isPlaying = (state != null && state.getState() == PlaybackState.STATE_PLAYING);
                    info.isPaused = (state != null && state.getState() == PlaybackState.STATE_PAUSED);
                    info.currentTrack = metadata != null ? 
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE) : null;
                    info.isActive = (controller == activeExternalController);
                    
                    sessions.add(info);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get active sessions", e);
            }
        }
        
        return sessions;
    }
    
    /**
     * Force set a specific package as active controller
     */
    public void forceSetActiveController(String packageName) {
        if (packageName.equals("usage.internal.player")) {
            // Switch to internal
            if (activeExternalController != null) {
                activeExternalController.unregisterCallback(externalCallback);
                activeExternalController = null;
            }
            lastActiveSource = MusicSource.INTERNAL;
            setPreferredPackage(packageName);
            notifyTrackChanged();
            notifyStateChanged();
            return;
        }
        
        // Find and set external controller
        if (mediaSessionManager != null) {
            try {
                ComponentName listener = new ComponentName(context, 
                    "net.osmand.plus.carlauncher.MediaNotificationListener");
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(listener);
                
                for (MediaController controller : controllers) {
                    if (controller.getPackageName().equals(packageName)) {
                        // Unregister old
                        if (activeExternalController != null) {
                            activeExternalController.unregisterCallback(externalCallback);
                        }
                        
                        // Set new
                        activeExternalController = controller;
                        activeExternalController.registerCallback(externalCallback);
                        lastActiveSource = MusicSource.EXTERNAL;
                        setPreferredPackage(packageName);
                        
                        // Update UI
                        externalCallback.onMetadataChanged(controller.getMetadata());
                        externalCallback.onPlaybackStateChanged(controller.getPlaybackState());
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to force set controller", e);
            }
        }
    }
    
    /**
     * NotificationListener tarafından çağrılır - session listesi güncellenir.
     * MediaNotificationListener.onNotificationPosted/Removed -> bu metodu tetikler.
     */
    public void onSessionsRefreshed(java.util.List<android.media.session.MediaController> controllers) {
        if (controllers != null) {
            updateActiveController(controllers);
        }
    }

    /**
     * Helper: Get app name from package
     */
    private String getAppName(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

}