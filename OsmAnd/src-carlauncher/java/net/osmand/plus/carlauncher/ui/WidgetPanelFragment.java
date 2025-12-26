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
import androidx.viewpager2.widget.ViewPager2;


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

    private LinearLayout widgetContainerList;
    private ViewPager2 widgetViewPager;
    private WidgetManager widgetManager;
    private OsmandApplication app;
    private BroadcastReceiver modeChangeReceiver;
    
    private ViewGroup rootContent;

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
            Toast.makeText(getContext(), "Debug: Sayfali Mod (1) Yuklendi", Toast.LENGTH_LONG).show();
            setupPagedLayout(mainFrame);
        } else {
            Toast.makeText(getContext(), "Debug: Liste Modu (0) Yuklendi - Deger: " + mode, Toast.LENGTH_LONG).show();
            setupListLayout(mainFrame);
        }

        // Long click to manage widgets
        mainFrame.setOnLongClickListener(v -> {
            showWidgetManagementDialog();
            return true;
        });

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
    private void setupListLayout(ViewGroup root) {
        boolean isPortrait = getResources()
                .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

        ViewGroup scrollContainer;
        ViewGroup.LayoutParams scrollParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        if (isPortrait) {
            android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(getContext());
            hScroll.setFillViewport(true);
            scrollContainer = hScroll;
        } else {
            ScrollView vScroll = new ScrollView(getContext());
            vScroll.setFillViewport(true);
            scrollContainer = vScroll;
        }
        scrollContainer.setLayoutParams(scrollParams);
        
        scrollContainer.setOnLongClickListener(v -> {
            showWidgetManagementDialog();
            return true;
        });

        widgetContainerList = new LinearLayout(getContext());
        widgetContainerList.setOrientation(isPortrait ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        widgetContainerList.setLayoutParams(new ViewGroup.LayoutParams(
                isPortrait ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT,
                isPortrait ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT));
        widgetContainerList.setPadding(2, 0, 2, 16);

        widgetContainerList.setOnLongClickListener(v -> {
            showWidgetManagementDialog();
            return true;
        });

        scrollContainer.addView(widgetContainerList);
        root.addView(scrollContainer);
    }

    // --- MODE 1: PAGED LAYOUT ---
    private void setupPagedLayout(ViewGroup root) {
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
        TabLayout tabLayout = new TabLayout(getContext());
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

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, widgetViewPager,
                (tab, position) -> {
                    // Start with empty dots
                }
        ).attach();
    }


    private void showWidgetManagementDialog() {
        if (getContext() == null || widgetManager == null)
            return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Widget Yonetimi");

        final LinearLayout listLayout = new LinearLayout(getContext());
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(16, 16, 16, 16);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(listLayout);

        builder.setView(scrollView);
        builder.setPositiveButton("Kapat", (dialog, which) -> {
            applyWidgetsToView(); // Re-attach based on mode
            widgetManager.startAllWidgets();
        });

        final android.app.AlertDialog dialog = builder.create();
        updateDialogList(listLayout, dialog);
        dialog.show();
    }

    private void updateDialogList(final LinearLayout listLayout, final android.app.AlertDialog dialog) {
        listLayout.removeAllViews();
        java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> widgets = widgetManager.getAllWidgets();

        for (int i = 0; i < widgets.size(); i++) {
            final net.osmand.plus.carlauncher.widgets.BaseWidget w = widgets.get(i);
            final int index = i;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);
            row.setBackgroundResource(android.R.drawable.list_selector_background);

            // Visibility Checkbox
            android.widget.CheckBox cb = new android.widget.CheckBox(getContext());
            cb.setChecked(w.isVisible());
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                widgetManager.setWidgetVisible(w.getId(), isChecked);
                widgetManager.saveWidgetConfig();
            });
            row.addView(cb);

            // Name
            android.widget.TextView nameView = new android.widget.TextView(getContext());
            nameView.setText(w.getTitle());
            nameView.setTextSize(16);
            nameView.setTextColor(0xFFFFFFFF);
            nameView.setPadding(16, 0, 16, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            row.addView(nameView, params);

            // Up Button
            if (index > 0) {
                android.widget.ImageButton upBtn = new android.widget.ImageButton(getContext());
                upBtn.setImageResource(android.R.drawable.arrow_up_float);
                upBtn.setBackgroundColor(0x00000000);
                upBtn.setPadding(24, 24, 24, 24);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(96, 96);
                btnParams.setMargins(8, 0, 8, 0);
                upBtn.setLayoutParams(btnParams);

                upBtn.setOnClickListener(v -> {
                    widgetManager.moveWidget(index, index - 1);
                    updateDialogList(listLayout, dialog);
                });
                row.addView(upBtn);
            } else {
                android.view.View spacer = new android.view.View(getContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(96, 96));
                row.addView(spacer);
            }

            // Down Button
            if (index < widgets.size() - 1) {
                android.widget.ImageButton downBtn = new android.widget.ImageButton(getContext());
                downBtn.setImageResource(android.R.drawable.arrow_down_float);
                downBtn.setBackgroundColor(0x00000000);
                downBtn.setPadding(24, 24, 24, 24);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(96, 96);
                btnParams.setMargins(8, 0, 8, 0);
                downBtn.setLayoutParams(btnParams);

                downBtn.setOnClickListener(v -> {
                    widgetManager.moveWidget(index, index + 1);
                    updateDialogList(listLayout, dialog);
                });
                row.addView(downBtn);
            }

            listLayout.addView(row);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (widgetManager != null) {
            widgetManager.loadWidgetConfig();
            applyWidgetsToView();
        }
    }

    private void applyWidgetsToView() {
        if (widgetContainerList != null) {
            // List Mode
            widgetManager.attachWidgetsToContainer(widgetContainerList);
        } else if (widgetViewPager != null) {
            // Paged Mode
            WidgetPagerAdapter adapter = new WidgetPagerAdapter(widgetManager.getVisibleWidgets());
            widgetViewPager.setAdapter(adapter);
            
            // Connect Tabs
            // Find TabLayout (sibling of pager in container)
             ViewGroup parent = (ViewGroup) widgetViewPager.getParent();
             if (parent != null) {
                 for(int i=0; i<parent.getChildCount(); i++) {
                     View child = parent.getChildAt(i);
                     if (child instanceof TabLayout) {
                         new TabLayoutMediator((TabLayout) child, widgetViewPager,
                                 (tab, position) -> {
                                     // Empty title, just dots
                                 }
                         ).attach();
                         break;
                     }
                 }
             }
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
