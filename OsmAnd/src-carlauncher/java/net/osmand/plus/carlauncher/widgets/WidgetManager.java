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
        if (fromIndex < 0 || fromIndex >= visibleWidgets.size() ||
                toIndex < 0 || toIndex >= visibleWidgets.size()) {
            return;
        }

        BaseWidget widget = visibleWidgets.remove(fromIndex);
        visibleWidgets.add(toIndex, widget);

        // Order'lari guncelle
        for (int i = 0; i < visibleWidgets.size(); i++) {
            visibleWidgets.get(i).setOrder(i);
        }
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
    public void saveWidgetConfig() {
        try {
            JSONObject config = new JSONObject();
            JSONArray widgetsArray = new JSONArray();

            for (BaseWidget widget : allWidgets) {
                JSONObject widgetObj = new JSONObject();
                widgetObj.put("id", widget.getId());
                widgetObj.put("visible", widget.isVisible());
                widgetObj.put("order", widget.getOrder());
                widgetsArray.put(widgetObj);
            }

            config.put("widgets", widgetsArray);

            prefs.edit()
                    .putString(KEY_WIDGET_CONFIG, config.toString())
                    .apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Widget config'i yukle.
     */
    public void loadWidgetConfig() {
        String configStr = prefs.getString(KEY_WIDGET_CONFIG, null);
        if (configStr == null)
            return;

        try {
            JSONObject config = new JSONObject(configStr);
            JSONArray widgetsArray = config.getJSONArray("widgets");

            for (int i = 0; i < widgetsArray.length(); i++) {
                JSONObject widgetObj = widgetsArray.getJSONObject(i);
                String id = widgetObj.getString("id");
                boolean visible = widgetObj.getBoolean("visible");
                int order = widgetObj.getInt("order");

                BaseWidget widget = findWidgetById(id);
                if (widget != null) {
                    widget.setVisible(visible);
                    widget.setOrder(order);
                }
            }

            updateVisibleWidgets();

        } catch (JSONException e) {
            e.printStackTrace();
        }
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
