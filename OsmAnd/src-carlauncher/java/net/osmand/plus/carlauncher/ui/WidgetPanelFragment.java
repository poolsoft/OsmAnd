package net.osmand.plus.carlauncher.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.Toast;


import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.DirectionWidget;
import net.osmand.plus.carlauncher.widgets.MusicWidget;
import net.osmand.plus.carlauncher.widgets.NavigationWidget;
import net.osmand.plus.carlauncher.widgets.OBDWidget;
import net.osmand.plus.carlauncher.widgets.SpeedWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin;

/**
 * Widget paneli fragment.
 * Widget'lari gosterir ve yonetir.
 * List (Scroll) ve Paged (ViewPager) modlarini destekler.
 */
public class WidgetPanelFragment extends Fragment {

    public static final String TAG = "WidgetPanelFragment";

    private RecyclerView listRecyclerView;
    private ViewPager2 widgetViewPager;
    private TabLayout tabLayout;
    private WidgetManager widgetManager;
    private OsmandApplication app;
    private BroadcastReceiver modeChangeReceiver;
    
    private ViewGroup rootContent;

    private android.widget.Button doneButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
            widgetManager = new WidgetManager(getContext(), app);
            initializeWidgets();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        // Main Container
        FrameLayout mainFrame = new FrameLayout(getContext());
        mainFrame.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT));
        mainFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_panel_modern);
        rootContent = mainFrame;

        // Determine Mode
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        int mode = settings.getWidgetDisplayMode(); // 0: List, 1: Paged

        if (mode == 1) {
            Toast.makeText(getContext(), "Debug: Sayfali Mod (1) Yuklendi - V2", Toast.LENGTH_LONG).show();
            initPagedLayout(mainFrame);
        } else {
            Toast.makeText(getContext(), "Debug: Liste Modu (0) Yuklendi - Deger: " + mode, Toast.LENGTH_LONG).show();
            initListLayout(mainFrame);
        }

        // --- Done Button for Edit Mode ---
        doneButton = new android.widget.Button(getContext());
        doneButton.setText("TAMAM");
        doneButton.setBackgroundColor(0xFF00AA00);
        doneButton.setTextColor(0xFFFFFFFF);
        doneButton.setVisibility(View.GONE);
        
        FrameLayout.LayoutParams doneParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
        doneParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        doneParams.setMargins(0, 0, 32, 32);
        
        doneButton.setOnClickListener(v -> {
            doneButton.setVisibility(View.GONE);
            if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                ((WidgetListAdapter) listRecyclerView.getAdapter()).setEditMode(false);
            }
        });
        
        mainFrame.addView(doneButton);

        // Long click to manage widgets (Legacy listener removed, now handled by Adapter)
        
        // Register Receiver for Mode Change
        modeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("net.osmand.carlauncher.WIDGET_MODE_CHANGED".equals(intent.getAction())) {
                    reloadFragment();
                }
            }
        };
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter("net.osmand.carlauncher.WIDGET_MODE_CHANGED");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(modeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(modeChangeReceiver, filter);
            }
        }

        return mainFrame;
    }
    
    // ... (rest of methods)

    private void applyWidgetsToView() {
        if (listRecyclerView != null) {
            // List Mode (RecyclerView)
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            // Adapter with Reorder Listener and Action Callbacks
            WidgetListAdapter adapter = new WidgetListAdapter(
                widgetManager.getVisibleWidgets(), 
                isPortrait, 
                new WidgetListAdapter.OnWidgetActionListener() {
                    @Override
                    public void onWidgetOrderChanged(java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> newOrder) {
                         widgetManager.updateVisibleOrder(newOrder);
                    }
                    
                    @Override
                    public void onWidgetRemoved(net.osmand.plus.carlauncher.widgets.BaseWidget widget) {
                        widgetManager.setWidgetVisible(widget.getId(), false);
                        widgetManager.saveWidgetConfig();
                        applyWidgetsToView(); // Refresh list to remove item
                        
                        // Keep Edit Mode Active
                         if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                            ((WidgetListAdapter) listRecyclerView.getAdapter()).setEditMode(true);
                            if (doneButton != null) doneButton.setVisibility(View.VISIBLE);
                        }
                    }
                    
                    @Override
                    public void onAddWidgetClicked() {
                        showAddWidgetDialog();
                    }
                    
                    @Override
                    public void onEditModeRequested() {
                         if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                            ((WidgetListAdapter) listRecyclerView.getAdapter()).setEditMode(true);
                            if (doneButton != null) doneButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            );
            listRecyclerView.setAdapter(adapter);
            
            // Attach Drag & Drop Helper
            androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new QuickItemTouchHelperCallback(adapter);
            androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(listRecyclerView);

            // Apply Layout Animation
            int resId = net.osmand.plus.R.anim.widget_layout_animation;
            android.view.animation.LayoutAnimationController animation = 
                android.view.animation.AnimationUtils.loadLayoutAnimation(getContext(), resId);
            listRecyclerView.setLayoutAnimation(animation);
            listRecyclerView.scheduleLayoutAnimation();
            
        } else if (widgetViewPager != null && tabLayout != null) {

    private void reloadFragment() {
         if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction()
                    .detach(this)
                    .attach(this)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getContext() != null && modeChangeReceiver != null) {
            try {
                getContext().unregisterReceiver(modeChangeReceiver);
            } catch (Exception e) {
                // ignore
            }
        }
        if (widgetManager != null) {
            widgetManager.saveWidgetConfig();
        }
    }

    // --- MODE 0: LIST LAYOUT ---
    // --- MODE 0: LIST LAYOUT ---
    private void initListLayout(ViewGroup root) {
        boolean isPortrait = getResources()
                .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

        listRecyclerView = new RecyclerView(getContext());
        listRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Layout Manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(isPortrait ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL);
        listRecyclerView.setLayoutManager(layoutManager);

        // Snap Helper (Snaps to start/center)
        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(listRecyclerView);
        
        // Long click listener for container logic if needed
        // RecyclerView handles its own touch, so we might need ItemTouchListener if we want "Empty Area" long click
        
        root.addView(listRecyclerView);
    }

    // --- MODE 1: PAGED LAYOUT ---
    private void initPagedLayout(ViewGroup root) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                 ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ViewPager
        widgetViewPager = new ViewPager2(getContext());
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        widgetViewPager.setLayoutParams(startParams);
        
        // TabLayout (Dots)
        tabLayout = new TabLayout(getContext());
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tabLayout.setLayoutParams(tabParams);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        tabLayout.setBackgroundColor(0x00000000);
        tabLayout.setSelectedTabIndicatorColor(0xFFFFFFFF);
        
        // Add Views
        container.addView(widgetViewPager);
        container.addView(tabLayout);
        root.addView(container);
        
        // Mediator attachment moved to applyWidgetsToView()
    }


    private void showAddWidgetDialog() {
        if (getContext() == null || widgetManager == null) return;

        java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> allWidgets = widgetManager.getAllWidgets();
        java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> hiddenWidgets = new java.util.ArrayList<>();
        
        for (net.osmand.plus.carlauncher.widgets.BaseWidget w : allWidgets) {
            if (!w.isVisible()) {
                hiddenWidgets.add(w);
            }
        }

        if (hiddenWidgets.isEmpty()) {
            Toast.makeText(getContext(), "Tüm widgetlar zaten ekli!", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Widget Ekle");
        
        String[] names = new String[hiddenWidgets.size()];
        for (int i = 0; i < hiddenWidgets.size(); i++) {
            names[i] = hiddenWidgets.get(i).getTitle();
        }

        builder.setItems(names, (dialog, which) -> {
            net.osmand.plus.carlauncher.widgets.BaseWidget selected = hiddenWidgets.get(which);
            widgetManager.setWidgetVisible(selected.getId(), true);
            widgetManager.saveWidgetConfig();
            
            // Refresh Adapter to show new widget
            if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                WidgetListAdapter adapter = (WidgetListAdapter) listRecyclerView.getAdapter();
                // We need to re-fetch visible widgets or just add this one?
                // Re-setting adapter is safest for order
                applyWidgetsToView();
                // Restore Edit Mode if needed
                if (listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                    ((WidgetListAdapter) listRecyclerView.getAdapter()).setEditMode(true);
                }
            }
        });

        builder.setNegativeButton("İptal", null);
        builder.show();
    }
    
    // Toggle Edit Mode Logic managed via Adapter callbacks below

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (widgetManager != null) {
            widgetManager.loadWidgetConfig();
            applyWidgetsToView();
        }
    }

    private void applyWidgetsToView() {
        if (listRecyclerView != null) {
            // List Mode (RecyclerView)
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            // Adapter with Reorder Listener
            WidgetListAdapter adapter = new WidgetListAdapter(
                widgetManager.getVisibleWidgets(), 
                isPortrait, 
                newOrder -> {
                     widgetManager.updateVisibleOrder(newOrder);
                }
            );
            listRecyclerView.setAdapter(adapter);
            
            // Attach Drag & Drop Helper
            androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new QuickItemTouchHelperCallback(adapter);
            androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(listRecyclerView);

            // Apply Layout Animation
            int resId = net.osmand.plus.R.anim.widget_layout_animation;
            android.view.animation.LayoutAnimationController animation = 
                android.view.animation.AnimationUtils.loadLayoutAnimation(getContext(), resId);
            listRecyclerView.setLayoutAnimation(animation);
            listRecyclerView.scheduleLayoutAnimation();
            
        } else if (widgetViewPager != null && tabLayout != null) {
            // Paged Mode
            WidgetPagerAdapter adapter = new WidgetPagerAdapter(widgetManager.getVisibleWidgets());
            widgetViewPager.setAdapter(adapter);
            
            // Connect Tabs (Only if not already connected - though mediator handles re-attach gracefully usually, 
            // but we can just create a new one as the adapter is new)
            new TabLayoutMediator(tabLayout, widgetViewPager,
                    (tab, position) -> {
                        // Empty title, just dots
                    }
            ).attach();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (widgetManager != null) {
            widgetManager.startAllWidgets();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (widgetManager != null) {
            widgetManager.stopAllWidgets();
        }
    }

    /**
     * Widget'lari initialize et.
     */
    private void initializeWidgets() {
        if (widgetManager == null || app == null)
            return;

        // Saat widget (her zaman en ustte)
        widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.Material3ClockWidget(getContext()));

        // Hiz widget
        widgetManager.addWidget(new SpeedWidget(getContext(), app));

        // Yon widget
        widgetManager.addWidget(new DirectionWidget(getContext(), app));

        // Anten Widget
        net.osmand.plus.carlauncher.antenna.AntennaPlugin antennaPlugin = net.osmand.plus.plugins.PluginsHelper
                .getPlugin(net.osmand.plus.carlauncher.antenna.AntennaPlugin.class);
        if (antennaPlugin != null && antennaPlugin.isActive()) {
            widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.AntennaWidget(getContext(), app));
        }

        // Navigasyon widget
        widgetManager.addWidget(new NavigationWidget(getContext(), app));

        // Muzik widget
        widgetManager.addWidget(new MusicWidget(getContext(), app));

        // OBD Widget
        VehicleMetricsPlugin obdPlugin = PluginsHelper
                .getPlugin(VehicleMetricsPlugin.class);
        if (obdPlugin != null && obdPlugin.isActive()) {
            widgetManager.addWidget(new OBDWidget(getContext(), app));
        }
    }

    public WidgetManager getWidgetManager() {
        return widgetManager;
    }
}
