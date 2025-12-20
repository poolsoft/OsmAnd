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

        // Fix visibility: Set fixed width because parent is WRAP_CONTENT
        // and we used MATCH_PARENT inside.
        // 220dp width, WRAP_CONTENT height (or fixed if needed)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(220),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        // Margins are now handled by WidgetManager
        rootFrame.setLayoutParams(params);

        // Initial setup for empty state (height might still be issue if empty)
        rootFrame.setMinimumHeight(dpToPx(100)); // Ensure min height similar to SpeedWidget

        // --- Album Art Background ---
        albumArtView = new ImageView(context);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setAlpha(0.6f); // Dimmed background
        RelativeLayout.LayoutParams artParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        // artParams.addRule(RelativeLayout.ALIGN_TOP, headerId); // No IDs yet
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

        // Short Click: Launch Selected App
        appIconView.setOnClickListener(v -> {
            String pkg = musicManager.getPreferredPackage();
            if (pkg != null) {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                }
            } else {
                showMusicAppPicker(); // If none selected, show picker
            }
        });

        // Long Click: Show Picker
        appIconView.setOnLongClickListener(v -> {
            showMusicAppPicker();
            return true;
        });

        headerLayout.addView(appIconView);

        // Track Title (Marquee)
        statusText = new TextView(context);
        statusText.setText("Muzik Secin");
        // ... (rest of View creation unchanged) ...
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(18);
        statusText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        statusText.setSingleLine(true);
        statusText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        statusText.setSelected(true);
        headerLayout.addView(statusText);

        // Artist Name (Small)
        this.artistText = new TextView(context);
        artistText.setId(View.generateViewId()); // Keep valid ID if needed
        artistText.setText("-");
        artistText.setTextColor(Color.LTGRAY);
        artistText.setTextSize(14);
        artistText.setSingleLine(true);
        artistText.setPadding(dpToPx(56) /* Icon + Margins */, 0, dpToPx(12), dpToPx(12));

        RelativeLayout.LayoutParams artistParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        artistParams.addRule(RelativeLayout.BELOW, headerId);
        rootFrame.addView(this.artistText, artistParams);

        // --- Controls (Prev, Play, Next) ---
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);

        // Prev
        ImageButton btnPrev = createControlButton(android.R.drawable.ic_media_previous, 32);
        btnPrev.setOnClickListener(v -> musicManager.previous());
        controlsLayout.addView(btnPrev);

        // Play/Pause
        this.btnPlay = createControlButton(android.R.drawable.ic_media_play, 40);
        this.btnPlay.setOnClickListener(v -> musicManager.playPause());
        controlsLayout.addView(this.btnPlay);

        // Next
        ImageButton btnNext = createControlButton(android.R.drawable.ic_media_next, 32);
        btnNext.setOnClickListener(v -> musicManager.next());
        controlsLayout.addView(btnNext);

        RelativeLayout.LayoutParams controlsParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        controlsParams.setMargins(0, 0, 0, dpToPx(8));
        rootFrame.addView(controlsLayout, controlsParams);

        // Click on widget opens Music Drawer
        rootFrame.setOnClickListener(v -> openMusicDrawer());

        rootView = rootFrame;
        return rootView;
    }

    // --- Helper Methods ---

    private void showMusicAppPicker() {
        // Uses version with filter=true
        new net.osmand.plus.carlauncher.dock.AppPickerDialog(context, true, (packageName, appName, icon) -> {
            // Save selection, DO NOT Launch immediately
            musicManager.setPreferredPackage(packageName);
            updateAppIcon(packageName);
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
        if (appIconView == null)
            return;

        String targetPackage = musicManager.getPreferredPackage();
        if (targetPackage == null) {
            targetPackage = packageName;
        }

        if (targetPackage == null)
            return;

        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(targetPackage);
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
