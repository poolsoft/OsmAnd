package net.osmand.plus.carlauncher.headunit;

public interface HeadUnitListener {
    // --- Donanım ve Telemetri (CAN-Bus) ---
    void onSpeedUpdated(float speedKmh);
    void onBatteryVoltageUpdated(float voltage);
    void onHeadlightStateChanged(boolean isLightOn);
    
    // --- Medya ve Radyo ---
    void onPlaybackStateChanged(boolean isPlaying);
    void onTrackInfoChanged(String title, String artist, String albumArtPath);
    void onRadioFrequencyChanged(String band, float freqMHz);

    /**
     * Vendor adapters use this only after a captured hardware input has been
     * distinguished from the application's own outgoing control broadcast.
     */
    default void onMediaKeyEvent(int keyCode) {
    }
}
