package net.osmand.plus.carlauncher.ui;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.carlauncher.CarLauncherSettings;

/**
 * Sağ panel içeriğini yöneten sınıf.
 * Android Auto UI mantığı: panel müzik/bildirim/app drawer gösterir.
 */
public class PanelContentManager {

    public enum PanelContent {
        WIDGETS,      // Varsayilan: widget listesi
        MUSIC,        // Müzik player
        NOTIFICATION, // Bildirim + müzik (üst üste)
        APP_DRAWER,   // App drawer listesi
        WEATHER       // Hava durumu
    }

    private PanelContent currentContent = PanelContent.WIDGETS;
    private final FragmentManager fragmentManager;
    private final int containerId;
    private Runnable onFullScreenToggle;

    public PanelContentManager(FragmentManager fragmentManager, int containerId) {
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
    }

    public void setOnFullScreenToggle(Runnable r) {
        this.onFullScreenToggle = r;
    }

    /**
     * Panel içeriğini değiştirir.
     * Her değişimde eski fragment remove edilir, yenisi eklenir.
     * APP_DRAWER/MUSIC icin fullscreen toggle otomatik.
     */
    public void setContent(PanelContent content) {
        if (currentContent == content) return;
        currentContent = content;
        
        // Fullscreen toggle
        boolean needsFullScreen = (content == PanelContent.APP_DRAWER || content == PanelContent.MUSIC);
        boolean wasFullScreen = (content == PanelContent.WIDGETS);
        if (onFullScreenToggle != null && (needsFullScreen || wasFullScreen)) {
            onFullScreenToggle.run();
        }

        Fragment fragment = null;
        String tag = content.name();

        // Mevcut fragment'i temizle
        Fragment old = fragmentManager.findFragmentById(containerId);
        if (old != null) {
            fragmentManager.beginTransaction().remove(old).commitNowAllowingStateLoss();
        }

        switch (content) {
            case WIDGETS:
                fragment = new WidgetPanelFragment();
                break;
            case MUSIC:
                fragment = new MusicPlayerFragment();
                break;
            case APP_DRAWER:
                fragment = new AppDrawerFragment();
                break;
            case WEATHER:
                try {
                    Class<?> weatherClass = Class.forName("net.osmand.plus.carlauncher.ui.WeatherConfigDialog");
                    fragment = (Fragment) weatherClass.newInstance();
                } catch (Exception e) {
                    fragment = new WidgetPanelFragment(); // fallback
                }
                break;
        }

        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .replace(containerId, fragment, tag)
                    .commitAllowingStateLoss();
        }
    }

    public PanelContent getCurrentContent() {
        return currentContent;
    }
}