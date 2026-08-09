package net.osmand.plus.carlauncher.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import net.osmand.plus.carlauncher.headunit.diagnostics.HardwareEventRecorder;
import net.osmand.plus.carlauncher.music.MusicManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Car Launcher Ayarlar Fragmenti.
 * Tum ayarlari gruplar halinde gosterir ve yonetir.
 */
public class CarLauncherSettingsFragment extends PreferenceFragmentCompat {

    public static final String TAG = "CarLauncherSettingsFragment";

    private CarLauncherSettings settings;
    
    private androidx.activity.result.ActivityResultLauncher<Intent> importVoiceModelLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        importVoiceModelLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importVoiceModelFromUri(uri);
                    }
                }
            }
        );
    }
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName("car_launcher_prefs");
        setPreferencesFromResource(R.xml.carlauncher_prefs, rootKey);

        if (getContext() != null) {
            settings = CarLauncherSettings.getInstance(getContext());
        }

        setupAppearanceGeneralPrefs();
        setupAppearancePortraitPrefs();
        setupAppearanceLandscapePrefs();
        setupMusicPrefs();
        setupAutoLaunchPrefs();
        setupBackupPrefs();
        setupDockLandscapePrefs();
        setupDockPortraitPrefs();
        setupAssistantPrefs();
        setupHardwareDiagnosticPrefs();
        setupAboutPrefs();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHardwareDiagnosticPrefs();
    }

    private android.widget.LinearLayout splitContainer;
    private androidx.preference.PreferenceCategory currentActiveCategory;
    private final List<androidx.preference.PreferenceCategory> allCategories = new ArrayList<>();
    private View selectionHighlight;
    private android.widget.LinearLayout categoriesList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Super creates the RecyclerView for preferences default view
        View prefsView = super.onCreateView(inflater, container, savedInstanceState);
        if (prefsView == null) return null;

        // Determine Orientation
        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
             return createSplitLayout(prefsView);
        } else {
             return createSingleLayout(prefsView);
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    private View createTitleBar() {
        android.widget.RelativeLayout titleBar = new android.widget.RelativeLayout(getContext());
        titleBar.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        titleBar.setBackgroundColor(0xFF14141C); // Sleek Space Grey
        titleBar.setElevation(8f);

        // Title
        android.widget.TextView titleView = new android.widget.TextView(getContext());
        titleView.setText("Araç Ayarları");
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(22);
        titleView.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        titleView.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        
        android.widget.RelativeLayout.LayoutParams titleParams = new android.widget.RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        titleParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        titleBar.addView(titleView, titleParams);

        // Close Button
        android.widget.ImageButton closeBtn = new android.widget.ImageButton(getContext());
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        android.util.TypedValue outValue = new android.util.TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        closeBtn.setBackgroundResource(outValue.resourceId);
        closeBtn.setColorFilter(0xFFFFFFFF);
        closeBtn.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        closeBtn.setOnClickListener(v -> closeSettings());

        android.widget.RelativeLayout.LayoutParams btnParams = new android.widget.RelativeLayout.LayoutParams(
                dpToPx(56), dpToPx(56)); 
        btnParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        btnParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        btnParams.setMarginEnd(dpToPx(8));
        titleBar.addView(closeBtn, btnParams);

        return titleBar;
    }

    private View createSingleLayout(View prefsView) {
        prefsView.setBackgroundColor(0xFF0B0B0E);
        if (prefsView instanceof androidx.recyclerview.widget.RecyclerView) {
            androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) prefsView;
            rv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(16));
            rv.setClipToPadding(false);
        }

        android.widget.LinearLayout mainContainer = new android.widget.LinearLayout(getContext());
        mainContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainContainer.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        mainContainer.setFitsSystemWindows(true);

        mainContainer.addView(createTitleBar());

        android.widget.LinearLayout.LayoutParams prefsParams = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mainContainer.addView(prefsView, prefsParams);

        restoreAllCategories();
        return mainContainer;
    }

    private View createSplitLayout(View prefsView) {
        android.widget.LinearLayout mainContainer = new android.widget.LinearLayout(getContext());
        mainContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainContainer.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        mainContainer.setBackgroundColor(0xFF0B0B0E);

        mainContainer.addView(createTitleBar());

        splitContainer = new android.widget.LinearLayout(getContext());
        splitContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.LinearLayout.LayoutParams splitParams = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        splitContainer.setLayoutParams(splitParams);

        // --- Left Pane: Categories ---
        android.widget.ScrollView leftScroll = new android.widget.ScrollView(getContext());
        leftScroll.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0.3f));
        leftScroll.setBackgroundColor(0xFF14141C); // Sleek Space Grey
        
        categoriesList = new android.widget.LinearLayout(getContext());
        categoriesList.setOrientation(android.widget.LinearLayout.VERTICAL);
        categoriesList.setPadding(0, dpToPx(8), 0, dpToPx(24));
        leftScroll.addView(categoriesList);
        
        splitContainer.addView(leftScroll);
        
        // --- Divider ---
        View divider = new View(getContext());
        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                dpToPx(1), android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        divider.setBackgroundColor(0xFF222232);
        splitContainer.addView(divider);

        // --- Right Pane: Content ---
        android.widget.FrameLayout rightPane = new android.widget.FrameLayout(getContext());
        rightPane.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0.7f));
        rightPane.setBackgroundColor(0xFF0B0B0E);
        
        if (prefsView.getParent() != null) {
            ((android.view.ViewGroup)prefsView.getParent()).removeView(prefsView);
        }
        
        if (prefsView instanceof androidx.recyclerview.widget.RecyclerView) {
            androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) prefsView;
            rv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(16));
            rv.setClipToPadding(false);
            rv.setBackgroundColor(0xFF0B0B0E);
        }
        rightPane.addView(prefsView);
        splitContainer.addView(rightPane);

        mainContainer.addView(splitContainer);

        setupCategoriesList();
        return mainContainer;
    }

    private void restoreAllCategories() {
         for (androidx.preference.PreferenceCategory cat : allCategories) {
             cat.setVisible(true);
         }
    }

    private void setupCategoriesList() {
        allCategories.clear();
        categoriesList.removeAllViews();
        
        androidx.preference.PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) return;
        
        int count = screen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = screen.getPreference(i);
            if (p instanceof androidx.preference.PreferenceCategory) {
                androidx.preference.PreferenceCategory cat = (androidx.preference.PreferenceCategory) p;
                allCategories.add(cat);
                addCategoryToMenu(cat);
                
                // Sağ panelde (Detay) bu Kategori başlığının gereksiz yer/padding kaplamaması için siliyoruz.
                // Sol menüye (Master) başlık ve ikon kopyalandığı için orası etkilenmez.
                cat.setTitle(null);
                cat.setIcon(null);
                cat.setIconSpaceReserved(false);
                cat.setLayoutResource(R.layout.empty_preference_category);
            }
        }
        
        // Select first default
        if (!allCategories.isEmpty()) {
            selectCategory(allCategories.get(0));
        }
    }

    private void addCategoryToMenu(androidx.preference.PreferenceCategory cat) {
        android.widget.TextView item = new android.widget.TextView(getContext());
        item.setText(cat.getTitle());
        item.setTextColor(0xFF8E8E93);
        item.setTextSize(15);
        item.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        item.setTag(cat);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        item.setMaxLines(2);
        item.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            item.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        }
        
        android.graphics.drawable.Drawable icon = cat.getIcon();
        if (icon != null) {
            android.graphics.drawable.Drawable wrappedIcon = androidx.core.graphics.drawable.DrawableCompat.wrap(icon.mutate());
            wrappedIcon.setBounds(0, 0, dpToPx(24), dpToPx(24));
            androidx.core.graphics.drawable.DrawableCompat.setTint(wrappedIcon, 0xFF8E8E93);
            item.setCompoundDrawables(wrappedIcon, null, null, null);
            item.setCompoundDrawablePadding(dpToPx(8));
        }

        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        item.setLayoutParams(lp);

        item.setOnClickListener(v -> selectCategory(cat));
        categoriesList.addView(item);
    }
    
    private void selectCategory(androidx.preference.PreferenceCategory target) {
        // Toggle Visibility
        for (androidx.preference.PreferenceCategory cat : allCategories) {
            cat.setVisible(cat == target);
        }
        
        // Update Menu UI (Highlight with HSL blue gradient/borders)
        for (int i = 0; i < categoriesList.getChildCount(); i++) {
            View child = categoriesList.getChildAt(i);
            if (child instanceof android.widget.TextView) {
                android.widget.TextView tv = (android.widget.TextView) child;
                android.graphics.drawable.Drawable[] drawables = tv.getCompoundDrawables();
                
                if (tv.getTag() == target) {
                    tv.setTextColor(0xFF3D63FF);
                    tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                    if (drawables[0] != null) {
                        androidx.core.graphics.drawable.DrawableCompat.setTint(drawables[0], 0xFF3D63FF);
                    }
                    
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setCornerRadius(dpToPx(12));
                    gd.setColor(0x1E3D63FF); // 12% opacity bright blue
                    gd.setStroke(dpToPx(1), 0xFF3D63FF); // Solid premium blue border
                    tv.setBackground(gd);
                } else {
                    tv.setTextColor(0xFF8E8E93);
                    tv.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
                    if (drawables[0] != null) {
                        androidx.core.graphics.drawable.DrawableCompat.setTint(drawables[0], 0xFF8E8E93);
                    }
                    
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setCornerRadius(dpToPx(12));
                    gd.setColor(0x00000000);
                    tv.setBackground(gd);
                }
            }
        }
        
        currentActiveCategory = target;
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



    // ═══════════════════════════════════════════════════════════════
    // GÖRÜNÜM AYARLARI
    // ═══════════════════════════════════════════════════════════════

    private void setupAppearanceGeneralPrefs() {
        // Status Bar
        SwitchPreferenceCompat statusBarPref = findPreference(CarLauncherSettings.KEY_STATUS_BAR);
        if (statusBarPref != null) {
            statusBarPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean show = (Boolean) newValue;
                CarLauncherSettings.getInstance(requireContext()).setStatusBarVisible(show);
                applyStatusBarVisibility(show);
                return true;
            });
        }

        // Fast Boot
        SwitchPreferenceCompat fastBootPref = findPreference(CarLauncherSettings.KEY_FAST_BOOT);
        if (fastBootPref != null) {
            fastBootPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean fast = (Boolean) newValue;
                CarLauncherSettings.getInstance(requireContext()).setFastBootEnabled(fast);
                return true;
            });
        }

        androidx.preference.ListPreference startupScreenPref =
                findPreference(CarLauncherSettings.KEY_STARTUP_SCREEN);
        if (startupScreenPref != null) {
            startupScreenPref.setOnPreferenceChangeListener((preference, newValue) -> {
                CarLauncherSettings.getInstance(requireContext())
                        .setStartupScreen(String.valueOf(newValue));
                return true;
            });
        }

        SwitchPreferenceCompat desktopCyclePref =
                findPreference(CarLauncherSettings.KEY_DESKTOP_IN_MODE_CYCLE);
        if (desktopCyclePref != null) {
            desktopCyclePref.setOnPreferenceChangeListener((preference, newValue) -> {
                CarLauncherSettings.getInstance(requireContext())
                        .setDesktopInModeCycleEnabled((Boolean) newValue);
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
        
        setupLanguagePrefs();

        // ... removed widget settings that are now unused here

        // Yuzen yardimci buton
        SwitchPreferenceCompat floatingButtonPref = findPreference(CarLauncherSettings.KEY_FLOATING_BUTTON);
        if (floatingButtonPref != null) {
            floatingButtonPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean val = (Boolean) newValue;
                if (settings != null) {
                    settings.setFloatingButtonEnabled(val);
                }
                if (getContext() != null) {
                    CarFloatingButtonManager.getInstance(getContext()).updateButtonState();
                }
                return true;
            });
        }

        // Yuzen Buton Surekli GPS (Force GPS)
        SwitchPreferenceCompat floatingButtonForceGpsPref = findPreference(CarLauncherSettings.KEY_FLOATING_BUTTON_FORCE_GPS);
        if (floatingButtonForceGpsPref != null) {
            floatingButtonForceGpsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean val = (Boolean) newValue;
                if (settings != null) {
                    settings.setFloatingButtonForceGpsEnabled(val);
                }
                return true;
            });
        }

        // Yuzen Buton Boyutu
        SeekBarPreference floatingButtonSizePref = findPreference(CarLauncherSettings.KEY_FLOATING_BUTTON_SIZE);
        if (floatingButtonSizePref != null) {
            floatingButtonSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
                int val = (Integer) newValue;
                if (settings != null) {
                    settings.setFloatingButtonSize(val);
                }
                if (getContext() != null) {
                    // Boyut degisikligini anlik yansitmak icin butonu kapatip aciyoruz
                    CarFloatingButtonManager mgr = CarFloatingButtonManager.getInstance(getContext());
                    mgr.hideButton();
                    mgr.updateButtonState();
                }
                return true;
            });
        }

        // Gece karartma modu (Turkce karakter yok)
        androidx.preference.ListPreference nightDimModePref = findPreference(CarLauncherSettings.KEY_NIGHT_DIM_MODE);
        if (nightDimModePref != null) {
            nightDimModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                if (settings != null) {
                    settings.setNightDimMode(val);
                }
                if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                    ((net.osmand.plus.activities.MapActivity) getActivity()).applyNightDimMode();
                } else if (getContext() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.NIGHT_DIM_CHANGED");
                    getContext().sendBroadcast(intent);
                }
                return true;
            });
        }

        // Premium Parallax Intensity
        SeekBarPreference parallaxIntensityPref = findPreference(CarLauncherSettings.KEY_PARALLAX_INTENSITY);
        if (parallaxIntensityPref != null) {
            parallaxIntensityPref.setOnPreferenceChangeListener((preference, newValue) -> {
                int val = (Integer) newValue;
                if (settings != null) {
                    settings.setParallaxIntensity(val);
                }
                if (getActivity() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
                    getActivity().sendBroadcast(intent);
                }
                return true;
            });
        }

        // Premium Background Style
        androidx.preference.ListPreference backgroundStylePref = findPreference(CarLauncherSettings.KEY_BACKGROUND_STYLE);
        if (backgroundStylePref != null) {
            backgroundStylePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                if (settings != null) {
                    settings.setBackgroundStyle(val);
                }
                if (getActivity() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
                    getActivity().sendBroadcast(intent);
                }
                return true;
            });
        }

        // Yuzen harita (PiP) ayari (Turkce karakter yok)
        SwitchPreferenceCompat pipPref = findPreference(CarLauncherSettings.KEY_PIP_MODE);
        if (pipPref != null) {
            pipPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean val = (Boolean) newValue;
                if (settings != null) {
                    settings.setPipModeEnabled(val);
                }
                return true;
            });
        }
    }

    private void setupAppearancePortraitPrefs() {
        // Dikey modda sadece harita
        SwitchPreferenceCompat portraitMapOnlyPref = findPreference(CarLauncherSettings.KEY_PORTRAIT_MAP_ONLY);
        if (portraitMapOnlyPref != null) {
            portraitMapOnlyPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean val = (Boolean) newValue;
                if (settings != null) {
                    settings.setPortraitMapOnly(val);
                }
                if (getActivity() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
                    getActivity().sendBroadcast(intent);
                }
                return true;
            });
        }

        // Panel Genisleme Davranisi (Dikey)
        androidx.preference.ListPreference portraitExpansionPref = findPreference(CarLauncherSettings.KEY_PORTRAIT_EXPANSION);
        if (portraitExpansionPref != null) {
            portraitExpansionPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                if (settings != null) {
                    settings.setPortraitExpansion(val);
                }
                if (getActivity() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
                    getActivity().sendBroadcast(intent);
                }
                return true;
            });
        }
    }

    private void setupAppearanceLandscapePrefs() {
        // Widget Paneli Konumu
        androidx.preference.ListPreference panelPositionPref = findPreference(CarLauncherSettings.KEY_WIDGET_PANEL_POSITION);
        if (panelPositionPref != null) {
            panelPositionPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                if (settings != null) {
                    settings.setWidgetPanelPosition(val);
                }
                if (getActivity() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
                    getActivity().sendBroadcast(intent);
                }
                return true;
            });
        }

        // Panel Genisleme Davranisi (Yatay)
        androidx.preference.ListPreference landscapeExpansionPref = findPreference(CarLauncherSettings.KEY_LANDSCAPE_EXPANSION);
        if (landscapeExpansionPref != null) {
            landscapeExpansionPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                if (settings != null) {
                    settings.setLandscapeExpansion(val);
                }
                if (getActivity() != null) {
                    Intent intent = new Intent("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
                    getActivity().sendBroadcast(intent);
                }
                return true;
            });
        }
    }

    private void applyStatusBarVisibility(boolean show) {
        if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
            ((net.osmand.plus.activities.MapActivity) getActivity()).applyStatusBarVisibility();
        }
    }

    private void setupLanguagePrefs() {
        androidx.preference.ListPreference langPref = findPreference("car_launcher_language");
        if (langPref != null) {
            net.osmand.plus.OsmandApplication app = (net.osmand.plus.OsmandApplication) getContext().getApplicationContext();
            
            // Secenekleri ayarla
            langPref.setEntries(new CharSequence[]{"Sistem (System)", "Türkçe", "English", "Deutsch"});
            langPref.setEntryValues(new CharSequence[]{"", "tr", "en", "de"});
            
            // Mevcut degeri set et
            String current = app.getSettings().PREFERRED_LOCALE.get();
            langPref.setValue(current);
            
            langPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                app.getSettings().PREFERRED_LOCALE.set(val);
                
                // Dil guncelle ve yeniden baslat
                Toast.makeText(getContext(), "Dil güncelleniyor...", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    Intent intent = getActivity().getIntent();
                    getActivity().finish();
                    startActivity(intent);
                }
                return true;
            });
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
            updateEqualizerSummary(eqPref);
            eqPref.setOnPreferenceClickListener(preference -> {
                showEqualizerOptions(eqPref);
                return true;
            });
        }

        // Muzikleri Yeniden Tara (Turkce karakter yok)
        Preference scanMusicPref = findPreference("car_launcher_scan_music");
        if (scanMusicPref != null) {
            scanMusicPref.setOnPreferenceClickListener(preference -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Muzik taramasi baslatildi...", Toast.LENGTH_SHORT).show();
                    MusicManager.getInstance(getContext()).getRepository().scanMusic((tracks, folders, artists) -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Tarama tamamlandi. Bulunan sarki: " + tracks.size(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }
                return true;
            });
        }

        // Ambians gorsellestirici ayari (Turkce karakter yok)
        SwitchPreferenceCompat ambianceVisualizerPref = findPreference(CarLauncherSettings.KEY_AMBIANCE_VISUALIZER);
        if (ambianceVisualizerPref != null) {
            ambianceVisualizerPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean val = (Boolean) newValue;
                if (settings != null) {
                    settings.setAmbianceVisualizerEnabled(val);
                }
                return true;
            });
        }

        // Otomatik Muzik Oynat
        SwitchPreferenceCompat autoPlayPref = findPreference(CarLauncherSettings.KEY_AUTO_PLAY_MUSIC);
        if (autoPlayPref != null) {
            autoPlayPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enable = (Boolean) newValue;
                CarLauncherSettings.getInstance(requireContext()).setAutoPlayMusicEnabled(enable);
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
            
            Preference pref = findPreference(CarLauncherSettings.KEY_MUSIC_APP);
            if (pref != null) updateMusicAppSummary(pref);
        }).show();
    }

    private void showEqualizerOptions(@NonNull Preference pref) {
        if (getContext() == null || settings == null) return;
        String[] options = {
                getString(R.string.car_settings_equalizer_auto),
                getString(R.string.car_settings_equalizer_choose_app),
                getString(R.string.car_settings_equalizer_custom_intent)
        };
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.car_pref_title_equalizer)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        settings.setEqualizerApp(null);
                        settings.setEqualizerIntent(null);
                        updateEqualizerSummary(pref);
                    } else if (which == 1) {
                        AppPickerDialog picker = new AppPickerDialog(getContext(), false,
                                (packageName, name, icon) -> {
                                    settings.setEqualizerApp(packageName);
                                    settings.setEqualizerIntent(null);
                                    updateEqualizerSummary(pref);
                                });
                        picker.setActivePackage(settings.getEqualizerApp());
                        picker.show();
                    } else {
                        showEqualizerIntentDialog(pref);
                    }
                }).show();
    }

    private void showEqualizerIntentDialog(@NonNull Preference pref) {
        if (getContext() == null || settings == null) return;
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint(R.string.car_settings_equalizer_intent_hint);
        input.setText(settings.getEqualizerIntent());
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.car_settings_equalizer_custom_intent)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    settings.setEqualizerIntent(value.isEmpty() ? null : value);
                    if (!value.isEmpty()) settings.setEqualizerApp(null);
                    updateEqualizerSummary(pref);
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void updateEqualizerSummary(@NonNull Preference pref) {
        if (getContext() == null || settings == null) return;
        String intentValue = settings.getEqualizerIntent();
        if (!android.text.TextUtils.isEmpty(intentValue)) {
            pref.setSummary(getString(R.string.car_settings_equalizer_intent_summary, intentValue));
            return;
        }
        String packageName = settings.getEqualizerApp();
        if (android.text.TextUtils.isEmpty(packageName)) {
            pref.setSummary(R.string.car_settings_equalizer_auto_summary);
            return;
        }
        try {
            PackageManager pm = getContext().getPackageManager();
            pref.setSummary(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
        } catch (Exception e) {
            pref.setSummary(packageName);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DOCK AYARLARI
    // ═══════════════════════════════════════════════════════════════

    private void setupDockLandscapePrefs() {
        androidx.preference.ListPreference dockPosPref = findPreference(CarLauncherSettings.KEY_DOCK_POSITION);
        if (dockPosPref != null) {
            dockPosPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                if (settings != null) {
                    settings.setDockPosition(val);
                }
                if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                    ((net.osmand.plus.activities.MapActivity) getActivity()).checkAndRefreshDockFragmentIfNeeded();
                }
                return true;
            });
        }

        SeekBarPreference dockSizePref = findPreference(CarLauncherSettings.KEY_DOCK_SIZE);
        if (dockSizePref != null) {
            dockSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
                int val = (Integer) newValue;
                if (settings != null) {
                    settings.setDockSize(val);
                }
                if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                    ((net.osmand.plus.activities.MapActivity) getActivity()).checkAndRefreshDockFragmentIfNeeded();
                }
                return true;
            });
        }

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

    private void setupDockPortraitPrefs() {
        androidx.preference.ListPreference dockPosPref = findPreference(CarLauncherSettings.KEY_DOCK_POSITION_PORTRAIT);
        if (dockPosPref != null) {
            dockPosPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String val = (String) newValue;
                if (settings != null) {
                    settings.setDockPositionPortrait(val);
                }
                if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                    ((net.osmand.plus.activities.MapActivity) getActivity()).checkAndRefreshDockFragmentIfNeeded();
                }
                return true;
            });
        }

        SeekBarPreference dockSizePref = findPreference(CarLauncherSettings.KEY_DOCK_SIZE_PORTRAIT);
        if (dockSizePref != null) {
            dockSizePref.setOnPreferenceChangeListener((preference, newValue) -> {
                int val = (Integer) newValue;
                if (settings != null) {
                    settings.setDockSizePortrait(val);
                }
                if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                    ((net.osmand.plus.activities.MapActivity) getActivity()).checkAndRefreshDockFragmentIfNeeded();
                }
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
    private static final int RC_HARDWARE_LOG_EXPORT = 104;

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

    private static final int RC_IMPORT_VOICE_MODEL = 103;

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
            } else if (requestCode == RC_IMPORT_VOICE_MODEL) {
                importVoiceModelFromUri(uri);
            } else if (requestCode == RC_HARDWARE_LOG_EXPORT) {
                HardwareEventRecorder recorder = HardwareEventRecorder.getInstance(requireContext());
                recorder.exportToUri(uri, (success, errorMessage) -> {
                    if (!isAdded()) return;
                    int message = success
                            ? R.string.car_toast_hardware_exported
                            : R.string.car_toast_hardware_export_failed;
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        }

    }

    private void setupHardwareDiagnosticPrefs() {
        if (getContext() == null) {
            return;
        }
        HardwareEventRecorder recorder = HardwareEventRecorder.getInstance(getContext());

        SwitchPreferenceCompat recordingPref = findPreference("car_launcher_hardware_recording");
        if (recordingPref != null) {
            recordingPref.setPersistent(false);
            recordingPref.setChecked(recorder.isRecording());
            recordingPref.setOnPreferenceChangeListener((preference, newValue) -> {
                recorder.setRecording(Boolean.TRUE.equals(newValue));
                try {
                    Intent serviceIntent = new Intent(requireContext(),
                            net.osmand.plus.carlauncher.media.CarMediaService.class);
                    serviceIntent.setAction(net.osmand.plus.carlauncher.media.CarMediaService
                            .ACTION_DIAGNOSTIC_STATE_CHANGED);
                    requireContext().startService(serviceIntent);
                } catch (Exception e) {
                    recorder.record("MEDIA_SESSION",
                            "diagnostic_state_update_failed=" + e.getClass().getSimpleName());
                }
                preference.setSummary(getHardwareRecordingSummary(recorder));
                return true;
            });
        }

        Preference exportPref = findPreference("car_launcher_hardware_export");
        if (exportPref != null) {
            exportPref.setOnPreferenceClickListener(preference -> {
                if (!recorder.hasLogs()) {
                    Toast.makeText(requireContext(), R.string.car_toast_hardware_empty,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/plain");
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                        java.util.Locale.US).format(new java.util.Date());
                intent.putExtra(Intent.EXTRA_TITLE,
                        "CarLauncher_HardwareEvents_" + timestamp + ".log");
                try {
                    startActivityForResult(intent, RC_HARDWARE_LOG_EXPORT);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.car_toast_hardware_export_failed,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference clearPref = findPreference("car_launcher_hardware_clear");
        if (clearPref != null) {
            clearPref.setOnPreferenceClickListener(preference -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.car_dialog_hardware_clear_title)
                        .setMessage(R.string.car_dialog_hardware_clear_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                recorder.clear((success, errorMessage) -> {
                                    if (!isAdded()) return;
                                    if (success) {
                                        Toast.makeText(requireContext(),
                                                R.string.car_toast_hardware_cleared,
                                                Toast.LENGTH_SHORT).show();
                                        refreshHardwareDiagnosticPrefs();
                                    }
                                }))
                        .show();
                return true;
            });
        }
        refreshHardwareDiagnosticPrefs();
    }

    private void refreshHardwareDiagnosticPrefs() {
        if (getContext() == null) {
            return;
        }
        HardwareEventRecorder recorder = HardwareEventRecorder.getInstance(getContext());
        SwitchPreferenceCompat recordingPref = findPreference("car_launcher_hardware_recording");
        if (recordingPref != null) {
            recordingPref.setChecked(recorder.isRecording());
            recordingPref.setSummary(getHardwareRecordingSummary(recorder));
        }
        Preference exportPref = findPreference("car_launcher_hardware_export");
        if (exportPref != null) {
            exportPref.setEnabled(recorder.hasLogs());
        }
        Preference clearPref = findPreference("car_launcher_hardware_clear");
        if (clearPref != null) {
            clearPref.setEnabled(recorder.hasLogs());
        }
    }

    private CharSequence getHardwareRecordingSummary(HardwareEventRecorder recorder) {
        if (!recorder.isRecording()) {
            return getString(R.string.car_pref_summary_hardware_recording_off);
        }
        long bytes = recorder.getLogSizeBytes();
        String size;
        if (bytes >= 1024L * 1024L) {
            size = String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            size = String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return getString(R.string.car_pref_summary_hardware_recording_on, size);
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

        Preference bootStatsPref = findPreference("car_launcher_boot_stats");
        if (bootStatsPref != null && getContext() != null) {
            String summary = CarLauncherInitManager.getInstance().getFormattedStatsSummary(getContext());
            bootStatsPref.setSummary(summary);
            bootStatsPref.setOnPreferenceClickListener(preference -> {
                showBootStatsDialog();
                return true;
            });
        }

        Preference githubPref = findPreference("car_launcher_github");
        if (githubPref != null) {
            githubPref.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/poolsoft/OsmAnd/tree/right-panel-plugin"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Tarayıcı açılamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference checkUpdatePref = findPreference("car_launcher_check_updates");
        if (checkUpdatePref != null) {
            checkUpdatePref.setOnPreferenceClickListener(preference -> {
                if (getContext() != null) {
                    UpdaterHelper.checkUpdates(getContext(), true);
                }
                return true;
            });
        }
    }

    private void showBootStatsDialog() {
        if (getContext() == null) return;
        String details = CarLauncherInitManager.getInstance().getFormattedStatsDetails(getContext());
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(getString(net.osmand.plus.R.string.car_dialog_boot_stats_title))
                .setMessage(details)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void setupAssistantPrefs() {
        Preference importModelPref = findPreference("car_launcher_import_voice_model");
        if (importModelPref != null) {
            importModelPref.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    // Vendor file managers often expose ZIP files as octet-stream
                    // or with no useful MIME type.
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                            "application/zip", "application/x-zip-compressed",
                            "application/octet-stream"
                    });
                    if (importVoiceModelLauncher != null) {
                        importVoiceModelLauncher.launch(intent);
                    } else {
                        startActivityForResult(intent, RC_IMPORT_VOICE_MODEL);
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Dosya seçici açılamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    private void importVoiceModelFromUri(Uri uri) {
        if (getContext() == null) return;
        Context appContext = getContext().getApplicationContext();
        Toast.makeText(appContext, "Model dosyası kopyalanıyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show();
        
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            File storageDir = appContext.getExternalFilesDir(null);
            if (storageDir == null) {
                storageDir = appContext.getFilesDir();
            }
            File targetDir = new File(storageDir, "vosk-model-tr");
            File stagingDir = new File(storageDir, "vosk-model-tr.installing");
            File backupDir = new File(storageDir, "vosk-model-tr.backup");
            File tempZip = new File(storageDir, "vosk-model-tr-temp.zip");
            File tempExtractDir = new File(storageDir, "vosk-model-temp-extract");
            
            try (java.io.InputStream fis = appContext.getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tempZip)) {
                if (fis == null) {
                    throw new java.io.IOException("Secilen dosya okunamadi");
                }
                
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
                if (tempZip.length() < 1024L * 1024L) {
                    throw new java.io.IOException("Secilen dosya gecerli bir Vosk model ZIP'i degil");
                }
                
                if (tempExtractDir.exists()) {
                    deleteRecursive(tempExtractDir);
                }
                tempExtractDir.mkdirs();
                
                unzip(tempZip, tempExtractDir);
                
                File actualModelDir = findModelDirRecursive(tempExtractDir);
                if (actualModelDir == null) {
                    throw new java.io.IOException("ZIP icinde gecerli Vosk modeli bulunamadi");
                }
                deleteRecursive(stagingDir);
                copyDirectory(actualModelDir, stagingDir);
                if (!isModelDirectoryValid(stagingDir)) {
                    throw new java.io.IOException("Model eksik: am/final.mdl, conf/model.conf veya graph bulunamadi");
                }
                deleteRecursive(backupDir);
                if (targetDir.exists() && !targetDir.renameTo(backupDir)) {
                    throw new java.io.IOException("Mevcut model yedeklenemedi");
                }
                if (!stagingDir.renameTo(targetDir)) {
                    copyDirectory(stagingDir, targetDir);
                    deleteRecursive(stagingDir);
                }
                if (!isModelDirectoryValid(targetDir)) {
                    deleteRecursive(targetDir);
                    backupDir.renameTo(targetDir);
                    throw new java.io.IOException("Model hedef klasore kurulamadi");
                }
                deleteRecursive(backupDir);
                appContext.getSharedPreferences("vosk_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("vosk_model_installed", false).apply();
                
                if (tempZip.exists()) {
                    tempZip.delete();
                }
                if (tempExtractDir.exists()) {
                    deleteRecursive(tempExtractDir);
                }
                
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(appContext, "Ses modeli başarıyla kuruldu! Servis yeniden başlatılıyor...", Toast.LENGTH_LONG).show();
                    restartVoiceService(appContext);
                });
                
            } catch (Exception e) {
                android.util.Log.e("CarLauncherSettings", "Model kopyalama/unzip hatası", e);
                if (backupDir.exists()) {
                    deleteRecursive(targetDir);
                    backupDir.renameTo(targetDir);
                }
                if (tempZip.exists()) tempZip.delete();
                if (tempExtractDir.exists()) deleteRecursive(tempExtractDir);
                if (stagingDir.exists()) deleteRecursive(stagingDir);
                
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(appContext, "Ses modeli yüklenemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private File findModelDirRecursive(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }
        if (isModelDirectoryValid(dir)) {
            return dir;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    File found = findModelDirRecursive(child);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private boolean isModelDirectoryValid(File dir) {
        return dir != null
                && new File(dir, "am/final.mdl").isFile()
                && new File(dir, "conf/model.conf").isFile()
                && new File(dir, "graph").isDirectory();
    }

    private void copyDirectory(File source, File destination) throws java.io.IOException {
        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new java.io.IOException("Klasor olusturulamadi: " + destination.getAbsolutePath());
            }
            File[] children = source.listFiles();
            if (children == null) {
                throw new java.io.IOException("Klasor okunamadi: " + source.getAbsolutePath());
            }
            for (File child : children) {
                copyDirectory(child, new File(destination, child.getName()));
            }
            return;
        }
        try (java.io.FileInputStream input = new java.io.FileInputStream(source);
             java.io.FileOutputStream output = new java.io.FileOutputStream(destination)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    private void restartVoiceService() {
        if (getContext() == null) return;
        restartVoiceService(getContext().getApplicationContext());
    }

    private void restartVoiceService(Context context) {
        Intent intent = new Intent(context, net.osmand.plus.carlauncher.voice.VoiceCommandService.class);
        if (net.osmand.plus.carlauncher.voice.VoiceCommandService.isServiceRunning) {
            context.stopService(intent);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void unzip(File zipFile, File targetDirectory) throws java.io.IOException {
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
            new java.io.BufferedInputStream(new java.io.FileInputStream(zipFile)));
        try {
            java.util.zip.ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                String targetPath = targetDirectory.getCanonicalPath() + File.separator;
                if (!file.getCanonicalPath().startsWith(targetPath)) {
                    throw new java.io.IOException("Gecersiz ZIP girdisi: " + ze.getName());
                }
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new java.io.IOException("Klasor olusturulamadi: " + dir.getAbsolutePath());
                }
                if (ze.isDirectory()) {
                    continue;
                }
                java.io.FileOutputStream fout = new java.io.FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) {
            return;
        }
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}
