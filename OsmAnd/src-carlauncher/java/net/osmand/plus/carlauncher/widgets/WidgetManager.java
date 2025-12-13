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
        }
    }

    /**
     * Widget cikar.
     */
    public void removeWidget(@NonNull BaseWidget widget) {
        allWidgets.remove(widget);
        visibleWidgets.remove(widget);
    }

    /**
     * Widget'i gosterilir/gizli yap.
     */
    public void setWidgetVisible(@NonNull String widgetId, boolean visible) {
        BaseWidget widget = findWidgetById(widgetId);
        if (widget != null) {
            widget.setVisible(visible);
            updateVisibleWidgets();
        }
    }

    /**
     * Widget siralamasini degistir.
     */
    public void moveWidget(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= allWidgets.size() ||
                toIndex < 0 || toIndex >= allWidgets.size()) {
            return;
        }

        BaseWidget widget = allWidgets.remove(fromIndex);
        allWidgets.add(toIndex, widget);

        // Update order fields
        for (int i = 0; i < allWidgets.size(); i++) {
            allWidgets.get(i).setOrder(i);
        }
        
        updateVisibleWidgets();
        saveWidgetConfig();
    }

    /**
     * Gorunur widget'lari guncelle ve sirala.
     */
    private void updateVisibleWidgets() {
        visibleWidgets.clear();
        for (BaseWidget widget : allWidgets) {
            if (widget.isVisible()) {
                visibleWidgets.add(widget);
            }
        }

        // Order'a gore sirala
        Collections.sort(visibleWidgets, new Comparator<BaseWidget>() {
            @Override
            public int compare(BaseWidget w1, BaseWidget w2) {
                return Integer.compare(w1.getOrder(), w2.getOrder());
            }
        });
    }

    /**
     * Widget'lari container'a ekle.
     */
    public void attachWidgetsToContainer(@NonNull ViewGroup container) {
        container.removeAllViews();

        for (BaseWidget widget : visibleWidgets) {
            View widgetView = widget.getRootView();
            if (widgetView == null) {
                widgetView = widget.createView();
            }

            if (widgetView != null) {
                container.addView(widgetView);
            }
        }
    }

    /**
     * Tum widget'lari guncelle.
     */
    public void updateAllWidgets() {
        for (BaseWidget widget : visibleWidgets) {
            if (widget.isStarted()) {
                widget.update();
            }
        }
    }

    /**
     * Tum widget'lari baslat.
     */
    public void startAllWidgets() {
        for (BaseWidget widget : visibleWidgets) {
            widget.onStart();
        }
    }

    /**
     * Tum widget'lari durdur.
     */
    public void stopAllWidgets() {
        for (BaseWidget widget : visibleWidgets) {
            widget.onStop();
        }
    }

    /**
     * Widget config'i kaydet.
     */
    /**
     * Widget config'i kaydet.
     */
    public void saveWidgetConfig() {
        net.osmand.plus.carlauncher.CarLauncherSettings settings = new net.osmand.plus.carlauncher.CarLauncherSettings(context);
        
        List<String> order = new ArrayList<>();
        for (BaseWidget widget : allWidgets) {
            settings.setWidgetVisible(widget.getId(), widget.isVisible());
            // Order is effectively the index in allWidgets dependent on how we manage it, 
            // but for simple list order saving:
            order.add(widget.getId()); 
        }
        
        // Actually, we should save the order of *visible* widgets or just the sorted order of all widgets via a specific list?
        // Let's save the order of 'visibleWidgets' first, then invisible ones appended? 
        // Or better: Let CarLauncherSettings store the 'Sort Order' of IDs.
        
        // Current 'allWidgets' list might be sorted by insertion or by previous load.
        // Let's create a list of IDs representing the current desired order.
        // Since 'moveWidget' modifies 'visibleWidgets' only, we need to reflect that in 'allWidgets' or just save 'visibleWidgets' order?
        
        // Strategy: Save all IDs in the order they should appear.
        // visibleWidgets are at the top (sorted). Invisible ones don't have an order per se, but let's keep them stable.
        
        List<String> sortedIds = new ArrayList<>();
        for (BaseWidget w : visibleWidgets) {
            sortedIds.add(w.getId());
        }
        // Add remaining invisible widgets
        for (BaseWidget w : allWidgets) {
            if (!visibleWidgets.contains(w)) {
                sortedIds.add(w.getId());
            }
        }
        
        settings.setWidgetOrder(sortedIds);
    }

    /**
     * Widget config'i yukle.
     */
    public void loadWidgetConfig() {
        net.osmand.plus.carlauncher.CarLauncherSettings settings = new net.osmand.plus.carlauncher.CarLauncherSettings(context);
        
        // Default order... if simple we rely on initialization order.
        List<String> savedOrder = settings.getWidgetOrder(null);
        
        if (savedOrder != null) {
            // Reorder 'allWidgets' based on savedOrder
            List<BaseWidget> reordered = new ArrayList<>();
            for (String id : savedOrder) {
                BaseWidget w = findWidgetById(id);
                if (w != null) {
                    reordered.add(w);
                }
            }
            // Add any new/unknown widgets that were not in saved settings
            for (BaseWidget w : allWidgets) {
                if (!reordered.contains(w)) {
                    reordered.add(w);
                }
            }
            
            allWidgets.clear();
            allWidgets.addAll(reordered);
        }
        
        // Restore visibility
        for (BaseWidget widget : allWidgets) {
            // Default visibility true? Or based on some default list? Assuming true for now.
            boolean visible = settings.isWidgetVisible(widget.getId(), true);
            widget.setVisible(visible);
            
            // Set internal order field based on list index
            widget.setOrder(allWidgets.indexOf(widget));
        }

        updateVisibleWidgets();
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
