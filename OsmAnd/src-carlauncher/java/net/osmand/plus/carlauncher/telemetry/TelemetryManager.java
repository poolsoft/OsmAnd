package net.osmand.plus.carlauncher.telemetry;

import android.os.Handler;
import android.os.Looper;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.OsmAndLocationProvider;

/**
 * TelemetryManager: GPS ve OBD gibi kaynaklardan arac verilerini toplayan merkez.
 */
public class TelemetryManager implements OsmAndLocationProvider.OsmAndLocationListener {

    private static TelemetryManager instance;
    private final OsmandApplication app;

    // Dinleyiciler listesi
    private final List<TelemetryListener> listeners = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Mevcut Veriler
    private float currentSpeedKmh = 0f;
    private double currentAltitudeMeters = 0.0;
    private float currentBearing = 0f;

    // OBD Gelecek Planı
    private boolean isObdConnected = false;
    private int currentEngineRpm = 0;

    private TelemetryManager(OsmandApplication app) {
        this.app = app;
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

    public void addListener(TelemetryListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // İlk kayıt sırasında anlık veriyi gönder
            listener.onTelemetryUpdated(currentSpeedKmh, currentAltitudeMeters, currentBearing, currentEngineRpm);
        }
    }

    public void removeListener(TelemetryListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void updateLocation(Location location) {
        if (location == null) return;

        // Eger OBD bagli degilse GPS hizini kullan
        if (!isObdConnected) {
            if (location.hasSpeed()) {
                // GPS hızı m/s cinsindendir, km/h'e cevir
                currentSpeedKmh = location.getSpeed() * 3.6f;
            } else {
                // Hiz degeri yoksa 0 (veya son degeri koruyabilirsiniz, biz simdilik 0 yapalim)
                currentSpeedKmh = 0f;
            }
        }

        if (location.hasAltitude()) {
            currentAltitudeMeters = location.getAltitude();
        }

        if (location.hasBearing()) {
            currentBearing = location.getBearing();
        }

        notifyListeners();
    }

    /**
     * OBD cihazından (ELM327) veri geldiginde cagrilacak (ILERIYE DONUK).
     */
    public void updateObdData(float speedKmh, int rpm) {
        this.isObdConnected = true;
        this.currentSpeedKmh = speedKmh;
        this.currentEngineRpm = rpm;
        notifyListeners();
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (TelemetryListener listener : listeners) {
                listener.onTelemetryUpdated(currentSpeedKmh, currentAltitudeMeters, currentBearing, currentEngineRpm);
            }
        });
    }

    public interface TelemetryListener {
        void onTelemetryUpdated(float speedKmh, double altitudeMeters, float bearing, int engineRpm);
    }
}
