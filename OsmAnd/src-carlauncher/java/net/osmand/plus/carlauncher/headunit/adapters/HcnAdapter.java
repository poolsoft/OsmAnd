package net.osmand.plus.carlauncher.headunit.adapters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Locale;

import net.osmand.plus.carlauncher.headunit.HeadUnitAdapter;
import net.osmand.plus.carlauncher.headunit.HeadUnitListener;
import net.osmand.plus.carlauncher.headunit.diagnostics.HardwareEventRecorder;

/**
 * HCN integration based on captured com.hcn.AutoMediaPlayer / TXZ traffic.
 * Unknown events are intentionally only recorded until their semantics are
 * confirmed on real hardware.
 */
public class HcnAdapter implements HeadUnitAdapter {

    private static final String ACTION_NEXT = "com.auto.apimediaplayer.notification.NEXT";
    private static final String ACTION_PREVIOUS = "com.auto.apimediaplayer.notification.PREV";
    private static final String ACTION_PLAY_PAUSE = "com.auto.apimediaplayer.notification.PLAYPAUSE";

    private BroadcastReceiver receiver;

    @Override
    public String getManufacturerName() {
        return "HCN";
    }

    @Override
    public boolean isSupported(Context context) {
        String[] packages = {
                "com.hcn.AutoMediaPlayer",
                "com.hcn.autoradio",
                "com.hcn.mediaservice"
        };
        for (String packageName : packages) {
            try {
                context.getPackageManager().getPackageInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        String fingerprint = (Build.MANUFACTURER + " " + Build.BRAND + " "
                + Build.DEVICE + " " + Build.PRODUCT).toLowerCase(Locale.US);
        return fingerprint.contains("hcn");
    }

    @Override
    public void startListening(Context context, HeadUnitListener listener) {
        if (receiver != null) {
            return;
        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                HardwareEventRecorder.getInstance(receiverContext)
                        .recordIntent("HCN_ADAPTER", intent);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction("com.txznet.extra.next");
        filter.addAction("com.txznet.extra.pre");
        filter.addAction("com.txznet.extra.play");
        filter.addAction("com.txznet.extra.pause");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
        } catch (Exception e) {
            HardwareEventRecorder.getInstance(context).record("HCN_ADAPTER",
                    "receiver_registration_failed=" + e.getClass().getSimpleName());
            receiver = null;
        }
    }

    @Override
    public void stopListening(Context context) {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
            receiver = null;
        }
    }

    @Override
    public void playMusic(Context context) {
        context.sendBroadcast(new Intent(ACTION_PLAY_PAUSE));
    }

    @Override
    public void pauseMusic(Context context) {
        context.sendBroadcast(new Intent(ACTION_PLAY_PAUSE));
    }

    @Override
    public void nextTrack(Context context) {
        context.sendBroadcast(new Intent(ACTION_NEXT));
    }

    @Override
    public void previousTrack(Context context) {
        context.sendBroadcast(new Intent(ACTION_PREVIOUS));
    }
}
