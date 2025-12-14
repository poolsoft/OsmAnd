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
import android.util.Log;

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
        LinearLayout rootLayout = new LinearLayout(getContext());
        rootLayout.setOrientation(
                currentOrientation == ORIENTATION_HORIZONTAL ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setGravity(android.view.Gravity.CENTER);
        rootLayout.setBackgroundResource(net.osmand.plus.R.drawable.bg_app_dock);
        rootLayout.setPadding(8, 8, 8, 8);

        // 1. Menu Button (Leftmost/Topmost)
        menuButton = createDockButton(android.R.drawable.ic_menu_preferences, v -> {
             // Open Settings or Menu
             if (listener != null) listener.onAppDrawerOpen(); // For now map to drawer/menu
        });
        rootLayout.addView(menuButton);

        // 2. Layout Toggle Button
        layoutButton = createDockButton(net.osmand.plus.R.drawable.dashboard_grid, v -> {
            if (listener != null) listener.onLayoutToggle();
        });
        rootLayout.addView(layoutButton);

        // 3. App List Button
        appListButton = createDockButton(android.R.drawable.ic_menu_sort_by_size, v -> { // sort_by_size looks like a list/grid
            if (listener != null) listener.onAppDrawerOpen();
        });
        rootLayout.addView(appListButton);

        // Separator / Spacer
        View spacer = new View(getContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            currentOrientation == ORIENTATION_HORIZONTAL ? 16 : ViewGroup.LayoutParams.MATCH_PARENT, 
            currentOrientation == ORIENTATION_HORIZONTAL ? ViewGroup.LayoutParams.MATCH_PARENT : 16));
        rootLayout.addView(spacer);

        // RecyclerView (Shortcuts)
        recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), currentOrientation, false));

        rootLayout.setOnLongClickListener(v -> {
             showAppPickerDialog();
             return true;
        });

        LinearLayout.LayoutParams recyclerParams = new LinearLayout.LayoutParams(
                currentOrientation == ORIENTATION_HORIZONTAL ? 0 : ViewGroup.LayoutParams.MATCH_PARENT,
                currentOrientation == ORIENTATION_HORIZONTAL ? ViewGroup.LayoutParams.MATCH_PARENT : 0,
                1.0f);
        rootLayout.addView(recyclerView, recyclerParams);

        // Remove old button logic
        return rootLayout;
    }

    private ImageButton createDockButton(int iconResId, View.OnClickListener onClick) {
        ImageButton btn = new ImageButton(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                80, 80); // Fixed size for touch targets
        params.setMargins(8, 8, 8, 8);
        btn.setLayoutParams(params);
        btn.setImageResource(iconResId);
        btn.setBackgroundResource(android.R.color.transparent);
        btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btn.setColorFilter(0xFFFFFFFF); // White icon
        btn.setPadding(16, 16, 16, 16);
        btn.setOnClickListener(onClick);
        return btn;
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
        toggleEditMode();
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
        if (getContext() == null || dockManager == null) return;

        AppPickerDialog dialog = new AppPickerDialog(getContext(), appInfo -> {
            // App secildiginde dock'a ekle
            String packageName = appInfo.packageName;
            // Assuming addShortcut method can take packageName and a default LaunchMode
            // Or you might need to show a launch mode selector here as well
            dockManager.addShortcut(packageName, LaunchMode.FULL_SCREEN); 
            
            // Adapteri guncelle
            if (adapter != null) {
                adapter.setShortcuts(dockManager.getShortcuts());
                adapter.notifyDataSetChanged();
            }
        });
        dialog.show();
    }

    @Override
    public void onShortcutLongClick(AppShortcut shortcut) {
         // Existing long click (edit/delete) logic...
         // For now let's just show picker too or edit options
         showAppPickerDialog(); // Temporary override: easier to just allow adding more
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
}
