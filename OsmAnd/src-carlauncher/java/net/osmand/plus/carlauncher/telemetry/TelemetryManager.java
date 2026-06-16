package net.osmand.plus.carlauncher.telemetry;

import android.os.Handler;
import android.os.Looper;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.TurnType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin;
import net.osmand.shared.obd.OBDDataComputer;

import java.util.ArrayList;
import java.util.List;

public class TelemetryManager implements OsmAndLocationProvider.OsmAndLocationListener {

    private static TelemetryManager instance;
    private final OsmandApplication app;

    private final List<TelemetryListener> listeners = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;

    // --- State Modelleri ---
    public static class LocationState {
        public float speedKmh = 0f;
        public double altitudeMeters = 0.0;
        public float bearing = 0f;
    }

    public static class NavigationState {
        public boolean isActive = false;
        public int turnIconRes = 0;
        public String distanceStr = "";
        public String instructionStr = "";
        public String etaStr = "";
    }

    public static class ObdState {
        public boolean isActive = false;
        public String rpm = "--";
        public String temp = "--";
        public String volt = "--";
        public String load = "--";
    }

    private final LocationState locationState = new LocationState();
    private final NavigationState navigationState = new NavigationState();
    private final ObdState obdState = new ObdState();

    // OBD Computers
    private OBDDataComputer.OBDComputerWidget compRpm;
    private OBDDataComputer.OBDComputerWidget compTemp;
    private OBDDataComputer.OBDComputerWidget compVolt;
    private OBDDataComputer.OBDComputerWidget compLoad;

    private TelemetryManager(OsmandApplication app) {
        this.app = app;
        initObdComputers();
    }

    public static TelemetryManager getInstance(OsmandApplication app) {
        if (instance == null) {
            instance = new TelemetryManager(app);
            if (app.getLocationProvider() != null) {
                app.getLocationProvider().addLocationListener(instance);
            }
        }
        return instance;
    }

