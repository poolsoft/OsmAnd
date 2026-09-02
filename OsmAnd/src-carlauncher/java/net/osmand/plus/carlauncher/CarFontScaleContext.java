package net.osmand.plus.carlauncher;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.NonNull;

/** Applies an optional font scale only to the Car Launcher UI contexts. */
public final class CarFontScaleContext {

    private CarFontScaleContext() {
    }

    @NonNull
    public static Context wrap(@NonNull Context base) {
        Float fontScale = getConfiguredFontScale(base);
        if (fontScale == null) {
            return base;
        }
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        if (Math.abs(configuration.fontScale - fontScale) < 0.001f) return base;
        configuration.fontScale = fontScale;
        return base.createConfigurationContext(configuration);
    }

    @NonNull
    public static Configuration applyTo(@NonNull Context context, @NonNull Configuration source) {
        Configuration configuration = new Configuration(source);
        Float fontScale = getConfiguredFontScale(context);
        if (fontScale != null) {
            configuration.fontScale = fontScale;
        }
        return configuration;
    }

    public static void applyToResources(@NonNull Context context) {
        Float fontScale = getConfiguredFontScale(context);
        if (fontScale == null) return;
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        if (Math.abs(configuration.fontScale - fontScale) < 0.001f) return;
        configuration.fontScale = fontScale;
        // Required on older head-unit ROMs where an Activity override context is
        // replaced by the vendor/AppCompat configuration during startup.
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    private static Float getConfiguredFontScale(@NonNull Context context) {
        String value = context.getSharedPreferences("car_launcher_prefs", Context.MODE_PRIVATE)
                .getString(CarLauncherSettings.KEY_APP_FONT_SCALE, "system");
        if (value == null || "system".equals(value)) return null;
        try {
            float fontScale = Float.parseFloat(value);
            return fontScale >= 0.75f && fontScale <= 1.30f ? fontScale : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
