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
    @NonNull
    @Override
    public View createView() {
        // Modern Kart Yapisi
        android.widget.FrameLayout rootFrame = new android.widget.FrameLayout(context);
        rootFrame.setPadding(16, 16, 16, 16);
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(16, 16, 16, 16);
        container.setLayoutParams(params);

        // Ikon (Pusula)
        android.widget.ImageView iconView = new android.widget.ImageView(context);
        iconView.setImageResource(android.R.drawable.ic_menu_compass);
        iconView.setColorFilter(android.graphics.Color.RED); // Pusula kirmizi
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(64, 64);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconParams.bottomMargin = 8;
        container.addView(iconView, iconParams);

        // Label
        labelText = new TextView(context);
        labelText.setText("YON");
        labelText.setTextColor(android.graphics.Color.LTGRAY);
        labelText.setTextSize(12);
        labelText.setGravity(Gravity.CENTER);
        container.addView(labelText);

        // Yon Metni
        directionText = new TextView(context);
        directionText.setTextColor(android.graphics.Color.WHITE);
        directionText.setTextSize(24);
        directionText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        directionText.setGravity(Gravity.CENTER);
        directionText.setText("--");
        container.addView(directionText);

        rootFrame.addView(container);
        rootView = rootFrame;
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
