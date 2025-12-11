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

/**
 * Yon widget - Pusula yonunu gosterir.
 */
public class DirectionWidget extends BaseWidget implements OsmAndLocationProvider.OsmAndLocationListener {

    private TextView labelText;
    private TextView directionText;
    private final OsmandApplication app;

    public DirectionWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "direction", "Yon");
        this.app = app;
        this.order = 2;
    }

    @NonNull
    @Override
    public View createView() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(16, 16, 16, 16);

        labelText = new TextView(context);
        labelText.setText("YON");
        labelText.setTextColor(0x88FFFFFF);
        labelText.setTextSize(12);
        labelText.setGravity(Gravity.CENTER);

        directionText = new TextView(context);
        directionText.setTextColor(0xFFFFFFFF);
        directionText.setTextSize(24);
        directionText.setGravity(Gravity.CENTER);
        directionText.setText("--");

        container.addView(labelText);
        container.addView(directionText);

        rootView = container;
        return rootView;
    }

    @Override
    public void update() {
        // Konum guncellemesi ile otomatik cagrilir
    }

    @Override
    public void updateLocation(Location location) {
        if (directionText != null && location != null) {
            if (location.hasBearing()) {
                int bearing = (int) location.getBearing();
                String direction = getDirectionString(bearing);
                String text = direction + " " + bearing + "°";
                directionText.post(() -> directionText.setText(text));
            } else {
                directionText.post(() -> directionText.setText("--"));
            }
        }
    }

    /**
     * Bearing degerini yon stringine cevir.
     */
    private String getDirectionString(int bearing) {
        if (bearing >= 337.5 || bearing < 22.5)
            return "K"; // Kuzey
        if (bearing >= 22.5 && bearing < 67.5)
            return "KD"; // Kuzeydogu
        if (bearing >= 67.5 && bearing < 112.5)
            return "D"; // Dogu
        if (bearing >= 112.5 && bearing < 157.5)
            return "GD"; // Guneydogu
        if (bearing >= 157.5 && bearing < 202.5)
            return "G"; // Guney
        if (bearing >= 202.5 && bearing < 247.5)
            return "GB"; // Guneybati
        if (bearing >= 247.5 && bearing < 292.5)
            return "B"; // Bati
        if (bearing >= 292.5 && bearing < 337.5)
            return "KB"; // Kuzeybati
        return "";
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
