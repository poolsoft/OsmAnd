package net.osmand.plus.carlauncher.ui;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.telemetry.TelemetryManager;
import net.osmand.plus.carlauncher.widgets.view.FuturisticSpeedometerView;
import net.osmand.plus.R;

public class NeonDashboardActivity extends AppCompatActivity implements TelemetryManager.TelemetryListener {

    private FuturisticSpeedometerView speedometerView;
    private TelemetryManager telemetryManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neon_dashboard);

        speedometerView = findViewById(R.id.futuristic_speed);
        ImageButton btnClose = findViewById(R.id.btn_close);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // OsmandApplication baglamindan TelemetryManager ornegini aliyoruz
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
            // Speedometer gorseline akici sekilde cizim yaptiriyoruz
            speedometerView.setSpeed(speedKmh);
        }
    }
}
