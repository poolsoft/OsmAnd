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
import net.osmand.plus.carlauncher.antenna.AntennaMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.OsmandMap;

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
    private View contentLayout;
    private ImageView iconExpand;
    private boolean isExpanded = false;

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
        View headerLayout = view.findViewById(net.osmand.plus.R.id.header_layout);
        contentLayout = view.findViewById(net.osmand.plus.R.id.content_layout);
        iconExpand = view.findViewById(net.osmand.plus.R.id.icon_expand);

        textPointA = view.findViewById(net.osmand.plus.R.id.text_point_a);
        textPointB = view.findViewById(net.osmand.plus.R.id.text_point_b);
        valDistance = view.findViewById(net.osmand.plus.R.id.val_distance);
        valAzimuth = view.findViewById(net.osmand.plus.R.id.val_azimuth);
        valElevation = view.findViewById(net.osmand.plus.R.id.val_elevation);
        compassArrow = view.findViewById(net.osmand.plus.R.id.compass_arrow);

        View btnSetA = view.findViewById(net.osmand.plus.R.id.btn_set_a);
        View btnSetB = view.findViewById(net.osmand.plus.R.id.btn_set_b);

        // Listeners
        headerLayout.setOnClickListener(v -> toggleExpand());

        btnSetA.setOnClickListener(v -> startPickPoint("A"));
        btnSetB.setOnClickListener(v -> startPickPoint("B"));

        View btnAlign = view.findViewById(net.osmand.plus.R.id.btn_align);
        btnAlign.setOnClickListener(v -> {
            if (manager.getPointA() != null && manager.getPointB() != null) {
                Intent intent = new Intent(context, net.osmand.plus.carlauncher.antenna.AntennaAlignmentActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Lütfen önce A ve B noktalarını seçin.", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Layout Params
        // view.setLayoutParams(new ViewGroup.LayoutParams(dpToPx(300),
        // ViewGroup.LayoutParams.WRAP_CONTENT));
        // Use match_parent width handled by container usually, but keep specific if
        // needed.
        // XML is match_parent width. FrameLayout container might constraint it.
        // Keeping dpToPx(300) causes it to be fixed width.
        // If user wants modern, maybe full width?
        // NavigationWidget uses match parent.
        view.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        rootView = view;
        updateExpandState();
        return rootView;
    }

    private void toggleExpand() {
        isExpanded = !isExpanded;
        updateExpandState();
    }

    @Override
    public void onStart() {
        super.onStart();
        manager.setListener(this);
        initMapLayer();
        updateUI();
        // Add layer only if expanded
        if (isExpanded) {
            addMapLayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        manager.setListener(null);
        removeMapLayer();
    }

    private void initMapLayer() {
        OsmandMapTileView mapView = getMapView();
        if (mapView != null && mapLayer == null) {
            mapLayer = new AntennaMapLayer(mapView);
        }
    }

    private void addMapLayer() {
        OsmandMapTileView mapView = getMapView();
        if (mapLayer != null && mapView != null) {
            if (!mapView.getLayers().contains(mapLayer)) {
                mapView.addLayer(mapLayer, 5.5f);
            }
        }
    }

    private void removeMapLayer() {
        OsmandMapTileView mapView = getMapView();
        if (mapLayer != null && mapView != null) {
            mapView.removeLayer(mapLayer);
        }
    }

    private OsmandMapTileView getMapView() {
        if (context.getApplicationContext() instanceof OsmandApplication) {
            OsmandApplication app = (OsmandApplication) context.getApplicationContext();
            OsmandMap osmandMap = app.getOsmandMap();
            if (osmandMap != null) {
                return osmandMap.getMapView();
            }
        }
        return null;
    }

    private void updateExpandState() {
        if (contentLayout != null) {
            contentLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            iconExpand.animate().rotation(isExpanded ? 180 : 0).setDuration(200).start();
        }
        if (isExpanded) {
            addMapLayer();
        } else {
            removeMapLayer();
        }
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
        if (mapLayer != null) {
            mapLayer.setPickingMode(type);
        } else {
            Toast.makeText(context, "Harita katmanı henüz hazır değil.", Toast.LENGTH_SHORT).show();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
