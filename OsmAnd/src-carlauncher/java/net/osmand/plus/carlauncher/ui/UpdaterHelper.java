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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

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
                        Toast.makeText(context, "Uygulama güncel (v" + pInfo.versionName + ")", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (showToastIfLatest) {
                        Toast.makeText(context, "Güncelleme kontrolü başarısız", Toast.LENGTH_SHORT).show();
                    }
                    android.util.Log.e("Updater", "Check update error", e);
                });
            }
        });
    }

    private static void showUpdateDialog(Context context, String versionName, String apkUrl) {
        new AlertDialog.Builder(context)
                .setTitle("Yeni Sürüm Mevcut!")
                .setMessage("Car Launcher v" + versionName + " indirilebilir. Güncellemek istiyor musunuz?")
                .setPositiveButton("İndir ve Yükle", (dialog, which) -> downloadAndInstallApk(context, apkUrl))
                .setNegativeButton("Daha Sonra", null)
                .show();
    }

    private static void downloadAndInstallApk(Context context, String url) {
        Toast.makeText(context, "Indirme baslatildi...", Toast.LENGTH_SHORT).show();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Car Launcher Guncelleme");
        request.setDescription("Yeni surum indiriliyor...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CarLauncher_Update.apk");
        
        // Mobil veri ve dolasimda indirmeye acikca izin vererek takilmalari onluyoruz
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            long downloadId = manager.enqueue(request);
            
            // Indirme bittiginde yakalamak icin bir receiver kaydediyoruz
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    installApk(c, manager, downloadId);
                    try {
                        context.getApplicationContext().unregisterReceiver(this);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            };

            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            // Android 14+ Guvenlik kuralina gore RECEIVER_EXPORTED bayragi ekliyoruz
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getApplicationContext().registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.getApplicationContext().registerReceiver(receiver, filter);
            }
        }
    }

    private static void installApk(Context context, DownloadManager manager, long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = manager.query(query);
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                Uri apkUri = Uri.parse(uriString);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7.0+ icin FileProvider kullanmaliyiz
                    Uri contentUri = FileProvider.getUriForFile(context, 
                        context.getPackageName() + ".fileprovider", 
                        new java.io.File(apkUri.getPath()));
                    
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } else if (status == DownloadManager.STATUS_FAILED) {
                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                Toast.makeText(context, "Indirme basarisiz oldu (Hata kodu: " + reason + ")", Toast.LENGTH_LONG).show();
            }
        }
        cursor.close();
    }
}
