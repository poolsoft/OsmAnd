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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.WidgetFactory;
import net.osmand.plus.carlauncher.widgets.WidgetManager;

import java.util.List;

/**
 * Widget paneli fragment.
 * Widget'lari gosterir ve yonetir.
 * Artik Multi-Instance + Grid Layout destekler (Phase 6).
 */
public class WidgetPanelFragment extends Fragment {

    public static final String TAG = "WidgetPanelFragment";

    private RecyclerView listRecyclerView;
    private ViewPager2 widgetViewPager;
    private TabLayout tabLayout;
    private WidgetManager widgetManager;
    private OsmandApplication app;
    private BroadcastReceiver modeChangeReceiver;
    
    // UI Elements
    private android.widget.Button doneButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
            widgetManager = new WidgetManager(getContext(), app);
            // Load config or migrate from legacy
            widgetManager.loadWidgetConfig();
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

        // Determine Mode
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        int mode = settings.getWidgetDisplayMode(); // 0: List, 1: Paged

        if (mode == 1) {
            initPagedLayout(mainFrame);
        } else {
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (widgetManager != null) {
            applyWidgetsToView();
        }
    }
    
    // --- MODE 0: LIST (GRID) LAYOUT ---
    private void initListLayout(ViewGroup root) {
        boolean isPortrait = getResources()
                .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

        listRecyclerView = new RecyclerView(getContext());
        listRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // GRID Layout Manager with 4 Columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        
        // Horizontal Scroll for Portrait, Vertical for Landscape?
        // User said: "Grid Layout with 4 column structure"
        // Landscape: 4 Columns wide. Vertical Scroll. (Standard)
        // Portrait: 4 Columns wide? Or 4 Rows? User said "dynamically adapt".
        // Let's stick to Vertical Scroll for now as it handles height better.
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        
        listRecyclerView.setLayoutManager(layoutManager);
        
        root.addView(listRecyclerView);
    }

    // --- MODE 1: PAGED LAYOUT (Legacy support for now) ---
    private void initPagedLayout(ViewGroup root) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        widgetViewPager = new ViewPager2(getContext());
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        widgetViewPager.setLayoutParams(startParams);
        
        tabLayout = new TabLayout(getContext());
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tabLayout.setLayoutParams(tabParams);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        tabLayout.setBackgroundColor(0x00000000);
        tabLayout.setSelectedTabIndicatorColor(0xFFFFFFFF);
        
        container.addView(widgetViewPager);
        container.addView(tabLayout);
        root.addView(container);
    }

    private void applyWidgetsToView() {
        if (listRecyclerView != null) {
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            
            // Adapter with Callbacks
            WidgetListAdapter adapter = new WidgetListAdapter(
                widgetManager.getVisibleWidgets(), 
                isPortrait, 
                new WidgetListAdapter.OnWidgetActionListener() {
                    @Override
                    public void onWidgetOrderChanged(List<BaseWidget> newOrder) {
                         widgetManager.updateVisibleOrder(newOrder);
                    }
                    
                    @Override
                    public void onWidgetRemoved(BaseWidget widget) {
                        widgetManager.removeWidget(widget); // Completely remove instance
                        applyWidgetsToView(); // Rebind
                        
                        // Keep Edit Mode
                        restoreEditMode();
                    }
                    
                    @Override
                    public void onWidgetSizeChanged(BaseWidget widget) {
                        widgetManager.saveWidgetConfig();
                        // Grid Layout needs invalidation to recalculate spans potentially?
                        // Adapter notifyItemChanged should handle it, but LayoutManager SpanSizeLookup needs to trigger.
                        // GridLayoutManager auto-invalidates span on notifyItemChanged usually.
                    }
                    
                    @Override
                    public void onAddWidgetClicked() {
                        showAddWidgetDialog();
                    }
                    
                    @Override
                    public void onEditModeRequested() {
                        restoreEditMode();
                    }
                }
            );
            
            // Set Span Size Lookup
            if (listRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
                GridLayoutManager glm = (GridLayoutManager) listRecyclerView.getLayoutManager();
                glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return adapter.getSpanSize(position);
                    }
                });
            }
            
            listRecyclerView.setAdapter(adapter);
            
            // Attach Drag & Drop (Grid Compatible)
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
            
            new TabLayoutMediator(tabLayout, widgetViewPager,
                    (tab, position) -> {}
            ).attach();
        }
    }
    
    private void restoreEditMode() {
        if (listRecyclerView != null && listRecyclerView.getAdapter() instanceof WidgetListAdapter) {
            ((WidgetListAdapter) listRecyclerView.getAdapter()).setEditMode(true);
            if (doneButton != null) doneButton.setVisibility(View.VISIBLE);
        }
    }

    private void showAddWidgetDialog() {
        if (getContext() == null || widgetManager == null) return;

        // List of Available Widget Types (Factories)
        // Multi-Instance allows adding multiple of same type
        final String[] types = {
            WidgetFactory.TYPE_CLOCK,
            WidgetFactory.TYPE_SPEED,
            WidgetFactory.TYPE_NAVIGATION,
            WidgetFactory.TYPE_MUSIC,
            WidgetFactory.TYPE_COMPASS,
            WidgetFactory.TYPE_ANTENNA,
            WidgetFactory.TYPE_OBD
        };
        
        final String[] names = {
            "Saat",
            "Hız",
            "Navigasyon",
            "Müzik",
            "Pusula/Yön",
            "Anten",
            "Araç Verileri (OBD)"
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Yeni Widget Ekle");
        builder.setItems(names, (dialog, which) -> {
            String selectedType = types[which];
            widgetManager.addWidgetByType(selectedType);
            
            // Refresh
            applyWidgetsToView();
            restoreEditMode();
        });

        builder.setNegativeButton("İptal", null);
        builder.show();
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
            } catch (Exception e) {}
        }
        if (widgetManager != null) {
            widgetManager.saveWidgetConfig();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (widgetManager != null) widgetManager.startAllWidgets();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (widgetManager != null) widgetManager.stopAllWidgets();
    }

    public WidgetManager getWidgetManager() {
        return widgetManager;
    }
}
