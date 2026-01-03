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
        // Prevent duplicates strictly by ID
        if (findWidgetById(widget.getId()) != null) {
             android.util.Log.d("WidgetDebug", "addWidget: Duplicate detected, skipping " + widget.getId());
             return;
        }

        if (!allWidgets.contains(widget)) {
            android.util.Log.d("WidgetDebug", "addWidget: " + widget.getId());
            allWidgets.add(widget);
            updateVisibleWidgets();
            if (isStarted) {
                widget.onStart();
            }
            saveWidgetConfig(); // SAVE ON ADD
        } else {
             android.util.Log.d("WidgetDebug", "addWidget: Already reference " + widget.getId());
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
     * Toplu siralama guncelleme (Adapter'dan gelen).
     */
    public void updateVisibleOrder(List<BaseWidget> newOrder) {
        // 1. Update order index
        for (int i = 0; i < newOrder.size(); i++) {
             BaseWidget w = newOrder.get(i);
             w.setOrder(i);
        }
        
        // 2. Reconstruct allWidgets to match visual order + invisible ones
        List<BaseWidget> newAllWidgets = new ArrayList<>(newOrder);
        
        for (BaseWidget w : allWidgets) {
            if (!newOrder.contains(w)) {
                newAllWidgets.add(w); // Append invisible ones
            }
        }
        
        allWidgets.clear();
        allWidgets.addAll(newAllWidgets);
        
        // 3. Sync visibleWidgets
        visibleWidgets.clear();
        visibleWidgets.addAll(newOrder);
        
        // 4. Save
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

        int margin = dpToPx(4); // Reduced margin

        for (int i = 0; i < visibleWidgets.size(); i++) {
            BaseWidget widget = visibleWidgets.get(i);
            View widgetView = widget.getRootView();
            if (widgetView == null) {
                widgetView = widget.createView();
            }

            if (widgetView != null) {
                // Apply margins consistently to all widgets
                // Detect Layout Orientation
                boolean isVertical = true;
                LinearLayout.LayoutParams params;
                if (container instanceof LinearLayout) {
                    isVertical = ((LinearLayout) container).getOrientation() == LinearLayout.VERTICAL;
                }

                if (isVertical) {
                    // Vertical (Landscape): Width Fill, Height Wrap
                    params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                } else {
                    // Horizontal (Portrait): Width Fixed (~100-130dp), Height Fill
                    int width = dpToPx(130);
                    params = new LinearLayout.LayoutParams(
                            width,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                }

                // First widget gets 0 top margin
                int topMargin = (i == 0) ? 0 : margin;
                params.setMargins(margin, topMargin, margin, margin);
                widgetView.setLayoutParams(params);

                container.addView(widgetView);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean isStarted = false;

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
        isStarted = true;
        for (BaseWidget widget : visibleWidgets) {
            widget.onStart();
        }
    }

    /**
     * Tum widget'lari durdur.
     */
    public void stopAllWidgets() {
        isStarted = false;
        for (BaseWidget widget : visibleWidgets) {
            widget.onStop();
        }
    }

    /**
     * Widget config'i kaydet.
     */
    public void saveWidgetConfig() {
        SharedPreferences.Editor editor = prefs.edit();
        
        List<String> ids = new ArrayList<>();
        for (BaseWidget widget : allWidgets) {
            ids.add(widget.getId());
            editor.putBoolean("visible_" + widget.getId(), widget.isVisible());
            editor.putInt("size_" + widget.getId(), widget.getSize().ordinal());
        }
        
        // Join Ids
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(",");
        }
        editor.putString(KEY_WIDGET_CONFIG, sb.toString());
        editor.apply();
    }

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
