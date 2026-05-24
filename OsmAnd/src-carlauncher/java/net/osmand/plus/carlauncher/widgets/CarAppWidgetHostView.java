package net.osmand.plus.carlauncher.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

public class CarAppWidgetHostView extends AppWidgetHostView {

    public CarAppWidgetHostView(Context context) {
        super(context);
    }

    @Override
    public void updateAppWidgetSize(android.os.Bundle newOptions, int minWidth, int minHeight, int maxWidth, int maxHeight) {
        // Android 12+ ve 3. parti widget'larda sifir (0) width/height veya anlamsiz limitler geldiginde (or. Layout hesaplanirken)
        // widget'in cökmesini engellemek amaciyla asgari degerleri info icinden DP'ye cevirerek yedek (fallback) olarak ekliyoruz.
        if (minWidth <= 0 || minHeight <= 0 || maxWidth <= 0 || maxHeight <= 0) {
            android.appwidget.AppWidgetProviderInfo info = getAppWidgetInfo();
            if (info != null) {
                float density = getResources().getDisplayMetrics().density;
                minWidth = Math.max(minWidth, (int) (info.minWidth / density));
                minHeight = Math.max(minHeight, (int) (info.minHeight / density));
                maxWidth = Math.max(maxWidth, (int) (info.minWidth / density));
                maxHeight = Math.max(maxHeight, (int) (info.minHeight / density));
            } else {
                minWidth = Math.max(minWidth, 100);
                minHeight = Math.max(minHeight, 100);
                maxWidth = Math.max(maxWidth, 100);
                maxHeight = Math.max(maxHeight, 100);
            }
        }
        
        if (newOptions == null) {
            newOptions = new android.os.Bundle();
        }
        // Xiaomi/OEM widget'larinin engellenmemesi icin Host Category belirteci
        newOptions.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, 
                          android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
                          
        super.updateAppWidgetSize(newOptions, minWidth, minHeight, maxWidth, maxHeight);
    }

    @Override
    protected View getErrorView() {
        String causeStr = "Bilinmeyen Neden";
        try {
            // Android Framework icerisindeki AppWidgetHostView sinifinin gizli exception field'ini ariyoruz
            for (java.lang.reflect.Field field : AppWidgetHostView.class.getDeclaredFields()) {
                if (Throwable.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Throwable t = (Throwable) field.get(this);
                    if (t != null) {
                        causeStr = android.util.Log.getStackTraceString(t);
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}

        String errMsg = "WIDGET FRAMEWORK HATASI: AppWidgetId " + getAppWidgetId() + " icin RemoteViews olusturulamadi.\n\n" +
                "--- ROOT CAUSE (EXCEPTION) ---\n" + causeStr + "\n-----------------------------";
        
        android.util.Log.e("CarAppWidgetHostView", errMsg);
        writeErrorToLogFile(errMsg);

        // Kullaniciya ozel hata dondur
        TextView errView = new TextView(getContext());
        errView.setText("Android RemoteViews Hatasi!\nLog dosyasina bakiniz.\nSebep: " + (causeStr.length() > 50 ? causeStr.substring(0,50)+"..." : causeStr));
        errView.setTextColor(Color.WHITE);
        errView.setPadding(16, 16, 16, 16);
        errView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        
        FrameLayout container = new FrameLayout(getContext());
        container.setBackgroundColor(0x88FF0000); // Yari saydam kirmizi
        container.addView(errView);
        
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
                pw.println("--- RemoteViews Rendering Error at " + new Date().toString() + " ---");
                pw.println(errorMsg);
                pw.println("AppWidgetInfo: " + (getAppWidgetInfo() != null ? getAppWidgetInfo().provider.toString() : "null"));
                pw.println();
                pw.close();
                fw.close();
            }
        } catch (Exception ex) {
            // Ignore
        }
    }
}
