package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Widget'lari yoneten merkezi sinif.
 * Widget ekleme, cikarma, siralama, kaydetme.
 */
public class WidgetManager {

    private static final String PREFS_NAME = "car_launcher_widgets";
    private static final String KEY_WIDGET_CONFIG = "widget_config";

    private final Context context;
    private final OsmandApplication app;
    private final SharedPreferences prefs;

    private final List<BaseWidget> allWidgets;
    private final List<BaseWidget> visibleWidgets;

    public WidgetManager(@NonNull Context context, @NonNull OsmandApplication app) {
        this.context = context;
        this.app = app;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.allWidgets = new ArrayList<>();
        this.visibleWidgets = new ArrayList<>();
    }

    /**
     * Widget ekle.
     */
    public void addWidget(@NonNull BaseWidget widget) {
        if (!allWidgets.contains(widget)) {
            allWidgets.add(widget);
            updateVisibleWidgets();
            if (isStarted) {
                widget.onStart();
            }
            saveWidgetConfig(); // SAVE ON ADD
        }
    }

    /**
     * Widget cikar.
     */
    public void removeWidget(@NonNull BaseWidget widget) {
        allWidgets.remove(widget);
        visibleWidgets.remove(widget);
        saveWidgetConfig(); // SAVE ON REMOVE
    }

    /**
     * Widget'i gosterilir/gizli yap.
     */
    public void setWidgetVisible(@NonNull String widgetId, boolean visible) {
        BaseWidget widget = findWidgetById(widgetId);
        if (widget != null) {
            widget.setVisible(visible);
            updateVisibleWidgets();
            saveWidgetConfig(); // SAVE ON VISIBILITY CHANGE
        }
    }
    
    // ...

    /**
     * Widget config'i yukle.
     */
    public boolean loadWidgetConfig() {
        // Check if key exists. If not, it means first run -> return false to load defaults.
        if (!prefs.contains(KEY_WIDGET_CONFIG)) return false;
        
        String savedConfig = prefs.getString(KEY_WIDGET_CONFIG, "");
        
        // If key exists but is empty, it means user cleared all widgets.
        // We should return TRUE (loaded successfully, result is empty) instead of FALSE (defaults).
        if (savedConfig.isEmpty()) {
            allWidgets.clear();
            visibleWidgets.clear();
            return true; 
        }

        String[] ids = savedConfig.split(",");
        List<BaseWidget> restoredWidgets = new ArrayList<>();
        
        for (String id : ids) {
            if (id.isEmpty()) continue;
            
            // Try to find existing (default) widget
            BaseWidget widget = findWidgetById(id);
            
            // If not found, try to create it (Dynamic Widget)
            if (widget == null) {
                // ID format: type OR type_timestamp
                // Extract type
                String type = id.split("_")[0];
                widget = WidgetRegistry.createWidget(context, app, type);
                if (widget != null) {
                    // Restore ID hack to match saved ID
                     try {
                        java.lang.reflect.Field idField = BaseWidget.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(widget, id);
                    } catch (Exception e) {
                        // Fail silently
                    }
                }
            }
            
            if (widget != null) {
                // Restore Properties
                boolean visible = prefs.getBoolean("visible_" + id, true);
                int sizeOrd = prefs.getInt("size_" + id, BaseWidget.WidgetSize.SMALL.ordinal());
                
                widget.setVisible(visible);
                if (sizeOrd >= 0 && sizeOrd < BaseWidget.WidgetSize.values().length) {
                    widget.setSize(BaseWidget.WidgetSize.values()[sizeOrd]);
                }
                
                restoredWidgets.add(widget);
            }
        }
        
        // Replace current list with restored list
        allWidgets.clear();
        allWidgets.addAll(restoredWidgets);
        
        updateVisibleWidgets();
        return true;
    }

    /**
     * ID'ye gore widget bul.
     */
    private BaseWidget findWidgetById(String id) {
        for (BaseWidget widget : allWidgets) {
            if (widget.getId().equals(id)) {
                return widget;
            }
        }
        return null;
    }

    /**
     * Tum widget'lari al.
     */
    public List<BaseWidget> getAllWidgets() {
        return new ArrayList<>(allWidgets);
    }

    /**
     * Gorunur widget'lari al.
     */
    public List<BaseWidget> getVisibleWidgets() {
        return new ArrayList<>(visibleWidgets);
    }
}
