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

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.views.controls.VerticalWidgetPanel;

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
            styleNativeBottomPanel(nativeBottomPanel, carCardMode);
            compactNativeBottomPanel(nativeBottomPanel, carCardMode);
            nativeBottomPanel.update(null);
        }
    }

    private void styleNativeBottomPanel(@NonNull View panel, boolean cardMode) {
        panel.setAlpha(1f);
        panel.setTranslationY(0f);
        panel.setElevation(cardMode ? dp(6) : 0);
        try {
            panel.setBackgroundResource(cardMode ? R.drawable.bg_car_route_info_card : 0);
        } catch (Resources.NotFoundException e) {
            // A theme attribute resolving to a drawable instead of a color must never
            // prevent the launcher from opening on vendor Android builds.
            panel.setBackgroundColor(Color.TRANSPARENT);
        }
        panel.setPadding(cardMode ? dp(4) : 0, 0, cardMode ? dp(4) : 0, 0);
        if (!cardMode) panel.setBackgroundColor(Color.TRANSPARENT);
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
            int margin = cardMode ? dp(8) : 0;
            params.setMargins(margin, 0, margin, margin);
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
