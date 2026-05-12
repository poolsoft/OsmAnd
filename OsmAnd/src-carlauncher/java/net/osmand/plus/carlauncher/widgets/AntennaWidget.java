package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.antenna.AntennaManager;
import net.osmand.plus.carlauncher.antenna.AntennaPointPickerDialog;

import java.util.Locale;

/**
 * Anten Hizalama Widget'i.
 * Kaynak ve hedef noktalari gosterir, secim dialog'unu acar.
 * Hesaplanan azimut, mesafe ve egim bilgilerini kompakt olarak gosterir.
 */
public class AntennaWidget extends BaseWidget implements AntennaManager.AntennaListener {

    private TextView textSource, textTarget;
    private TextView valDistance, valAzimuth, valElevation;
    private View statsContainer;

    private final AntennaManager manager;

    public AntennaWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "antenna", "Anten");
        this.manager = AntennaManager.getInstance(context);
        this.order = 4;
    }

    @NonNull
    @Override
    public View createView() {
        View view = LayoutInflater.from(context)
                .inflate(net.osmand.plus.R.layout.widget_antenna_modern, null);

        // View baglama
        textSource = view.findViewById(net.osmand.plus.R.id.text_source);
        textTarget = view.findViewById(net.osmand.plus.R.id.text_target);
        valDistance = view.findViewById(net.osmand.plus.R.id.val_distance);
        valAzimuth = view.findViewById(net.osmand.plus.R.id.val_azimuth);
        valElevation = view.findViewById(net.osmand.plus.R.id.val_elevation);
        statsContainer = view.findViewById(net.osmand.plus.R.id.stats_container);
        android.widget.ImageButton btnSwap = view.findViewById(net.osmand.plus.R.id.btn_swap);

        // Swap butonu — sadece iki nokta da seciliyse aktif
        btnSwap.setOnClickListener(v -> {
            if (manager.getSource() != null && manager.getTarget() != null) {
                manager.swapPoints();
            } else {
                android.widget.Toast.makeText(context,
                        "Kaynak ve hedef nokta secilmeden swap yapilamaz.",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Kaynak butonu
        view.findViewById(net.osmand.plus.R.id.btn_set_source)
                .setOnClickListener(v -> openPickerDialog(true));

        // Hedef butonu
        view.findViewById(net.osmand.plus.R.id.btn_set_target)
                .setOnClickListener(v -> openPickerDialog(false));

        // Hizalama penceresi
        view.findViewById(net.osmand.plus.R.id.btn_align).setOnClickListener(v -> {
            if (manager.getSource() != null && manager.getTarget() != null) {
                try {
                    Intent intent = new Intent(context,
                            net.osmand.plus.carlauncher.antenna.AntennaAlignmentActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Hizalama ekrani baslatilirken hata.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(context, "Lutfen once kaynak ve hedef noktayi secin.", Toast.LENGTH_SHORT).show();
            }
        });

        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        rootView = view;
        updateUI();
        return rootView;
    }

    /**
     * Picker dialog'u acar.
     * @param isSource true=Kaynak secimi, false=Hedef secimi
     */
    private void openPickerDialog(boolean isSource) {
        if (context instanceof FragmentActivity) {
            FragmentManager fm = ((FragmentActivity) context).getSupportFragmentManager();
            AntennaPointPickerDialog dialog = isSource
                    ? AntennaPointPickerDialog.forSource()
                    : AntennaPointPickerDialog.forTarget();
            dialog.show(fm, AntennaPointPickerDialog.TAG);
        } else {
            // FragmentActivity yoksa eski harita secimi yontemine fallback
            manager.setPickingMode(isSource ? AntennaManager.PICK_SOURCE : AntennaManager.PICK_TARGET);
            manager.setLayerVisible(true);
            Toast.makeText(context,
                    "Haritaya tikladiginizdaki konumu " + (isSource ? "kaynak" : "hedef") + " olarak ayarlar.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        manager.setListener(this);
        manager.setLayerVisible(true);
        updateUI();
    }

    @Override
    public void onStop() {
        super.onStop();
        manager.setListener(null);
        manager.setLayerVisible(false);
    }

    @Override
    public void update() {
        updateUI();
    }

    @Override
    public void onAntennaPointsChanged() {
        if (rootView != null) {
            rootView.post(this::updateUI);
        }
    }

    private void updateUI() {
        if (rootView == null) return;

        AntennaManager.AntennaPoint source = manager.getSource();
        AntennaManager.AntennaPoint target = manager.getTarget();

        // Swap butonu durumu: iki nokta seciliyse aktif, degil ise gri
        android.widget.ImageButton swapBtn = rootView.findViewById(net.osmand.plus.R.id.btn_swap);
        if (swapBtn != null) {
            boolean bothSelected = source != null && target != null;
            swapBtn.setEnabled(bothSelected);
            swapBtn.setAlpha(bothSelected ? 1.0f : 0.3f);
            // Aktifken tint rengi degistir
            int tintColor = bothSelected ? 0xFF4CAF50 : 0xFF555555;
            swapBtn.setColorFilter(tintColor);
        }

        // Kaynak etiketi
        if (source != null) {
            String name = (source.name != null && !source.name.isEmpty())
                    ? source.name
                    : String.format(Locale.US, "%.4f, %.4f", source.lat, source.lon);
            textSource.setText(name);
            textSource.setTextColor(0xFF4FC3F7); // Mavi — ayarlanmis
        } else {
            textSource.setText("Sec...");
            textSource.setTextColor(0xFF888888);
        }

        // Hedef etiketi
        if (target != null) {
            String name = (target.name != null && !target.name.isEmpty())
                    ? target.name
                    : String.format(Locale.US, "%.4f, %.4f", target.lat, target.lon);
            textTarget.setText(name);
            textTarget.setTextColor(0xFFFFD700); // Altin — ayarlanmis
        } else {
            textTarget.setText("Sec...");
            textTarget.setTextColor(0xFF888888);
        }

        // Hesaplama degerleri
        if (source != null && target != null) {
            statsContainer.setVisibility(View.VISIBLE);

            double distMeters = manager.getDistanceMeters();
            valDistance.setText(distMeters >= 1000
                    ? String.format(Locale.US, "%.2f km", distMeters / 1000)
                    : String.format(Locale.US, "%.0f m", distMeters));

            double az = manager.getAzimuthSourceToTarget();
            if (az < 0) az += 360;
            valAzimuth.setText(String.format(Locale.US, "%.1f deg", az));

            double elev = manager.getElevationSourceToTarget();
            valElevation.setText(String.format(Locale.US, "%.1f deg", elev));
        } else {
            statsContainer.setVisibility(View.GONE);
        }
    }
}
