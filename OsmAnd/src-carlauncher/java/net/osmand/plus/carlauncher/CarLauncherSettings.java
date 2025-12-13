package net.osmand.plus.carlauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CarLauncherSettings {
    private static final String PREF_NAME = "car_launcher_prefs";
    private static final String KEY_WIDGET_ORDER = "widget_order";
    private static final String KEY_WIDGET_VISIBILITY_PREFIX = "widget_visible_";

    private final SharedPreferences prefs;

    public CarLauncherSettings(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

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
}
