package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;
import net.osmand.plus.carlauncher.widgets.SpeedWidget;
import net.osmand.plus.carlauncher.widgets.DirectionWidget;
import net.osmand.plus.carlauncher.widgets.MusicWidget;
import net.osmand.plus.carlauncher.widgets.NavigationWidget;
import net.osmand.plus.carlauncher.widgets.OBDWidget;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.carlauncher.AutoLaunchManager;
import net.osmand.plus.OsmandApplication;

/**
 * Widget paneli fragment.
 * GRID LAYOUT IMPLEMENTATION WITH SEPARATE PORTRAIT/LANDSCAPE SETTINGS.
 */
public class WidgetPanelFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "WidgetPanelFragment";

    private RecyclerView listRecyclerView;
    private WidgetManager widgetManager;
    private OsmandApplication app;
    private ViewGroup rootContent;
    private View widgetContentFrame;
    
    private boolean isPinned = true; 
    private static final String PREF_IS_PINNED = "widget_panel_pinned";
    private int currentUnitSize = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
            widgetManager = WidgetManager.getInstance(getContext());
            widgetManager.forceResetForNewSession(); // Clean start
            widgetManager.updateActivityContext(getContext());
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            isPinned = prefs.getBoolean(PREF_IS_PINNED, true);

            
            if (!widgetManager.loadWidgetConfig()) {
                initializeWidgets(); // Load default if empty
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // XML Layout Implementation (Engineering Recommendation)
        View root;
        try {
            root = inflater.inflate(net.osmand.plus.R.layout.fragment_widget_panel, container, false);
        } catch (Exception e) {
            // Fallback if XML missing in rare case
            return createProgrammaticView();
        }

        listRecyclerView = root.findViewById(net.osmand.plus.R.id.widget_recycler_view);
        View menuBtn = root.findViewById(net.osmand.plus.R.id.btn_widget_menu);
        
        widgetContentFrame = root;
        rootContent = (ViewGroup) root;

        initListLayout();
        setupMenuButton(menuBtn);
        
        return root;
    }
    
    // Fallback Method (just in case)
    private View createProgrammaticView() {
        FrameLayout contentFrame = new FrameLayout(getContext());
        contentFrame.setBackgroundColor(0xFF111111);
        listRecyclerView = new RecyclerView(getContext());
        contentFrame.addView(listRecyclerView);
        return contentFrame;
    }

    private void initListLayout() {
        if (listRecyclerView == null) return;
        
        // Layout Manager (Grid)
        listRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        
        // Post configuration update
        listRecyclerView.post(() -> {
              if (getView() != null) {
                  updateLayoutConfiguration(); 
                  applyWidgetsToView();
              }
         });
    }

    private void setupMenuButton(View menuBtn) {
        if (menuBtn == null) return;
        
        menuBtn.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), menuBtn);
            
            // 1. Edit Widgets
            popup.getMenu().add(0, 1, 0, "Widget Duzenle");
            
            // 2. Layout Mode Submenu
            android.view.Menu layoutMenu = popup.getMenu().addSubMenu(0, 3, 1, "Görünüm");
            android.view.MenuItem itemClassic = layoutMenu.add(0, 31, 0, "Klasik (Liste)");
            android.view.MenuItem itemMetro = layoutMenu.add(0, 32, 1, "Metro (Izgara)");
            
            itemClassic.setCheckable(true);
            itemMetro.setCheckable(true);
            
            CarLauncherSettings settings = new CarLauncherSettings(getContext());
            boolean isMetro = settings.isMetroMode();
            if (isMetro) itemMetro.setChecked(true);
            else itemClassic.setChecked(true);
            
            // 3. Pin Toggle
            android.view.MenuItem pinItem = popup.getMenu().add(0, 2, 2, "Sabitle (Pinned)");
            pinItem.setCheckable(true);
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            isPinned = prefs.getBoolean(PREF_IS_PINNED, true);
            pinItem.setChecked(isPinned);
            
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1) { // Edit
                    showWidgetControlDialog();
                    return true;
                } else if (id == 2) { // Pin
                    isPinned = !isPinned;
                    item.setChecked(isPinned);
                    PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit().putBoolean(PREF_IS_PINNED, isPinned).apply();

                    if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {

                        ((net.osmand.plus.activities.MapActivity) getActivity()).updateWidgetPanelMode();
                    }
                    return true;
                } else if (id == 31) { // Classic
                    if (settings.isMetroMode()) {
                        settings.setMetroMode(false);
                        updateLayoutConfiguration();
                        applyWidgetsToView();
                    }
                    return true;
                } else if (id == 32) { // Metro
                    if (!settings.isMetroMode()) {
                        settings.setMetroMode(true);
                        updateLayoutConfiguration();
                        applyWidgetsToView();
                    }
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void showWidgetControlDialog() {
        WidgetControlDialog dialog = new WidgetControlDialog();
        dialog.setWidgetManager(widgetManager);
        dialog.setOnDismissCallback(() -> {
            updateLayoutConfiguration();
            applyWidgetsToView();
        });
        dialog.show(getChildFragmentManager(), "WidgetControlDialog");
    }

    private void updateLayoutConfiguration() {
        if (listRecyclerView == null || !(listRecyclerView.getLayoutManager() instanceof GridLayoutManager)) return;
        
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        int systemOrientation = getResources().getConfiguration().orientation;
        boolean isSystemPortrait = systemOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        boolean isHorizontalScroll = isSystemPortrait;
        
        boolean isMetro = settings.isMetroMode();
        int slots;

        if (isMetro) {
            slots = 4; // Metro Mode: Fixed 4-column Grid
        } else {
             // CLASSIC Mode: Slots determines "Items Per Screen" for sizing, NOT columns.
             // Columns should always be 1 (List).
             if (isSystemPortrait) {
                // Portrait mode: Avoid cramming too many widgets
                slots = Math.min(settings.getPortraitSlotCount(), 3); 
                if (slots < 2) slots = 2; // Min 2 for better visibility
            } else {
                slots = settings.getLandscapeSlotCount();
            }
        }

        GridLayoutManager glm = (GridLayoutManager) listRecyclerView.getLayoutManager();
        boolean changed = false;
        
        // Determine actual Span Count for Layout Manager
        int targetSpanCount = isMetro ? 4 : 1; 
        
        if (glm.getSpanCount() != targetSpanCount) {
            glm.setSpanCount(targetSpanCount);
            changed = true;
        }
        
        int targetOrientation = isHorizontalScroll ? GridLayoutManager.HORIZONTAL : GridLayoutManager.VERTICAL;
        if (glm.getOrientation() != targetOrientation) {
            glm.setOrientation(targetOrientation);
            changed = true;
        }
        
        // Custom Span Lookup for Metro Mode
        if (isMetro) {
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                        WidgetListAdapter adapter = (WidgetListAdapter) listRecyclerView.getAdapter();
                        if (position < adapter.getItemCount()) {
                            net.osmand.plus.carlauncher.widgets.BaseWidget w = adapter.getWidgetAt(position);
                            if (w != null) {
                                switch (w.getSize()) {
                                    case MEDIUM: return 2; // 2x1 (Wide)
                                    case LARGE: return 2;  // 2x2 (Big)
                                    default: return 1;     // 1x1 (Small)
                                }
                            }
                        }
                    }
                    return 1;
                }
            });
        } else {
            glm.setSpanSizeLookup(new GridLayoutManager.DefaultSpanSizeLookup());
        }

        if (changed && listRecyclerView.getAdapter() != null) {
            listRecyclerView.getAdapter().notifyDataSetChanged();
        }
        
        updateUnitSize();
    }
    
    private void updateUnitSize() {
        if (listRecyclerView == null) return;
        
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        boolean isMetro = settings.isMetroMode();
        
        int systemOrientation = getResources().getConfiguration().orientation;
        boolean isSystemPortrait = systemOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        boolean isHorizontalScroll = isSystemPortrait;
        
        // Recalculate slots locally for unit math
        int slots;
        if (isMetro) {
            slots = 4;
        } else {
             if (isSystemPortrait) slots = settings.getPortraitSlotCount();
             else slots = settings.getLandscapeSlotCount();
        }
        if (slots <= 0) slots = 1;

        if (isHorizontalScroll) {
             // Horizontal Scroll: Width is flexible
             int width = getView() != null ? getView().getWidth() : 0;
             if (width > 0) {
                 currentUnitSize = width / slots;
             }
        } else {
             // Vertical Scroll (Landscape)
             if (isMetro) {
                 // Metro Vertical: Unit should be based on WIDTH to form squares
                 // Total Width / Slots (4) = Unit Width
                 int width = getView() != null ? getView().getWidth() : 0;
                 if (width > 0) {
                     currentUnitSize = width / slots;
                 }
             } else {
                 // Classic Vertical: Unit based on HEIGHT (List style)
                 int height = listRecyclerView.getHeight();
                 if (height == 0 && getView() != null) height = getView().getHeight();
                 
                 if (height > 0) {
                     currentUnitSize = height / slots;
                 } else {
                      currentUnitSize = (int) android.util.TypedValue.applyDimension(
                         android.util.TypedValue.COMPLEX_UNIT_DIP, 85, getResources().getDisplayMetrics());
                 }
             }
        }
        
        if (listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
            ((WidgetListAdapter) listRecyclerView.getAdapter()).setMetroMode(isMetro);
            ((WidgetListAdapter) listRecyclerView.getAdapter()).setUnitSize(currentUnitSize, isHorizontalScroll);
        }
    }
    
    private androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper;

    private void applyWidgetsToView() {
        if (listRecyclerView != null) {
            CarLauncherSettings settings = new CarLauncherSettings(getContext());
            int systemOrientation = getResources().getConfiguration().orientation;
            boolean isSystemPortrait = systemOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            boolean isHorizontalScroll = isSystemPortrait;
            
            java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> visibleWidgets = widgetManager.getVisibleWidgets();
            for (net.osmand.plus.carlauncher.widgets.BaseWidget w : visibleWidgets) {
                if (getActivity() != null) w.setContext(getActivity());
            }

            WidgetListAdapter adapter = new WidgetListAdapter(
                visibleWidgets, 
                isHorizontalScroll, 
                new WidgetListAdapter.OnWidgetActionListener() {
                    @Override public void onWidgetLongClicked(View view, net.osmand.plus.carlauncher.widgets.BaseWidget widget) { }
                    @Override public void onAddWidgetClicked() { 
                         showWidgetControlDialog(); 
                    }
                    @Override public void onWidgetOrderChanged(java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> newOrder) {
                        widgetManager.updateVisibleOrder(newOrder); 
                    }
                    @Override public void onWidgetRemoved(net.osmand.plus.carlauncher.widgets.BaseWidget widget) {
                        widgetManager.removeWidget(widget);
                        applyWidgetsToView(); 
                    }
                    @Override public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                        if (itemTouchHelper != null) {
                            itemTouchHelper.startDrag(viewHolder);
                        }
                    }
                }
            );
            
            updateUnitSize(); // Ensure size is calculated
            if (currentUnitSize > 0) {
                adapter.setUnitSize(currentUnitSize, isHorizontalScroll);
            }
            listRecyclerView.setAdapter(adapter);
            
             androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                @Override public boolean isLongPressDragEnabled() { return false; } // Handled manually via onStartDrag
                @Override public boolean isItemViewSwipeEnabled() { return false; }
                @Override public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    int dragFlags = androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN |
                                    androidx.recyclerview.widget.ItemTouchHelper.START | androidx.recyclerview.widget.ItemTouchHelper.END;
                    return makeMovementFlags(dragFlags, 0);
                }
                @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                    adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }
                @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
                
                @Override
                public void onSelectedChanged(@androidx.annotation.Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG) {
                         if (viewHolder instanceof WidgetListAdapter.WidgetViewHolder) {
                            ((WidgetListAdapter.WidgetViewHolder) viewHolder).setDragState(true);
                             // Haptic feedback
                            viewHolder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        }
                    }
                }

                @Override public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    
                    if (viewHolder instanceof WidgetListAdapter.WidgetViewHolder) {
                        ((WidgetListAdapter.WidgetViewHolder) viewHolder).setDragState(false);
                    }
                    
                    if (recyclerView.getAdapter() instanceof WidgetListAdapter) {
                        WidgetListAdapter adapter = (WidgetListAdapter) recyclerView.getAdapter();
                         if (widgetManager != null) {
                            widgetManager.updateVisibleOrder(adapter.getWidgets());
                        }
                    }
                }
            };
            itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(listRecyclerView);
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);
        }
        if (getContext() != null) {
            WidgetManager wm = WidgetManager.getInstance(getContext());
            if (!wm.isHasAutoLaunched()) {
                wm.setHasAutoLaunched(true);
                new AutoLaunchManager(getContext()).execute();
            }
        }
        if (widgetManager != null) {
            widgetManager.startAllWidgets();
            // Adapter mevcutsa yeniden olusturma — sadece listeyi guncelle (RAM tasarrufu)
            if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                ((WidgetListAdapter) listRecyclerView.getAdapter())
                        .refresh(widgetManager.getVisibleWidgets());
                updateLayoutConfiguration();
            } else {
                applyWidgetsToView();
            }
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
        if (widgetManager != null) {
            widgetManager.stopAllWidgets();
        }
    }


    private void initializeWidgets() {
        if (widgetManager == null || app == null) return;
        
        widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.Material3ClockWidget(getContext()));
        widgetManager.addWidget(new SpeedWidget(getContext(), app));
        widgetManager.addWidget(new DirectionWidget(getContext(), app));
        
        net.osmand.plus.carlauncher.antenna.AntennaPlugin antennaPlugin = net.osmand.plus.plugins.PluginsHelper
                .getPlugin(net.osmand.plus.carlauncher.antenna.AntennaPlugin.class);
        if (antennaPlugin != null && antennaPlugin.isActive()) {
            widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.AntennaWidget(getContext(), app));
        }
        
        widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.WeatherWidget(getContext(), app));
        widgetManager.addWidget(new NavigationWidget(getContext(), app));
        widgetManager.addWidget(new MusicWidget(getContext(), app));
        
        VehicleMetricsPlugin obdPlugin = PluginsHelper.getPlugin(VehicleMetricsPlugin.class);
        if (obdPlugin != null && obdPlugin.isActive()) {
            widgetManager.addWidget(new OBDWidget(getContext(), app));
        }
    }

    public WidgetManager getWidgetManager() {
        return widgetManager;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (CarLauncherSettings.KEY_WIDGET_SLOTS_PORTRAIT.equals(key) || 
            CarLauncherSettings.KEY_WIDGET_SLOTS_LANDSCAPE.equals(key)) {
            updateLayoutConfiguration();
            applyWidgetsToView();
        }
    }
}
