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

    private static WidgetManager instance;
    
    private boolean hasAutoLaunched = false;

    public boolean isHasAutoLaunched() {
        return hasAutoLaunched;
    }

    public void setHasAutoLaunched(boolean hasAutoLaunched) {
        this.hasAutoLaunched = hasAutoLaunched;
    }

    public static synchronized WidgetManager getInstance(Context context) {
        if (instance == null) {
            instance = new WidgetManager(context);
        }
        return instance;
    }

    private WidgetManager(@NonNull Context context) {
        this.context = context.getApplicationContext(); // Use App Context for storage/prefs
        this.app = (OsmandApplication) context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.allWidgets = new ArrayList<>();
        this.visibleWidgets = new ArrayList<>();
    }

    private boolean configLoaded = false;

    public boolean isConfigLoaded() {
        return configLoaded;
    }
    
    /**
     * Force reset for new Fragment session.
     * Clears all widget lists and config flag to ensure clean state.
     * This prevents duplication when singleton persists across app "restarts".
     */
    public void forceResetForNewSession() {
        allWidgets.clear();
        visibleWidgets.clear();
        isStarted = false;
        configLoaded = false;
    }

    /**
     * Update Activity Context for all widgets (Post-Rotation).
     */
    public void updateActivityContext(Context activityContext) {
        for (BaseWidget widget : allWidgets) {
            widget.setContext(activityContext);
             // Clear old view so it gets recreated with new context
            widget.onDestroy(); 
        }
    }

    /**
     * Widget ekle.
     */
    public void addWidget(@NonNull BaseWidget widget) {
        if (findWidgetById(widget.getId()) != null) {
            return; // Duplicate — sessizce atla
        }
        if (!allWidgets.contains(widget)) {
            allWidgets.add(widget);
            updateVisibleWidgets();
            if (isStarted) {
                widget.onStart();
            }
            saveWidgetConfig();
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
    
    public void updateVisibleOrder(List<BaseWidget> newOrder) {
        // 1. Update order index
        for (int i = 0; i < newOrder.size(); i++) {
             BaseWidget w = newOrder.get(i);
             w.setOrder(i);
        }
        
        // 2. FIXED: Simply replace allWidgets with newOrder
        //    Dialog's editingList is the single source of truth
        //    Deleted widgets should NOT be preserved as "invisible"
        allWidgets.clear();
        allWidgets.addAll(newOrder);
        
        // 3. Sync visibleWidgets (all widgets in newOrder are visible by definition)
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
        java.util.Set<String> processingIds = new java.util.HashSet<>();
        
        for (String id : ids) {
            if (id.isEmpty()) continue;
            
            // (Deduplication moved to creation logic)
            
            // Try to find existing (default) widget
            BaseWidget widget = findWidgetById(id);
            
            // If not found, try to create it (Dynamic Widget)
            if (widget == null) {
                // ID format: type OR type_timestamp
                String type = id.split("_")[0];
                
                // DATA HEALING: If this ID was already processed, it's a DUPLICATE in the config.
                // We must recover it as a NEW unique widget.
                if (processingIds.contains(id)) {
                    android.util.Log.w("WidgetDebug", "Heal: Duplicate ID found: " + id + ". Creating new instance.");
                    widget = WidgetRegistry.createUniqueWidget(context, app, type); // Generates new ID
                    // Note: We cannot restore settings for this one effectively since keys clash, 
                    // but we save the instance.
                } else {
                    // Normal Creation
                    widget = WidgetRegistry.createWidget(context, app, type);
                    if (widget != null) {
                         try {
                             // Restore ID hack
                            java.lang.reflect.Field idField = BaseWidget.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(widget, id);
                        } catch (Exception e) {}
                    }
                }
            } else {
                 if (processingIds.contains(id)) {
                     // Existing Default Widget is duplicated? This shouldn't happen normally for Singletons,
                     // but if it does, we can't clone the Singleton (like SpeedWidget).
                     // We skip duplicates of Singletons.
                     continue;
                 }
            }
            
            if (widget != null) {
                processingIds.add(widget.getId()); // Add the FINAL id (handles healed ones)
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
        configLoaded = true;
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

    /**
     * Mevcut widget config ve durumlarini user_ prefix'i ile SharedPreferences'a kaydeder.
     * Turkce karakter kullanilmamistir.
     */
    public void saveUserLayout() {
        String savedConfig = prefs.getString(KEY_WIDGET_CONFIG, "");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_" + KEY_WIDGET_CONFIG, savedConfig);
        
        for (BaseWidget widget : allWidgets) {
            String id = widget.getId();
            editor.putBoolean("user_visible_" + id, widget.isVisible());
            editor.putInt("user_size_" + id, widget.getSize().ordinal());
        }
        editor.apply();
    }

    /**
     * user_ prefix'i ile kaydedilmis olan widget config ve durumlarini yukler.
     * Turkce karakter kullanilmamistir.
     */
    public boolean loadUserLayout() {
        if (!prefs.contains("user_" + KEY_WIDGET_CONFIG)) {
            return false;
        }
        
        String userConfig = prefs.getString("user_" + KEY_WIDGET_CONFIG, "");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_WIDGET_CONFIG, userConfig);
        
        if (!userConfig.isEmpty()) {
            String[] ids = userConfig.split(",");
            for (String id : ids) {
                if (id.isEmpty()) continue;
                if (prefs.contains("user_visible_" + id)) {
                    editor.putBoolean("visible_" + id, prefs.getBoolean("user_visible_" + id, true));
                }
                if (prefs.contains("user_size_" + id)) {
                    editor.putInt("size_" + id, prefs.getInt("user_size_" + id, BaseWidget.WidgetSize.SMALL.ordinal()));
                }
            }
        }
        editor.apply();
        return loadWidgetConfig();
    }
}
