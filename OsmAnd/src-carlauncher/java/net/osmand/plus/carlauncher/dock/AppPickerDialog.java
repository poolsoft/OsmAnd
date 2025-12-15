package net.osmand.plus.carlauncher.dock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Uygulama secim dialogu.
 * Sistemdeki uygulamalari listeler ve secim yaptirir.
 */
public class AppPickerDialog {

    public interface OnAppSelectedListener {
        void onAppSelected(String packageName, String appName, Drawable icon);
    }

    private final Context context;
    private final OnAppSelectedListener listener;

    public AppPickerDialog(@NonNull Context context, @NonNull OnAppSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Dialogu goster.
     */
    public void show() {
        List<AppInfo> apps = getInstalledApps();

        if (apps.isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Uygulama Sec");

        // Scroll view
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        int topPadding = (int) (50 * context.getResources().getDisplayMetrics().density);
        scrollView.setPadding(0, topPadding, 0, 0); // Status bar padding

        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(16, 16, 16, 16);

        // Ensure list takes full width
        listLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (AppInfo app : apps) {
            View itemView = createAppItem(app);
            listLayout.addView(itemView);
        }

        scrollView.addView(listLayout);
        builder.setView(scrollView);
        builder.setNegativeButton("Iptal", null);

        builder.show();
    }

    /**
     * Uygulama item view olustur.
     */
    private View createAppItem(AppInfo app) {
        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(12, 16, 12, 16);
        itemLayout.setClickable(true);
        itemLayout.setFocusable(true);
        itemLayout.setBackgroundResource(android.R.drawable.list_selector_background);

        // Icon
        ImageView iconView = new ImageView(context);
        iconView.setImageDrawable(app.icon);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        itemLayout.addView(iconView);

        // Name
        TextView nameView = new TextView(context);
        nameView.setText(app.name);
        nameView.setTextColor(0xFFFFFFFF);
        nameView.setTextSize(16);
        nameView.setPadding(16, 0, 0, 0);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        itemLayout.addView(nameView, nameParams);

        // Click listener
        itemLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppSelected(app.packageName, app.name, app.icon);
            }
        });

        return itemLayout;
    }

    private static List<AppInfo> cachedApps = null;

    /**
     * Yuklu uygulamalari al.
     */
    private List<AppInfo> getInstalledApps() {
        if (cachedApps != null && !cachedApps.isEmpty()) {
            return cachedApps;
        }

        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : resolveInfos) {
            try {
                String packageName = info.activityInfo.packageName;
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

                // Sistem uygulamalarini filtrele (opsiyonel)
                // if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

                AppInfo app = new AppInfo();
                app.name = appInfo.loadLabel(pm).toString();
                app.packageName = packageName;
                app.icon = appInfo.loadIcon(pm);

                apps.add(app);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Alfabetik sirala
        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a1, AppInfo a2) {
                return a1.name.compareToIgnoreCase(a2.name);
            }
        });

        cachedApps = apps;
        return apps;
    }

    /**
     * Cache'i temizle (Ornegin yeni uyulama yuklendiginde).
     */
    public static void clearCache() {
        cachedApps = null;
    }

    /**
     * Uygulama bilgisi.
     */
    private static class AppInfo {
        String name;
        String packageName;
        Drawable icon;
    }
}
