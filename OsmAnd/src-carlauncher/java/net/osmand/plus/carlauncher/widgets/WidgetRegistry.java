package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import net.osmand.plus.OsmandApplication;
import java.util.ArrayList;
import java.util.List;

/**
 * Dinamik Widget Kayıt Sistemi.
 * Yeni widget eklemek icin buraya register etmek yeterlidir.
 * UI (WidgetPanelFragment) listeyi buradan otomatik ceker.
 */
public class WidgetRegistry {

    // Sabit Widget Tipleri
    public static final String TYPE_SPEED = "speed";
    public static final String TYPE_MUSIC = "music";
    public static final String TYPE_NAVIGATION = "navigation";
    public static final String TYPE_COMPASS = "compass";
    public static final String TYPE_OBD = "obd";
    public static final String TYPE_CLOCK = "clock";
    public static final String TYPE_ANTENNA = "antenna";

    // Widget Yaratma Arayuzu (Lambda icin)
    public interface WidgetCreator {
        BaseWidget create(Context context, OsmandApplication app);
    }

    // Widget Tanim Bilgisi
    public static class WidgetEntry {
        public final String typeId;
        public final String displayName;
        public final WidgetCreator creator;

        public WidgetEntry(String typeId, String displayName, WidgetCreator creator) {
            this.typeId = typeId;
            this.displayName = displayName;
            this.creator = creator;
        }
    }

    private static final List<WidgetEntry> availableWidgets = new ArrayList<>();

    // Statik blok ile temel widget'lari kaydediyoruz.
    static {
        register(TYPE_SPEED, "Hız Göstergesi", SpeedWidget::new);
        register(TYPE_MUSIC, "Müzik Çalar", MusicWidget::new);
        register(TYPE_NAVIGATION, "Navigasyon", NavigationWidget::new);
        register(TYPE_COMPASS, "Pusula", DirectionWidget::new);
        register(TYPE_OBD, "OBD Bilgileri", OBDWidget::new);
        register(TYPE_CLOCK, "Analog Saat", (ctx, app) -> new Material3ClockWidget(ctx));
        register(TYPE_ANTENNA, "Rakım (Anten)", AntennaWidget::new);
    }

    /**
     * Yeni bir widget tipi kaydet.
     */
    public static void register(String typeId, String displayName, WidgetCreator creator) {
        availableWidgets.add(new WidgetEntry(typeId, displayName, creator));
    }

    /**
     * Kayitli tum widget tiplerini getir.
     */
    public static List<WidgetEntry> getAvailableWidgets() {
        return new ArrayList<>(availableWidgets);
    }

    /**
     * ID'ye gore widget olustur.
     */
    public static BaseWidget createWidget(Context context, OsmandApplication app, String typeId) {
        for (WidgetEntry entry : availableWidgets) {
            if (entry.typeId.equals(typeId)) {
                return entry.creator.create(context, app);
            }
        }
        return null;
    }
}
