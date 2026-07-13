package net.osmand.plus.carlauncher.headunit;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.carlauncher.headunit.adapters.XyAutoAdapter;

public class HeadUnitManager {

    private static HeadUnitManager instance;
    private HeadUnitAdapter activeAdapter;
    private final List<HeadUnitListener> listeners = new ArrayList<>();

    private HeadUnitManager(Context context) {
        // Find supported adapter
        List<HeadUnitAdapter> availableAdapters = new ArrayList<>();
        availableAdapters.add(new XyAutoAdapter());
        // Add more adapters here in the future (e.g. HcnAdapter)

        for (HeadUnitAdapter adapter : availableAdapters) {
            if (adapter.isSupported(context)) {
                this.activeAdapter = adapter;
                break;
            }
        }

        if (this.activeAdapter != null) {
            this.activeAdapter.startListening(context, new HeadUnitListener() {
                @Override
                public void onSpeedUpdated(float speedKmh) {
                    for (HeadUnitListener listener : listeners) listener.onSpeedUpdated(speedKmh);
                }

                @Override
                public void onBatteryVoltageUpdated(float voltage) {
                    for (HeadUnitListener listener : listeners) listener.onBatteryVoltageUpdated(voltage);
                }

                @Override
                public void onHeadlightStateChanged(boolean isLightOn) {
                    for (HeadUnitListener listener : listeners) listener.onHeadlightStateChanged(isLightOn);
                }

                @Override
                public void onPlaybackStateChanged(boolean isPlaying) {
                    for (HeadUnitListener listener : listeners) listener.onPlaybackStateChanged(isPlaying);
                }

                @Override
                public void onTrackInfoChanged(String title, String artist, String albumArtPath) {
                    for (HeadUnitListener listener : listeners) listener.onTrackInfoChanged(title, artist, albumArtPath);
                }

                @Override
                public void onRadioFrequencyChanged(String band, float freqMHz) {
                    for (HeadUnitListener listener : listeners) listener.onRadioFrequencyChanged(band, freqMHz);
                }
            });
        }
    }

    public static synchronized HeadUnitManager getInstance(Context context) {
        if (instance == null) {
            instance = new HeadUnitManager(context.getApplicationContext());
        }
        return instance;
    }

    public void addListener(HeadUnitListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(HeadUnitListener listener) {
        listeners.remove(listener);
    }

    public boolean hasActiveAdapter() {
        return activeAdapter != null;
    }

    public HeadUnitAdapter getActiveAdapter() {
        return activeAdapter;
    }

}
