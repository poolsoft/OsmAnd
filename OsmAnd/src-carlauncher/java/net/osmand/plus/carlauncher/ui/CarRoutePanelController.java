package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.views.controls.VerticalWidgetPanel;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.Set;

/** Keeps route information usable on short, wide head-unit displays. */
public final class CarRoutePanelController implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String MODE_CAR_CARD = "car_card";

    private final MapActivity activity;
    private final FrameLayout mapContainer;
    private final CarLauncherSettings settings;
    private final SharedPreferences preferences;

    private VerticalWidgetPanel nativeBottomPanel;
    private int lastMapWidth;
    private final View.OnLayoutChangeListener mapLayoutListener = (v, left, top, right, bottom,
            oldLeft, oldTop, oldRight, oldBottom) -> {
        int width = right - left;
        if (width != lastMapWidth) {
            lastMapWidth = width;
            applyLayoutMode();
        }
    };

    public CarRoutePanelController(@NonNull MapActivity activity, @NonNull FrameLayout mapContainer) {
        this.activity = activity;
        this.mapContainer = mapContainer;
        settings = CarLauncherSettings.getInstance(activity);
        preferences = activity.getSharedPreferences("car_launcher_prefs", Context.MODE_PRIVATE);

        mapContainer.addOnLayoutChangeListener(mapLayoutListener);
        preferences.registerOnSharedPreferenceChangeListener(this);
        mapContainer.post(this::applyLayoutMode);
    }

    private void applyLayoutMode() {
        View panel = activity.findViewById(R.id.map_bottom_widgets_panel);
        nativeBottomPanel = panel instanceof VerticalWidgetPanel ? (VerticalWidgetPanel) panel : null;
        boolean carCardMode = MODE_CAR_CARD.equals(settings.getRouteInfoLayout());

        if (nativeBottomPanel != null) {
            nativeBottomPanel.setVisibilityAllowed(true);
            MapWidgetInfo routeWidget = carCardMode ? ensureRouteWidgetEnabled() : findRouteWidget(false);
            compactNativeBottomPanel(nativeBottomPanel, carCardMode);
            nativeBottomPanel.update(null);
            styleRouteWidget(routeWidget, carCardMode);
        }
    }

    private void styleRouteWidget(MapWidgetInfo routeWidget, boolean cardMode) {
        View panel = nativeBottomPanel;
        panel.setAlpha(1f);
        panel.setTranslationY(0f);
        panel.setElevation(0f);
        panel.setBackgroundColor(Color.TRANSPARENT);
        panel.setPadding(0, 0, 0, 0);
        if (routeWidget == null || routeWidget.widget == null) return;

        View routeView = routeWidget.widget.getView();
        routeView.setElevation(cardMode ? dp(4) : 0);
        try {
            routeView.setBackgroundResource(cardMode ? R.drawable.bg_car_route_info_card : 0);
        } catch (Resources.NotFoundException e) {
            // A theme attribute resolving to a drawable instead of a color must never
            // prevent the launcher from opening on vendor Android builds.
            routeView.setBackgroundColor(Color.TRANSPARENT);
        }
        routeView.setPadding(0, 0, 0, 0);
    }

    private MapWidgetInfo ensureRouteWidgetEnabled() {
        MapWidgetInfo routeWidget = findRouteWidget(true);
        if (routeWidget == null) return null;

        ApplicationMode appMode = getApp().getSettings().getApplicationMode();
        ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
        if (!routeWidget.isEnabledForAppMode(appMode, layoutMode)) {
            getWidgetRegistry().enableDisableWidgetForMode(appMode, routeWidget,
                    true, layoutMode, false);
        }
        return routeWidget;
    }

    private MapWidgetInfo findRouteWidget(boolean preferDefault) {
        ApplicationMode appMode = getApp().getSettings().getApplicationMode();
        ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
        MapWidgetInfo defaultWidget = null;
        MapWidgetInfo fallback = null;
        Set<MapWidgetInfo> widgets = getWidgetRegistry().getWidgetsForPanel(WidgetsPanel.BOTTOM);
        for (MapWidgetInfo info : widgets) {
            if (info.getWidgetType() != WidgetType.ROUTE_INFO) continue;
            if (info.isEnabledForAppMode(appMode, layoutMode)) return info;
            if (!info.isCustomWidget()) defaultWidget = info;
            if (fallback == null) fallback = info;
        }
        return preferDefault && defaultWidget != null ? defaultWidget : fallback;
    }

    private MapWidgetRegistry getWidgetRegistry() {
        return activity.getMapLayers().getMapWidgetRegistry();
    }

    private OsmandApplication getApp() {
        return (OsmandApplication) activity.getApplication();
    }

    private void compactNativeBottomPanel(@NonNull View panel, boolean cardMode) {
        ViewGroup.LayoutParams rawParams = panel.getLayoutParams();
        int availableWidth = mapContainer.getWidth();
        if (availableWidth <= 0) {
            mapContainer.post(() -> compactNativeBottomPanel(panel, cardMode));
            return;
        }
        int desiredWidth = Math.min(dp(430), Math.max(dp(280), Math.round(availableWidth * 0.58f)));
        rawParams.width = desiredWidth;
        rawParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (rawParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rawParams;
            params.gravity = Gravity.BOTTOM | Gravity.START;
            params.setMargins(0, 0, 0, 0);
        }
        panel.setLayoutParams(rawParams);
        panel.requestLayout();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (CarLauncherSettings.KEY_ROUTE_INFO_LAYOUT.equals(key)) {
            mapContainer.post(this::applyLayoutMode);
        }
    }

    public void destroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        mapContainer.removeOnLayoutChangeListener(mapLayoutListener);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
