package net.osmand.plus.carlauncher.headunit;

import android.content.Context;

public interface HeadUnitAdapter {
    
    /**
     * Teyp veya sistem markasını dondurur. (Örn: "XyAuto", "HCN")
     */
    String getManufacturerName();

    /**
     * Sistemin mevcut cihazda çalışıp çalışamayacağını kontrol eder.
     * Genellikle paket adı kontrolü yapılır.
     */
    boolean isSupported(Context context);

    /**
     * Yayınları ve verileri dinlemeye başlar.
     */
    void startListening(Context context, HeadUnitListener listener);

    /**
     * Dinlemeyi sonlandırır.
     */
    void stopListening(Context context);

    // --- Medya Kontrolleri ---
    void playMusic(Context context);
    void pauseMusic(Context context);
    void nextTrack(Context context);
    void previousTrack(Context context);
}
