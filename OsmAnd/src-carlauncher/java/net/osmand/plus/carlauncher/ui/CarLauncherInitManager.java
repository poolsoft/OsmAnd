package net.osmand.plus.carlauncher.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Car Launcher startup coordinator.
 *
 * This class never starts or changes OsmAnd core initialization. It only observes
 * lifecycle milestones exposed by OsmAnd and keeps launcher-only work from
 * competing with map startup.
 */
public class CarLauncherInitManager {

    private static final String TAG = "CarLauncherInit";
    private static final long LOW_RAM_POST_CORE_GRACE_MS = 1500L;
    private static final long SLOW_CORE_WARNING_MS = 20000L;
    private static CarLauncherInitManager instance;
    private volatile boolean isCoreReady;
    private volatile boolean isBackgroundReady;
    private volatile boolean isLauncherBackgroundWorkReleased;
    private boolean startupProfileConfigured;
    private boolean lowRamProfile;
    
    // Performance & Benchmark Metrics
    private long initStartTimeMs = 0;
    private long uiReadyTimeMs = 0;
    private long coreReadyTimeMs = 0;
    private long backgroundReadyTimeMs = 0;
    private long initialMemoryBytes = 0;

    private final CopyOnWriteArrayList<OnInitStateListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnLauncherBackgroundReadyListener>
            backgroundWorkListeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable lowRamCoreGraceRunnable =
            () -> releaseLauncherBackgroundWork("low_ram_core_grace");

    public interface OnInitStateListener {
        void onCoreReady();
    }

    public interface OnLauncherBackgroundReadyListener {
        void onLauncherBackgroundReady();
    }

    public static synchronized CarLauncherInitManager getInstance() {
        if (instance == null) {
            instance = new CarLauncherInitManager();
        }
        return instance;
    }

    public void startInitTimer() {
        if (initStartTimeMs == 0) {
            initStartTimeMs = SystemClock.elapsedRealtime();
            Runtime rt = Runtime.getRuntime();
            initialMemoryBytes = rt.totalMemory() - rt.freeMemory();
            mainHandler.postDelayed(() -> {
                if (!isCoreReady) {
                    Log.w(TAG, "Map core is still initializing after "
                            + SLOW_CORE_WARNING_MS + " ms; launcher work remains deferred");
                }
            }, SLOW_CORE_WARNING_MS);
        }
    }

    public synchronized void configureStartupProfile(Context context) {
        if (startupProfileConfigured || context == null) {
            return;
        }
        lowRamProfile = detectLowRamDevice(context.getApplicationContext());
        startupProfileConfigured = true;
        Log.i(TAG, "Startup profile=" + (lowRamProfile ? "LOW_RAM" : "STANDARD"));
    }

    public void markUiReady() {
        if (uiReadyTimeMs == 0) {
            uiReadyTimeMs = SystemClock.elapsedRealtime();
            Log.i(TAG, "Launcher UI ready in " + getUiReadyDurationMs() + " ms");
        }
    }

    public boolean isCoreReady() {
        return isCoreReady;
    }

