package net.osmand.plus.carlauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Car Launcher ayarlarini yoneten sinif.
 * SharedPreferences wrapper.
 */
public class CarLauncherSettings {
    private static final String PREF_NAME = "car_launcher_prefs";

    // Widget Keys
    private static final String KEY_WIDGET_ORDER = "widget_order";
    private static final String KEY_WIDGET_VISIBILITY_PREFIX = "widget_visible_";
    public static final String KEY_WIDGET_DISPLAY_MODE = "widget_display_mode"; // 0: List, 1: Paged

    // Grid Slot Keys (NEW) - v2 to force reset defaults and support separate modes
    public static final String KEY_WIDGET_SLOTS_PORTRAIT = "widget_slot_count_portrait_v2";
    public static final String KEY_WIDGET_SLOTS_LANDSCAPE = "widget_slot_count_landscape_v2";
    
    // Appearance Keys
    public static final String KEY_STATUS_BAR = "car_launcher_status_bar";
    public static final String KEY_DARK_THEME = "car_launcher_dark_theme";

    // Music Keys
    public static final String KEY_MUSIC_APP = "car_launcher_music_app";

    // Dock Keys
    public static final String KEY_MAX_SHORTCUTS = "car_launcher_max_shortcuts";

    private final SharedPreferences prefs;

    public CarLauncherSettings(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- Widget Settings ---

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
            return 0; // Default to List
        }
    }

    public void setWidgetDisplayMode(int mode) {
        prefs.edit().putString(KEY_WIDGET_DISPLAY_MODE, String.valueOf(mode)).apply();
    }

    // --- Grid Slot Settings ---
    public int getPortraitSlotCount() {
        return prefs.getInt(KEY_WIDGET_SLOTS_PORTRAIT, 3); // Default 3 (Standard Density)
    }

    public void setPortraitSlotCount(int count) {
        prefs.edit().putInt(KEY_WIDGET_SLOTS_PORTRAIT, count).apply();
    }
    
    public int getLandscapeSlotCount() {
        return prefs.getInt(KEY_WIDGET_SLOTS_LANDSCAPE, 3); // Default 3
    }
    
    public void setLandscapeSlotCount(int count) {
        prefs.edit().putInt(KEY_WIDGET_SLOTS_LANDSCAPE, count).apply();
    }

    // --- Appearance Settings ---

    public boolean isStatusBarVisible() {
        return prefs.getBoolean(KEY_STATUS_BAR, true);
    }

    public void setStatusBarVisible(boolean visible) {
        prefs.edit().putBoolean(KEY_STATUS_BAR, visible).apply();
    }

    public boolean isDarkTheme() {
        return prefs.getBoolean(KEY_DARK_THEME, true);
    }

    public void setDarkTheme(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply();
    }

    // --- Music Settings ---

    public String getMusicApp() {
        return prefs.getString(KEY_MUSIC_APP, "internal");
    }

    public void setMusicApp(String packageName) {
        prefs.edit().putString(KEY_MUSIC_APP, packageName).apply();
    }
    
    private static final String KEY_AUTO_PLAY_MUSIC = "car_launcher_auto_play_music";
    
    public boolean isAutoPlayMusicEnabled() {
        return prefs.getBoolean(KEY_AUTO_PLAY_MUSIC, false);
    }
    
    public void setAutoPlayMusicEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_MUSIC, enabled).apply();
    }
    
    // --- Weather Settings ---
    private static final String KEY_WEATHER_ENABLED = "car_launcher_weather_enabled";
    
    public boolean isWeatherEnabled() {
        return prefs.getBoolean(KEY_WEATHER_ENABLED, true);
    }
    
    public void setWeatherEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WEATHER_ENABLED, enabled).apply();
    }

    // --- Auto Launch Settings ---
    public static final String KEY_AUTOLAUNCH_ENABLE_PREFIX = "autolaunch_enable_";
    public static final String KEY_AUTOLAUNCH_PKG_PREFIX = "autolaunch_pkg_";
    public static final String KEY_AUTOLAUNCH_NAME_PREFIX = "autolaunch_name_";

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

    // --- Dock Settings ---

    public int getMaxShortcuts() {
        return prefs.getInt(KEY_MAX_SHORTCUTS, 6);
    }

    public void setMaxShortcuts(int max) {
        prefs.edit().putInt(KEY_MAX_SHORTCUTS, max).apply();
    }

    public void resetDock() {
        // Reset is handled by AppDockManager.clearAllShortcuts()
        // This method is a placeholder for future use
    }

    // --- General ---

    public boolean isLauncherEnabled() {
        return prefs.getBoolean("car_launcher_enabled", true);
    }

    public void setLauncherEnabled(boolean enabled) {
        prefs.edit().putBoolean("car_launcher_enabled", enabled).apply();
    }

    // --- Utility ---

    public SharedPreferences getPrefs() {
        return prefs;
    }
}
