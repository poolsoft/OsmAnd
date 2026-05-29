package net.osmand.plus.carlauncher.antenna;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.R;

import java.util.Locale;

public class AntennaAlignmentFragment extends Fragment implements SensorEventListener, AntennaManager.AntennaListener {

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private boolean hasRotationVector = false;

    private AlignmentView alignmentView;
    private TextView textTargetInfo;
    private TextView textCurrentAzimuth;
    private TextView textCurrentPitch;
    private TextView textSource;
    private TextView textTarget;
    private View btnSwap;

    private float targetAzimuth = 0;
    private float targetPitch = 0;

    private float currentAzimuth = 0;
    private float currentPitch = 0;

    private static final float ALPHA = 0.1f; // Smoothing factor
    private float[] rMat = new float[9];
    private float[] orientation = new float[3];

    // Fallback for sensors
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;

    private AntennaManager manager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_antenna_alignment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        manager = AntennaManager.getInstance(requireContext());

        // Bind views
        alignmentView = view.findViewById(R.id.alignment_view);
        textTargetInfo = view.findViewById(R.id.text_target_info);
        textCurrentAzimuth = view.findViewById(R.id.text_current_azimuth);
        textCurrentPitch = view.findViewById(R.id.text_current_pitch);
        textSource = view.findViewById(R.id.text_source);
        textTarget = view.findViewById(R.id.text_target);
        btnSwap = view.findViewById(R.id.btn_swap);

        // Kapatma butonu aksiyonu (Turkce karakter yok)
        View btnClose = view.findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
                    // Sag panelde aciksa varsayilan widget listesine geri don
                    ((net.osmand.plus.activities.MapActivity) getActivity()).setPanelContent(
                            net.osmand.plus.carlauncher.ui.PanelContentManager.PanelContent.WIDGETS
                    );
                } else if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }

        // Kaynak nokta secimi (Turkce karakter yok)
        View btnSetSource = view.findViewById(R.id.btn_set_source);
        if (btnSetSource != null) {
            btnSetSource.setOnClickListener(v -> AntennaPointPickerDialog.show(
                    requireContext(), manager, true, null));
        }

        // Hedef nokta secimi (Turkce karakter yok)
        View btnSetTarget = view.findViewById(R.id.btn_set_target);
        if (btnSetTarget != null) {
            btnSetTarget.setOnClickListener(v -> AntennaPointPickerDialog.show(
                    requireContext(), manager, false, null));
        }

        // Swap (Turkce karakter yok)
        if (btnSwap != null) {
            btnSwap.setOnClickListener(v -> {
                if (manager.getSource() != null && manager.getTarget() != null) {
                    manager.swapPoints();
                } else {
                    Toast.makeText(requireContext(), "Once kaynak ve hedef nokta secin.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Sensorleri hazirla (Turkce karakter yok)
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationVectorSensor != null) {
            hasRotationVector = true;
        } else {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        manager.setListener(this);
        if (hasRotationVector) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            if (accelerometer != null)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null)
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        manager.setListener(null);
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            calculateOrientation();
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
            if (lastMagnetometerSet) {
                SensorManager.getRotationMatrix(rMat, null, lastAccelerometer, lastMagnetometer);
                calculateOrientation();
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onAntennaPointsChanged() {
        if (getView() != null) {
            getView().post(this::updateUI);
        }
    }

    private void calculateOrientation() {
        SensorManager.getOrientation(rMat, orientation);

        // Azimuth: orientation[0] (-PI to PI)
        float az = (float) Math.toDegrees(orientation[0]);
        if (az < 0)
            az += 360;

        // Pitch: orientation[1] (-PI/2 to PI/2)
        float pitch = (float) Math.toDegrees(orientation[1]);

        float declination = 0;
        az += declination;
        if (az >= 360)
            az -= 360;

        // Smooth
        currentAzimuth = lowPass(az, currentAzimuth);
        currentPitch = lowPass(pitch, currentPitch);

        updateCompassUI();
    }

    private void updateCompassUI() {
        if (textCurrentAzimuth != null) {
            textCurrentAzimuth.setText(String.format(Locale.US, "Yon: %.1f°", currentAzimuth));
        }
        if (textCurrentPitch != null) {
            textCurrentPitch.setText(String.format(Locale.US, "Egim: %.1f°", currentPitch));
        }
        if (alignmentView != null) {
            alignmentView.setCurrent(currentAzimuth, currentPitch);
        }
    }

    private void updateUI() {
        AntennaManager.AntennaPoint source = manager.getSource();
        AntennaManager.AntennaPoint target = manager.getTarget();

        // Swap butonu durumu (Turkce karakter yok)
        if (btnSwap != null) {
            boolean both = source != null && target != null;
            btnSwap.setEnabled(both);
            btnSwap.setAlpha(both ? 1.0f : 0.3f);
        }

        // Kaynak etiketini guncelle (Turkce karakter yok)
        if (source != null) {
            String name = (source.name != null && !source.name.isEmpty())
                    ? source.name
                    : String.format(Locale.US, "%.4f, %.4f", source.lat, source.lon);
            if (textSource != null) {
                textSource.setText("Kaynak: " + name);
                textSource.setTextColor(0xFF4FC3F7);
            }
        } else {
            if (textSource != null) {
                textSource.setText("Kaynak Sec...");
                textSource.setTextColor(0xFF888888);
            }
        }

        // Hedef etiketini guncelle (Turkce karakter yok)
        if (target != null) {
            String name = (target.name != null && !target.name.isEmpty())
                    ? target.name
                    : String.format(Locale.US, "%.4f, %.4f", target.lat, target.lon);
            if (textTarget != null) {
                textTarget.setText("Hedef: " + name);
                textTarget.setTextColor(0xFFFFD700);
            }
        } else {
            if (textTarget != null) {
                textTarget.setText("Hedef Sec...");
                textTarget.setTextColor(0xFF888888);
            }
        }

        // Hedef verilerini hesapla ve guncelle (Turkce karakter yok)
        if (source != null && target != null) {
            targetAzimuth = (float) manager.getAzimuthSourceToTarget();
            if (targetAzimuth < 0)
                targetAzimuth += 360;

            targetPitch = (float) manager.getElevationSourceToTarget();

            if (textTargetInfo != null) {
                double distMeters = manager.getDistanceMeters();
                String distStr = distMeters >= 1000
                        ? String.format(Locale.US, "%.2f km", distMeters / 1000)
                        : String.format(Locale.US, "%.0f m", distMeters);

                textTargetInfo.setText(String.format(Locale.US,
                        "KAYNAK: %s -> HEDEF: %s\nMesafe: %s | Azimut %.1f° | Egim %.1f°",
                        source.name, target.name, distStr, targetAzimuth, targetPitch));
            }

            if (alignmentView != null) {
                alignmentView.setTarget(targetAzimuth, targetPitch);
            }
        } else {
            if (textTargetInfo != null) {
                textTargetInfo.setText("Kaynak ve hedef nokta secilmemis.");
            }
        }
    }

    private float lowPass(float input, float output) {
        if (Math.abs(input - output) > 180) {
            if (input > output)
                output += 360;
            else
                input += 360;
        }
        float res = output + ALPHA * (input - output);
        if (res >= 360)
            res -= 360;
        return res;
    }
}
