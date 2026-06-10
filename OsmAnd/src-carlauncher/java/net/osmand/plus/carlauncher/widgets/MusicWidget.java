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
import net.osmand.plus.carlauncher.music.MusicManager;

/**
 * Modern Muzik Widget.
 * MusicManager ile entegre calisir.
 */
public class MusicWidget extends BaseWidget implements MusicManager.MusicUIListener, MusicManager.MusicVisualizerListener {

    private TextView statusText;
    private TextView artistText;
    private ImageButton btnPlay;
    private ImageView albumArtView;
    private ImageView appIconView;
    private MusicVisualizerView visualizerView;

    private final MusicManager musicManager;

    public MusicWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "music", "Muzik");
        this.musicManager = MusicManager.getInstance(context);
        this.order = 3;
    }

    @Override
    public View createView() {
        View view = android.view.LayoutInflater.from(context).inflate(net.osmand.plus.R.layout.widget_music_modern, null);

        // --- Bind Views ---
        appIconView = view.findViewById(net.osmand.plus.R.id.widget_app_icon);
        statusText = view.findViewById(net.osmand.plus.R.id.widget_track_title);
        artistText = view.findViewById(net.osmand.plus.R.id.widget_track_artist);
        albumArtView = view.findViewById(net.osmand.plus.R.id.widget_album_art);
        visualizerView = view.findViewById(net.osmand.plus.R.id.widget_visualizer);

        ImageButton btnPrev = view.findViewById(net.osmand.plus.R.id.widget_btn_prev);
        ImageButton btnNext = view.findViewById(net.osmand.plus.R.id.widget_btn_next);
        btnPlay = view.findViewById(net.osmand.plus.R.id.widget_btn_play);

        // --- Listeners ---
        appIconView.setOnClickListener(v -> {
            String pkg = musicManager.getPreferredPackage();
            if (pkg != null) {
                Intent launchIntent = v.getContext().getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    v.getContext().startActivity(launchIntent);
                }
            } else {
                showMusicAppPicker(v);
            }
        });
        appIconView.setOnLongClickListener(v -> {
            showMusicAppPicker(v);
            return true;
        });

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> musicManager.skipToPrevious());
            btnPrev.setImageResource(net.osmand.plus.R.drawable.ic_music_prev);
        }
        if (btnPlay != null)
            btnPlay.setOnClickListener(v -> musicManager.togglePlayPause());
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> musicManager.skipToNext());
            btnNext.setImageResource(net.osmand.plus.R.drawable.ic_music_next);
        }

        View contentArea = view.findViewById(net.osmand.plus.R.id.widget_track_title);
        if (contentArea != null) {
            contentArea.setOnClickListener(v -> openMusicDrawer());
        }

        view.setLayoutParams(new ViewGroup.LayoutParams(dpToPx(220), ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setOnLongClickListener(v -> false);
        view.setOnClickListener(v -> openMusicDrawer());

        rootView = view;
        // Important: Force apply size constraints now that view is created
        onSizeChanged(this.size);
        return rootView;
    }

    private void showMusicAppPicker(View v) {
        Context activityContext = v.getContext();
        if (!musicManager.checkNotificationAccess()) {
             android.widget.Toast.makeText(activityContext, "Lütfen 'Bildirim Erişimi' iznini verin.", android.widget.Toast.LENGTH_LONG).show();
             try {
                activityContext.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
             } catch (Exception e) {
                  android.widget.Toast.makeText(activityContext, "Ayarlar açılamadı, manuel gidin.", android.widget.Toast.LENGTH_SHORT).show();
             }
             return;
        }

        new net.osmand.plus.carlauncher.dock.AppPickerDialog(activityContext, true, (packageName, appName, icon) -> {
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
        if (appIconView == null) return;
        String target = musicManager.getPreferredPackage();
        if (target == null) target = packageName;
        if (target == null) return;
        // Dahili oynatici icin uygulamanin kendi ikonunu goster
        if ("usage.internal.player".equals(target)) {
            target = context.getPackageName();
        }
        try {
            appIconView.setImageDrawable(context.getPackageManager().getApplicationIcon(target));
        } catch (Exception e) {
            appIconView.setImageResource(net.osmand.plus.R.drawable.ic_music_play);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSizeChanged(WidgetSize newSize) {
        if (rootView == null) return;

        boolean isSmall = (newSize == WidgetSize.SMALL);

        // Views
        View btnPrev = rootView.findViewById(net.osmand.plus.R.id.widget_btn_prev);
        View btnNext = rootView.findViewById(net.osmand.plus.R.id.widget_btn_next);
        TextView artist = rootView.findViewById(net.osmand.plus.R.id.widget_track_artist);
        TextView title = rootView.findViewById(net.osmand.plus.R.id.widget_track_title);
        View btnPlay = rootView.findViewById(net.osmand.plus.R.id.widget_btn_play);

        // Visibility
        int visibility = isSmall ? View.GONE : View.VISIBLE;
        if (btnPrev != null) btnPrev.setVisibility(visibility);
        if (btnNext != null) btnNext.setVisibility(visibility);
        if (artist != null) artist.setVisibility(visibility);

        // Visualizer Visibility
        if (visualizerView != null) {
            visualizerView.setVisibility(isSmall ? View.INVISIBLE : View.VISIBLE);
        }

        // --- Music Layout is now handled by XML (widget_music_modern.xml) ---
        // Programmatic constraints removed to avoid portrait/landscape conflicts.

        if (newSize == WidgetSize.LARGE) {
            if (title != null) title.setTextSize(20);
            if (artist != null) artist.setTextSize(16);
            if (albumArtView != null) {
                ViewGroup.LayoutParams lp = albumArtView.getLayoutParams();
                lp.width = dpToPx(80);
                lp.height = dpToPx(80);
                albumArtView.setLayoutParams(lp);
            }
        } else {
            if (title != null) title.setTextSize(16);
            if (artist != null) artist.setTextSize(13);
            if (albumArtView != null) {
                ViewGroup.LayoutParams lp = albumArtView.getLayoutParams();
                lp.width = dpToPx(60);
                lp.height = dpToPx(60);
                albumArtView.setLayoutParams(lp);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        musicManager.addListener(this);
        musicManager.addVisualizerListener(this);
        updateAppIcon(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        musicManager.removeListener(this);
        musicManager.removeVisualizerListener(this);
    }

    @Override
    public void update() {
    }

    @Override
    public void onTrackChanged(String title, String artist, Bitmap albumArt, String packageName) {
        if (rootView != null) {
            rootView.post(() -> {
                if (statusText != null)
                    statusText.setText(title != null ? title : "Müzik Seçin");
                if (artistText != null)
                    artistText.setText(artist != null ? artist : "");

                if (albumArtView != null) {
                    if (albumArt != null) {
                        albumArtView.setImageBitmap(albumArt);
                    } else {
                        albumArtView.setImageResource(net.osmand.plus.R.drawable.ic_default_album_art);
                    }
                }

                if (visualizerView != null) {
                    if (albumArt != null) {
                        int color = getDominantColor(albumArt);
                        visualizerView.setDominantColor(color);
                    } else {
                        visualizerView.setDominantColor(0);
                    }
                }

                updateAppIcon(packageName);
            });
        }
    }

    private int getDominantColor(Bitmap bitmap) {
        if (bitmap == null) return 0xFF00FFFF;
        try {
            Bitmap small = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
            int color = small.getPixel(0, 0);
            small.recycle();
            return color;
        } catch (Exception e) {
            return 0xFF00FFFF;
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (btnPlay != null) {
            btnPlay.post(() -> {
                btnPlay.setImageResource(
                        isPlaying ? net.osmand.plus.R.drawable.ic_music_pause : net.osmand.plus.R.drawable.ic_music_play);
            });
        }
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
        // Handled by MusicManager centralization
    }
    
    // --- Music Visualizer Listener ---
    @Override
    public void onFftDataCapture(byte[] fft) {
        if (visualizerView != null) {
            visualizerView.updateVisualizer(fft);
        }
    }
}