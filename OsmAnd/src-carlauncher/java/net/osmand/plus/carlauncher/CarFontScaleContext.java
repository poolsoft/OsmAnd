package net.osmand.plus.carlauncher;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

/** Applies an optional font scale only to the Car Launcher UI contexts. */
public final class CarFontScaleContext {

    private CarFontScaleContext() {
    }

    @NonNull
    public static Context wrap(@NonNull Context base) {
        String value = base.getSharedPreferences("car_launcher_prefs", Context.MODE_PRIVATE)
                .getString(CarLauncherSettings.KEY_APP_FONT_SCALE, "system");
        if (value == null || "system".equals(value)) return base;

        try {
            float fontScale = Float.parseFloat(value);
            if (fontScale < 0.75f || fontScale > 1.30f) return base;
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            if (Math.abs(configuration.fontScale - fontScale) < 0.001f) return base;
            configuration.fontScale = fontScale;
            return base.createConfigurationContext(configuration);
        } catch (NumberFormatException e) {
            return base;
        }
    }
}