    public void addListener(OnInitStateListener listener) {
        if (listener == null) return;
        if (isCoreReady) {
            mainHandler.post(listener::onCoreReady);
            return;
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        // Handle readiness racing with the listener registration.
        if (isCoreReady && listeners.remove(listener)) {
            mainHandler.post(listener::onCoreReady);
        }
    }

    public void removeListener(OnInitStateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void addLauncherBackgroundReadyListener(
            OnLauncherBackgroundReadyListener listener) {
        if (listener == null) {
            return;
        }
        if (isLauncherBackgroundWorkReleased) {
            mainHandler.post(listener::onLauncherBackgroundReady);
            return;
        }
        if (!backgroundWorkListeners.contains(listener)) {
            backgroundWorkListeners.add(listener);
        }
        if (isLauncherBackgroundWorkReleased && backgroundWorkListeners.remove(listener)) {
            mainHandler.post(listener::onLauncherBackgroundReady);
        }
    }

    public void removeLauncherBackgroundReadyListener(
            OnLauncherBackgroundReadyListener listener) {
        if (listener != null) {
            backgroundWorkListeners.remove(listener);
        }
    }

    public void markCoreReady() {
        markCoreReady((Context) null);
    }

    public void markCoreReady(Context context) {
        if (!isCoreReady) {
            isCoreReady = true;
            coreReadyTimeMs = SystemClock.elapsedRealtime();
            
            long elapsedTimeMs = initStartTimeMs > 0 ? (coreReadyTimeMs - initStartTimeMs) : 0;
            double elapsedTimeSec = elapsedTimeMs / 1000.0;

            mainHandler.post(() -> {
                if (elapsedTimeMs > 0) {
                    Log.i(TAG, String.format(Locale.US,
                            "Map core ready in %d ms (%.2f s)", elapsedTimeMs, elapsedTimeSec));
                }
                notifyCoreReady();
                if (lowRamProfile) {
                    // The map core is already ready. Give its first render a short,
                    // bounded exclusive window before launcher-only heavy work.
                    mainHandler.removeCallbacks(lowRamCoreGraceRunnable);
                    mainHandler.postDelayed(lowRamCoreGraceRunnable,
                            LOW_RAM_POST_CORE_GRACE_MS);
                } else {
                    releaseLauncherBackgroundWork("standard_core_ready");
                }
            });
        }
    }

    private void notifyCoreReady() {
        for (OnInitStateListener listener : listeners) {
            try {
                listener.onCoreReady();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        listeners.clear();
    }

    public void markBackgroundReady() {
        if (!isBackgroundReady) {
            isBackgroundReady = true;
            backgroundReadyTimeMs = SystemClock.elapsedRealtime();
        }
        mainHandler.removeCallbacks(lowRamCoreGraceRunnable);
        releaseLauncherBackgroundWork("osmand_background_ready");
    }

    public long getBackgroundReadyDurationMs() {
        if (initStartTimeMs > 0 && backgroundReadyTimeMs > 0) {
            return backgroundReadyTimeMs - initStartTimeMs;
        }
        return 0;
    }

    public boolean isLowRamDevice(Context context) {
        if (!startupProfileConfigured && context != null) {
            configureStartupProfile(context);
        }
        return lowRamProfile;
    }

    public String getStartupProfileName(Context context) {
        if (context == null) {
            return lowRamProfile ? "LOW_RAM" : "STANDARD";
        }
        return context.getString(lowRamProfile
                ? net.osmand.plus.R.string.car_startup_profile_low_ram
                : net.osmand.plus.R.string.car_startup_profile_standard);
    }

    private boolean detectLowRamDevice(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return false;
        }
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        long threeGb = 3L * 1024L * 1024L * 1024L;
        boolean limitedTotalMemory = memoryInfo.totalMem > 0
                && memoryInfo.totalMem <= threeGb;
        boolean lowAvailableMemory = memoryInfo.totalMem > 0
                && memoryInfo.availMem * 4L < memoryInfo.totalMem;
        return am.isLowRamDevice() || memoryInfo.lowMemory
                || limitedTotalMemory || lowAvailableMemory;
    }

    private synchronized void releaseLauncherBackgroundWork(String reason) {
        if (isLauncherBackgroundWorkReleased) {
            return;
        }
        isLauncherBackgroundWorkReleased = true;
        Log.i(TAG, "Launcher background work released: " + reason);
        mainHandler.post(() -> {
            for (OnLauncherBackgroundReadyListener listener : backgroundWorkListeners) {
                try {
                    listener.onLauncherBackgroundReady();
                } catch (Exception e) {
                    Log.e(TAG, "Launcher background listener failed", e);
                }
            }
            backgroundWorkListeners.clear();
        });
    }

    // --- Statistics Helper Methods ---

    public long getUiReadyDurationMs() {
        if (initStartTimeMs > 0 && uiReadyTimeMs > 0) {
            return uiReadyTimeMs - initStartTimeMs;
        }
        return 0;
    }

    public long getCoreReadyDurationMs() {
        if (initStartTimeMs > 0 && coreReadyTimeMs > 0) {
            return coreReadyTimeMs - initStartTimeMs;
        }
        return 0;
    }

    public long getInitialMemoryMB() {
        return initialMemoryBytes / (1024 * 1024);
    }

    public long getCurrentUsedMemoryMB() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    public long getMaxHeapMemoryMB() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    public String getSystemAvailableRamGB(Context context) {
        if (context == null) return "Bilinmiyor";
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                double availGB = mi.availMem / (1024.0 * 1024.0 * 1024.0);
                double totalGB = mi.totalMem / (1024.0 * 1024.0 * 1024.0);
                return String.format(Locale.US, "%.2f GB / %.2f GB", availGB, totalGB);
            }
        } catch (Exception e) {
            // ignore
        }
        return "Bilinmiyor";
    }

    public String getFormattedStatsSummary(Context context) {
        long uiMs = getUiReadyDurationMs();
        long coreMs = getCoreReadyDurationMs();
        long backgroundMs = getBackgroundReadyDurationMs();
        long initRam = getInitialMemoryMB();
        long currRam = getCurrentUsedMemoryMB();

        boolean fastBoot = true;
        if (context != null) {
            fastBoot = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(context).isFastBootEnabled();
        }

        String modeStr = fastBoot ? "Hızlı Başlatma" : "Klasik Yükleme";
        return String.format(Locale.US, 
                "⚡ Arayüz: %d ms | 🗺️ Harita: %d ms | ✅ Tamamı: %d ms (%s)\n💾 Başlangıç RAM: %d MB | 📊 Şu Anki RAM: %d MB",
                uiMs, coreMs, backgroundMs, modeStr, initRam, currRam);
    }

    public String getFormattedStatsDetails(Context context) {
        long uiMs = getUiReadyDurationMs();
        long coreMs = getCoreReadyDurationMs();
        long backgroundMs = getBackgroundReadyDurationMs();
        long initRam = getInitialMemoryMB();
        long currRam = getCurrentUsedMemoryMB();
        long maxRam = getMaxHeapMemoryMB();
        String sysRam = getSystemAvailableRamGB(context);

        boolean fastBoot = true;
        if (context != null) {
            fastBoot = net.osmand.plus.carlauncher.CarLauncherSettings.getInstance(context).isFastBootEnabled();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 CAR LAUNCHER AÇILIŞ VE PERFORMANS RAPORU\n");
        sb.append("────────────────────────────────────────────\n");
        sb.append("⚡ Arayüz Açılma Süresi: ").append(uiMs > 0 ? uiMs + " ms (" + String.format(Locale.US, "%.2f", uiMs / 1000.0) + " sn)" : "Ölçülemedi").append("\n");
        sb.append("🗺️ Harita Motoru Yükleme Süresi: ").append(coreMs > 0 ? coreMs + " ms (" + String.format(Locale.US, "%.2f", coreMs / 1000.0) + " sn)" : "Yükleniyor...").append("\n");
        sb.append("✅ Arka Plan Başlatma Süresi: ").append(backgroundMs > 0 ? backgroundMs + " ms (" + String.format(Locale.US, "%.2f", backgroundMs / 1000.0) + " sn)" : "Devam ediyor...").append("\n");
        sb.append("🚀 Başlatma Modu: ").append(fastBoot ? "Hızlı Başlatma (Arka Planda)" : "Klasik Yükleme (Senkron)").append("\n\n");
        sb.append("🧠 Cihaz Profili: ").append(getStartupProfileName(context)).append("\n");
        sb.append("💾 Başlangıçtaki RAM (Heap): ").append(initRam).append(" MB\n");
        sb.append("📊 O Anki Aktif RAM (Heap): ").append(currRam).append(" MB / ").append(maxRam).append(" MB\n");
        sb.append("📱 Cihaz Fiziksel RAM (Boş/Toplam): ").append(sysRam).append("\n");
        return sb.toString();
    }
}
