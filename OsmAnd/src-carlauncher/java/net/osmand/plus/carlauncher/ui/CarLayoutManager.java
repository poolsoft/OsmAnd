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

        // 1. Reset all regions (widget_handle'i resetleme - elle yonetilir)
        int[] ids = {R.id.app_dock, R.id.widget_panel, R.id.map_container, R.id.app_drawer_container};
        for (int id : ids) {
            cs.clear(id, ConstraintSet.TOP);
            cs.clear(id, ConstraintSet.BOTTOM);
            cs.clear(id, ConstraintSet.START);
            cs.clear(id, ConstraintSet.END);
        }

        // 2. Dock Region
        int dockSize = (int) activity.getResources().getDimension(R.dimen.dock_height);
        int sidebarWidth = (int) (64 * activity.getResources().getDisplayMetrics().density);
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

        // 3. Widget Region - Handle her zaman gorunur, panel acik/kapali durumdan bagimsiz
        float panelPercent = carSettings.getWidgetPanelWidthPercent();
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        if (!isWidgetPanelOpen) {
            cs.setVisibility(R.id.widget_panel, View.GONE);
            // Panel kapaliyken handle'i parent'in sag/sol kenarina yasla
            // (translationX ile panel acikkenki pozisyonuna kayacak)
        } else {
            cs.setVisibility(R.id.widget_panel, View.VISIBLE);
            if (isPortrait || "bottom".equals(widgetPos)) {
                cs.connect(R.id.widget_panel, ConstraintSet.BOTTOM, "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM);
                cs.connect(R.id.widget_panel, ConstraintSet.START, "left".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "left".equals(dockPos) ? ConstraintSet.END : ConstraintSet.START);
                cs.connect(R.id.widget_panel, ConstraintSet.END, "right".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "right".equals(dockPos) ? ConstraintSet.START : ConstraintSet.END);
                cs.constrainHeight(R.id.widget_panel, (int)(screenHeight * 0.30f));
                cs.constrainWidth(R.id.widget_panel, 0);
                View clockContainer = activity.findViewById(R.id.clock_settings_container);
                if (clockContainer != null) clockContainer.setVisibility(isPortrait ? View.GONE : View.VISIBLE);
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

        // 6. Handle constraint'lerini ayarla (translationX henuz yok)
        updateWidgetHandleConstraints(cs, carSettings, isWidgetPanelOpen);

        // 7. Final UI Touch-ups (Elevation, etc)
        updateElevations(isPortrait);

        // 8. APPLY ALL CONSTRAINTS AT ONCE
        cs.applyTo(rootLayout);

        // 9. BRING TO FRONT (Z-INDEX)
        if (widgetHandle != null) {
            widgetHandle.bringToFront();
        }

        // 10. TRANSLATIONX - constraint uygulandiktan SONRA
        //     (cs.applyTo() translationX'i sifirlayabilir)
        applyWidgetHandleTranslation(carSettings, isWidgetPanelOpen);
        
        // 11. Refresh Dock orientation
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
            boolean isPortrait = activity.getResources().getConfiguration().orientation 
                    == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            if (isPortrait) {
                widgetHandle.setVisibility(View.GONE);
                return;
            }
            
            widgetHandle.setVisibility(View.VISIBLE);
            String widgetPos = settings.getWidgetPanelPosition();
            float density = activity.getResources().getDisplayMetrics().density;
            int handleWidth = (int)(48 * density);
            int handleHeight = (int)(96 * density);
            
            // 1. TAM TEMIZLIK
            cs.clear(R.id.widget_handle);
            
            // 2. BOYUTLANDIRMA
            cs.constrainWidth(R.id.widget_handle, handleWidth);
            cs.constrainHeight(R.id.widget_handle, handleHeight);
            
            // 3. DIKEY HIZALAMA - parent'a gore, panel'den bagimsiz
            cs.connect(R.id.widget_handle, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(R.id.widget_handle, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.setVerticalBias(R.id.widget_handle, settings.getWidgetHandleVerticalBias());

            // 4. YATAY POZISYON - START constraint + translationX ile
            // START ve END ayni yone baglanirsa ConstraintLayout cozulemez,
            // bu nedenle SADECE START baglanir, genislik constrainWidth ile verilir
            if ("left".equals(widgetPos)) {
                cs.connect(R.id.widget_handle, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            } else {
                cs.connect(R.id.widget_handle, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            }
            
            // 5. EN UST KATMAN GARANTISI
            widgetHandle.setElevation(100f); 
            widgetHandle.setZ(100f);
        }
    }

    /**
     * Constraint uygulandiktan SONRA translationX ayarlanir.
     * cs.applyTo() tum view pozisyonlarini sifirlar, bu nedenle
     * translationX ANCAK ondan sonra gecerli olur.
     */
    private void applyWidgetHandleTranslation(CarLauncherSettings settings, boolean isOpen) {
        if (widgetHandle == null) return;
        
        boolean isPortrait = activity.getResources().getConfiguration().orientation 
                == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        if (isPortrait) return;
        
        String widgetPos = settings.getWidgetPanelPosition();
        float density = activity.getResources().getDisplayMetrics().density;
        int handleWidth = (int)(48 * density);
        float panelTranslateX = 0;
        
        if (isOpen) {
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            float panelPercent = settings.getWidgetPanelWidthPercent();
            int panelWidth = (int)(screenWidth * panelPercent);
            
            if ("left".equals(widgetPos)) {
                // Panel solda -> handle widget'in SAĞ kenarinda, harita uzerinde
                // Ekranin solundan panelWidth kadar uzakta, handle'in yarisi haritaya binsin
                panelTranslateX = panelWidth - handleWidth / 3f;
            } else {
                // Panel sagda -> handle widget'in SOL kenarinda, harita uzerinde
                // Ekranin sagindan panelWidth kadar uzakta
                panelTranslateX = -(panelWidth - handleWidth / 3f);
            }
        } else {
            // Panel kapali -> handle ekranin en kenarinda hafif gorunur
            if ("left".equals(widgetPos)) {
                panelTranslateX = -handleWidth / 3f;
            } else {
                panelTranslateX = handleWidth / 3f;
            }
        }
        
        widgetHandle.setTranslationX(panelTranslateX);
        
        // Ikon yonu - ayrik olarak burada da ayarla (guvenlik)
        if ("left".equals(widgetPos)) {
            widgetHandle.setImageResource(isOpen ? R.drawable.ic_chevron_left : R.drawable.ic_chevron_right);
        } else {
            widgetHandle.setImageResource(isOpen ? R.drawable.ic_chevron_right : R.drawable.ic_chevron_left);
        }
    }

    private void refreshDockFragment(boolean isVertical) {
        AppDockFragment dock = activity.getAppDockFragment();
        if (dock != null) {
            dock.setOrientation(isVertical);
        }
    }
}
