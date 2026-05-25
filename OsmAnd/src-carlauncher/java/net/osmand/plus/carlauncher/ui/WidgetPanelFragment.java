package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import net.osmand.plus.carlauncher.CarLauncherInterface;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.WorkspacePageAdapter;

/**
 * Cok Sayfali Premium Grid Widget Workspace Fragment.
 * ViewPager2 tabanli, 4x4 Grid sayfali ve premium micro-indicator animasyonlu.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class WidgetPanelFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "WidgetPanelFragment";

    private androidx.viewpager2.widget.ViewPager2 viewPager;
    private android.widget.LinearLayout pageIndicator;
    private WidgetManager widgetManager;
    private OsmandApplication app;
    private ViewGroup rootContent;
    private View widgetContentFrame;
    private android.widget.ImageView parallaxBg;
    
    private boolean isPinned = true; 
    private static final String PREF_IS_PINNED = "widget_panel_pinned";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
            widgetManager = WidgetManager.getInstance(getContext());
            widgetManager.forceResetForNewSession(); // Temiz baslangic
            widgetManager.updateActivityContext(getContext());
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            isPinned = prefs.getBoolean(PREF_IS_PINNED, true);

            if (!widgetManager.loadWidgetConfig()) {
                initializeWidgets(); // Default widget'lari yukle
            }
            widgetManager.updateActivityContext(getContext()); // Yukleme sonrasinda context'leri guncelle
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root;
        try {
            root = inflater.inflate(net.osmand.plus.R.layout.fragment_widget_panel, container, false);
        } catch (Exception e) {
            return createProgrammaticView();
        }

        viewPager = root.findViewById(net.osmand.plus.R.id.widget_view_pager);
        pageIndicator = root.findViewById(net.osmand.plus.R.id.workspace_page_indicator);
        View menuBtn = root.findViewById(net.osmand.plus.R.id.btn_widget_menu);
        
        // Parallax arka plan baglantisi
        parallaxBg = root.findViewById(net.osmand.plus.R.id.workspace_parallax_bg);
        updateBackgroundStyle();
        
        // Bottom Navigation
        View navWidgets = root.findViewById(net.osmand.plus.R.id.nav_widgets);
        View navNavigation = root.findViewById(net.osmand.plus.R.id.nav_navigation);
        View navApps = root.findViewById(net.osmand.plus.R.id.nav_apps);
        View navSettings = root.findViewById(net.osmand.plus.R.id.nav_settings);
        
        setupBottomNav(navWidgets, navNavigation, navApps, navSettings);
        
        widgetContentFrame = root;
        rootContent = (ViewGroup) root;
 
        initListLayout();
        setupMenuButton(menuBtn);
        setupViewPagerCallback();
        
        return root;
    }
    
    private View createProgrammaticView() {
        FrameLayout contentFrame = new FrameLayout(getContext());
        contentFrame.setBackgroundColor(0xFF111111);
        
        // Programatik parallax arka plan
        parallaxBg = new android.widget.ImageView(getContext());
        parallaxBg.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        contentFrame.addView(parallaxBg, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        updateBackgroundStyle();

        viewPager = new androidx.viewpager2.widget.ViewPager2(getContext());
        contentFrame.addView(viewPager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setupViewPagerCallback();
        return contentFrame;
    }

    private void initListLayout() {
        if (viewPager == null) return;
        viewPager.post(() -> {
            if (getView() != null) {
                applyWidgetsToView();
            }
        });
    }

    private void setupMenuButton(View menuBtn) {
        if (menuBtn == null) return;
        
        menuBtn.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), menuBtn);
            
            popup.getMenu().add(0, 1, 0, "Widget Ekle (Yeni)");
            popup.getMenu().add(0, 2, 1, "Launcher Ayarlari");
            
            // Profil Secenekleri
            android.view.Menu presetsMenu = popup.getMenu().addSubMenu(0, 3, 2, "Widget Profilleri");
            presetsMenu.add(0, 31, 0, "Navigasyon Odakli");
            presetsMenu.add(0, 32, 1, "Medya Odakli");
            presetsMenu.add(0, 33, 2, "Minimalist");
            presetsMenu.add(0, 34, 3, "Kullanici Secimi");
            
            popup.getMenu().add(0, 4, 3, "Mevcut Duzeni Kaydet");
            
            android.view.MenuItem pinItem = popup.getMenu().add(0, 5, 4, "Sabitle (Pinned)");
            pinItem.setCheckable(true);
            pinItem.setChecked(isPinned);
            
            popup.getMenu().add(0, 6, 5, "Masaustunu Duzenle (Edit Mode)");
            
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1) {
                    showWidgetControlDialog();
                    return true;
                } else if (id == 2) {
                    if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                        ((net.osmand.plus.activities.MapActivity) getActivity()).openCarLauncherSettings();
                    }
                    return true;
                } else if (id == 5) {
                    isPinned = !isPinned;
                    item.setChecked(isPinned);
                    PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit().putBoolean(PREF_IS_PINNED, isPinned).apply();
 
                    if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                        ((net.osmand.plus.activities.MapActivity) getActivity()).updateWidgetPanelMode();
                    }
                    return true;
                } else if (id == 6) {
                    if (viewPager != null && viewPager.getAdapter() instanceof WorkspacePageAdapter) {
                        WorkspacePageAdapter adapter = (WorkspacePageAdapter) viewPager.getAdapter();
                        WorkspacePageAdapter.isEditMode = true;
                        adapter.notifyDataSetChanged();
                        if (adapter.getEditModeListener() != null) {
                            adapter.getEditModeListener().onEditModeChanged(true);
                        }
                    }
                    return true;
                } else if (id == 31) {
                    applyLayoutPreset(LayoutPreset.NAVIGATION);
                    return true;
                } else if (id == 32) {
                    applyLayoutPreset(LayoutPreset.MEDIA);
                    return true;
                } else if (id == 33) {
                    applyLayoutPreset(LayoutPreset.MINIMALIST);
                    return true;
                } else if (id == 34) {
                    applyLayoutPreset(LayoutPreset.USER);
                    return true;
                } else if (id == 4) {
                    if (widgetManager != null) {
                        widgetManager.saveUserLayout();
                        if (getView() != null) {
                            getView().performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                        }
                        android.widget.Toast.makeText(getContext(), "Mevcut widget duzeni basariyla kaydedildi", android.widget.Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private enum LayoutPreset {
        NAVIGATION, MEDIA, MINIMALIST, USER
    }

    private void applyLayoutPreset(LayoutPreset preset) {
        if (widgetManager == null || getContext() == null) return;

        widgetManager.stopAllWidgets();
        widgetManager.forceResetForNewSession();
        widgetManager.updateActivityContext(getContext());

        switch (preset) {
            case NAVIGATION:
                widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.Material3ClockWidget(getContext()));
                widgetManager.addWidget(new SpeedWidget(getContext(), app));
                widgetManager.addWidget(new DirectionWidget(getContext(), app));
                widgetManager.addWidget(new NavigationWidget(getContext(), app));
                break;
            case MEDIA:
                widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.Material3ClockWidget(getContext()));
                widgetManager.addWidget(new MusicWidget(getContext(), app));
                widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.WeatherWidget(getContext(), app));
                break;
            case MINIMALIST:
                widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.Material3ClockWidget(getContext()));
                widgetManager.addWidget(new SpeedWidget(getContext(), app));
                break;
            case USER:
                if (!widgetManager.loadUserLayout()) {
                    android.widget.Toast.makeText(getContext(), "Kaydedilmis kullanici duzeni bulunamadi", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }

        widgetManager.saveWidgetConfig();
        widgetManager.startAllWidgets();
        applyWidgetsToView();
        
        if (getView() != null) {
            getView().performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
        }
    }

    private void showWidgetControlDialog() {
        WidgetPickerDialog dialog = new WidgetPickerDialog();
        dialog.setWidgetManager(widgetManager);
        if (viewPager != null) {
            dialog.setActivePageIndex(viewPager.getCurrentItem());
        }
        dialog.setOnDismissCallback(() -> {
            int targetPage = viewPager != null ? viewPager.getCurrentItem() : 0;
            // Let the post method inside applyWidgetsToView find the exact final page index after binding
            applyWidgetsToView(targetPage);
        });
        dialog.show(getChildFragmentManager(), "WidgetPickerDialog");
    }

    private void applyWidgetsToView() {
        applyWidgetsToView(-1);
    }

    private void applyWidgetsToView(final int forcePageIndex) {
        if (viewPager != null) {
            final int currentItem = forcePageIndex >= 0 ? forcePageIndex : viewPager.getCurrentItem();
            java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> visibleWidgets = widgetManager.getVisibleWidgets();
            for (net.osmand.plus.carlauncher.widgets.BaseWidget w : visibleWidgets) {
                if (getActivity() != null) w.setContext(getActivity());
            }

            if (viewPager.getAdapter() instanceof WorkspacePageAdapter) {
                final WorkspacePageAdapter adapter = (WorkspacePageAdapter) viewPager.getAdapter();
                adapter.updateWidgetsList(visibleWidgets);
                updatePageIndicator();

                viewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        int targetPage = currentItem;
                        java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> list = widgetManager.getVisibleWidgets();
                        if (!list.isEmpty()) {
                            net.osmand.plus.carlauncher.widgets.BaseWidget lastAdded = list.get(list.size() - 1);
                            targetPage = lastAdded.getPageIndex();
                        }
                        int count = adapter.getItemCount();
                        targetPage = Math.max(0, Math.min(count - 1, targetPage));
                        viewPager.setCurrentItem(targetPage, false);
                    }
                });
            } else {
                final WorkspacePageAdapter adapter = new WorkspacePageAdapter(
                    getContext(),
                    getChildFragmentManager(),
                    visibleWidgets,
                    new Runnable() {
                        @Override
                        public void run() {
                            updatePageIndicator();
                        }
                    }
                );
                adapter.setEditModeListener(new WorkspacePageAdapter.EditModeListener() {
                    @Override
                    public void onEditModeChanged(boolean isEditMode) {
                        viewPager.setUserInputEnabled(!isEditMode);
                        if (isEditMode) {
                            android.widget.Toast.makeText(getContext(), 
                                "Duzenleme Modu Aktif. Cikmak icin bos alana tiklayin.", 
                                android.widget.Toast.LENGTH_LONG).show();
                        } else {
                            android.widget.Toast.makeText(getContext(), 
                                "Duzenlemeler Kaydedildi.", 
                                android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                viewPager.setAdapter(adapter);
                
                setupPageIndicator(adapter.getItemCount());

                // Sayfayi asenkron olarak hedef sayfa konumuna kaydir ve koru
                viewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        int targetPage = currentItem;
                        java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> list = widgetManager.getVisibleWidgets();
                        if (!list.isEmpty()) {
                            // Adapter render olduktan sonra eger yer yoktuysa widget baska sayfaya tasinmis olabilir
                            net.osmand.plus.carlauncher.widgets.BaseWidget lastAdded = list.get(list.size() - 1);
                            targetPage = lastAdded.getPageIndex();
                        }
                        int count = adapter.getItemCount();
                        targetPage = Math.max(0, Math.min(count - 1, targetPage));
                        viewPager.setCurrentItem(targetPage, false);
                    }
                });
            }
        }
    }

    private void setupPageIndicator(int count) {
        if (pageIndicator == null) return;
        pageIndicator.removeAllViews();
        int margin = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()),
                    (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())
            );
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            
            // Programatik GradientDrawable (Sifir risk, maksimum premium gorunum)
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(0x55FFFFFF);
            dot.setBackground(gd);
            
            pageIndicator.addView(dot);
        }
        updatePageIndicatorSelection(0);
    }

    private void updatePageIndicatorSelection(int selectedPosition) {
        if (pageIndicator == null) return;
        for (int i = 0; i < pageIndicator.getChildCount(); i++) {
            View dot = pageIndicator.getChildAt(i);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            
            android.view.ViewGroup.LayoutParams params = dot.getLayoutParams();
            if (i == selectedPosition) {
                gd.setColor(0xFFFFFFFF);
                params.width = (int) android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
            } else {
                gd.setColor(0x55FFFFFF);
                params.width = (int) android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            }
            dot.setBackground(gd);
            dot.setLayoutParams(params);
        }
    }

    private void setupViewPagerCallback() {
        if (viewPager == null) return;
        viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (parallaxBg != null) {
                    float totalScroll = position + positionOffset;
                    int width = viewPager.getWidth() > 0 ? viewPager.getWidth() : getResources().getDisplayMetrics().widthPixels;
                    float intensity = 20f;
                    if (getContext() != null) {
                        CarLauncherSettings settings = new CarLauncherSettings(getContext());
                        intensity = settings.getParallaxIntensity();
                    }
                    float translationX = -totalScroll * width * (intensity / 100f); // Premium parallax kaydirma katsayisi
                    parallaxBg.setTranslationX(translationX);
                }
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePageIndicatorSelection(position);
            }
        });
    }

    private void updatePageIndicator() {
        if (viewPager != null && viewPager.getAdapter() != null) {
            setupPageIndicator(viewPager.getAdapter().getItemCount());
            updatePageIndicatorSelection(viewPager.getCurrentItem());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBackgroundStyle();
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
            boolean isPanelOpen = true;
            if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                isPanelOpen = ((net.osmand.plus.activities.MapActivity) getActivity()).isWidgetPanelOpen();
            }
            
            if (isPanelOpen) {
                widgetManager.startAllWidgets();
            }
            
            if (viewPager != null && viewPager.getAdapter() != null) {
                viewPager.getAdapter().notifyDataSetChanged();
                updatePageIndicator();
            } else {
                applyWidgetsToView();
            }
        }
    }

    public void onPanelVisibilityChanged(boolean visible) {
        if (widgetManager != null) {
            if (visible) {
                widgetManager.startAllWidgets();
                if (viewPager != null && viewPager.getAdapter() != null) {
                    viewPager.getAdapter().notifyDataSetChanged();
                    updatePageIndicator();
                }
            } else {
                widgetManager.stopAllWidgets();
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

    private void setupBottomNav(View navWidgets, View navNavigation, View navApps, View navSettings) {
        setActiveNav(navWidgets);

        if (navWidgets != null) {
            navWidgets.setOnClickListener(v -> {
                setActiveNav(navWidgets);
            });
        }
        if (navNavigation != null) {
            navNavigation.setOnClickListener(v -> {
                setActiveNav(navNavigation);
            });
        }
        if (navApps != null) {
            navApps.setOnClickListener(v -> {
                setActiveNav(navApps);
                if (getActivity() instanceof net.osmand.plus.carlauncher.CarLauncherInterface) {
                    CarLauncherInterface ci = (CarLauncherInterface) getActivity();
                    ci.setPanelContent(PanelContentManager.PanelContent.APP_DRAWER);
                    ci.openAppDrawer();
                }
            });
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                setActiveNav(navSettings);
                if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                    ((net.osmand.plus.activities.MapActivity) getActivity()).openCarLauncherSettings();
                }
            });
        }
    }

    private void setActiveNav(View active) {
        View root = getView();
        if (root == null) return;
        int[] navIds = {net.osmand.plus.R.id.nav_widgets, net.osmand.plus.R.id.nav_navigation, 
                        net.osmand.plus.R.id.nav_apps, net.osmand.plus.R.id.nav_settings};
        for (int id : navIds) {
            View v = root.findViewById(id);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(0xFF888888);
            }
        }
        if (active instanceof TextView) {
            ((TextView) active).setTextColor(0xFFFFFFFF);
        }
    }

    public WidgetManager getWidgetManager() {
        return widgetManager;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (CarLauncherSettings.KEY_BACKGROUND_STYLE.equals(key)) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::updateBackgroundStyle);
            }
        }
    }

    private void updateBackgroundStyle() {
        if (parallaxBg == null || getContext() == null) return;
        CarLauncherSettings settings = new CarLauncherSettings(getContext());
        String style = settings.getBackgroundStyle();
        int resId = net.osmand.plus.R.drawable.bg_panel_modern;
        if ("carbon".equals(style)) {
            resId = net.osmand.plus.R.drawable.bg_panel_carbon;
        } else if ("space".equals(style)) {
            resId = net.osmand.plus.R.drawable.bg_panel_space;
        }
        parallaxBg.setImageResource(resId);
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getContext() != null && widgetManager != null) {
            widgetManager.updateActivityContext(getContext());
            widgetManager.loadWidgetConfig();
            applyWidgetsToView();
        }
    }
}
