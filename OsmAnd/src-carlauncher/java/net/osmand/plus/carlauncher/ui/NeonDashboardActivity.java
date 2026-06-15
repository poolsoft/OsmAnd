package net.osmand.plus.carlauncher.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.telemetry.TelemetryManager;
import net.osmand.plus.carlauncher.widgets.view.FuturisticSpeedometerView;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.R;

public class NeonDashboardActivity extends Activity implements TelemetryManager.TelemetryListener {

    private FuturisticSpeedometerView speedometerView;
    private TelemetryManager telemetryManager;

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

        telemetryManager = TelemetryManager.getInstance((OsmandApplication) getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (telemetryManager != null) {
            telemetryManager.addListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (telemetryManager != null) {
            telemetryManager.removeListener(this);
        }
    }

    // --- TelemetryListener ---
    @Override
    public void onTelemetryUpdated(float speedKmh, double altitudeMeters, float bearing, int engineRpm) {
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
    }

    private String getStreetName() {
        try {
            OsmandApplication app = (OsmandApplication) getApplication();
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
