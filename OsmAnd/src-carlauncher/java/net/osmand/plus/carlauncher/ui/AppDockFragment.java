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
    private boolean isVerticalMode = false;

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
        void onLayoutModeToggle();
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
        
        // Register Local Broadcast Receiver
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(dockUpdateReceiver, new android.content.IntentFilter("net.osmand.carlauncher.DOCK_UPDATED"));
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

        // Dikey ve yatay ekran durumlarina gore mini player tasarimini ozellestir
        adjustMiniPlayerLayout();

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

        // Sag Buton: 3-nokta Menu (Tiklandiginda acilir menu gosterir)
        if (layoutButton != null) {
            layoutButton.setOnClickListener(v -> {
                showDockPopupMenu(v);
            });
            // Uzun basildiginda da dogrudan ayarlari acar
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

        // Setup RecyclerView
        updateRecyclerViewOrientation(root);

        // Responsive visibility listener based on available dock width
        View dockContainer = root.findViewById(net.osmand.plus.R.id.dock_content_container);
        if (dockContainer != null) {
            dockContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    int width = right - left;
                    if (width > 0) {
                        adjustVisibilityByAvailableWidth(width);
                    }
                }
            });
        }

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
        
        // Ensure orientation is correct based on global settings before creating adapter
        net.osmand.plus.carlauncher.CarLauncherSettings settings = new net.osmand.plus.carlauncher.CarLauncherSettings(getContext());
        String dockPos = settings.getDockPosition();
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        this.isVerticalMode = ("left".equals(dockPos) || "right".equals(dockPos)) && !isPortrait;
        
        adapter = new AppDockAdapter(getContext(), this);
        adapter.setVerticalMode(isVerticalMode); // Set it before attaching to recycler
        recyclerView.setAdapter(adapter);
        
        if (dockManager != null) {
            adapter.setShortcuts(dockManager.getShortcuts());
        }
        
        // Force apply UI constraints
        applyOrientationState(view, isVerticalMode);
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
        
        // Dynamic Color Logic
        int color = android.graphics.Color.WHITE;
        if (albumArt != null) {
            color = getDominantColor(albumArt);
        }

        final int finalColor = color;
        if (miniMusicIcon != null) miniMusicIcon.post(() -> miniMusicIcon.setColorFilter(finalColor));
        if (miniBtnPlay != null) miniBtnPlay.post(() -> miniBtnPlay.setColorFilter(finalColor));
        if (miniBtnNext != null) miniBtnNext.post(() -> miniBtnNext.setColorFilter(finalColor));
    }

    private int getDominantColor(Bitmap bitmap) {
        if (bitmap == null) return android.graphics.Color.WHITE;
        try {
            // Sample 1x1 pixel for average
            Bitmap small = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
            int color = small.getPixel(0, 0);
            small.recycle();

            // Check Luminance (if too dark, return White)
            int r = android.graphics.Color.red(color);
            int g = android.graphics.Color.green(color);
            int b = android.graphics.Color.blue(color);
            double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

            if (lum < 0.5) return android.graphics.Color.WHITE; // Ensure visibility on dark background
            return color;
        } catch (Exception e) {
            return android.graphics.Color.WHITE;
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (miniBtnPlay != null) {
            miniBtnPlay.post(() -> miniBtnPlay.setImageResource(
                    isPlaying ? net.osmand.plus.R.drawable.ic_music_pause : net.osmand.plus.R.drawable.ic_music_play));
        }
    }

    @Override
    public void onSourceChanged(boolean isInternal) {
        // Kaynak degistiginde yapilacaklar (Ornegin ikon degisimi)
        if (miniMusicIcon != null) {
            miniMusicIcon.post(() -> {
                miniMusicIcon.setImageResource(isInternal ? net.osmand.plus.R.drawable.ic_music_play : // Internal icon
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
        if (getContext() == null || dockManager == null) return;
        
        String[] options = {
            "Uygulamayi Ac (Tam Ekran)",
            "Uygulamayi Yan Yana Ac (Bolunmus Ekran / Coolwalk)",
            "Varsayilan Acilis Modu: Tam Ekran Yap",
            "Varsayilan Acilis Modu: Yan Yana (Bolunmus) Yap",
            "Kisayolu Kaldir"
        };
        
        new AlertDialog.Builder(getContext())
                .setTitle(shortcut.getAppName() + " Secenekleri")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Tam Ekran Baslat
                            launchAppWithMode(shortcut, LaunchMode.FULL_SCREEN);
                            break;
                        case 1: // Yan Yana / Split-Screen Baslat
                            launchAppWithMode(shortcut, LaunchMode.SPLIT_SCREEN);
                            break;
                        case 2: // Varsayilan modu Tam Ekran yap
                            shortcut.setLaunchMode(LaunchMode.FULL_SCREEN);
                            dockManager.saveShortcuts();
                            if (adapter != null) {
                                adapter.setShortcuts(dockManager.getShortcuts());
                            }
                            break;
                        case 3: // Varsayilan modu Split-Screen yap
                            shortcut.setLaunchMode(LaunchMode.SPLIT_SCREEN);
                            dockManager.saveShortcuts();
                            if (adapter != null) {
                                adapter.setShortcuts(dockManager.getShortcuts());
                            }
                            break;
                        case 4: // Kaldir
                            onRemoveClick(shortcut);
                            break;
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void launchAppWithMode(AppShortcut shortcut, LaunchMode mode) {
        try {
            Intent intent = getContext().getPackageManager()
                    .getLaunchIntentForPackage(shortcut.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (mode == LaunchMode.SPLIT_SCREEN) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    }
                }
                getContext().startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(dockUpdateReceiver);
            } catch (Exception e) {
                // Ignore if already unregistered
            }
        }
    }

    public void updateLayoutIcon(int mode) {
        if (layoutButton == null) return;
        
        layoutButton.post(() -> {
            switch (mode) {
                case 0: // Normal (Widgets Visible)
                    layoutButton.setImageResource(android.R.drawable.ic_menu_mapmode);
                    break;
                case 2: // Full Screen (Map Only)
                    layoutButton.setImageResource(net.osmand.plus.R.drawable.ic_action_view_as_list);
                    break;
                default: 
                    layoutButton.setImageResource(android.R.drawable.ic_menu_mapmode);
                    break;
            }
        });
    }


    public void setOrientation(boolean isVertical) {
        this.isVerticalMode = isVertical;
        this.currentOrientation = isVertical ? ORIENTATION_VERTICAL : ORIENTATION_HORIZONTAL;
        
        if (getView() != null) {
            applyOrientationState(getView(), isVertical);
        }
    }

    private void applyOrientationState(View root, boolean isVertical) {
        root.post(() -> {
            if (getContext() == null) return;
            
            // 1. Update Recycler Orientation
            updateRecyclerViewOrientation(root);
            
            // 2. Main Container Setup
            LinearLayout ll = root.findViewById(net.osmand.plus.R.id.dock_content_container);
            if (ll != null) {
                ll.setOrientation(isVertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                // CRITICAL: Force TOP alignment in Sidebar mode
                ll.setGravity(isVertical ? android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP : android.view.Gravity.CENTER_VERTICAL);
                ll.setPadding(isVertical ? 0 : 16, isVertical ? 8 : 0, isVertical ? 0 : 16, isVertical ? 8 : 0);
                
                ViewGroup.LayoutParams lp = ll.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                ll.setLayoutParams(lp);
            }
            
            // 3. Conditional Visibility
            if (miniMusicContainer != null) {
                miniMusicContainer.setVisibility(isVertical ? View.GONE : View.VISIBLE);
                if (!isVertical) {
                    adjustMiniPlayerLayout();
                }
            }
            
            View clockContainer = root.findViewById(net.osmand.plus.R.id.clock_settings_container);
            if (clockContainer != null) {
                clockContainer.setVisibility(isVertical ? View.GONE : View.VISIBLE);
            }
            
            // 4. Item Layout Params Adjustments
            int gravity = isVertical ? android.view.Gravity.CENTER_HORIZONTAL : android.view.Gravity.CENTER_VERTICAL;
            
            if (appListButton != null) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) appListButton.getLayoutParams();
                lp.gravity = gravity;
                // Reduced top margin in vertical to keep it closer and match horizontal margins
                lp.setMargins(isVertical ? 0 : 8, isVertical ? 10 : 0, isVertical ? 0 : 8, isVertical ? 10 : 0);
                appListButton.setLayoutParams(lp);
            }
            
            if (layoutButton != null) {
                layoutButton.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) layoutButton.getLayoutParams();
                lp.gravity = gravity;
                if (isVertical) {
                    lp.setMargins(0, 0, 0, 10); // Reduced bottom margin
                } else {
                    lp.setMargins(4, 0, 4, 0);
                }
                layoutButton.setLayoutParams(lp);
            }
            
            // 5. RecyclerView Layout Adjustments
            if (recyclerView != null) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) recyclerView.getLayoutParams();
                lp.width = isVertical ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
                lp.height = isVertical ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
                lp.weight = 1.0f;
                recyclerView.setLayoutParams(lp);
            }
        });
    }

    private void updateRecyclerViewOrientation(View root) {
        if (recyclerView == null) recyclerView = root.findViewById(net.osmand.plus.R.id.dock_recycler);
        if (recyclerView != null) {
            // Force clear recycled view pool to ensure fresh ViewHolders with correct LayoutParams
            recyclerView.setRecycledViewPool(new androidx.recyclerview.widget.RecyclerView.RecycledViewPool());
            
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), 
                isVerticalMode ? LinearLayoutManager.VERTICAL : LinearLayoutManager.HORIZONTAL, false));
            if (adapter != null) {
                adapter.setVerticalMode(isVerticalMode);
            }
        }
    }

    private void adjustMiniPlayerLayout() {
        if (getContext() == null || miniMusicContainer == null) return;
        
        boolean isScreenPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (isScreenPortrait) {
            miniMusicContainer.setVisibility(View.VISIBLE);
            miniMusicContainer.setBackground(null);
            miniMusicContainer.setPadding(0, 0, 0, 0);
            miniMusicContainer.setMinimumWidth(0);
            
            if (miniMusicIcon != null) miniMusicIcon.setVisibility(View.GONE);
            if (miniMusicTitle != null) miniMusicTitle.setVisibility(View.GONE);
            
            if (miniBtnPlay != null) {
                miniBtnPlay.setBackgroundResource(net.osmand.plus.R.drawable.bg_mini_play_circle);
                miniBtnPlay.setColorFilter(0xFFFFFFFF);
                miniBtnPlay.setElevation(dpToPx(2)); // Play sits on top of Next
                android.view.ViewGroup.LayoutParams lp = miniBtnPlay.getLayoutParams();
                lp.width = dpToPx(36);
                lp.height = dpToPx(36);
                miniBtnPlay.setLayoutParams(lp);
                miniBtnPlay.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            }
            
            if (miniBtnNext != null) {
                miniBtnNext.setBackgroundResource(net.osmand.plus.R.drawable.bg_mini_next_half_pill);
                miniBtnNext.setColorFilter(0xFFFFFFFF);
                miniBtnNext.setElevation(dpToPx(1)); // Next sits under Play
                android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) miniBtnNext.getLayoutParams();
                lp.width = dpToPx(54); // Longer width to slide out
                lp.height = dpToPx(36); // Same height as play circle
                lp.leftMargin = dpToPx(-18); // Overlaps right half of play circle
                miniBtnNext.setLayoutParams(lp);
                // Pushes Next icon to the visible right part to center it beautifully
                miniBtnNext.setPadding(dpToPx(22), dpToPx(8), dpToPx(8), dpToPx(8));
            }
        } else {
            // Restore default landscape styles if needed (in case of dynamic layout configuration updates)
            miniMusicContainer.setBackgroundResource(net.osmand.plus.R.drawable.bg_drawer_rounded);
            miniMusicContainer.setPadding(dpToPx(12), 0, dpToPx(12), 0);
            miniMusicContainer.setMinimumWidth(dpToPx(160));
            
            if (miniMusicIcon != null) miniMusicIcon.setVisibility(View.VISIBLE);
            if (miniMusicTitle != null) miniMusicTitle.setVisibility(View.VISIBLE);
            
            if (miniBtnPlay != null) {
                miniBtnPlay.setBackgroundResource(0);
                miniBtnPlay.setBackgroundResource(android.R.drawable.screen_background_light_transparent); // selectableItemBackgroundBorderless fallback
                miniBtnPlay.setBackground(getResources().getDrawable(android.R.drawable.screen_background_light_transparent, null)); // generic background
                // We can set it to selectableItemBackground
                android.util.TypedValue outValue = new android.util.TypedValue();
                getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                miniBtnPlay.setBackgroundResource(outValue.resourceId);
                
                android.view.ViewGroup.LayoutParams lp = miniBtnPlay.getLayoutParams();
                lp.width = dpToPx(32);
                lp.height = dpToPx(32);
                miniBtnPlay.setLayoutParams(lp);
                miniBtnPlay.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            }
            
            if (miniBtnNext != null) {
                android.util.TypedValue outValue = new android.util.TypedValue();
                getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                miniBtnNext.setBackgroundResource(outValue.resourceId);
                
                android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) miniBtnNext.getLayoutParams();
                lp.width = dpToPx(32);
                lp.height = dpToPx(32);
                lp.leftMargin = 0;
                miniBtnNext.setLayoutParams(lp);
                miniBtnNext.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            }
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    private void showDockPopupMenu(View anchor) {
        if (getContext() == null) return;
        
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), anchor);
        
        // Turkce karakter kullanmadan menuyu dinamik olarak dolduruyoruz
        popup.getMenu().add(0, 1, 0, "Gorunumu Degistir (Buyuk/Kucuk Panel)");
        
        if (adapter != null) {
            popup.getMenu().add(0, 2, 1, adapter.isEditMode() ? "Duzenleme Modunu Kapat" : "Kisayollari Duzenle");
        }
        
        popup.getMenu().add(0, 3, 2, "Ayarlar");
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    if (listener != null) {
                        listener.onLayoutModeToggle();
                    }
                    return true;
                case 2:
                    if (adapter != null) {
                        adapter.setEditMode(!adapter.isEditMode());
                    }
                    return true;
                case 3:
                    openSettings();
                    return true;
                default:
                    return false;
            }
        });
        
        popup.show();
    }

    private void adjustVisibilityByAvailableWidth(int totalWidthPx) {
        if (getContext() == null) return;
        
        // Sadece SCREEN dikey (portrait) konumdayken bu dinamik visibility kurallarini uygula!
        boolean isScreenPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (!isScreenPortrait) {
            // Yatay modda veya dikey dock durumlarinda standart gorunurlugu koru
            if (clockView != null && clockView.getParent() != null) {
                ((View) clockView.getParent()).setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            return;
        }
        
        // Genisligi DP degerine donustur
        float density = getResources().getDisplayMetrics().density;
        int totalWidthDp = Math.round(totalWidthPx / density);
        
        View clockContainer = clockView != null ? (View) clockView.getParent() : null;
        
        // Oncelikli gizleme mantigi (Priority Rules):
        // 1. Esik 1: Genislik >= 480dp -> Saat, Player ve Recycler acik
        if (totalWidthDp >= 480) {
            if (clockContainer != null) clockContainer.setVisibility(View.VISIBLE);
            if (miniMusicContainer != null) {
                miniMusicContainer.setVisibility(View.VISIBLE);
                adjustMiniPlayerLayout();
            }
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }
        // 2. Esik 2: 420dp <= Genislik < 480dp -> Saat gizlenir, Player ve Recycler acik
        else if (totalWidthDp >= 420) {
            if (clockContainer != null) clockContainer.setVisibility(View.GONE);
            if (miniMusicContainer != null) {
                miniMusicContainer.setVisibility(View.VISIBLE);
                adjustMiniPlayerLayout();
            }
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }
        // 3. Esik 3: 280dp <= Genislik < 420dp -> Saat VE Player gizlenir, Kisayol Recycler'ina tam alan kalir! (Dikey portrait telefon ve teyplerde simgelerin sigmasini saglar)
        else if (totalWidthDp >= 280) {
            if (clockContainer != null) clockContainer.setVisibility(View.GONE);
            if (miniMusicContainer != null) miniMusicContainer.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }
        // 4. Esik 4: Genislik < 280dp -> Kisayollar da gizlenir
        else {
            if (clockContainer != null) clockContainer.setVisibility(View.GONE);
            if (miniMusicContainer != null) miniMusicContainer.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        }
    }
}
