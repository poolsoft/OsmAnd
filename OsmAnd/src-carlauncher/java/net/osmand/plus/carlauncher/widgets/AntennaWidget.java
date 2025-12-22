package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.antenna.AntennaManager;

import java.util.Locale;

/**
 * Widget for Antenna Alignment.
 * Connects to AntennaManager to display A/B points and calculated angles.
 */
public class AntennaWidget extends BaseWidget implements AntennaManager.AntennaListener {

    public static final String ACTION_PICK_ANTENNA_POINT = "net.osmand.carlauncher.PICK_ANTENNA_POINT";
    public static final String EXTRA_POINT_TYPE = "point_type"; // "A" or "B"

    private TextView textPointA, textPointB;
    private TextView valDistance, valAzimuth, valElevation;
    private ImageView compassArrow;

    private final AntennaManager manager;

    public AntennaWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "antenna", "Anten");
        this.manager = AntennaManager.getInstance(context);
        this.order = 4; // Place after Music
    }

    @NonNull
    @Override
    public View createView() {
        View view = LayoutInflater.from(context).inflate(net.osmand.plus.R.layout.widget_antenna_modern, null);

        // Bind Views
        textPointA = view.findViewById(net.osmand.plus.R.id.text_point_a);
        textPointB = view.findViewById(net.osmand.plus.R.id.text_point_b);
        valDistance = view.findViewById(net.osmand.plus.R.id.val_distance);
        valAzimuth = view.findViewById(net.osmand.plus.R.id.val_azimuth);
        valElevation = view.findViewById(net.osmand.plus.R.id.val_elevation);
        compassArrow = view.findViewById(net.osmand.plus.R.id.compass_arrow);

        View btnSetA = view.findViewById(net.osmand.plus.R.id.btn_set_a);
        View btnSetB = view.findViewById(net.osmand.plus.R.id.btn_set_b);

        // Listeners
        btnSetA.setOnClickListener(v -> startPickPoint("A"));
        btnSetB.setOnClickListener(v -> startPickPoint("B"));

        // Setup Layout Params
        view.setLayoutParams(new ViewGroup.LayoutParams(dpToPx(300), ViewGroup.LayoutParams.WRAP_CONTENT));

        rootView = view;
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        manager.setListener(this);
        updateUI();
    }

    @Override
    public void onStop() {
        super.onStop();
        manager.setListener(null);
    }

    @Override
    public void update() {
        updateUI();
    }

    @Override
    public void onAntennaPointsChanged() {
        if (rootView != null) {
            rootView.post(this::updateUI);
        }
    }

    private void updateUI() {
        if (rootView == null)
            return;

        AntennaManager.AntennaPoint pA = manager.getPointA();
        AntennaManager.AntennaPoint pB = manager.getPointB();

        textPointA.setText(pA != null ? (pA.name != null ? pA.name : "Lat: " + String.format(Locale.US, "%.4f", pA.lat))
                : "Seçiniz");
        textPointB.setText(pB != null ? (pB.name != null ? pB.name : "Lat: " + String.format(Locale.US, "%.4f", pB.lat))
                : "Seçiniz");

        if (pA != null && pB != null) {
            // Distance
            double distMeters = manager.getDistanceMeters();
            if (distMeters >= 1000) {
                valDistance.setText(String.format(Locale.US, "%.2f km", distMeters / 1000));
            } else {
                valDistance.setText(String.format(Locale.US, "%.0f m", distMeters));
            }

            // Azimuth
            double azimuth = manager.getAzimuthAtoB();
            valAzimuth.setText(String.format(Locale.US, "%.1f°", azimuth));

            // Rotate Arrow (relative to Up being North)
            compassArrow.setRotation((float) azimuth);

            // Elevation
            double elev = manager.getElevationAtoB();
            valElevation.setText(String.format(Locale.US, "%.1f°", elev));
        } else {
            valDistance.setText("-");
            valAzimuth.setText("-");
            valElevation.setText("-");
            compassArrow.setRotation(0);
        }
    }

    private void startPickPoint(String type) {
        // Broadcast to Activity to open Map Selection
        Intent intent = new Intent(ACTION_PICK_ANTENNA_POINT);
        intent.putExtra(EXTRA_POINT_TYPE, type);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);

        Toast.makeText(context, "Haritada bir noktaya uzun basarak seçin.", Toast.LENGTH_LONG).show();
    }
}
