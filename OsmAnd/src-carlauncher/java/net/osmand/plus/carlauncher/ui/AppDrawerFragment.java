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
    private View loadingView;
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
        return inflater.inflate(net.osmand.plus.R.layout.fragment_app_drawer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        recyclerView = view.findViewById(net.osmand.plus.R.id.apps_recycler_view);
        loadingView = view.findViewById(net.osmand.plus.R.id.loading_progress);

        android.view.View closeBtn = view.findViewById(net.osmand.plus.R.id.btn_close_drawer);
        android.widget.EditText searchInput = view.findViewById(net.osmand.plus.R.id.search_input);

        // Logic
        closeBtn.setOnClickListener(v -> closeDrawer());

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5));

        // Search Filter
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null)
                    adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

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

            // Add internal apps at the beginning
            List<AppItem> internalApps = getInternalApps();
            apps.addAll(0, internalApps);

            return apps;
        }

        private List<AppItem> getInternalApps() {
            List<AppItem> internal = new ArrayList<>();

            // Settings
            AppItem settings = new AppItem();
            settings.label = "⚙️ Car Launcher Ayarlar";
            settings.packageName = "internal://settings";
            settings.icon = context.getResources().getDrawable(android.R.drawable.ic_menu_preferences, null);
            internal.add(settings);

            // Music Player
            AppItem music = new AppItem();
            music.label = "🎵 Muzik Calici";
            music.packageName = "internal://music";
            music.icon = context.getResources().getDrawable(android.R.drawable.ic_media_play, null);
            internal.add(music);

            return internal;
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

        private List<AppItem> originalApps;
        private List<AppItem> displayedApps;

        AppDrawerAdapter(List<AppItem> apps) {
            this.originalApps = new ArrayList<>(apps);
            this.displayedApps = new ArrayList<>(apps);
        }

        void filter(String query) {
            displayedApps.clear();
            if (android.text.TextUtils.isEmpty(query)) {
                displayedApps.addAll(originalApps);
            } else {
                String q = query.toLowerCase();
                for (AppItem item : originalApps) {
                    if (item.label.toLowerCase().contains(q)) {
                        displayedApps.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Using ID directly might fail if R is not imported correctly, but following
            // pattern
            View view = LayoutInflater.from(parent.getContext()).inflate(net.osmand.plus.R.layout.item_app_drawer,
                    parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppItem item = displayedApps.get(position);
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
            return displayedApps.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconView;
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                iconView = itemView.findViewById(net.osmand.plus.R.id.app_icon);
                textView = itemView.findViewById(net.osmand.plus.R.id.app_label);
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

            // Send broadcast to refresh Dock
            Intent updateIntent = new Intent("net.osmand.carlauncher.DOCK_UPDATED");
            getContext().sendBroadcast(updateIntent);
        }
        closeDrawer();
    }

    private void launchApp(String packageName) {
        // Handle internal apps
        if (packageName != null && packageName.startsWith("internal://")) {
            handleInternalApp(packageName);
            return;
        }

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

    private void handleInternalApp(String internalUri) {
        if (getActivity() == null || !(getActivity() instanceof MapActivity))
            return;

        MapActivity activity = (MapActivity) getActivity();
        // Note: Don't call closeDrawer() - internal fragments open in same container

        switch (internalUri) {
            case "internal://settings":
                activity.openCarLauncherSettings();
                break;
            case "internal://music":
                activity.openMusicPlayer();
                break;
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
