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
    // --- Visualizer ---
    private android.media.audiofx.Visualizer mVisualizer;
    private MusicVisualizerView visualizerView;
    private static final int PERMISSION_REQ_CODE = 202;

    @Override
    public View createView() {
        // ... (Existing implementation, copying relevant parts)
        View view = android.view.LayoutInflater.from(context).inflate(net.osmand.plus.R.layout.widget_music_modern,
                null);

        // --- Bind Views ---
        appIconView = view.findViewById(net.osmand.plus.R.id.widget_app_icon);
        statusText = view.findViewById(net.osmand.plus.R.id.widget_track_title);
        artistText = view.findViewById(net.osmand.plus.R.id.widget_track_artist);
        albumArtView = view.findViewById(net.osmand.plus.R.id.widget_album_art);
        visualizerView = view.findViewById(net.osmand.plus.R.id.widget_visualizer);

        ImageButton btnPrev = view.findViewById(net.osmand.plus.R.id.widget_btn_prev);
        ImageButton btnNext = view.findViewById(net.osmand.plus.R.id.widget_btn_next);
        btnPlay = view.findViewById(net.osmand.plus.R.id.widget_btn_play);

        // ... (Listeners same as before) ...
        appIconView.setOnClickListener(v -> {
            String pkg = musicManager.getPreferredPackage();
            if (pkg != null) {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    context.startActivity(launchIntent);
                }
            } else {
                showMusicAppPicker();
            }
        });
        appIconView.setOnLongClickListener(v -> {
            showMusicAppPicker();
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
        return rootView;
    }

    // ... (Helpers same as before) ...

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
        if (appIconView == null) return;
        String target = musicManager.getPreferredPackage();
        if (target == null) target = packageName;
        if (target == null) return;
        try {
            appIconView.setImageDrawable(context.getPackageManager().getApplicationIcon(target));
        } catch (Exception e) {
            appIconView.setImageResource(net.osmand.plus.R.drawable.ic_music_play);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    
    // ... (onSizeChanged same, skipping for brewity but logic retained implicitly if I don't touch it? No I MUST provide full replacement for the block I selected)
    // Actually I selected up to 302, which is end of file. I should paste onSizeChanged too.
    
    @Override
    protected void onSizeChanged(WidgetSize newSize) {
         if (rootView == null) return;
        
        boolean isSmall = (newSize == WidgetSize.SMALL);
        
        // Views (Redefine to be safe)
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
        
        // Visualizer Visibility (Hide on small to save space/clutter?)
        if (visualizerView != null) {
            visualizerView.setVisibility(isSmall ? View.INVISIBLE : View.VISIBLE);
        }

        // Constraints Logic (Same as before)
        if (rootView instanceof androidx.constraintlayout.widget.ConstraintLayout) {
            androidx.constraintlayout.widget.ConstraintLayout layout = (androidx.constraintlayout.widget.ConstraintLayout) rootView;
            androidx.constraintlayout.widget.ConstraintSet set = new androidx.constraintlayout.widget.ConstraintSet();
            set.clone(layout);
            
            if (isSmall) {
                set.clear(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.TOP);
                set.clear(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.BOTTOM);
                set.connect(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.TOP, 
                            net.osmand.plus.R.id.widget_app_icon, androidx.constraintlayout.widget.ConstraintSet.BOTTOM);
                set.connect(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 
                            net.osmand.plus.R.id.widget_btn_play, androidx.constraintlayout.widget.ConstraintSet.TOP);
                set.connect(net.osmand.plus.R.id.widget_btn_play, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 
                            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.BOTTOM);
                set.setMargin(net.osmand.plus.R.id.widget_btn_play, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, dpToPx(8));
            } else {
                set.clear(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.TOP);
                set.clear(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.BOTTOM);
                set.connect(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.TOP, 
                            net.osmand.plus.R.id.widget_app_icon, androidx.constraintlayout.widget.ConstraintSet.BOTTOM);
                set.connect(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 
                            net.osmand.plus.R.id.widget_track_artist, androidx.constraintlayout.widget.ConstraintSet.TOP);
                set.connect(net.osmand.plus.R.id.widget_track_artist, androidx.constraintlayout.widget.ConstraintSet.TOP, 
                             net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.BOTTOM);
                set.connect(net.osmand.plus.R.id.widget_track_artist, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 
                             net.osmand.plus.R.id.widget_btn_play, androidx.constraintlayout.widget.ConstraintSet.TOP);
                set.setVerticalChainStyle(net.osmand.plus.R.id.widget_track_title, androidx.constraintlayout.widget.ConstraintSet.CHAIN_PACKED);
                set.connect(net.osmand.plus.R.id.widget_btn_play, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 
                            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.BOTTOM);
                set.setMargin(net.osmand.plus.R.id.widget_btn_play, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, dpToPx(12));
            }
            set.applyTo(layout);
        }
        
        if (newSize == WidgetSize.LARGE) {
             if (title != null) title.setTextSize(18);
             if (artist != null) artist.setTextSize(15);
        } else {
             if (title != null) title.setTextSize(16);
             if (artist != null) artist.setTextSize(13);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        musicManager.addListener(this);
        updateAppIcon(null);
        startVisualizer();
    }

    @Override
    public void onStop() {
        super.onStop();
        musicManager.removeListener(this);
        stopVisualizer();
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
                updateAppIcon(packageName);
            });
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
        if (isPlaying) startVisualizer();
        else stopVisualizer();
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
        // Visualizer should work for both if possible. Don't stop it here.
        // If switching source, maybe restart visualizer to catch new session?
        stopVisualizer();
        startVisualizer();
    }

    // --- Visualizer Logic ---
    private void startVisualizer() {
        if (visualizerView == null) return;
        if (mVisualizer != null) return; // Already running
        
        // Allow external via Session 0 or Internal via specific session
        int sessionId = 0;
        
        if (musicManager.getInternalPlayer().isPlaying()) {
             sessionId = musicManager.getInternalPlayer().getAudioSessionId();
        } else {
             // External player (Global Mix)
             // Note: Session 0 requires RECORD_AUDIO permission (already checked below)
             sessionId = 0;
        }

        // Check Permissions
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
             // API 23+ Run-time Check (Activity contest required for dialog)
             if (context instanceof android.app.Activity) {
                 androidx.core.app.ActivityCompat.requestPermissions(
                         (android.app.Activity) context,
                         new String[]{android.Manifest.permission.RECORD_AUDIO},
                         PERMISSION_REQ_CODE
                 );
             }
             return;
        }

        try {
            mVisualizer = new android.media.audiofx.Visualizer(sessionId);
            mVisualizer.setCaptureSize(android.media.audiofx.Visualizer.getCaptureSizeRange()[1]);
            mVisualizer.setDataCaptureListener(new android.media.audiofx.Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(android.media.audiofx.Visualizer visualizer, byte[] waveform, int samplingRate) {
                }

                @Override
                public void onFftDataCapture(android.media.audiofx.Visualizer visualizer, byte[] fft, int samplingRate) {
                    if (visualizerView != null) {
                        visualizerView.updateVisualizer(fft);
                    }
                }
            }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 2, false, true);
            
            mVisualizer.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: If session 0 fails or system prevents it, try to clear
            if (mVisualizer != null) {
                mVisualizer.release();
                mVisualizer = null;
            }
        }
    }

    private void stopVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.release();
            mVisualizer = null;
        }
        if (visualizerView != null) {
            visualizerView.clear();
        }
    }
}