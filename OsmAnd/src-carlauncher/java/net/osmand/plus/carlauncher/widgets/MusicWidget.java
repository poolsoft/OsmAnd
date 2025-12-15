package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
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
        FrameLayout rootFrame = new FrameLayout(context);
        rootFrame.setPadding(0, 0, 0, 0);
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);
        rootFrame.setClipToOutline(true);

        // --- Album Art Background ---
        albumArtView = new ImageView(context);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setImageResource(android.R.drawable.ic_media_play); // Placeholder
        albumArtView.setAlpha(0.6f); // Hafif seffaflik
        rootFrame.addView(albumArtView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // --- Overlay (Dark Gradient) ---
        albumArtOverlay = new View(context);
        albumArtOverlay.setBackgroundColor(Color.parseColor("#88000000")); // Dark overlay
        rootFrame.addView(albumArtOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // --- Content Layout (Vertical) ---
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setPadding(16, 16, 16, 16);

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

        // Prev Button
        ImageButton btnPrev = createControlButton(android.R.drawable.ic_media_previous, 48);
        btnPrev.setOnClickListener(v -> musicManager.prev());
        controlsLayout.addView(btnPrev);

        // Play/Pause Button (Buyuk ve Yuvarlak)
        btnPlay = createControlButton(android.R.drawable.ic_media_play, 64);
        btnPlay.setOnClickListener(v -> musicManager.playPause());
        // Biraz margin verelim
        LinearLayout.LayoutParams playParams = (LinearLayout.LayoutParams) btnPlay.getLayoutParams();
        playParams.setMargins(24, 0, 24, 0);
        btnPlay.setLayoutParams(playParams);
        controlsLayout.addView(btnPlay);

        // Next Button
        ImageButton btnNext = createControlButton(android.R.drawable.ic_media_next, 48);
        btnNext.setOnClickListener(v -> musicManager.next());
        controlsLayout.addView(btnNext);

        contentLayout.addView(controlsLayout);

        rootFrame.addView(contentLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        // Root click -> Open Drawer? Or just play
        rootFrame.setOnClickListener(v -> {
            // Opsiyonel: Music Drawer acilabilir
        });

        rootView = rootFrame;
        return rootView;
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
    public void onTrackChanged(String title, String artist, Bitmap albumArt) {
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
