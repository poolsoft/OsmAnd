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
    private WidgetManager widgetManager;
    private OsmandApplication app;
    private BroadcastReceiver modeChangeReceiver;
    
    private ViewGroup rootContent;
    private int currentUnitSize = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
            widgetManager = new WidgetManager(getContext(), app);
            if (!widgetManager.loadWidgetConfig()) {
                initializeWidgets();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout mainFrame = new FrameLayout(getContext());
        mainFrame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Background for panel
        mainFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_panel_modern); 
        
        rootContent = mainFrame;
        
        initListLayout(mainFrame);
        setupMenuButton(mainFrame);
        
        return mainFrame;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootContent = null;
    }

    // --- MODE 0: LIST LAYOUT (UNIT BASED) ---
    private void initListLayout(ViewGroup root) {
        boolean isPortrait = getResources()
                .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

        listRecyclerView = new RecyclerView(getContext());
        listRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Zero Margin/Padding for perfect stacking
        listRecyclerView.setPadding(0, 0, 0, 0);
        listRecyclerView.setClipToPadding(false);

        // Layout Manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(isPortrait ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL);
        listRecyclerView.setLayoutManager(layoutManager);

        listRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (listRecyclerView.getMeasuredHeight() > 0 && listRecyclerView.getMeasuredWidth() > 0) {
                    listRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    
                    if (isPortrait) {
                        currentUnitSize = listRecyclerView.getMeasuredWidth() / 2;
                    } else {
                        currentUnitSize = listRecyclerView.getMeasuredHeight() / 6;
                    }
                    
                    if (listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                        ((WidgetListAdapter) listRecyclerView.getAdapter()).setUnitSize(currentUnitSize, isPortrait);
                    }
                }
            }
        });

        applyWidgetsToView(); // Initial Load
        root.addView(listRecyclerView);
    }

    private void setupMenuButton(ViewGroup root) {
        android.widget.ImageView menuBtn = new android.widget.ImageView(getContext());
        menuBtn.setImageResource(android.R.drawable.ic_menu_more);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            menuBtn.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            menuBtn.setBackgroundResource(android.R.drawable.item_background_borderless_material_dark); // Ripple
        } else {
             menuBtn.setBackgroundColor(0); // Transparent
        }
        menuBtn.setPadding(16, 16, 16, 16);
        
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
             ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        params.setMargins(16, 16, 16, 16);
        
        root.addView(menuBtn, params);
        
        menuBtn.setOnClickListener(v -> {
            showWidgetControlDialog();
        });
    }

    private void showWidgetControlDialog() {
        WidgetControlDialog dialog = new WidgetControlDialog();
        dialog.setWidgetManager(widgetManager);
        dialog.setOnDismissCallback(this::applyWidgetsToView);
        dialog.show(getParentFragmentManager(), "WidgetControl");
    }

    private void showAddWidgetDialog() {
        java.util.List<net.osmand.plus.carlauncher.widgets.WidgetRegistry.WidgetEntry> widgets = 
            net.osmand.plus.carlauncher.widgets.WidgetRegistry.getAvailableWidgets();
            
        final String[] displayNames = new String[widgets.size()];
        final String[] typeKeys = new String[widgets.size()];
        
        for (int i = 0; i < widgets.size(); i++) {
            displayNames[i] = widgets.get(i).displayName;
            typeKeys[i] = widgets.get(i).typeId;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Widget Ekle");
        builder.setItems(displayNames, (dialog, which) -> {
            String selectedType = typeKeys[which];
            showWidgetSizeDialog(selectedType);
        });
        
        builder.show();
    }

    private void showWidgetSizeDialog(String type) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Boyut Seçiniz");
        
        final String[] sizes = {"Küçük (Standart)", "Orta (Geniş)", "Büyük (Tam)"};
        
        builder.setItems(sizes, (dialog, which) -> {
            BaseWidget.WidgetSize size = BaseWidget.WidgetSize.SMALL;
            switch (which) {
                case 0: size = BaseWidget.WidgetSize.SMALL; break;
                case 1: size = BaseWidget.WidgetSize.MEDIUM; break;
                case 2: size = BaseWidget.WidgetSize.LARGE; break;
            }
            addNewWidget(type, size);
        });
        
        builder.show();
    }
    
    private void addNewWidget(String type, BaseWidget.WidgetSize size) {
        BaseWidget widget = net.osmand.plus.carlauncher.widgets.WidgetRegistry.createWidget(getContext(), app, type);
        if (widget != null) {
            widget.setSize(size);
            try {
                java.lang.reflect.Field idField = BaseWidget.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(widget, type + "_" + System.currentTimeMillis());
            } catch (Exception e) {
                // Ignore
            }
            
            widgetManager.addWidget(widget);
            applyWidgetsToView();
        }
    }

    private void applyWidgetsToView() {
        if (listRecyclerView != null) {
            // List Mode (RecyclerView)
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            WidgetListAdapter adapter = new WidgetListAdapter(
                widgetManager.getVisibleWidgets(), 
                isPortrait,
                new WidgetListAdapter.OnWidgetActionListener() {
                    @Override
                    public void onWidgetLongClicked(View view, net.osmand.plus.carlauncher.widgets.BaseWidget widget) {
                        // Unused
                    }
                    
                    @Override
                    public void onAddWidgetClicked() {
                        showAddWidgetDialog();
                    }

                    @Override
                    public void onWidgetOrderChanged(java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> newOrder) {
                        widgetManager.updateVisibleOrder(newOrder); 
                    }

                    @Override
                    public void onWidgetRemoved(net.osmand.plus.carlauncher.widgets.BaseWidget widget) {
                        widgetManager.removeWidget(widget);
                        applyWidgetsToView(); 
                    }
                }
            );
            
            if (currentUnitSize > 0) {
                adapter.setUnitSize(currentUnitSize, isPortrait);
            }
            
            listRecyclerView.setAdapter(adapter);
            
            // ... (ItemTouchHelper code continues)
            
            // Drag & Drop
            androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                @Override
                public boolean isLongPressDragEnabled() {
                    return true; // Use native drag on long press
                }

                @Override
                public boolean isItemViewSwipeEnabled() { return false; }

                @Override
                public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    int dragFlags = androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN |
                                    androidx.recyclerview.widget.ItemTouchHelper.START | androidx.recyclerview.widget.ItemTouchHelper.END;
                    return makeMovementFlags(dragFlags, 0);
                }

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                    adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }

                @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
                
                @Override
                public void onSelectedChanged(@androidx.annotation.Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState != androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE && viewHolder != null) {
                        viewHolder.itemView.setAlpha(0.7f);
                        viewHolder.itemView.setScaleX(1.05f);
                        viewHolder.itemView.setScaleY(1.05f);
                    }
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    viewHolder.itemView.setAlpha(1.0f);
                    viewHolder.itemView.setScaleX(1.0f);
                    viewHolder.itemView.setScaleY(1.0f);
                    
                    if (recyclerView.getAdapter() instanceof WidgetListAdapter) {
                        WidgetListAdapter adapter = (WidgetListAdapter) recyclerView.getAdapter();
                        if (widgetManager != null) {
                            widgetManager.updateVisibleOrder(adapter.getWidgets());
                        }
                    }
                }
            };
            
            androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(listRecyclerView);
            
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

        // Default Widgets Logic...
        // (If config exists, this is skipped by onCreate logic)
        
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
