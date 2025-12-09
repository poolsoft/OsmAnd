package net.osmand.plus.plugins.rightpanel;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmAndApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;

public class RightPanelPlugin extends OsmandPlugin {

    public RightPanelPlugin(@NonNull OsmAndApplication app) {
        super(app);
    }

    @Override
    public String getId() {
        return "right_panel";
    }

    @Override
    public String getName() {
        return "Right Panel";
    }

    @Override
    public CharSequence getDescription(boolean linksEnabled) {
        return "Right panel with various widgets";
    }

    @Override
    public int getLogoResourceId() {
        return R.drawable.ic_action_settings;
    }

    // Either remove this method if not needed, 
    // or update the signature to match the interface
    @Override 
    public boolean setNavDrawerLogo(String imageUri) throws android.os.RemoteException {
        // Implementation
    }
}
