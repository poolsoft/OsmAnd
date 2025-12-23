package net.osmand.plus.carlauncher.antenna;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages Antenna Points (A and B) and calculations.
 * Singleton.
 */
public class AntennaManager {

    private static AntennaManager instance;
    private static final String PREFS_NAME = "antenna_prefs";
    private static final String KEY_POINT_A = "point_a";
    private static final String KEY_POINT_B = "point_b";

    private boolean layerVisible = false;
    private String pickingMode = null; // "A" or "B" or null

    private final Context context;
    private final SharedPreferences prefs;

    private AntennaPoint pointA;
    private AntennaPoint pointB;
    private AntennaListener listener;

    public interface AntennaListener {
        void onAntennaPointsChanged();
    }

    private AntennaManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadPoints();
    }

    public static synchronized AntennaManager getInstance(Context context) {
        if (instance == null) {
            instance = new AntennaManager(context);
        }
        return instance;
    }

    public void setListener(AntennaListener listener) {
        this.listener = listener;
    }

    public void setPointA(LatLon latLon, String name, double altitude) {
        this.pointA = new AntennaPoint(latLon.getLatitude(), latLon.getLongitude(), altitude, name);
        savePoints();
        notifyListener();
    }

    public void setPointB(LatLon latLon, String name, double altitude) {
        this.pointB = new AntennaPoint(latLon.getLatitude(), latLon.getLongitude(), altitude, name);
        savePoints();
        notifyListener();
    }

    public void clearPoints() {
        this.pointA = null;
        this.pointB = null;
        savePoints();
        notifyListener();
    }

    @Nullable
    public AntennaPoint getPointA() {
        return pointA;
    }

    @Nullable
    public AntennaPoint getPointB() {
        return pointB;
    }

    public boolean isLayerVisible() {
        return layerVisible;
    }

    public void setLayerVisible(boolean visible) {
        this.layerVisible = visible;
        notifyListener();
    }

    public String getPickingMode() {
        return pickingMode;
    }

    public void setPickingMode(String mode) {
        this.pickingMode = mode;
        notifyListener();
    }

    // Calculations

    public double getDistanceMeters() {
        if (pointA == null || pointB == null)
            return 0;
        return MapUtils.getDistance(pointA.lat, pointA.lon, pointB.lat, pointB.lon);
    }

    public double getAzimuthAtoB() {
        if (pointA == null || pointB == null)
            return 0;
        // MapUtils doesn't have simple bearing? We can use Location or manual calc.
        // Using basic math for now or android.location.Location if available.
        android.location.Location locA = new android.location.Location("");
        locA.setLatitude(pointA.lat);
        locA.setLongitude(pointA.lon);

        android.location.Location locB = new android.location.Location("");
        locB.setLatitude(pointB.lat);
        locB.setLongitude(pointB.lon);

        return locA.bearingTo(locB);
    }

    public double getElevationAtoB() {
        if (pointA == null || pointB == null)
            return 0;
        double dist = getDistanceMeters();
        double altDiff = pointB.altitude - pointA.altitude;
        // Simple elevation angle: tan(angle) = diff / dist
        // Note: Earth curvature might be relevant for very long distances, but for
        // typical WiFi (<50km) this is close enough for alignment start.

        // Correction for Earth curvature (visual line of sight) involves more complex
        // math,
        // but for antenna alignment, usually "Geometric Elevation" is what's needed
        // initially.
        return Math.toDegrees(Math.atan2(altDiff, dist));
    }

    // Persistence

    private void savePoints() {
        SharedPreferences.Editor editor = prefs.edit();
        if (pointA != null)
            editor.putString(KEY_POINT_A, pointA.toJson().toString());
        else
            editor.remove(KEY_POINT_A);

        if (pointB != null)
            editor.putString(KEY_POINT_B, pointB.toJson().toString());
        else
            editor.remove(KEY_POINT_B);

        editor.apply();
    }

    private void loadPoints() {
        String jsonA = prefs.getString(KEY_POINT_A, null);
        if (jsonA != null)
            pointA = AntennaPoint.fromJson(jsonA);

        String jsonB = prefs.getString(KEY_POINT_B, null);
        if (jsonB != null)
            pointB = AntennaPoint.fromJson(jsonB);
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onAntennaPointsChanged();
        }
    }

    public static class AntennaPoint {
        public double lat;
        public double lon;
        public double altitude;
        public String name;

        public AntennaPoint(double lat, double lon, double altitude, String name) {
            this.lat = lat;
            this.lon = lon;
            this.altitude = altitude;
            this.name = name;
        }

        public LatLon toLatLon() {
            return new LatLon(lat, lon);
        }

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("lat", lat);
                obj.put("lon", lon);
                obj.put("alt", altitude);
                obj.put("name", name);
                return obj;
            } catch (JSONException e) {
                return new JSONObject();
            }
        }

        public static AntennaPoint fromJson(String jsonStr) {
            try {
                JSONObject obj = new JSONObject(jsonStr);
                return new AntennaPoint(
                        obj.optDouble("lat"),
                        obj.optDouble("lon"),
                        obj.optDouble("alt", 0),
                        obj.optString("name", "Unknown"));
            } catch (JSONException e) {
                return null;
            }
        }
    }
}
