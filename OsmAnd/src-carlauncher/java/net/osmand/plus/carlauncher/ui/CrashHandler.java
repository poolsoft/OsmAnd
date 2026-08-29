package net.osmand.plus.carlauncher.ui;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Launcher'a ozel hata yakalayici. 
 * OsmAnd'in genel hata raporundan bagimsiz calisir.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init(Context context) {
        // MapActivity can be recreated. Do not build a chain of CrashHandlers where
        // every old instance writes and forwards the same crash again.
        if (Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String crashLog = getCrashInfo(throwable);
        saveToFile(crashLog);
        
        // Hata olustugunda uygulamayi kapatmadan once logu bastir
        Log.e("CarLauncherCrash", crashLog);
        
        // Varsayilan handler'a geri don (OsmAnd raporu yine de cikabilir ama biz logu aldik)
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }

    private String getCrashInfo(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);

        StringBuilder sb = new StringBuilder();
        sb.append("===== CAR LAUNCHER CRASH REPORT =====\n");
        sb.append("Time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("-------------------------------------\n");
        sb.append(sw.toString());
        sb.append("\n=====================================");
        return sb.toString();
    }

    private void saveToFile(String log) {
        byte[] bytes = (log + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Always retain a private copy. This path remains writable even when an OEM ROM has
        // broken ownership on Android/data after an update or restore.
        try {
            File file = new File(context.getNoBackupFilesDir(), "launcher_crash.log");
            try (FileOutputStream fos = new FileOutputStream(file, true)) {
                fos.write(bytes);
            }
        } catch (Exception e) {
            Log.e("CarLauncherCrash", "Private crash log could not be saved", e);
        }

        // Also create a user-accessible copy for head units where ADB is unavailable.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME,
                        "launcher_crash_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt");
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OsmAndAuto");
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                        if (output != null) output.write(bytes);
                    }
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "OsmAndAuto");
                if ((dir.exists() || dir.mkdirs())) {
                    File file = new File(dir, "launcher_crash.txt");
                    try (FileOutputStream fos = new FileOutputStream(file, true)) {
                        fos.write(bytes);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CarLauncherCrash", "Public crash log could not be saved", e);
        }
    }
}
