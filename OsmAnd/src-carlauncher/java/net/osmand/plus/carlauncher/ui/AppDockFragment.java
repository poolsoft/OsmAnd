package net.osmand.plus.carlauncher.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
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
import net.osmand.plus.carlauncher.music.MusicManager;

/**
 * App Dock fragment.
 * <<<<<<< HEAD
 * Uygulama kisayollarini ve Mini Player'i gosterir.
 * =======
 * >>>>>>> 32c2ee2e47809bba5011d01849fb227eaed926a9
 */
public class AppDockFragment extends Fragment
        implements AppDockAdapter.OnShortcutListener, MusicManager.MusicUIListener {

    public static final String TAG = "AppDockFragment";
    // Sync Fix: Ensure remote matches local
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
    private ImageButton miniBtnNext;
    private ImageView miniMusicIcon;

    private MusicManager musicManager;

    public interface OnAppDockListener {
        void onLayoutToggle();

        void onAppDrawerOpen();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            dockManager = new AppDockManager(getContext());
            dockManager.loadShortcuts();
            overlayManager = new OverlayWindowManager(getContext());
            prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentOrientation = prefs.getInt(KEY_ORIENTATION, ORIENTATION_HORIZONTAL);

            // Music Manager
            musicManager = MusicManager.getInstance(getContext());

            // Register Dock Update Receiver
            dockUpdateReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    refreshDock();
                }
            };
            android.content.IntentFilter filter = new android.content.IntentFilter(
                    "net.osmand.carlauncher.DOCK_UPDATED");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(dockUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(dockUpdateReceiver, filter);
            }
        }
    }

    private android.content.BroadcastReceiver dockUpdateReceiver;

    /**
     * Dock'u yeniden yukle ve UI'i guncelle.
     */
    public void refreshDock() {
        if (dockManager != null && adapter != null && getActivity() != null) {
            dockManager.loadShortcuts();
            getActivity().runOnUiThread(() -> {
                adapter.setShortcuts(dockManager.getShortcuts());
                adapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAppDockListener) {
            listener = (OnAppDockListener) context;
        } else {
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
        miniBtnNext = root.findViewById(net.osmand.plus.R.id.mini_btn_next);
        miniMusicIcon = root.findViewById(net.osmand.plus.R.id.mini_music_icon);

        if (clockView != null) {
            clockView.setOnClickListener(v -> openSettings());
            // Typeface digitalFont = Typeface.createFromAsset(context.getAssets(),
            // "fonts/curved-seven-segment.ttf");
            Typeface digitalFont = Typeface.createFromAsset(requireContext().getAssets(), "fonts/Cross Boxed.ttf");
            clockView.setTypeface(digitalFont);
        }

        // Hide Mini Player in Portrait Mode
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            if (miniMusicContainer != null)
                miniMusicContainer.setVisibility(View.GONE);
        }

        if (miniBtnPlay != null) {
            miniBtnPlay.setOnClickListener(v -> musicManager.togglePlayPause());
        }

        if (miniBtnNext != null) {
            miniBtnNext.setOnClickListener(v -> musicManager.skipToNext());
        }

        if (miniMusicContainer != null) {
            miniMusicContainer.setOnClickListener(v -> {
                // Use both global and local broadcast for compatibility
                Intent intent = new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER");
                intent.setPackage(getContext().getPackageName());
                getContext().sendBroadcast(intent);

                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getContext())
                        .sendBroadcast(new Intent("net.osmand.carlauncher.OPEN_MUSIC_DRAWER"));
            });
        }

        // Start Clock
        startClock();

        // Setup Buttons
        // Listeners
        appListButton.setOnClickListener(v -> {
            if (listener != null)
                listener.onAppDrawerOpen();
        });

        // Right Button: Layout Toggle
        if (layoutButton != null) {
            layoutButton.setOnClickListener(v -> {
                if (listener != null)
                    listener.onLayoutToggle();
            });
            // Long press opens settings as backup
            layoutButton.setOnLongClickListener(v -> {
                openSettings();
                return true;
            });
        }

        // Clock container (Removes generic click to avoid confusion with layout toggle)
        View clockContainer = root.findViewById(net.osmand.plus.R.id.clock_settings_container);
        if (clockContainer != null) {
            // Optional: Clicking clock could open clock app, but removing settings link for
            // now
            clockContainer.setOnClickListener(null);
        }

        // Setup RecyclerView (Force Horizontal)
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Long Press on Root & Recycler to add apps
        View.OnLongClickListener longClickListener = v -> {
            showAppPickerDialog();
            return true;
        };
        root.setOnLongClickListener(longClickListener);
        recyclerView.setOnLongClickListener(longClickListener);

        // Drag and Drop
        androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new androidx.recyclerview.widget.ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = androidx.recyclerview.widget.ItemTouchHelper.LEFT
                        | androidx.recyclerview.widget.ItemTouchHelper.RIGHT
                        | androidx.recyclerview.widget.ItemTouchHelper.UP
                        | androidx.recyclerview.widget.ItemTouchHelper.DOWN;
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();

                // Update Adapter
                if (adapter != null) {
                    adapter.onItemMove(from, to);
                }
                // Update Manager
                if (dockManager != null) {
                    dockManager.moveShortcut(from, to);
                }
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // No swipe
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        };
        new androidx.recyclerview.widget.ItemTouchHelper(callback).attachToRecyclerView(recyclerView);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new AppDockAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);
        if (dockManager != null) {
            adapter.setShortcuts(dockManager.getShortcuts());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (musicManager != null)
            musicManager.addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (musicManager != null)
            musicManager.removeListener(this);
    }

    // --- MusicUIListener ---

    @Override
    public void onTrackChanged(String title, String artist, Bitmap albumArt, String packageName) {
        if (miniMusicTitle != null) {
            miniMusicTitle.post(() -> miniMusicTitle.setText(title != null ? title : "Muzik"));
        }
        // Ikon degistirme logic'i (Opsiyonel: Kaynak ikonunu veya album art'i kucultup
        // goster)
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (miniBtnPlay != null) {
            miniBtnPlay.post(() -> miniBtnPlay.setImageResource(
                    isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play));
        }
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
        // Kaynak degistiginde yapilacaklar (Ornegin ikon degisimi)
        if (miniMusicIcon != null) {
            miniMusicIcon.post(() -> {
                miniMusicIcon.setImageResource(isInternal ? android.R.drawable.ic_media_play : // Internal icon
                        android.R.drawable.stat_sys_headset); // External icon placeholder
                miniMusicIcon.setColorFilter(android.graphics.Color.WHITE);
            });
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
                    if (overlayManager != null)
                        try {
                            overlayManager.showOverlay(packageName);
                        } catch (Exception e) {
                            launchAppStandard(packageName);
                        }
                    break;
                case SPLIT_SCREEN:
                    launchAppSplitScreen(packageName);
                    break;
                case WIDGET_ONLY:
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
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                }
                getContext().startActivity(intent);
            }
        } catch (Exception e) {
            launchAppStandard(packageName);
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
            AppShortcut newShortcut = new AppShortcut(packageName, appName, icon, dockManager.getShortcuts().size(),
                    LaunchMode.FULL_SCREEN);
            if (dockManager.addShortcut(newShortcut)) {
                if (adapter != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setShortcuts(dockManager.getShortcuts());
                        adapter.notifyDataSetChanged();
                    });
                }
            }
        });
        dialog.show();
    }

    private void openSettings() {
        if (getContext() instanceof net.osmand.plus.activities.MapActivity) {
            ((net.osmand.plus.activities.MapActivity) getContext()).openCarLauncherSettings();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (clockRunnable != null)
            clockHandler.removeCallbacks(clockRunnable);

        // Unregister dock update receiver
        if (dockUpdateReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(dockUpdateReceiver);
            } catch (Exception e) {
                // Ignore if already unregistered
            }
        }
    }
}
