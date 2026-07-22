package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import net.osmand.plus.OsmandApplication;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CarLauncher Izole Baslatici Yonetici (Core Izolasyon Kurali).
 * OsmAnd cekirdek servislerinin arka planda asenkron yuklenmesini yonetir.
 * Instant Shell modunda CarLauncher arayuzu 1s altinda acilirken,
 * harita ve telemetry servisleri bu yonetici uzerinden yuklenme durumunu takip eder.
 */
public class CarLauncherInitManager {

    private static CarLauncherInitManager instance;
    private boolean isCoreReady = false;
    private boolean isInitializing = false;
    private final CopyOnWriteArrayList<OnInitStateListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService initExecutor = Executors.newSingleThreadExecutor();

    public interface OnInitStateListener {
        void onCoreReady();
    }

    public static synchronized CarLauncherInitManager getInstance() {
        if (instance == null) {
            instance = new CarLauncherInitManager();
        }
        return instance;
    }

    public boolean isCoreReady() {
        return isCoreReady;
    }

    public void addListener(OnInitStateListener listener) {
        if (listener == null) return;
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        if (isCoreReady) {
            mainHandler.post(listener::onCoreReady);
        }
    }

    public void removeListener(OnInitStateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void startAsyncCoreInit(Context context) {
        if (isCoreReady || isInitializing) return;
        isInitializing = true;

        initExecutor.execute(() -> {
            try {
                OsmandApplication app = (OsmandApplication) context.getApplicationContext();
                if (app != null && app.getAppInitializer() != null) {
                    app.checkApplicationIsBeingInitialized(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isCoreReady = true;
                isInitializing = false;
                mainHandler.post(this::notifyCoreReady);
            }
        });
    }

    public void markCoreReady() {
        if (!isCoreReady) {
            isCoreReady = true;
            isInitializing = false;
            mainHandler.post(this::notifyCoreReady);
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
    }
}
