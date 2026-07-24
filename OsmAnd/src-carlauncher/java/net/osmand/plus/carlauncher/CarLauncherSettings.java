package net.osmand.plus.carlauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Car Launcher ayarlarini yoneten Singleton repository sinifi.
 * Tum ayarlar bellekte cache'lenir, disk I/O sadece setter cagirildiginda yapilir.
 * Uygulama yasam dongusu boyunca tek instance kullanilir.
 */
public class CarLauncherSettings {

    private static volatile CarLauncherSettings instance;
    private static final String PREF_NAME = "car_launcher_prefs";

    // --- Ayar Anahtarlari (Keys) ---

    // Widget Keys
    private static final String KEY_WIDGET_ORDER = "widget_order";
    private static final String KEY_WIDGET_VISIBILITY_PREFIX = "widget_visible_";
    public static final String KEY_WIDGET_DISPLAY_MODE = "widget_display_mode"; // 0: List, 1: Paged

    // Grid Slot Keys - v2
    public static final String KEY_WIDGET_SLOTS_PORTRAIT = "widget_slot_count_portrait_v2";
    public static final String KEY_WIDGET_SLOTS_LANDSCAPE = "widget_slot_count_landscape_v2";

    // Panel Resize Keys
    public static final String KEY_WIDGET_PANEL_WIDTH_PERCENT = "widget_panel_width_percent";
    public static final String KEY_WIDGET_PANEL_HEIGHT_PORTRAIT = "widget_panel_height_portrait";

    // Layout Mode (Classic/Metro)
    public static final String KEY_WIDGET_LAYOUT_MODE = "widget_layout_mode_v2";

    // Appearance Keys - Genel
    public static final String KEY_STATUS_BAR = "car_launcher_status_bar";
    public static final String KEY_FAST_BOOT = "car_launcher_fast_boot";

    public static final String KEY_DARK_THEME = "car_launcher_dark_theme";
    public static final String KEY_FLOATING_BUTTON = "car_launcher_floating_button";
    public static final String KEY_NIGHT_DIM_MODE = "car_launcher_night_dim_mode";
    public static final String KEY_PARALLAX_INTENSITY = "car_launcher_parallax_intensity";
    public static final String KEY_BACKGROUND_STYLE = "car_launcher_background_style";

    // Appearance Keys - Dikey Ekran
    public static final String KEY_PORTRAIT_MAP_ONLY = "car_launcher_portrait_map_only";
    public static final String KEY_PORTRAIT_EXPANSION = "car_launcher_portrait_expansion"; // "expand_up", "expand_down"

    // Appearance Keys - Yatay Ekran
    public static final String KEY_WIDGET_PANEL_POSITION = "widget_panel_position"; // "left", "right"
    public static final String KEY_LANDSCAPE_EXPANSION = "car_launcher_landscape_expansion"; // "expand_right", "expand_left"

    // Eski anahtar (migrasyon icin korunuyor)
    public static final String KEY_EXPANSION_BEHAVIOR = "car_launcher_expansion_behavior";

    // Music Keys
    public static final String KEY_MUSIC_APP = "car_launcher_music_app";
    public static final String KEY_AMBIANCE_VISUALIZER = "car_launcher_ambiance_visualizer";

    // Dock Keys - Yatay
    public static final String KEY_DOCK_POSITION = "car_launcher_dock_position"; // "bottom", "left", "right"
    public static final String KEY_DOCK_STYLE = "car_launcher_dock_style"; // "glass", "solid", "transparent"
    public static final String KEY_DOCK_SIZE = "car_launcher_dock_size"; // 0-100

    // Dock Keys - Dikey (yeni)
    public static final String KEY_DOCK_POSITION_PORTRAIT = "car_launcher_dock_position_portrait"; // "bottom", "left", "right"
    public static final String KEY_DOCK_SIZE_PORTRAIT = "car_launcher_dock_size_portrait"; // 0-100

    public static final String KEY_MAX_SHORTCUTS = "car_launcher_max_shortcuts";

    // Widget Panel Keys
    public static final String KEY_WIDGET_CARD_STYLE = "widget_card_style"; // "modern", "classic", "minimal"

    // PiP, Floating Button
    public static final String KEY_PIP_MODE = "car_launcher_pip_mode";
    public static final String KEY_FLOATING_BUTTON_FORCE_GPS = "car_launcher_floating_button_force_gps";
    public static final String KEY_FLOATING_BUTTON_SIZE = "car_launcher_floating_button_size";

