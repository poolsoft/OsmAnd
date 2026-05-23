package net.osmand.plus.carlauncher.widgets;

import android.appwidget.AppWidgetHost;
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
    private final AppWidgetHost appWidgetHost;

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
        this.appWidgetHost = new AppWidgetHost(this.context, 1024);
    }

    public AppWidgetHost getAppWidgetHost() {
        return appWidgetHost;
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
     * Tum widget'lar icin Activity Context bilgisini guncelle (Post-Rotation).
     * Tekil AppWidgetHost dinleme durumunu guvenle tazeler.
     * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
     */
    public void updateActivityContext(Context activityContext) {
        if (isStarted && this.appWidgetHost != null) {
            try {
                this.appWidgetHost.stopListening();
                this.appWidgetHost.startListening();
            } catch (Exception e) {
                android.util.Log.e("WidgetManager", "AppWidgetHost tazeleme hatasi: " + e.getMessage());
            }
        }
        
        for (BaseWidget widget : allWidgets) {
            widget.setContext(activityContext);
             // Yeni context ile yeniden olusturulmasi icin eski view'i temizle
            widget.onDestroy(); 
        }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private Point findEmptySpace(boolean[][] occupied, int spanX, int spanY) {
        for (int y = 0; y <= 4 - spanY; y++) {
            for (int x = 0; x <= 4 - spanX; x++) {
                boolean fits = true;
                for (int sy = 0; sy < spanY; sy++) {
                    for (int sx = 0; sx < spanX; sx++) {
                        if (occupied[x + sx][y + sy]) {
                            fits = false;
                            break;
                        }
                    }
                    if (!fits) break;
                }
                if (fits) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    private void autoPlaceWidget(BaseWidget widget) {
        int pageCount = 3;
        int spanX = widget.getSpanX();
        int spanY = widget.getSpanY();
        
        // Oncelikle widget'in kendi pageIndex sayfasini dene
        int targetPage = widget.getPageIndex();
        if (targetPage < 0 || targetPage >= pageCount) {
            targetPage = 0;
        }
        
        // Sayfalari hedef sayfadan baslayarak sirayla kontrol et
        for (int i = 0; i < pageCount; i++) {
            int page = (targetPage + i) % pageCount;
            boolean[][] occupied = new boolean[4][4];
            
            // Bu sayfadaki diger gorunur widget'lari bulup isaretle
            for (BaseWidget w : allWidgets) {
                if (w != widget && w.isVisible() && w.getPageIndex() == page) {
                    int cx = w.getCellX();
                    int cy = w.getCellY();
                    int sx = w.getSpanX();
                    int sy = w.getSpanY();
                    if (cx >= 0 && cx + sx <= 4 && cy >= 0 && cy + sy <= 4) {
                        for (int x = cx; x < cx + sx; x++) {
                            for (int y = cy; y < cy + sy; y++) {
                                occupied[x][y] = true;
                            }
                        }
                    }
                }
            }
            
            Point p = findEmptySpace(occupied, spanX, spanY);
            if (p != null) {
                widget.setPageIndex(page);
                widget.setCellX(p.x);
                widget.setCellY(p.y);
                return;
            }
        }
        
        // Sayfalarda yer kalmadiysa, en son sayfaya fallback olarak 0,0 ata
        widget.setPageIndex(pageCount - 1);
        widget.setCellX(0);
        widget.setCellY(0);
    }

    /**
     * Widget ekle veya zaten varsa goster.
     * Bos yerleri dinamik olarak otomatik hesaplar ve yerlestirir.
     * Kod icinde kesinlikle Turkce karakter kullanilmamistir.
     */
    public void addWidget(@NonNull BaseWidget widget) {
        BaseWidget targetWidget = widget;
        BaseWidget existing = findWidgetById(widget.getId());
        if (existing != null) {
            existing.setVisible(true);
            existing.setSize(widget.getSize());
            targetWidget = existing;
        } else {
            if (!allWidgets.contains(widget)) {
                allWidgets.add(widget);
            }
            targetWidget = widget;
        }

        // Eger koordinatlar -1 ise bos yer bulup yerlestir
        if (targetWidget.getCellX() == -1 || targetWidget.getCellY() == -1) {
            autoPlaceWidget(targetWidget);
        }

        updateVisibleWidgets();
        if (isStarted && !targetWidget.isStarted()) {
            targetWidget.onStart();
        }
        saveWidgetConfig();
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
     * Sistem widget'larinin eski view referanslarini sifirlayarak taze context ile baslatir.
     * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
     */
    public void startAllWidgets() {
        isStarted = true;
        try {
            appWidgetHost.startListening();
        } catch (Exception e) {
            android.util.Log.e("WidgetManager", "AppWidgetHost dinlemesi baslatilamadi: " + e.getMessage());
        }
        for (BaseWidget widget : visibleWidgets) {
            if (widget instanceof SystemAppWidget) {
                widget.onDestroy(); // Taze context ile yeniden yaratilmasi icin view referansini sifirla
            }
            widget.onStart();
        }
    }

    /**
     * Tum widget'lari durdur.
     */
    public void stopAllWidgets() {
        isStarted = false;
        try {
            appWidgetHost.stopListening();
        } catch (Exception e) {
            android.util.Log.e("WidgetManager", "AppWidgetHost dinlemesi durdurulamadi: " + e.getMessage());
        }
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
            
            // Grid koordinatlari ve boyutlari
            editor.putInt("page_" + widget.getId(), widget.getPageIndex());
            editor.putInt("cellx_" + widget.getId(), widget.getCellX());
            editor.putInt("celly_" + widget.getId(), widget.getCellY());
            editor.putInt("spanx_" + widget.getId(), widget.getSpanX());
            editor.putInt("spany_" + widget.getId(), widget.getSpanY());
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
                if (id.startsWith("appwidget_")) {
                    try {
                        int appWidgetId = Integer.parseInt(id.substring("appwidget_".length()));
                        widget = new SystemAppWidget(context, appWidgetId);
                    } catch (Exception e) {
                        android.util.Log.e("WidgetManager", "SystemAppWidget geri yuklenirken hata: " + e.getMessage());
                    }
                } else {
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
                
                // Grid koordinatlarini geri yukleme
                int page = prefs.getInt("page_" + id, 0);
                int cellx = prefs.getInt("cellx_" + id, -1);
                int celly = prefs.getInt("celly_" + id, -1);
                int spanx = prefs.getInt("spanx_" + id, 1);
                int spany = prefs.getInt("spany_" + id, 1);
                
                widget.setPageIndex(page);
                widget.setCellX(cellx);
                widget.setCellY(celly);
                widget.setSpanX(spanx);
                widget.setSpanY(spany);
                
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
            
            // Grid koordinatlarini da yedekle
            editor.putInt("user_page_" + id, widget.getPageIndex());
            editor.putInt("user_cellx_" + id, widget.getCellX());
            editor.putInt("user_celly_" + id, widget.getCellY());
            editor.putInt("user_spanx_" + id, widget.getSpanX());
            editor.putInt("user_spany_" + id, widget.getSpanY());
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
                if (prefs.contains("user_page_" + id)) {
                    editor.putInt("page_" + id, prefs.getInt("user_page_" + id, 0));
                }
                if (prefs.contains("user_cellx_" + id)) {
                    editor.putInt("cellx_" + id, prefs.getInt("user_cellx_" + id, -1));
                }
                if (prefs.contains("user_celly_" + id)) {
                    editor.putInt("celly_" + id, prefs.getInt("user_celly_" + id, -1));
                }
                if (prefs.contains("user_spanx_" + id)) {
                    editor.putInt("spanx_" + id, prefs.getInt("user_spanx_" + id, 1));
                }
                if (prefs.contains("user_spany_" + id)) {
                    editor.putInt("spany_" + id, prefs.getInt("user_spany_" + id, 1));
                }
            }
        }
        editor.apply();
        return loadWidgetConfig();
    }
}
