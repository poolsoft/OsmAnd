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

        // Force List Layout (Unified)
        initListLayout(mainFrame);

        // Register Receiver for Mode Change (Legacy support - kept for safety if external intents trigger it)
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
        
        // Setup Top-Right Menu Button
        setupMenuButton(mainFrame);

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
        LinearSnapHelper snapHelper = new StartSnapHelper(); 
        snapHelper.attachToRecyclerView(listRecyclerView);
        
        root.addView(listRecyclerView);
    }
    
    // ... (This assumes I'm just replacing the helper instantiation block)

    // --- GLOBAL MENU ---
    private void setupMenuButton(ViewGroup root) {
        android.widget.ImageView menuBtn = new android.widget.ImageView(getContext());
        menuBtn.setImageResource(android.R.drawable.ic_menu_more);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            menuBtn.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        }
        menuBtn.setBackgroundColor(0x44000000); // Semi-transparent bg
        menuBtn.setPadding(16, 16, 16, 16);
        
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
             ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        params.setMargins(16, 16, 16, 16);
        
        root.addView(menuBtn, params);
        
        menuBtn.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), menuBtn);
            popup.getMenu().add(0, 1, 0, "Widget Ekle");
            popup.getMenu().add(0, 2, 0, "Düzenle (Sil)");
            // popup.getMenu().add(0, 3, 0, "Ayarlar"); // Future
            
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        showAddWidgetDialog();
                        return true;
                    case 2:
                        setEditMode(true);
                        return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // --- ADD WIDGET DIALOG ---
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
        
        // 0: Small (1/3), 1: Medium (1/2), 2: Large (Full)
        // Note: Descriptions are for Landscape (Vertical List).
        // For Portrait: Small (1/2), Medium (?), Large (?) -> Maybe generic text?
        // "Küçük", "Orta", "Büyük"
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
            // Unique ID generation or handling could be here, but Manager likely uses widget.getId() which is type-based?
            // If multiple of same type allowed, WidgetFactory/Registry creates distinct instances but IDs?
            // BaseWidget constructor usually takes type as ID.
            // If we want multiple clocks, ID needs to be unique.
            // Let's modify ID to be type + timestamp if needed.
            // Checking BaseWidget/Implementation... usually ID is fixed prefix.
            // Hack for uniqueness:
            try {
                java.lang.reflect.Field idField = BaseWidget.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(widget, type + "_" + System.currentTimeMillis());
            } catch (Exception e) {
                // Ignore
            }
            
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
            
            WidgetListAdapter adapter = new WidgetListAdapter(
                widgetManager.getVisibleWidgets(), 
                isPortrait,
                new WidgetListAdapter.OnWidgetActionListener() {
                    @Override
                    public void onWidgetLongClicked(View view, net.osmand.plus.carlauncher.widgets.BaseWidget widget) {
                        // Unused (Drag handled by ItemTouchHelper)
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
                }
            };
            
            androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(listRecyclerView);
            
        }
    }
    
    // Toggle Edit Mode (Delete Buttons)
    private void setEditMode(boolean enable) {
        if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
            ((WidgetListAdapter) listRecyclerView.getAdapter()).setEditMode(enable);
            if (enable) showDoneButton();
            else removeDoneButton();
        }
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
                         view.performClick();
                         return true;
                }
                return false;
            }
        });
        
        btn.setOnClickListener(v -> {
            setEditMode(false);
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

    private static class StartSnapHelper extends LinearSnapHelper {
        private androidx.recyclerview.widget.OrientationHelper mVerticalHelper, mHorizontalHelper;

        @Override
        public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager,
                                                  @NonNull View targetView) {
            int[] out = new int[2];
            if (layoutManager.canScrollHorizontally()) {
                out[0] = distanceToStart(targetView, getHorizontalHelper(layoutManager));
            } else {
                out[0] = 0;
            }
            if (layoutManager.canScrollVertically()) {
                out[1] = distanceToStart(targetView, getVerticalHelper(layoutManager));
            } else {
                out[1] = 0;
            }
            return out;
        }

        @Override
        public View findSnapView(RecyclerView.LayoutManager layoutManager) {
            if (layoutManager instanceof LinearLayoutManager) {
                if (layoutManager.canScrollHorizontally()) {
                    return findStartView(layoutManager, getHorizontalHelper(layoutManager));
                } else {
                    return findStartView(layoutManager, getVerticalHelper(layoutManager));
                }
            }
            return super.findSnapView(layoutManager);
        }

        private int distanceToStart(View targetView, androidx.recyclerview.widget.OrientationHelper helper) {
            return helper.getDecoratedStart(targetView) - helper.getStartAfterPadding();
        }

        private View findStartView(RecyclerView.LayoutManager layoutManager,
                                   androidx.recyclerview.widget.OrientationHelper helper) {
            int childCount = layoutManager.getChildCount();
            if (childCount == 0) {
                return null;
            }
            View closestChild = null;
            int startest = Integer.MAX_VALUE;

            for (int i = 0; i < childCount; i++) {
                final View child = layoutManager.getChildAt(i);
                int childStart = helper.getDecoratedStart(child);
                int distance = Math.abs(childStart - helper.getStartAfterPadding());

                if (distance < startest) {
                    startest = distance;
                    closestChild = child;
                }
            }
            return closestChild;
        }

        private androidx.recyclerview.widget.OrientationHelper getVerticalHelper(RecyclerView.LayoutManager layoutManager) {
            if (mVerticalHelper == null || mVerticalHelper.getLayoutManager() != layoutManager) {
                mVerticalHelper = androidx.recyclerview.widget.OrientationHelper.createVerticalHelper(layoutManager);
            }
            return mVerticalHelper;
        }

        private androidx.recyclerview.widget.OrientationHelper getHorizontalHelper(RecyclerView.LayoutManager layoutManager) {
            if (mHorizontalHelper == null || mHorizontalHelper.getLayoutManager() != layoutManager) {
                mHorizontalHelper = androidx.recyclerview.widget.OrientationHelper.createHorizontalHelper(layoutManager);
            }
            return mHorizontalHelper;
        }
    }
}
