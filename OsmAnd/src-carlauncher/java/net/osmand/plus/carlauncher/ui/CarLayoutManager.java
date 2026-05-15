package net.osmand.plus.carlauncher.ui;

import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.carlauncher.CarLauncherSettings;

/**
 * Manages the layout constraints and UI state for the CarLauncher interface.
 * Decouples layout logic from MapActivity.
 */
public class CarLayoutManager {

    private final MapActivity activity;
    private final ConstraintLayout rootLayout;
    private final View mapContainer;
    private final View widgetPanel;
    private final View appDock;
    private final View appDrawerContainer;
    private final ImageButton widgetHandle;

    public CarLayoutManager(MapActivity activity) {
        this.activity = activity;
        this.rootLayout = activity.findViewById(R.id.root_layout);
        this.mapContainer = activity.findViewById(R.id.map_container);
        this.widgetPanel = activity.findViewById(R.id.widget_panel);
        this.appDock = activity.findViewById(R.id.app_dock);
        this.appDrawerContainer = activity.findViewById(R.id.app_drawer_container);
        this.widgetHandle = activity.findViewById(R.id.widget_handle);
    }

    public void applyLayout(boolean isWidgetPanelOpen, int layoutMode) {
        if (rootLayout == null || widgetPanel == null || appDock == null) return;

        CarLauncherSettings carSettings = new CarLauncherSettings(activity);
        String dockPos = carSettings.getDockPosition(); 
        String widgetPos = carSettings.getWidgetPanelPosition();
        boolean isPortrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        ConstraintSet cs = new ConstraintSet();
        cs.clone(rootLayout);

        // 1. Reset all regions
        int[] ids = {R.id.app_dock, R.id.widget_panel, R.id.map_container, R.id.app_drawer_container};
        for (int id : ids) {
            cs.clear(id, ConstraintSet.TOP);
            cs.clear(id, ConstraintSet.BOTTOM);
            cs.clear(id, ConstraintSet.START);
            cs.clear(id, ConstraintSet.END);
        }

        // 2. Dock Region
        int dockSize = (int) activity.getResources().getDimension(R.dimen.dock_height);
        int sidebarWidth = (int) (64 * activity.getResources().getDisplayMetrics().density); // Fixed safe width for sidebar
        if (isPortrait) dockPos = "bottom";

        switch (dockPos) {
            case "left":
                cs.connect(R.id.app_dock, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                cs.connect(R.id.app_dock, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(R.id.app_dock, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                cs.constrainWidth(R.id.app_dock, sidebarWidth);
                cs.constrainHeight(R.id.app_dock, 0);
                break;
            case "right":
                cs.connect(R.id.app_dock, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                cs.connect(R.id.app_dock, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(R.id.app_dock, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                cs.constrainWidth(R.id.app_dock, sidebarWidth);
                cs.constrainHeight(R.id.app_dock, 0);
                break;
            default: // bottom
                cs.connect(R.id.app_dock, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                cs.connect(R.id.app_dock, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                cs.connect(R.id.app_dock, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                cs.constrainHeight(R.id.app_dock, dockSize);
                cs.constrainWidth(R.id.app_dock, 0);
                break;
        }

        // 3. Widget Region
        if (!isWidgetPanelOpen) {
            cs.setVisibility(R.id.widget_panel, View.GONE);
        } else {
            cs.setVisibility(R.id.widget_panel, View.VISIBLE);
            float panelPercent = carSettings.getWidgetPanelWidthPercent();
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

            if (isPortrait || "bottom".equals(widgetPos)) {
                cs.connect(R.id.widget_panel, ConstraintSet.BOTTOM, "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM);
                cs.connect(R.id.widget_panel, ConstraintSet.START, "left".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "left".equals(dockPos) ? ConstraintSet.END : ConstraintSet.START);
                cs.connect(R.id.widget_panel, ConstraintSet.END, "right".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "right".equals(dockPos) ? ConstraintSet.START : ConstraintSet.END);
                cs.constrainHeight(R.id.widget_panel, (int)(screenHeight * 0.42f)); // Optimized portrait height
                cs.constrainWidth(R.id.widget_panel, 0);
            } else if ("left".equals(widgetPos)) {
                cs.connect(R.id.widget_panel, ConstraintSet.START, "left".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "left".equals(dockPos) ? ConstraintSet.END : ConstraintSet.START);
                cs.connect(R.id.widget_panel, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(R.id.widget_panel, ConstraintSet.BOTTOM, "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM);
                cs.constrainWidth(R.id.widget_panel, (int)(screenWidth * panelPercent));
                cs.constrainHeight(R.id.widget_panel, 0);
            } else { // right
                cs.connect(R.id.widget_panel, ConstraintSet.END, "right".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "right".equals(dockPos) ? ConstraintSet.START : ConstraintSet.END);
                cs.connect(R.id.widget_panel, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(R.id.widget_panel, ConstraintSet.BOTTOM, "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM);
                cs.constrainWidth(R.id.widget_panel, (int)(screenWidth * panelPercent));
                cs.constrainHeight(R.id.widget_panel, 0);
            }
        }

        // 4. Map Region
        cs.connect(R.id.map_container, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        if (isPortrait) {
            if (isWidgetPanelOpen) {
                cs.connect(R.id.map_container, ConstraintSet.BOTTOM, R.id.widget_panel, ConstraintSet.TOP);
            } else {
                cs.connect(R.id.map_container, ConstraintSet.BOTTOM, R.id.app_dock, ConstraintSet.TOP);
            }
            cs.connect(R.id.map_container, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            cs.connect(R.id.map_container, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        } else {
            // Landscape Start
            if (isWidgetPanelOpen && "left".equals(widgetPos)) {
                cs.connect(R.id.map_container, ConstraintSet.START, R.id.widget_panel, ConstraintSet.END);
            } else if ("left".equals(dockPos)) {
                cs.connect(R.id.map_container, ConstraintSet.START, R.id.app_dock, ConstraintSet.END);
            } else {
                cs.connect(R.id.map_container, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            }
            // Landscape End
            if (isWidgetPanelOpen && "right".equals(widgetPos)) {
                cs.connect(R.id.map_container, ConstraintSet.END, R.id.widget_panel, ConstraintSet.START);
            } else if ("right".equals(dockPos)) {
                cs.connect(R.id.map_container, ConstraintSet.END, R.id.app_dock, ConstraintSet.START);
            } else {
                cs.connect(R.id.map_container, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            }
            // Landscape Bottom
            if (isWidgetPanelOpen && "bottom".equals(widgetPos)) {
                cs.connect(R.id.map_container, ConstraintSet.BOTTOM, R.id.widget_panel, ConstraintSet.TOP);
            } else if ("bottom".equals(dockPos)) {
                cs.connect(R.id.map_container, ConstraintSet.BOTTOM, R.id.app_dock, ConstraintSet.TOP);
            } else {
                cs.connect(R.id.map_container, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            }
        }

        // 5. App Drawer Region
        cs.connect(R.id.app_drawer_container, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        cs.connect(R.id.app_drawer_container, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        cs.connect(R.id.app_drawer_container, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        if (isPortrait) {
            cs.connect(R.id.app_drawer_container, ConstraintSet.BOTTOM, isWidgetPanelOpen ? R.id.widget_panel : R.id.app_dock, ConstraintSet.TOP);
        } else {
            cs.connect(R.id.app_drawer_container, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        }

        cs.applyTo(rootLayout);

        // 6. Final UI Touch-ups (Elevation, etc)
        updateElevations(isPortrait);
        updateWidgetHandleConstraints(cs, carSettings, isWidgetPanelOpen);
        
        // 7. Refresh Dock orientation (CRITICAL: Always refresh on every layout apply to fix orientation issues)
        boolean isVertical = ("left".equals(dockPos) || "right".equals(dockPos)) && !isPortrait;
        refreshDockFragment(isVertical);
    }

    private void updateElevations(boolean isPortrait) {
        if (mapContainer != null) mapContainer.setElevation(0f);
        if (appDock != null) appDock.setElevation(10f);
        if (widgetPanel != null) widgetPanel.setElevation(isPortrait ? 2f : 15f);
        if (appDrawerContainer != null) appDrawerContainer.setElevation(50f);
    }

    private void updateWidgetHandleConstraints(ConstraintSet cs, CarLauncherSettings settings, boolean isOpen) {
        if (widgetHandle != null) {
            String widgetPos = settings.getWidgetPanelPosition();
            
            // AGGRESSIVE CLEAR: Remove all potential constraint anchors for handle
            cs.clear(R.id.widget_handle);
            
            // Re-apply common constraints
            cs.constrainWidth(R.id.widget_handle, (int)(48 * activity.getResources().getDisplayMetrics().density));
            cs.constrainHeight(R.id.widget_handle, (int)(96 * activity.getResources().getDisplayMetrics().density));
            
            if ("left".equals(widgetPos)) {
                // Panel solda, ok panelin SAĞ (END) sınırına dayanmalı ve haritaya doğru taşmalı
                cs.connect(R.id.widget_handle, ConstraintSet.START, R.id.widget_panel, ConstraintSet.END);
                cs.setMargin(R.id.widget_handle, ConstraintSet.START, (int)(-16 * activity.getResources().getDisplayMetrics().density)); // Overlap slightly
                widgetHandle.setImageResource(isOpen ? net.osmand.plus.R.drawable.ic_chevron_left : net.osmand.plus.R.drawable.ic_chevron_right);
            } else {
                // Panel sağda, ok panelin SOL (START) sınırına dayanmalı ve haritaya doğru taşmalı
                cs.connect(R.id.widget_handle, ConstraintSet.END, R.id.widget_panel, ConstraintSet.START);
                // Haritaya doğru 8dp taşması için margin veriyoruz
                cs.setMargin(R.id.widget_handle, ConstraintSet.END, (int)(8 * activity.getResources().getDisplayMetrics().density));
                widgetHandle.setImageResource(isOpen ? net.osmand.plus.R.drawable.ic_chevron_right : net.osmand.plus.R.drawable.ic_chevron_left);
            }
            
            // Dikey konum ve Yüksek Z-Ekseni (Z-Order)
            cs.setVerticalBias(R.id.widget_handle, settings.getWidgetHandleVerticalBias());
            cs.connect(R.id.widget_handle, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(R.id.widget_handle, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            
            // Görsel olarak önde durmasını garanti et (Elevation programatik olarak da set edilebilir)
            widgetHandle.setElevation(100f); 
        }
    }

    private void refreshDockFragment(boolean isVertical) {
        AppDockFragment dock = activity.getAppDockFragment();
        if (dock != null) {
            dock.setOrientation(isVertical);
        }
    }
}
