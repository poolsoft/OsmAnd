package net.osmand.plus.carlauncher.widgets;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.OsmAndFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Widget paneli fragment - Sag taraftaki %30'luk alan.
 * Hiz, saat, yon ve diger widget'lari icerir.
 */
public class WidgetPanelFragment extends Fragment implements OsmAndLocationProvider.OsmAndLocationListener {

    public static final String TAG = "WidgetPanelFragment";

    private OsmandApplication app;
    private Handler handler;
    private Runnable clockUpdater;

    // Widget views
    private TextView clockWidget;
    private TextView speedWidget;
    private TextView directionWidget;
    private TextView altitudeWidget;
    private TextView nextTurnWidget;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
        }
        handler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_widget_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Widget'lari bagla
        clockWidget = view.findViewById(R.id.widget_clock);
        speedWidget = view.findViewById(R.id.widget_speed);
        directionWidget = view.findViewById(R.id.widget_direction);
        altitudeWidget = view.findViewById(R.id.widget_altitude);
        nextTurnWidget = view.findViewById(R.id.widget_next_turn);

        // Saat guncelleme dongusu baslat
        startClockUpdater();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (app != null) {
            app.getLocationProvider().addLocationListener(this);
        }
        startClockUpdater();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (app != null) {
            app.getLocationProvider().removeLocationListener(this);
        }
        stopClockUpdater();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopClockUpdater();
    }

    /**
     * Saat widget'ini her saniye guncelle.
     */
    private void startClockUpdater() {
        if (clockUpdater == null) {
            clockUpdater = new Runnable() {
                @Override
                public void run() {
                    updateClock();
                    handler.postDelayed(this, 1000);
                }
            };
        }
        handler.post(clockUpdater);
    }

    private void stopClockUpdater() {
        if (clockUpdater != null) {
            handler.removeCallbacks(clockUpdater);
        }
    }

    private void updateClock() {
        if (clockWidget != null && isAdded()) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            clockWidget.setText(sdf.format(new Date()));
        }
    }

    @Override
    public void updateLocation(Location location) {
        if (location == null || app == null || !isAdded())
            return;

        // Ana thread'de guncelle
        handler.post(() -> {
            // Hiz
            if (speedWidget != null) {
                if (location.hasSpeed()) {
                    String speed = OsmAndFormatter.getFormattedSpeed(location.getSpeed(), app);
                    speedWidget.setText(speed);
                } else {
                    speedWidget.setText("--");
                }
            }

            // Yon (bearing)
            if (directionWidget != null) {
                if (location.hasBearing()) {
                    int bearing = (int) location.getBearing();
                    String direction = getDirectionString(bearing);
                    directionWidget.setText(direction + " " + bearing + "°");
                } else {
                    directionWidget.setText("--");
                }
            }

            // Irtifa
            if (altitudeWidget != null) {
                if (location.hasAltitude()) {
                    String altitude = OsmAndFormatter.getFormattedAlt(location.getAltitude(), app);
                    altitudeWidget.setText(altitude);
                } else {
                    altitudeWidget.setText("--");
                }
            }
        });
    }

    /**
     * Bearing degerini yon stringine cevir.
     */
    private String getDirectionString(int bearing) {
        if (bearing >= 337.5 || bearing < 22.5)
            return "K"; // Kuzey
        if (bearing >= 22.5 && bearing < 67.5)
            return "KD"; // Kuzeydogu
        if (bearing >= 67.5 && bearing < 112.5)
            return "D"; // Dogu
        if (bearing >= 112.5 && bearing < 157.5)
            return "GD"; // Guneydogu
        if (bearing >= 157.5 && bearing < 202.5)
            return "G"; // Guney
        if (bearing >= 202.5 && bearing < 247.5)
            return "GB"; // Guneybati
        if (bearing >= 247.5 && bearing < 292.5)
            return "B"; // Bati
        if (bearing >= 292.5 && bearing < 337.5)
            return "KB"; // Kuzeybati
        return "";
    }

    /**
     * Navigasyon sonraki donus bilgisini guncelle.
     */
    public void updateNextTurn(String turnInfo) {
        if (nextTurnWidget != null && isAdded()) {
            handler.post(() -> nextTurnWidget.setText(turnInfo));
        }
    }
}
