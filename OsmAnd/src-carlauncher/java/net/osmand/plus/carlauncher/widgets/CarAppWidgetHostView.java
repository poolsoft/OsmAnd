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
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Ozel AppWidgetHostView sinifi.
 *
 * updateAppWidget(RemoteViews) metodunu override ederek:
 * - RemoteViews NULL ise: super.updateAppWidget(null) cagirmaz
 *   (cunku o getDefaultView -> getErrorView zincirine duser).
 *   Bunun yerine kendi "Yukleniyor" view'ini gosterir ve
 *   provider'a broadcast gondererek RemoteViews istemesini saglar.
 * - RemoteViews doluysa: normal akisa devam eder.
 *
 * Bu sayede provider'in RemoteViews gondermesini BEKLERIZ
 * ve hata gostermek yerine yukleniyor ekrani gosteririz.
 *
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class CarAppWidgetHostView extends AppWidgetHostView {

    private static final int MAX_RETRY = 8;
    private static final int RETRY_DELAY_MS = 4000; // 4 saniye arayla
    private int retryCount = 0;
    private boolean hasReceivedRemoteViews = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public CarAppWidgetHostView(Context context) {
        super(context);
    }

    /**
     * Android Framework bu metodu iki durumda cagirabilir:
     * 1. host.createView() icinden (ilk olusturma - genelde null gelir)
     * 2. Provider guncelleme gonderdiginde (gercek RemoteViews gelir)
     *
     * Null geldiginde super.updateAppWidget(null) cagirmak
     * getDefaultView() -> initialLayout inflate -> basarisiz -> getErrorView()
     * zincirine duser. Biz bunu engelliyoruz.
     */
    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        if (remoteViews != null) {
            // Provider gercek RemoteViews gonderdi!
            hasReceivedRemoteViews = true;
            retryCount = MAX_RETRY; // Retry'i durdur
            handler.removeCallbacksAndMessages(null); // Bekleyen retry'lari iptal et

            writeToLog("BASARILI: AppWidgetId " + getAppWidgetId() +
                    " icin RemoteViews alindi (package: " + remoteViews.getPackage() + ")");

            try {
                super.updateAppWidget(remoteViews);
            } catch (Exception e) {
                // RemoteViews.apply() basarisiz oldu - tam stack trace logla
                writeToLog("APPLY HATASI: AppWidgetId " + getAppWidgetId() +
                        "\n" + android.util.Log.getStackTraceString(e));
                android.util.Log.e("CarAppWidgetHostView", "RemoteViews.apply() hatasi", e);
            }
        } else {
            // RemoteViews NULL - provider henuz gondermedi
            // super.updateAppWidget(null) CAGIRMIYORUZ!
            // Cunku o getDefaultView -> getErrorView zincirine duser.
            if (!hasReceivedRemoteViews) {
                writeToLog("NULL_REMOTEVIEWS: AppWidgetId " + getAppWidgetId() +
                        " - Provider henuz RemoteViews gondermedi. Retry " + (retryCount + 1) + "/" + MAX_RETRY);
                showLoadingView();
                scheduleRetryBroadcast();
            }
            // Eger daha once basarili RemoteViews geldiyse ve simdi null geliyorsa,
            // mevcut icerigi koru (null ile ezme).
        }
    }

    /**
     * "Widget Yukleniyor..." gorseli gosterir.
     * getErrorView()'dan farkli olarak bu kalici bir hata degil,
     * bekleme durumunu ifade eder.
     */
    private void showLoadingView() {
        // Mevcut child'lari temizle
        removeAllViews();

        TextView loadingView = new TextView(getContext());
        loadingView.setText("Widget yukleniyor...\n(" + (retryCount + 1) + "/" + MAX_RETRY + ")");
        loadingView.setTextColor(0xAAFFFFFF);
        loadingView.setTextSize(12);
        loadingView.setPadding(16, 16, 16, 16);
        loadingView.setGravity(Gravity.CENTER);

        FrameLayout container = new FrameLayout(getContext());
        container.setBackgroundColor(0x22FFFFFF);
        container.addView(loadingView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        addView(container, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /**
     * Provider'a ACTION_APPWIDGET_UPDATE broadcast'i gondererek
     * RemoteViews gondermesini tetikler.
     */
    private void scheduleRetryBroadcast() {
        if (retryCount >= MAX_RETRY) {
            showFinalErrorView();
            return;
        }
        retryCount++;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (hasReceivedRemoteViews) return; // Artik geldiyse birak

                try {
                    AppWidgetProviderInfo info = getAppWidgetInfo();
                    if (info != null && getContext() != null) {
                        // Yontem 1: Dogrudan provider'a broadcast
                        Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        updateIntent.setComponent(info.provider);
                        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{getAppWidgetId()});
                        getContext().sendBroadcast(updateIntent);

                        // Yontem 2: updateAppWidgetOptions ile tetikleme
                        // Provider'in onAppWidgetOptionsChanged callback'ini cagirir
                        AppWidgetManager awm = AppWidgetManager.getInstance(getContext());
                        android.os.Bundle currentOptions = awm.getAppWidgetOptions(getAppWidgetId());
                        if (currentOptions != null) {
                            awm.updateAppWidgetOptions(getAppWidgetId(), currentOptions);
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.w("CarAppWidgetHostView", "Retry broadcast hatasi: " + e.getMessage());
                }
            }
        }, RETRY_DELAY_MS);
    }

    /**
     * Tum denemeler bittikten sonra gosterilen kalici hata gorunumu.
     */
    private void showFinalErrorView() {
        removeAllViews();

        TextView errView = new TextView(getContext());
        errView.setText("Widget yanitlamiyor.\nBu widget bizim\nlauncher'da\ncalismayabilir.");
        errView.setTextColor(0xAAFF6666);
        errView.setTextSize(11);
        errView.setPadding(16, 16, 16, 16);
        errView.setGravity(Gravity.CENTER);

        FrameLayout container = new FrameLayout(getContext());
        container.setBackgroundColor(0x22FF0000);
        container.addView(errView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        addView(container, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        writeToLog("SONUC: AppWidgetId " + getAppWidgetId() + " icin " + MAX_RETRY +
                " deneme yapildi. Provider RemoteViews gondermedi." +
                " Provider: " + (getAppWidgetInfo() != null ? getAppWidgetInfo().provider.toString() : "null"));
    }

    /**
     * getErrorView, artik SADECE remoteViews.apply() basarisiz oldugunda cagrilir.
     * (updateAppWidget override'imiz null durumu burada gelmesini engeller)
     * Yani buraya dusersek, provider RemoteViews gonderdi ama apply() patladidir.
     */
    @Override
    protected View getErrorView() {
        String errMsg = "APPLY_FAILURE: AppWidgetId " + getAppWidgetId() +
                " - RemoteViews alindi ama ekrana cizilemedi (apply hatasi).";

        // Apply hatasinin tam sebebini bul
        try {
            for (java.lang.reflect.Field field : AppWidgetHostView.class.getDeclaredFields()) {
                if (Throwable.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Throwable t = (Throwable) field.get(this);
                    if (t != null) {
                        errMsg += "\nException: " + android.util.Log.getStackTraceString(t);
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}

        android.util.Log.e("CarAppWidgetHostView", errMsg);
        writeToLog(errMsg);

        TextView errView = new TextView(getContext());
        errView.setText("Widget cizim hatasi");
        errView.setTextColor(0xAAFF6666);
        errView.setTextSize(11);
        errView.setGravity(Gravity.CENTER);

        FrameLayout container = new FrameLayout(getContext());
        container.setBackgroundColor(0x22FF0000);
        container.addView(errView);

        return container;
    }

    private void writeToLog(String msg) {
        try {
            if (getContext() == null) return;
            File logDir = getContext().getExternalFilesDir(null);
            if (logDir != null) {
                File logFile = new File(logDir, "carlauncher_widget_error.log");
                FileWriter fw = new FileWriter(logFile, true);
                PrintWriter pw = new PrintWriter(fw);
                pw.println("--- " + new Date().toString() + " ---");
                pw.println(msg);
                pw.println();
                pw.close();
                fw.close();
            }
        } catch (Exception ex) {
            // Ignore
        }
    }
}
