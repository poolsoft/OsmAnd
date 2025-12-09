package net.osmand.plus.plugins.internal;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.fragments.SettingsScreenType;

public class OsmandInternalPlugin extends OsmandPlugin {

    public static final String PLUGIN_ID = "osmand_internal";

    public OsmandInternalPlugin(OsmandApplication app) {
        super(app);
    }

    @Override
    public boolean init(@NonNull OsmandApplication app, Activity activity) {
        return true;
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