    // Auto Launch
    public static final String KEY_AUTOLAUNCH_ENABLE_PREFIX = "autolaunch_enable_";
    public static final String KEY_AUTOLAUNCH_PKG_PREFIX = "autolaunch_pkg_";
    public static final String KEY_AUTOLAUNCH_NAME_PREFIX = "autolaunch_name_";
    public static final String KEY_AUTO_START_ON_BOOT = "car_launcher_auto_start_on_boot";

    // Weather
    private static final String KEY_WEATHER_ENABLED = "car_launcher_weather_enabled";

    // Auto Play Music
    public static final String KEY_AUTO_PLAY_MUSIC = "car_launcher_auto_play_music";

    // Widget Handle
    public static final String KEY_WIDGET_HANDLE_VERTICAL_BIAS = "widget_handle_vertical_bias";

    // Generic Widget Config
    private static final String KEY_WIDGET_CONFIG_PREFIX = "widget_config_";

    // Migrasyon flag
    private static final String KEY_MIGRATION_V2_DONE = "car_launcher_migration_v2_done";

    // --- Bellekte tutulan cache alanlari ---
    private boolean cStatusBarVisible;
    private boolean cFastBoot;
    private boolean cDarkTheme;
    private boolean cPortraitMapOnly;
    private boolean cFloatingButton;
    private boolean cFloatingButtonForceGps;
    private int cFloatingButtonSize;
    private String cNightDimMode;
    private int cParallaxIntensity;
    private String cBackgroundStyle;
    private boolean cPipMode;
    private String cLandscapeExpansion;
    private String cPortraitExpansion;
    private String cWidgetPanelPosition;
    private String cWidgetCardStyle;
    private String cDockPosition;
    private String cDockStyle;
    private int cDockSize;
    private String cDockPositionPortrait;
    private int cDockSizePortrait;
    private int cMaxShortcuts;
    
    private boolean cAutoStartOnBoot;
    private String cMusicApp;
    private boolean cAmbianceVisualizer;
    private boolean cAutoPlayMusic;
    private boolean cWeatherEnabled;
    private float cWidgetPanelWidthPercent;
    private float cWidgetPanelHeightPortrait;
    private float cWidgetHandleVerticalBias;
    private int cPortraitSlotCount;
    private int cLandscapeSlotCount;
    private boolean cMetroMode;

    private final SharedPreferences prefs;

    // --- Singleton ---

    private CarLauncherSettings(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        migrateIfNeeded();
        loadAll();
    }

    public static CarLauncherSettings getInstance(Context context) {
        if (instance == null) {
            synchronized (CarLauncherSettings.class) {
                if (instance == null) {
                    instance = new CarLauncherSettings(context);
                }
            }
        }
        return instance;
    }

    /**
     * Eski ayar degerlerini yeni anahtarlara tasir (sadece 1 kere calisir).
     */
    private void migrateIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATION_V2_DONE, false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        // Eski expansion_behavior degerini yeni landscape anahtarina tasi
        String oldExpansion = prefs.getString(KEY_EXPANSION_BEHAVIOR, null);
        if (oldExpansion != null && !prefs.contains(KEY_LANDSCAPE_EXPANSION)) {
            if ("swap".equals(oldExpansion)) {
                editor.putString(KEY_LANDSCAPE_EXPANSION, "expand_right");
            } else if ("fixed".equals(oldExpansion)) {
                editor.putString(KEY_LANDSCAPE_EXPANSION, "expand_left");
            }
        }

        // Eski widget_panel_position "bottom" ise varsayilana dondur (yatayda artik bottom yok)
        String oldPanelPos = prefs.getString(KEY_WIDGET_PANEL_POSITION, null);
        if ("bottom".equals(oldPanelPos)) {
            editor.putString(KEY_WIDGET_PANEL_POSITION, "left");
        }

        // Dikey dock ayarlari yoksa yatay degerlerden kopyala
        if (!prefs.contains(KEY_DOCK_POSITION_PORTRAIT)) {
            editor.putString(KEY_DOCK_POSITION_PORTRAIT, "bottom");
        }
        if (!prefs.contains(KEY_DOCK_SIZE_PORTRAIT)) {
            editor.putInt(KEY_DOCK_SIZE_PORTRAIT, prefs.getInt(KEY_DOCK_SIZE, 50));
        }

