package net.osmand.plus.carlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;

/** One-time low-RAM defaults; users can re-enable any stock plugin afterwards. */
public final class CarLauncherPluginPolicy {
    private static final String PREFS = "car_launcher_prefs";
    private static final String POLICY_APPLIED = "car_launcher_plugin_policy_v1";
    private static final String[] LOW_RAM_DEFAULTS_OFF = {
            "osmand.aistracker",
            "osmand.weather",
            "osmand.srtm.paid"
    };

    private CarLauncherPluginPolicy() { }

    public static void apply(OsmandApplication app, OsmandSettings settings, boolean lowRamProfile) {
        if (!lowRamProfile || settings == null) return;
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(POLICY_APPLIED, false)) return;
        for (String pluginId : LOW_RAM_DEFAULTS_OFF) {
            settings.enablePlugin(pluginId, false);
        }
        prefs.edit().putBoolean(POLICY_APPLIED, true).apply();
        net.osmand.plus.carlauncher.ui.StartupPerformanceRecorder.getInstance().record(app,
                "LOW_RAM_PLUGIN_POLICY disabled=ais,weather,srtm preserved=obd,audio_notes,parking,external_sensors");
    }
}
