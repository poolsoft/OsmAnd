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

    private boolean isContentFullScreen = false;

    public void setContentFullScreen(boolean fullScreen) {
        this.isContentFullScreen = fullScreen;
    }

    public boolean isContentFullScreen() {
        return isContentFullScreen;
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

        // 2. Dock Region - dockSize (0-100) ayarina gore olceklendir
        // 0=min(0.3x), 50=normal(1.0x), 100=max(1.7x)
        int dockSizePercent = carSettings.getDockSize();
        float dockScale = 0.3f + (dockSizePercent / 100.0f) * 1.4f;
        int dockSize = (int) (activity.getResources().getDimension(R.dimen.dock_height) * dockScale);
        int sidebarWidth = (int) (64 * activity.getResources().getDisplayMetrics().density * dockScale);
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

        // 3. Widget ve Harita Alanlari - Harita herzaman ekranda kalacak sekilde swap mantigi
        float panelPercent = carSettings.getWidgetPanelWidthPercent();
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        if (!isWidgetPanelOpen) {
            cs.setVisibility(R.id.widget_panel, View.GONE);
            // Harita tum ekrani kaplar (dock haric)
            cs.connect(R.id.map_container, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            if (isPortrait) {
                cs.connect(R.id.map_container, ConstraintSet.BOTTOM, "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM);
                cs.connect(R.id.map_container, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                cs.connect(R.id.map_container, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            } else {
                cs.connect(R.id.map_container, ConstraintSet.START, "left".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "left".equals(dockPos) ? ConstraintSet.END : ConstraintSet.START);
                cs.connect(R.id.map_container, ConstraintSet.END, "right".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "right".equals(dockPos) ? ConstraintSet.START : ConstraintSet.END);
                cs.connect(R.id.map_container, ConstraintSet.BOTTOM, "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM);
            }
            cs.constrainWidth(R.id.map_container, 0);
            cs.constrainHeight(R.id.map_container, 0);
        } else {
            cs.setVisibility(R.id.widget_panel, View.VISIBLE);
            boolean isSwapped = isContentFullScreen;

            if (isPortrait || "bottom".equals(widgetPos)) {
                // Dikey yerlesim: Ust Panel ve Alt Panel
                int topViewId;
                int bottomViewId;
                if (isSwapped) {
                    topViewId = R.id.widget_panel;   // LARGE
                    bottomViewId = R.id.map_container; // SMALL
                } else {
                    topViewId = R.id.map_container;  // LARGE
                    bottomViewId = R.id.widget_panel;  // SMALL
                }

                float portraitPanelHeight = isPortrait ? carSettings.getWidgetPanelHeightPortrait() : 0.30f;
                int smallHeight = (int) (screenHeight * portraitPanelHeight);

                // Her iki gorunum de yatayda yayilir
                int leftBorder = "left".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID;
                int rightBorder = "right".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID;
                int leftSide = "left".equals(dockPos) ? ConstraintSet.END : ConstraintSet.START;
                int rightSide = "right".equals(dockPos) ? ConstraintSet.START : ConstraintSet.END;

                cs.connect(topViewId, ConstraintSet.START, leftBorder, leftSide);
                cs.connect(topViewId, ConstraintSet.END, rightBorder, rightSide);
                cs.constrainWidth(topViewId, 0);

                cs.connect(bottomViewId, ConstraintSet.START, leftBorder, leftSide);
                cs.connect(bottomViewId, ConstraintSet.END, rightBorder, rightSide);
                cs.constrainWidth(bottomViewId, 0);

                // Dikey zincirleme (Vertical chains)
                cs.connect(topViewId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(topViewId, ConstraintSet.BOTTOM, bottomViewId, ConstraintSet.TOP);

                cs.connect(bottomViewId, ConstraintSet.TOP, topViewId, ConstraintSet.BOTTOM);
                cs.connect(bottomViewId, ConstraintSet.BOTTOM, "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID, "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM);

                // Yukseklikleri sinirla
                cs.constrainHeight(topViewId, 0); // LARGE gorunum kalan alanı doldurur
                cs.constrainHeight(bottomViewId, smallHeight); // SMALL gorunum sabit/hesapli yukseklik alir

                View clockContainer = activity.findViewById(R.id.clock_settings_container);
                if (clockContainer != null) clockContainer.setVisibility(isPortrait ? View.GONE : View.VISIBLE);

            } else {
                // Yatay yerlesim: Sol Panel ve Sag Panel
                int leftBorder = "left".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID;
                int rightBorder = "right".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID;
                int leftSide = "left".equals(dockPos) ? ConstraintSet.END : ConstraintSet.START;
                int rightSide = "right".equals(dockPos) ? ConstraintSet.START : ConstraintSet.END;

                int leftViewId;
                int rightViewId;
                boolean leftViewIsSmall = "left".equals(widgetPos);

                if (leftViewIsSmall) {
                    if (isSwapped) {
                        // SMALL solda (harita), LARGE sagda (widget)
                        leftViewId = R.id.map_container;
                        rightViewId = R.id.widget_panel;
                    } else {
                        // SMALL solda (widget), LARGE sagda (harita)
                        leftViewId = R.id.widget_panel;
                        rightViewId = R.id.map_container;
                    }
                } else {
                    // Sagdaki panel SMALL
                    if (isSwapped) {
                        // LARGE solda (widget), SMALL sagda (harita)
                        leftViewId = R.id.widget_panel;
                        rightViewId = R.id.map_container;
                    } else {
                        // LARGE solda (harita), SMALL sagda (widget)
                        leftViewId = R.id.map_container;
                        rightViewId = R.id.widget_panel;
                    }
                }

                int smallWidth = (int) (screenWidth * panelPercent);

                // Her iki gorunum de dikeyde yayilir
                int bottomBorder = "bottom".equals(dockPos) ? R.id.app_dock : ConstraintSet.PARENT_ID;
                int bottomSide = "bottom".equals(dockPos) ? ConstraintSet.TOP : ConstraintSet.BOTTOM;

                cs.connect(leftViewId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(leftViewId, ConstraintSet.BOTTOM, bottomBorder, bottomSide);
                cs.constrainHeight(leftViewId, 0);

                cs.connect(rightViewId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(rightViewId, ConstraintSet.BOTTOM, bottomBorder, bottomSide);
                cs.constrainHeight(rightViewId, 0);

                // Yatay zincirleme (Horizontal chains)
                cs.connect(leftViewId, ConstraintSet.START, leftBorder, leftSide);
                cs.connect(leftViewId, ConstraintSet.END, rightViewId, ConstraintSet.START);

                cs.connect(rightViewId, ConstraintSet.START, leftViewId, ConstraintSet.END);
                cs.connect(rightViewId, ConstraintSet.END, rightBorder, rightSide);

                // Genislikleri yerlesime gore ayarla
                if (leftViewIsSmall) {
                    cs.constrainWidth(leftViewId, smallWidth);
                    cs.constrainWidth(rightViewId, 0);
                } else {
                    cs.constrainWidth(leftViewId, 0);
                    cs.constrainWidth(rightViewId, smallWidth);
                }
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
            float density = activity.getResources().getDisplayMetrics().density;
            
            cs.clear(R.id.widget_handle);
            
            if (isPortrait) {
                // PORTRAIT: handle yatay cubuk, panelin ust kenarinda, panel genisliginde
                int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                widgetHandle.setVisibility(View.VISIBLE);
                cs.constrainWidth(R.id.widget_handle, screenWidth);
                cs.constrainHeight(R.id.widget_handle, (int)(12 * density));
                cs.connect(R.id.widget_handle, ConstraintSet.TOP, R.id.widget_panel, ConstraintSet.TOP);
                cs.connect(R.id.widget_handle, ConstraintSet.START, R.id.widget_panel, ConstraintSet.START);
                cs.connect(R.id.widget_handle, ConstraintSet.END, R.id.widget_panel, ConstraintSet.END);
                widgetHandle.setImageResource(R.drawable.ic_action_view);
            } else {
                // LANDSCAPE: daire handle, ekranin kenarinda
                widgetHandle.setVisibility(View.VISIBLE);
                int handleSize = (int)(48 * density);
                String widgetPos = settings.getWidgetPanelPosition();
                
                cs.constrainWidth(R.id.widget_handle, handleSize);
                cs.constrainHeight(R.id.widget_handle, handleSize);
                cs.connect(R.id.widget_handle, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(R.id.widget_handle, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                cs.setVerticalBias(R.id.widget_handle, settings.getWidgetHandleVerticalBias());
                
                if ("left".equals(widgetPos)) {
                    cs.connect(R.id.widget_handle, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                } else {
                    cs.connect(R.id.widget_handle, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                }
            }
            
            widgetHandle.setElevation(8f); 
            widgetHandle.setZ(8f);
        }
    }

    /**
     * Constraint uygulandiktan SONRA translation/pozisyon ayarlanir.
     */
    private void applyWidgetHandleTranslation(CarLauncherSettings settings, boolean isOpen) {
        if (widgetHandle == null) return;
        
        boolean isPortrait = activity.getResources().getConfiguration().orientation 
                == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        float density = activity.getResources().getDisplayMetrics().density;
        
        if (isPortrait) {
            // Portrait: yatay cubuk panelin ustunde, surukleme ile height degisir
            // Handle panelin ust kenarina yapismis, zaten constraint ile orada
            // Drag handler ayri bir metodda setup edilir (ilk cagrida bir kere)
            if (widgetHandle.getTag() == null) {
                setupPortraitHandleDrag(settings);
                widgetHandle.setTag("draggable");
            }
            widgetHandle.setTranslationX(0);
            widgetHandle.setTranslationY(0);
            widgetHandle.setImageResource(R.drawable.ic_action_view);
            return;
        }
        
        // LANDSCAPE: daire handle
        String widgetPos = settings.getWidgetPanelPosition();
        int handleWidth = (int)(48 * density);
        float panelTranslateX = 0;
        
        if (isOpen) {
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            float panelPercent = settings.getWidgetPanelWidthPercent();
            int panelWidth = (int)(screenWidth * panelPercent);
            
            if ("left".equals(widgetPos)) {
                panelTranslateX = panelWidth - handleWidth / 3f;
            } else {
                panelTranslateX = -(panelWidth - handleWidth / 3f);
            }
        } else {
            if ("left".equals(widgetPos)) {
                panelTranslateX = -handleWidth / 3f;
            } else {
                panelTranslateX = handleWidth / 3f;
            }
        }
        
        widgetHandle.setTranslationX(panelTranslateX);
        
        if ("left".equals(widgetPos)) {
            widgetHandle.setImageResource(isOpen ? R.drawable.ic_chevron_left : R.drawable.ic_chevron_right);
        } else {
            widgetHandle.setImageResource(isOpen ? R.drawable.ic_chevron_right : R.drawable.ic_chevron_left);
        }
    }

    /**
     * Portrait modda widget_panel height'ini degistirmek icin
     * handle'a surukleme (drag) ozelligi ekler.
     */
    private void setupPortraitHandleDrag(CarLauncherSettings settings) {
        widgetHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setScaleY(1.5f); // gorsel feedback
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
                    float touchY = event.getRawY();
                    // Panel height = ekranin en altindan touch noktasina kadar
                    int maxHeight = (int)(screenHeight * 0.7f);
                    int minHeight = (int)(screenHeight * 0.1f);
                    int newHeight = Math.max(minHeight, Math.min(maxHeight, screenHeight - (int)touchY));
                    float heightPercent = (float)newHeight / screenHeight;
                    
                    // Panel height'ini dogrudan degistir
                    ViewGroup.LayoutParams lp = widgetPanel.getLayoutParams();
                    if (lp != null) {
                        lp.height = newHeight;
                        widgetPanel.setLayoutParams(lp);
                    }
                    
                    // Kaydet (canli)
                    settings.setWidgetPanelHeightPortrait(heightPercent);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setScaleY(1.0f);
                    v.performClick();
                    return true;
            }
            return false;
        });
    }

    private void refreshDockFragment(boolean isVertical) {
        AppDockFragment dock = activity.getAppDockFragment();
        if (dock != null) {
            dock.setOrientation(isVertical);
        }
    }
}
