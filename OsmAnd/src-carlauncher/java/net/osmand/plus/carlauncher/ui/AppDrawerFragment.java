package net.osmand.plus.carlauncher.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AppDrawerFragment extends Fragment {

    public static final String TAG = "AppDrawerFragment";

    private RecyclerView recyclerView;
    private AppDrawerAdapter adapter;
    private View loadingView;
    private static volatile List<AppItem> cachedApps; // Static Cache to prevent reloading
    private BroadcastReceiver packageReceiver; // Paket degisikliklerini izlemek icin alici (Turkce karakter yok)
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private Context callbackContext;
    private Future<?> loadAppsFuture;
    private int loadGeneration;
    
    // Asynchronous LruCache for holding app icons (Turkce karakter yok)
    private static android.util.LruCache<String, Drawable> iconCache;
    private static final ExecutorService ICON_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final ExecutorService APP_LIST_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static int iconCacheGeneration;

    public static synchronized void clearCache() {
        cachedApps = null;
        iconCacheGeneration++;
        if (iconCache != null) {
            iconCache.evictAll();
        }
    }

    public static class AppItem { // Public static class
        public String label;
        public String packageName;
    }

    // Kolay ve guvenli sekilde uygulama ikonunu yukleme yardimcisi (Turkce karakter yok)
    public static Drawable getAppIcon(Context context, String packageName) {
        if (context == null || packageName == null) {
            return null;
        }
        Drawable icon = null;
        if (net.osmand.plus.carlauncher.dock.InternalApp.isInternalApp(packageName)) {
            net.osmand.plus.carlauncher.dock.InternalApp app = net.osmand.plus.carlauncher.dock.InternalApp.fromPackageName(packageName);
            if (app != null) {
                icon = app.getIcon(context);
            }
        } else {
            try {
                icon = context.getPackageManager().getApplicationIcon(packageName);
            } catch (Exception e) {
                // fallback
            }
        }
        if (icon == null) {
            try {
                icon = context.getPackageManager().getDefaultActivityIcon();
            } catch (Exception e) {
                return null;
            }
        }

        // Internal icons already contain their own automotive tile background.
        if (!net.osmand.plus.carlauncher.dock.InternalApp.isInternalApp(packageName)
                && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    && !(icon instanceof android.graphics.drawable.AdaptiveIconDrawable))) {
            return createCircularIcon(context, icon);
        }
        return icon;
    }

    private static Drawable createCircularIcon(Context context, Drawable icon) {
        if (icon == null) return null;
        
        // 1. Dairesel arka plan olustur (Turkce karakter yok)
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        background.setColor(android.graphics.Color.parseColor("#2A2C32")); // Koyu gri sik arka plan (Turkce karakter yok)
        
        // 2. LayerDrawable ile ust uste koy (Turkce karakter yok)
        Drawable[] layers = new Drawable[2];
        layers[0] = background;
        layers[1] = icon;
        
        android.graphics.drawable.LayerDrawable layerDrawable = new android.graphics.drawable.LayerDrawable(layers);
        int padding = (int) (14 * context.getResources().getDisplayMetrics().density);
        layerDrawable.setLayerInset(1, padding, padding, padding, padding);
        
        return layerDrawable;
    }

    private static void loadIconAsync(ImageView imageView, Context context, String packageName) {
        java.lang.ref.WeakReference<ImageView> imageRef =
                new java.lang.ref.WeakReference<>(imageView);
        Context appContext = context.getApplicationContext();
        final int generation;
        synchronized (AppDrawerFragment.class) {
            generation = iconCacheGeneration;
        }
        ICON_EXECUTOR.execute(() -> {
            Drawable drawable = getAppIcon(appContext, packageName);
            MAIN_HANDLER.post(() -> {
                synchronized (AppDrawerFragment.class) {
                    if (generation != iconCacheGeneration) {
                        return;
                    }
                    if (drawable != null && iconCache != null) {
                        iconCache.put(packageName, drawable);
                    }
                }
                ImageView target = imageRef.get();
                if (target != null && packageName.equals(target.getTag())) {
                    target.setImageDrawable(drawable);
                }
            });
        });
    }

    private final android.content.ComponentCallbacks2 componentCallbacks = new android.content.ComponentCallbacks2() {
        @Override
        public void onTrimMemory(int level) {
            if (level >= TRIM_MEMORY_MODERATE) {
                clearCache();
            }
        }

        @Override
        public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        }
        @Override
        public void onLowMemory() {
            clearCache();
        }
    };

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (recyclerView != null && recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            int spanCount = (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ? 6 : 4;
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanCount(spanCount);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        callbackContext = context.getApplicationContext();
        try {
            callbackContext.registerComponentCallbacks(componentCallbacks);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void onDetach() {
        try {
            if (callbackContext != null) {
                callbackContext.unregisterComponentCallbacks(componentCallbacks);
            }
        } catch (Exception e) {
            // ignore
        }
        super.onDetach();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Icon Cache dynamically if not exists (Turkce karakter yok)
        if (iconCache == null) {
            int cacheSizeBytes = 4 * 1024 * 1024;
            try {
                android.app.ActivityManager am = (android.app.ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    int memoryClass = am.getMemoryClass();
                    cacheSizeBytes = Math.max(4, Math.min(16, memoryClass / 32))
                            * 1024 * 1024;
                }
            } catch (Exception e) {
                // fallback
            }
            iconCache = new android.util.LruCache<String, Drawable>(cacheSizeBytes) {
                @Override
                protected int sizeOf(String key, Drawable drawable) {
                    if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                        android.graphics.Bitmap bitmap =
                                ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                        if (bitmap != null) {
                            return Math.max(1, bitmap.getAllocationByteCount());
                        }
                    }
                    int width = Math.max(48, drawable.getIntrinsicWidth());
                    int height = Math.max(48, drawable.getIntrinsicHeight());
                    return Math.max(1, width * height * 4);
                }
            };
        }

        // Paket degisikliklerini dinlemek icin alici (Turkce karakter yok)
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                clearCache();
                if (loadAppsFuture != null) {
                    loadAppsFuture.cancel(true);
                    loadAppsFuture = null;
                }
                loadGeneration++;
                if (isAdded() && !isDetached()) {
                    loadApps();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        if (callbackContext != null) {
            callbackContext.registerReceiver(packageReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (loadAppsFuture != null) {
            loadAppsFuture.cancel(true);
            loadAppsFuture = null;
        }
        loadGeneration++;
        if (packageReceiver != null && callbackContext != null) {
            try {
                callbackContext.unregisterReceiver(packageReceiver);
            } catch (Exception e) {
                // ignore
            }
        }
        try {
            if (callbackContext != null) {
                callbackContext.unregisterComponentCallbacks(componentCallbacks);
            }
        } catch (Exception e) {
            // ignore
        }
        callbackContext = null;
        super.onDestroy();
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

        boolean isLandscape = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), isLandscape ? 6 : 4));

        // Search Filter (Debounced)
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString();
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    if (adapter != null) {
                        adapter.filter(query);
                    }
                };
                searchHandler.postDelayed(searchRunnable, 200); // 200ms gecikme (Debounce)
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        loadApps();
    }

    @Override
    public void onDestroyView() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
            searchRunnable = null;
        }
        recyclerView = null;
        adapter = null;
        loadingView = null;
        super.onDestroyView();
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
            if (adapter == null && recyclerView != null) {
                adapter = new AppDrawerAdapter(cachedApps);
                recyclerView.setAdapter(adapter);
            }
            return;
        }
        if (loadAppsFuture != null) {
            return;
        }

        // Cache yoksa yukle
        if (loadingView != null)
            loadingView.setVisibility(View.VISIBLE);
        if (getContext() != null) {
            Context appContext = getContext().getApplicationContext();
            int generation = ++loadGeneration;
            loadAppsFuture = APP_LIST_EXECUTOR.submit(() -> {
                List<AppItem> appItems;
                try {
                    appItems = queryApps(appContext);
                } catch (RuntimeException e) {
                    // Some vendor PackageManager implementations can fail while
                    // their launcher database is rebuilding. Keep internal apps
                    // available and allow the next package event to retry.
                    appItems = getInternalApps();
                }
                final List<AppItem> loadedApps = appItems;
                MAIN_HANDLER.post(() -> applyLoadedApps(generation, loadedApps));
            });
        }
    }

    private List<AppItem> queryApps(Context context) {
        List<AppItem> apps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        Set<String> seenPackages = new HashSet<>();

        for (ResolveInfo info : activities) {
            if (Thread.currentThread().isInterrupted()) {
                return Collections.emptyList();
            }
            if (info.activityInfo == null || !seenPackages.add(info.activityInfo.packageName)) {
                continue;
            }
            AppItem item = new AppItem();
            CharSequence label = info.loadLabel(pm);
            item.label = label == null ? info.activityInfo.packageName : label.toString();
            item.packageName = info.activityInfo.packageName;
            apps.add(item);
        }
        java.text.Collator collator = java.text.Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(a.label, b.label));
        apps.addAll(0, getInternalApps());
        return apps;
    }

    private List<AppItem> getInternalApps() {
        List<AppItem> internal = new ArrayList<>();
        for (net.osmand.plus.carlauncher.dock.InternalApp app
                : net.osmand.plus.carlauncher.dock.InternalApp.values()) {
            AppItem item = new AppItem();
            item.label = app.getDefaultName();
            item.packageName = app.getPackageName();
            internal.add(item);
        }
        return internal;
    }

    private void applyLoadedApps(int generation, List<AppItem> appItems) {
        if (generation != loadGeneration) {
            return;
        }
        loadAppsFuture = null;
        cachedApps = appItems;
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            adapter = new AppDrawerAdapter(appItems);
            recyclerView.setAdapter(adapter);
        }
    }



    private class AppDrawerAdapter extends RecyclerView.Adapter<AppDrawerAdapter.ViewHolder> {

        private List<AppItem> originalApps;
        private List<AppItem> displayedApps;

        AppDrawerAdapter(List<AppItem> apps) {
            this.originalApps = new ArrayList<>(apps);
            this.displayedApps = new ArrayList<>(apps);
            setHasStableIds(true);
        }

        void filter(String query) {
            displayedApps.clear();
            if (android.text.TextUtils.isEmpty(query)) {
                displayedApps.addAll(originalApps);
            } else {
                String q = query.toLowerCase(Locale.getDefault());
                for (AppItem item : originalApps) {
                    if (item.label.toLowerCase(Locale.getDefault()).contains(q)) {
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
            
            // Geri donusum sirasinda yanlis ikon gosterilmesini engelle (Turkce karakter yok)
            holder.iconView.setTag(item.packageName);

            Drawable cachedIcon = iconCache != null ? iconCache.get(item.packageName) : null;
            if (cachedIcon != null) {
                holder.iconView.setImageDrawable(cachedIcon);
            } else {
                // Varsayilan bos/placeholder ikon set et (Turkce karakter yok)
                holder.iconView.setImageDrawable(null);
                if (holder.iconView.getContext() != null) {
                    loadIconAsync(holder.iconView, holder.iconView.getContext(), item.packageName);
                }
            }

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

        @Override
        public long getItemId(int position) {
            return displayedApps.get(position).packageName.hashCode();
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
        View dialogView = LayoutInflater.from(getContext()).inflate(net.osmand.plus.R.layout.dialog_app_options, null);
        
        ImageView iconView = dialogView.findViewById(net.osmand.plus.R.id.dialog_app_icon);
        TextView labelView = dialogView.findViewById(net.osmand.plus.R.id.dialog_app_label);
        iconView.setTag(item.packageName);
        
        Drawable icon = iconCache != null ? iconCache.get(item.packageName) : null;
        if (icon != null) {
            iconView.setImageDrawable(icon);
        } else if (getContext() != null) {
            loadIconAsync(iconView, getContext(), item.packageName);
        }
        labelView.setText(item.label);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialogView.findViewById(net.osmand.plus.R.id.btn_dock_standard).setOnClickListener(v -> {
            addToDock(item, LaunchMode.FULL_SCREEN);
            dialog.dismiss();
        });

        View splitButton = dialogView.findViewById(net.osmand.plus.R.id.btn_dock_split);
        splitButton.setOnClickListener(v -> {
            addToDock(item, LaunchMode.SPLIT_SCREEN);
            dialog.dismiss();
        });

        View overlayButton = dialogView.findViewById(net.osmand.plus.R.id.btn_dock_overlay);
        overlayButton.setOnClickListener(v -> {
            addToDock(item, LaunchMode.OVERLAY);
            dialog.dismiss();
        });

        View appInfoButton = dialogView.findViewById(net.osmand.plus.R.id.btn_app_info);
        appInfoButton.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + item.packageName));
            startActivity(intent);
            dialog.dismiss();
        });

        View uninstallButton = dialogView.findViewById(net.osmand.plus.R.id.btn_uninstall);
        uninstallButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(android.net.Uri.parse("package:" + item.packageName));
            startActivity(intent);
            dialog.dismiss();
        });

        if (net.osmand.plus.carlauncher.dock.InternalApp.isInternalApp(item.packageName)) {
            splitButton.setVisibility(View.GONE);
            overlayButton.setVisibility(View.GONE);
            appInfoButton.setVisibility(View.GONE);
            uninstallButton.setVisibility(View.GONE);
        }

        dialog.show();
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
        Drawable icon = iconCache != null ? iconCache.get(item.packageName) : null;
        if (icon == null && getContext() != null) {
            icon = getAppIcon(getContext(), item.packageName);
        }
        AppShortcut shortcut = new AppShortcut(item.packageName, item.label, icon, order, mode);

        if (dockManager.addShortcut(shortcut)) {
            Toast.makeText(getContext(), item.label + " dock'a eklendi.", Toast.LENGTH_SHORT).show();

            // Package-scoped broadcast is sufficient for the dock receiver.
            Intent updateIntent = new Intent("net.osmand.carlauncher.DOCK_UPDATED");
            updateIntent.setPackage(getContext().getPackageName()); // Security
            getContext().sendBroadcast(updateIntent);
        }
        closeDrawer();
    }

    private void launchApp(String packageName) {
        // Handle internal apps
        if (net.osmand.plus.carlauncher.dock.InternalApp.isInternalApp(packageName)) {
            net.osmand.plus.carlauncher.dock.InternalAppLauncher.launch(getContext(), packageName);
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



    public static List<AppItem> getCachedApps() {
        return cachedApps;
    }

    public static android.util.LruCache<String, Drawable> getIconCache() {
        return iconCache;
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
