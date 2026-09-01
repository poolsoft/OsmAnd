package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.carlauncher.telemetry.TelemetryManager;
import net.osmand.plus.views.controls.VerticalWidgetPanel;

/** Keeps route information usable on short, wide head-unit displays. */
public final class CarRoutePanelController implements TelemetryManager.TelemetryListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String MODE_CAR_CARD = "car_card";

    private final MapActivity activity;
    private final FrameLayout mapContainer;
    private final CarLauncherSettings settings;
    private final SharedPreferences preferences;
    private final TelemetryManager telemetryManager;

    private VerticalWidgetPanel nativeBottomPanel;
    private View card;
    private TextView arrivalValue;
    private TextView remainingValue;
    private TextView distanceValue;
    private TelemetryManager.NavigationState lastNavigationState;
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
        telemetryManager = TelemetryManager.getInstance((OsmandApplication) activity.getApplication());

        createCard();
        mapContainer.addOnLayoutChangeListener(mapLayoutListener);
        preferences.registerOnSharedPreferenceChangeListener(this);
        telemetryManager.addListener(this);
        mapContainer.post(this::applyLayoutMode);
    }

    private void createCard() {
        card = activity.getLayoutInflater().inflate(R.layout.car_route_info_card, mapContainer, false);
        arrivalValue = card.findViewById(R.id.car_route_arrival_value);
        remainingValue = card.findViewById(R.id.car_route_remaining_value);
        distanceValue = card.findViewById(R.id.car_route_distance_value);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.START;
        int margin = dp(10);
        params.setMargins(margin, margin, margin, margin);
        mapContainer.addView(card, params);
    }

    private void applyLayoutMode() {
        View panel = activity.findViewById(R.id.map_bottom_widgets_panel);
        nativeBottomPanel = panel instanceof VerticalWidgetPanel ? (VerticalWidgetPanel) panel : null;
        boolean carCardMode = MODE_CAR_CARD.equals(settings.getRouteInfoLayout());

        if (nativeBottomPanel != null) {
            nativeBottomPanel.setVisibilityAllowed(!carCardMode);
            if (carCardMode) {
                nativeBottomPanel.setVisibility(View.GONE);
            } else {
                compactNativeBottomPanel(nativeBottomPanel);
                nativeBottomPanel.update(null);
            }
        }
        updateCard(lastNavigationState);
    }

    private void compactNativeBottomPanel(@NonNull View panel) {
        ViewGroup.LayoutParams rawParams = panel.getLayoutParams();
        int availableWidth = mapContainer.getWidth();
        if (availableWidth <= 0) {
            mapContainer.post(() -> compactNativeBottomPanel(panel));
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
    public void onTelemetryUpdated(TelemetryManager.LocationState loc,
                                   TelemetryManager.NavigationState nav,
                                   TelemetryManager.ObdState obd) {
        lastNavigationState = nav;
        updateCard(nav);
    }

    private void updateCard(TelemetryManager.NavigationState nav) {
        if (card == null) return;
        boolean show = MODE_CAR_CARD.equals(settings.getRouteInfoLayout())
                && nav != null && nav.isActive
                && !isEmpty(nav.routeDistanceStr) && !isEmpty(nav.routeRemainingTimeStr);
        if (!show) {
            card.setVisibility(View.GONE);
            return;
        }
        arrivalValue.setText(nav.routeArrivalTimeStr);
        remainingValue.setText(nav.routeRemainingTimeStr);
        distanceValue.setText(nav.routeDistanceStr);
        card.setVisibility(View.VISIBLE);
        card.bringToFront();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (CarLauncherSettings.KEY_ROUTE_INFO_LAYOUT.equals(key)) {
            mapContainer.post(this::applyLayoutMode);
        }
    }

    public void destroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        telemetryManager.removeListener(this);
        mapContainer.removeOnLayoutChangeListener(mapLayoutListener);
        if (card != null && card.getParent() == mapContainer) {
            mapContainer.removeView(card);
        }
        card = null;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
