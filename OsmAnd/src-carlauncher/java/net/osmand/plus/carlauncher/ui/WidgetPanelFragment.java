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

        // Unit Size Calculation
        // Landscape: Total Height / 6 = 1 Unit
        // Portrait: Total Width / 2 = 1 Unit (User Requirement: 2 widgets side-by-side)
        
        listRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (listRecyclerView.getMeasuredHeight() > 0 && listRecyclerView.getMeasuredWidth() > 0) {
                    listRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    
                    int unitSize;
                    if (isPortrait) {
                        // Portrait: 2 Items per screen width
                        unitSize = listRecyclerView.getMeasuredWidth() / 2;
                    } else {
                        // Landscape: 6 Units per screen height
                        unitSize = listRecyclerView.getMeasuredHeight() / 6;
                    }
                    
                    if (listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                        ((WidgetListAdapter) listRecyclerView.getAdapter()).setUnitSize(unitSize, isPortrait);
                    }
                }
            }
        });

        // Snap Helper: Use PagerSnapHelper because our items are perfectly sized to fit the screen 
        // (e.g. 3xSmall = 1 Screen, 2xMedium = 1 Screen). PagerSnap helps lock them in place.
        // Or LinearSnapHelper for free scrolling but snapping to edges. 
        // User requested "Magnetic", PagerSnapHelper is strongest for this if items fill screen.
        // However, if we have 1 Medium + 1 Small, that's 5/6 screen. PagerSnap might be weird.
        // Let's use LinearSnapHelper which snaps to Closest View Center/Start.
        LinearSnapHelper snapHelper = new LinearSnapHelper(); 
        snapHelper.attachToRecyclerView(listRecyclerView);
        
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


    // --- ADD WIDGET DIALOG ---
    private void showAddWidgetDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Widget Ekle");

        final String[] widgetTypes = {
            "Hız Göstergesi", 
            "Müzik Çalar", 
            "Navigasyon", 
            "Pusula", 
            "OBD Bilgileri",
            "Analog Saat",
            "Rakım (Anten)"
        };
        
        final String[] typeKeys = {
            net.osmand.plus.carlauncher.widgets.WidgetFactory.TYPE_SPEED,
            net.osmand.plus.carlauncher.widgets.WidgetFactory.TYPE_MUSIC,
            net.osmand.plus.carlauncher.widgets.WidgetFactory.TYPE_NAVIGATION,
            net.osmand.plus.carlauncher.widgets.WidgetFactory.TYPE_COMPASS,
            net.osmand.plus.carlauncher.widgets.WidgetFactory.TYPE_OBD,
            net.osmand.plus.carlauncher.widgets.WidgetFactory.TYPE_CLOCK,
            net.osmand.plus.carlauncher.widgets.WidgetFactory.TYPE_ANTENNA
        };

        builder.setItems(widgetTypes, (dialog, which) -> {
            String selectedType = typeKeys[which];
            showWidgetSizeDialog(selectedType);
        });
        
        builder.show();
    }

    private void showWidgetSizeDialog(String type) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Boyut Seçiniz");
        
        // 0: Small (2/6), 1: Medium (3/6), 2: Large (6/6)
        final String[] sizes = {"Küçük (1/3 Ekran)", "Orta (1/2 Ekran)", "Büyük (Tam Ekran)"};
        
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
        BaseWidget widget = net.osmand.plus.carlauncher.widgets.WidgetFactory.createWidget(getContext(), app, type);
        if (widget != null) {
            widget.setSize(size);
            // Assign unique ID if needed or let Manager handle it
            widgetManager.addWidget(widget);
            
            // Refresh View
            applyWidgetsToView();
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
        if (listRecyclerView != null) {
            // List Mode (RecyclerView)
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
                    @Override
                    public void onWidgetLongClicked(View view, net.osmand.plus.carlauncher.widgets.BaseWidget widget) {
                        showWidgetOptionsMenu(widget);
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
            
            listRecyclerView.setAdapter(adapter);
            
            // Drag & Drop (Initially Disabled)
            androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                @Override
                public boolean isLongPressDragEnabled() {
                    // Only allow drag if we are explicitly in SORT MODE (checked via adapter or local flag)
                    // We can reuse adapter.isEditMode() as "Sort Mode" or add a new flag.
                    // Let's us adapter.isEditMode() as "Touch Interaction Mode". 
                    // But wait, "Edit Mode" in adapter shows Delete buttons. "Sort Mode" hides them?
                    // User said: "Sort" -> Drag & Drop. "Edit" -> Delete.
                    // So we might need two states or just one 'Edit' state where drag is enabled too?
                    // User separated "Edit" and "Sort" in menu.
                    // Implementation: 
                    // adapter.isEditMode() -> Shows Delete buttons.
                    // adapter.isSortMode() (New?) -> Enables Drag.
                    // For simplicity, let's use a local flag `isSortMode` in Fragment.
                    return isSortMode;
                }

                @Override
                public boolean isItemViewSwipeEnabled() { return false; }

                @Override
                public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    if (viewHolder.getItemViewType() == 1) return makeMovementFlags(0, 0);
                    int dragFlags = androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN |
                                    androidx.recyclerview.widget.ItemTouchHelper.START | androidx.recyclerview.widget.ItemTouchHelper.END;
                    return makeMovementFlags(dragFlags, 0);
                }

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                    if (source.getItemViewType() != target.getItemViewType()) return false;
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
                }
            };
            
            androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(listRecyclerView);
            
        } else if (widgetViewPager != null && tabLayout != null) {
             WidgetPagerAdapter adapter = new WidgetPagerAdapter(widgetManager.getVisibleWidgets());
             widgetViewPager.setAdapter(adapter);
             new TabLayoutMediator(tabLayout, widgetViewPager, (tab, position) -> {}).attach();
        }
    }
    
    private boolean isSortMode = false;

    private void showWidgetOptionsMenu(final net.osmand.plus.carlauncher.widgets.BaseWidget widget) {
        String[] options = {"Düzenle (Sil)", "Yeni Ekle", "Sırala"};
        
        new android.app.AlertDialog.Builder(getContext())
            .setTitle(widget.getTitle())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Edit (Delete Mode)
                        setEditMode(true);
                        break;
                    case 1: // Add New
                        showAddWidgetDialog();
                        break;
                    case 2: // Sort
                        setSortMode(true);
                        break;
                }
            })
            .show();
    }
    
    // Toggle Edit Mode (Delete Buttons)
    private void setEditMode(boolean enable) {
        if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
            ((WidgetListAdapter) listRecyclerView.getAdapter()).setEditMode(enable);
            if (enable) showDoneButton();
            else removeDoneButton();
        }
    }

    // Toggle Sort Mode (Drag & Drop)
    private void setSortMode(boolean enable) {
        this.isSortMode = enable;
        if (enable) showDoneButton();
        else removeDoneButton();
    }
    
    private View doneButton;
    
    private void showDoneButton() {
        if (rootContent == null) return;
        if (doneButton != null) return; // Already shown
        
        android.widget.Button btn = new android.widget.Button(getContext());
        btn.setText("BİTTİ");
        btn.setBackgroundColor(0xFF4CAF50); // Green
        btn.setTextColor(0xFFFFFFFF);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            btn.setElevation(20);
        }
        
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
             ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        params.setMargins(32, 32, 32, 32);
        
        btn.setLayoutParams(params);
        
        // Draggable Logic
        btn.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View view, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        return true; // Consumed
                    case android.view.MotionEvent.ACTION_MOVE:
                        view.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        // Click detection (if didn't move much)
                        // Simple check: if this was a tap, treat as click.
                        // Ideally we check movement delta. 
                        // For now let's say if UP happens quickly? 
                        // Or just separate ClickListener? OnTouch returning true consumes Click.
                        // Let's handle click here manually.
                        // If moved < 10px?
                         // Re-implementing simplified click:
                         view.performClick();
                         return true;
                }
                return false;
            }
        });
        
        btn.setOnClickListener(v -> {
            setEditMode(false);
            setSortMode(false);
            removeDoneButton();
        });
        
        rootContent.addView(btn);
        doneButton = btn;
    }
    
    private void removeDoneButton() {
        if (doneButton != null && rootContent != null) {
            rootContent.removeView(doneButton);
            doneButton = null;
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
