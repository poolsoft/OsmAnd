package net.osmand.plus.carlauncher.widgets;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Muzik widget - Medya kontrolu.
 * MediaSession API kullanarak aktif muzik uygulamasini kontrol eder.
 */
public class MusicWidget extends BaseWidget {

    private ImageView appIconView;
    private TextView titleText;
    private TextView artist Text;
    private ImageButton btnPrevious;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;

    private MediaSessionManager mediaSessionManager;
    private MediaController currentController;
    private final OsmandApplication app;
    private String currentPackageName;

    private final MediaController.Callback mediaCallback=new MediaController.Callback(){@Override public void onPlaybackStateChanged(PlaybackState state){updatePlayPauseButton(state);}

    @Override public void onMetadataChanged(android.media.MediaMetadata metadata){updateTrackInfo(metadata);}};

    public MusicWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "music", "Muzik");
        this.app = app;
        this.order = 10;
    }

    @NonNull
    @Override
    public View createView() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(16, 16, 16, 16);
        container.setBackgroundColor(0x22FFFFFF);

        // App icon (tiklanabilir)
        appIconView = new ImageView(context);
        appIconView.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        appIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        appIconView.setImageResource(android.R.drawable.ic_menu_music);
        appIconView.setPadding(0, 0, 0, 12);

        // Click -> Uygulamayi ac
        appIconView.setOnClickListener(v -> launchMusicApp());

        // Long click -> Muzik uygulamalari listesi
        appIconView.setOnLongClickListener(v -> {
            showMusicAppSelector();
            return true;
        });

        container.addView(appIconView);

        // Track title
        titleText = new TextView(context);
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setTextSize(16);
        titleText.setGravity(Gravity.CENTER);
        titleText.setText("Muzik calmiyor");
        titleText.setMaxLines(1);
        titleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        container.addView(titleText);

        // Artist
        artistText = new TextView(context);
        artistText.setTextColor(0xAAFFFFFF);
        artistText.setTextSize(12);
        artistText.setGravity(Gravity.CENTER);
        artistText.setText("");
        artistText.setMaxLines(1);
        artistText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        artistText.setPadding(0, 0, 0, 12);
        container.addView(artistText);

        // Control buttons
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);

        btnPrevious = createControlButton(android.R.drawable.ic_media_previous);
        btnPrevious.setOnClickListener(v -> skipToPrevious());

        btnPlayPause = createControlButton(android.R.drawable.ic_media_play);
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        btnNext = createControlButton(android.R.drawable.ic_media_next);
        btnNext.setOnClickListener(v -> skipToNext());

        controlsLayout.addView(btnPrevious);
        controlsLayout.addView(btnPlayPause);
        controlsLayout.addView(btnNext);

        container.addView(controlsLayout);

        rootView = container;
        return rootView;
    }

    private ImageButton createControlButton(int iconRes) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconRes);
        button.setBackgroundColor(0x00000000);
        button.setColorFilter(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(64, 64);
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);

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

                    // Package name al
                    currentPackageName = currentController.getPackageName();
                    updateAppIcon(currentPackageName);

                    updateTrackInfo(currentController.getMetadata());
                    updatePlayPauseButton(currentController.getPlaybackState());
                }
            } else {
                if (currentController != null) {
                    currentController.unregisterCallback(mediaCallback);
                    currentController = null;
                }
                currentPackageName = null;
                resetUI();
            }
        } catch (SecurityException e) {
            resetUI();
            if (titleText != null) {
                titleText.post(() -> titleText.setText("Izin gerekli"));
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
            appIconView.post(() -> appIconView.setImageDrawable(icon));
        } catch (Exception e) {
            appIconView.post(() -> appIconView.setImageResource(android.R.drawable.ic_menu_music));
        }
    }

    private void updateTrackInfo(android.media.MediaMetadata metadata) {
        if (metadata == null || titleText == null || artistText == null) {
            resetUI();
            return;
        }

        String title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST);

        if (title == null)
            title = "Bilinmeyen";
        if (artist == null)
            artist = "";

        final String finalTitle = title;
        final String finalArtist = artist;

        titleText.post(() -> titleText.setText(finalTitle));
        artistText.post(() -> artistText.setText(finalArtist));
    }

    private void updatePlayPauseButton(PlaybackState state) {
        if (state == null || btnPlayPause == null)
            return;

        boolean isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
        int icon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        btnPlayPause.post(() -> btnPlayPause.setImageResource(icon));
    }

    private void resetUI() {
        if (titleText != null) {
            titleText.post(() -> titleText.setText("Muzik calmiyor"));
        }
        if (artistText != null) {
            artistText.post(() -> artistText.setText(""));
        }
        if (btnPlayPause != null) {
            btnPlayPause.post(() -> btnPlayPause.setImageResource(android.R.drawable.ic_media_play));
        }
        if (appIconView != null) {
            appIconView.post(() -> appIconView.setImageResource(android.R.drawable.ic_menu_music));
        }
    }

    private void launchMusicApp() {
        if (currentPackageName != null) {
            try {
                Intent intent = context.getPackageManager().getLaunchIntentFor Package(currentPackageName);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }catch(

    Exception e)
    {
        e.printStackTrace();
    }
    }}

    private void showMusicAppSelector() {
        List<MusicApp> musicApps = findMusicApps();

        if (musicApps.isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Muzik Uygulamasi Sec");

        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(16, 16, 16, 16);

        for (MusicApp app : musicApps) {
            LinearLayout itemLayout = new LinearLayout(context);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);
            itemLayout.setPadding(12, 16, 12, 16);
            itemLayout.setClickable(true);
            itemLayout.setFocusable(true);
            itemLayout.setBackgroundResource(android.R.drawable.list_selector_background);

            ImageView iconView = new ImageView(context);
            iconView.setImageDrawable(app.icon);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            itemLayout.addView(iconView);

            TextView nameView = new TextView(context);
            nameView.setText(app.name);
            nameView.setTextColor(0xFFFFFFFF);
            nameView.setTextSize(16);
            nameView.setPadding(16, 0, 0, 0);

            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            itemLayout.addView(nameView, nameParams);

            final String packageName = app.packageName;
            itemLayout.setOnClickListener(v -> {
                launchMusicAppAndPlay(packageName);
                builder.create().dismiss();
            });

            listLayout.addView(itemLayout);
        }

        builder.setView(listLayout);
        builder.setNegativeButton("Iptal", null);
        builder.show();
    }

    private void launchMusicAppAndPlay(String packageName) {
        try {
            Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                rootView.postDelayed(() -> {
                    findActiveMediaController();
                    if (currentController != null) {
                        currentController.getTransportControls().play();
                    }
                }, 1500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<MusicApp> findMusicApps() {
        List<MusicApp> musicApps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        String[] knownMusicApps = {
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
                // Uygulama yuklu degil
            }
        }

        return musicApps;
    }

    private static class MusicApp {
        String name;
        String packageName;
        Drawable icon;
    }

    private void togglePlayPause() {
        if (currentController == null) {
            findActiveMediaController();
            return;
        }

        PlaybackState state = currentController.getPlaybackState();
        if (state != null) {
            if (state.getState() == PlaybackState.STATE_PLAYING) {
                currentController.getTransportControls().pause();
            } else {
                currentController.getTransportControls().play();
            }
        }
    }

    private void skipToPrevious() {
        if (currentController != null) {
            currentController.getTransportControls().skipToPrevious();
        }
    }

    private void skipToNext() {
        if (currentController != null) {
            currentController.getTransportControls().skipToNext();
        }
    }
}
