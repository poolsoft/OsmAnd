package net.osmand.plus.carlauncher.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

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

    private static CarLauncherInitManager instance;
    private volatile boolean isCoreReady;
    private volatile boolean isBackgroundReady;
    
    // Performance & Benchmark Metrics
    private long initStartTimeMs = 0;
    private long uiReadyTimeMs = 0;
    private long coreReadyTimeMs = 0;
    private long backgroundReadyTimeMs = 0;
    private long initialMemoryBytes = 0;

    private final CopyOnWriteArrayList<OnInitStateListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnInitStateListener {
        void onCoreReady();
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
        }
    }

    public void markUiReady() {
        if (uiReadyTimeMs == 0) {
            uiReadyTimeMs = SystemClock.elapsedRealtime();
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
                if (context != null && elapsedTimeMs > 0) {
                    String timeMsg = String.format(Locale.US, "⏱️ OsmAnd Harita Motoru Yüklendi!\nYükleme Süresi: %d ms (%.2f saniye)", elapsedTimeMs, elapsedTimeSec);
                    android.widget.Toast.makeText(context, timeMsg, android.widget.Toast.LENGTH_LONG).show();
                }
                notifyCoreReady();
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
    }

    public long getBackgroundReadyDurationMs() {
        if (initStartTimeMs > 0 && backgroundReadyTimeMs > 0) {
            return backgroundReadyTimeMs - initStartTimeMs;
        }
        return 0;
    }

    public boolean isLowRamDevice(Context context) {
        if (context == null) {
            return false;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return false;
        }
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        long threeGb = 3L * 1024L * 1024L * 1024L;
        return am.isLowRamDevice() || memoryInfo.totalMem <= threeGb;
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
        sb.append("💾 Başlangıçtaki RAM (Heap): ").append(initRam).append(" MB\n");
        sb.append("📊 O Anki Aktif RAM (Heap): ").append(currRam).append(" MB / ").append(maxRam).append(" MB\n");
        sb.append("📱 Cihaz Fiziksel RAM (Boş/Toplam): ").append(sysRam).append("\n");
        return sb.toString();
    }
}
