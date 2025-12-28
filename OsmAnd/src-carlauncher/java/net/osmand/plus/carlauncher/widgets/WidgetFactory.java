package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import net.osmand.plus.OsmandApplication;

public class WidgetFactory {
    public static final String TYPE_SPEED = "speed";
    public static final String TYPE_MUSIC = "music";
    public static final String TYPE_NAVIGATION = "navigation";
    public static final String TYPE_COMPASS = "compass";
    public static final String TYPE_OBD = "obd";
    public static final String TYPE_CLOCK = "clock";
    public static final String TYPE_ANTENNA = "antenna";

    public static BaseWidget createWidget(Context context, OsmandApplication app, String type) {
        switch (type) {
            case TYPE_SPEED:
                return new SpeedWidget(context, app);
            case TYPE_MUSIC:
                return new MusicWidget(context, app);
            case TYPE_NAVIGATION:
                return new NavigationWidget(context, app);
            case TYPE_COMPASS:
                return new DirectionWidget(context, app);
            case TYPE_OBD:
                return new OBDWidget(context, app);
            case TYPE_CLOCK:
                return new Material3ClockWidget(context);
            case TYPE_ANTENNA:
                return new AntennaWidget(context, app);
            default:
                return null;
        }
    }
}
