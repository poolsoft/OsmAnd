package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin;
import net.osmand.shared.obd.OBDDataComputer;

/**
 * OBD Dashboard Widget.
 * Displays RPM, Coolant Temp, and Battery Voltage.
 * Requires VehicleMetricsPlugin to be enabled.
 */
public class OBDWidget extends BaseWidget {

    private final OsmandApplication app;
    private VehicleMetricsPlugin plugin;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    private TextView rpmText;
    private TextView tempText;
    private TextView voltText;

    private OBDDataComputer.OBDComputerWidget rpmComputer;
    private OBDDataComputer.OBDComputerWidget tempComputer;
    private OBDDataComputer.OBDComputerWidget voltComputer;

    public OBDWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "obd_dashboard", "Araç Verileri");
        this.app = app;
        this.order = 2; // Position after Clock and Speed
    }

    @Override
    public View createView() {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_obd_dashboard, null);

        rpmText = view.findViewById(R.id.obd_rpm_value);
        tempText = view.findViewById(R.id.obd_temp_value);
        voltText = view.findViewById(R.id.obd_volt_value);

        // Click to open OBD Settings
        view.setOnClickListener(v -> {
            if (plugin != null) {
                // Open OBD Plugin Info/Settings
                // We should ideally open OBDDevicesListFragment but that needs support manager
                // For now, simpler action or just toast if not connected
                if (!plugin.isConnected()) {
                    plugin.connectToLastConnectedDevice(1);
                    android.widget.Toast.makeText(context, "OBD Bağlanıyor...", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(context, "OBD Bağlı: " + plugin.getConnectedDeviceName(), android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });

        rootView = view;
        initComputers();
        startUpdate();
        return rootView;
    }

    private void initComputers() {
        plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin.class);
        if (plugin != null && plugin.isActive()) {
            rpmComputer = OBDDataComputer.INSTANCE.registerWidget(OBDDataComputer.OBDTypeWidget.RPM, 0);
            tempComputer = OBDDataComputer.INSTANCE.registerWidget(OBDDataComputer.OBDTypeWidget.TEMPERATURE_COOLANT, 0);
            voltComputer = OBDDataComputer.INSTANCE.registerWidget(OBDDataComputer.OBDTypeWidget.BATTERY_VOLTAGE, 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startUpdate();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdate();
    }

    private void startUpdate() {
        stopUpdate();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(updateRunnable);
    }

    private void stopUpdate() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }

    private void updateUI() {
        if (plugin == null) {
            plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin.class);
        }

        if (plugin == null || !plugin.isActive()) {
            if (rpmText != null) rpmText.setText("OBD Pasif");
            return;
        }

        if (!plugin.isConnected()) {
            if (rpmText != null) rpmText.setText("Bağlı Değil");
            if (tempText != null) tempText.setText("--");
            if (voltText != null) voltText.setText("--");
            return;
        }

        // Fetch Values
        if (rpmComputer != null && rpmText != null) {
            rpmText.setText(plugin.getWidgetValue(rpmComputer));
        }

        if (tempComputer != null && tempText != null) {
            String tempVal = plugin.getWidgetValue(tempComputer);
            tempText.setText(tempVal + "°C");
        }

        if (voltComputer != null && voltText != null) {
            String voltVal = plugin.getWidgetValue(voltComputer);
            voltText.setText(voltVal + " V");
        }
    }
    
    @Override
    public void update() {
        // Called by Launcher update cycle if needed, but we use internal timer
    }
}
