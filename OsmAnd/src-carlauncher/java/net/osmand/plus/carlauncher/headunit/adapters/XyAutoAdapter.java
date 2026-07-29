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

public class XyAutoAdapter implements HeadUnitAdapter {

    private HeadUnitListener listener;
    private BroadcastReceiver receiver;

    @Override
    public String getManufacturerName() {
        return "XyAuto";
    }

    @Override
    public boolean isSupported(Context context) {
        String[] packages = {
                "com.xyauto.common",
                "com.acloud.stub.localmusic",
                "com.acloud.stub.extradio"
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
        return fingerprint.contains("xyauto") || fingerprint.contains("acloud");
    }

    @Override
    public void startListening(Context context, HeadUnitListener listener) {
        this.listener = listener;
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    HardwareEventRecorder.getInstance(context).recordIntent("XYAUTO_ADAPTER", intent);
                    handleIntent(intent);
                }
            };

            IntentFilter filter = new IntentFilter();
            // Telemetry (CAN-Bus)
            filter.addAction("xy.auto.canbus.speed");
            filter.addAction("com.xygala.canbus.tata.speed");
            filter.addAction("xy.auto.canbus.battery");
            filter.addAction("xy.auto.canbus.light");
            filter.addAction("xy.xygala.lamplet");
            
            // Local Music
            filter.addAction("update.widget.playbtnstate");
            filter.addAction("update.widget.songname");
            
            // Radio
            filter.addAction("com.android.radio.widget.freq_volue");

            // Bluetooth Music
            filter.addAction("com.acloud.intent.play_status");
            filter.addAction("xy.android.playpause");
            filter.addAction("xy.android.nextmedia");
            filter.addAction("xy.android.previousmedia");

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(receiver, filter);
                }
            } catch (Exception e) {
                HardwareEventRecorder.getInstance(context).record("XYAUTO_ADAPTER",
                        "receiver_registration_failed=" + e.getClass().getSimpleName());
                receiver = null;
            }
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
        listener = null;
    }

    private void handleIntent(Intent intent) {
        if (listener == null || intent == null || intent.getAction() == null) return;
        
        String action = intent.getAction();

        // Speed
        if ("xy.auto.canbus.speed".equals(action) || "com.xygala.canbus.tata.speed".equals(action)) {
            int speed = intent.getIntExtra("speed", -1);
            if (speed == -1) speed = intent.getIntExtra("Engine_speed", 0);
            listener.onSpeedUpdated((float) speed);
        }
        // Battery
        else if ("xy.auto.canbus.battery".equals(action)) {
            int battery = intent.getIntExtra("battery", 0);
            listener.onBatteryVoltageUpdated((float) battery);
        }
        // Headlights
        else if ("xy.auto.canbus.light".equals(action)) {
            int light = intent.getIntExtra("light", 0);
            listener.onHeadlightStateChanged(light > 0);
        } else if ("xy.xygala.lamplet".equals(action)) {
            int lamplet = intent.getIntExtra("lamplet_state", 0);
            listener.onHeadlightStateChanged(lamplet != 0); // 0 means on according to their logic sometimes, we'll map generic
        }
        
        // Music Playback Status
        else if ("update.widget.playbtnstate".equals(action)) {
            boolean isPlaying = intent.getBooleanExtra("PlayState", false);
            listener.onPlaybackStateChanged(isPlaying);
        }
        // Music Metadata
        else if ("update.widget.songname".equals(action)) {
            boolean isPlaying = intent.getBooleanExtra("PlayState", false);
            String title = intent.getStringExtra("curplaysong");
            String albumArt = intent.getStringExtra("artistPicPath");
            listener.onPlaybackStateChanged(isPlaying);
            listener.onTrackInfoChanged(title, null, albumArt);
        }
        // Bluetooth Music Status
        else if ("com.acloud.intent.play_status".equals(action)) {
            byte status = intent.getByteExtra("play_status", (byte) 0);
            listener.onPlaybackStateChanged(status == 1); // 1 = playing
        }
        // Radio
        else if ("com.android.radio.widget.freq_volue".equals(action)) {
            String band = intent.getStringExtra("fmoram");
            int freq = intent.getIntExtra("freq", 0);
            if (band != null) {
                float realFreq = freq;
                if (band.equalsIgnoreCase("FM") || band.equalsIgnoreCase("fm")) {
                    realFreq = freq / 100.0f;
                } else if (band.equalsIgnoreCase("FM1")) {
                    realFreq = freq / 1000.0f;
                }
                listener.onRadioFrequencyChanged(band, realFreq);
            }
        }
    }

    // --- Media Controls via Broadcat & Service ---
    @Override
    public void playMusic(Context context) {
        context.sendBroadcast(new Intent("xy.android.playpause"));
        // Also BT force play
        context.sendBroadcast(new Intent("xy.android.forceplay"));
    }

    @Override
    public void pauseMusic(Context context) {
        context.sendBroadcast(new Intent("xy.android.playpause"));
        // Also BT force pause
        context.sendBroadcast(new Intent("xy.android.forcepause"));
    }

    @Override
    public void nextTrack(Context context) {
        context.sendBroadcast(new Intent("xy.android.nextmedia"));
    }

    @Override
    public void previousTrack(Context context) {
        context.sendBroadcast(new Intent("xy.android.previousmedia"));
    }
}
