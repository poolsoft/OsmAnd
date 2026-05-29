package net.osmand.plus.carlauncher.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

/**
 * Car Launcher guncelleme ve APK indirme/yukleme yardimci sinifi.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class UpdaterHelper {

    // Latest Release altindaki version.json'a ulasacagiz.
    private static final String VERSION_JSON_URL = "https://github.com/poolsoft/OsmAnd/releases/latest/download/version.json";

    public static void checkUpdates(Context context, boolean showToastIfLatest) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(VERSION_JSON_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                int latestVersionCode = json.getInt("versionCode");
                String latestVersionName = json.getString("versionName");
                String apkUrl = json.getString("apkUrl");

                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                long currentVersionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pInfo.getLongVersionCode() : pInfo.versionCode;

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (latestVersionCode > currentVersionCode) {
                        showUpdateDialog(context, latestVersionName, apkUrl);
                    } else if (showToastIfLatest) {
                        Toast.makeText(context, "Uygulama guncel (v" + pInfo.versionName + ")", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (showToastIfLatest) {
                        Toast.makeText(context, "Guncelleme kontrolu basarisiz", Toast.LENGTH_SHORT).show();
                    }
                    android.util.Log.e("Updater", "Check update error", e);
                });
            }
        });
    }

    private static void showUpdateDialog(Context context, String versionName, String apkUrl) {
        new AlertDialog.Builder(context)
                .setTitle("Yeni Surum Mevcut!")
                .setMessage("Car Launcher v" + versionName + " indirilebilir. Guncellemek istiyor musunuz?")
                .setPositiveButton("Indir ve Yukle", (dialog, which) -> downloadAndInstallApk(context, apkUrl))
                .setNegativeButton("Daha Sonra", null)
                .show();
    }

    private static void downloadAndInstallApk(Context context, String url) {
        // Indirme baslamadan once eski indirilmis APK varsa siliyoruz (Cakismlari onlemek icin)
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File oldApk = new File(downloadsDir, "CarLauncher_Update.apk");
            if (oldApk.exists()) {
                oldApk.delete();
            }
        } catch (Exception e) {
            android.util.Log.e("Updater", "Eski APK silinirken hata olustu", e);
        }

        Toast.makeText(context, "Indirme baslatildi...", Toast.LENGTH_SHORT).show();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Car Launcher Guncelleme");
        request.setDescription("Yeni surum indiriliyor...");
        // Bildirimin indirme esnasinda periyodik gurultu yapmasini onlemek icin sadece tamamlandiginda bildirim gosteriyoruz
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CarLauncher_Update.apk");
        
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            long downloadId = manager.enqueue(request);
            
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        installApk(c, manager, downloadId);
                    }, 1000);
                    try {
                        context.getApplicationContext().unregisterReceiver(this);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            };

            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getApplicationContext().registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.getApplicationContext().registerReceiver(receiver, filter);
            }
        }
    }

    private static void installApk(Context context, DownloadManager manager, long downloadId) {
        // Android 8.0+ icin Bilinmeyen Kaynaklar yukleme izni kontrolu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(context, "Uygulama yukleme izni verilmelidir. Ayarlar aciliyor...", Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("Updater", "Ayarlar acilamadi", e);
                }
                return;
            }
        }

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = manager.query(query);
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                // 1. Yol: DownloadManager'in kendi guvenli content URI'sini kullanmayi deniyoruz (En garanti ve modern yol)
                Uri downloadUri = null;
                try {
                    downloadUri = manager.getUriForDownloadedFile(downloadId);
                } catch (Exception e) {
                    android.util.Log.e("Updater", "DownloadManager URI alinirken hata olustu", e);
                }

                if (downloadUri != null) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(downloadUri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        cursor.close();
                        return; // Basarili sekilde baslatildi
                    } catch (Exception e) {
                        android.util.Log.e("Updater", "DownloadManager URI ile kurulum baslatilamadi, FileProvider denenecek", e);
                    }
                }

                // 2. Yol (Fallback): Eger ilk yol basarisiz olursa statik dosya referansi ve FileProvider kullaniyoruz
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File apkFile = new File(downloadsDir, "CarLauncher_Update.apk");

                if (apkFile.exists()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri contentUri = null;
                        try {
                            contentUri = FileProvider.getUriForFile(context, 
                                context.getPackageName() + ".fileprovider", 
                                apkFile);
                        } catch (Exception e) {
                            android.util.Log.e("Updater", "FileProvider URI olusturulurken hata", e);
                        }

                        if (contentUri != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } else {
                            Toast.makeText(context, "Guncelleme dosyasi URI'si olusturulamadi", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Uri apkUri = Uri.fromFile(apkFile);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                } else {
                    Toast.makeText(context, "Indirilen APK dosyasi bulunamadi", Toast.LENGTH_LONG).show();
                }
            } else if (status == DownloadManager.STATUS_FAILED) {
                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                Toast.makeText(context, "Indirme basarisiz oldu (Hata kodu: " + reason + ")", Toast.LENGTH_LONG).show();
            }
        }
        cursor.close();
    }
}
