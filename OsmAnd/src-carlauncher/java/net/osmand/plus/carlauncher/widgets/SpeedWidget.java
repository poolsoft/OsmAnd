package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.routing.RoutingHelper;

/**
 * Hız widget - Anlık hızı ve Hız Limitini gosterir.
 */
public class SpeedWidget extends BaseWidget implements OsmAndLocationProvider.OsmAndLocationListener {

    private TextView speedText;
    private TextView unitText; // Birim (km/h)
    private TextView limitText; // Hız Limiti
    private LinearLayout limitContainer; // Limiti gizleyip asmak icin
    private final OsmandApplication app;

    public SpeedWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "speed", "Hiz");
        this.app = app;
        this.order = 1;
    }

    @NonNull
    @Override
    public View createView() {
        // Modern Kart Yapisi (FrameLayout)
        android.widget.FrameLayout rootFrame = new android.widget.FrameLayout(context);
        rootFrame.setPadding(12, 12, 12, 12);

        // Arka Plan (XML Kaynagi)
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);

        // Ana Icerik (Horizontal: Limit - Hiz)
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        contentLayout.setLayoutParams(params);

        // --- SOL: Hız Limiti ---
        limitContainer = new LinearLayout(context);
        limitContainer.setOrientation(LinearLayout.VERTICAL);
        limitContainer.setGravity(Gravity.CENTER);
        limitContainer.setVisibility(View.GONE); // Baslangicta gizli
        limitContainer.setPadding(0, 0, dpToPx(16), 0); // Sagdan bosluk

        limitText = new TextView(context);
        limitText.setBackgroundResource(net.osmand.plus.R.drawable.bg_speed_limit);
        limitText.setTextColor(Color.BLACK); // Beyaz uzerine siyah
        limitText.setTextSize(22);
        limitText.setTypeface(Typeface.DEFAULT_BOLD);
        limitText.setGravity(Gravity.CENTER);
        limitText.setText("50");
        limitText.setIncludeFontPadding(false);

        // Daire boyutu icin layout params
        int size = dpToPx(52);
        LinearLayout.LayoutParams limitParams = new LinearLayout.LayoutParams(size, size);
        limitContainer.addView(limitText, limitParams);

        contentLayout.addView(limitContainer);

        // --- SAG: Mevcut Hız ve Birim (Vertical) ---
        LinearLayout speedContainer = new LinearLayout(context);
        speedContainer.setOrientation(LinearLayout.VERTICAL);
        speedContainer.setGravity(Gravity.CENTER);

        // Hız Değeri
        speedText = new TextView(context);
        speedText.setTextColor(Color.parseColor("#FFFFFF")); // Beyaz (Daha net)
        speedText.setTextSize(64); // Larger for 7-segment
        speedText.setGravity(Gravity.CENTER);
        speedText.setText("--");
        speedText.setIncludeFontPadding(false);

        try {
            Typeface digitalFont = Typeface.createFromAsset(context.getAssets(), "fonts/curved-seven-segment.ttf");
            speedText.setTypeface(digitalFont);
        } catch (Exception e) {
            speedText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }

        speedContainer.addView(speedText);

        // Birim (km/h)
        unitText = new TextView(context);
        unitText.setTextColor(Color.LTGRAY);
        unitText.setTextSize(14);
        unitText.setGravity(Gravity.CENTER);
        unitText.setText("km/h");
        unitText.setTranslationY(-dpToPx(4)); // Sayiya yaklastir

        speedContainer.addView(unitText);

        contentLayout.addView(speedContainer);

        rootFrame.addView(contentLayout);
        rootView = rootFrame;
        return rootView;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void update() {
        // Konum guncellemesi ile otomatik cagrilir
    }

    @Override
    public void updateLocation(Location location) {
        if (location == null)
            return;

        // 1. Mevcut Hız ve Birim
        if (speedText != null) {
            String speedStr = "--";
            String unitStr = "";

            if (location.hasSpeed()) {
                FormattedValue formatted = OsmAndFormatter.getFormattedSpeedValue(location.getSpeed(), app);
                speedStr = formatted.value;
                unitStr = formatted.unit;
            }

            final String finalSpeed = speedStr;
            final String finalUnit = unitStr;

            speedText.post(() -> {
                speedText.setText(finalSpeed);
                if (unitText != null)
                    unitText.setText(finalUnit);
            });
        }

        // 2. Hız Limiti
        updateMaxSpeed(location);
    }

    private void updateMaxSpeed(Location location) {
        if (limitText == null || limitContainer == null)
            return;

        float maxSpeed = getMaxSpeed(location);

        limitText.post(() -> {
            if (maxSpeed > 0 && maxSpeed != RouteDataObject.NONE_MAX_SPEED) {
                FormattedValue formatted = OsmAndFormatter.getFormattedSpeedValue(maxSpeed, app);
                // Sadece sayiyi al (km/h olmadan)
                String limitStr = formatted.value;
                try {
                    String[] parts = limitStr.trim().split(" ");
                    if (parts.length > 0)
                        limitStr = parts[0];
                } catch (Exception e) {
                }

                limitText.setText(limitStr);
                limitContainer.setVisibility(View.VISIBLE);
            } else {
                limitContainer.setVisibility(View.GONE);
            }
        });
    }

    private float getMaxSpeed(Location location) {
        RoutingHelper routingHelper = app.getRoutingHelper();
        if (routingHelper == null)
            return 0;

        // Logic from MaxSpeedWidget
        if ((!routingHelper.isFollowingMode()
                || routingHelper.isDeviatedFromRoute()
                || (routingHelper.getCurrentGPXRoute() != null && !routingHelper.isCurrentGPXRouteV2()))) {
            // MapViewLinkedToLocation kontrolu yerine direkt locationProvider'dan bakalim
            if (app.getLocationProvider() != null) {
                RouteDataObject routeObject = app.getLocationProvider().getLastKnownRouteSegment();
                if (routeObject != null) {
                    boolean direction = routeObject.bearingVsRouteDirection(location);
                    return routeObject.getMaximumSpeed(direction);
                }
            }
        } else {
            return routingHelper.getCurrentMaxSpeed();
        }
        return 0;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (app != null) {
            app.getLocationProvider().addLocationListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (app != null) {
            app.getLocationProvider().removeLocationListener(this);
        }
    }
}
