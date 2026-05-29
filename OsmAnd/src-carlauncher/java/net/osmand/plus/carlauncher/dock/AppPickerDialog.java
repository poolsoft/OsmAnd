package net.osmand.plus.carlauncher.dock;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class AppPickerDialog {

    public interface OnAppSelectedListener {
        void onAppSelected(String packageName, String appName, Drawable icon);
    }

    private final Context context;
    private final OnAppSelectedListener listener;
    private final boolean onlyMusicApps;
    private BottomSheetDialog dialog;

    public AppPickerDialog(@NonNull Context context, @NonNull OnAppSelectedListener listener) {
        this(context, false, listener);
    }

    public AppPickerDialog(@NonNull Context context, boolean onlyMusicApps, @NonNull OnAppSelectedListener listener) {
        this.context = context;
        this.onlyMusicApps = onlyMusicApps;
        this.listener = listener;
    }

    public void show() {
        dialog = new BottomSheetDialog(context, net.osmand.plus.R.style.Theme_Design_BottomSheetDialog);
        
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(android.graphics.Color.parseColor("#1C1C1E"));
        root.setPadding(16, 24, 16, 16);
        
        View handle = new View(context);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(96, 12);
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = 32;
        handle.setBackgroundColor(android.graphics.Color.GRAY);
        root.addView(handle, handleLp);

        TextView titleView = new TextView(context);
        titleView.setText(onlyMusicApps ? "MÜZİK UYGULAMASI SEÇ" : "UYGULAMA SEÇ");
        titleView.setTextColor(android.graphics.Color.WHITE);
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 32);
        root.addView(titleView);

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 4));
        AppInfoAdapter adapter = new AppInfoAdapter(app -> {
            if (listener != null) listener.onAppSelected(app.packageName, app.name, app.icon);
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);
        root.addView(recyclerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                (int)(context.getResources().getDisplayMetrics().heightPixels * 0.6f)));

        List<AppInfo> apps = getInstalledApps();
        adapter.setApps(apps);

        dialog.setContentView(root);
        View parent = (View) root.getParent();
        if (parent != null) parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        dialog.show();
    }

    private static List<AppInfo> cachedAllApps = null;
    private static List<AppInfo> cachedMusicApps = null;

    private List<AppInfo> getInstalledApps() {
        if (onlyMusicApps && cachedMusicApps != null) return cachedMusicApps;
        if (!onlyMusicApps && cachedAllApps != null) return cachedAllApps;

        List<AppInfo> apps = new ArrayList<>();
        
        // AppDrawer'daki cache'lenmis uygulamalari ve ikonlari kullan (Turkce karakter yok)
        List<net.osmand.plus.carlauncher.ui.AppDrawerFragment.AppItem> cachedList = 
                net.osmand.plus.carlauncher.ui.AppDrawerFragment.getCachedApps();
        android.util.LruCache<String, Drawable> cacheIcons = 
                net.osmand.plus.carlauncher.ui.AppDrawerFragment.getIconCache();

        if (onlyMusicApps) {
            AppInfo internalPlayer = new AppInfo();
            internalPlayer.name = "Dahili Muzik Calar";
            internalPlayer.packageName = "usage.internal.player";
            internalPlayer.icon = context.getResources().getDrawable(android.R.drawable.ic_media_play, null);
            apps.add(internalPlayer);
        }

        if (cachedList != null && !cachedList.isEmpty()) {
            PackageManager pm = context.getPackageManager();
            List<String> musicPackages = new ArrayList<>();
            if (onlyMusicApps) {
                Intent musicIntent = new Intent("android.media.browse.MediaBrowserService");
                List<ResolveInfo> musicServices = pm.queryIntentServices(musicIntent, 0);
                for (ResolveInfo info : musicServices) musicPackages.add(info.serviceInfo.packageName);
                
                musicPackages.add("com.acloud.stub.localmusic");
                musicPackages.add("com.xyauto.music");
                musicPackages.add("com.android.music");
                musicPackages.add("com.txznet.music");
                musicPackages.add("com.syd.music");
                musicPackages.add("com.mediatek.music");
                musicPackages.add("com.spotify.music");
                musicPackages.add("com.google.android.apps.youtube.music");
                musicPackages.add("com.google.android.youtube");
                musicPackages.add("com.apple.android.music");
                musicPackages.add("deezer.android.app");
                musicPackages.add("com.aspiro.tidal");
                musicPackages.add("com.soundcloud.android");
                musicPackages.add("tunein.player");
                musicPackages.add("org.videolan.vlc");
            }

            for (net.osmand.plus.carlauncher.ui.AppDrawerFragment.AppItem item : cachedList) {
                // Dahili uygulamalari secici listesinde gosterme (Turkce karakter yok)
                if (item.packageName != null && item.packageName.startsWith("internal://")) {
                    continue;
                }
                
                if (onlyMusicApps && !musicPackages.contains(item.packageName)) {
                    continue;
                }

                Drawable icon = null;
                if (cacheIcons != null) {
                    icon = cacheIcons.get(item.packageName);
                }
                if (icon == null) {
                    try {
                        icon = pm.getApplicationIcon(item.packageName);
                    } catch (Exception e) {
                        icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
                    }
                }

                AppInfo app = new AppInfo();
                app.name = item.label;
                app.packageName = item.packageName;
                app.icon = icon;
                apps.add(app);
            }
            
            Collections.sort(apps, (a1, a2) -> a1.name.compareToIgnoreCase(a2.name));
            if (onlyMusicApps) cachedMusicApps = apps;
            else cachedAllApps = apps;
            return apps;
        }

        // Cache henuz yuklenmemisse fallback olarak senkron cagirir (Turkce karakter yok)
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<String> musicPackages = new ArrayList<>();
        if (onlyMusicApps) {
            Intent musicIntent = new Intent("android.media.browse.MediaBrowserService");
            List<ResolveInfo> musicServices = pm.queryIntentServices(musicIntent, 0);
            for (ResolveInfo info : musicServices) musicPackages.add(info.serviceInfo.packageName);
            
            musicPackages.add("com.acloud.stub.localmusic");
            musicPackages.add("com.xyauto.music");
            musicPackages.add("com.android.music");
            musicPackages.add("com.txznet.music");
            musicPackages.add("com.syd.music");
            musicPackages.add("com.mediatek.music");
            musicPackages.add("com.spotify.music");
            musicPackages.add("com.google.android.apps.youtube.music");
            musicPackages.add("com.google.android.youtube");
            musicPackages.add("com.apple.android.music");
            musicPackages.add("deezer.android.app");
            musicPackages.add("com.aspiro.tidal");
            musicPackages.add("com.soundcloud.android");
            musicPackages.add("tunein.player");
            musicPackages.add("org.videolan.vlc");
        }

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo info : resolveInfos) {
            try {
                String packageName = info.activityInfo.packageName;
                if (onlyMusicApps && !musicPackages.contains(packageName)) continue;
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                AppInfo app = new AppInfo();
                app.name = appInfo.loadLabel(pm).toString();
                app.packageName = packageName;
                app.icon = appInfo.loadIcon(pm);
                apps.add(app);
            } catch (Exception e) { }
        }

        Collections.sort(apps, (a1, a2) -> a1.name.compareToIgnoreCase(a2.name));
        if (onlyMusicApps) cachedMusicApps = apps;
        else cachedAllApps = apps;
        return apps;
    }

    public static void clearCache() {
        cachedAllApps = null;
        cachedMusicApps = null;
    }

    public static class AppInfo {
        public String name;
        public String packageName;
        public Drawable icon;
    }
}
