package net.osmand.plus;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.carlauncher.CarLauncherPlugin;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Car Launcher icin ozel OsmandApplication.
 * CarLauncherPlugin'i dinamik olarak kaydeder.
 */
public class CarLauncherOsmandApplication extends OsmandApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        // CarLauncherPlugin'i kaydet
        registerCarLauncherPlugin();
    }

    /**
     * CarLauncherPlugin'i PluginsHelper'a reflection ile ekle.
     */
    private void registerCarLauncherPlugin() {
        try {
            // PluginsHelper'daki allPlugins listesine eriş
            Field allPluginsField = PluginsHelper.class.getDeclaredField("allPlugins");
            allPluginsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<OsmandPlugin> allPlugins = (List<OsmandPlugin>) allPluginsField.get(null);

            if (allPlugins != null) {
                // CarLauncherPlugin zaten var mi kontrol et
                boolean alreadyExists = false;
                for (OsmandPlugin plugin : allPlugins) {
                    if (plugin instanceof CarLauncherPlugin) {
                        alreadyExists = true;
                        break;
                    }
                }

                // Yoksa ekle
                if (!alreadyExists) {
                    CarLauncherPlugin carLauncherPlugin = new CarLauncherPlugin(this);
                    allPlugins.add(carLauncherPlugin);

                    // Plugin'i aktif et
                    getSettings().enablePlugin(CarLauncherPlugin.PLUGIN_ID, true);
                    carLauncherPlugin.init(this, null);
                    carLauncherPlugin.setEnabled(true);
                }
            }
        } catch (Exception e) {
            // Hata durumunda log
            android.util.Log.e("CarLauncherApp", "CarLauncherPlugin yuklenirken hata: " + e.getMessage());
        }
    }
}
