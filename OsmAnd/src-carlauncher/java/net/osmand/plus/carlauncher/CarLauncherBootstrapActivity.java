package net.osmand.plus.carlauncher;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.carlauncher.ui.AppDockFragment;
import net.osmand.plus.carlauncher.ui.CarLauncherInitManager;
import net.osmand.plus.carlauncher.ui.CarLayoutManager;
import net.osmand.plus.carlauncher.ui.PanelContentManager;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Lightweight HOME surface displayed while OsmAnd finishes application startup.
 * It intentionally reuses the real launcher layout and fragments, and never
 * creates the map view or touches OsmAnd core initialization.
 */
public class CarLauncherBootstrapActivity extends AppCompatActivity
        implements CarLauncherInterface, AppDockFragment.OnAppDockListener {

    private static final long MIN_SHELL_VISIBLE_MS = 250L;
    private static final long MAP_START_DEADLINE_MS = 20_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable deadlineRunnable = () -> CarLauncherInitManager.getInstance()
            .recordStartupTimeout(this, MAP_START_DEADLINE_MS);
    private long firstFrameTime;
    private boolean mapLaunchRequested;
    private OsmandApplication app;
    private AppInitializeListener initListener;
    private CarLayoutManager layoutManager;
    private PanelContentManager panelContentManager;
    private int layoutMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CarLauncherInitManager initManager = CarLauncherInitManager.getInstance();
        // When music keeps the process alive, the OsmAnd core and usually the map task are
        // already warm. Skip rebuilding the temporary launcher shell and route HOME directly
        // to the one canonical MapActivity task.
        if (initManager.isCoreReady()) {
            openMapActivity("warm_home");
            return;
        }
        initManager.configureStartupProfile(this);
        initManager.startInitTimer();
        setContentView(R.layout.activity_car_launcher);

        FrameLayout mapContainer = findViewById(R.id.map_container);
        if (mapContainer != null) {
            getLayoutInflater().inflate(R.layout.layout_map_loading_placeholder,
                    mapContainer, true);
        }

        layoutManager = new CarLayoutManager(this);
        panelContentManager = new PanelContentManager(
                getSupportFragmentManager(), R.id.widget_panel);
        panelContentManager.setOnFullScreenStateChangeListener(fullScreen -> {
            layoutManager.setContentFullScreen(fullScreen);
            layoutManager.applyLayout(true, layoutMode);
        });
        layoutManager.applyLayout(true, layoutMode);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.app_dock, new AppDockFragment(), "bootstrap_dock")
                .commitNowAllowingStateLoss();
        panelContentManager.setContent(PanelContentManager.PanelContent.WIDGETS);

        View root = findViewById(R.id.root_layout);
        if (root != null) {
            root.post(() -> android.view.Choreographer.getInstance().postFrameCallback(
                    frameTimeNanos -> {
                        firstFrameTime = android.os.SystemClock.elapsedRealtime();
                        initManager.markUiReady();
                        observeCoreReadiness();
                    }));
        } else {
            observeCoreReadiness();
        }
    }

    private void observeCoreReadiness() {
        if (mapLaunchRequested || isFinishing()) return;
        app = (OsmandApplication) getApplication();
        mainHandler.removeCallbacks(deadlineRunnable);
        mainHandler.postDelayed(deadlineRunnable, MAP_START_DEADLINE_MS);
        if (!app.isApplicationInitializing()) {
            openMapActivity("already_ready");
            return;
        }
        initListener = new AppInitializeListener() {
            @Override
            public void onProgress(@NonNull AppInitializer init,
                                   @NonNull net.osmand.plus.AppInitEvents event) {
                CarLauncherInitManager manager = CarLauncherInitManager.getInstance();
                if (manager.isCoreReady()) {
                    openMapActivity("minimum_map_ready");
                }
            }

            @Override
            public void onFinish(@NonNull AppInitializer init) {
                CarLauncherInitManager manager = CarLauncherInitManager.getInstance();
                manager.markCoreReady(CarLauncherBootstrapActivity.this);
                manager.markBackgroundReady();
                openMapActivity("background_finished");
            }
        };
        app.checkApplicationIsBeingInitialized(initListener);
        if (CarLauncherInitManager.getInstance().isCoreReady()) {
            openMapActivity("minimum_map_ready_race");
            return;
        }
        if (!app.isApplicationInitializing()) {
            openMapActivity("ready_race");
        }
    }

    private void openMapActivity(String reason) {
        if (mapLaunchRequested || isFinishing() || isDestroyed()) return;
        long now = android.os.SystemClock.elapsedRealtime();
        boolean coldTransition = "minimum_map_ready".equals(reason)
                || "background_finished".equals(reason);
        long remaining = !coldTransition || firstFrameTime == 0L ? 0L
                : MIN_SHELL_VISIBLE_MS - (now - firstFrameTime);
        if (remaining > 0L) {
            mainHandler.postDelayed(() -> openMapActivity(reason), remaining);
            return;
        }
        mapLaunchRequested = true;
        mainHandler.removeCallbacks(deadlineRunnable);
        if (app != null && initListener != null) {
            app.unsubscribeInitListener(initListener);
            initListener = null;
        }
        if (app == null || !app.isApplicationInitializing()) {
            CarLauncherInitManager.getInstance().markCoreReady(this);
        }
        // Do not forward the HOME/LAUNCHER intent categories or vendor flags to MapActivity.
        // Several head-unit ROMs treat that copied intent as a new launcher task even though
        // MapActivity is singleTask, leaving an empty bootstrap card in recents each time HOME
        // is pressed. Always address the canonical map task with a clean explicit intent.
        Intent target = new Intent(this, MapActivity.class);
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        target.putExtra("car_launcher_bootstrap_reason", reason);
        startActivity(target);
        overridePendingTransition(0, 0);
        // Bootstrap has its own affinity, so remove the empty transition task as well as
        // finishing the Activity. Otherwise some head-unit ROMs retain one empty recent task
        // for every HOME transition until their task manager becomes congested.
        finishAndRemoveTask();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(deadlineRunnable);
        if (app != null && initListener != null) {
            app.unsubscribeInitListener(initListener);
        }
        super.onDestroy();
    }

    @Override public void openAppDrawer() { setPanelContent(PanelContentManager.PanelContent.APP_DRAWER); }
    @Override public void closeAppDrawer() { setPanelContent(PanelContentManager.PanelContent.WIDGETS); }
    @Override public void openMusicPlayer() { setPanelContent(PanelContentManager.PanelContent.MUSIC); }
    @Override public void openWeatherDashboard() { setPanelContent(PanelContentManager.PanelContent.WEATHER); }
    @Override public void openAntennaAlignmentInPanel() { setPanelContent(PanelContentManager.PanelContent.ANTENNA); }
    @Override public void openAntennaAlignmentFullscreen() { openMapActivity("antenna_requested"); }
    @Override public void setPanelContent(PanelContentManager.PanelContent content) {
        if (panelContentManager != null) panelContentManager.setContent(content);
    }
    @Nullable @Override public OsmandMapTileView getMapView() { return null; }

    @Override public void onLayoutModeToggle() {
        layoutMode = (layoutMode + 1) % 2;
        if (layoutManager != null) layoutManager.applyLayout(true, layoutMode);
    }
    @Override public void onAppDrawerOpen() { openAppDrawer(); }
    @Override public void onDesktopModeToggle() { openMapActivity("desktop_requested"); }
}
