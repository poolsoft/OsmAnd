package net.osmand.plus.plugins.internal;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.View;
import android.widget.FrameLayout;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.fragments.SettingsScreenType;

public class OsmandInternalPlugin extends OsmandPlugin implements OsmAndLocationProvider.OsmAndLocationListener {

    public static final String PLUGIN_ID = "osmand_internal";
    private RightPanelFragment rightPanelFragment;

    public OsmandInternalPlugin(OsmandApplication app) {
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
            addRightPanelFragment(activity);
        }
    }

    @Override
    public void mapActivityPause(MapActivity activity) {
        super.mapActivityPause(activity);
        if (activity != null) {
            app.getLocationProvider().removeLocationListener(this);
            removeRightPanelFragment(activity);
        }
    }

    private void addRightPanelFragment(MapActivity activity) {
        rightPanelFragment = (RightPanelFragment) activity.getSupportFragmentManager().findFragmentByTag(RightPanelFragment.TAG);
        if (rightPanelFragment == null) {
            rightPanelFragment = new RightPanelFragment();
            // Using android.R.id.content to overlay on top of everything, or map_hud_container if valid.
            // map_hud_container is R.id.map_hud_container.
            View container = activity.findViewById(R.id.map_hud_container);
            int containerId = container != null ? R.id.map_hud_container : android.R.id.content;
            
            activity.getSupportFragmentManager().beginTransaction()
                    .add(containerId, rightPanelFragment, RightPanelFragment.TAG)
                    .commitAllowingStateLoss();
        }
    }

    private void removeRightPanelFragment(MapActivity activity) {
        if (rightPanelFragment != null) {
            activity.getSupportFragmentManager().beginTransaction()
                    .remove(rightPanelFragment)
                    .commitAllowingStateLoss();
            rightPanelFragment = null;
        }
    }

    @Override
    public void updateLocation(Location location) {
        if (rightPanelFragment != null && rightPanelFragment.isAdded()) {
            rightPanelFragment.updateLocation(location);
        }
    }

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public CharSequence getDescription(boolean linksEnabled) {
        return "Internal Development Plugin";
    }

    @Override
    public String getName() {
        return "Internal Plugin";
    }

    @Nullable
    @Override
    public SettingsScreenType getSettingsScreenType() {
        return null; // No settings screen for now
    }

    @Override
    public void disable(@NonNull OsmandApplication app) {
        super.disable(app);
    }

    @Override
    public int getLogoResourceId() {
        return R.drawable.ic_extension_dark; // Default icon
    }
}
