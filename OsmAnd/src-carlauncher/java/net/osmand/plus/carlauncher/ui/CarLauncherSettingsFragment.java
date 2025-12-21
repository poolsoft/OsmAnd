package net.osmand.plus.carlauncher.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.carlauncher.dock.AppDockManager;
import net.osmand.plus.carlauncher.music.MusicManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Car Launcher Ayarlar Fragmenti.
 * Tum ayarlari gruplar halinde gosterir ve yonetir.
 */
public class CarLauncherSettingsFragment extends PreferenceFragmentCompat {

    public static final String TAG = "CarLauncherSettingsFragment";

    private CarLauncherSettings settings;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.carlauncher_prefs, rootKey);

        if (getContext() != null) {
            settings = new CarLauncherSettings(getContext());
        }

        setupAppearancePrefs();
        setupMusicPrefs();
        setupDockPrefs();
        setupAboutPrefs();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View prefsView = super.onCreateView(inflater, container, savedInstanceState);
        if (prefsView == null)
            return null;

        prefsView.setBackgroundColor(0xFF111111); // Dark background

        // Wrapper to hold Prefs + Close Button
        android.widget.FrameLayout wrapper = new android.widget.FrameLayout(getContext());
        wrapper.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        wrapper.setFitsSystemWindows(true); // Ensure padding for status bar

        // Add Prefs View
        wrapper.addView(prefsView, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        // Add Close Button (Top-Right)
        if (getContext() != null) {
            android.widget.ImageButton closeBtn = new android.widget.ImageButton(getContext());
            closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            closeBtn.setBackground(getContext().getDrawable(android.R.drawable.selectable_item_background));
            closeBtn.setColorFilter(0xFFFFFFFF);
            closeBtn.setPadding(32, 32, 32, 32);
            closeBtn.setOnClickListener(v -> closeSettings());

            android.widget.FrameLayout.LayoutParams btnParams = new android.widget.FrameLayout.LayoutParams(
                    120, 120); // approx 40-48dp
            btnParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            btnParams.setMargins(24, 24, 24, 24);

            wrapper.addView(closeBtn, btnParams);
        }

        return wrapper;
    }

    private void closeSettings() {
        if (getActivity() != null) {
            if (getActivity() instanceof net.osmand.plus.carlauncher.CarLauncherInterface) {
                ((net.osmand.plus.carlauncher.CarLauncherInterface) getActivity()).closeAppDrawer();
            } else {
                getActivity().onBackPressed();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Clean up old logic if any remains
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖRÜNÜM AYARLARI
    // ═══════════════════════════════════════════════════════════════

    private void setupAppearancePrefs() {
        // Status Bar
        SwitchPreferenceCompat statusBarPref = findPreference(CarLauncherSettings.KEY_STATUS_BAR);
        if (statusBarPref != null) {
            statusBarPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean show = (Boolean) newValue;
                applyStatusBarVisibility(show);
                return true;
            });
        }

        // Dark Theme
        SwitchPreferenceCompat themePref = findPreference(CarLauncherSettings.KEY_DARK_THEME);
        if (themePref != null) {
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean dark = (Boolean) newValue;
                // Tema degisikligi icin activity restart gerekebilir
                Toast.makeText(getContext(), "Tema degisikligi uygulamanin yeniden baslatilmasini gerektirir",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // Widget Manager
        Preference widgetPref = findPreference("car_launcher_widget_manager");
        if (widgetPref != null) {
            widgetPref.setOnPreferenceClickListener(preference -> {
                openWidgetManager();
                return true;
            });
        }
    }

    private void applyStatusBarVisibility(boolean show) {
        if (getActivity() == null)
            return;

        Window window = getActivity().getWindow();
        if (show) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void openWidgetManager() {
        // Widget Settings Dialog - requires WidgetManager from WidgetPanelFragment
        // For now, show a message since WidgetManager is not accessible here
        if (getContext() != null) {
            Toast.makeText(getContext(), "Widget ayarlari ust panelde duzenlenir", Toast.LENGTH_SHORT).show();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MÜZİK AYARLARI
    // ═══════════════════════════════════════════════════════════════

    private void setupMusicPrefs() {
        // Music App Picker
        Preference musicAppPref = findPreference(CarLauncherSettings.KEY_MUSIC_APP);
        if (musicAppPref != null) {
            updateMusicAppSummary(musicAppPref);
            musicAppPref.setOnPreferenceClickListener(preference -> {
                showMusicAppPicker();
                return true;
            });
        }

        // Equalizer
        Preference eqPref = findPreference("car_launcher_equalizer");
        if (eqPref != null) {
            eqPref.setOnPreferenceClickListener(preference -> {
                openEqualizer();
                return true;
            });
        }
    }

    private void updateMusicAppSummary(Preference pref) {
        if (settings == null || getContext() == null)
            return;

        String pkg = settings.getMusicApp();
        if ("internal".equals(pkg)) {
            pref.setSummary("Dahili Player");
        } else {
            try {
                PackageManager pm = getContext().getPackageManager();
                String appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                pref.setSummary(appName);
            } catch (PackageManager.NameNotFoundException e) {
                pref.setSummary(pkg);
            }
        }
    }

    private void showMusicAppPicker() {
        if (getContext() == null)
            return;

        // Find installed music apps
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MUSIC);
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);

        List<String> names = new ArrayList<>();
        List<String> packages = new ArrayList<>();

        // Add internal option
        names.add("Dahili Player");
        packages.add("internal");

        for (ResolveInfo ri : apps) {
            String name = ri.loadLabel(pm).toString();
            String pkg = ri.activityInfo.packageName;
            if (!packages.contains(pkg)) {
                names.add(name);
                packages.add(pkg);
            }
        }

        // Also check for popular music apps
        String[] popularApps = {
                "com.spotify.music",
                "com.google.android.apps.youtube.music",
                "com.amazon.mp3",
                "com.apple.android.music",
                "deezer.android.app"
        };

        for (String pkg : popularApps) {
            if (!packages.contains(pkg)) {
                try {
                    String name = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                    names.add(name);
                    packages.add(pkg);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }

        String[] nameArray = names.toArray(new String[0]);
        new AlertDialog.Builder(getContext())
                .setTitle("Müzik Uygulaması Seçin")
                .setItems(nameArray, (dialog, which) -> {
                    String selectedPkg = packages.get(which);
                    settings.setMusicApp(selectedPkg);

                    // Update MusicManager
                    MusicManager musicManager = MusicManager.getInstance(getContext());
                    musicManager.setPreferredPackage("internal".equals(selectedPkg) ? null : selectedPkg);

                    // Update summary
                    Preference pref = findPreference(CarLauncherSettings.KEY_MUSIC_APP);
                    if (pref != null) {
                        updateMusicAppSummary(pref);
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void openEqualizer() {
        try {
            Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Equalizer bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DOCK AYARLARI
    // ═══════════════════════════════════════════════════════════════

    private void setupDockPrefs() {
        // Max Shortcuts
        SeekBarPreference maxShortcutsPref = findPreference(CarLauncherSettings.KEY_MAX_SHORTCUTS);
        if (maxShortcutsPref != null) {
            maxShortcutsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                int max = (Integer) newValue;
                if (settings != null) {
                    settings.setMaxShortcuts(max);
                }
                return true;
            });
        }

        // Reset Dock
        Preference resetPref = findPreference("car_launcher_reset_dock");
        if (resetPref != null) {
            resetPref.setOnPreferenceClickListener(preference -> {
                confirmResetDock();
                return true;
            });
        }
    }

    private void confirmResetDock() {
        if (getContext() == null)
            return;

        new AlertDialog.Builder(getContext())
                .setTitle("Dock'u Sıfırla")
                .setMessage("Tüm uygulama kısayolları silinecek. Emin misiniz?")
                .setPositiveButton("Sıfırla", (dialog, which) -> {
                    resetDock();
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void resetDock() {
        if (getContext() == null)
            return;

        AppDockManager dockManager = new AppDockManager(getContext());
        dockManager.clearAllShortcuts();

        // Send broadcast to refresh dock
        Intent intent = new Intent("net.osmand.carlauncher.DOCK_UPDATED");
        getContext().sendBroadcast(intent);

        Toast.makeText(getContext(), "Dock sıfırlandı", Toast.LENGTH_SHORT).show();
    }

    // ═══════════════════════════════════════════════════════════════
    // HAKKINDA
    // ═══════════════════════════════════════════════════════════════

    private void setupAboutPrefs() {
        // Version
        Preference versionPref = findPreference("car_launcher_version");
        if (versionPref != null) {
            try {
                String version = getContext().getPackageManager()
                        .getPackageInfo(getContext().getPackageName(), 0).versionName;
                versionPref.setSummary("v" + version);
            } catch (Exception e) {
                versionPref.setSummary("1.0.0");
            }
        }

        // GitHub
        Preference githubPref = findPreference("car_launcher_github");
        if (githubPref != null) {
            githubPref.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/nicksoftware/OsmAnd-CarLauncher"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Tarayıcı açılamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }
}
