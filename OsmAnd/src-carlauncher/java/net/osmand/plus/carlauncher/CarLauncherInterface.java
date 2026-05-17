package net.osmand.plus.carlauncher;

import net.osmand.plus.carlauncher.ui.PanelContentManager;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Interface to expose CarLauncher specific methods from MapActivity.
 */
public interface CarLauncherInterface {
    void openAppDrawer();

    void closeAppDrawer();

    void openMusicPlayer();
    
    void openWeatherDashboard();

    /**
     * Sağ panel içeriğini değiştirir (Android Auto UI).
     * @param content PanelContent enum değeri
     */
    void setPanelContent(PanelContentManager.PanelContent content);

    OsmandMapTileView getMapView();
}
