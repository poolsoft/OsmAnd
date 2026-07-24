package net.osmand.plus.activities;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_DRAWER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_STYLE_ID;
import static net.osmand.plus.chooseplan.OsmAndFeature.UNLIMITED_MAP_DOWNLOADS;
import static net.osmand.plus.firstusage.FirstUsageWizardFragment.FIRST_USAGE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.PLAN_ROUTE_MODE;
import static net.osmand.plus.search.ShowQuickSearchMode.CURRENT;
import static net.osmand.plus.settings.enums.ThemeUsageContext.MAP;
import static net.osmand.plus.settings.enums.ThemeUsageContext.OVER_MAP;
import static net.osmand.plus.views.AnimateDraggingMapThread.TARGET_NO_ROTATION;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceManager;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.SecondSplashScreenFragment;
import net.osmand.StateChangedListener;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.aidl.OsmandAidlApi.AMapPointUpdateListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.ValueHolder;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.HMDPromoFragment;
import net.osmand.plus.chooseplan.HugerockPromoFragment;
import net.osmand.plus.chooseplan.TripltekPromoFragment;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.WhatsNewDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.feedback.CrashBottomSheetDialogFragment;
import net.osmand.plus.feedback.RateUsHelper;
import net.osmand.plus.feedback.RenderInitErrorBottomSheet;
import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.firstusage.FirstUsageWizardFragment;
import net.osmand.plus.helpers.*;
import net.osmand.plus.helpers.LockHelper.LockUIAdapter;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ui.ImportGpxBottomSheetDialogFragment;
import net.osmand.plus.keyevent.KeyEventHelper;
import net.osmand.plus.keyevent.TrackballController;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedFragment;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.mapmarkers.PlanRouteFragment;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.MapAccessibilityActions;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.carlauncher.widgets.weather.WeatherManager;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.Location;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper.TransportRouteCalculationProgressCallback;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.datastorage.SharedStorageWarningFragment;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.simulation.LoadSimulatedLocationsTask.LoadSimulatedLocationsListener;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.simulation.SimulatedLocation;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.MapViewWithLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.OsmandMapTileView.OnDrawMapListener;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;
import net.osmand.plus.carlauncher.ui.AppDockFragment;
import android.transition.TransitionManager;
import android.transition.AutoTransition;
import android.transition.Transition;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends OsmandActionBarActivity implements AppDockFragment.OnAppDockListener, DownloadEvents,
		IRouteInformationListener, AMapPointUpdateListener, MapMarkerChangedListener,
		OnDrawMapListener, OsmAndAppCustomizationListener, LockUIAdapter,
		OnPreferenceStartFragmentCallback, net.osmand.plus.carlauncher.CarLauncherInterface {

	public static final String INTENT_KEY_PARENT_MAP_ACTIVITY = "intent_parent_map_activity_key";
	public static final String INTENT_PARAMS = "intent_prarams";
    
    // Weather Listener
    private OsmAndLocationProvider.OsmAndLocationListener weatherLocationListener;

	private static final int ZOOM_LABEL_DISPLAY = 16;
	private static final int MAX_ZOOM_OUT_STEPS = 2;
	private static final int SECOND_SPLASH_TIME_OUT = 8000;

	private static final Log LOG = PlatformUtil.getLog(MapActivity.class);

	private static final MapContextMenu mapContextMenu = new MapContextMenu();
	private static final MapRouteInfoMenu mapRouteInfoMenu = new MapRouteInfoMenu();
	private static final TrackDetailsMenu trackDetailsMenu = new TrackDetailsMenu();
	@Nullable
	private static Intent prevActivityIntent = null;

	private BroadcastReceiver screenOffReceiver;
	private BroadcastReceiver carFloatingButtonReceiver;
	private WidgetsVisibilityHelper mapWidgetsVisibilityHelper;
	private ExtendedMapActivity extendedMapActivity;

	private LockHelper lockHelper;
	private ImportHelper importHelper;
	private IntentHelper intentHelper;
	private MapScrollHelper mapScrollHelper;
	private RestoreNavigationHelper restoreNavigationHelper;

	private StateChangedListener<ApplicationMode> applicationModeListener;

	private final DashboardOnMap dashboardOnMap = new DashboardOnMap(this);
	private final MapFragmentsHelper fragmentsHelper = new MapFragmentsHelper(this);
	private final TrackballController trackballController = new TrackballController(this);
	private final MapPermissionsResultCallback permissionsResultCallback = new MapPermissionsResultCallback(this);

	private AppInitializeListener initListener;
	private MapViewWithLayers mapViewWithLayers;
	private DrawerLayout drawerLayout;
	private boolean drawerDisabled;

	private boolean mIsDestroyed;
	private boolean pendingPause;
	private Timer splashScreenTimer;
	private boolean activityRestartNeeded;
	private boolean stopped = true;

	// CarLauncher Fields
	private androidx.constraintlayout.widget.ConstraintLayout rootLayout;
	private net.osmand.plus.carlauncher.ui.ExactFrameLayout mapContainer;
	private android.widget.FrameLayout widgetPanel;
	private android.widget.ImageButton widgetHandle; 
	private View appDock;
	private View appDrawerContainer;
	private net.osmand.plus.carlauncher.ui.CarLayoutManager carLayoutManager;
	private net.osmand.plus.carlauncher.ui.PanelContentManager panelContentManager;
	private View mainLayoutRoot; // main.xml root reference

    private boolean isWidgetPanelOpen = true;
    private boolean isDesktopMode = false;
    private boolean isTransitioning = false;
    private static final String PREF_IS_PINNED = "widget_panel_pinned";
    
    // Sag panel iceriginin recreate sonrasinda korunmasi icin statik degisken (Turkce karakter yok)
    private static net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent lastPanelContent = null;
    
    // Global package receiver for App Drawer cache dynamic sync (Turkce karakter yok)
    private BroadcastReceiver globalPackageReceiver;

    public boolean isWidgetPanelOpen() {
        return isWidgetPanelOpen;
    }

	// Layout Mode: 0 = Normal, 1 = No Widgets, 2 = Full Screen
	private int layoutMode = 0;
	private int previousLayoutMode = -1;
	
	private net.osmand.plus.carlauncher.voice.VoiceVisualizerView voiceVisualizerView;
	private android.content.BroadcastReceiver voiceStateReceiver;

	private android.content.BroadcastReceiver musicDrawerReceiver = new android.content.BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			openMusicPlayer();
		}
	};

	private android.content.BroadcastReceiver antennaPanelReceiver = new android.content.BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS);
		}
	};

    private final StateChangedListener<Integer> mapScreenOrientationSettingListener = new StateChangedListener<Integer>() {
		@Override
		public void stateChanged(Integer change) {
			app.runInUIThread(() -> applyScreenOrientation());
		}
	};

	private final StateChangedListener<Boolean> useSystemScreenTimeoutListener = new StateChangedListener<Boolean>() {
		@Override
		public void stateChanged(Boolean change) {
			app.runInUIThread(() -> changeKeyguardFlags());
		}
	};

	private final StateChangedListener<Boolean> pinchZoomMagnificationListener = new StateChangedListener<Boolean>() {
		@Override
		public void stateChanged(Boolean enabled) {
			app.runInUIThread(() -> {
				OsmandMapTileView mapView = getMapView();
				mapView.setPinchZoomMagnificationEnabled(enabled);
			});
		}
	};
	private KeyEventHelper keyEventHelper;
	private RouteCalculationProgressListener routeCalculationProgressCallback;
	private TransportRouteCalculationProgressCallback transportRouteCalculationProgressCallback;
	private LoadSimulatedLocationsListener simulatedLocationsListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		net.osmand.plus.carlauncher.ui.CrashHandler.init(this);
		long time = System.currentTimeMillis();
		app.applyTheme(this);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(AndroidUiHelper.getScreenOrientation(this));
		net.osmand.plus.plugins.PluginsHelper.registerPlugin(new net.osmand.plus.carlauncher.antenna.AntennaPlugin(app));
		super.onCreate(savedInstanceState);

		// Orijinal dosyalara dokunmadan Plus ozelliklerini kalici olarak aktif et (Turkce karakter yok)
		app.getSettings().FULL_VERSION_PURCHASED.set(true);
		app.getSettings().LIVE_UPDATES_PURCHASED.set(true);
		app.getSettings().OSMAND_MAPS_PURCHASED.set(true);
		app.getSettings().OSMAND_PRO_PURCHASED.set(true);
		app.getSettings().CONTOUR_LINES_PURCHASED.set(true);
		app.getSettings().DEPTH_CONTOURS_PURCHASED.set(true);
		app.getSettings().BACKUP_PURCHASE_ACTIVE.set(true);

		// Navigasyonda seslerin duyulabilmesi icin varsayilan olarak muzik kesme ayarini aktif et (Turkce karakter yok)
		if (!app.getSettings().INTERRUPT_MUSIC.get()) {
			app.getSettings().INTERRUPT_MUSIC.set(true);
		}
        
        // V8: Initialize Car Launcher UI AFTER window setup
	    setupCarLauncherUI();


		// Car Launcher Specific Header
		net.osmand.plus.carlauncher.CarLauncherSettings carSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(
				this);
		if (carSettings.isLauncherEnabled()) {
			applyStatusBarVisibility();
		}

		lockHelper = app.getLockHelper();
		mapScrollHelper = new MapScrollHelper(app);
		keyEventHelper = app.getKeyEventHelper();
		restoreNavigationHelper = new RestoreNavigationHelper(app, this);

		getMapActions().setMapActivity(this);
		mapContextMenu.setMapActivity(this);
		mapRouteInfoMenu.setMapActivity(this);
		trackDetailsMenu.setMapActivity(this);

		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
				.registerReceiver(musicDrawerReceiver,
						new android.content.IntentFilter("net.osmand.carlauncher.OPEN_MUSIC_DRAWER"));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(musicDrawerReceiver,
					new android.content.IntentFilter("net.osmand.carlauncher.OPEN_MUSIC_DRAWER"),
					Context.RECEIVER_NOT_EXPORTED);
		} else {
			registerReceiver(musicDrawerReceiver,
					new android.content.IntentFilter("net.osmand.carlauncher.OPEN_MUSIC_DRAWER"));
		}

		// Register Antenna Panel Close Receiver (Turkce karakter yok)
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
				.registerReceiver(antennaPanelReceiver,
						new android.content.IntentFilter("net.osmand.carlauncher.CLOSE_ANTENNA_PANEL"));

		// Register globalPackageReceiver for App Drawer cache dynamic sync (Turkce karakter yok)
		globalPackageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				net.osmand.plus.carlauncher.ui.AppDrawerFragment.clearCache();
			}
		};
		IntentFilter packageFilter = new IntentFilter();
		packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		packageFilter.addDataScheme("package");
		registerReceiver(globalPackageReceiver, packageFilter);

		// CarLauncher: Direkt activity_car_launcher layout'unu set et
		// setupCarLauncherUI(); // Removed duplicate call (Already called in onCreate)

		// DEBUG: Verify CarLauncher Activity
		//android.widget.Toast.makeText(this, "CarLauncher MapActivity Active!", android.widget.Toast.LENGTH_LONG).show();

		enterToFullScreen();
		// Navigation Drawer
		AndroidUtils.addStatusBarPadding21v(this, findViewById(R.id.menuItems));

		View mapHudLayout = findViewById(R.id.map_hud_container);
		if (InsetsUtils.isEdgeToEdgeSupported()) {
			mapHudLayout.setFitsSystemWindows(false);
		}

		InsetsUtils.processInsets(this, findViewById(R.id.drawer_layout), null, false);

		if (WhatsNewDialogFragment.shouldShowDialog(app)) {
			boolean showed = WhatsNewDialogFragment.showInstance(getSupportFragmentManager());
			if (showed) {
				SecondSplashScreenFragment.SHOW = false;
			}
		}
		mapWidgetsVisibilityHelper = new WidgetsVisibilityHelper(this);
		dashboardOnMap.createDashboardView();
		extendedMapActivity = new ExtendedMapActivity();

		getMapActions().setMapActivity(this);
		getMapView().setMapActivity(this);
		getMapLayers().setMapActivity(this);

		intentHelper = new IntentHelper(this);
		intentHelper.parseLaunchIntents();

		OsmandMapTileView mapView = getMapView();

		mapView.setTrackBallDelegate(e -> {
			mapView.showAndHideMapPosition();
			return onTrackballEvent(e);
		});
		mapView.setAccessibilityActions(new MapAccessibilityActions(this));
		getMapViewTrackingUtilities().setMapView(mapView);
		getMapLayers().createAdditionalLayers(this);

		createProgressBarForRouting();
		updateStatusBarColor();

		if ((app.getRoutingHelper().isRouteCalculated() || app.getRoutingHelper().isRouteBeingCalculated())
				&& !app.getRoutingHelper().isRoutePlanningMode()
				&& !settings.FOLLOW_THE_ROUTE.get()
				&& app.getTargetPointsHelper().getAllPoints().size() > 0) {
			app.getRoutingHelper().clearCurrentRoute(null, new ArrayList<>());
			app.getTargetPointsHelper().removeAllWayPoints(false, false);
		}

		if (!settings.isLastKnownMapLocation()) {
			// show first time when application ran
			net.osmand.Location location = app.getLocationProvider().getFirstTimeRunDefaultLocation(loc -> {
				if (app.getLocationProvider().getLastKnownLocation() == null) {
					setMapInitialLatLon(getMapView(), loc);
				}
			});
			getMapViewTrackingUtilities().setMapLinkedToLocation(true);
			if (location != null) {
				setMapInitialLatLon(mapView, location);
			}
		}
		PluginsHelper.onMapActivityCreate(this);
		importHelper = app.getImportHelper();
		importHelper.setUiActivity(this);
		if (System.currentTimeMillis() - time > 50) {
			LOG.error("OnCreate for MapActivity took " + (System.currentTimeMillis() - time) + " ms");
		}
		mapView.refreshMap(true);

		drawerLayout = findViewById(R.id.drawer_layout);
		mapViewWithLayers = findViewById(R.id.map_view_with_layers);

		checkAppInitialization();

		getMapActions().updateDrawerMenu();

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		screenOffReceiver = new ScreenOffReceiver();
		registerReceiver(screenOffReceiver, filter);

		carFloatingButtonReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if ("net.osmand.carlauncher.ACTION_LAYOUT_TOGGLE".equals(action)) {
					onLayoutModeToggle();
				} else if ("net.osmand.carlauncher.ACTION_DESKTOP_TOGGLE".equals(action)) {
					onDesktopModeToggle();
				} else if ("net.osmand.carlauncher.ACTION_OPEN_SETTINGS".equals(action)) {
					openCarLauncherSettings();
				} else if ("net.osmand.carlauncher.NIGHT_DIM_CHANGED".equals(action)) {
					applyNightDimMode();
				} else if ("net.osmand.carlauncher.WIDGET_MODE_CHANGED".equals(action)) {
					applyWidgetPanelState();
				}
			}
		};
		IntentFilter floatFilter = new IntentFilter();
		floatFilter.addAction("net.osmand.carlauncher.ACTION_LAYOUT_TOGGLE");
		floatFilter.addAction("net.osmand.carlauncher.ACTION_DESKTOP_TOGGLE");
		floatFilter.addAction("net.osmand.carlauncher.ACTION_OPEN_SETTINGS");
		floatFilter.addAction("net.osmand.carlauncher.NIGHT_DIM_CHANGED");
		floatFilter.addAction("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(carFloatingButtonReceiver, floatFilter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			registerReceiver(carFloatingButtonReceiver, floatFilter);
		}

		app.getAidlApi().onCreateMapActivity(this);

		lockHelper.setLockUIAdapter(this);
		keyEventHelper.setMapActivity(this);
		mIsDestroyed = false;
		if (mapViewWithLayers != null) {
			mapViewWithLayers.onCreate(savedInstanceState);
		}
		extendedMapActivity.onCreate(this, savedInstanceState);

		// CarLauncher: Listen for settings changes via backstack
		getSupportFragmentManager().addOnBackStackChangedListener(() -> {
			if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
				// Returned to main screen -> Refresh UI
				applyWidgetPanelState();
			}
		});
	}

	protected int getRootViewId() {
		return R.id.drawer_layout;
	}

	@Override
	@Nullable
	public <T extends View> T findViewById(int id) {
		// İlk önce CarLauncher layout'unu set et
		T view = super.findViewById(id);
		if (view != null) {
			return view;
		}

		// Bulunamadıysa main.xml içinde ara (nested)
		if (mainLayoutRoot != null) {
			return mainLayoutRoot.findViewById(id);
		}

		return null;
	}

	private void setupCarLauncherUI() {
		// 1. Önce CarLauncher layout'unu set et (tek setContentView)
		setContentView(R.layout.activity_car_launcher);

		// 2. CarLauncher view'larını bul
		rootLayout = findViewById(R.id.root_layout);
		mapContainer = findViewById(R.id.map_container);
		widgetPanel = findViewById(R.id.widget_panel);
		widgetHandle = findViewById(R.id.widget_handle);
		appDock = findViewById(R.id.app_dock);
		appDrawerContainer = findViewById(R.id.app_drawer_container);

		// Panellerin yuvarlak koselerini zorla aktif et (Turkce karakter yok)
		if (widgetPanel != null) {
			widgetPanel.setBackgroundResource(R.drawable.bg_panel_rounded);
			widgetPanel.setClipToOutline(true);
		}
		if (mapContainer != null) {
			mapContainer.setBackgroundResource(R.drawable.bg_card_rounded_dark);
			mapContainer.setClipToOutline(true);
		}

		// --- VOICE VISUALIZER UI SETUP ---
		voiceVisualizerView = new net.osmand.plus.carlauncher.voice.VoiceVisualizerView(this);
		voiceVisualizerView.setId(View.generateViewId()); // ConstraintLayout cökmesini önlemek icin
		voiceVisualizerView.setVisibility(View.GONE);
		android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
				android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
				300
		);
		lp.gravity = android.view.Gravity.BOTTOM;
		lp.bottomMargin = 220; // Dock'un uzerinde gorunmesi icin margin
		if (rootLayout instanceof android.view.ViewGroup) {
			((android.view.ViewGroup) rootLayout).addView(voiceVisualizerView, lp);
		}

		voiceStateReceiver = new android.content.BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String state = intent.getStringExtra("state");
				if ("LISTENING".equals(state)) {
					voiceVisualizerView.startListening();
				} else if ("PROCESSING".equals(state)) {
					voiceVisualizerView.startProcessing();
				} else if ("CLOSED".equals(state)) {
					voiceVisualizerView.stop();
				}
			}
		};
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(voiceStateReceiver,
					new android.content.IntentFilter("net.osmand.plus.carlauncher.VOICE_STATE"),
					Context.RECEIVER_NOT_EXPORTED);
		} else {
			registerReceiver(voiceStateReceiver,
					new android.content.IntentFilter("net.osmand.plus.carlauncher.VOICE_STATE"));
		}
		// --- END VOICE VISUALIZER UI SETUP ---
		
		// Initialize Layout Manager & Panel Content Manager
		carLayoutManager = new net.osmand.plus.carlauncher.ui.CarLayoutManager(this);
		panelContentManager = new net.osmand.plus.carlauncher.ui.PanelContentManager(
				getSupportFragmentManager(), R.id.widget_panel);
		panelContentManager.setOnFullScreenStateChangeListener(isFullScreen -> {
			carLayoutManager.setContentFullScreen(isFullScreen);
			applyWidgetPanelState();
			if (mapContainer != null) {
				mapContainer.setInterceptTouch(isFullScreen, () -> closeAppDrawer());
			}
		});
		
		if (widgetHandle != null) {
		    widgetHandle.bringToFront(); // Force Top Z-Order
		    widgetHandle.setImageResource(net.osmand.plus.R.drawable.ic_more_vert);
		    widgetHandle.setColorFilter(0xCCFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
		    widgetHandle.setVisibility(View.VISIBLE); // Baslangicta dogrudan aktif et (Turkce karakter yok)
		    
		    widgetHandle.setOnTouchListener(new View.OnTouchListener() {
		        private float initialTouchX;
		        private float initialTouchY;
		        private boolean isDragging = false;
		        private static final int TOUCH_SLOP = 10; // Surukleme esigi

		        @Override
		        public boolean onTouch(View v, MotionEvent event) {
		            switch (event.getAction()) {
		                case MotionEvent.ACTION_DOWN:
		                    initialTouchX = event.getRawX();
		                    initialTouchY = event.getRawY();
		                    isDragging = false;
		                    break;
		                case MotionEvent.ACTION_MOVE:
		                    float dx = event.getRawX() - initialTouchX;
		                    float dy = event.getRawY() - initialTouchY;
		                    if (!isDragging && (Math.abs(dx) > TOUCH_SLOP || Math.abs(dy) > TOUCH_SLOP)) {
		                        isDragging = true;
		                    }
		                    if (isDragging) {
		                        updateCarWidgetPanelSize(event.getRawX(), event.getRawY());
		                    }
		                    break;
		                case MotionEvent.ACTION_UP:
		                case MotionEvent.ACTION_CANCEL:
		                    // Surukleme modu yalnizca surukleyince calisacak, performClick veya swap olmayacak
		                    break;
		            }
		            return true;
		        }
		    });
		}
		
		applyWidgetPanelState();



		// 3. Orijinal main.xml layout'unu inflate et ve referansını sakla
		mainLayoutRoot = getLayoutInflater().inflate(R.layout.main, mapContainer, false);

		// 4. main.xml'i map_container'a ekle
		mapContainer.addView(mainLayoutRoot);

		// Instant Shell: Harita yukleme placeholder'ini ekle
		final View mapLoadingPlaceholder = getLayoutInflater().inflate(R.layout.layout_map_loading_placeholder, mapContainer, false);
		if (mapContainer != null) {
			mapContainer.addView(mapLoadingPlaceholder);
		}

		if (net.osmand.plus.carlauncher.ui.CarLauncherInitManager.getInstance().isCoreReady()) {
			if (mapLoadingPlaceholder != null) mapLoadingPlaceholder.setVisibility(View.GONE);
		} else {
			net.osmand.plus.carlauncher.ui.CarLauncherInitManager.getInstance().addListener(() -> {
				if (mapLoadingPlaceholder != null) {
					mapLoadingPlaceholder.animate()
							.alpha(0f)
							.setDuration(500)
							.withEndAction(() -> mapLoadingPlaceholder.setVisibility(View.GONE))
							.start();
				}
			});
		}

		// Harita kucuk paneldeyken dokunmalari engellemek ve tiklayinca buyutmek icin
		if (mapContainer != null && carLayoutManager != null) {
			mapContainer.setInterceptTouch(carLayoutManager.isContentFullScreen(), () -> closeAppDrawer());
		}


		// 5. CarLauncher bileşenlerini başlat
		embedWidgetPanel();
		embedAppDock();

		// 6. Check for permissions
		checkOverlayPermission();

		// Gece karartma overlay kontrolu (Turkce karakter yok)
		applyNightDimMode();

		// Status Bar gorunurluk ayarini uygula (Turkce karakter yok)
		applyStatusBarVisibility();
	}

	private void updateCarWidgetPanelSize(float rawX, float rawY) {
		if (carLayoutManager == null) return;
		
		boolean isPortrait = getResources().getConfiguration().orientation 
				== android.content.res.Configuration.ORIENTATION_PORTRAIT;
				
		net.osmand.plus.carlauncher.CarLauncherSettings carSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(this);
		
		if (isPortrait) {
			int screenHeight = getResources().getDisplayMetrics().heightPixels;
			if (screenHeight <= 0) return;
			
			float rawPercent = (screenHeight - rawY) / (float) screenHeight;
			// %25 ile %50 arasinda sinirla, pürüzsüz kaydir (Turkce karakter yok)
			float percent = Math.max(0.25f, Math.min(0.50f, rawPercent));
			
			carSettings.setWidgetPanelHeightPortrait(percent);
		} else {
			int screenWidth = getResources().getDisplayMetrics().widthPixels;
			if (screenWidth <= 0) return;
			
			String widgetPos = carSettings.getWidgetPanelPosition();
			boolean isLeft = "left".equals(widgetPos);
			
			float rawPercent;
			if (isLeft) {
				rawPercent = rawX / (float) screenWidth;
			} else {
				rawPercent = (screenWidth - rawX) / (float) screenWidth;
			}
			
			// %25 ile %50 arasinda sinirla, pürüzsüz kaydir (Turkce karakter yok)
			float percent = Math.max(0.25f, Math.min(0.50f, rawPercent));
			
			carSettings.setWidgetPanelWidthPercent(percent);
		}
		
		applyWidgetPanelState();
	}

	public void applyNightDimMode() {
		try {
			View nightDimOverlay = findViewById(R.id.night_dim_overlay);
			if (nightDimOverlay != null) {
				boolean isNight = false;
				net.osmand.plus.carlauncher.CarLauncherSettings clSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(this);
				String mode = clSettings.getNightDimMode();
				
				if ("osmand".equals(mode)) {
					net.osmand.plus.helpers.DayNightHelper helper = app.getDaynightHelper();
					if (helper != null) {
						isNight = helper.isNightMode(net.osmand.plus.settings.enums.ThemeUsageContext.APP);
					}
				} else if ("auto".equals(mode)) {
					// Zamana gore otomatik mod (19:00 - 07:00 arasi gece)
					int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
					isNight = (hour >= 19 || hour < 7);
				} else if ("day".equals(mode)) {
					isNight = false; // Her zaman gunduz (pasif)
				} else if ("night".equals(mode)) {
					isNight = true; // Her zaman gece (aktif)
				}
				
				if (isNight) {
					if (nightDimOverlay.getVisibility() != View.VISIBLE) {
						nightDimOverlay.setVisibility(View.VISIBLE);
						nightDimOverlay.setAlpha(0f);
						nightDimOverlay.animate().alpha(1f).setDuration(400).start();
					}
				} else {
					if (nightDimOverlay.getVisibility() == View.VISIBLE) {
						nightDimOverlay.animate().alpha(0f).setDuration(400).withEndAction(new Runnable() {
							@Override
							public void run() {
								nightDimOverlay.setVisibility(View.GONE);
							}
						}).start();
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}
	}

	private void embedWidgetPanel() {
		if (widgetPanel != null && panelContentManager != null) {
			net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent contentToRestore = 
				(lastPanelContent != null) ? lastPanelContent : net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS;
			panelContentManager.setContent(contentToRestore);
		}
	}

	private void embedAppDock() {
		if (appDock != null) {
			net.osmand.plus.carlauncher.ui.AppDockFragment appDockFragment = new net.osmand.plus.carlauncher.ui.AppDockFragment();
			getSupportFragmentManager().beginTransaction()
					.replace(appDock.getId(), appDockFragment, "app_dock")
					.commitAllowingStateLoss();
		}
	}
	
	public void toggleLayoutMode() {
	    // Cycle: 0 (Normal) -> 1 (No Widgets) -> 2 (Full Screen) -> 0
	    layoutMode = (layoutMode + 1) % 3;
	    
	    // Map layoutMode to isWidgetPanelOpen
	    // Mode 0 = panel açık, Mode 1/2 = panel kapalı
	    isWidgetPanelOpen = (layoutMode == 0);
	    applyWidgetPanelState();
	}
	
	public net.osmand.plus.carlauncher.ui.AppDockFragment getAppDockFragment() {
	    androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag("app_dock");
	    if (f instanceof net.osmand.plus.carlauncher.ui.AppDockFragment) {
	        return (net.osmand.plus.carlauncher.ui.AppDockFragment) f;
	    }
	    return null;
	}
	
	public void updateWidgetPanelMode() {
	    applyWidgetPanelState();
	}

    private void applyWidgetPanelState() {
        if (carLayoutManager != null) {
            if (rootLayout != null) {
                if (rootLayout.isAttachedToWindow()) {
                    isTransitioning = true;
                    
                    // Guvenlik onlemi: Gecis animasyonu tamamlanmazsa kilidi 500ms sonra otomatik ac (Turkce karakter yok)
                    rootLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isTransitioning = false;
                        }
                    }, 500);

                    AutoTransition transition = new AutoTransition();
                    transition.addListener(new Transition.TransitionListener() {
                        @Override
                        public void onTransitionStart(Transition transition) {}

                        @Override
                        public void onTransitionEnd(Transition transition) {
                            isTransitioning = false;
                        }

                        @Override
                        public void onTransitionCancel(Transition transition) {
                            isTransitioning = false;
                        }

                        @Override
                        public void onTransitionPause(Transition transition) {}

                        @Override
                        public void onTransitionResume(Transition transition) {}
                    });
                    TransitionManager.beginDelayedTransition(rootLayout, transition);
                } else {
                    isTransitioning = false;
                }
            }
            carLayoutManager.applyLayout(isWidgetPanelOpen, layoutMode);

            // Harita kucuk ekrana gectiginde butonlari otomatik gizle
            View mapHudContainer = findViewById(R.id.map_hud_container);
            if (mapHudContainer != null) {
                boolean isMapMinimized = isWidgetPanelOpen && carLayoutManager.isContentFullScreen();
                mapHudContainer.setVisibility(isMapMinimized ? View.GONE : View.VISIBLE);
            }
        }
        // Yüzen buton yöneticisine tam ekran harita modunu bildir (Türkçe karakter yok)
        boolean isFull = (layoutMode == 2);
        net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).setFullScreenMap(isFull);

        // WidgetPanelFragment'a gorunurluk durumunu bildir (Turkce karakter yok)
        Fragment panelFragment = getSupportFragmentManager().findFragmentByTag(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.DESKTOP.name());
        if (panelFragment instanceof net.osmand.plus.carlauncher.ui.WidgetPanelFragment) {
            ((net.osmand.plus.carlauncher.ui.WidgetPanelFragment) panelFragment).onPanelVisibilityChanged(isWidgetPanelOpen);
        }
    }

    public void checkAndRefreshDockFragmentIfNeeded() {
        net.osmand.plus.carlauncher.ui.AppDockFragment dock = getAppDockFragment();
        if (dock != null && dock.needsLayoutUpdate()) {
            net.osmand.plus.carlauncher.ui.AppDockFragment newDock = new net.osmand.plus.carlauncher.ui.AppDockFragment();
            getSupportFragmentManager().beginTransaction()
                .replace(net.osmand.plus.R.id.app_dock, newDock, "app_dock")
                .commitAllowingStateLoss();
        }
    }

	@Override
	public void onLayoutModeToggle() {
		if (isTransitioning) {
			// Animasyon devam ederken yeni tiklamalari engelle (Turkce karakter yok)
			return;
		}
		// Toggle Logic: Normal (0) -> Full Screen (2) (for API compatibility)
		layoutMode = (layoutMode == 0) ? 2 : 0;
		isWidgetPanelOpen = (layoutMode == 0);
		
		updateLayoutMode();
	}

	private void updateLayoutMode() {
		// Yüzen buton yöneticisine tam ekran harita modunu bildir (Türkçe karakter yok)
		boolean isFull = (layoutMode == 2);
		net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).setFullScreenMap(isFull);

		// Delegate Widget Panel Visibility & Constraints to helper
		applyWidgetPanelState();
		
		switch (layoutMode) {
			case 0: // Normal
				if (appDock != null) appDock.setVisibility(View.VISIBLE);
				break;
			case 1: // No Widgets
				if (appDock != null) appDock.setVisibility(View.VISIBLE);
				break;
			case 2: // Full Screen
				if (appDock != null) appDock.setVisibility(View.VISIBLE);
				break;
		}

        // Update App Dock Icon
        androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentByTag("app_dock");
        if (fragment instanceof net.osmand.plus.carlauncher.ui.AppDockFragment) {
            ((net.osmand.plus.carlauncher.ui.AppDockFragment) fragment).updateLayoutIcon(layoutMode);
        }
	}

	public boolean isDesktopMode() {
		return isDesktopMode;
	}

	public int getLayoutMode() {
		return layoutMode;
	}

	@Override
	public void onDesktopModeToggle() {
		if (isTransitioning) {
			// Animasyon devam ederken yeni tiklamalari engelle (Turkce karakter yok)
			return;
		}

		// YENI: Eger panelde Muzik, Uygulamalar vb. aciksa bu butonu "Geri Don" olarak kullan
		if (panelContentManager != null) {
			net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent current = panelContentManager.getCurrentContent();
			if (current != net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS && 
				current != net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.DESKTOP) {
				
				panelContentManager.setContent(isDesktopMode ? 
					net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.DESKTOP : 
					net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS);
				return;
			}
		}

		if (!isDesktopMode && layoutMode == 0) {
			// Normal -> Full Harita
			layoutMode = 2;
			isWidgetPanelOpen = false;
			updateLayoutMode();
		} else if (!isDesktopMode && layoutMode == 2) {
			// Full Harita -> Desktop Mode
			layoutMode = 0;
			isWidgetPanelOpen = true;
			updateLayoutMode();
			setDesktopMode(true);
		} else {
			// Desktop Mode -> Normal
			setDesktopMode(false);
			layoutMode = 0;
			isWidgetPanelOpen = true;
			updateLayoutMode();
		}
	}

	public void setDesktopMode(boolean active) {
		if (this.isDesktopMode == active) return;
		this.isDesktopMode = active;

		if (active) {
			// Masaustu modunu acarken WidgetPanelFragment (DESKTOP) icerigini yukle
			if (panelContentManager != null) {
				panelContentManager.setContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.DESKTOP);
			}
		} else {
			// Pasif yaparken varsayilan premium birlesik paneli (WIDGETS) geri yukle
			if (panelContentManager != null) {
				panelContentManager.setContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS);
			}
		}

		// Yerlesimi guncelle (haritayi gizle, widget panelini tam ekran yap)
		applyWidgetPanelState();

		// App Dock uzerindeki buton aktif/pasif durumunu guncelle
		net.osmand.plus.carlauncher.ui.AppDockFragment dock = getAppDockFragment();
		if (dock != null) {
			dock.updateDesktopModeState(active);
		}
	}

	@Override
	public void onAppDrawerOpen() {
		openAppDrawer();
	}

	public void openAppDrawer() {
		// Uygulama cekmecesini sag panel swap kullanarak buyuk ekranda ac/kapat (toggle)
		if (panelContentManager != null && panelContentManager.getCurrentContent() == net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.APP_DRAWER) {
			closeAppDrawer();
		} else {
			setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.APP_DRAWER);
		}
	}

	public void openWeatherDashboard() {
		// Hava durumu panelini sag panel swap kullanarak buyuk ekranda ac
		setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WEATHER);
	}

	public void closeAppDrawer() {
		// Tum buyuk panelleri kapatip varsayilan premium birlesik panele geri don
		setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS);
	}

	// Permission Check for Overlay (Widget updates in background etc)
	public void checkOverlayPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!android.provider.Settings.canDrawOverlays(this)) {
				new android.app.AlertDialog.Builder(this)
						.setTitle("Izin Gerekli")
						.setMessage(
								"Widgetlerin duzgun calismasi icin 'Diger uygulamalarin uzerinde goster' iznine ihtiyac var.")
						.setPositiveButton("Ayarlar", (dialog, which) -> {
							try {
								Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
										Uri.parse("package:" + getPackageName()));
								startActivity(intent);
							} catch (Exception e) {
								LOG.error("Failed to open overlay settings", e);
							}
						})
						.setNegativeButton("Iptal", null)
						.show();
			} else {
				// If overlay is granted, check notification access
				checkNotificationPermission();
			}
		}
	}

	public void checkNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			String enabledListeners = android.provider.Settings.Secure.getString(getContentResolver(),
					"enabled_notification_listeners");
			ComponentName myListener = new ComponentName(getPackageName(),
					"net.osmand.plus.carlauncher.MediaNotificationListener");
			boolean isEnabled = enabledListeners != null && enabledListeners.contains(myListener.flattenToString());

			if (!isEnabled) {
				new android.app.AlertDialog.Builder(this)
						.setTitle("Medya Kontrol Izni")
						.setMessage(
								"Muzik kontrolunun calismasi icin 'Bildirim Erisim' izni gereklidir. OsmAnd Media Control secenegini aktif edin.")
						.setPositiveButton("Ayarlar", (dialog, which) -> {
							try {
								Intent intent = new Intent(
										android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
								startActivity(intent);
							} catch (Exception e) {
								LOG.error("Failed to open notification settings", e);
							}
						})
						.setNegativeButton("Iptal", null)
						.show();
			}
		}
	}

	@Override
	public void setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent content) {
		if (isTransitioning) {
			return; // Animasyon devam ederken icerik degisimini engelle (Turkce karakter yok)
		}
		if (panelContentManager != null) {
			// Eger DESKTOP disinda bir sey aciliyorsa Masaustu modundan cik
			if (content != net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.DESKTOP && isDesktopMode) {
				isDesktopMode = false;
				applyWidgetPanelState();
				net.osmand.plus.carlauncher.ui.AppDockFragment dock = getAppDockFragment();
				if (dock != null) {
					dock.updateDesktopModeState(false);
				}
			}

			// KULLANICI ISTEGI: Eger Map Full Screen (layoutMode != 0) ise ve WIDGETS disinda bir panel aciliyorsa
			if (content != net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS && content != net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.DESKTOP) {
				if (layoutMode != 0) {
					previousLayoutMode = layoutMode;
					layoutMode = 0;
					isWidgetPanelOpen = true;
					updateLayoutMode();
				}
			} else {
				// WIDGETS (Veya DESKTOP) aciliyorsa (Yani diger paneller kapaniyorsa)
				if (previousLayoutMode != -1) {
					layoutMode = previousLayoutMode;
					isWidgetPanelOpen = (layoutMode == 0);
					previousLayoutMode = -1;
					updateLayoutMode();
				}
			}

			panelContentManager.setContent(content);
			// Son basarili panel icerigini statik olarak sakla (Turkce karakter yok)
			lastPanelContent = content;
		}
	}

	public net.osmand.plus.carlauncher.ui.PanelContentManager getPanelContentManager() {
		return panelContentManager;
	}

	public void openMusicPlayer() {
		// Muzik oynaticiyi sag panel swap kullanarak buyuk ekranda ac
		setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.MUSIC);
	}

	public void openCarLauncherSettings() {
		if (panelContentManager != null) {
			if (panelContentManager.getCurrentContent() == net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.SETTINGS) {
				setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS);
			} else {
				setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.SETTINGS);
			}
		}
	}

	@Override
	public void openAntennaAlignmentInPanel() {
		setPanelContent(net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.ANTENNA);
	}

	@Override
	public void openAntennaAlignmentFullscreen() {
		try {
			android.content.Intent intent = new android.content.Intent(this,
					net.osmand.plus.carlauncher.antenna.AntennaAlignmentActivity.class);
			intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} catch (Exception e) {
			android.widget.Toast.makeText(this, "Hizalama ekrani baslatilirken hata.", android.widget.Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = new InsetTargetsCollection();
		collection.add(InsetTarget.createCustomBuilder(R.id.menuItems)
				.portraitSides(InsetSide.TOP, InsetSide.BOTTOM).landscapeSides(InsetSide.TOP)
				.applyPadding(true).build());
		collection.add(InsetTarget.createLeftSideContainer(true, true, R.id.menuItems));

		View dashboardView = findViewById(R.id.dashboard);
		if (dashboardView != null) {
			ObservableScrollView scrollView = dashboardView.findViewById(R.id.main_scroll);
			collection.add(InsetTarget.createLeftSideContainer(false, true, dashboardView));
			collection.add(InsetTarget.createLeftSideContainer(true, true, scrollView));
			collection.add(InsetTarget.createLeftSideContainer(true, false, R.id.dashboard_content_container));
			collection.add(InsetTarget.createScrollable(scrollView).landscapeSides().build());
		}

		return collection;
	}

	@Override
	public void onApplyInsets(@NonNull WindowInsetsCompat insets) {
		super.onApplyInsets(insets);
		getMapLayers().setWindowInsets(insets);
	}

	private void setMapInitialLatLon(@NonNull OsmandMapTileView mapView, @Nullable Location location) {
		if (location != null) {
			mapView.setLatLon(location.getLatitude(), location.getLongitude());
			mapView.setIntZoom(14);
		}
	}

	public void exitFromFullScreen(View view) {
		if (!PluginsHelper.isDevelopment() || settings.TRANSPARENT_STATUS_BAR.get()) {
			AndroidUtils.exitFromFullScreen(this, view);
		}
	}

	public void enterToFullScreen() {
		net.osmand.plus.carlauncher.CarLauncherSettings carSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(this);
		if (carSettings.isLauncherEnabled() && carSettings.isStatusBarVisible()) {
			// Status bar gorunur olmali, enterToFullScreen'in status bar'i gizlemesini engelle (Turkce karakter yok)
			return;
		}
		if (!PluginsHelper.isDevelopment() || settings.TRANSPARENT_STATUS_BAR.get()) {
			AndroidUtils.enterToFullScreen(this, getLayout());
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (fragmentsHelper.removeFragment(PlanRouteFragment.TAG)) {
			app.getMapMarkersHelper().getPlanRouteContext().setFragmentVisible(true);
		}
		fragmentsHelper.removeFragment(ImportGpxBottomSheetDialogFragment.TAG);
		fragmentsHelper.removeFragment(AdditionalActionsBottomSheetDialogFragment.TAG);
		extendedMapActivity.onSaveInstanceState(this, outState);
		super.onSaveInstanceState(outState);
	}

	private void checkAppInitialization() {
		net.osmand.plus.carlauncher.CarLauncherSettings carSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(this);
		boolean fastBoot = !carSettings.isLauncherEnabled() || carSettings.isFastBootEnabled();

		net.osmand.plus.carlauncher.ui.CarLauncherInitManager.getInstance().startInitTimer();

		if (app.isApplicationInitializing()) {
			View initProgress = findViewById(R.id.init_progress);
			if (initProgress != null) {
				initProgress.setVisibility(fastBoot ? View.GONE : View.VISIBLE);
			}

			initListener = new MapAppInitializeListener(this) {
				@Override
				public void onFinish(@NonNull AppInitializer init) {
					super.onFinish(init);
					net.osmand.plus.carlauncher.ui.CarLauncherInitManager.getInstance().markCoreReady(MapActivity.this);
				}
			};
			app.checkApplicationIsBeingInitialized(initListener);
		} else {
			net.osmand.plus.carlauncher.ui.CarLauncherInitManager.getInstance().markCoreReady(MapActivity.this);
			app.getOsmandMap().setupRenderingView();
			restoreNavigationHelper.checkRestoreRoutingMode();
		}
	}


	private void createProgressBarForRouting() {
		routeCalculationProgressCallback = new MapRouteCalculationProgressListener(this);

		app.getRoutingHelper().addCalculationProgressListener(routeCalculationProgressCallback);

		transportRouteCalculationProgressCallback = new TransportRouteCalculationProgressCallback() {
			@Override
			public void start() {
				if (routeCalculationProgressCallback != null) {
					routeCalculationProgressCallback.onCalculationStart();
				}
			}

			@Override
			public void updateProgress(int progress) {
				if (routeCalculationProgressCallback != null) {
					routeCalculationProgressCallback.onUpdateCalculationProgress(progress);
				}
			}

			@Override
			public void finish() {
				if (routeCalculationProgressCallback != null) {
					routeCalculationProgressCallback.onCalculationFinish();
				}
			}
		};
		app.getTransportRoutingHelper().setProgressBar(transportRouteCalculationProgressCallback);

		simulatedLocationsListener = new LoadSimulatedLocationsListener() {
			@Override
			public void onLocationsStartedLoading() {
				updateProgress(true);
			}

			@Override
			public void onLocationsLoadingProgress(int progress) {
				if (!isRouteBeingCalculated()) {
					updateProgress(progress);
				}
			}

			@Override
			public void onLocationsLoaded(@Nullable List<SimulatedLocation> locations) {
				updateProgress(false);
			}
		};
		app.getLocationProvider().getLocationSimulation().addListener(simulatedLocationsListener);
	}

	private void destroyProgressBarForRouting() {
		app.getLocationProvider().getLocationSimulation().removeListener(simulatedLocationsListener);
		simulatedLocationsListener = null;
		app.getTransportRoutingHelper().setProgressBar(null);
		transportRouteCalculationProgressCallback = null;
		app.getRoutingHelper().removeCalculationProgressListener(routeCalculationProgressCallback);
		routeCalculationProgressCallback = null;
	}

	public void updateProgress(boolean visible) {
		app.runInUIThread(() -> {
			if (!isRouteBeingCalculated()) {
				AndroidUiHelper.updateVisibility(findViewById(R.id.map_horizontal_progress), visible);
			}
		});
	}

	public void updateProgress(int progress) {
		ProgressBar progressBar = findViewById(R.id.map_horizontal_progress);
		if (findViewById(R.id.map_hud_layout).getVisibility() == View.VISIBLE) {
			if (mapRouteInfoMenu.isVisible() || dashboardOnMap.isVisible() || isOnlineRoutingWithApproximation()) {
				AndroidUiHelper.updateVisibility(progressBar, false);
				return;
			}
			if (progressBar.getVisibility() == View.GONE) {
				AndroidUiHelper.updateVisibility(progressBar, true);
			}
			progressBar.setProgress(progress);
			progressBar.invalidate();
			progressBar.requestLayout();
		}
	}

	public boolean isOnlineRoutingWithApproximation() {
		ApplicationMode mode = getRoutingHelper().getAppMode();
		if (mode != null && mode.getRouteService() == RouteService.ONLINE) {
			OnlineRoutingEngine engine = app.getOnlineRoutingHelper().getEngineByKey(mode.getRoutingProfile());
			return engine != null
					? engine.isOnlineEngineWithApproximation()
					: app.getOnlineRoutingHelper().wasOnlineEngineWithApproximationUsed();
		}
		return false;
	}

	private boolean isRouteBeingCalculated() {
		return app.getRoutingHelper().isRouteBeingCalculated()
				|| app.getTransportRoutingHelper().isRouteBeingCalculated();
	}

	public void setupRouteCalculationProgressBar(@NonNull ProgressBar pb) {
		RoutingHelper routingHelper = getRoutingHelper();
		setupProgressBar(pb, routingHelper.isPublicTransportMode() || !routingHelper.isOsmandRouting());
	}

	public void setupProgressBar(@NonNull ProgressBar pb, boolean indeterminate) {
		DayNightHelper dayNightHelper = app.getDaynightHelper();

		boolean nightMode = dayNightHelper.isNightMode(OVER_MAP);
		boolean useRouteLineColor = nightMode == dayNightHelper.isNightMode(ThemeUsageContext.MAP);

		int bgColorId = nightMode ? R.color.map_progress_bar_bg_dark : R.color.map_progress_bar_bg_light;
		int bgColor = ContextCompat.getColor(this, bgColorId);

		int progressColor = useRouteLineColor
				? getMapLayers().getRouteLayer().getRouteLineColor(nightMode)
				: ContextCompat.getColor(this, R.color.active_color_primary_light);

		pb.setProgressDrawable(AndroidUtils.createProgressDrawable(bgColor, progressColor));
		pb.setIndeterminate(indeterminate);
		pb.getIndeterminateDrawable().setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
	}

	public ImportHelper getImportHelper() {
		return importHelper;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		// Home intent'i geldiyse, gereksiz launch/content parsing islemlerini atlayarak
		// activity'nin yenilenmesini (yeniden basliyor gibi davranmasini) engelleyelim.
		if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_HOME)) {
			return;
		}

		importHelper.setUiActivity(this);
		if (!intentHelper.parseLaunchIntents()) {
			intentHelper.parseContentIntent();
		}
	}

	@Override
	public void startActivity(Intent intent) {
		clearPrevActivityIntent();
		super.startActivity(intent);
	}

    @Override
    public void onBackPressed() {
        if (dashboardOnMap.onBackPressed()) {
            return;
        }


		if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
			closeDrawer();
			return;
		}
		if (getMapLayers().getContextMenuLayer().isInAddGpxPointMode()) {
			quitAddGpxPointMode();
		}
		int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
		if (backStackEntryCount == 0 && launchPrevActivityIntent()) {
			return;
		}
		ExplorePlacesFragment explorePlacesFragment = fragmentsHelper.getExplorePlacesFragment();
		if (explorePlacesFragment != null) {
			if (!explorePlacesFragment.onBackPress()) {
				fragmentsHelper.closeExplore();
				fragmentsHelper.showQuickSearch(CURRENT, false);
			}
			return;
		}
		QuickSearchDialogFragment quickSearchFragment = fragmentsHelper.getQuickSearchDialogFragment();
		if ((backStackEntryCount == 0 || mapContextMenu.isVisible()) && quickSearchFragment != null
				&& quickSearchFragment.isSearchHidden()) {
			fragmentsHelper.showQuickSearch(ShowQuickSearchMode.CURRENT, false);
			return;
		}
		if (mapContextMenu.isVisible()) {
			MenuController menuController = mapContextMenu.getMenuController();
			if (menuController != null && menuController.hasBackAction()) {
				mapContextMenu.backToolbarAction(menuController);
				return;
			}
		}
		if (panelContentManager != null && panelContentManager.getCurrentContent() != net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS) {
			closeAppDrawer();
			return;
		}

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }


        if (appDrawerContainer != null && appDrawerContainer.getVisibility() == View.VISIBLE) {
            closeAppDrawer();
            return;
        }

		// If at standard Map state (no drawers/menus), do nothing (block exit)
		// super.onBackPressed();
	}

	public boolean launchPrevActivityIntent() {
		if (prevActivityIntent != null) {
			prevActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			AndroidUtils.startActivityIfSafe(this, prevActivityIntent);
			prevActivityIntent = null;
			return true;
		}
		return false;
	}

	private void quitAddGpxPointMode() {
		getMapLayers().getContextMenuLayer().getAddGpxPointBottomSheetHelper().hide();
		getMapLayers().getContextMenuLayer().quitAddGpxPoint();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		intentHelper.parseLaunchIntents();
	}

	@Override
	protected void onResume() {
		super.onResume();
		applyNightDimMode();
		hideSystemUI();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			CarLauncherSettings carSettings = CarLauncherSettings.getInstance(this);
			if (carSettings.isFloatingButtonEnabled() && !android.provider.Settings.canDrawOverlays(this)) {
				Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
						Uri.parse("package:" + getPackageName()));
				startActivity(intent);
				android.widget.Toast.makeText(this, "Yuzen buton icin diger uygulamalarin uzerinde gorunme izni vermelisiniz.", android.widget.Toast.LENGTH_LONG).show();
			}
		}
		MapActivity mapViewMapActivity = getMapView().getMapActivity();
		if (mapViewMapActivity != null && mapViewMapActivity != this) {
			// A newer MapActivity owns the shared map. Recreating this stale instance
			// causes two launcher tasks to continuously replace each other.
			finish();
			return;
		}
		if (activityRestartNeeded || !getMapLayers().hasMapActivity()) {
			activityRestartNeeded = false;
			recreate();
			return;
		}
		net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).setAppInForeground(true);
		net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).updateButtonState();
		importHelper.setUiActivity(this);
		app.getLocationProvider().ensureLatestLocation();

		long time = System.currentTimeMillis();
		FragmentManager fragmentManager = getSupportFragmentManager();

		if (app.getMapMarkersHelper().getPlanRouteContext().isFragmentVisible()) {
			PlanRouteFragment.showInstance(this, null);
		}

		if (app.isApplicationInitializing() || DashboardOnMap.staticVisible) {
			if (!dashboardOnMap.isVisible() && settings.SHOW_DASHBOARD_ON_START.get()) {
				dashboardOnMap.setDashboardVisibility(true, DashboardOnMap.staticVisibleType);
			}
		}
		dashboardOnMap.updateLocation(true, true, false);

		if (!dashboardOnMap.isVisible()) {
			if (RenderInitErrorBottomSheet.shouldShow(app)) {
				SecondSplashScreenFragment.SHOW = false;
				RenderInitErrorBottomSheet.showInstance(fragmentManager);
			} else if (CrashBottomSheetDialogFragment.shouldShow(settings, this)) {
				SecondSplashScreenFragment.SHOW = false;
				CrashBottomSheetDialogFragment.showInstance(fragmentManager);
			} else if (RateUsHelper.shouldShowRateDialog(app)) {
				SecondSplashScreenFragment.SHOW = false;
				RateUsHelper.showRateDialog(this);
			} else if (TripltekPromoFragment.shouldShow(app)) {
				SecondSplashScreenFragment.SHOW = false;
				TripltekPromoFragment.showInstance(fragmentManager);
			} else if (HugerockPromoFragment.shouldShow(app)) {
				SecondSplashScreenFragment.SHOW = false;
				HugerockPromoFragment.showInstance(fragmentManager);
			} else if (HMDPromoFragment.shouldShow(app)) {
				SecondSplashScreenFragment.SHOW = false;
				HMDPromoFragment.showInstance(fragmentManager);
			}
		}

		boolean showStorageMigrationScreen = false;
		if (fragmentsHelper.getFragment(WhatsNewDialogFragment.TAG) == null || WhatsNewDialogFragment.wasNotShown()) {
			if (fragmentsHelper.getFragment(SharedStorageWarningFragment.TAG) == null
					&& SharedStorageWarningFragment.dialogShowRequired(app)) {
				showStorageMigrationScreen = true;
				SecondSplashScreenFragment.SHOW = false;
				SharedStorageWarningFragment.showInstance(getSupportFragmentManager(), true);
			}
		}

		app.getNotificationHelper().refreshNotifications();
        // fixing bug with action bar appearing on android 2.3.3
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        checkAndRefreshDockFragmentIfNeeded();
        applyStatusBarVisibility();

		// for voice navigation. Lags behind routingAppMode changes, hence repeated
		// under onCalculationFinish()
		ApplicationMode routingAppMode = getRoutingHelper().getAppMode();
		if (routingAppMode != null && settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode) != null) {
			setVolumeControlStream(settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode));
		}

		applicationModeListener = prevAppMode -> app.runInUIThread(() -> {
			if (settings.APPLICATION_MODE.get() != prevAppMode) {
				settings.executePreservingPrefTimestamp(prevAppMode, () -> {
					settings.setLastKnownMapRotation(prevAppMode, getMapRotateTarget());
					settings.setLastKnownMapElevation(prevAppMode, getMapElevationAngle());
				});
				updateApplicationModeSettings();
			}
		});
		settings.APPLICATION_MODE.addListener(applicationModeListener);
		updateApplicationModeSettings(!app.getPoiFilters().isShowingAnyPoi());

		// if destination point was changed try to recalculate route
		TargetPointsHelper targets = app.getTargetPointsHelper();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode()
				&& (!Algorithms.objectEquals(targets.getPointToNavigate().getLatLon(), routingHelper.getFinalLocation())
						|| !Algorithms
								.objectEquals(targets.getIntermediatePointsLatLonNavigation(),
										routingHelper.getIntermediatePoints()))) {
			targets.updateRouteAndRefresh(true);
		}
		app.getLocationProvider().resumeAllUpdates();

		OsmandMapTileView mapView = getMapView();
		if (settings.isLastKnownMapLocation()) {
			LatLon l = settings.getLastKnownMapLocation();
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setHeight(settings.getLastKnownMapHeight());
			mapView.setZoomWithFloatPart(settings.getLastKnownMapZoom(), settings.getLastKnownMapZoomFloatPart());
			mapView.initMapRotationByCompassMode();
		}

		settings.MAP_ACTIVITY_ENABLED = true;
		LOG.info(">>>> MAP_ACTIVITY_ENABLED = true");

		mapView.showAndHideMapPosition();

		readLocationToShow();

		PluginsHelper.checkInstalledMarketPlugins(app, this);
		PluginsHelper.onMapActivityResume(this);

		intentHelper.parseContentIntent();
		mapView.refreshMap(true);

		if (mapViewWithLayers != null) {
			mapViewWithLayers.onResume();
		}
		app.getLauncherShortcutsHelper().updateLauncherShortcuts();
		app.getDownloadThread().setUiActivity(this);

		boolean routeWasFinished = routingHelper.isRouteWasFinished();
		if (routeWasFinished && !DestinationReachedFragment.wasShown()) {
			DestinationReachedFragment.show(this);
		}

		routingHelper.addListener(this);
		app.getMapMarkersHelper().addListener(this);
		app.getAutoBackupHelper().requestAutoBackup();

		if (System.currentTimeMillis() - time > 50) {
			LOG.error("onResume for MapActivity took " + (System.currentTimeMillis() - time) + " ms");
		}

		boolean showOsmAndWelcomeScreen = true;
		Intent intent = getIntent();
		if (intent != null && intent.hasExtra(FirstUsageWizardFragment.SHOW_OSMAND_WELCOME_SCREEN)) {
			showOsmAndWelcomeScreen = intent.getBooleanExtra(FirstUsageWizardFragment.SHOW_OSMAND_WELCOME_SCREEN, true);
		}
		boolean showWelcomeScreen = ((app.getAppInitializer().isFirstTime() && Version.isDeveloperVersion(app))
				|| !app.getResourceManager().isAnyMapInstalled())
				&& settings.SHOW_OSMAND_WELCOME_SCREEN.get()
				&& showOsmAndWelcomeScreen && !showStorageMigrationScreen;

		if (!showWelcomeScreen && !MapPermissionsResultCallback.permissionDone
				&& !app.getAppInitializer().isFirstTime()) {
			if (!permissionsResultCallback.permissionAsked) {
				if (app.isExternalStorageDirectoryReadOnly() && !showStorageMigrationScreen
						&& fragmentManager.findFragmentByTag(SharedStorageWarningFragment.TAG) == null
						&& fragmentManager.findFragmentByTag(SettingsScreenType.DATA_STORAGE.fragmentName) == null) {
					if (AndroidUtils.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						Bundle args = new Bundle();
						args.putBoolean(FIRST_USAGE, true);
						BaseSettingsFragment.showInstance(this, SettingsScreenType.DATA_STORAGE, null, args, null);
					} else {
						ActivityCompat.requestPermissions(this,
								new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
								DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
					}
				}
			} else {
				if (permissionsResultCallback.permissionGranted) {
					RestartActivity.doRestart(this, getString(R.string.storage_permission_restart_is_required));
				} else if (fragmentManager.findFragmentByTag(SettingsScreenType.DATA_STORAGE.fragmentName) == null) {
					Bundle args = new Bundle();
					args.putBoolean(FIRST_USAGE, true);
					BaseSettingsFragment.showInstance(this, SettingsScreenType.DATA_STORAGE, null, args, null);
				}
				permissionsResultCallback.permissionAsked = false;
				permissionsResultCallback.permissionGranted = false;
				MapPermissionsResultCallback.permissionDone = true;
			}
		}
		if (isDrawerAvailable()) {
			enableDrawer();
		} else {
			disableDrawer();
		}

		if (showWelcomeScreen && FirstUsageWizardFragment.showInstance(this)) {
			SecondSplashScreenFragment.SHOW = false;
		} else if (SendAnalyticsBottomSheetDialogFragment.shouldShowDialog(app)) {
			SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager, null);
		}
		if (fragmentsHelper.isFirstScreenShowing()
				&& (!settings.SHOW_OSMAND_WELCOME_SCREEN.get() || !showOsmAndWelcomeScreen)) {
			fragmentsHelper.disableFirstUsageFragment();
		}
		if (SecondSplashScreenFragment.SHOW && SecondSplashScreenFragment.showInstance(fragmentManager)) {
			SecondSplashScreenFragment.SHOW = false;
			SecondSplashScreenFragment.VISIBLE = true;
			mapView.setOnDrawMapListener(this);
			splashScreenTimer = new Timer();
			splashScreenTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					app.runInUIThread(fragmentsHelper::dismissSecondSplashScreen);
				}
			}, SECOND_SPLASH_TIME_OUT);
		} else {
			if (SecondSplashScreenFragment.VISIBLE) {
				fragmentsHelper.dismissSecondSplashScreen();
			}
			applyScreenOrientation();
		}

		settings.MAP_SCREEN_ORIENTATION.addListener(mapScreenOrientationSettingListener);
		settings.USE_SYSTEM_SCREEN_TIMEOUT.addListener(useSystemScreenTimeoutListener);
		settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.addListener(pinchZoomMagnificationListener);

		extendedMapActivity.onResume(this);

		getMapView().getAnimatedDraggingThread().toggleAnimations();
        
        // --- Weather Widget & Telemetry Integration ---
        if (weatherLocationListener == null) {
            weatherLocationListener = new OsmAndLocationProvider.OsmAndLocationListener() {
                @Override
                public void updateLocation(net.osmand.Location location) {
                     WeatherManager.getInstance(app).updateLocation(location);
                     net.osmand.plus.carlauncher.telemetry.TelemetryManager.getInstance(app).updateLocation(location);
                }
            };
        }
        if (app.getLocationProvider() != null) {
            app.getLocationProvider().addLocationListener(weatherLocationListener);
            // Initial Update
            net.osmand.Location loc = app.getLocationProvider().getLastKnownLocation();
            if (loc != null) {
                 WeatherManager.getInstance(app).updateLocation(loc);
                 net.osmand.plus.carlauncher.telemetry.TelemetryManager.getInstance(app).updateLocation(loc);
            }
        }
    }





	@Override
	public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
		if (isTopResumedActivity) {
			PluginsHelper.onMapActivityResumeOnTop(this);
		}
	}

	public void applyScreenOrientation() {
		if (settings.MAP_SCREEN_ORIENTATION.get() != getRequestedOrientation()) {
			setRequestedOrientation(settings.MAP_SCREEN_ORIENTATION.get());
		}
	}

	public void setKeepScreenOn(boolean keepScreenOn) {
		if (mapViewWithLayers != null) {
			mapViewWithLayers.setKeepScreenOn(keepScreenOn);
		}
	}

	@Override
	public void updateStatusBarColor() {
		UiUtilities.updateSystemBarColors(this);
	}

	@Override
	public boolean isNavigationBarContentLight() {
		return !app.getDaynightHelper().isNightMode(MAP);
	}

	public boolean isInAppPurchaseAllowed() {
		return true;
	}

	@Override
	public void onDrawOverMap() {
		getMapView().setOnDrawMapListener(null);
		cancelSplashScreenTimer();
		fragmentsHelper.dismissSecondSplashScreen();
	}

	private void cancelSplashScreenTimer() {
		if (splashScreenTimer != null) {
			splashScreenTimer.cancel();
			splashScreenTimer = null;
		}
	}

	public boolean isActivityDestroyed() {
		return mIsDestroyed;
	}

	public boolean isMapVisible() {
		if (fragmentsHelper.isFragmentVisible()) {
			return false;
		}
		return AndroidUtils.isActivityNotDestroyed(this) && settings.MAP_ACTIVITY_ENABLED
				&& !dashboardOnMap.isVisible();
	}

	public void readLocationToShow() {
		showMapControls();
		OsmandMapTileView mapView = getMapView();
		LatLon currentLatLon = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		LatLon latLonToShow = settings.getAndClearMapLocationToShow();
		PointDescription mapLabelToShow = settings.getAndClearMapLabelToShow(latLonToShow);
		Object toShow = settings.getAndClearObjectToShow();
		boolean editToShow = settings.getAndClearEditObjectToShow();
		int status = settings.isRouteToPointNavigateAndClear();
		String searchRequestToShow = settings.getAndClearSearchRequestToShow();
		if (status != 0 || searchRequestToShow != null || latLonToShow != null) {
			fragmentsHelper.dismissSettingsScreens();
		}
		if (status != 0) {
			// always enable and follow and let calculate it (i.e.GPS is not accessible in a
			// garage)
			Location loc = new Location("map");
			loc.setLatitude(mapView.getLatitude());
			loc.setLongitude(mapView.getLongitude());
			getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true);
			if (dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
		}
		if (trackDetailsMenu.isVisible()) {
			trackDetailsMenu.show();
		}
		if (searchRequestToShow != null) {
			fragmentsHelper.showQuickSearch(searchRequestToShow);
		}
		if (latLonToShow != null) {
			if (dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
			// remember if map should come back to isMapLinkedToLocation=true
			getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (mapLabelToShow != null && !mapLabelToShow.contextMenuDisabled()) {
				mapContextMenu.setMapCenter(latLonToShow);
				mapContextMenu.setCenterMarker(true);

				RotatedTileBox tb = mapView.getRotatedTileBox();
				LatLon prevCenter = tb.getCenterLatLon();

				double border = 0.8;
				int tbw = (int) (tb.getPixWidth() * border);
				int tbh = (int) (tb.getPixHeight() * border);
				tb.setPixelDimensions(tbw, tbh);

				tb.setLatLonCenter(latLonToShow.getLatitude(), latLonToShow.getLongitude());

				int zoom = settings.hasMapZoomToShow() ? settings.getMapZoomToShow() : ZOOM_LABEL_DISPLAY;
				tb.setZoom(zoom);
				while (!tb.containsLatLon(prevCenter.getLatitude(), prevCenter.getLongitude())
						&& tb.getZoom() > zoom - MAX_ZOOM_OUT_STEPS) {
					tb.setZoom(tb.getZoom() - 1);
				}
				boolean containsPrevious = tb.containsLatLon(prevCenter.getLatitude(), prevCenter.getLongitude());
				mapContextMenu.setMapZoom(containsPrevious ? tb.getZoom() : zoom);

				if (toShow instanceof GpxDisplayItem displayItem) {
					trackDetailsMenu.setGpxItem(displayItem);
					trackDetailsMenu.show();
				} else if (mapRouteInfoMenu.isVisible()) {
					mapContextMenu.showMinimized(latLonToShow, mapLabelToShow, toShow);
					mapRouteInfoMenu.updateMenu();
					MapRouteInfoMenu.showLocationOnMap(this, latLonToShow.getLatitude(), latLonToShow.getLongitude());
				} else if (toShow instanceof GpxFile gpxFile) {
					hideContextAndRouteInfoMenues();
					SelectedGpxFile selectedGpxFile;
					if (gpxFile.isShowCurrentTrack()) {
						selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
					} else {
						GpxSelectionParams params = GpxSelectionParams.newInstance()
								.showOnMap().selectedAutomatically().saveSelection();
						selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);
					}

					TrackAppearanceFragment.showInstance(this, selectedGpxFile, null);
				} else if (toShow instanceof QuadRect qr) {
					mapView.fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
				} else if (toShow instanceof NewGpxPoint newGpxPoint) {
					QuadRect qr = newGpxPoint.getRect();
					mapView.fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
					getMapLayers().getContextMenuLayer().enterAddGpxPointMode(newGpxPoint);
				} else if (toShow instanceof GpxData gpxData) {
					hideContextAndRouteInfoMenues();

					QuadRect qr = gpxData.getRect();
					mapView.fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
					MeasurementEditingContext editingContext = new MeasurementEditingContext(app);
					editingContext.setGpxData(gpxData);
					MeasurementToolFragment.showInstance(getSupportFragmentManager(), editingContext, PLAN_ROUTE_MODE,
							true);
				} else {
					mapContextMenu.show(latLonToShow, mapLabelToShow, toShow);
				}
				if (editToShow) {
					mapContextMenu.openEditor();
				}
			} else if (!latLonToShow.equals(currentLatLon)) {
				mapView.getAnimatedDraggingThread().startMoving(latLonToShow.getLatitude(),
						latLonToShow.getLongitude(), settings.getMapZoomToShow());
			}
		}
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (trackballController.onTrackballEvent(event)) {
			return true;
		}
		return super.onTrackballEvent(event);
	}

	@Override
	protected void onStart() {
		super.onStart();
		stopped = false;
		lockHelper.onStart();
		app.getNotificationHelper().showNotifications();
		extendedMapActivity.onStart(this);
	}

	@Override
	protected void onStop() {
		app.getNotificationHelper().removeNotifications(true);
        if (app != null && app.getLocationProvider() != null && weatherLocationListener != null) {
            app.getLocationProvider().removeLocationListener(weatherLocationListener);
        }
		if (pendingPause) {
			onPauseActivity();
		}
		stopped = true;
		lockHelper.onStop(this);
		extendedMapActivity.onStop(this);

		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (voiceStateReceiver != null) {
			try {
				unregisterReceiver(voiceStateReceiver);
			} catch (Exception e) {}
		}
		destroyProgressBarForRouting();
		boolean ownsSharedMap = getMapView().getMapActivity() == this;
		if (ownsSharedMap) {
			getMapActions().setMapActivity(null);
			getMapLayers().setMapActivity(null);
			getMapView().setMapActivity(null);
			mapContextMenu.setMapActivity(null);
			mapRouteInfoMenu.setMapActivity(null);
			trackDetailsMenu.setMapActivity(null);
		}
		unregisterReceiver(screenOffReceiver);
		if (carFloatingButtonReceiver != null) {
			unregisterReceiver(carFloatingButtonReceiver);
		}
		if (globalPackageReceiver != null) {
			unregisterReceiver(globalPackageReceiver);
		}
		
		// Unregister musicDrawerReceiver (Turkce karakter yok)
		if (musicDrawerReceiver != null) {
			try {
				androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(musicDrawerReceiver);
				unregisterReceiver(musicDrawerReceiver);
			} catch (Exception e) {
				// ignore
			}
		}
		
		// Unregister Antenna Panel Close Receiver (Turkce karakter yok)
		if (antennaPanelReceiver != null) {
			androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
					.unregisterReceiver(antennaPanelReceiver);
		}
		if (ownsSharedMap) {
			net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).hideButton();
		}
		app.getAidlApi().onDestroyMapActivity(this);
		app.getImportHelper().resetUIActivity(this);
		PluginsHelper.onMapActivityDestroy(this);
		app.unsubscribeInitListener(initListener);
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession == null || !carNavigationSession.hasStarted()) {
			getMapViewTrackingUtilities().setMapView(null);
		}
		if (mapViewWithLayers != null) {
			mapViewWithLayers.onDestroy();
		}
		if (ownsSharedMap) {
			lockHelper.setLockUIAdapter(null);
			keyEventHelper.setMapActivity(null);
		}
		extendedMapActivity.onDestroy(this);

		mIsDestroyed = true;

		removeActivityResultListener(importHelper.getSaveFileResultListener());
	}

	public LatLon getMapLocation() {
		return getMapViewTrackingUtilities().getMapLocation();
	}

	public float getMapRotate() {
		return getMapView().getRotate();
	}

	public float getMapRotateTarget() {
		OsmandMapTileView mapView = getMapView();
		if (mapView.isAnimatingMapRotation()) {
			float targetRotate = mapView.getAnimatedDraggingThread().getTargetRotate();
			if (targetRotate != TARGET_NO_ROTATION) {
				return targetRotate;
			}
		}
		return mapView.getRotate();
	}

	public float getMapElevationAngle() {
		return getMapView().getElevationAngle();
	}

	// Duplicate methods to OsmAndApplication
	@Nullable
	public TargetPoint getPointToNavigate() {
		return app.getTargetPointsHelper().getPointToNavigate();
	}

	public RoutingHelper getRoutingHelper() {
		return app.getRoutingHelper();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (getMapView().getMapActivity() == this) {
			net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).setAppInForeground(false);
		}
		settings.LAST_MAP_ACTIVITY_PAUSED_TIME.set(System.currentTimeMillis());
		
		boolean isInPip = false;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			isInPip = isInPictureInPictureMode();
		}

		if (isInPip) {
			// PiP modundayken haritanin guncellenmeye devam etmesi icin onPauseActivity'yi cagirmiyoruz (Turkce karakter yok)
			pendingPause = false;
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
				pendingPause = true;
			} else {
				onPauseActivity();
			}
		}
		extendedMapActivity.onPause(this);
	}

	private void onPauseActivity() {
		settings.MAP_SCREEN_ORIENTATION.removeListener(mapScreenOrientationSettingListener);
		settings.USE_SYSTEM_SCREEN_TIMEOUT.removeListener(useSystemScreenTimeoutListener);
		settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.removeListener(pinchZoomMagnificationListener);
		if (!app.getRoutingHelper().isRouteWasFinished()) {
			DestinationReachedFragment.resetShownState();
		}
		if (trackDetailsMenu.isVisible()) {
			trackDetailsMenu.dismiss(false);
		}
		pendingPause = false;
		OsmandMapTileView mapView = getMapView();
		mapView.setOnDrawMapListener(null);
		cancelSplashScreenTimer();
		app.getMapMarkersHelper().removeListener(this);
		app.getRoutingHelper().removeListener(this);
		app.getDownloadThread().resetUiActivity(this);

		if (mapViewWithLayers != null) {
			mapViewWithLayers.onPause();
		}

		// Yuzen Buton (Floating Button) Force GPS Kontrolu (Turkce karakter yok)
		net.osmand.plus.carlauncher.CarLauncherSettings carSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(this);
		boolean shouldKeepGpsAlive = carSettings.isFloatingButtonEnabled() && carSettings.isFloatingButtonForceGpsEnabled();
		
		if (!shouldKeepGpsAlive) {
			app.getLocationProvider().pauseAllUpdates();
		}

		app.getDaynightHelper().stopSensorIfNeeded();
		settings.APPLICATION_MODE.removeListener(applicationModeListener);

		LatLon mapLocation = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		settings.setLastKnownMapLocation(mapLocation);
		AnimateDraggingMapThread animatedThread = mapView.getAnimatedDraggingThread();
		if (animatedThread.isAnimating() && animatedThread.getTargetIntZoom() != 0
				&& !getMapViewTrackingUtilities().isMapLinkedToLocation()) {
			settings.setMapLocationToShow(animatedThread.getTargetLatitude(), animatedThread.getTargetLongitude(),
					animatedThread.getTargetIntZoom());
		}
		mapView.syncRotate();

		MapRendererView mapRenderer = mapView.getMapRenderer();
		if (mapRenderer != null)
			settings.setLastKnownMapHeight(mapRenderer.getMapTargetHeightInMeters());
		settings.setLastKnownMapZoom(mapView.getZoom());
		settings.setLastKnownMapZoomFloatPart(mapView.getZoomFloatPart());
		settings.setLastKnownMapRotation(mapView.getRotate());
		settings.setLastKnownMapElevation(mapView.getElevationAngle());
		settings.MAP_ACTIVITY_ENABLED = false;
		LOG.info(">>>> MAP_ACTIVITY_ENABLED = false");

		getMapView().getAnimatedDraggingThread().toggleAnimations();
		app.getResourceManager().interruptRendering();
		PluginsHelper.onMapActivityPause(this);
	}

	public void updateApplicationModeSettings() {
		updateApplicationModeSettings(true);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			hideSystemUI();
		}
	}

	private void hideSystemUI() {
		// Enforce Immersive Sticky Mode
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
						| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	public void updateApplicationModeSettings(boolean forceUpdatePoiFilters) {
		changeKeyguardFlags();
		updateMapSettings(false);
		if (forceUpdatePoiFilters) {
			app.getPoiFilters().loadSelectedPoiFilters();
		}
		app.getSearchUICore().refreshCustomPoiFilters();
		app.getMapButtonsHelper().updateActiveActions();
		getMapViewTrackingUtilities().appModeChanged();
		keyEventHelper.updateGlobalCommands();

		OsmandMapTileView mapView = getMapView();

		mapView.setPinchZoomMagnificationEnabled(settings.ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION.get());

		MapLayers mapLayers = getMapLayers();
		if (mapLayers.getMapInfoLayer() != null) {
			mapLayers.getMapInfoLayer().recreateAllControls(this);
		}
		if (mapLayers.getMapQuickActionLayer() != null) {
			mapLayers.getMapQuickActionLayer().refreshLayer();
		}
		MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
		if (mapControlsLayer != null) {
			mapControlsLayer.refreshButtons();
			if (!mapControlsLayer.isMapControlsVisible() && !settings.MAP_EMPTY_STATE_ALLOWED.get()) {
				showMapControls();
			}
		}

		mapLayers.updateLayers(this);

		getMapActions().updateDrawerMenu();
		updateNavigationBarColor();
		// mapView.setComplexZoom(mapView.getZoom(), mapView.getSettingsMapDensity());
		mapView.setMapDensity(mapView.getSettingsMapDensity());
		app.getDaynightHelper().startSensorIfNeeded(change -> app.runInUIThread(() -> getMapView().refreshMap(true)));
		getMapView().refreshMap(true);
		applyScreenOrientation();
		app.getAppCustomization().updateMapMargins(this);
		dashboardOnMap.onAppModeChanged();
	}

	public void updateMapSettings(boolean updateMapRenderer) {
		getMapView().updateMapSettings(updateMapRenderer, changed -> {
			if (changed) {
				ConfigureMapFragment fragment = ConfigureMapFragment.getVisibleInstance(this);
				if (fragment != null) {
					fragment.onRefreshItem(MAP_STYLE_ID);
				}
			}
			return true;
		});
	}

	public MapScrollHelper getMapScrollHelper() {
		return mapScrollHelper;
	}

	// onBackPressed already defined in super or elsewhere, ensuring we don't
	// duplicate.
	// We will merge logic if needed, but currently removing duplicate.

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyDown(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyUp(keyCode, event)) {
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyLongPress(keyCode, event)) {
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyMultiple(keyCode, repeatCount, event)) {
			return true;
		}
		return super.onKeyMultiple(keyCode, repeatCount, event);
	}

	public void showMapControls() {
		MapLayers mapLayers = getMapLayers();
		if (!getDashboard().isVisible() && mapLayers.getMapControlsLayer() != null) {
			mapLayers.getMapControlsLayer().showMapControlsIfHidden();
		}
	}

	public boolean shouldHideTopControls() {
		boolean hideTopControls = !mapContextMenu.shouldShowTopControls();

		TrackMenuFragment fragment = fragmentsHelper.getTrackMenuFragment();
		if (fragment != null) {
			hideTopControls = hideTopControls || !fragment.shouldShowTopControls();
		}

		return hideTopControls;
	}

	@NonNull
	public OsmandMapTileView getMapView() {
		return app.getOsmandMap().getMapView();
	}

	@NonNull
	public MapViewTrackingUtilities getMapViewTrackingUtilities() {
		return app.getMapViewTrackingUtilities();
	}

	@NonNull
	public MapDisplayPositionManager getMapPositionManager() {
		return app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
	}

	@NonNull
	public MapActivityActions getMapActions() {
		return app.getOsmandMap().getMapActions();
	}

	@NonNull
	public MapLayers getMapLayers() {
		return app.getOsmandMap().getMapLayers();
	}

	@NonNull
	public WidgetsVisibilityHelper getWidgetsVisibilityHelper() {
		return mapWidgetsVisibilityHelper;
	}

	public static void launchMapActivityMoveToTop(@NonNull Context activity) {
		launchMapActivityMoveToTop(activity, null, null, null);
	}

	public static void launchMapActivityMoveToTop(@NonNull Context activity,
			@Nullable Bundle prevIntentParams,
			@Nullable Uri intentData,
			@Nullable Bundle intentParams) {
		if (activity instanceof MapActivity) {
			if (((MapActivity) activity).getDashboard().isVisible()) {
				((MapActivity) activity).getDashboard().hideDashboard();
			}
			((MapActivity) activity).readLocationToShow();
		} else {
			int additionalFlags = 0;
			if (activity instanceof Activity) {
				Intent intent = ((Activity) activity).getIntent();
				if (intent != null) {
					prevActivityIntent = new Intent(intent);
					if (prevIntentParams != null) {
						prevActivityIntent.putExtra(INTENT_PARAMS, prevIntentParams);
						prevActivityIntent.putExtras(prevIntentParams);
					}
					prevActivityIntent.putExtra(INTENT_KEY_PARENT_MAP_ACTIVITY, true);
				} else {
					prevActivityIntent = null;
				}
			} else {
				prevActivityIntent = null;
				additionalFlags = Intent.FLAG_ACTIVITY_NEW_TASK;
			}

			Intent newIntent = new Intent(activity, ((OsmandApplication) activity.getApplicationContext())
					.getAppCustomization().getMapActivity());
			newIntent
					.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP | additionalFlags);
			if (intentData != null) {
				newIntent.setAction(Intent.ACTION_VIEW);
				newIntent.setData(intentData);
			}
			if (intentParams != null) {
				newIntent.putExtra(INTENT_PARAMS, intentParams);
				newIntent.putExtras(intentParams);
			}
			AndroidUtils.startActivityIfSafe(activity, newIntent);
		}
	}

	public static void clearPrevActivityIntent() {
		prevActivityIntent = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		PluginsHelper.onMapActivityResult(requestCode, resultCode, data);
		extendedMapActivity.onActivityResult(this, requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void refreshMap() {
		getMapView().refreshMap();
	}

	public void updateLayers() {
		getMapLayers().updateLayers(this);
	}

	public void refreshMapComplete() {
		getMapView().refreshMapComplete();
	}

	public View getLayout() {
		return getWindow().getDecorView().findViewById(android.R.id.content);
	}

	public void setMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
		View layout = getLayout();
		if (layout != null) {
			ViewGroup.LayoutParams params = layout.getLayoutParams();
			if (params instanceof ViewGroup.MarginLayoutParams) {
				((ViewGroup.MarginLayoutParams) params).setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
			}
		}
	}

	@NonNull
	public DashboardOnMap getDashboard() {
		return dashboardOnMap;
	}

	@NonNull
	public MapContextMenu getContextMenu() {
		return mapContextMenu;
	}

	@NonNull
	public MapRouteInfoMenu getMapRouteInfoMenu() {
		return mapRouteInfoMenu;
	}

	@NonNull
	public TrackDetailsMenu getTrackDetailsMenu() {
		return trackDetailsMenu;
	}

	@NonNull
	public MapFragmentsHelper getFragmentsHelper() {
		return fragmentsHelper;
	}

	@NonNull
	public RestoreNavigationHelper getRestoreNavigationHelper() {
		return restoreNavigationHelper;
	}

	public void hideContextAndRouteInfoMenues() {
		mapContextMenu.hideMenus();
		mapRouteInfoMenu.hide();
	}

	public void openDrawer() {
		if (isDrawerAvailable()) {
			getMapActions().updateDrawerMenu();
			boolean animate = !settings.DO_NOT_USE_ANIMATIONS.get();
			drawerLayout.openDrawer(GravityCompat.START, animate);
		}
	}

	public void disableDrawer() {
		drawerDisabled = true;
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			closeDrawer();
		}
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
	}

	public void enableDrawer() {
		if (isDrawerAvailable()) {
			drawerDisabled = false;
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}
	}

	public boolean isDrawerDisabled() {
		return drawerDisabled;
	}

	public boolean isDrawerAvailable() {
		return app.getAppCustomization().isFeatureEnabled(FRAGMENT_DRAWER_ID);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (lockHelper.isScreenLocked()) {
			return lockHelper.getLockGestureDetector(this).onTouchEvent(event);
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
			lockHelper.resetLockTimerIfNeeded();
		}

		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
					int drawerWidth = AndroidUtils.dpToPx(this, 280);
					int screenWidth = AndroidUtils.getScreenWidth(this);
					boolean isLayoutRtl = AndroidUtils.isLayoutRtl(app);
					if ((!isLayoutRtl && event.getRawX() > drawerWidth)
							|| (isLayoutRtl && event.getRawX() <= screenWidth - drawerWidth)) {
						closeDrawer();
					}
				}
			}
		}
		return super.dispatchTouchEvent(event);
	}

	public void closeDrawer() {
		boolean animate = !settings.DO_NOT_USE_ANIMATIONS.get();
		drawerLayout.closeDrawer(GravityCompat.START, animate);
	}

	public void toggleDrawer() {
		if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
			closeDrawer();
		} else {
			openDrawer();
		}
	}

	// DownloadEvents
	@Override
	public void onUpdatedIndexesList() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadEvents && fragment.isAdded()) {
				((DownloadEvents) fragment).onUpdatedIndexesList();
			}
		}
		if (dashboardOnMap.isVisible()) {
			dashboardOnMap.onNewDownloadIndexes();
		}
		refreshMap();
	}

	@Override
	public void downloadInProgress() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadEvents && fragment.isAdded()) {
				((DownloadEvents) fragment).downloadInProgress();
			}
		}
		if (dashboardOnMap.isVisible()) {
			dashboardOnMap.onDownloadInProgress();
		}
	}

	@Override
	public void downloadingError(@NonNull String error) {
		if (Algorithms.stringsEqual(error, DownloadValidationManager.getFreeVersionMessage(app))) {
			ChoosePlanFragment.showInstance(this, UNLIMITED_MAP_DOWNLOADS);
		}
	}

	@Override
	public void downloadHasFinished() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadEvents && fragment.isAdded()) {
				((DownloadEvents) fragment).downloadHasFinished();
			}
		}
		if (dashboardOnMap.isVisible()) {
			dashboardOnMap.onDownloadHasFinished();
		}
		refreshMapComplete();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
		permissionsResultCallback.onRequestPermissionsResult(requestCode, permissions, grantResults);
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public void onMapMarkerChanged(MapMarker mapMarker) {
		refreshMap();
	}

	@Override
	public void onMapMarkersChanged() {
		refreshMap();
	}

	@Override
	public void onAMapPointUpdated(AidlMapPointWrapper point, String layerId) {
		if (canUpdateAMapPointMenu(point, layerId)) {
			app.runInUIThread(() -> {
				LatLon latLon = point.getLocation();
				PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_MARKER,
						point.getFullName());
				mapContextMenu.update(latLon, pointDescription, point);
				mapContextMenu.centerMarkerLocation();
			});
		}
	}

	private boolean canUpdateAMapPointMenu(AidlMapPointWrapper point, String layerId) {
		Object object = mapContextMenu.getObject();
		if (!mapContextMenu.isVisible() || !(object instanceof AidlMapPointWrapper)) {
			return false;
		}
		AidlMapPointWrapper oldPoint = (AidlMapPointWrapper) object;
		return oldPoint.getLayerId().equals(layerId) && oldPoint.getId().equals(point.getId());
	}

	public void changeKeyguardFlags() {
		boolean enabled = settings.TURN_SCREEN_ON_TIME_INT.get() >= 0;
		boolean keepScreenOn = !settings.USE_SYSTEM_SCREEN_TIMEOUT.get();
		changeKeyguardFlags(enabled, keepScreenOn);
	}

	private void changeKeyguardFlags(boolean enable, boolean forceKeepScreenOn) {
		if (enable) {
			getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
					WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
							| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
					| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}
		setKeepScreenOn(forceKeepScreenOn);
	}

	@Override
	public void lock() {
		changeKeyguardFlags(false, false);
	}

	@Override
	public void unlock() {
		changeKeyguardFlags(true, true);
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		return fragmentsHelper.onPreferenceStartFragment(caller, pref);
	}

	private class ScreenOffReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			PluginsHelper.onMapActivityScreenOff(MapActivity.this);
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		if (mapRouteInfoMenu.isSelectFromMap()) {
			return;
		}
		refreshMap();
		RoutingHelper rh = app.getRoutingHelper();
		if (newRoute && rh.isRoutePlanningMode() && !getMapView().isCarView()) {
			app.runInUIThread(this::fitCurrentRouteToMap, 300);
		}
		if (app.getSettings().simulateNavigation) {
			OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
			if (newRoute && rh.isFollowingMode() && !sim.isRouteAnimating()) {
				sim.startStopRouteAnimation(this);
			}
		}
		for (OsmandPlugin plugin : PluginsHelper.getEnabledPlugins()) {
			plugin.newRouteIsCalculated(newRoute);
		}
	}

	private void fitCurrentRouteToMap() {
		boolean portrait = true;
		int leftBottomPaddingPx = 0;
		WeakReference<?> fragmentRef = mapRouteInfoMenu.findMenuFragment();
		if (fragmentRef == null) {
			fragmentRef = mapRouteInfoMenu.findFollowTrackFragment();
		}
		View mapBottomView = findViewById(R.id.map_bottom_widgets_panel);
		int mapBottomViewHeight = mapBottomView.getHeight();
		if (fragmentRef != null) {
			ContextMenuFragment f = (ContextMenuFragment) fragmentRef.get();
			portrait = f.isPortrait();
			if (!portrait) {
				leftBottomPaddingPx = f.getWidth();
			} else {
				leftBottomPaddingPx = Math.max(0, f.getHeight() - mapBottomViewHeight);
			}
		}
		app.getOsmandMap().fitCurrentRouteToMap(portrait, leftBottomPaddingPx);
	}

	@Override
	public void routeWasCancelled() {
		changeKeyguardFlags();
	}

	@Override
	public void routeWasFinished() {
		if (!mIsDestroyed) {
			DestinationReachedFragment.show(this);
			changeKeyguardFlags();
		}
	}

	public boolean isTopToolbarActive() {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		return mapInfoLayer.hasTopToolbar();
	}

	public TopToolbarController getTopToolbarController(@NonNull TopToolbarControllerType type) {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		return mapInfoLayer.getTopToolbarController(type);
	}

	public void showTopToolbar(@NonNull TopToolbarController controller) {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		mapInfoLayer.addTopToolbarController(controller);
		updateStatusBarColor();
	}

	public void hideTopToolbar(@NonNull TopToolbarController controller) {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		mapInfoLayer.removeTopToolbarController(controller);
		updateStatusBarColor();
	}

	public void hideTopToolbar(@NonNull TopToolbarControllerType type) {
		TopToolbarController controller = getTopToolbarController(type);
		if (controller != null) {
			hideTopToolbar(controller);
		}
	}

	@Nullable
	protected List<View> getHidingViews() {
		List<View> views = new ArrayList<>();
		View mainContainer = findViewById(R.id.map_hud_layout);
		if (mainContainer != null) {
			views.add(mainContainer);
		}
		return views;
	}

	@Override
	public List<Fragment> getActiveTalkbackFragments() {
		return fragmentsHelper.getActiveTalkbackFragments();
	}

	@Override
	public void onOsmAndSettingsCustomized() {
		restart();
	}

	public void restart() {
		if (stopped) {
			activityRestartNeeded = true;
		} else {
			recreate();
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		applyNightDimMode(); // Gece modu karartma overlay guncellemesi (Turkce karakter yok)

		app.getLocaleHelper().setLanguage(this);
		app.runInUIThread(fragmentsHelper::updateFragments);
		app.getNotificationHelper().refreshNotifications();

		// Orientation degistiginde ana ekran yerlesimini (ConstraintLayout) guncelle (Turkce karakter yok)
		if (carLayoutManager != null) {
			carLayoutManager.applyLayout(isWidgetPanelOpen, layoutMode);
		}
        
        checkAndRefreshDockFragmentIfNeeded();
	}

	@Override
	public void onInAppPurchaseGetItems() {
		DiscountHelper.checkAndDisplay(this);
	}

	@Override
	public void onInAppPurchaseItemPurchased(String sku) {
		getMapLayers().getRouteLayer().resetColorAvailabilityCache();
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		// Ayarlarda PiP aktifse, navigasyon aciksa ve Android 8.0+ ise gir (Turkce karakter yok)
		net.osmand.plus.carlauncher.CarLauncherSettings carSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(this);
		net.osmand.plus.routing.RoutingHelper routingHelper = app.getRoutingHelper();
		boolean isNavigating = routingHelper != null && routingHelper.isFollowingMode() && routingHelper.isRouteCalculated();

		if (isNavigating && carSettings.isPipModeEnabled() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			try {
				android.app.PictureInPictureParams.Builder builder = new android.app.PictureInPictureParams.Builder();
				android.util.Rational aspectRatio = new android.util.Rational(16, 9);
				builder.setAspectRatio(aspectRatio);
				enterPictureInPictureMode(builder.build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// Navigasyon yoksa yuzen buton gosterimini tetikle (Turkce karakter yok)
			net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).updateButtonState();
		}
	}

	@Override
	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
		super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

		View appDock = findViewById(R.id.app_dock);
		View widgetPanel = findViewById(R.id.widget_panel);
		View widgetHandle = findViewById(R.id.widget_handle);
		View mapHudContainer = findViewById(R.id.map_hud_container);

		// Yuzen butona PiP durumunu bildir (Turkce karakter yok)
		net.osmand.plus.carlauncher.ui.CarFloatingButtonManager.getInstance(this).setInPipMode(isInPictureInPictureMode);

		if (isInPictureInPictureMode) {
			// PiP modunda sadece harita kalir, diger her sey gizlenir (Turkce karakter yok)
			if (appDock != null) appDock.setVisibility(View.GONE);
			if (widgetPanel != null) widgetPanel.setVisibility(View.GONE);
			if (widgetHandle != null) widgetHandle.setVisibility(View.GONE);
			if (mapHudContainer != null) mapHudContainer.setVisibility(View.GONE);

			// CarLayoutManager'a PiP yerlesimini uygula (Turkce karakter yok)
			if (carLayoutManager != null) {
				carLayoutManager.applyPipLayout(true);
			}
			// Haritanin aninda PiP boyutlarina gore render edilmesini sagla (Turkce karakter yok)
			if (getMapView() != null) {
				getMapView().refreshMap(true);
			}
		} else {
			// PiP modundan cikildiginda elemanlari geri yukle (Turkce karakter yok)
			if (appDock != null) appDock.setVisibility(View.VISIBLE);
			if (carLayoutManager != null) {
				carLayoutManager.applyPipLayout(false);
			}
			applyWidgetPanelState();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			applyStatusBarVisibility();
		}
	}

	public void applyStatusBarVisibility() {
		net.osmand.plus.carlauncher.CarLauncherSettings carSettings = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(this);
		if (!carSettings.isLauncherEnabled()) return;

		boolean show = carSettings.isStatusBarVisible();
		Window window = getWindow();
		if (window == null) return;

		View rootLayout = findViewById(R.id.root_layout);
		androidx.core.view.WindowInsetsControllerCompat controller =
				androidx.core.view.WindowCompat.getInsetsController(window, window.getDecorView());

		// Edge-to-edge
		androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false);

		if (show) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			if (controller != null) {
				controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars());
			}
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			if (controller != null) {
				controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars());
				controller.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
			}
		}

		if (rootLayout != null) {
			rootLayout.post(() -> {
				int topPadding = show ? getStatusBarHeight() : 0;
				rootLayout.setPadding(rootLayout.getPaddingLeft(), topPadding, rootLayout.getPaddingRight(), rootLayout.getPaddingBottom());
				rootLayout.requestLayout();
			});
		}
	}

	private int getStatusBarHeight() {
		int height = 0;
		if (getWindow() != null && getWindow().getDecorView() != null) {
			androidx.core.view.WindowInsetsCompat insets = androidx.core.view.ViewCompat.getRootWindowInsets(getWindow().getDecorView());
			if (insets != null) {
				height = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
			}
		}
		if (height <= 0) {
			int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
			if (resourceId > 0) {
				height = getResources().getDimensionPixelSize(resourceId);
			}
		}
		return height;
	}
}

