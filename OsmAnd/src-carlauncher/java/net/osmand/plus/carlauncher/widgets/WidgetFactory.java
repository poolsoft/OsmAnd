package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.antenna.AntennaPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.OBDPlugin; // Assuming OBDPlugin class name, checking later if needed or usage via PluginsHelper generic

public class WidgetFactory {

    public static final String TYPE_CLOCK = "clock";
    public static final String TYPE_SPEED = "speed";
    public static final String TYPE_COMPASS = "compass"; // DirectionWidget
    public static final String TYPE_NAVIGATION = "navigation";
    public static final String TYPE_MUSIC = "music";
    public static final String TYPE_ANTENNA = "antenna";
    public static final String TYPE_OBD = "obd";

    /**
     * Creates a new widget instance based on the type.
     */
    @Nullable
    public static BaseWidget createWidget(@NonNull Context context, @NonNull OsmandApplication app, @NonNull String type) {
        switch (type) {
            case TYPE_CLOCK:
                return new Material3ClockWidget(context);
            case TYPE_SPEED:
                return new SpeedWidget(context, app);
            case TYPE_COMPASS:
                return new DirectionWidget(context, app);
            case TYPE_NAVIGATION:
                return new NavigationWidget(context, app);
            case TYPE_MUSIC:
                return new MusicWidget(context, app);
            case TYPE_ANTENNA:
                AntennaPlugin antennaPlugin = PluginsHelper
                        .getPlugin(AntennaPlugin.class);
                if (antennaPlugin != null && antennaPlugin.isActive()) {
                    return new AntennaWidget(context, app);
                }
                return null;
            case TYPE_OBD:
                 // Assuming VehicleMetricsPlugin for OBD
                net.osmand.plus.carlauncher.plugins.VehicleMetricsPlugin obdPlugin = PluginsHelper
                         .getPlugin(net.osmand.plus.carlauncher.plugins.VehicleMetricsPlugin.class);
                 if (obdPlugin != null && obdPlugin.isActive()) {
                     return new OBDWidget(context, app);
                 }
                return null;
            default:
                return null;
        }
    }
}
