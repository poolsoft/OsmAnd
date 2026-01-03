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
    private View slidingContainer; // The container that moves
    private android.widget.ImageButton handleBtn;
    private View widgetContentFrame; // The frame holding widgets
    
    private boolean isPinned = true; 
    private static final String PREF_IS_PINNED = "widget_panel_pinned";
    private int currentUnitSize = 0;

    private void initListLayout(ViewGroup root) {
        listRecyclerView = new RecyclerView(getContext());
        listRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listRecyclerView.setClipToPadding(false);
        // Bottom padding avoids overlap with bottom elements regarding of orientation
        listRecyclerView.setPadding(0, 0, 0, 0); 
        
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), isPortrait ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL, false);
        listRecyclerView.setLayoutManager(layoutManager);
        
        root.addView(listRecyclerView);
        
         // Initial Calculation
         listRecyclerView.post(() -> {
              if (getView() != null) {
                  // Dynamic Unit Size Calculation (V13)
                  updateUnitSize();
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
            widgetManager = new WidgetManager(getContext(), app);
            
            android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
            isPinned = prefs.getBoolean(PREF_IS_PINNED, true);
            
            if (!widgetManager.loadWidgetConfig()) {
                initializeWidgets();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // V8: Clean Content Frame (No internal handle, no sliding container)
        // Activity handles sliding/drawer via ConstraintLayout.
        
        FrameLayout contentFrame = new FrameLayout(getContext());
        contentFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_panel_modern);
        contentFrame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        widgetContentFrame = contentFrame;
        rootContent = contentFrame;
        
        initListLayout(contentFrame);
        setupMenuButton(contentFrame);
        
        return contentFrame;
    }
    
    // Internal Toggle removed - Handled by MapActivity
    public void onPanelStateChanged(boolean isOpen) {
        // Optional: Update internal state if needed
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
                    // Save preference
                    android.preference.PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit().putBoolean(PREF_IS_PINNED, isPinned).apply();
                        
                    // Notify Activity to update Layout
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

    // ... (initListLayout updates for Expand Button) ...
    // Inside initListLayout, we need to handle "Expand" button visibility logic. 
    // Wait, Expand button is part of `fragment_music_player` (Full Screen) or `widget_music`?
    // User said "Expand button ... in full screen mode". This likely refers to `MusicPlayerFragment`.
    // Let's check `MusicPlayerFragment` for the expand/collapse button.
    
    // BUT, if the user means the "Full Screen Mode" of the LAUNCHER (Map + Widgets), 
    // they might be referring to an existing button on the UI.
    // Screenshot 3 shows a "Mode 2" toast. 
    // And screenshot 2 shows a button with "Arrows Out" (Fullscreen?) near the bottom right map controls?
    // Or in the music widget itself?
    // Let's assume it's the Music Widget's "Launch Full Screen" button.
    // That button is inside `MusicWidget`.


    private void showWidgetControlDialog() {
        WidgetControlDialog dialog = new WidgetControlDialog();
        dialog.setWidgetManager(widgetManager);
        dialog.setOnDismissCallback(() -> {
            updateUnitSize(); // Recalculate based on new pref
            applyWidgetsToView();
            // WidgetManager saves automatically on changes now (V12)
        });
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

    private void updateUnitSize() {
        if (listRecyclerView == null) return;
        
        boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        
        if (isPortrait) {
             // Portrait: Vertical Stack (Horizontal Scroll) -> Unit = Width / Slots?
             // No, Portrait is "Horizontal List", so Unit is Width.
             // Actually, usually in horizontal scroll, "Unit" is the width of one item.
             // User requested "3 Small items visible".
             // So Unit = ScreenWidth / 3 ?
             // Current logic: currentUnitSize = getView().getWidth().
             // Let's keep Portrait as "1 Page Width" for now unless user wants split.
             // Wait, previous logic was `currentUnitSize = isPortrait ? getView().getWidth() : defaultUnitPx`.
             // If we want dynamic slotting in Portrait, we should use similar logic.
             // But for now let's focus on Landscape (Vertical Stack) as per request.
             
             // Check preference
             android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
             int slots = prefs.getInt("widget_slot_count", 3);
             
             // Portrait Mode (Horizontal List):
             // If we want multiple items on screen, we divide Width by Slots.
             currentUnitSize = getView() != null ? (getView().getWidth() / slots) : 0;
             // If 0, fallback?
             if (currentUnitSize == 0 && getView() != null) currentUnitSize = getView().getWidth();
             
        } else {
             // Landscape: Vertical Stack
             int height = listRecyclerView.getHeight();
             if (height == 0 && getView() != null) height = getView().getHeight();
             
             android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
             int slots = prefs.getInt("widget_slot_count", 3);
             
             if (height > 0) {
                 currentUnitSize = height / slots;
             } else {
                 // Fallback if height not ready (e.g. 85dp * scale?)
                 // Use 85dp as base for 3 slots ~ 255dp height.
                 currentUnitSize = (int) android.util.TypedValue.applyDimension(
                     android.util.TypedValue.COMPLEX_UNIT_DIP, 
                     85, 
                     getResources().getDisplayMetrics()
                 );
             }
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
            applyWidgetsToView(); // Ensure view is refreshed on resume
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
