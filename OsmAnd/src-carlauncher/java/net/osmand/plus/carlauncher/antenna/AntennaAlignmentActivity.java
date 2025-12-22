package net.osmand.plus.carlauncher.antenna;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.GeomagneticField;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import net.osmand.plus.carlauncher.antenna.AntennaManager.AntennaPoint;

import java.util.Locale;

public class AntennaAlignmentActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;

    // Fallback if Rotation Vector not available
    private boolean hasRotationVector = false;

    private AlignmentView alignmentView;
    private TextView textTargetInfo;
    private TextView textCurrentAzimuth;
    private TextView textCurrentPitch;

    private float targetAzimuth = 0;
    private float targetPitch = 0;

    private float currentAzimuth = 0;
    private float currentPitch = 0;

    // Smoothing
    private static final float ALPHA = 0.1f; // Smoothing factor
    private float[] rMat = new float[9];
    private float[] orientation = new float[3];

    // For fallback
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;

    private AntennaManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(net.osmand.plus.R.layout.activity_antenna_alignment);

        manager = AntennaManager.getInstance(this);

        // Init Views
        alignmentView = findViewById(net.osmand.plus.R.id.alignment_view);
        textTargetInfo = findViewById(net.osmand.plus.R.id.text_target_info);
        textCurrentAzimuth = findViewById(net.osmand.plus.R.id.text_current_azimuth);
        textCurrentPitch = findViewById(net.osmand.plus.R.id.text_current_pitch);

        findViewById(net.osmand.plus.R.id.btn_close).setOnClickListener(v -> finish());

        // Init Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationVectorSensor != null) {
            hasRotationVector = true;
        } else {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        loadTargetData();
    }

    private void loadTargetData() {
        AntennaPoint pA = manager.getPointA();
        AntennaPoint pB = manager.getPointB();

        if (pA != null && pB != null) {
            targetAzimuth = (float) manager.getAzimuthAtoB();
            if (targetAzimuth < 0)
                targetAzimuth += 360;

            targetPitch = (float) manager.getElevationAtoB();

            textTargetInfo.setText(String.format(Locale.US, "HEDEF: Azimuth %.1f° | Pitch %.1f°\n%s -> %s",
                    targetAzimuth, targetPitch, pA.name, pB.name));

            alignmentView.setTarget(targetAzimuth, targetPitch);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasRotationVector) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            if (accelerometer != null)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null)
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    private void calculateOrientation() {
        // Remap for landscape/portrait if needed. Assuming standard hold for now
        // (Portrait or Landscape).
        // Let's assume standard phone hold: Screen facing user.

        SensorManager.getOrientation(rMat, orientation);

        // Azimuth: orientation[0] (-PI to PI)
        float az = (float) Math.toDegrees(orientation[0]);
        if (az < 0)
            az += 360;

        // Pitch: orientation[1] (-PI/2 to PI/2)
        float pitch = (float) Math.toDegrees(orientation[1]);

        // Geomagnetic Field Declination Compensation
        // TODO: Get actual location. For now, ignoring or using simple offset if
        // configured.
        // Assuming True North for simplification or use GPS if available.
        float declination = 0;
        az += declination;
        if (az >= 360)
            az -= 360;

        // Smooth
        currentAzimuth = lowPass(az, currentAzimuth);
        currentPitch = lowPass(pitch, currentPitch);

        updateUI();
    }

    private void updateUI() {
        textCurrentAzimuth.setText(String.format(Locale.US, "Yön: %.1f°", currentAzimuth));
        textCurrentPitch.setText(String.format(Locale.US, "Eğim: %.1f°", currentPitch));

        alignmentView.setCurrent(currentAzimuth, currentPitch);
    }

    private float lowPass(float input, float output) {
        // Handle 360 wrap for azimuth
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
