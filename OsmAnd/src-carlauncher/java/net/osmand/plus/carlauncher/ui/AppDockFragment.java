package net.osmand.plus.carlauncher.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.carlauncher.dock.AppDockAdapter;
import net.osmand.plus.carlauncher.dock.AppDockManager;
import net.osmand.plus.carlauncher.dock.AppPickerDialog;
import net.osmand.plus.carlauncher.CarLauncherInterface;
import net.osmand.plus.carlauncher.dock.AppShortcut;
import net.osmand.plus.carlauncher.dock.LaunchMode;
import net.osmand.plus.carlauncher.overlay.OverlayWindowManager;

/**
 * App Dock fragment.
 * Uygulama kisayollarini gosterir ve yonetir.
 * Yatay (altta) veya dikey (solda) olabilir.
 */
public class AppDockFragment extends Fragment implements AppDockAdapter.OnShortcutListener {

    public static final String TAG = "AppDockFragment";
    private static final String PREFS_NAME = "app_dock_settings";
    private static final String KEY_ORIENTATION = "orientation";
    private static final int ORIENTATION_HORIZONTAL = LinearLayoutManager.HORIZONTAL;
    private static final int ORIENTATION_VERTICAL = LinearLayoutManager.VERTICAL;

    private RecyclerView recyclerView;
    private ImageButton addButton;
    private ImageButton orientationButton;
    private AppDockAdapter adapter;
    private AppDockManager dockManager;
    private OverlayWindowManager overlayManager;
    private SharedPreferences prefs;
    private boolean isEditMode = false;
    private int currentOrientation = ORIENTATION_HORIZONTAL; // Varsayilan yatay

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            dockManager = new AppDockManager(getContext());
            dockManager.loadShortcuts();

            overlayManager = new OverlayWindowManager(getContext());

            prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentOrientation = prefs.getInt(KEY_ORIENTATION, ORIENTATION_HORIZONTAL);
        }
    }

    private OnAppDockListener listener;
    private ImageButton menuButton;
    private ImageButton layoutButton;
    private ImageButton appListButton;

    // New Views
    private TextView clockView;
    private android.os.Handler clockHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable clockRunnable;

    // Mini Music Player
    private LinearLayout miniMusicContainer;
    private TextView miniMusicTitle;
    private ImageButton miniBtnPlay;
    private ImageView miniMusicIcon;
    private MediaSessionManager mediaSessionManager;
    private MediaController currentController;
    private MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updateMiniPlayer(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateMiniMetadata(metadata);
        }
    };

    public interface OnAppDockListener {
        void onLayoutToggle();

        void onAppDrawerOpen();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAppDockListener) {
            listener = (OnAppDockListener) context;
        } else {
            // Log warning but don't crash, helpful for testing
            android.util.Log.w(TAG, "Host activity does not implement OnAppDockListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Force Horizontal Layout for Bottom Dock
        View root = inflater.inflate(net.osmand.plus.R.layout.fragment_app_dock, container, false);

        // Find Views
        appListButton = root.findViewById(net.osmand.plus.R.id.btn_app_list);
        layoutButton = root.findViewById(net.osmand.plus.R.id.btn_layout_toggle);
        recyclerView = root.findViewById(net.osmand.plus.R.id.dock_recycler);

        // Setup New Views
        clockView = root.findViewById(net.osmand.plus.R.id.dock_clock);
        miniMusicContainer = root.findViewById(net.osmand.plus.R.id.mini_music_container);
        miniMusicTitle = root.findViewById(net.osmand.plus.R.id.mini_music_title);
        miniBtnPlay = root.findViewById(net.osmand.plus.R.id.mini_btn_play);
        miniMusicIcon = root.findViewById(net.osmand.plus.R.id.mini_music_icon);

        if (miniBtnPlay != null) {
            miniBtnPlay.setOnClickListener(v -> togglePlayPause());
        }

        // Start Clock
        startClock();

        // Start Music Listener
        if (getContext() != null) {
            mediaSessionManager = (MediaSessionManager) getContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
            findActiveMediaController();
        }

        // Setup Buttons
        appListButton.setOnClickListener(v -> {
            if (listener != null)
                listener.onAppDrawerOpen();
        });

        layoutButton.setOnClickListener(v -> {
            if (listener != null)
                listener.onLayoutToggle();
        });

        // Setup RecyclerView (Force Horizontal)
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Long Press on Root & Recycler to add apps
        View.OnLongClickListener longClickListener = v -> {
            showAppPickerDialog();
            return true;
        };
        root.setOnLongClickListener(longClickListener);
        recyclerView.setOnLongClickListener(longClickListener); // Ensures empty space in recycler triggers it too

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Adapter setup
        adapter = new AppDockAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);

        // Kisayollari yukle
        if (dockManager != null) {
            adapter.setShortcuts(dockManager.getShortcuts());
        }
    }

    @Override
    public void onShortcutClick(AppShortcut shortcut) {
        if (getContext() == null)
            return;

        LaunchMode mode = shortcut.getLaunchMode();
        String packageName = shortcut.getPackageName();

        try {
            switch (mode) {
                case FULL_SCREEN:
                    launchAppStandard(packageName);
                    break;

                case OVERLAY:
                    if (overlayManager != null) {
                        try {
                            overlayManager.showOverlay(packageName);
                        } catch (Exception e) {
                            // Fallback to standard
                            launchAppStandard(packageName);
                        }
                    }
                    break;

                case SPLIT_SCREEN:
                    launchAppSplitScreen(packageName);
                    break;

                case WIDGET_ONLY:
                    // Sadece widget, uygulama acilmaz
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching app: " + packageName, e);
        }
    }

    private void launchAppStandard(String packageName) {
        try {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Launch failed", e);
        }
    }

    private void launchAppSplitScreen(String packageName) {
        try {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT); // Split screen flag
                }
                getContext().startActivity(intent);
            }
        } catch (Exception e) {
            launchAppStandard(packageName); // Fallback
        }
    }

    @Override
    public void onShortcutLongClick(AppShortcut shortcut) {
        onRemoveClick(shortcut);
    }

    public void onRemoveClick(AppShortcut shortcut) {
        new AlertDialog.Builder(getContext())
                .setTitle("Kisayol Kaldir")
                .setMessage(shortcut.getAppName() + " kisayolunu kaldirmak istiyor musunuz?")
                .setPositiveButton("Kaldir", (dialog, which) -> {
                    if (dockManager != null) {
                        dockManager.removeShortcut(shortcut);
                        adapter.setShortcuts(dockManager.getShortcuts());
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void showAppPickerDialog() {
        if (getContext() == null || dockManager == null)
            return;

        AppPickerDialog dialog = new AppPickerDialog(getContext(), (packageName, appName, icon) -> {
            // App secildiginde dock'a ekle
            AppShortcut newShortcut = new AppShortcut(packageName, appName, icon, dockManager.getShortcuts().size(),
                    LaunchMode.FULL_SCREEN);
            boolean added = dockManager.addShortcut(newShortcut);

            if (added) {
                // Adapteri guncelle
                if (adapter != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setShortcuts(dockManager.getShortcuts());
                        adapter.notifyDataSetChanged(); // Explicit refresh
                    });
                }
                android.widget.Toast.makeText(getContext(), appName + " eklendi", android.widget.Toast.LENGTH_SHORT)
                        .show();
            } else {
                android.widget.Toast
                        .makeText(getContext(), "Limit asildi veya zaten var", android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
        });
        dialog.show();
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (adapter != null) {
            adapter.setEditMode(isEditMode);
        }

        if (addButton != null) {
            addButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
        if (orientationButton != null) {
            orientationButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }

        if (isEditMode && getView() != null) {
            getView().postDelayed(() -> {
                if (isEditMode) {
                    toggleEditMode();
                }
            }, 10000);
        }
    }

    private void toggleOrientation() {
        currentOrientation = (currentOrientation == ORIENTATION_HORIZONTAL) ? ORIENTATION_VERTICAL
                : ORIENTATION_HORIZONTAL;

        if (prefs != null) {
            prefs.edit().putInt(KEY_ORIENTATION, currentOrientation).apply();
        }

        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction()
                    .detach(this)
                    .attach(this)
                    .commitAllowingStateLoss();
        }
    }

    private void showAppPicker() {
        if (getContext() == null || dockManager == null)
            return;

        if (!dockManager.canAddMore()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Limit")
                    .setMessage("Maksimum " + dockManager.getMaxShortcuts() + " kisayol ekleyebilirsiniz.")
                    .setPositiveButton("Tamam", null)
                    .show();
            return;
        }

        AppPickerDialog dialog = new AppPickerDialog(
                getContext(),
                (packageName, appName, icon) -> {
                    showLaunchModeSelector(packageName, appName, icon);
                });
        dialog.show();
    }

    private void showLaunchModeSelector(String packageName, String appName, Drawable icon) {
        LaunchMode[] modes = LaunchMode.values();
        String[] modeNames = new String[modes.length];

        for (int i = 0; i < modes.length; i++) {
            modeNames[i] = modes[i].getDisplayName();
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Acilis Modu Sec: " + appName)
                .setItems(modeNames, (dialog, which) -> {
                    LaunchMode selectedMode = modes[which];
                    addShortcut(packageName, appName, icon, selectedMode);
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void addShortcut(String packageName, String appName, Drawable icon, LaunchMode launchMode) {
        if (dockManager == null)
            return;

        int order = dockManager.getShortcuts().size();
        AppShortcut shortcut = new AppShortcut(packageName, appName, icon, order, launchMode);

        if (dockManager.addShortcut(shortcut)) {
            adapter.setShortcuts(dockManager.getShortcuts());
        } else {
            if (getContext() != null) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Hata")
                        .setMessage("Kisayol eklenemedi. Zaten mevcut veya limit asildi.")
                        .setPositiveButton("Tamam", null)
                        .show();
            }
        }
    }

    private void openAppDrawer() {
        if (getActivity() instanceof CarLauncherInterface) {
            ((CarLauncherInterface) getActivity()).openAppDrawer();
        }
    }

    // --- Clock Logic ---
    private void startClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (clockView != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm",
                            java.util.Locale.getDefault());
                    clockView.setText(sdf.format(new java.util.Date()));
                }
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    // --- Mini Music Player Logic ---
    private void findActiveMediaController() {
        if (getContext() == null || mediaSessionManager == null)
            return;
        try {
            // Use same notification listener component as MusicWidget
            android.content.ComponentName notificationListener = new android.content.ComponentName(getContext(),
                    "net.osmand.plus.carlauncher.MediaNotificationListener");
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(notificationListener);
            if (controllers != null && !controllers.isEmpty()) {
                MediaController newController = controllers.get(0);
                if (currentController != newController) {
                    if (currentController != null)
                        currentController.unregisterCallback(mediaCallback);
                    currentController = newController;
                    currentController.registerCallback(mediaCallback);
                    updateMiniMetadata(currentController.getMetadata());
                    updateMiniPlayer(currentController.getPlaybackState());

                    // Update Icon from Package
                    try {
                        Drawable icon = getContext().getPackageManager()
                                .getApplicationIcon(currentController.getPackageName());
                        if (miniMusicIcon != null)
                            miniMusicIcon.setImageDrawable(icon);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void updateMiniPlayer(PlaybackState state) {
        if (state == null || miniBtnPlay == null)
            return;
        boolean isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
        miniBtnPlay.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void updateMiniMetadata(MediaMetadata metadata) {
        if (metadata == null || miniMusicTitle == null)
            return;
        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        miniMusicTitle.setText(title != null ? title : "Muzik");
    }

    private void togglePlayPause() {
        if (currentController != null) {
            PlaybackState state = currentController.getPlaybackState();
            if (state != null) {
                if (state.getState() == PlaybackState.STATE_PLAYING)
                    currentController.getTransportControls().pause();
                else
                    currentController.getTransportControls().play();
            }
        } else {
            findActiveMediaController(); // Try to reconnect
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (clockRunnable != null)
            clockHandler.removeCallbacks(clockRunnable);
        if (currentController != null)
            currentController.unregisterCallback(mediaCallback);
    }
}