        editor.putBoolean(KEY_MIGRATION_V2_DONE, true);
        editor.apply();
    }

    /**
     * Tum ayarlari SharedPreferences'tan bellege yukler (sadece 1 kere calisir).
     */
    private void loadAll() {
        cStatusBarVisible = prefs.getBoolean(KEY_STATUS_BAR, true);
        cFastBoot = prefs.getBoolean(KEY_FAST_BOOT, true);
        cDarkTheme = prefs.getBoolean(KEY_DARK_THEME, true);
        cPortraitMapOnly = prefs.getBoolean(KEY_PORTRAIT_MAP_ONLY, false);
        cFloatingButton = prefs.getBoolean(KEY_FLOATING_BUTTON, false);
        cFloatingButtonForceGps = prefs.getBoolean(KEY_FLOATING_BUTTON_FORCE_GPS, false);
        cFloatingButtonSize = prefs.getInt(KEY_FLOATING_BUTTON_SIZE, 86);
        cNightDimMode = prefs.getString(KEY_NIGHT_DIM_MODE, "osmand");
        cParallaxIntensity = prefs.getInt(KEY_PARALLAX_INTENSITY, 20);
        cBackgroundStyle = prefs.getString(KEY_BACKGROUND_STYLE, "modern");
        cPipMode = prefs.getBoolean(KEY_PIP_MODE, true);

        // Dikey/Yatay genisletme
        cLandscapeExpansion = prefs.getString(KEY_LANDSCAPE_EXPANSION, "expand_right");
        cPortraitExpansion = prefs.getString(KEY_PORTRAIT_EXPANSION, "expand_up");
        cWidgetPanelPosition = prefs.getString(KEY_WIDGET_PANEL_POSITION, "left");
        cWidgetCardStyle = prefs.getString(KEY_WIDGET_CARD_STYLE, "modern");

        // Dock - Yatay
        cDockPosition = prefs.getString(KEY_DOCK_POSITION, "bottom");
        cDockStyle = prefs.getString(KEY_DOCK_STYLE, "glass");
        cDockSize = prefs.getInt(KEY_DOCK_SIZE, 50);

        // Dock - Dikey
        cDockPositionPortrait = prefs.getString(KEY_DOCK_POSITION_PORTRAIT, "bottom");
        cDockSizePortrait = prefs.getInt(KEY_DOCK_SIZE_PORTRAIT, 50);

        cMaxShortcuts = prefs.getInt(KEY_MAX_SHORTCUTS, 6);

        // Muzik
        cAutoPlayMusic = prefs.getBoolean(KEY_AUTO_PLAY_MUSIC, false);
        cAmbianceVisualizer = prefs.getBoolean(KEY_AMBIANCE_VISUALIZER, true);
        
        cAutoStartOnBoot = prefs.getBoolean(KEY_AUTO_START_ON_BOOT, false);
        cMusicApp = prefs.getString(KEY_MUSIC_APP, "internal");

        // Hava Durumu
        cWeatherEnabled = prefs.getBoolean(KEY_WEATHER_ENABLED, true);

        // Panel boyutlari
        cWidgetPanelWidthPercent = prefs.getFloat(KEY_WIDGET_PANEL_WIDTH_PERCENT, 0.4f);
        cWidgetPanelHeightPortrait = prefs.getFloat(KEY_WIDGET_PANEL_HEIGHT_PORTRAIT, 0.30f);
        cWidgetHandleVerticalBias = prefs.getFloat(KEY_WIDGET_HANDLE_VERTICAL_BIAS, 0.5f);

        // Grid slot
        cPortraitSlotCount = prefs.getInt(KEY_WIDGET_SLOTS_PORTRAIT, 3);
        cLandscapeSlotCount = prefs.getInt(KEY_WIDGET_SLOTS_LANDSCAPE, 3);

        // Metro mode
        cMetroMode = "metro".equals(prefs.getString(KEY_WIDGET_LAYOUT_MODE, "classic"));
    }

    // =====================================================================
    // Widget Settings (Dinamik — cache'lenemez, key degisken)
    // =====================================================================

    public List<String> getWidgetOrder(List<String> defaultOrder) {
        String savedOrder = prefs.getString(KEY_WIDGET_ORDER, null);
        if (savedOrder == null) {
            return defaultOrder;
        }
        return new ArrayList<>(Arrays.asList(TextUtils.split(savedOrder, ",")));
    }

    public void setWidgetOrder(List<String> order) {
        prefs.edit().putString(KEY_WIDGET_ORDER, TextUtils.join(",", order)).apply();
    }

    public boolean isWidgetVisible(String widgetKey, boolean defaultValue) {
        return prefs.getBoolean(KEY_WIDGET_VISIBILITY_PREFIX + widgetKey, defaultValue);
    }

    public void setWidgetVisible(String widgetKey, boolean visible) {
        prefs.edit().putBoolean(KEY_WIDGET_VISIBILITY_PREFIX + widgetKey, visible).apply();
    }

    public int getWidgetDisplayMode() {
        try {
            String val = prefs.getString(KEY_WIDGET_DISPLAY_MODE, "0");
            return Integer.parseInt(val);
        } catch (Exception e) {
            return 0;
        }
    }

    public void setWidgetDisplayMode(int mode) {
        prefs.edit().putString(KEY_WIDGET_DISPLAY_MODE, String.valueOf(mode)).apply();
    }

    // =====================================================================
    // Grid Slot Settings (Cache)
    // =====================================================================

    public int getPortraitSlotCount() {
        return cPortraitSlotCount;
    }

    public void setPortraitSlotCount(int count) {
        this.cPortraitSlotCount = count;
        prefs.edit().putInt(KEY_WIDGET_SLOTS_PORTRAIT, count).apply();
    }

    public int getLandscapeSlotCount() {
        return cLandscapeSlotCount;
    }

    public void setLandscapeSlotCount(int count) {
        this.cLandscapeSlotCount = count;
        prefs.edit().putInt(KEY_WIDGET_SLOTS_LANDSCAPE, count).apply();
    }

    public float getWidgetPanelWidthPercent() {
        return cWidgetPanelWidthPercent;
    }

    public void setWidgetPanelWidthPercent(float percent) {
        this.cWidgetPanelWidthPercent = percent;
        prefs.edit().putFloat(KEY_WIDGET_PANEL_WIDTH_PERCENT, percent).apply();
    }

    // =====================================================================
    // Portrait Panel Height (Cache)
    // =====================================================================

    public float getWidgetPanelHeightPortrait() {
        return cWidgetPanelHeightPortrait;
    }

    public void setWidgetPanelHeightPortrait(float percent) {
        float clamped = Math.max(0.1f, Math.min(0.7f, percent));
        this.cWidgetPanelHeightPortrait = clamped;
        prefs.edit().putFloat(KEY_WIDGET_PANEL_HEIGHT_PORTRAIT, clamped).apply();
    }

    public float getWidgetHandleVerticalBias() {
        return cWidgetHandleVerticalBias;
    }

    public void setWidgetHandleVerticalBias(float bias) {
        this.cWidgetHandleVerticalBias = bias;
        prefs.edit().putFloat(KEY_WIDGET_HANDLE_VERTICAL_BIAS, bias).apply();
    }

    public boolean isMetroMode() {
        return cMetroMode;
    }

    public void setMetroMode(boolean isMetro) {
        this.cMetroMode = isMetro;
        prefs.edit().putString(KEY_WIDGET_LAYOUT_MODE, isMetro ? "metro" : "classic").apply();
    }

    // =====================================================================
    // Appearance Settings - Genel (Cache)
    // =====================================================================

    public boolean isStatusBarVisible() {
        return cStatusBarVisible;
    }

    public void setStatusBarVisible(boolean visible) {
        this.cStatusBarVisible = visible;
        prefs.edit().putBoolean(KEY_STATUS_BAR, visible).apply();
    }

    public boolean isFastBootEnabled() {
        return cFastBoot;
    }

    public void setFastBootEnabled(boolean enabled) {
        this.cFastBoot = enabled;
        prefs.edit().putBoolean(KEY_FAST_BOOT, enabled).apply();
    }

    public boolean isDarkTheme() {
        return cDarkTheme;
    }

    public void setDarkTheme(boolean dark) {
        this.cDarkTheme = dark;
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply();
    }

    public boolean isFloatingButtonEnabled() {
        return cFloatingButton;
    }

    public void setFloatingButtonEnabled(boolean enabled) {
        this.cFloatingButton = enabled;
        prefs.edit().putBoolean(KEY_FLOATING_BUTTON, enabled).apply();
    }

    public boolean isFloatingButtonForceGpsEnabled() {
        return cFloatingButtonForceGps;
    }

    public void setFloatingButtonForceGpsEnabled(boolean enabled) {
        this.cFloatingButtonForceGps = enabled;
        prefs.edit().putBoolean(KEY_FLOATING_BUTTON_FORCE_GPS, enabled).apply();
    }


    public int getFloatingButtonSize() {
        return cFloatingButtonSize;
    }

    public void setFloatingButtonSize(int size) {
        int clamped = Math.max(50, Math.min(140, size));
        this.cFloatingButtonSize = clamped;
        prefs.edit().putInt(KEY_FLOATING_BUTTON_SIZE, clamped).apply();
    }

    public String getNightDimMode() {
        return cNightDimMode;
    }

    public void setNightDimMode(String mode) {
        this.cNightDimMode = mode;
        prefs.edit().putString(KEY_NIGHT_DIM_MODE, mode).apply();
    }

    public int getParallaxIntensity() {
        return cParallaxIntensity;
    }

    public void setParallaxIntensity(int intensity) {
        this.cParallaxIntensity = intensity;
        prefs.edit().putInt(KEY_PARALLAX_INTENSITY, intensity).apply();
    }

    public String getBackgroundStyle() {
        return cBackgroundStyle;
    }

    public void setBackgroundStyle(String style) {
        this.cBackgroundStyle = style;
        prefs.edit().putString(KEY_BACKGROUND_STYLE, style).apply();
    }

    public boolean isPipModeEnabled() {
        return cPipMode;
    }

    public void setPipModeEnabled(boolean enabled) {
        this.cPipMode = enabled;
        prefs.edit().putBoolean(KEY_PIP_MODE, enabled).apply();
    }

    // =====================================================================
    // Appearance Settings - Dikey Ekran (Cache)
    // =====================================================================

    public boolean isPortraitMapOnly() {
        return cPortraitMapOnly;
    }

    public void setPortraitMapOnly(boolean only) {
        this.cPortraitMapOnly = only;
        prefs.edit().putBoolean(KEY_PORTRAIT_MAP_ONLY, only).apply();
    }

    public String getPortraitExpansion() {
        return cPortraitExpansion;
    }

    public void setPortraitExpansion(String expansion) {
        this.cPortraitExpansion = expansion;
        prefs.edit().putString(KEY_PORTRAIT_EXPANSION, expansion).apply();
    }

    // =====================================================================
    // Appearance Settings - Yatay Ekran (Cache)
    // =====================================================================

    public String getWidgetPanelPosition() {
        return cWidgetPanelPosition;
    }

    public void setWidgetPanelPosition(String pos) {
        this.cWidgetPanelPosition = pos;
        prefs.edit().putString(KEY_WIDGET_PANEL_POSITION, pos).apply();
    }

    public String getLandscapeExpansion() {
        return cLandscapeExpansion;
    }

    public void setLandscapeExpansion(String expansion) {
        this.cLandscapeExpansion = expansion;
        prefs.edit().putString(KEY_LANDSCAPE_EXPANSION, expansion).apply();
    }

    /**
     * Geriye donuk uyumluluk: Eski getExpansionBehavior cagirilari icin.
     * Yeni kodda getLandscapeExpansion() kullanin.
     */
    public String getExpansionBehavior() {
        return cLandscapeExpansion;
    }

    public void setExpansionBehavior(String behavior) {
        setLandscapeExpansion(behavior);
    }

    // =====================================================================
    // Widget Card Style (Cache)
    // =====================================================================

    public String getWidgetCardStyle() {
        return cWidgetCardStyle;
    }

    public void setWidgetCardStyle(String style) {
        this.cWidgetCardStyle = style;
        prefs.edit().putString(KEY_WIDGET_CARD_STYLE, style).apply();
    }

    // =====================================================================
    // Music Settings (Cache)
    // =====================================================================

    public String getMusicApp() {
        return cMusicApp;
    }

    public void setMusicApp(String packageName) {
        this.cMusicApp = packageName;
        prefs.edit().putString(KEY_MUSIC_APP, packageName).apply();
    }

    public boolean isAutoPlayMusicEnabled() {
        return cAutoPlayMusic;
    }

    public void setAutoPlayMusicEnabled(boolean enabled) {
        this.cAutoPlayMusic = enabled;
        prefs.edit().putBoolean(KEY_AUTO_PLAY_MUSIC, enabled).apply();
    }

    public boolean isAmbianceVisualizerEnabled() {
        return cAmbianceVisualizer;
    }

    public void setAmbianceVisualizerEnabled(boolean enabled) {
        this.cAmbianceVisualizer = enabled;
        prefs.edit().putBoolean(KEY_AMBIANCE_VISUALIZER, enabled).apply();
    }

    // =====================================================================
    // Weather Settings (Cache)
    // =====================================================================

    public boolean isWeatherEnabled() {
        return cWeatherEnabled;
    }

    public void setWeatherEnabled(boolean enabled) {
        this.cWeatherEnabled = enabled;
        prefs.edit().putBoolean(KEY_WEATHER_ENABLED, enabled).apply();
    }

    // =====================================================================
    // Auto Launch Settings (Dinamik — slot bazli)
    // =====================================================================

    public boolean isAutoStartOnBootEnabled() {
        return cAutoStartOnBoot;
    }

    public void setAutoStartOnBootEnabled(boolean enabled) {
        this.cAutoStartOnBoot = enabled;
        prefs.edit().putBoolean(KEY_AUTO_START_ON_BOOT, enabled).apply();
    }

    public boolean isAutoLaunchEnabled(int slot) {
        return prefs.getBoolean(KEY_AUTOLAUNCH_ENABLE_PREFIX + slot, false);
    }

    public void setAutoLaunchEnabled(int slot, boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTOLAUNCH_ENABLE_PREFIX + slot, enabled).apply();
    }

    public String getAutoLaunchPackage(int slot) {
        return prefs.getString(KEY_AUTOLAUNCH_PKG_PREFIX + slot, null);
    }

    public String getAutoLaunchAppName(int slot) {
        return prefs.getString(KEY_AUTOLAUNCH_NAME_PREFIX + slot, "Seçilmedi");
    }

    public void setAutoLaunchApp(int slot, String pkg, String name) {
        prefs.edit()
             .putString(KEY_AUTOLAUNCH_PKG_PREFIX + slot, pkg)
             .putString(KEY_AUTOLAUNCH_NAME_PREFIX + slot, name)
             .apply();
    }

    // =====================================================================
    // Dock Settings - Yatay (Cache)
    // =====================================================================

    public int getMaxShortcuts() {
        return cMaxShortcuts;
    }

    public void setMaxShortcuts(int max) {
        this.cMaxShortcuts = max;
        prefs.edit().putInt(KEY_MAX_SHORTCUTS, max).apply();
    }

    public void resetDock() {
        // Reset is handled by AppDockManager.clearAllShortcuts()
    }

    public String getDockPosition() {
        return cDockPosition;
    }

    public void setDockPosition(String pos) {
        this.cDockPosition = pos;
        prefs.edit().putString(KEY_DOCK_POSITION, pos).apply();
    }

    public String getDockStyle() {
        return cDockStyle;
    }

    public void setDockStyle(String style) {
        this.cDockStyle = style;
        prefs.edit().putString(KEY_DOCK_STYLE, style).apply();
    }

    public int getDockSize() {
        return cDockSize;
    }

    public void setDockSize(int sizePercent) {
        int clamped = Math.max(0, Math.min(100, sizePercent));
        this.cDockSize = clamped;
        prefs.edit().putInt(KEY_DOCK_SIZE, clamped).apply();
    }

    // =====================================================================
    // Dock Settings - Dikey (Cache)
    // =====================================================================

    public String getDockPositionPortrait() {
        return cDockPositionPortrait;
    }

    public void setDockPositionPortrait(String pos) {
        this.cDockPositionPortrait = pos;
        prefs.edit().putString(KEY_DOCK_POSITION_PORTRAIT, pos).apply();
    }

    public int getDockSizePortrait() {
        return cDockSizePortrait;
    }

    public void setDockSizePortrait(int sizePercent) {
        int clamped = Math.max(0, Math.min(100, sizePercent));
        this.cDockSizePortrait = clamped;
        prefs.edit().putInt(KEY_DOCK_SIZE_PORTRAIT, clamped).apply();
    }

    // =====================================================================
    // General
    // =====================================================================

    public boolean isLauncherEnabled() {
        return prefs.getBoolean("car_launcher_enabled", true);
    }

    public void setLauncherEnabled(boolean enabled) {
        prefs.edit().putBoolean("car_launcher_enabled", enabled).apply();
    }

    // =====================================================================
    // Generic Widget Config (Dinamik — key degisken)
    // =====================================================================

    public String getWidgetConfig(String widgetId) {
        return prefs.getString(KEY_WIDGET_CONFIG_PREFIX + widgetId, null);
    }

    public void setWidgetConfig(String widgetId, String config) {
        prefs.edit().putString(KEY_WIDGET_CONFIG_PREFIX + widgetId, config).apply();
    }

    // =====================================================================
    // SharedPreferences direkt erişim (geriye donuk uyumluluk)
    // =====================================================================

    public SharedPreferences getPrefs() {
        return prefs;
    }
}
