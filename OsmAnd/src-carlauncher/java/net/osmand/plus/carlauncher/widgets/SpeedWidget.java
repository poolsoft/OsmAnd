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
        rootFrame.setPadding(0, 0, 0, 0); // No extra padding

        // Arka Plan (XML Kaynagi)
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_modern);

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
        int size = dpToPx(48);
        LinearLayout.LayoutParams limitParams = new LinearLayout.LayoutParams(size, size);
        limitContainer.addView(limitText, limitParams);

        contentLayout.addView(limitContainer);

        // --- SAG: Mevcut Hız ve Birim (Vertical) ---
        LinearLayout speedContainer = new LinearLayout(context);
        speedContainer.setOrientation(LinearLayout.VERTICAL);
        speedContainer.setGravity(Gravity.CENTER);

        // Hız Değeri
        speedText = new TextView(context);
        speedText.setTextColor(Color.parseColor("#6582c1ff")); // Beyaz (Daha net)
        speedText.setTextSize(64); // Larger for 7-segment
        speedText.setGravity(Gravity.CENTER);
        speedText.setText("--");
        speedText.setIncludeFontPadding(false);
        // Auto-resize for Speed (Min 24sp, Max 80sp)
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                speedText, 24, 80, 2, android.util.TypedValue.COMPLEX_UNIT_SP);

        try {
            // Typeface digitalFont = Typeface.createFromAsset(context.getAssets(),
            // "fonts/curved-seven-segment.ttf");
            Typeface digitalFont = Typeface.createFromAsset(context.getAssets(), "fonts/Cross Boxed.ttf");
            speedText.setTypeface(digitalFont);
        } catch (Exception e) {
            speedText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }

        speedContainer.addView(speedText);

        // Alignments - Make it compact
        limitContainer.setPadding(0, 0, dpToPx(8), 0);
        speedText.setTextSize(64); // Reduced from 68
        
        // Removed Unit Text as per user request
        contentLayout.addView(speedContainer); // Add speed container back!

        // Reduce vertical space by centering tightly
        contentLayout.setGravity(Gravity.CENTER);
        
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

        float currentSpeed = 0;
        // 1. Mevcut Hız ve Birim
        if (speedText != null) {
            String speedStr = "--";
            String unitStr = "";

            if (location.hasSpeed()) {
                currentSpeed = location.getSpeed();
                FormattedValue formatted = OsmAndFormatter.getFormattedSpeedValue(currentSpeed, app);
                speedStr = formatted.value;
                unitStr = formatted.unit;
            }

            final String finalSpeed = speedStr;
            final String finalUnit = unitStr;

            speedText.post(() -> {
                speedText.setText(finalSpeed);
            });
        }

        // 2. Hız Limiti ve Uyarı
        updateMaxSpeed(location, currentSpeed);
    }

    private void updateMaxSpeed(Location location, float currentSpeed) {
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

                // Speed Limit Warning Logic
                // Tolerance: +2 km/h (~0.55 m/s) buffer
                float tolerance = 0.55f;
                if (currentSpeed > (maxSpeed + tolerance)) {
                    // Warning: Red Circle, White Text
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    gd.setColor(Color.parseColor("#CCFF0000")); // Semi-transparent Red
                    gd.setStroke(dpToPx(4), Color.RED);
                    limitText.setBackground(gd);
                    limitText.setTextColor(Color.WHITE);
                } else {
                    // Normal
                    limitText.setBackgroundResource(net.osmand.plus.R.drawable.bg_speed_limit);
                    limitText.setTextColor(Color.BLACK);
                }

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
