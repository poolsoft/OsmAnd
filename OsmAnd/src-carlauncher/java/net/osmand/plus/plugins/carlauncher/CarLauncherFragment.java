package net.osmand.plus.plugins.carlauncher;

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
import net.osmand.plus.utils.OsmAndFormatter;
// import net.osmand.plus.R; // R comes from main package or unified package

public class CarLauncherFragment extends Fragment {

    public static final String TAG = "CarLauncherFragment";

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
        // R.layout.fragment_car_launcher will be available after resource merging
        // We might need to use getIdentifier if R cannot be resolved directly in IDE before build,
        // but for compilation it should be fine if package name is correct.
        // As we are in 'net.osmand.plus.plugins.carlauncher', we might need to import net.osmand.plus.R
        
        return inflater.inflate(net.osmand.plus.R.layout.fragment_car_launcher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Using net.osmand.plus.R for IDs
        textSpeed = view.findViewById(net.osmand.plus.R.id.car_launcher_speed);
        textDirection = view.findViewById(net.osmand.plus.R.id.car_launcher_direction);
        textAltitude = view.findViewById(net.osmand.plus.R.id.car_launcher_altitude);
    }

    public void updateLocation(Location location) {
        if (location == null || app == null || !isAdded()) return;

        // Use custom strings from strings.xml
        String speedLabel = getString(net.osmand.plus.R.string.car_launcher_speed_label);
        String directionLabel = getString(net.osmand.plus.R.string.car_launcher_direction_label);
        String altitudeLabel = getString(net.osmand.plus.R.string.car_launcher_altitude_label);

        if (location.hasSpeed()) {
            textSpeed.setText(speedLabel + ": " + OsmAndFormatter.getFormattedSpeed(location.getSpeed(), app));
        } else {
            textSpeed.setText(speedLabel + ": --");
        }

        if (location.hasBearing()) {
             textDirection.setText(directionLabel + ": " + (int)location.getBearing() + "°");
        } else {
            textDirection.setText(directionLabel + ": --");
        }

        if (location.hasAltitude()) {
            textAltitude.setText(altitudeLabel + ": " + OsmAndFormatter.getFormattedDistance((float) location.getAltitude(), app));
        } else {
            textAltitude.setText(altitudeLabel + ": --");
        }
    }
}
