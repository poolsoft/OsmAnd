package net.osmand.plus.carlauncher.widgets;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern Muzik Widget.
 * - Album Kapagi Arka Plani (Blur/Dim efektli)
 * - Gelismis Medya Kontrolleri
 * - Radyo destegi (com.hcn.autoradio)
 */
public class MusicWidget extends BaseWidget {

    private ImageView albumArtView;
    private View albumArtOverlay;
    private ImageView appIconView;
    private TextView titleText;
    private TextView artistText;
    private ImageButton btnPrevious;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;

    private MediaSessionManager mediaSessionManager;
    private MediaController currentController;
    private final OsmandApplication app;
    private String currentPackageName;

    // Loglardan tespit edilen Radyo paketi
    private static final String RADIO_PACKAGE = "com.hcn.autoradio";

    private final MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updatePlayPauseButton(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateTrackInfo(metadata);
        }
    };

    public MusicWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "music", "Muzik");
        this.app = app;
        this.order = 10;
    }

    private FrameLayout rootView;

    @NonNull
    @Override
    public View createView() {
        // Ana Tasiyici (FrameLayout) - Katmanli yapi icin
        rootView = new FrameLayout(context);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 1. Katman: Album Kapagi (Arka Plan)
        albumArtView = new ImageView(context);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        albumArtView.setAlpha(0.6f); // Hafif seffaflik
        rootView.addView(albumArtView);

        // 2. Katman: Karartma Overlay (Yazi okunurlugu icin)
        albumArtOverlay = new View(context);
        albumArtOverlay.setBackgroundColor(Color.parseColor("#99000000")); // %60 Siyah
        albumArtOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootView.addView(albumArtOverlay);

        // 3. Katman: Icerik (LinearLayout)
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setPadding(24, 24, 24, 24);
        contentLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Android Auto benzeri kart gorunumu icin boyut sinirlamasi
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        contentLayout.setLayoutParams(contentParams);

        // Sol: Album/App Icon
        appIconView = new ImageView(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(120, 120);
        iconParams.setMargins(0, 0, 24, 0);
        appIconView.setLayoutParams(iconParams);
        appIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // Default icon
        appIconView.setImageResource(android.R.drawable.ic_media_play);
        appIconView.setColorFilter(Color.WHITE);

        // Tiklama olaylari
        appIconView.setOnClickListener(v -> launchMusicApp());
        appIconView.setOnLongClickListener(v -> {
            showMusicAppSelector();
            return true;
        });
        contentLayout.addView(appIconView);

        // Orta: Metin Bilgileri
        LinearLayout textInfoLayout = new LinearLayout(context);
        textInfoLayout.setOrientation(LinearLayout.VERTICAL);
        textInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        titleText = new TextView(context);
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(18); // Daha buyuk baslik
        titleText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleText.setText("Muzik Secin");
        titleText.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        titleText.setSingleLine(true);
        titleText.setSelected(true); // Kayan yazi icin
        textInfoLayout.addView(titleText);

        artistText = new TextView(context);
        artistText.setTextColor(Color.LTGRAY);
        artistText.setTextSize(14);
        artistText.setText("Tiklayip baslatin");
        artistText.setSingleLine(true);
        artistText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textInfoLayout.addView(artistText);

        contentLayout.addView(textInfoLayout);

        // Sag: Kontroller
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER_VERTICAL);

        btnPrevious = createControlButton(android.R.drawable.ic_media_previous, 48);
        btnPrevious.setOnClickListener(v -> skipToPrevious());

        btnPlayPause = createControlButton(android.R.drawable.ic_media_play, 64); // Play butonu daha buyuk
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        btnNext = createControlButton(android.R.drawable.ic_media_next, 48);
        btnNext.setOnClickListener(v -> skipToNext());

        controlsLayout.addView(btnPrevious);
        controlsLayout.addView(btnPlayPause);
        controlsLayout.addView(btnNext);

        contentLayout.addView(controlsLayout);

        rootView.addView(contentLayout);

        return rootView;
    }

    private ImageButton createControlButton(int iconRes, int sizeDp) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setBackgroundResource(android.R.color.transparent); // Ripple eklenebilir
        button.setColorFilter(Color.WHITE);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // DP to PX
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);
        int paddingPx = (int) (8 * density);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
        params.setMargins(paddingPx, 0, paddingPx, 0);
        button.setLayoutParams(params);
        button.setPadding(4, 4, 4, 4);

        return button;
    }

    @Override
    public void update() {
        findActiveMediaController();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            findActiveMediaController();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (currentController != null) {
            currentController.unregisterCallback(mediaCallback);
            currentController = null;
        }
    }

    private void findActiveMediaController() {
        if (mediaSessionManager == null)
            return;

        try {
            ComponentName notificationListener = new ComponentName(
                    context,
                    "net.osmand.plus.carlauncher.MediaNotificationListener");

            List<MediaController> controllers = mediaSessionManager.getActiveSessions(notificationListener);

            if (controllers != null && !controllers.isEmpty()) {
                MediaController newController = controllers.get(0);
                if (currentController != newController) {
                    if (currentController != null) {
                        currentController.unregisterCallback(mediaCallback);
                    }
                    currentController = newController;
                    currentController.registerCallback(mediaCallback);

                    currentPackageName = currentController.getPackageName();
                    updateAppIcon(currentPackageName);
                    updateTrackInfo(currentController.getMetadata());
                    updatePlayPauseButton(currentController.getPlaybackState());
                }
            } else {
                // Controller yoksa resetle
                currentController = null;
                currentPackageName = null;
                resetUI();
            }
        } catch (SecurityException e) {
            // Izin yok durumu
            resetUI();
            if (titleText != null)
                titleText.setText("Bildirim Izni Gerekli");
            if (artistText != null)
                artistText.setText("Ayarlamak icin tiklayin");
            if (rootView != null) {
                rootView.setOnClickListener(v -> {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        context.startActivity(intent);
                    } catch (Exception ex) {
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAppIcon(String packageName) {
        if (packageName == null || appIconView == null)
            return;
        try {
            PackageManager pm = context.getPackageManager();
            Drawable icon = pm.getApplicationIcon(packageName);
            appIconView.post(() -> {
                appIconView.setImageDrawable(icon);
                appIconView.setColorFilter(null); // Orijinal ikon renkleri
            });
        } catch (Exception e) {
            appIconView.post(() -> {
                appIconView.setImageResource(android.R.drawable.ic_media_play);
                appIconView.setColorFilter(Color.WHITE);
            });
        }
    }

    private void updateTrackInfo(MediaMetadata metadata) {
        if (metadata == null)
            return;

        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        Bitmap albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);

        if (title == null)
            title = "Bilinmeyen Parca";
        if (artist == null)
            artist = "";

        final String finalTitle = title;
        final String finalArtist = artist;
        final Bitmap finalAlbumArt = albumArt;

        if (titleText != null)
            titleText.post(() -> titleText.setText(finalTitle));
        if (artistText != null)
            artistText.post(() -> artistText.setText(finalArtist));

        // Album Art guncelleme
        if (albumArtView != null) {
            albumArtView.post(() -> {
                if (finalAlbumArt != null) {
                    albumArtView.setImageBitmap(finalAlbumArt);
                    albumArtOverlay.setVisibility(View.VISIBLE);
                } else {
                    // Album kapagi yoksa varsayilan bir gradient/renk veya temizle
                    albumArtView.setImageDrawable(new ColorDrawable(Color.DKGRAY)); // Düz renk
                    albumArtOverlay.setVisibility(View.GONE);
                }
            });
        }
    }

    private void updatePlayPauseButton(PlaybackState state) {
        if (state == null || btnPlayPause == null)
            return;
        boolean isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
        int icon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        btnPlayPause.post(() -> btnPlayPause.setImageResource(icon));
    }

    private void resetUI() {
        if (titleText != null)
            titleText.post(() -> titleText.setText("Muzik Secin"));
        if (artistText != null)
            artistText.post(() -> artistText.setText("Tiklayip baslatin"));
        if (albumArtView != null)
            albumArtView.post(() -> albumArtView.setImageDrawable(null));
        if (appIconView != null) {
            appIconView.post(() -> {
                appIconView.setImageResource(android.R.drawable.ic_media_play);
                appIconView.setColorFilter(Color.WHITE);
            });
        }
    }

    // --- Actions ---

    private void togglePlayPause() {
        if (currentController != null) {
            PlaybackState state = currentController.getPlaybackState();
            if (state != null) {
                if (state.getState() == PlaybackState.STATE_PLAYING) {
                    currentController.getTransportControls().pause();
                } else {
                    currentController.getTransportControls().play();
                }
            }
        } else {
            showMusicAppSelector();
        }
    }

    private void skipToPrevious() {
        if (currentController != null)
            currentController.getTransportControls().skipToPrevious();
    }

    private void skipToNext() {
        if (currentController != null)
            currentController.getTransportControls().skipToNext();
    }

    private void launchMusicApp() {
        if (currentPackageName != null) {
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(currentPackageName);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } catch (Exception e) {
            }
        } else {
            showMusicAppSelector();
        }
    }

    // --- App Selector ---

    private void showMusicAppSelector() {
        List<MusicApp> musicApps = findMusicApps();
        if (musicApps.isEmpty())
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Muzik Uygulamasi Sec");

        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL); // Dikey liste
        listLayout.setPadding(16, 16, 16, 16);

        for (MusicApp app : musicApps) {
            LinearLayout itemLayout = new LinearLayout(context);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);
            itemLayout.setPadding(20, 24, 20, 24);
            itemLayout.setBackgroundResource(android.R.drawable.list_selector_background);

            ImageView iconView = new ImageView(context);
            iconView.setImageDrawable(app.icon);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(96, 96)); // Buyuk ikon
            itemLayout.addView(iconView);

            TextView nameView = new TextView(context);
            nameView.setText(app.name);
            nameView.setTextSize(18);
            nameView.setTextColor(Color.BLACK); // Dialog genellikle acik tema
            nameView.setPadding(32, 0, 0, 0);
            itemLayout.addView(nameView);

            itemLayout.setOnClickListener(v -> {
                launchMusicAppAndPlay(app.packageName);
                // builder ile olusturulan dialog kapatilacak
                // Not: Basit bir dialog mekanizmasi, custom dialog class'i olmadigi icin
                // builder.create().dismiss() referansi zor.
                // Pratikte bu view'in parent'i DialogView'dir ama erismek zor.
                // Cozum: Alert Dialog yerine basit bir IntentChooser veya PopupMenu
                // kullanilabilir ama
                // biz burada kullanicinin secmesini sagliyoruz. Dialog instance'ini yakalamak
                // lazim.
            });

            listLayout.addView(itemLayout);
        }

        // Dialog referansini yakalamak icin trick:
        // Ancak burada list item click icinde dialog.dismiss() diyebilmek icin final
        // ref lazim.
        // Yukaridaki kodda builder.create() yapip show() yapmak daha dogru.

        final AlertDialog dialog = builder.setView(listLayout).create();

        // Click listenerlari yeniden set edelim ki dialog referansini alabilsinler
        for (int i = 0; i < listLayout.getChildCount(); i++) {
            View item = listLayout.getChildAt(i);
            final int index = i;
            item.setOnClickListener(v -> {
                launchMusicAppAndPlay(musicApps.get(index).packageName);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void launchMusicAppAndPlay(String packageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                // Gecikmeli kontrol (uygulama acilinca session almak icin)
                if (rootView != null) {
                    rootView.postDelayed(this::findActiveMediaController, 2000);
                }
            }
        } catch (Exception e) {
        }
    }

    private List<MusicApp> findMusicApps() {
        List<MusicApp> musicApps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        String[] knownMusicApps = {
                RADIO_PACKAGE, // Loglardan bulunan Radyo
                "com.hcn.AutoMediaPlayer", // Loglardan bulunan Müzik Player
                "com.spotify.music",
                "com.google.android.youtube",
                "com.google.android.apps.youtube.music",
                "deezer.android.app",
                "com.amazon.mp3",
                "com.apple.android.music",
                "com.soundcloud.android",
                "com.aspiro.tidal"
        };

        for (String packageName : knownMusicApps) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                MusicApp musicApp = new MusicApp();
                musicApp.name = appInfo.loadLabel(pm).toString();
                musicApp.packageName = packageName;
                musicApp.icon = appInfo.loadIcon(pm);
                musicApps.add(musicApp);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return musicApps;
    }

    private static class MusicApp {
        String name;
        String packageName;
        Drawable icon;
    }
}
