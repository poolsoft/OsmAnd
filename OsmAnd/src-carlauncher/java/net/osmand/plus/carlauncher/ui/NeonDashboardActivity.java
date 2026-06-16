package net.osmand.plus.carlauncher.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.telemetry.TelemetryManager;
import net.osmand.plus.carlauncher.widgets.view.FuturisticSpeedometerView;
import net.osmand.plus.R;

public class NeonDashboardActivity extends Activity implements TelemetryManager.TelemetryListener {

    private FuturisticSpeedometerView speedometerView;
    private TelemetryManager telemetryManager;
    private OsmandApplication app;

    // Lokasyon (Sol/Sag)
    private TextView tvLeftMetric;
    private TextView tvRightMetric;

    // Navigasyon (Ust)
    private LinearLayout navContainer;
    private ImageView navIcon;
    private TextView navDistance;
    private TextView navInstruction;
    private TextView navEta;

    // OBD (Alt)
    private LinearLayout obdContainer;
    private TextView obdRpm;
    private TextView obdTemp;
    private TextView obdVolt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neon_dashboard);

        speedometerView = findViewById(R.id.futuristic_speed);
        ImageButton btnClose = findViewById(R.id.btn_close);
        
        tvLeftMetric = findViewById(R.id.tv_left_metric);
        tvRightMetric = findViewById(R.id.tv_right_metric);

        navContainer = findViewById(R.id.nav_container);
        navIcon = findViewById(R.id.nav_icon);
        navDistance = findViewById(R.id.nav_distance);
        navInstruction = findViewById(R.id.nav_instruction);
        navEta = findViewById(R.id.nav_eta);

        obdContainer = findViewById(R.id.obd_container);
        obdRpm = findViewById(R.id.obd_rpm);
        obdTemp = findViewById(R.id.obd_temp);
        obdVolt = findViewById(R.id.obd_volt);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        app = (OsmandApplication) getApplication();
        telemetryManager = TelemetryManager.getInstance(app);
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

    @Override
    public void onTelemetryUpdated(TelemetryManager.LocationState loc, 
                                   TelemetryManager.NavigationState nav, 
                                   TelemetryManager.ObdState obd) {
        
        // 1. HIZ VE LOKASYON
        if (speedometerView != null) {
            speedometerView.setSpeed(loc.speedKmh);
        }
        if (tvRightMetric != null) {
            tvRightMetric.setText((int) loc.altitudeMeters + " m");
        }

        // 2. NAVIGASYON
        if (nav.isActive) {
            if (navContainer != null) navContainer.setVisibility(View.VISIBLE);
            if (navIcon != null && nav.turnIconRes != 0) navIcon.setImageResource(nav.turnIconRes);
            if (navDistance != null) navDistance.setText(nav.distanceStr);
            if (navInstruction != null) navInstruction.setText(nav.instructionStr);
            if (navEta != null) navEta.setText(nav.etaStr);
            
            // Eger navigasyon varsa sol panele pusula koy, cunku yukarida sokak var
            if (tvLeftMetric != null) {
                tvLeftMetric.setText(getCompassDirection(loc.bearing));
            }
        } else {
            if (navContainer != null) navContainer.setVisibility(View.GONE);
            
            // Navigasyon yoksa sol panelde sokak adi gostermeye calis
            if (tvLeftMetric != null) {
                String street = getStreetNameFallback();
                if (!street.isEmpty()) {
                    tvLeftMetric.setText(street);
                } else {
                    tvLeftMetric.setText(getCompassDirection(loc.bearing));
                }
            }
        }

        // 3. OBD
        if (obdContainer != null) obdContainer.setVisibility(View.VISIBLE);
        if (obdRpm != null) obdRpm.setText(obd.rpm);
        if (obdTemp != null) obdTemp.setText(obd.temp);
        if (obdVolt != null) obdVolt.setText(obd.volt);
    }

    private String getCompassDirection(float bearing) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = Math.round((bearing % 360) / 45);
        if (index >= 8) index = 0;
        return directions[index];
    }

    private String getStreetNameFallback() {
        try {
            net.osmand.plus.routing.RoutingHelper routingHelper = app.getRoutingHelper();
            if (routingHelper != null) {
                net.osmand.plus.routing.NextDirectionInfo info = new net.osmand.plus.routing.NextDirectionInfo();
                net.osmand.plus.routing.CurrentStreetName csn = routingHelper.getCurrentName(info, false);
                if (csn != null && csn.text != null) {
                    return csn.text;
                }
            }
        } catch (Exception e) {}
        return "";
    }
}
