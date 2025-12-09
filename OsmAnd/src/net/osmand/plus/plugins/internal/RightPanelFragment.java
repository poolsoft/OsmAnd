package net.osmand.plus.plugins.internal;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;

public class RightPanelFragment extends Fragment {

    public static final String TAG = "RightPanelFragment";

    private TextView textSpeed;
    private TextView textDirection;
    private TextView textAltitude;
    private OsmandApplication app;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_right_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textSpeed = view.findViewById(R.id.text_speed);
        textDirection = view.findViewById(R.id.text_direction);
        textAltitude = view.findViewById(R.id.text_altitude);
    }

    public void updateLocation(Location location) {
        if (location == null || app == null || !isAdded()) return;

        if (location.hasSpeed()) {
            textSpeed.setText(getString(R.string.map_widget_speed) + ": " + OsmAndFormatter.getFormattedSpeed(location.getSpeed(), app));
        } else {
            textSpeed.setText(getString(R.string.map_widget_speed) + ": --");
        }

        if (location.hasBearing()) {
             textDirection.setText(getString(R.string.map_widget_magnetic_bearing) + ": " + (int)location.getBearing() + "°");
        } else {
            textDirection.setText(getString(R.string.map_widget_magnetic_bearing) + ": --");
        }

        if (location.hasAltitude()) {
            textAltitude.setText(getString(R.string.map_widget_altitude) + ": " + OsmAndFormatter.getFormattedDistance((float) location.getAltitude(), app));
        } else {
            textAltitude.setText(getString(R.string.map_widget_altitude) + ": --");
        }
    }
}
