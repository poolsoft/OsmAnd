package net.osmand.plus.carlauncher.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.view.FuturisticSpeedometerView;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.R;

public class NeonDashboardActivity extends Activity implements OsmAndLocationProvider.OsmAndLocationListener {

    private FuturisticSpeedometerView speedometerView;
    private OsmandApplication app;

    private TextView tvLeftMetric;
    private TextView tvRightMetric;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neon_dashboard);

        speedometerView = findViewById(R.id.futuristic_speed);
        ImageButton btnClose = findViewById(R.id.btn_close);
        tvLeftMetric = findViewById(R.id.tv_left_metric);
        tvRightMetric = findViewById(R.id.tv_right_metric);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        app = (OsmandApplication) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (app != null && app.getLocationProvider() != null) {
            app.getLocationProvider().addLocationListener(this);
            // Ilk giriste son bilinen lokasyonu basalim
            updateLocation(app.getLocationProvider().getLastKnownLocation());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (app != null && app.getLocationProvider() != null) {
            app.getLocationProvider().removeLocationListener(this);
        }
    }

    @Override
    public void updateLocation(Location location) {
        if (location == null) return;

        float speedKmh = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;
        double altitudeMeters = location.hasAltitude() ? location.getAltitude() : 0.0;
        float bearing = location.hasBearing() ? location.getBearing() : 0f;

        runOnUiThread(() -> {
            if (speedometerView != null) {
                speedometerView.setSpeed(speedKmh);
            }

            if (tvRightMetric != null) {
                tvRightMetric.setText((int) altitudeMeters + " m");
            }

            if (tvLeftMetric != null) {
                String streetName = getStreetName();
                if (streetName != null && !streetName.trim().isEmpty()) {
                    tvLeftMetric.setText(streetName);
                } else {
                    tvLeftMetric.setText(getCompassDirection(bearing));
                }
            }
        });
    }

    private String getStreetName() {
        try {
            RoutingHelper routingHelper = app.getRoutingHelper();
            if (routingHelper != null) {
                NextDirectionInfo info = new NextDirectionInfo();
                CurrentStreetName csn = routingHelper.getCurrentName(info, false);
                if (csn != null && csn.text != null) {
                    return csn.text;
                }
            }
        } catch (Exception e) {
            // Ignore if street name cannot be retrieved
        }
        return "";
    }

    private String getCompassDirection(float bearing) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = Math.round((bearing % 360) / 45);
        if (index >= 8) index = 0;
        return directions[index];
    }
}
