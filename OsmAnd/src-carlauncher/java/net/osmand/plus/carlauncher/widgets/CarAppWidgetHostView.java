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
    protected View getErrorView() {
        String errMsg = "WIDGET FRAMEWORK HATASI: AppWidgetId " + getAppWidgetId() + " icin RemoteViews olusturulamadi veya gosterilemedi. " +
                "Bunun nedeni widget'in bizim uygulamamizi (3rd party) reddetmesi, eksik bundle secenekleri veya cokmus olmasi olabilir.";
        
        android.util.Log.e("CarAppWidgetHostView", errMsg);
        writeErrorToLogFile(errMsg);

        // Kullaniciya ozel hata dondur ki Android'in standart siyah kutusu yerine bizim hatamizi gorsun
        TextView errView = new TextView(getContext());
        errView.setText("Android RemoteViews Hatasi!\nLog dosyasina bakiniz.\n(OEM kisitlamasi olabilir)");
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
