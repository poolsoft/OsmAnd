package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.antenna.AntennaPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin; // Correct Import

public class WidgetFactory {

    public static final String TYPE_CLOCK = "clock";
// ...
            case TYPE_OBD:
                 // Assuming VehicleMetricsPlugin for OBD
                VehicleMetricsPlugin obdPlugin = PluginsHelper
                         .getPlugin(VehicleMetricsPlugin.class);
                 if (obdPlugin != null && obdPlugin.isActive()) {
                     return new OBDWidget(context, app);
                 }
                return null;
            default:
                return null;
        }
    }
}
