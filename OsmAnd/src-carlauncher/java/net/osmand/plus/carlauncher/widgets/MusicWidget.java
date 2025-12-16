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
        // --- Root Container (Card) ---
        android.widget.RelativeLayout rootFrame = new android.widget.RelativeLayout(context);
        rootFrame.setPadding(0, 0, 0, 0);
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);
        rootFrame.setClipToOutline(true);

        // ID for content to anchor images
        int contentId = View.generateViewId();

        // --- Content Layout (Vertical) -> Source of Truth for Size ---
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setId(contentId);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setPadding(4, 4, 4, 4);

        // 1. Title (Song Name)
        statusText = new TextView(context);
        statusText.setText("Muzik Secin");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(18);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusText.setGravity(Gravity.CENTER);
        statusText.setSingleLine(true);
        statusText.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        statusText.setSelected(true); // Marquee icin
        contentLayout.addView(statusText);

        // 2. Artist Name
        artistText = new TextView(context);
        artistText.setText("");
        artistText.setTextColor(Color.LTGRAY);
        artistText.setTextSize(14);
        artistText.setGravity(Gravity.CENTER);
        artistText.setSingleLine(true);
        artistText.setPadding(0, 4, 0, 16);
        contentLayout.addView(artistText);

        // 3. Controls (Prev - Play - Next)
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);

        // Prev
        ImageButton btnPrev = createControlButton(android.R.drawable.ic_media_previous, 48);
        btnPrev.setOnClickListener(v -> musicManager.prev());
        controlsLayout.addView(btnPrev);

        // Play
        btnPlay = createControlButton(android.R.drawable.ic_media_play, 64);
        btnPlay.setOnClickListener(v -> musicManager.playPause());
        LinearLayout.LayoutParams playParams = (LinearLayout.LayoutParams) btnPlay.getLayoutParams();
        playParams.setMargins(24, 0, 24, 0);
        btnPlay.setLayoutParams(playParams);
        controlsLayout.addView(btnPlay);

        // Next
        ImageButton btnNext = createControlButton(android.R.drawable.ic_media_next, 48);
        btnNext.setOnClickListener(v -> musicManager.next());
        controlsLayout.addView(btnNext);

        contentLayout.addView(controlsLayout);

        // --- Album Art Background (Anchored to Content) ---
        albumArtView = new ImageView(context);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setImageResource(android.R.drawable.ic_media_play);
        albumArtView.setAlpha(0.6f);

        android.widget.RelativeLayout.LayoutParams artParams = new android.widget.RelativeLayout.LayoutParams(0, 0);
        artParams.addRule(android.widget.RelativeLayout.ALIGN_LEFT, contentId);
        artParams.addRule(android.widget.RelativeLayout.ALIGN_RIGHT, contentId);
        artParams.addRule(android.widget.RelativeLayout.ALIGN_TOP, contentId);
        artParams.addRule(android.widget.RelativeLayout.ALIGN_BOTTOM, contentId);
        rootFrame.addView(albumArtView, artParams);

        // --- Overlay (Dark Gradient) (Anchored to Content) ---
        albumArtOverlay = new View(context);
        albumArtOverlay.setBackgroundColor(Color.parseColor("#88000000"));
        rootFrame.addView(albumArtOverlay, artParams); // Same params

        // Add Content Last (to sit on top, but Z-order in RelativeLayout usually
        // depends on add order.
        // Wait, if Art is added first, it is behind. Correct.)
        android.widget.RelativeLayout.LayoutParams contentParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        contentParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
        rootFrame.addView(contentLayout, contentParams);

        // 4. App Icon (Top Start of Parent)
        appIconView = new ImageView(context);
        int iconSize = dpToPx(32);
        android.widget.RelativeLayout.LayoutParams iconParams = new android.widget.RelativeLayout.LayoutParams(iconSize,
                iconSize);
        iconParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
        iconParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        iconParams.setMargins(24, 24, 0, 0);
        appIconView.setLayoutParams(iconParams);
        appIconView.setImageResource(android.R.drawable.ic_media_play);
        appIconView.setOnClickListener(v -> openMusicDrawer());

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
