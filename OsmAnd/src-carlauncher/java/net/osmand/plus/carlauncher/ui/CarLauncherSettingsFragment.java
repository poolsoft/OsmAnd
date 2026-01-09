package net.osmand.plus.carlauncher.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.carlauncher.AutoLaunchManager;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.carlauncher.LauncherBackupManager;
import net.osmand.plus.carlauncher.dock.AppDockManager;
import net.osmand.plus.carlauncher.dock.AppPickerDialog;
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
        getPreferenceManager().setSharedPreferencesName("car_launcher_prefs");
        setPreferencesFromResource(R.xml.carlauncher_prefs, rootKey);

        if (getContext() != null) {
            settings = new CarLauncherSettings(getContext());
        }

        setupAppearancePrefs();
        setupMusicPrefs();
        setupAutoLaunchPrefs();
        setupBackupPrefs();
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
            android.util.TypedValue outValue = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            closeBtn.setBackgroundResource(outValue.resourceId);
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
                Toast.makeText(getContext(), "Tema degisikligi uygulamanin yeniden baslatilmasini gerektirir",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // Widget Display Mode
        androidx.preference.ListPreference displayModePref = findPreference(CarLauncherSettings.KEY_WIDGET_DISPLAY_MODE);
        if (displayModePref != null) {
            displayModePref.setEntries(new CharSequence[]{"Liste (Varsayılan)", "Sayfalı (Carousel)"});
            displayModePref.setEntryValues(new CharSequence[]{"0", "1"});
            displayModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                CarLauncherSettings settings = new CarLauncherSettings(getContext());
                try {
                    settings.setWidgetDisplayMode(Integer.parseInt((String) newValue));
                } catch (NumberFormatException e) {
                    settings.setWidgetDisplayMode(0);
                }

                Toast.makeText(getContext(), "Görünüm değişikliği için widget paneli yenilenecek",
                        Toast.LENGTH_SHORT).show();
                
                 if (getActivity() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
                    getActivity().sendBroadcast(intent);
                }
                return true;
            });
        }

        // Widget Manager
        Preference widgetPref = findPreference("car_launcher_widget_manager");
        if (widgetPref != null) {
            widgetPref.setOnPreferenceClickListener(preference -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Widget ayarlari ust panelde duzenlenir", Toast.LENGTH_SHORT).show();
                }
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
        if (getContext() == null) return;
        new AppPickerDialog(getContext(), true, (packageName, name, icon) -> {
            settings.setMusicApp(packageName);
            MusicManager musicManager = MusicManager.getInstance(getContext());
            musicManager.setPreferredPackage("internal".equals(packageName) ? null : packageName);
            
            Preference pref = findPreference(CarLauncherSettings.KEY_MUSIC_APP);
            if (pref != null) updateMusicAppSummary(pref);
        }).show();
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

        Preference resetPref = findPreference("car_launcher_reset_dock");
        if (resetPref != null) {
            resetPref.setOnPreferenceClickListener(preference -> {
                confirmResetDock();
                return true;
            });
        }
    }

    private void confirmResetDock() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Dock'u Sıfırla")
                .setMessage("Tüm uygulama kısayolları silinecek. Emin misiniz?")
                .setPositiveButton("Sıfırla", (dialog, which) -> resetDock())
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void resetDock() {
        if (getContext() == null) return;
        AppDockManager dockManager = new AppDockManager(getContext());
        dockManager.clearAllShortcuts();
        Intent intent = new Intent("net.osmand.carlauncher.DOCK_UPDATED");
        getContext().sendBroadcast(intent);
        Toast.makeText(getContext(), "Dock sıfırlandı", Toast.LENGTH_SHORT).show();
    }

    // ═══════════════════════════════════════════════════════════════
    // OTOMATİK BAŞLATMA
    // ═══════════════════════════════════════════════════════════════

    private void setupAutoLaunchPrefs() {
        bindAutoLaunchSlot(1);
        bindAutoLaunchSlot(2);
        bindAutoLaunchSlot(3);
    }

    private void bindAutoLaunchSlot(int slot) {
        String key = CarLauncherSettings.KEY_AUTOLAUNCH_ENABLE_PREFIX + slot;
        SwitchPreferenceCompat pref = findPreference(key);
        if (pref != null) {
            String appName = settings.getAutoLaunchAppName(slot);
            if (settings.getAutoLaunchPackage(slot) != null) {
                pref.setSummary(appName);
            } else {
                pref.setSummary("Seçmek için metne tıklayın");
            }

            pref.setOnPreferenceClickListener(preference -> {
                if (getContext() == null) return true;
                
                new AppPickerDialog(getContext(), false, (packageName, name, icon) -> {
                    settings.setAutoLaunchApp(slot, packageName, name);
                    pref.setSummary(name);
                    // Force Enable
                    settings.setAutoLaunchEnabled(slot, true);
                    pref.setChecked(true);
                }).show();
                
                return true; // Consume click event -> Handled by dialog
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    private static final int RC_BACKUP_EXPORT = 101;
    private static final int RC_BACKUP_IMPORT = 102;

    private void setupBackupPrefs() {
        Preference exportPref = findPreference("action_backup_export");
        if (exportPref != null) {
            exportPref.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "CarLauncher_Backup_" + System.currentTimeMillis() + ".json");
                    startActivityForResult(intent, RC_BACKUP_EXPORT);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Dosya oluşturulamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference importPref = findPreference("action_backup_import");
        if (importPref != null) {
            importPref.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json"); 
                    startActivityForResult(intent, RC_BACKUP_IMPORT);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Dosya seçici açılamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == RC_BACKUP_EXPORT) {
                LauncherBackupManager.exportBackup(getContext(), uri);
            } else if (requestCode == RC_BACKUP_IMPORT) {
                LauncherBackupManager.restoreBackup(getContext(), uri);
                getPreferenceScreen().removeAll();
                onCreatePreferences(null, getPreferenceScreen().getKey());
                Toast.makeText(getContext(), "Ayarlar yenilendi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HAKKINDA
    // ═══════════════════════════════════════════════════════════════

    private void setupAboutPrefs() {
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
