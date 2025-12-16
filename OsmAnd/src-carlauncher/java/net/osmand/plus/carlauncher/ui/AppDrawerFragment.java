package net.osmand.plus.carlauncher.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.carlauncher.CarLauncherInterface;
import net.osmand.plus.carlauncher.dock.AppDockManager;
import net.osmand.plus.carlauncher.dock.AppShortcut;
import net.osmand.plus.carlauncher.dock.LaunchMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppDrawerFragment extends Fragment {

    public static final String TAG = "AppDrawerFragment";

    private RecyclerView recyclerView;
    private AppDrawerAdapter adapter;
    private FrameLayout loadingView;
    private List<AppItem> cachedApps; // Cache
    private PackageReceiver packageReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register BroadcastReceiver
        packageReceiver = new PackageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");

        if (getContext() != null) {
            getContext().registerReceiver(packageReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getContext() != null && packageReceiver != null) {
            getContext().unregisterReceiver(packageReceiver);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        FrameLayout rootLayout = new FrameLayout(getContext());
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Close button (Top Right)
        ImageButton closeButton = new ImageButton(getContext());
        closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeButton.setBackgroundColor(0x00000000);
        closeButton.setColorFilter(0xFFFFFFFF);
        closeButton.setOnClickListener(v -> closeDrawer());

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(64, 64);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(32, 32, 32, 32);
        rootLayout.addView(closeButton, closeParams);

        // RecyclerView
        recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5)); // 5 Columns
        recyclerView.setPadding(32, 100, 32, 32); // Top padding for close button space
        recyclerView.setClipToPadding(false);

        rootLayout.addView(recyclerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Loading View
        loadingView = new FrameLayout(getContext());
        TextView loadingText = new TextView(getContext());
        loadingText.setText("Yukleniyor...");
        loadingText.setTextColor(0xFFFFFFFF);
        loadingText.setGravity(Gravity.CENTER);
        loadingView.addView(loadingText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        rootLayout.addView(loadingView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        return rootLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadApps();
    }

    private void closeDrawer() {
        if (getActivity() instanceof CarLauncherInterface) {
            ((CarLauncherInterface) getActivity()).closeAppDrawer();
        }
    }

    private void loadApps() {
        // Cache varsa ve doluysa tekrar yukleme
        if (cachedApps != null && !cachedApps.isEmpty()) {
            if (loadingView != null)
                loadingView.setVisibility(View.GONE);
            if (adapter == null) {
                adapter = new AppDrawerAdapter(cachedApps);
                recyclerView.setAdapter(adapter);
            }
            return;
        }

        // Cache yoksa yukle
        // Cache yoksa yukle
        if (loadingView != null)
            loadingView.setVisibility(View.VISIBLE);
        if (getContext() != null) {
            new LoadAppsTask(getContext()).execute();
        }
    }

    // Broadcast Receiver for App Updates
    private class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Clear cache and reload
            cachedApps = null;
            loadApps();
        }
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppItem>> {
        private final Context context;

        public LoadAppsTask(Context context) {
            this.context = context.getApplicationContext(); // Use application context
        }

        @Override
        protected List<AppItem> doInBackground(Void... voids) {
            List<AppItem> apps = new ArrayList<>();
            if (context == null)
                return apps;
            PackageManager pm = context.getPackageManager();

            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

            for (ResolveInfo info : activities) {
                AppItem item = new AppItem();
                item.label = info.loadLabel(pm).toString();
                item.packageName = info.activityInfo.packageName;
                item.icon = info.loadIcon(pm);
                apps.add(item);
            }

            Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
            return apps;
        }

        @Override
        protected void onPostExecute(List<AppItem> appItems) {
            cachedApps = appItems; // Cache'e kaydet
            if (loadingView != null)
                loadingView.setVisibility(View.GONE);
            adapter = new AppDrawerAdapter(appItems);
            if (recyclerView != null)
                recyclerView.setAdapter(adapter);
        }
    }

    private class AppItem {
        String label;
        String packageName;
        Drawable icon;
    }

    private class AppDrawerAdapter extends RecyclerView.Adapter<AppDrawerAdapter.ViewHolder> {

        private List<AppItem> apps;

        AppDrawerAdapter(List<AppItem> apps) {
            this.apps = apps;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(16, 16, 16, 16);
            layout.setBackgroundResource(android.R.drawable.list_selector_background);

            // Icon
            ImageView iconView = new ImageView(getContext());
            iconView.setLayoutParams(new LinearLayout.LayoutParams(96, 96)); // Large Icon
            layout.addView(iconView);

            // Text
            TextView textView = new TextView(getContext());
            textView.setTextColor(0xFFFFFFFF);
            textView.setTextSize(14);
            textView.setGravity(Gravity.CENTER);
            textView.setSingleLine(true);
            textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textView.setPadding(0, 8, 0, 0);

            layout.addView(textView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            return new ViewHolder(layout, iconView, textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppItem item = apps.get(position);
            holder.textView.setText(item.label);
            holder.iconView.setImageDrawable(item.icon);

            holder.itemView.setOnClickListener(v -> {
                launchApp(item.packageName);
                closeDrawer();
            });

            holder.itemView.setOnLongClickListener(v -> {
                showAppOptions(item);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconView;
            TextView textView;

            ViewHolder(View itemView, ImageView iconView, TextView textView) {
                super(itemView);
                this.iconView = iconView;
                this.textView = textView;
            }
        }
    }

    private void showAppOptions(AppItem item) {
        String[] options = { "Dock'a Ekle (Standart)", "Dock'a Ekle (Overlay)", "Uygulama Bilgisi" };

        new AlertDialog.Builder(getContext())
                .setTitle(item.label)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            addToDock(item, LaunchMode.FULL_SCREEN);
                            break;
                        case 1:
                            addToDock(item, LaunchMode.OVERLAY);
                            break;
                        case 2:
                            showAppInfo(item.packageName);
                            break;
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    private void addToDock(AppItem item, LaunchMode mode) {
        AppDockManager dockManager = new AppDockManager(getContext());
        dockManager.loadShortcuts();

        if (!dockManager.canAddMore()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Hata")
                    .setMessage("Dock dolu! Maksimum " + dockManager.getMaxShortcuts() + " kisayol.")
                    .setPositiveButton("Tamam", null)
                    .show();
            return;
        }

        int order = dockManager.getShortcuts().size();
        AppShortcut shortcut = new AppShortcut(item.packageName, item.label, item.icon, order, mode);

        if (dockManager.addShortcut(shortcut)) {
            Toast.makeText(getContext(), item.label + " dock'a eklendi.", Toast.LENGTH_SHORT).show();
            // Dock fragmentini yenilemek icin activity'yi uyar veya broadcast yolla
            // En basiti: AppDockFragment resume olunca yukler diye umalim ancak
            // Drawer, ayni Activity icinde oldugu icin AppDockFragment refresh olmayabilir.
            // Simdilik broadcast gonderelim.
            // Ama broadcast receiver yok AppDockFragment'ta (henuz).
            // O yuzden sadece Toast gosterelim (Context null degilse). Eklendi diye.
            // User dock'u acip kapatabilir veya restart edebilir.
            // GELISTIRME: Bir dahaki update'te AppDockFragment broadcast dinleyebilir.
        }
        closeDrawer();
    }

    private void launchApp(String packageName) {
        try {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAppInfo(String packageName) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
