package net.osmand.plus.plugins.carlauncher;

import android.app.Activity;
import android.view.View;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.fragments.SettingsScreenType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CarLauncherPlugin extends OsmandPlugin implements OsmAndLocationProvider.OsmAndLocationListener {

    public static final String PLUGIN_ID = "osmand_car_launcher";
    private CarLauncherFragment carLauncherFragment;

    public CarLauncherPlugin(OsmandApplication app) {
        super(app);
    }

    @Override
    public boolean init(@NonNull OsmandApplication app, Activity activity) {
        return true;
    }

    @Override
    public void mapActivityResume(MapActivity activity) {
        super.mapActivityResume(activity);
        if (activity != null) {
            app.getLocationProvider().addLocationListener(this);
            addCarLauncherFragment(activity);
        }
    }

    @Override
    public void mapActivityPause(MapActivity activity) {
        super.mapActivityPause(activity);
        if (activity != null) {
            app.getLocationProvider().removeLocationListener(this);
            removeCarLauncherFragment(activity);
        }
    }

    private void addCarLauncherFragment(MapActivity activity) {
        carLauncherFragment = (CarLauncherFragment) activity.getSupportFragmentManager().findFragmentByTag(CarLauncherFragment.TAG);
        if (carLauncherFragment == null) {
            carLauncherFragment = new CarLauncherFragment();
            // Try to find map_hud_container, otherwise fallback to content.
            // Note: Since we are in a separate source set, we rely on R class merging.
            // However, R class references might need fully qualified name or just relying on merge.
            // For safety in this quick implementation, we look up by ID using identifiers if needed,
            // but standard R usage should work after gradle merge.
            int containerId = activity.getResources().getIdentifier("map_hud_container", "id", activity.getPackageName());
            if (containerId == 0) {
                containerId = android.R.id.content;
            }
            
            activity.getSupportFragmentManager().beginTransaction()
                    .add(containerId, carLauncherFragment, CarLauncherFragment.TAG)
                    .commitAllowingStateLoss();
        }
    }

    private void removeCarLauncherFragment(MapActivity activity) {
        if (carLauncherFragment != null) {
            activity.getSupportFragmentManager().beginTransaction()
                    .remove(carLauncherFragment)
                    .commitAllowingStateLoss();
            carLauncherFragment = null;
        }
    }

    @Override
    public void updateLocation(Location location) {
        if (carLauncherFragment != null && carLauncherFragment.isAdded()) {
            carLauncherFragment.updateLocation(location);
        }
    }

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public CharSequence getDescription(boolean linksEnabled) {
        return "Car Launcher Interface and Widgets";
    }

    @Override
    public String getName() {
        return "Car Launcher";
    }

    @Nullable
    @Override
    public SettingsScreenType getSettingsScreenType() {
        return null;
    }

    @Override
    public void disable(@NonNull OsmandApplication app) {
        super.disable(app);
    }

    @Override
    public int getLogoResourceId() {
        return net.osmand.plus.R.drawable.ic_extension_dark;
    }
}
