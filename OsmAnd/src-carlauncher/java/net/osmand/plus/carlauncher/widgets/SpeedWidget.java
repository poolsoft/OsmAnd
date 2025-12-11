package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter;

/**
 * Hiz widget - GPS hizini gosterir.
 */
public class SpeedWidget extends BaseWidget implements OsmAndLocationProvider.OsmAndLocationListener {

    private TextView labelText;
    private TextView speedText;
    private final OsmandApplication app;

    public SpeedWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "speed", "Hiz");
        this.app = app;
        this.order = 1;
    }

    @NonNull
    @Override
    public View createView() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(16, 16, 16, 16);

        labelText = new TextView(context);
        labelText.setText("HIZ");
        labelText.setTextColor(0x88FFFFFF);
        labelText.setTextSize(12);
        labelText.setGravity(Gravity.CENTER);

        speedText = new TextView(context);
        speedText.setTextColor(0xFFFFFFFF);
        speedText.setTextSize(36);
        speedText.setGravity(Gravity.CENTER);
        speedText.setText("--");

        container.addView(labelText);
        container.addView(speedText);

        rootView = container;
        return rootView;
    }

    @Override
    public void update() {
        // Konum guncellemesi ile otomatik cagrilir
    }

    @Override
    public void updateLocation(Location location) {
        if (speedText != null && location != null) {
            if (location.hasSpeed()) {
                String speed = OsmAndFormatter.getFormattedSpeed(location.getSpeed(), app);
                speedText.post(() -> speedText.setText(speed));
            } else {
                speedText.post(() -> speedText.setText("--"));
            }
        }
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
