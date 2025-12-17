package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
        // Root Frame (Card)
        RelativeLayout rootFrame = new RelativeLayout(context);
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);
        rootFrame.setClipToOutline(true);

        // --- Album Art Background ---
        albumArtView = new ImageView(context);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setAlpha(0.6f); // Dimmed background
        RelativeLayout.LayoutParams artParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootFrame.addView(albumArtView, artParams);

        // --- Gradient Overlay (Better readability) ---
        albumArtOverlay = new View(context);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { Color.parseColor("#88000000"), Color.parseColor("#DD000000") });
        albumArtOverlay.setBackground(gradient);
        rootFrame.addView(albumArtOverlay, artParams);

        // --- Header Container (Icon + Title) ---
        int headerId = View.generateViewId();
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setId(headerId);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), 0);

        RelativeLayout.LayoutParams headerParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        rootFrame.addView(headerLayout, headerParams);

        // App Icon
        appIconView = new ImageView(context);
        int iconSize = dpToPx(24);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMargins(0, 0, dpToPx(12), 0);
        appIconView.setLayoutParams(iconParams);
        appIconView.setImageResource(android.R.drawable.ic_media_play);
        appIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        appIconView.setOnClickListener(v -> showMusicAppPicker()); // Dedicated click listener
        headerLayout.addView(appIconView);

        // Track Title (Marquee)
        statusText = new TextView(context);
        statusText.setText("Muzik Secin");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(18);
        statusText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        statusText.setSingleLine(true);
        statusText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        statusText.setSelected(true);
        headerLayout.addView(statusText);

        // --- Artist Text ---
        int artistId = View.generateViewId();
        artistText = new TextView(context);
        artistText.setId(artistId);
        artistText.setText("Sanatci Yok");
        artistText.setTextColor(Color.LTGRAY);
        artistText.setTextSize(14);
        artistText.setSingleLine(true);
        artistText.setGravity(Gravity.CENTER);

        RelativeLayout.LayoutParams artistParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        artistParams.addRule(RelativeLayout.BELOW, headerId);
        artistParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        artistParams.setMargins(0, dpToPx(4), 0, dpToPx(8));
        rootFrame.addView(artistText, artistParams);

        // --- Controls Container ---
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);

        RelativeLayout.LayoutParams controlsParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsParams.addRule(RelativeLayout.BELOW, artistId);
        controlsParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        // Ensure controls are pushed towards bottom/center
        controlsParams.setMargins(0, dpToPx(8), 0, dpToPx(12));
        rootFrame.addView(controlsLayout, controlsParams);

        ImageButton btnPrev = createControlButton(android.R.drawable.ic_media_previous, 48);
        btnPlay = createControlButton(android.R.drawable.ic_media_play, 64);
        ImageButton btnNext = createControlButton(android.R.drawable.ic_media_next, 48);

        // Add Listeners to Buttons
        btnPrev.setOnClickListener(v -> musicManager.prev());
        btnPlay.setOnClickListener(v -> musicManager.playPause());
        btnNext.setOnClickListener(v -> musicManager.next());

        controlsLayout.addView(btnPrev);

        // Play Button Margin
        LinearLayout.LayoutParams playBtnParams = (LinearLayout.LayoutParams) btnPlay.getLayoutParams();
        playBtnParams.setMargins(dpToPx(24), 0, dpToPx(24), 0);
        btnPlay.setLayoutParams(playBtnParams);

        controlsLayout.addView(btnPlay);
        controlsLayout.addView(btnNext);

        // --- Main Click Listener (For Drawer) ---
        // We set it on a background view to avoid conflict, or handle touches.
        // But framing setOnClickListener works if children don't consume it.
        // Buttons consume it. AppIcon consumes it.
        // Remaining area opens drawer.
        rootFrame.setOnClickListener(v -> openMusicDrawer());

        rootView = rootFrame;
        return rootView;
    }

    // --- Helper Methods ---

    private void showMusicAppPicker() {
        new net.osmand.plus.carlauncher.dock.AppPickerDialog(context, (packageName, appName, icon) -> {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                context.startActivity(intent);
                // Also update the icon immediately to reflect selection
                updateAppIcon(packageName);
            }
        }).show();
    }

    private void openMusicDrawer() {
        // Send Broadcast to MapActivity
        Intent intent = new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER");
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER"));
    }

    private void updateAppIcon(String packageName) {
        if (appIconView == null || packageName == null)
            return;
        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(packageName);
            appIconView.setImageDrawable(icon);
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
        // Important: Make clickable but focusable false for TV/Car nav if needed,
        // but here we want touch.
        int sizePx = dpToPx(sizeDp);
        btn.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));
        return btn;
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    // --- Lifecycle & Updates ---

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
                    albumArtView.setColorFilter(Color.parseColor("#44000000"));
                } else {
                    albumArtView.setImageResource(0); // Clear or placeholder
                    albumArtView.setColorFilter(Color.BLACK);
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
