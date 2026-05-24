package net.osmand.plus.carlauncher.widgets;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Ozel AppWidgetHostView sinifi.
 *
 * Android Framework'unde bir widget'in initialLayout'u bizim context'imizde
 * inflate edilemediginde getErrorView() cagrilir. Bunun yerine
 * "Yukleniyor..." gostererek provider'in gercek RemoteViews'ini
 * gondermesini bekliyoruz ve belirli araliklarla yeniden deniyoruz.
 *
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class CarAppWidgetHostView extends AppWidgetHostView {

    private static final int MAX_RETRY = 5;
    private static final int RETRY_DELAY_MS = 3000; // 3 saniye arayla
    private int retryCount = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public CarAppWidgetHostView(Context context) {
        super(context);
    }

    @Override
    protected View getErrorView() {
        // Log dosyasina yaz (sadece ilk seferde, log'u sismemesi icin)
        if (retryCount == 0) {
            writeErrorToLogFile("AppWidgetId " + getAppWidgetId() + " icin initialLayout inflate edilemedi. " +
                    "Provider'in RemoteViews gondermesini bekliyoruz. Retry basliyor...");
        }

        // "Yukleniyor..." gorseli olustur
        TextView loadingView = new TextView(getContext());
        loadingView.setText("Widget yukleniyor...\n(" + (retryCount + 1) + "/" + MAX_RETRY + ")");
        loadingView.setTextColor(0xAAFFFFFF);
        loadingView.setTextSize(11);
        loadingView.setPadding(16, 16, 16, 16);
        loadingView.setGravity(Gravity.CENTER);

        FrameLayout container = new FrameLayout(getContext());
        container.setBackgroundColor(0x33FFFFFF); // Hafif yari saydam
        container.addView(loadingView);

        // Otomatik yeniden deneme zamanlayicisi
        if (retryCount < MAX_RETRY) {
            retryCount++;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Provider'a tekrar update broadcast'i gonder
                        AppWidgetProviderInfo info = getAppWidgetInfo();
                        if (info != null && getContext() != null) {
                            Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            updateIntent.setComponent(info.provider);
                            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{getAppWidgetId()});
                            getContext().sendBroadcast(updateIntent);
                        }
                    } catch (Exception e) {
                        android.util.Log.w("CarAppWidgetHostView", "Retry broadcast hatasi: " + e.getMessage());
                    }
                }
            }, RETRY_DELAY_MS);
        } else {
            // Tum denemeler bitti, kalici hata goster
            loadingView.setText("Widget yanitlamiyor.\nBu widget uygulamasi\nbizim launcher'da\ncalismayabilir.");
            loadingView.setTextColor(0xAAFF6666);
            container.setBackgroundColor(0x22FF0000);

            writeErrorToLogFile("AppWidgetId " + getAppWidgetId() + " icin " + MAX_RETRY + " deneme yapildi. " +
                    "Provider RemoteViews gondermedi. Widget bu launcher'da calismayabilir. " +
                    "Provider: " + (getAppWidgetInfo() != null ? getAppWidgetInfo().provider.toString() : "null"));
        }

        return container;
    }

    private void writeErrorToLogFile(String errorMsg) {
        try {
            if (getContext() == null) return;
            File logDir = getContext().getExternalFilesDir(null);
            if (logDir != null) {
                File logFile = new File(logDir, "carlauncher_widget_error.log");
                FileWriter fw = new FileWriter(logFile, true);
                PrintWriter pw = new PrintWriter(fw);
                pw.println("--- " + new Date().toString() + " ---");
                pw.println(errorMsg);
                pw.println();
                pw.close();
                fw.close();
            }
        } catch (Exception ex) {
            // Ignore
        }
    }
}
