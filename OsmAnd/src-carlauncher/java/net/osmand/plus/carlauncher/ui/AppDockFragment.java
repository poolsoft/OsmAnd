package net.osmand.plus.carlauncher.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.carlauncher.dock.AppDockAdapter;
import net.osmand.plus.carlauncher.dock.AppDockManager;
import net.osmand.plus.carlauncher.dock.AppPickerDialog;
import net.osmand.plus.carlauncher.dock.AppShortcut;

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
    private SharedPreferences prefs;
    private boolean isEditMode = false;
    private int currentOrientation = ORIENTATION_HORIZONTAL; // Varsayilan yatay

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            dockManager = new AppDockManager(getContext());
            dockManager.loadShortcuts();

            prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentOrientation = prefs.getInt(KEY_ORIENTATION, ORIENTATION_HORIZONTAL);
        }
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
        rootLayout.setBackgroundColor(0x00000000);
        rootLayout.setPadding(8, 8, 8, 8);

        // RecyclerView
        recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), currentOrientation, false));

        LinearLayout.LayoutParams recyclerParams = new LinearLayout.LayoutParams(
                currentOrientation == ORIENTATION_HORIZONTAL ? 0 : ViewGroup.LayoutParams.MATCH_PARENT,
                currentOrientation == ORIENTATION_HORIZONTAL ? ViewGroup.LayoutParams.MATCH_PARENT : 0,
                1.0f);
        rootLayout.addView(recyclerView, recyclerParams);

        // Buttons container
        LinearLayout buttonsLayout = new LinearLayout(getContext());
        buttonsLayout.setOrientation(
                currentOrientation == ORIENTATION_HORIZONTAL ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        buttonsLayout.setGravity(android.view.Gravity.CENTER);

        // Orientation toggle button
        orientationButton = new ImageButton(getContext());
        orientationButton.setImageResource(
                currentOrientation == ORIENTATION_HORIZONTAL ? android.R.drawable.ic_menu_sort_alphabetically : // Dikey
                                                                                                                // icon
                        android.R.drawable.ic_menu_more); // Yatay icon
        orientationButton.setBackgroundColor(0x00000000);
        orientationButton.setColorFilter(0xFF88FFFF);
        orientationButton.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        orientationButton.setOnClickListener(v -> toggleOrientation());
        orientationButton.setVisibility(View.GONE); // Sadece edit mode'da gorunur
        buttonsLayout.addView(orientationButton);

        // Add button
        addButton = new ImageButton(getContext());
        addButton.setImageResource(android.R.drawable.ic_input_add);
        addButton.setBackgroundColor(0x00000000);
        addButton.setColorFilter(0xFFFFFFFF);
        addButton.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        addButton.setOnClickListener(v -> showAppPicker());
        addButton.setVisibility(View.GONE); // Sadece edit mode'da gorunur
        buttonsLayout.addView(addButton);

        rootLayout.addView(buttonsLayout);

        return rootLayout;
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
        // Adapter icinde launchApp ile handle ediliyor
    }

    @Override
    public void onShortcutLongClick(AppShortcut shortcut) {
        // Uzun basma -> Edit mode
        toggleEditMode();
    }

    @Override
    public void onRemoveClick(AppShortcut shortcut) {
        // Kaldirma onay dialogu
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

    /**
     * Edit mode toggle.
     */
    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (adapter != null) {
            adapter.setEditMode(isEditMode);
        }

        // Button visibility
        if (addButton != null) {
            addButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
        if (orientationButton != null) {
            orientationButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }

        // Otomatik kapat (10 saniye sonra)
        if (isEditMode && getView() != null) {
            getView().postDelayed(() -> {
                if (isEditMode) {
                    toggleEditMode();
                }
            }, 10000);
        }
    }

    /**
     * Orientation toggle (yatay <-> dikey).
     */
    private void toggleOrientation() {
        currentOrientation = (currentOrientation == ORIENTATION_HORIZONTAL) ? ORIENTATION_VERTICAL
                : ORIENTATION_HORIZONTAL;

        // Kaydet
        if (prefs != null) {
            prefs.edit().putInt(KEY_ORIENTATION, currentOrientation).apply();
        }

        // Fragment'i yeniden olustur
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction()
                    .detach(this)
                    .attach(this)
                    .commitAllowingStateLoss();
        }
    }

    /**
     * Uygulama secici goster.
     */
    private void showAppPicker() {
        if (getContext() == null || dockManager == null)
            return;

        // Limit kontrolu
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
                    // Uygulama secildi
                    addShortcut(packageName, appName, icon);
                });
        dialog.show();
    }

    /**
     * Kisayol ekle.
     */
    private void addShortcut(String packageName, String appName, Drawable icon) {
        if (dockManager == null)
            return;

        int order = dockManager.getShortcuts().size();
        AppShortcut shortcut = new AppShortcut(packageName, appName, icon, order);

        if (dockManager.addShortcut(shortcut)) {
            adapter.setShortcuts(dockManager.getShortcuts());
        } else {
            // Eklenemedi
            if (getContext() != null) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Hata")
                        .setMessage("Kisayol eklenemedi. Zaten mevcut veya limit asildi.")
                        .setPositiveButton("Tamam", null)
                        .show();
            }
        }
    }
}
