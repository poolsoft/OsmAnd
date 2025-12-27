package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.CarLauncherSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget'lari yoneten merkezi sinif.
 * - Multi-Instance destegi (UUID based IDs)
 * - JSON Configuration (Type, ID, Size, Order)
 * - WidgetFactory entegrasyonu
 */
public class WidgetManager {

    private final Context context;
    private final OsmandApplication app;
    private final CarLauncherSettings settings;

    // List of active widget instances
    private final List<BaseWidget> widgets;

    public WidgetManager(@NonNull Context context, @NonNull OsmandApplication app) {
        this.context = context;
        this.app = app;
        this.settings = new CarLauncherSettings(context);
        this.widgets = new ArrayList<>();
    }

    /**
     * Initializes widgets from saved configuration or default migration.
     */
    public void loadWidgetConfig() {
        widgets.clear();
        String jsonConfig = settings.getWidgetConfigJson();

        if (!TextUtils.isEmpty(jsonConfig)) {
            loadFromJson(jsonConfig);
        } else {
            loadFromLegacyAndMigrate();
        }
    }

    private void loadFromJson(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String type = obj.optString("type");
                String instanceId = obj.optString("id");
                String sizeName = obj.optString("size", "LARGE");
                boolean visible = obj.optBoolean("visible", true);

                BaseWidget widget = WidgetFactory.createWidget(context, app, type);
                if (widget != null) {
                    if (!TextUtils.isEmpty(instanceId)) {
                        widget.setInstanceId(instanceId);
                    }
                    try {
                        widget.setSize(BaseWidget.WidgetSize.valueOf(sizeName));
                    } catch (IllegalArgumentException e) {
                        widget.setSize(BaseWidget.WidgetSize.LARGE);
                    }
                    widget.setVisible(visible);
                    widgets.add(widget);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            loadFromLegacyAndMigrate(); // Fallback
        }
    }

    private void loadFromLegacyAndMigrate() {
        // Default Legacy List
        java.util.List<String> defaults = new ArrayList<>();
        defaults.add(WidgetFactory.TYPE_CLOCK);
        defaults.add(WidgetFactory.TYPE_SPEED);
        defaults.add(WidgetFactory.TYPE_OBD); // OBD
        defaults.add(WidgetFactory.TYPE_NAVIGATION);
        defaults.add(WidgetFactory.TYPE_MUSIC);
        defaults.add(WidgetFactory.TYPE_ANTENNA);
        defaults.add(WidgetFactory.TYPE_COMPASS);

        // Load old order from legacy settings
        List<String> legacyOrder = settings.getWidgetOrder(defaults);

        for (String type : legacyOrder) {
            // Map legacy IDs to new Types if needed (e.g. "clock_material3" -> TYPE_CLOCK)
            String mappedType = mapLegacyType(type);
            
            BaseWidget widget = WidgetFactory.createWidget(context, app, mappedType);
            if (widget != null) {
                // Visibility was stored separately in legacy
                boolean visible = settings.isWidgetVisible(type, true); // Use old ID for lookup
                widget.setVisible(visible);
                widgets.add(widget);
            }
        }
        
        saveWidgetConfig(); // Save as new JSON format
    }

    private String mapLegacyType(String oldId) {
        if ("clock_material3".equals(oldId)) return WidgetFactory.TYPE_CLOCK;
        if ("direction".equals(oldId)) return WidgetFactory.TYPE_COMPASS;
        if ("obd_dashboard".equals(oldId)) return WidgetFactory.TYPE_OBD;
        // Others matched factory types: "speed", "music", "navigation", "antenna"
        return oldId;
    }

    public void saveWidgetConfig() {
        JSONArray arr = new JSONArray();
        for (BaseWidget widget : widgets) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", widget.getType());
                obj.put("id", widget.getId()); // Instance ID
                obj.put("size", widget.getSize().name());
                obj.put("visible", widget.isVisible());
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        settings.setWidgetConfigJson(arr.toString());
    }

    /**
     * Create and add a new widget instance by type.
     */
    public void addWidgetByType(String type) {
        BaseWidget widget = WidgetFactory.createWidget(context, app, type);
        if (widget != null) {
            widgets.add(widget);
            saveWidgetConfig();
            updateVisibleOrder(widgets); // Notify listeners? Adapter triggers this usually.
            // Actually adapter is listening to changes if we implement observer pattern.
            // For now, caller usually refreshes adapter.
        }
    }

    public void removeWidget(BaseWidget widget) {
        widgets.remove(widget);
        saveWidgetConfig();
    }
    
    // For manual addition if needed (e.g. from tests or old init)
    public void addWidget(BaseWidget widget) {
         if (widget != null && !widgets.contains(widget)) {
             widgets.add(widget);
             saveWidgetConfig();
         }
    }

    public void updateVisibleOrder(List<BaseWidget> newOrder) {
         // newOrder contains only visible widgets in order.
         // We need to reconstruct 'widgets' list: New Visible Order + Hidden Widgets.
         
         List<BaseWidget> hiddenWidgets = new ArrayList<>();
         for (BaseWidget w : widgets) {
             if (!newOrder.contains(w)) {
                 hiddenWidgets.add(w);
             }
         }
         
         widgets.clear();
         widgets.addAll(newOrder);
         widgets.addAll(hiddenWidgets);
         
         saveWidgetConfig();
    }

    public List<BaseWidget> getAllWidgets() {
        return new ArrayList<>(widgets);
    }

    public List<BaseWidget> getVisibleWidgets() {
        List<BaseWidget> visible = new ArrayList<>();
        for (BaseWidget w : widgets) {
            if (w.isVisible()) {
                visible.add(w);
            }
        }
        return visible;
    }
    
    // --- Lifecycle Methods ---

    public void startAllWidgets() {
        for (BaseWidget widget : widgets) {
            if (widget.isVisible()) widget.onStart();
        }
    }

    public void stopAllWidgets() {
        for (BaseWidget widget : widgets) {
            widget.onStop();
        }
    }
    
    public void updateAllWidgets() {
        for (BaseWidget widget : widgets) {
             if(widget.isVisible() && widget.isStarted()) widget.update();
        }
    }
    public void moveWidget(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < widgets.size() && toIndex >= 0 && toIndex < widgets.size()) {
            BaseWidget widget = widgets.remove(fromIndex);
            widgets.add(toIndex, widget);
            saveWidgetConfig();
        }
    }
}