    private void initObdComputers() {
        VehicleMetricsPlugin plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin.class);
        if (plugin != null && plugin.isActive()) {
            compRpm = OBDDataComputer.INSTANCE.registerWidget(OBDDataComputer.OBDTypeWidget.RPM, 0);
            compTemp = OBDDataComputer.INSTANCE.registerWidget(OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT, 0);
            compVolt = OBDDataComputer.INSTANCE.registerWidget(OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE, 0);
            compLoad = OBDDataComputer.INSTANCE.registerWidget(OBDDataComputer.OBDTypeWidget.CALCULATED_ENGINE_LOAD, 0);
        }
    }

    public void addListener(TelemetryListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            listener.onTelemetryUpdated(locationState, navigationState, obdState);
        }
        if (listeners.size() == 1) {
            startPolling(); // Ilk dinleyici eklendiginde basla
        }
    }

    public void removeListener(TelemetryListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            stopPolling(); // Dinleyici kalmadiginda dur
        }
    }

    @Override
    public void updateLocation(Location location) {
        if (location == null) return;

        if (location.hasSpeed()) locationState.speedKmh = location.getSpeed() * 3.6f;
        if (location.hasAltitude()) locationState.altitudeMeters = location.getAltitude();
        if (location.hasBearing()) locationState.bearing = location.getBearing();

        notifyListeners();
    }

    private void startPolling() {
        if (pollingRunnable == null) {
            pollingRunnable = new Runnable() {
                @Override
                public void run() {
                    pollNavigation();
                    pollObd();
                    notifyListeners();
                    mainHandler.postDelayed(this, 1000); // 1 saniyede bir
                }
            };
            mainHandler.post(pollingRunnable);
        }
    }

    private void stopPolling() {
        if (pollingRunnable != null) {
            mainHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    private void pollNavigation() {
        RoutingHelper routingHelper = app.getRoutingHelper();
        if (routingHelper != null && routingHelper.isFollowingMode() && routingHelper.isRouteCalculated()) {
            navigationState.isActive = true;
            try {
                NextDirectionInfo nextDirection = routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
                if (nextDirection != null && nextDirection.distanceTo > 0) {
                    navigationState.distanceStr = OsmAndFormatter.getFormattedDistance(nextDirection.distanceTo, app);
                    
                    if (nextDirection.directionInfo != null) {
                        TurnType turnType = nextDirection.directionInfo.getTurnType();
                        navigationState.turnIconRes = getTurnIcon(turnType);
                        navigationState.instructionStr = getTurnInstruction(turnType, nextDirection.directionInfo.getStreetName());
                    }
                } else {
                    navigationState.distanceStr = "--";
                    navigationState.turnIconRes = android.R.drawable.arrow_up_float;
                    navigationState.instructionStr = "Duz git";
                }

                int remainingDistance = routingHelper.getLeftDistance();
                int remainingTime = routingHelper.getLeftTime();
                if (remainingDistance > 0 && remainingTime > 0) {
                    navigationState.etaStr = OsmAndFormatter.getFormattedDuration(remainingTime, app) + " (" + OsmAndFormatter.getFormattedDistance(remainingDistance, app) + ")";
                } else {
                    navigationState.etaStr = "";
                }
            } catch (Exception e) {
                navigationState.isActive = false;
            }
        } else {
            navigationState.isActive = false;
        }
    }

    private void pollObd() {
        VehicleMetricsPlugin plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin.class);
        if (plugin != null && plugin.isActive() && plugin.isConnected()) {
            obdState.isActive = true;
            if (compRpm != null) obdState.rpm = plugin.getWidgetValue(compRpm);
            if (compTemp != null) obdState.temp = plugin.getWidgetValue(compTemp) + "°C";
            if (compVolt != null) obdState.volt = plugin.getWidgetValue(compVolt) + "V";
            if (compLoad != null) obdState.load = plugin.getWidgetValue(compLoad) + "%";
        } else {
            obdState.isActive = false;
            obdState.rpm = "--";
            obdState.temp = "--";
            obdState.volt = "--";
            obdState.load = "--";
        }
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (TelemetryListener listener : listeners) {
                listener.onTelemetryUpdated(locationState, navigationState, obdState);
            }
        });
    }

    private int getTurnIcon(TurnType turnType) {
        if (turnType == null) return 0;
        if (turnType.isRoundAbout()) return android.R.drawable.ic_menu_rotate;
        switch (turnType.getValue()) {
            case TurnType.C: return android.R.drawable.arrow_up_float;
            case TurnType.TL:
            case TurnType.TSLL: return android.R.drawable.ic_menu_revert;
            case TurnType.TR:
            case TurnType.TSLR: return android.R.drawable.ic_menu_always_landscape_portrait;
            case TurnType.TU: return android.R.drawable.ic_menu_rotate;
            case TurnType.KL: return android.R.drawable.ic_menu_revert;
            case TurnType.KR: return android.R.drawable.ic_menu_always_landscape_portrait;
            default: return android.R.drawable.arrow_up_float;
        }
    }

    private String getTurnInstruction(TurnType turnType, String streetName) {
        if (turnType == null) return "Devam et";
        String inst = "Devam et";
        if (turnType.isRoundAbout()) {
            inst = "Doneleden " + turnType.getExitOut() + ". cikis";
        } else {
            switch (turnType.getValue()) {
                case TurnType.C: inst = "Duz git"; break;
                case TurnType.TL: inst = "Sola don"; break;
                case TurnType.TSLL: inst = "Keskin sola don"; break;
                case TurnType.TR: inst = "Saga don"; break;
                case TurnType.TSLR: inst = "Keskin saga don"; break;
                case TurnType.TU: inst = "U donus yap"; break;
                case TurnType.KL: inst = "Sola devam et"; break;
                case TurnType.KR: inst = "Saga devam et"; break;
            }
        }
        if (streetName != null && !streetName.isEmpty()) {
            inst += "\n" + streetName;
        }
        return inst;
    }

    public interface TelemetryListener {
        void onTelemetryUpdated(LocationState loc, NavigationState nav, ObdState obd);
    }
}
