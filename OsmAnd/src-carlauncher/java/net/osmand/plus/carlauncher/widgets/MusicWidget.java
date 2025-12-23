package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.music.MusicManager;

/**
 * Modern Muzik Widget.
 * MusicManager ile entegre calisir.
 */
public class MusicWidget extends BaseWidget implements MusicManager.MusicUIListener {

    private TextView statusText;
    private TextView artistText;
    private ImageButton btnPlay;
    private ImageView albumArtView;
    private ImageView appIconView;

    private final MusicManager musicManager;

    public MusicWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "music", "Muzik");
        this.musicManager = MusicManager.getInstance(context);
        this.order = 3;
    }

    @NonNull
    @Override
    public View createView() {
        // Inflate XML
        View view = android.view.LayoutInflater.from(context).inflate(net.osmand.plus.R.layout.widget_music_modern,
                null);

        // --- Bind Views ---
        appIconView = view.findViewById(net.osmand.plus.R.id.widget_app_icon);
        statusText = view.findViewById(net.osmand.plus.R.id.widget_track_title);
        artistText = view.findViewById(net.osmand.plus.R.id.widget_track_artist);
        albumArtView = view.findViewById(net.osmand.plus.R.id.widget_album_art);

        ImageButton btnPrev = view.findViewById(net.osmand.plus.R.id.widget_btn_prev);
        ImageButton btnNext = view.findViewById(net.osmand.plus.R.id.widget_btn_next);
        btnPlay = view.findViewById(net.osmand.plus.R.id.widget_btn_play);

        // --- Setup Listeners ---
        appIconView.setOnClickListener(v -> {
            String pkg = musicManager.getPreferredPackage();
            if (pkg != null) {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null)
                    context.startActivity(launchIntent);
            } else {
                showMusicAppPicker();
            }
        });
        appIconView.setOnLongClickListener(v -> {
            showMusicAppPicker();
            return true;
        });

        if (btnPrev != null)
            btnPrev.setOnClickListener(v -> musicManager.skipToPrevious());
        if (btnPlay != null)
            btnPlay.setOnClickListener(v -> musicManager.togglePlayPause());
        if (btnNext != null)
            btnNext.setOnClickListener(v -> musicManager.skipToNext());

        // Open Music Drawer on content click
        View contentArea = view.findViewById(net.osmand.plus.R.id.widget_track_info);
        if (contentArea != null) {
            contentArea.setOnClickListener(v -> openMusicDrawer());
        }

        // Widget Layout Params for Grid
        view.setLayoutParams(new ViewGroup.LayoutParams(dpToPx(220), ViewGroup.LayoutParams.WRAP_CONTENT));

        // Long click propagation
        view.setOnLongClickListener(v -> false);

        rootView = view;
        return rootView;
    }

    // --- Helpers ---

    private void showMusicAppPicker() {
        new net.osmand.plus.carlauncher.dock.AppPickerDialog(context, true, (packageName, appName, icon) -> {
            musicManager.setPreferredPackage(packageName);
            updateAppIcon(packageName);
        }).show();
    }

    private void openMusicDrawer() {
        if (context instanceof net.osmand.plus.carlauncher.CarLauncherInterface) {
            ((net.osmand.plus.carlauncher.CarLauncherInterface) context).openMusicPlayer();
        } else {
            Intent intent = new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER");
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
        }
    }

    private void updateAppIcon(String packageName) {
        if (appIconView == null)
            return;
        String target = musicManager.getPreferredPackage();
        if (target == null)
            target = packageName;
        if (target == null)
            return;

        try {
            appIconView.setImageDrawable(context.getPackageManager().getApplicationIcon(target));
        } catch (Exception e) {
            appIconView.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private ImageButton createControlButton(int iconRes, int sizeDp) {
        ImageButton btn = new ImageButton(context);
        btn.setImageResource(iconRes);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btn.setColorFilter(Color.WHITE);
        int size = dpToPx(sizeDp);
        // Butonların tıklama alanı geniş olsun ama ikon sığsın
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dpToPx(8), 0, dpToPx(8), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    // --- Lifecycle ---

    @Override
    public void onStart() {
        super.onStart();
        musicManager.addListener(this);
        updateAppIcon(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        musicManager.removeListener(this);
    }

    @Override
    public void update() {
    }

    @Override
    public void onTrackChanged(String title, String artist, Bitmap albumArt, String packageName) {
        if (rootView != null) {
            rootView.post(() -> {
                if (statusText != null)
                    statusText.setText(title != null ? title : "Muzik Secin");
                if (artistText != null)
                    artistText.setText(artist != null ? artist : "");

                if (albumArt != null) {
                    albumArtView.setImageBitmap(albumArt);
                } else {
                    albumArtView.setImageResource(0);
                }
                updateAppIcon(packageName);
            });
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (btnPlay != null) {
            btnPlay.post(() -> {
                btnPlay.setImageResource(
                        isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            });
        }
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
    }
}