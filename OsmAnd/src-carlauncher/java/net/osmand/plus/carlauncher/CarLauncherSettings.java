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
