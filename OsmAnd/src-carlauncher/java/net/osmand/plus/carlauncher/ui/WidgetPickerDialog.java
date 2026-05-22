package net.osmand.plus.carlauncher.ui;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;
import net.osmand.plus.carlauncher.widgets.SpeedWidget;
import net.osmand.plus.carlauncher.widgets.DirectionWidget;
import net.osmand.plus.carlauncher.widgets.MusicWidget;
import net.osmand.plus.carlauncher.widgets.NavigationWidget;
import net.osmand.plus.carlauncher.widgets.OBDWidget;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin;
import net.osmand.plus.plugins.PluginsHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium BottomSheet tabanli Widget Ekleme Arayuzu.
 * Kendi widget'larimizi ve harici Android Sistem Widget'larini (AppWidget) eklemeyi saglar.
 * Android Automotive / Custom ROM'larda coken ACTION_APPWIDGET_PICK yerine
 * kendi premium entegre secim listesini ve programatik bind (ACTION_APPWIDGET_BIND) yapisini kullanir.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class WidgetPickerDialog extends BottomSheetDialogFragment {

    private static final int REQUEST_CREATE_APPWIDGET = 502;
    private static final int REQUEST_BIND_APPWIDGET = 503;

    private WidgetManager widgetManager;
    private Runnable onDismissCallback;
    private OsmandApplication app;
    private int activePageIndex = 0;

    private LinearLayout itemsContainer;
    private TextView titleView;
    private Button btnSystemWidget;
    private boolean showingSystemWidgets = false;
    private int pendingAppWidgetId = -1;

    public void setWidgetManager(WidgetManager wm) {
        this.widgetManager = wm;
    }

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    public void setActivePageIndex(int pageIndex) {
        this.activePageIndex = pageIndex;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = getContext();
        if (ctx == null) return null;

        // Ana Root Container (Premium Koyu Tema)
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(24));
        
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(0xEE1E1E1E); // Yari saydam koyu gri
        rootBg.setCornerRadii(new float[]{dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16), 0, 0, 0, 0});
        root.setBackground(rootBg);

        // Surukleme Barı (Header Handle)
        View handle = new View(ctx);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dpToPx(16);
        handle.setLayoutParams(handleParams);
        
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x44FFFFFF);
        handleBg.setCornerRadius(dpToPx(2));
        handle.setBackground(handleBg);
        root.addView(handle);

        // Baslik
        titleView = new TextView(ctx);
        titleView.setText("Widget Ekle");
        titleView.setTextSize(20);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleView.setPadding(0, 0, 0, dpToPx(16));
        root.addView(titleView);

        // ScrollView
        ScrollView scrollView = new ScrollView(ctx);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);
        
        itemsContainer = new LinearLayout(ctx);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        
        scrollView.addView(itemsContainer);
        root.addView(scrollView);

        // Ayirici cizgi
        View divider = new View(ctx);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
        divParams.setMargins(0, dpToPx(16), 0, dpToPx(16));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(0x22FFFFFF);
        root.addView(divider);

        // Premium Sistem Widget Ekle Butonu
        btnSystemWidget = new Button(ctx);
        btnSystemWidget.setText("Sistem Widgeti Ekle (Spotify, Maps vb.)");
        btnSystemWidget.setTextColor(Color.WHITE);
        btnSystemWidget.setTextSize(14);
        btnSystemWidget.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSystemWidget.setAllCaps(false);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48));
        btnSystemWidget.setLayoutParams(btnParams);

        GradientDrawable btnBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF2196F3, 0xFF00BCD4} // Premium Mavi - Turkuaz Degrade
        );
        btnBg.setCornerRadius(dpToPx(24));
        btnSystemWidget.setBackground(btnBg);
        
        btnSystemWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingSystemWidgets = !showingSystemWidgets;
                updatePickerView();
            }
        });
        root.addView(btnSystemWidget);

        // Ilk listeyi doldur
        updatePickerView();

        return root;
    }

    private void updatePickerView() {
        Context ctx = getContext();
        if (ctx == null || itemsContainer == null || titleView == null || btnSystemWidget == null) return;

        itemsContainer.removeAllViews();

        if (!showingSystemWidgets) {
            titleView.setText("Widget Ekle");
            btnSystemWidget.setText("Sistem Widgeti Ekle (Spotify, Maps vb.)");

            List<WidgetInfo> availableWidgets = getAvailableWidgets();
            for (WidgetInfo info : availableWidgets) {
                itemsContainer.addView(createWidgetRow(ctx, info));
            }
        } else {
            titleView.setText("Sistem Widgeti Secin");
            btnSystemWidget.setText("Geri Don");

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
            List<AppWidgetProviderInfo> providers = null;
            try {
                providers = appWidgetManager.getInstalledProviders();
            } catch (Exception e) {
                android.util.Log.e("WidgetPickerDialog", "getInstalledProviders hatasi: " + e.getMessage());
            }
            android.content.pm.PackageManager pm = ctx.getPackageManager();

            if (providers == null || providers.isEmpty()) {
                TextView noWidgets = new TextView(ctx);
                noWidgets.setText("Yuklu sistem widgeti bulunamadi.");
                noWidgets.setTextColor(Color.GRAY);
                noWidgets.setGravity(Gravity.CENTER);
                noWidgets.setPadding(0, dpToPx(32), 0, dpToPx(32));
                itemsContainer.addView(noWidgets);
                return;
            }

            for (AppWidgetProviderInfo provider : providers) {
                itemsContainer.addView(createSystemWidgetRow(ctx, provider, pm));
            }
        }
    }

    private List<WidgetInfo> getAvailableWidgets() {
        List<WidgetInfo> list = new ArrayList<>();
        Context ctx = getContext();
        if (ctx == null) return list;

        list.add(new WidgetInfo("clock", "Dijital Saat & Tarih", "Masaustu saati ve gunun tarihi (1x1)", BaseWidget.WidgetSize.SMALL));
        list.add(new WidgetInfo("speed", "Hiz & Limit", "Anlik GPS hizi ve yol hiz limiti (1x1)", BaseWidget.WidgetSize.SMALL));
        list.add(new WidgetInfo("direction", "Pusula & Yon", "Surus yonu, derece ve pusula (1x1)", BaseWidget.WidgetSize.SMALL));
        list.add(new WidgetInfo("weather", "Hava Durumu", "Anlik sicaklik ve hava durumu tahmini (2x1)", BaseWidget.WidgetSize.MEDIUM));
        list.add(new WidgetInfo("music", "Medya Calar", "Muzik cover arkaplani, parca kontrolu (2x2)", BaseWidget.WidgetSize.LARGE));
        list.add(new WidgetInfo("navigation", "Navigasyon", "Donus yonleri ve mesafe bilgisi (2x1)", BaseWidget.WidgetSize.MEDIUM));

        // OBD Aktiflik Kontrolu
        VehicleMetricsPlugin obdPlugin = PluginsHelper.getPlugin(VehicleMetricsPlugin.class);
        if (obdPlugin != null && obdPlugin.isActive()) {
            list.add(new WidgetInfo("obd", "OBD Gostergeleri", "Motor RPM, sicaklik ve OBD verileri (2x2)", BaseWidget.WidgetSize.LARGE));
        }

        // Anten Aktiflik Kontrolu
        net.osmand.plus.carlauncher.antenna.AntennaPlugin antennaPlugin = PluginsHelper.getPlugin(net.osmand.plus.carlauncher.antenna.AntennaPlugin.class);
        if (antennaPlugin != null && antennaPlugin.isActive()) {
            list.add(new WidgetInfo("antenna", "Anten Durumu", "Anten baglanti ve sinyal kalitesi (1x1)", BaseWidget.WidgetSize.SMALL));
        }

        return list;
    }

    private View createWidgetRow(final Context ctx, final WidgetInfo info) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        row.setClickable(true);
        row.setFocusable(true);

        // Satir Arkaplani ve Hover Efekti
        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(0x11FFFFFF);
        rowBg.setCornerRadius(dpToPx(8));
        row.setBackground(rowBg);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dpToPx(8);
        row.setLayoutParams(rowParams);

        // Widget Ikon/Renk Alanı (Premium Micro Visual)
        View colorBadge = new View(ctx);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(32));
        badgeParams.rightMargin = dpToPx(12);
        colorBadge.setLayoutParams(badgeParams);
        
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dpToPx(4));
        int badgeColor = 0xFF9E9E9E; // Default gri
        if (info.type.equals("clock")) badgeColor = 0xFFFF9800; // Turuncu
        else if (info.type.equals("speed")) badgeColor = 0xFF4CAF50; // Yesil
        else if (info.type.equals("direction")) badgeColor = 0xFF00BCD4; // Turkuaz
        else if (info.type.equals("weather")) badgeColor = 0xFF2196F3; // Mavi
        else if (info.type.equals("music")) badgeColor = 0xFFE91E63; // Pembe
        else if (info.type.equals("navigation")) badgeColor = 0xFF9C27B0; // Mor
        
        badgeBg.setColor(badgeColor);
        colorBadge.setBackground(badgeBg);
        row.addView(colorBadge);

        // Yazi Alani (Baslik ve Alt Bilgi)
        LinearLayout textContainer = new LinearLayout(ctx);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textContainer.setLayoutParams(textParams);

        TextView txtTitle = new TextView(ctx);
        txtTitle.setText(info.title);
        txtTitle.setTextColor(Color.WHITE);
        txtTitle.setTextSize(15);
        txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textContainer.addView(txtTitle);

        TextView txtDesc = new TextView(ctx);
        txtDesc.setText(info.desc);
        txtDesc.setTextColor(0xAAFFFFFF);
        txtDesc.setTextSize(12);
        textContainer.addView(txtDesc);

        row.addView(textContainer);

        // Ekle Butonu
        Button btnAdd = new Button(ctx);
        btnAdd.setText("Ekle");
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setTextSize(12);
        btnAdd.setAllCaps(false);
        
        LinearLayout.LayoutParams addBtnParams = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(32));
        addBtnParams.leftMargin = dpToPx(8);
        btnAdd.setLayoutParams(addBtnParams);

        GradientDrawable addBtnBg = new GradientDrawable();
        addBtnBg.setColor(0x33FFFFFF);
        addBtnBg.setCornerRadius(dpToPx(16));
        addBtnBg.setStroke(dpToPx(1), 0x55FFFFFF);
        btnAdd.setBackground(addBtnBg);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewWidget(info);
            }
        });
        row.addView(btnAdd);

        return row;
    }

    private View createSystemWidgetRow(final Context ctx, final AppWidgetProviderInfo provider, android.content.pm.PackageManager pm) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        row.setClickable(true);
        row.setFocusable(true);

        // Satir Arkaplani ve Hover Efekti
        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(0x11FFFFFF);
        rowBg.setCornerRadius(dpToPx(8));
        row.setBackground(rowBg);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dpToPx(8);
        row.setLayoutParams(rowParams);

        // Uygulama Ikonu
        android.widget.ImageView iconView = new android.widget.ImageView(ctx);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        iconParams.rightMargin = dpToPx(12);
        iconView.setLayoutParams(iconParams);
        try {
            iconView.setImageDrawable(provider.loadIcon(ctx, ctx.getResources().getDisplayMetrics().densityDpi));
        } catch (Exception e) {
            iconView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        row.addView(iconView);

        // Yazi Alani
        LinearLayout textContainer = new LinearLayout(ctx);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textContainer.setLayoutParams(textParams);

        String label = provider.loadLabel(pm);
        if (label == null || label.trim().isEmpty()) {
            label = provider.provider.getShortClassName();
        }

        TextView txtTitle = new TextView(ctx);
        txtTitle.setText(label);
        txtTitle.setTextColor(Color.WHITE);
        txtTitle.setTextSize(15);
        txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textContainer.addView(txtTitle);

        int spanX = Math.max(1, Math.min(4, Math.round(provider.minWidth / 70f)));
        int spanY = Math.max(1, Math.min(4, Math.round(provider.minHeight / 70f)));
        
        String appName = "";
        try {
            appName = pm.getApplicationLabel(pm.getApplicationInfo(provider.provider.getPackageName(), 0)).toString();
        } catch (Exception e) {
            appName = provider.provider.getPackageName();
        }

        TextView txtDesc = new TextView(ctx);
        txtDesc.setText(appName + " (" + spanX + "x" + spanY + ")");
        txtDesc.setTextColor(0xAAFFFFFF);
        txtDesc.setTextSize(12);
        textContainer.addView(txtDesc);

        row.addView(textContainer);

        // Ekle Butonu
        Button btnAdd = new Button(ctx);
        btnAdd.setText("Ekle");
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setTextSize(12);
        btnAdd.setAllCaps(false);

        LinearLayout.LayoutParams addBtnParams = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(32));
        addBtnParams.leftMargin = dpToPx(8);
        btnAdd.setLayoutParams(addBtnParams);

        GradientDrawable addBtnBg = new GradientDrawable();
        addBtnBg.setColor(0x33FFFFFF);
        addBtnBg.setCornerRadius(dpToPx(16));
        addBtnBg.setStroke(dpToPx(1), 0x55FFFFFF);
        btnAdd.setBackground(addBtnBg);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSystemWidgetSelected(provider);
            }
        });
        row.addView(btnAdd);

        return row;
    }

    private void addNewWidget(WidgetInfo info) {
        if (widgetManager == null || getContext() == null || app == null) return;

        BaseWidget widget = null;
        if (info.type.equals("clock")) {
            widget = new net.osmand.plus.carlauncher.widgets.Material3ClockWidget(getContext());
        } else if (info.type.equals("speed")) {
            widget = new SpeedWidget(getContext(), app);
        } else if (info.type.equals("direction")) {
            widget = new DirectionWidget(getContext(), app);
        } else if (info.type.equals("weather")) {
            widget = new net.osmand.plus.carlauncher.widgets.WeatherWidget(getContext(), app);
        } else if (info.type.equals("music")) {
            widget = new MusicWidget(getContext(), app);
        } else if (info.type.equals("navigation")) {
            widget = new NavigationWidget(getContext(), app);
        } else if (info.type.equals("obd")) {
            widget = new OBDWidget(getContext(), app);
        } else if (info.type.equals("antenna")) {
            widget = new net.osmand.plus.carlauncher.widgets.AntennaWidget(getContext(), app);
        }

        if (widget != null) {
            // Bos bir alan bulup aktif sayfaya veya uygun ilk sayfaya yerlestirilecek
            widget.setPageIndex(activePageIndex);
            widget.setCellX(-1);
            widget.setCellY(-1);
            widget.setSize(info.defaultSize);
            
            widgetManager.addWidget(widget);
            Toast.makeText(getContext(), widget.getTitle() + " eklendi.", Toast.LENGTH_SHORT).show();
            dismiss();
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        }
    }

    private void addSystemWidgetSelected(AppWidgetProviderInfo provider) {
        if (widgetManager == null || getContext() == null) return;
        AppWidgetHost host = widgetManager.getAppWidgetHost();
        if (host == null) return;

        try {
            int appWidgetId = host.allocateAppWidgetId();
            pendingAppWidgetId = appWidgetId;
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
            
            // Programatik olarak bind etmeye calis
            boolean allowed = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider);
            if (allowed) {
                configureOrAddSystemWidget(appWidgetId, provider);
            } else {
                // Launcher bind yetkisine sahip degilse, sistem bind onay ekranini ac
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Sistem widgeti eklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void configureOrAddSystemWidget(int appWidgetId, AppWidgetProviderInfo info) {
        if (info.configure != null) {
            try {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(info.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Widget konfigurasyon ekrani acilamadi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Konfigurasyon acilamazsa yine de widget'i eklemeyi dene
                addSystemAppWidgetToWorkspace(appWidgetId);
            }
        } else {
            addSystemAppWidgetToWorkspace(appWidgetId);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Context ctx = getContext();
        if (ctx == null) return;

        if (requestCode == REQUEST_BIND_APPWIDGET) {
            int appWidgetId = (data != null) ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : pendingAppWidgetId;
            if (appWidgetId == -1) appWidgetId = pendingAppWidgetId;
            
            if (resultCode == Activity.RESULT_OK && appWidgetId != -1) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
                AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
                if (info != null) {
                    configureOrAddSystemWidget(appWidgetId, info);
                } else {
                    addSystemAppWidgetToWorkspace(appWidgetId);
                }
            } else {
                if (appWidgetId != -1 && widgetManager != null && widgetManager.getAppWidgetHost() != null) {
                    try {
                        widgetManager.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
                    } catch (Exception e) {
                        android.util.Log.e("WidgetPickerDialog", "deleteAppWidgetId hatasi: " + e.getMessage());
                    }
                }
            }
            pendingAppWidgetId = -1;
            return;
        }

        if (requestCode == REQUEST_CREATE_APPWIDGET) {
            int appWidgetId = (data != null) ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : pendingAppWidgetId;
            if (appWidgetId == -1) appWidgetId = pendingAppWidgetId;

            if (resultCode == Activity.RESULT_OK && appWidgetId != -1) {
                addSystemAppWidgetToWorkspace(appWidgetId);
            } else {
                if (appWidgetId != -1 && widgetManager != null && widgetManager.getAppWidgetHost() != null) {
                    try {
                        widgetManager.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
                    } catch (Exception e) {
                        android.util.Log.e("WidgetPickerDialog", "deleteAppWidgetId hatasi: " + e.getMessage());
                    }
                }
            }
            pendingAppWidgetId = -1;
        }
    }

    private void addSystemAppWidgetToWorkspace(int appWidgetId) {
        Context ctx = getContext();
        if (widgetManager != null && ctx != null) {
            net.osmand.plus.carlauncher.widgets.SystemAppWidget widget = 
                new net.osmand.plus.carlauncher.widgets.SystemAppWidget(ctx, appWidgetId);
            widget.setPageIndex(activePageIndex); 
            widget.setCellX(-1);
            widget.setCellY(-1);
            
            // AppWidgetProviderInfo'ya bakarak boyutu dinamik ata
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
            if (info != null) {
                int spanX = Math.max(1, Math.min(4, Math.round(info.minWidth / 70f)));
                int spanY = Math.max(1, Math.min(4, Math.round(info.minHeight / 70f)));
                if (spanX == 1 && spanY == 1) {
                    widget.setSize(BaseWidget.WidgetSize.SMALL);
                } else if (spanX <= 2 && spanY == 1) {
                    widget.setSize(BaseWidget.WidgetSize.MEDIUM);
                } else {
                    widget.setSize(BaseWidget.WidgetSize.LARGE);
                }
            } else {
                widget.setSize(BaseWidget.WidgetSize.MEDIUM); // Default fallback
            }

            widgetManager.addWidget(widget);
            Toast.makeText(ctx, "Sistem widgeti eklendi.", Toast.LENGTH_SHORT).show();
            dismiss();
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private static class WidgetInfo {
        String type;
        String title;
        String desc;
        BaseWidget.WidgetSize defaultSize;

        WidgetInfo(String type, String title, String desc, BaseWidget.WidgetSize defaultSize) {
            this.type = type;
            this.title = title;
            this.desc = desc;
            this.defaultSize = defaultSize;
        }
    }
}
