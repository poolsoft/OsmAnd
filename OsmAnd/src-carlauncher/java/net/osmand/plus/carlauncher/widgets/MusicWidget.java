package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import net.osmand.plus.OsmandApplication;
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
    private View albumArtOverlay;
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

        android.widget.RelativeLayout rootFrame = new android.widget.RelativeLayout(context);
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);
        rootFrame.setClipToOutline(true);

        int contentId = View.generateViewId();

        // --- Album Art ---
        albumArtView = new ImageView(context);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setAlpha(1.0f);

        android.widget.RelativeLayout.LayoutParams artParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);

        rootFrame.addView(albumArtView, artParams);

        // --- Gradient Overlay ---
        albumArtOverlay = new View(context);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        Color.parseColor("#55000000"),
                        Color.parseColor("#AA000000")
                });
        albumArtOverlay.setBackground(gradient);
        rootFrame.addView(albumArtOverlay, artParams);

        // --- Content ---
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setId(contentId);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setPadding(24, 24, 24, 24);

        android.widget.RelativeLayout.LayoutParams contentParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        contentParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);

        // --- App Icon ---
        appIconView = new ImageView(context);
        int iconSize = dpToPx(28);

        android.widget.RelativeLayout.LayoutParams iconParams = new android.widget.RelativeLayout.LayoutParams(iconSize,
                iconSize);
        iconParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        iconParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
        iconParams.setMargins(20, 20, 0, 0);

        appIconView.setImageResource(android.R.drawable.ic_media_play);
        appIconView.setAlpha(0.9f);
        appIconView.setOnClickListener(v -> openMusicDrawer());

        // --- Title ---
        statusText = new TextView(context);
        statusText.setText("Muzik Secin");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(20);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusText.setLetterSpacing(0.03f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setSingleLine(true);
        statusText.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        statusText.setSelected(true);

        // --- Artist ---
        artistText = new TextView(context);
        artistText.setTextColor(Color.LTGRAY);
        artistText.setTextSize(14);
        artistText.setAlpha(0.75f);
        artistText.setGravity(Gravity.CENTER);
        artistText.setPadding(0, 8, 0, 24);

        // --- Controls ---
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);

        ImageButton btnPrev = createControlButton(android.R.drawable.ic_media_previous, 56);
        ImageButton btnPlay = createControlButton(android.R.drawable.ic_media_play, 72);
        ImageButton btnNext = createControlButton(android.R.drawable.ic_media_next, 56);

        LinearLayout.LayoutParams playParams = (LinearLayout.LayoutParams) btnPlay.getLayoutParams();
        playParams.setMargins(32, 0, 32, 0);
        btnPlay.setLayoutParams(playParams);

        controlsLayout.addView(btnPrev);
        controlsLayout.addView(btnPlay);
        controlsLayout.addView(btnNext);

        // --- Build hierarchy ---
        contentLayout.addView(statusText);
        contentLayout.addView(artistText);
        contentLayout.addView(controlsLayout);

        rootFrame.addView(contentLayout, contentParams);
        rootFrame.addView(appIconView);

        rootFrame.setOnClickListener(v -> openMusicDrawer());

        rootView = rootFrame;
        return rootView;
    }

    private void openMusicDrawer() {
        Intent intent = new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER");
        intent.setPackage(context.getPackageName()); // Explicit for Android 14
        context.sendBroadcast(intent);

        // Also send LocalBroadcast for safety
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER"));
    }

    // Geriye donuk uyumluluk veya internal update icin gerekli olabilir
    private void updateAppIcon(String packageName) {
        if (appIconView == null || packageName == null)
            return;
        try {
            android.graphics.drawable.Drawable icon = context.getPackageManager().getApplicationIcon(packageName);
            appIconView.setImageDrawable(icon);
        } catch (Exception e) {
            appIconView.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private ImageButton createControlButton(int iconRes, int sizeDp) {
        ImageButton btn = new ImageButton(context);
        btn.setImageResource(iconRes);
        btn.setBackgroundColor(Color.TRANSPARENT); // Arka plan yok veya shape olabilir
        btn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btn.setColorFilter(Color.WHITE);
        int sizePx = dpToPx(sizeDp);
        btn.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));
        return btn;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onStart() {
        super.onStart();
        musicManager.addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        musicManager.removeListener(this);
    }

    @Override
    public void update() {
        // Listener uzerinden guncelleniyor
    }

    // --- MusicUIListener ---

    @Override
    public void onTrackChanged(String title, String artist, Bitmap albumArt, String packageName) {
        if (rootView != null) {
            rootView.post(() -> {
                if (statusText != null)
                    statusText.setText(title != null ? title : "Bilinmiyor");
                if (artistText != null)
                    artistText.setText(artist != null ? artist : "");

                if (albumArt != null) {
                    albumArtView.setImageBitmap(albumArt);
                    albumArtView.setColorFilter(Color.parseColor("#44000000")); // Karartma
                } else {
                    albumArtView.setImageResource(android.R.drawable.ic_menu_gallery); // Varsayilan bir resim
                    albumArtView.setColorFilter(Color.parseColor("#AA000000")); // Cok Siyah
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
        // Kaynak degistiginde UI'da bir sey gostermek istersek
    }
}
