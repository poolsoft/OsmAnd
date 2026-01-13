package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.view.AnalogSpeedometerView;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.routing.RoutingHelper;

/**
 * Hız widget - S/M/L destegi.
 * S: Hız
 * M: Hız + Limit
 * L: Analog Hız
 */
public class SpeedWidget extends BaseWidget implements OsmAndLocationProvider.OsmAndLocationListener {

    private final OsmandApplication app;
    
    // Containers
    private FrameLayout rootFrame;
    private View digitalView;
    private AnalogSpeedometerView analogView;

    // Digital UI
    private TextView speedText;
    private TextView limitText;
    private LinearLayout limitContainer;

    public SpeedWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "speed", "Hiz");
        this.app = app;
        this.order = 1;
    }

    @NonNull
    @Override
    public View createView() {
        rootFrame = new FrameLayout(context);
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_modern);
        rootFrame.setPadding(0, 0, 0, 0);

        // Default Layout
        setupDigitalLayout(rootFrame);

        rootView = rootFrame;
        return rootView;
    }

    private void setupDigitalLayout(ViewGroup root) {
        root.removeAllViews();
        analogView = null;

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // --- Limit (Left) ---
        limitContainer = new LinearLayout(context);
        limitContainer.setOrientation(LinearLayout.VERTICAL);
        limitContainer.setGravity(Gravity.CENTER);
        limitContainer.setVisibility(View.GONE);
        limitContainer.setPadding(0, 0, dpToPx(16), 0);

        limitText = new TextView(context);
        limitText.setBackgroundResource(net.osmand.plus.R.drawable.bg_speed_limit);
        limitText.setTextColor(Color.BLACK);
        limitText.setTextSize(22);
        limitText.setTypeface(Typeface.DEFAULT_BOLD);
        limitText.setGravity(Gravity.CENTER);
        limitText.setText("--");
        
        int size = dpToPx(48);
        limitContainer.addView(limitText, new LinearLayout.LayoutParams(size, size));
        contentLayout.addView(limitContainer);

        // --- Speed (Right) ---
        LinearLayout speedContainer = new LinearLayout(context);
        speedContainer.setOrientation(LinearLayout.VERTICAL);
        speedContainer.setGravity(Gravity.CENTER);

        speedText = new TextView(context);
        speedText.setTextColor(Color.parseColor("#6582c1ff"));
        speedText.setTextSize(64);
        speedText.setGravity(Gravity.CENTER);
        speedText.setText("--");
        speedText.setIncludeFontPadding(false);
        try {
            Typeface digitalFont = Typeface.createFromAsset(context.getAssets(), "fonts/Cross Boxed.ttf");
            speedText.setTypeface(digitalFont);
        } catch (Exception e) {
            speedText.setTypeface(Typeface.DEFAULT_BOLD);
        }
        
        speedContainer.addView(speedText);
        contentLayout.addView(speedContainer); // Re-add speed container

        root.addView(contentLayout);
        digitalView = contentLayout;
        
        // Re-apply size logic if needed
        applyDigitalVisibility();
    }

    private void setupAnalogLayout(ViewGroup root) {
        root.removeAllViews();
        digitalView = null;
        limitContainer = null;
        limitText = null;
        speedText = null;

        analogView = new AnalogSpeedometerView(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        int margin = dpToPx(8);
        params.setMargins(margin, margin, margin, margin);
        analogView.setLayoutParams(params);

        root.addView(analogView);
    }

    private void applyDigitalVisibility() {
        if (limitContainer != null) {
            if (size == WidgetSize.SMALL) {
                limitContainer.setVisibility(View.GONE);
                if (speedText != null) speedText.setTextSize(48); // Smaller for compact
            } else {
                // Visibility set by updateMaxSpeed
                 if (speedText != null) speedText.setTextSize(64); // Normal
            }
        }
    }

    @Override
    protected void onSizeChanged(WidgetSize newSize) {
        if (rootFrame == null) return;

        if (newSize == WidgetSize.LARGE) {
            setupAnalogLayout(rootFrame);
        } else {
            // If switching from Analog -> Digital, rebuild
            if (digitalView == null) {
                setupDigitalLayout(rootFrame);
            }
            // Logic for S vs M (Limit visibility)
            applyDigitalVisibility();
        }
        
        // Trigger update to refresh data on new views
        updateLocation(app.getLocationProvider().getLastKnownLocation());
    }

    @Override
    public void update() {}

    @Override
    public void updateLocation(Location location) {
        if (location == null) return;

        float currentSpeed = 0;
        if (location.hasSpeed()) {
            currentSpeed = location.getSpeed();
        }

        // 1. Digital View Update
        if (speedText != null) {
             FormattedValue formatted = OsmAndFormatter.getFormattedSpeedValue(currentSpeed, app);
             final String val = formatted.value;
             speedText.post(() -> speedText.setText(val));
        }

        // 2. Analog View Update
        if (analogView != null) {
             float kmh = currentSpeed * 3.6f;
             analogView.setSpeed(kmh);
        }

        // 3. Limit Update
        updateMaxSpeed(location, currentSpeed);
    }
    
    private void updateMaxSpeed(Location location, float currentSpeed) {
        float maxSpeed = getMaxSpeed(location); // in m/s

        // Digital Logic
        if (limitText != null && limitContainer != null) {
             if (size == WidgetSize.SMALL) {
                 limitContainer.setVisibility(View.GONE);
             } else {
                 if (maxSpeed > 0 && maxSpeed != RouteDataObject.NONE_MAX_SPEED) {
                     limitContainer.setVisibility(View.VISIBLE);
                     FormattedValue formatted = OsmAndFormatter.getFormattedSpeedValue(maxSpeed, app);
                     
                     // Clean string ("50 km/h" -> "50")
                     String limitStr = formatted.value;
                     try { limitStr = limitStr.split(" ")[0]; } catch(Exception e){}
                     
                     limitText.setText(limitStr);
                     
                     // Color Logic (Gradual)
                     // maxSpeed is m/s. currentSpeed is m/s.
                     float diff = currentSpeed - maxSpeed;
                     // Tolerance: e.g. up to 10% or 5km/h
                     // Let's use simplified km/h based logic for visual check
                     float diffKmh = diff * 3.6f;
                     
                     int defaultColor = Color.parseColor("#6582c1ff"); // White
                     int warningColor = Color.parseColor("#FFD54F"); // Amber
                     int dangerColor = Color.parseColor("#FF5252"); // Red
                     
                     if (diffKmh > 5) { // +5 km/h over
                         if (speedText != null) speedText.setTextColor(dangerColor);
                         
                         // Limit Box Danger
                         android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                         gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                         gd.setColor(Color.parseColor("#CCFF0000"));
                         gd.setStroke(dpToPx(4), Color.RED);
                         limitText.setBackground(gd);
                         limitText.setTextColor(Color.WHITE);
                         
                     } else if (diffKmh > 0) { // 0-5 km/h over
                         if (speedText != null) speedText.setTextColor(warningColor);
                         
                         // Limit Box Warning
                         limitText.setBackgroundResource(net.osmand.plus.R.drawable.bg_speed_limit);
                         limitText.setTextColor(Color.RED); // Text Red
                     } else {
                         // Normal
                         if (speedText != null) speedText.setTextColor(defaultColor);
                         limitText.setBackgroundResource(net.osmand.plus.R.drawable.bg_speed_limit);
                         limitText.setTextColor(Color.BLACK);
                     }

                 } else {
                     limitContainer.setVisibility(View.GONE);
                     if (speedText != null) speedText.setTextColor(Color.parseColor("#6582c1ff"));
                 }
             }
        }
        
        // Analog Logic
        if (analogView != null) {
            if (maxSpeed > 0 && maxSpeed != RouteDataObject.NONE_MAX_SPEED) {
                analogView.setSpeedLimit(maxSpeed * 3.6f);
            } else {
                analogView.setSpeedLimit(0);
            }
        }
    }

    private float getMaxSpeed(Location location) {
        RoutingHelper routingHelper = app.getRoutingHelper();
        if (routingHelper == null) return 0;
        
        if ((!routingHelper.isFollowingMode()
                || routingHelper.isDeviatedFromRoute()
                || (routingHelper.getCurrentGPXRoute() != null && !routingHelper.isCurrentGPXRouteV2()))) {
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

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (app != null) app.getLocationProvider().addLocationListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (app != null) app.getLocationProvider().removeLocationListener(this);
    }
}
