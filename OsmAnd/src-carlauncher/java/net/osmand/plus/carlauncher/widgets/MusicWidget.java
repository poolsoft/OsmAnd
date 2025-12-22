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
        // --- Root Frame ---
        RelativeLayout rootFrame = new RelativeLayout(context);
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);
        rootFrame.setClipToOutline(true);

        // Sabit Genişlik (Grid yapısı için)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(220),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rootFrame.setLayoutParams(params);
        rootFrame.setMinimumHeight(dpToPx(120)); // Yüksekliği butonlar sığsın diye biraz arttırdım

        // --- Album Art ---
        albumArtView = new ImageView(context);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setAlpha(0.6f);
        RelativeLayout.LayoutParams artParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootFrame.addView(albumArtView, artParams);

        // --- Gradient Overlay ---
        View albumArtOverlay = new View(context);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { Color.parseColor("#88000000"), Color.parseColor("#FF000000") });
        albumArtOverlay.setBackground(gradient);
        rootFrame.addView(albumArtOverlay, artParams);

        // --- Header (Icon + Title) ---
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

        // Listeners
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
        headerLayout.addView(appIconView);

        // Title Text
        statusText = new TextView(context);
        statusText.setText("Muzik Secin");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(18);
        statusText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        statusText.setSingleLine(true);
        statusText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        statusText.setSelected(true);
        headerLayout.addView(statusText);

        // --- Artist Name ---
        artistText = new TextView(context);
        artistText.setId(View.generateViewId());
        artistText.setText("-");
        artistText.setTextColor(Color.LTGRAY);
        artistText.setTextSize(14);
        artistText.setSingleLine(true);
        artistText.setEllipsize(TextUtils.TruncateAt.END);
        artistText.setPadding(dpToPx(48) /* Icon hizası */, 0, dpToPx(12), 0);

        RelativeLayout.LayoutParams artistParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        artistParams.addRule(RelativeLayout.BELOW, headerId);
        artistParams.setMargins(0, dpToPx(4), 0, 0);
        rootFrame.addView(artistText, artistParams);

        // --- Controls ---
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);

        // !!! NOT: MusicManager'daki metot isimlerinizi kontrol edip burayı güncelleyin
        // !!!
        // Örn: previous() mu skipToPrevious() mu?

        // Prev
        ImageButton btnPrev = createControlButton(android.R.drawable.ic_media_previous, 48);
        btnPrev.setOnClickListener(v -> musicManager.skipToPrevious()); // Veya musicManager.previous()
        controlsLayout.addView(btnPrev);

        // Play/Pause
        btnPlay = createControlButton(android.R.drawable.ic_media_play, 56);
        btnPlay.setOnClickListener(v -> musicManager.togglePlayPause()); // Veya musicManager.playPause()
        controlsLayout.addView(btnPlay);

        // Next
        ImageButton btnNext = createControlButton(android.R.drawable.ic_media_next, 48);
        btnNext.setOnClickListener(v -> musicManager.skipToNext()); // Veya musicManager.next()
        controlsLayout.addView(btnNext);

        RelativeLayout.LayoutParams controlsParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        controlsParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        rootFrame.addView(controlsLayout, controlsParams);

        // Drawer Opener
        rootFrame.setOnClickListener(v -> openMusicDrawer());
        rootFrame.setOnLongClickListener(v -> false); // Allow parent to handle long click (Widget Management)

        rootView = rootFrame;
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
        Intent intent = new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER");
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
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