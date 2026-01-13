package net.osmand.plus.carlauncher.widgets.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.CarLauncherSettings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hava Durumu Yoneticisi.
 * Open-Meteo API ile iletisim kurar ve verileri onbellege alir.
 * Singleton yapisindadir.
 */
public class WeatherManager {

    private static final String TAG = "WeatherManager";
    private static WeatherManager instance;
    private final Context context;
    private final SharedPreferences prefs;

    // Cache Keys
    private static final String PREF_NAME = "weather_cache";
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private static final String KEY_CACHED_JSON = "cached_json";
    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LON = "last_lon";

    // Constants
    private static final long STALE_THRESHOLD_MS = 3 * 60 * 60 * 1000; // 3 Saat (Eski veri) - FIXED typo
    private static final long REFRESH_INTERVAL_MS = 30 * 60 * 1000; // 30 Dakika
    private static final float LOCATION_CHANGE_THRESHOLD = 20000; // 20 km

    // Data Listeners
    private final List<WeatherListener> listeners = new ArrayList<>();

    public interface WeatherListener {
        void onWeatherUpdated(WeatherData data);
        void onWeatherError(String error);
    }

    private static final String KEY_UPDATE_INTERVAL = "update_interval_minutes";

    private WeatherManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getUpdateIntervalMinutes() {
        return prefs.getInt(KEY_UPDATE_INTERVAL, 30);
    }

    public void setUpdateIntervalMinutes(int minutes) {
        prefs.edit().putInt(KEY_UPDATE_INTERVAL, minutes).apply();
    }

    public void forceRefresh() {
        Location loc = null;
        if (context instanceof OsmandApplication) {
             loc = ((OsmandApplication) context).getSettings().getLastKnownLocation();
        } 
        // Need access to last location if possible, or just ignore if no loc.
        // Assuming we have cached lat/lon
        double lat = getDouble(prefs, KEY_LAST_LAT, 0);
        double lon = getDouble(prefs, KEY_LAST_LON, 0);
        if (lat != 0 && lon != 0) {
            fetchWeather(lat, lon);
        }
    }

    public static synchronized WeatherManager getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherManager(context);
        }
        return instance;
    }

    public void addListener(WeatherListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // Hemen var olan veriyi gonder
            WeatherData cached = getCachedWeather();
            if (cached != null) {
                listener.onWeatherUpdated(cached);
            }
        }
    }

    public void removeListener(WeatherListener listener) {
        listeners.remove(listener);
    }

    /**
     * Konum guncellendiginde cagrilir.
     * Veri eski ise veya konum cok degistiyse yenileme yapar.
     */
    public void updateLocation(Location location) {
        if (location == null) return;

        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        double lastLat = getDouble(prefs, KEY_LAST_LAT, 0);
        double lastLon = getDouble(prefs, KEY_LAST_LON, 0);

        float[] results = new float[1];
        android.location.Location.distanceBetween(lastLat, lastLon, location.getLatitude(), location.getLongitude(), results);
        float distance = results[0];

        boolean timeExpired = (System.currentTimeMillis() - lastUpdate) > REFRESH_INTERVAL_MS;
        boolean locationChanged = distance > LOCATION_CHANGE_THRESHOLD;

        CarLauncherSettings settings = new CarLauncherSettings(context);
        if (settings.isWeatherEnabled() && (timeExpired || locationChanged)) {
            fetchWeather(location.getLatitude(), location.getLongitude());
        }
    }

    /**
     * API'den hava durumu verisi ceker.
     */
    public void fetchWeather(double lat, double lon) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Internet yok, veri cekilemedi.");
            return;
        }

        String url = String.format(Locale.US, 
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto", 
            lat, lon);

        new FetchTask().execute(url, String.valueOf(lat), String.valueOf(lon));
    }

    public WeatherData getCachedWeather() {
        String jsonStr = prefs.getString(KEY_CACHED_JSON, null);
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);

        if (jsonStr == null) return null;

        return parseWeather(jsonStr, lastUpdate);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    // --- Helper Methods ---

    private void saveCache(String json, double lat, double lon) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_CACHED_JSON, json);
        editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
        putDouble(editor, KEY_LAST_LAT, lat);
        putDouble(editor, KEY_LAST_LON, lon);
        editor.apply();
    }

    private WeatherData parseWeather(String jsonStr, long timestamp) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            WeatherData data = new WeatherData();
            
            // Current Units
            JSONObject current = json.getJSONObject("current");
            data.temp = current.getDouble("temperature_2m");
            data.weatherCode = current.getInt("weather_code");
            
            // Daily High/Low (Index 0 is today)
            JSONObject daily = json.getJSONObject("daily");
            if (daily != null) {
                data.maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0);
                data.minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0);
            }
            
            data.timestamp = timestamp;
            data.isStale = (System.currentTimeMillis() - timestamp) > STALE_THRESHOLD_MS;
            
            return data;
        } catch (Exception e) {
            Log.e(TAG, "JSON Parse Error", e);
            return null;
        }
    }

    private void notifyListeners(WeatherData data) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (WeatherListener l : listeners) {
                l.onWeatherUpdated(data);
            }
        });
    }

    // SharedPreferences doesn't support double directly
    private void putDouble(SharedPreferences.Editor editor, String key, double value) {
        editor.putLong(key, Double.doubleToRawLongBits(value));
    }

    private double getDouble(SharedPreferences prefs, String key, double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToRawLongBits(defaultValue)));
    }

    // --- AsyncTask ---

    private class FetchTask extends AsyncTask<String, Void, String> {
        double lat, lon;

        @Override
        protected String doInBackground(String... params) {
            String urlStr = params[0];
            lat = Double.parseDouble(params[1]);
            lon = Double.parseDouble(params[2]);

            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch Error", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String json) {
            if (json != null) {
                saveCache(json, lat, lon);
                WeatherData data = parseWeather(json, System.currentTimeMillis());
                notifyListeners(data);
            }
        }
    }

    // --- Data Model ---

    public static class WeatherData {
        public double temp;
        public double maxTemp;
        public double minTemp;
        public int weatherCode;
        public long timestamp;
        public boolean isStale;

        public String getIconName() {
            // WMO Code Mapping
            // 0: Clear sky
            // 1, 2, 3: Mainly clear, partly cloudy, and overcast
            // 45, 48: Fog
            // 51-55: Drizzle
            // 61-65: Rain
            // 71-77: Snow
            // 80-82: Rain showers
            // 95-99: Thunderstorm
            
            if (weatherCode == 0) return "ic_weather_clear";
            if (weatherCode >= 1 && weatherCode <= 3) return "ic_weather_cloudy";
            if (weatherCode >= 45 && weatherCode <= 48) return "ic_weather_cloudy"; // Fog -> Cloudy
            if (weatherCode >= 51 && weatherCode <= 67) return "ic_weather_rain";
            if (weatherCode >= 71 && weatherCode <= 77) return "ic_weather_rain"; // Snow -> Rain (Temp)
            if (weatherCode >= 80 && weatherCode <= 82) return "ic_weather_rain";
            if (weatherCode >= 95) return "ic_weather_rain"; // Storm -> Rain (Temp)
            
            return "ic_weather_cloudy"; // Default fallback
        }
    }
}
