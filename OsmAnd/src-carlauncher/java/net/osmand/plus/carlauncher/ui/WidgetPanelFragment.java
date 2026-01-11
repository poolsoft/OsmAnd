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
import android.preference.PreferenceManager;
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

    private void initListLayout(ViewGroup root) {
        listRecyclerView = new RecyclerView(getContext());
        listRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listRecyclerView.setClipToPadding(false);
        listRecyclerView.setPadding(0, 0, 0, 0); 
        
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        boolean isSystemPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        
        int scrollDir;
        if (isSystemPortrait) {
            scrollDir = settings.getPortraitScrollDirection(); // 0:Horz, 1:Vert
        } else {
            scrollDir = settings.getLandscapeScrollDirection();
        }
        
        int spanCount = 1; // Initial dummy
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount, 
                                                                    scrollDir == 0 ? GridLayoutManager.HORIZONTAL : GridLayoutManager.VERTICAL, 
                                                                    false);
                                                                    
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
                    WidgetListAdapter adapter = (WidgetListAdapter) listRecyclerView.getAdapter();
                    if (position >= 0 && position < adapter.getItemCount()) {
                         try {
                             BaseWidget widget = adapter.getWidgetAt(position);
                             if (widget != null) {
                                 BaseWidget.WidgetSize size = widget.getSize();
                                 if (size == BaseWidget.WidgetSize.LARGE) return layoutManager.getSpanCount();
                                 if (size == BaseWidget.WidgetSize.MEDIUM) return layoutManager.getSpanCount() > 1 ? 2 : 1;
                             }
                         } catch (Exception e) { }
                    }
                }
                return 1;
            }
        });
        
        listRecyclerView.setLayoutManager(layoutManager);
        root.addView(listRecyclerView);
        
         listRecyclerView.post(() -> {
              if (getView() != null) {
                  updateLayoutConfiguration(); 
                  applyWidgetsToView();
              }
         });
        
        applyWidgetsToView();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
            widgetManager = WidgetManager.getInstance(getContext());
            widgetManager.forceResetForNewSession(); // Clean start
            widgetManager.updateActivityContext(getContext());
            
            android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
            isPinned = prefs.getBoolean(PREF_IS_PINNED, true);
            
            if (!widgetManager.loadWidgetConfig()) {
                initializeWidgets(); // Load default if empty
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout contentFrame = new FrameLayout(getContext());
        contentFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_panel_modern);
        contentFrame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        widgetContentFrame = contentFrame;
        rootContent = contentFrame;
        
        initListLayout(contentFrame);
        setupMenuButton(contentFrame);
        
        return contentFrame;
    }

    private void setupMenuButton(ViewGroup root) {
        android.widget.ImageView menuBtn = new android.widget.ImageView(getContext());
        menuBtn.setImageResource(net.osmand.plus.R.drawable.ic_more_vert);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            menuBtn.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
             android.util.TypedValue outValue = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            menuBtn.setBackgroundResource(outValue.resourceId);
        }
        menuBtn.setPadding(16, 16, 16, 16);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
             ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        params.setMargins(4, 4, 4, 4);
        root.addView(menuBtn, params);
        
        menuBtn.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), menuBtn);
            popup.getMenu().add(0, 1, 0, "Widget Duzenle");
            android.view.MenuItem pinItem = popup.getMenu().add(0, 2, 0, "Sabitle (Pinned)");
            pinItem.setCheckable(true);
            pinItem.setChecked(isPinned);
            
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    showWidgetControlDialog();
                    return true;
                } else if (item.getItemId() == 2) {
                    isPinned = !isPinned;
                    item.setChecked(isPinned);
                    android.preference.PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit().putBoolean(PREF_IS_PINNED, isPinned).apply();
                    if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                        ((net.osmand.plus.activities.MapActivity) getActivity()).updateWidgetPanelMode();
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
        dialog.show(getParentFragmentManager(), "WidgetControl");
    }

    private void updateLayoutConfiguration() {
        if (listRecyclerView == null || !(listRecyclerView.getLayoutManager() instanceof GridLayoutManager)) return;
        
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        int systemOrientation = getResources().getConfiguration().orientation;
        boolean isSystemPortrait = systemOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        
        int slots;
        int orientationPref; // 0=Auto, 1=Horizontal, 2=Vertical
        
        if (isSystemPortrait) {
            slots = settings.getPortraitSlotCount();
            orientationPref = settings.getPortraitScrollDirection();
        } else {
            slots = settings.getLandscapeSlotCount();
            orientationPref = settings.getLandscapeScrollDirection();
        }
        
        // Resolve Target Scroll Direction (Horizontal/Vertical)
        boolean isHorizontalScroll;
        if (orientationPref == 1) {
            isHorizontalScroll = true; // Force Horizontal
        } else if (orientationPref == 2) {
            isHorizontalScroll = false; // Force Vertical
        } else {
            // Auto (0): Portrait -> Horizontal (Bottom), Landscape -> Vertical (Right)
            isHorizontalScroll = isSystemPortrait; 
        }

        GridLayoutManager glm = (GridLayoutManager) listRecyclerView.getLayoutManager();
        boolean changed = false;
        
        if (glm.getSpanCount() != slots) {
            glm.setSpanCount(slots);
            changed = true;
        }
        
        int targetOrientation = isHorizontalScroll ? GridLayoutManager.HORIZONTAL : GridLayoutManager.VERTICAL;
        if (glm.getOrientation() != targetOrientation) {
            glm.setOrientation(targetOrientation);
            changed = true;
        }
        
        if (changed && listRecyclerView.getAdapter() != null) {
            listRecyclerView.getAdapter().notifyDataSetChanged();
        }
        
        updateUnitSize();
    }
    
    private void updateUnitSize() {
        if (listRecyclerView == null) return;
        
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        int systemOrientation = getResources().getConfiguration().orientation;
        boolean isSystemPortrait = systemOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        
        int slots;
        int orientationPref;
        
        if (isSystemPortrait) {
            slots = settings.getPortraitSlotCount();
            orientationPref = settings.getPortraitScrollDirection();
        } else {
            slots = settings.getLandscapeSlotCount();
            orientationPref = settings.getLandscapeScrollDirection();
        }

        boolean isHorizontalScroll;
        if (orientationPref == 1) {
            isHorizontalScroll = true;
        } else if (orientationPref == 2) {
            isHorizontalScroll = false;
        } else {
            isHorizontalScroll = isSystemPortrait;
        }
        
        if (isHorizontalScroll) {
             // Horizontal Scroll: Width is flexible
             int width = getView() != null ? getView().getWidth() : 0;
             if (width > 0) {
                 if (slots > 0) currentUnitSize = width / slots;
             }
        } else {
             // Vertical Scroll: Height is flexible
             int height = listRecyclerView.getHeight();
             if (height == 0 && getView() != null) height = getView().getHeight();
             
             if (height > 0) {
                 if (slots > 0) currentUnitSize = height / slots;
             } else {
                  currentUnitSize = (int) android.util.TypedValue.applyDimension(
                     android.util.TypedValue.COMPLEX_UNIT_DIP, 85, getResources().getDisplayMetrics());
             }
        }
    }
    
    private void applyWidgetsToView() {
        if (listRecyclerView != null) {
            CarLauncherSettings settings = new CarLauncherSettings(getContext());
            int systemOrientation = getResources().getConfiguration().orientation;
            boolean isSystemPortrait = systemOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            int orientationPref = isSystemPortrait ? settings.getPortraitScrollDirection() : settings.getLandscapeScrollDirection();
            
            boolean isHorizontalScroll;
            if (orientationPref == 1) isHorizontalScroll = true;
            else if (orientationPref == 2) isHorizontalScroll = false;
            else isHorizontalScroll = isSystemPortrait;
            
            WidgetListAdapter adapter = new WidgetListAdapter(
                widgetManager.getVisibleWidgets(), 
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
                }
            );
            
            updateUnitSize(); // Ensure size is calculated
            if (currentUnitSize > 0) {
                adapter.setUnitSize(currentUnitSize, isHorizontalScroll);
            }
            listRecyclerView.setAdapter(adapter);
            
             androidx.recyclerview.widget.ItemTouchHelper.Callback callback = new androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                @Override public boolean isLongPressDragEnabled() { return true; }
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
                @Override public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
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
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
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
            applyWidgetsToView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
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
            CarLauncherSettings.KEY_WIDGET_SLOTS_LANDSCAPE.equals(key) ||
            CarLauncherSettings.KEY_WIDGET_SCROLL_PORTRAIT.equals(key) ||
            CarLauncherSettings.KEY_WIDGET_SCROLL_LANDSCAPE.equals(key)) {
            updateLayoutConfiguration();
            applyWidgetsToView();
        }
    }
}
