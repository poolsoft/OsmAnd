package net.osmand.plus.carlauncher.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Android Sistem Widget'larini (Spotify, Google Haritalar vb.)
 * Car Launcher widget panelinde gostermek icin sarmalayici sinif.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class SystemAppWidget extends BaseWidget {

    private final int appWidgetId;
    private AppWidgetHostView hostView;

    public SystemAppWidget(@NonNull Context context, int appWidgetId) {
        super(context, "appwidget_" + appWidgetId, "Sistem Widget");
        this.appWidgetId = appWidgetId;
        this.size = WidgetSize.MEDIUM; // Varsayilan olarak orta boy
    }

    public int getAppWidgetId() {
        return appWidgetId;
    }

    @NonNull
    @Override
    public View createView() {
        Context currentContext = getContext();
        if (currentContext == null) {
            currentContext = context;
        }

        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(currentContext);
            AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);

            if (appWidgetInfo == null) {
                // Widget bilgisi bulunamadi hatasi (Silinmis veya gecersiz ID)
                return createErrorView(currentContext, "Widget bulunamadi (ID: " + appWidgetId + ")");
            }

            // AppWidgetHost ornegini WidgetManager uzerinden aliyoruz
            AppWidgetHost host = WidgetManager.getInstance(currentContext).getAppWidgetHost();
            if (host == null) {
                return createErrorView(currentContext, "Widget Host hazir degil");
            }

            // HostView olustur ve yerlestir
            hostView = host.createView(currentContext, appWidgetId, appWidgetInfo);
            hostView.setAppWidget(appWidgetId, appWidgetInfo);
            
            try {
                android.os.Bundle options = new android.os.Bundle();
                // 3. parti widgetlar icin sadece kategori yolluyoruz, min/max boyutlarini Android Framework yonetsin.
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
                hostView.updateAppWidgetOptions(options);
                appWidgetManager.updateAppWidgetOptions(appWidgetId, options);
            } catch (Exception ignored) {}
            
            // Layout parametrelerini ayarla
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            hostView.setLayoutParams(params);

            rootView = hostView;
            return hostView;

        } catch (Exception e) {
            android.util.Log.e("SystemAppWidget", "Widget olusturulurken hata: " + e.getMessage());
            return createErrorView(currentContext, "Yukleme hatasi: " + e.getLocalizedMessage());
        }
    }

    private View createErrorView(Context ctx, String errorMessage) {
        TextView errView = new TextView(ctx);
        errView.setText(errorMessage);
        errView.setTextColor(0xFFFF3333); // Kirmizi hata rengi
        errView.setPadding(16, 16, 16, 16);
        errView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        
        FrameLayout container = new FrameLayout(ctx);
        container.setBackgroundColor(0x33FF0000); // Yari saydam kirmizi arka plan
        container.addView(errView);
        
        rootView = container;
        return container;
    }

    @Override
    public void update() {
        // Sistem widget'lari kendi kendilerini gunceller, ekstra tetiklemeye gerek yoktur.
    }

    @Override
    public void onStart() {
        super.onStart();
        // Gerekli baslatma islemleri
    }

    @Override
    public void onStop() {
        super.onStop();
        // Durdurma islemleri
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (hostView != null) {
            hostView = null;
        }
    }
}
