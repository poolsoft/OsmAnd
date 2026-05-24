package net.osmand.plus.carlauncher.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Android Sistem Widget'larini (Spotify, Google Haritalar vb.)
 * Car Launcher widget panelinde gostermek icin sarmalayici sinif.
 * 
 * Widget provider'larin bazilarinin (BatteryGuru, Google Keep, Weather vb.)
 * initialLayout'lari bizim context'imizde inflate edilemiyor.
 * Bu yuzden createView() sonrasinda provider'a ACTION_APPWIDGET_UPDATE
 * broadcast'i gondererek gercek RemoteViews'i tetikliyoruz.
 * 
 * hostView bir kez olusturulur ve cache'lenir, her sayfa degisiminde
 * yeniden olusturulmaz.
 * 
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

        // Eger daha once olusturulmus bir hostView varsa, onu tekrar kullan.
        // Bu sayede ViewPager2 sayfa degisimlerinde widget yeniden olusturulmaz
        // ve provider'in gonderdigi RemoteViews kaybolmaz.
        if (hostView != null) {
            rootView = hostView;
            return hostView;
        }

        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(currentContext);
            AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);

            if (appWidgetInfo == null) {
                return createErrorView(currentContext, "Widget bulunamadi (ID: " + appWidgetId + ")");
            }

            AppWidgetHost host = WidgetManager.getInstance(currentContext).getAppWidgetHost();
            if (host == null) {
                return createErrorView(currentContext, "Widget Host hazir degil");
            }

            // HostView olustur
            hostView = host.createView(currentContext, appWidgetId, appWidgetInfo);
            hostView.setAppWidget(appWidgetId, appWidgetInfo);

            // Layout parametrelerini ayarla
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            hostView.setLayoutParams(params);

            rootView = hostView;

            // Provider'a "guncelleme yolla" broadcast'i gonder.
            // BatteryGuru, Google Keep, Weather gibi veri-bagimlisi widget'lar
            // initialLayout inflate basarisiz olsa bile, bu broadcast sayesinde
            // gercek RemoteViews'lerini gonderecekler ve hostView otomatik guncellenecek.
            try {
                Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.setComponent(appWidgetInfo.provider);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
                currentContext.sendBroadcast(updateIntent);
            } catch (Exception broadcastErr) {
                android.util.Log.w("SystemAppWidget", "Widget update broadcast gonderilemedi: " + broadcastErr.getMessage());
            }

            return hostView;

        } catch (Exception e) {
            android.util.Log.e("SystemAppWidget", "Widget olusturulurken hata: " + e.getMessage());
            return createErrorView(currentContext, "Yukleme hatasi: " + e.getLocalizedMessage());
        }
    }

    private View createErrorView(Context ctx, String errorMessage) {
        TextView errView = new TextView(ctx);
        errView.setText(errorMessage);
        errView.setTextColor(0xFFFF3333);
        errView.setPadding(16, 16, 16, 16);
        errView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        FrameLayout container = new FrameLayout(ctx);
        container.setBackgroundColor(0x33FF0000);
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
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (hostView != null) {
            hostView = null;
        }
    }
}
