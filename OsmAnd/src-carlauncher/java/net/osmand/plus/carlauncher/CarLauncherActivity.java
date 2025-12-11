package net.osmand.plus.carlauncher;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * CarLauncher variant'i icin ozellestirilmis MapActivity.
 * %70 harita + %30 widget panel + app dock layout.
 */
public class CarLauncherActivity extends MapActivity {

    private static final String TAG = "CarLauncherActivity";

    // Layout bileşenleri
    private ConstraintLayout rootLayout;
    private FrameLayout mapContainer;
    private FrameLayout widgetPanel;
    private LinearLayout appDock;
    private ImageButton btnToggleUi;
    private ImageButton btnToggleDock;
    private FrameLayout appDrawerContainer;

    // Durum
    private boolean isUiVisible = true;
    private boolean isDockVisible = true;

    // Layout modları
    private enum LayoutMode {
        LAUNCHER, // %70 harita + %30 widget + dock
        NAVIGATION, // %70 harita + %30 widget (dock gizli)
        FULL_MAP // %100 harita (tum UI gizli)
    }

    private LayoutMode currentMode = LayoutMode.LAUNCHER;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Tam ekran ayarlari
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);

        Log.i(TAG, "CarLauncher activity started");

        // Custom layout'u inflate et
        inflateCarLauncherLayout();

        // Toggle button'lari ayarla
        setupToggleButtons();
    }

    /**
     * Custom car launcher layout'unu inflate eder.
     */
    private void inflateCarLauncherLayout() {
        // Ana layout'u inflate et
        View carLauncherView = getLayoutInflater().inflate(
                R.layout.activity_car_launcher, null);

        // MapActivity'nin root view'ına ekle
        ViewGroup rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.addView(carLauncherView);
        }

        // View referanslarını al
        rootLayout = findViewById(R.id.root_layout);
        mapContainer = findViewById(R.id.map_container);
        widgetPanel = findViewById(R.id.widget_panel);
        appDock = findViewById(R.id.app_dock);
        btnToggleUi = findViewById(R.id.btn_toggle_ui);
        btnToggleDock = findViewById(R.id.btn_toggle_dock);
        appDrawerContainer = findViewById(R.id.app_drawer_container);

        // MapActivity'nin harita view'ını map_container'a taşı
        embedMapView();
    }

    /**
     * MapActivity'nin harita view'ini map_container'a gomur.
     */
    private void embedMapView() {
        try {
            // Find MapActivity's real map view by ID
            // OsmandMapTileView returned by getMapView() is not a View object!
            View mapView = findViewById(R.id.map_view_with_layers);

            if (mapView != null && mapContainer != null) {
                // Eski parent'tan ayır
                if (mapView.getParent() != null) {
                    ((ViewGroup) mapView.getParent()).removeView(mapView);
                }

                // Yeni container'a ekle
                mapContainer.addView(mapView,
                        new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));

                Log.d(TAG, "Map view embedded successfully");
            } else {
                Log.e(TAG, "Map view not found by ID: R.id.map_view_with_layers");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error embedding map view", e);
        }

        // Widget panelini ekle
        embedWidgetPanel();
    }

    /**
     * Widget panelini widget_panel container'a ekle.
     */
    private void embedWidgetPanel() {
        if (widgetPanel != null) {
            net.osmand.plus.carlauncher.ui.WidgetPanelFragment widgetPanelFragment = new net.osmand.plus.carlauncher.ui.WidgetPanelFragment();

            getSupportFragmentManager().beginTransaction()
                    .replace(widgetPanel.getId(), widgetPanelFragment, "widget_panel")
                    .commitAllowingStateLoss();

            Log.d(TAG, "Widget panel embedded successfully");
        }

        // App dock'u ekle
        embedAppDock();
    }

    /**
     * App dock'u app_dock container'a ekle.
     */
    private void embedAppDock() {
        if (appDock != null) {
            net.osmand.plus.carlauncher.ui.AppDockFragment appDockFragment = new net.osmand.plus.carlauncher.ui.AppDockFragment();

            getSupportFragmentManager().beginTransaction()
                    .replace(appDock.getId(), appDockFragment, "app_dock")
                    .commitAllowingStateLoss();

            Log.d(TAG, "App dock embedded successfully");
        }
    }

    /**
     * Toggle button listener'larini ayarla.
     */
    private void setupToggleButtons() {
        if (btnToggleUi != null) {
            btnToggleUi.setOnClickListener(v -> toggleUiVisibility());
        }

        if (btnToggleDock != null) {
            btnToggleDock.setOnClickListener(v -> toggleDockVisibility());
        }
    }

    /**
     * Tum UI'yi (widget panel + dock) gizle/goster.
     */
    private void toggleUiVisibility() {
        isUiVisible = !isUiVisible;

        if (isUiVisible) {
            // Launcher mode'a don
            setLayoutMode(LayoutMode.LAUNCHER);
        } else {
            // Full map mode
            setLayoutMode(LayoutMode.FULL_MAP);
        }
    }

    /**
     * Sadece dock'u gizle/goster.
     */
    private void toggleDockVisibility() {
        isDockVisible = !isDockVisible;

        if (isDockVisible) {
            setLayoutMode(LayoutMode.LAUNCHER);
        } else {
            setLayoutMode(LayoutMode.NAVIGATION);
        }
    }

    /**
     * Layout modunu degistir.
     */
    private void setLayoutMode(LayoutMode mode) {
        if (rootLayout == null)
            return;

        currentMode = mode;

        // Animasyon basla
        TransitionManager.beginDelayedTransition(rootLayout,
                new AutoTransition().setDuration(300));

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(rootLayout);

        switch (mode) {
            case LAUNCHER:
                // %70 harita + %30 widget + dock gorunur
                setLauncherMode(constraintSet);
                break;

            case NAVIGATION:
                // %70 harita + %30 widget, dock gizli
                setNavigationMode(constraintSet);
                break;

            case FULL_MAP:
                // %100 harita, tum UI gizli
                setFullMapMode(constraintSet);
                break;
        }

        constraintSet.applyTo(rootLayout);
        Log.d(TAG, "Layout mode changed to: " + mode);
    }

    /**
     * Launcher mode: %70-30 split + dock
     */
    private void setLauncherMode(ConstraintSet cs) {
        widgetPanel.setVisibility(View.VISIBLE);
        appDock.setVisibility(View.VISIBLE);
        btnToggleUi.setVisibility(View.VISIBLE);
        btnToggleDock.setVisibility(View.VISIBLE);

        // %70-30 split
        cs.setHorizontalWeight(R.id.map_container, 0.7f);
        cs.setHorizontalWeight(R.id.widget_panel, 0.3f);
    }

    /**
     * Navigation mode: %70-30 split, dock gizli
     */
    private void setNavigationMode(ConstraintSet cs) {
        widgetPanel.setVisibility(View.VISIBLE);
        appDock.setVisibility(View.GONE);
        btnToggleUi.setVisibility(View.VISIBLE);
        btnToggleDock.setVisibility(View.VISIBLE);

        // %70-30 split
        cs.setHorizontalWeight(R.id.map_container, 0.7f);
        cs.setHorizontalWeight(R.id.widget_panel, 0.3f);
    }

    /**
     * Full map mode: %100 harita
     */
    private void setFullMapMode(ConstraintSet cs) {
        widgetPanel.setVisibility(View.GONE);
        appDock.setVisibility(View.GONE);
        btnToggleUi.setVisibility(View.VISIBLE); // Sadece toggle button gorunur
        btnToggleDock.setVisibility(View.GONE);

        // %100 harita
        cs.setHorizontalWeight(R.id.map_container, 1.0f);
        cs.setHorizontalWeight(R.id.widget_panel, 0.0f);
    }

    /**
     * App Drawer'i ac.
     */
    public void openAppDrawer() {
        if (appDrawerContainer != null) {
            appDrawerContainer.setVisibility(View.VISIBLE);

            // Fragment ekli degilse ekle
            if (getSupportFragmentManager()
                    .findFragmentByTag(net.osmand.plus.carlauncher.ui.AppDrawerFragment.TAG) == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.app_drawer_container, new net.osmand.plus.carlauncher.ui.AppDrawerFragment(),
                                net.osmand.plus.carlauncher.ui.AppDrawerFragment.TAG)
                        .commitAllowingStateLoss();
            }
        }
    }

    /**
     * App Drawer'i kapat.
     */
    public void closeAppDrawer() {
        if (appDrawerContainer != null) {
            appDrawerContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (appDrawerContainer != null && appDrawerContainer.getVisibility() == View.VISIBLE) {
            closeAppDrawer();
            return;
        }

        // Full map mode'daysa launcher mode'a don
        if (currentMode == LayoutMode.FULL_MAP) {
            setLayoutMode(LayoutMode.LAUNCHER);
            isUiVisible = true;
            isDockVisible = true;
        } else {
            // Launcher modunda geri tusu hicbir sey yapmasin
            // (home launcher davranisi)
        }
    }
}
