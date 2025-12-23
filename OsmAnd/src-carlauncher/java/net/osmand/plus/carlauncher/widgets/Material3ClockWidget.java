package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Material 3 Design Clock Widget.
 * Horizontal 4-Digit Layout with Custom Font.
 */
public class Material3ClockWidget extends BaseWidget {

    private TextView tvHourFirst, tvHourSecond;
    private TextView tvMinFirst, tvMinSecond;
    private android.graphics.Typeface clockTypeface;

    private final SimpleDateFormat hourFormat;
    private final SimpleDateFormat minuteFormat;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    public Material3ClockWidget(@NonNull Context context) {
        super(context, "clock_material3", "Saat (M3)");
        this.hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
        this.minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());
        this.order = 0; // Top position
    }

    @Override
    public View createView() {
        View view = LayoutInflater.from(context).inflate(net.osmand.plus.R.layout.widget_clock_material3, null);

        tvHourFirst = view.findViewById(net.osmand.plus.R.id.hour_digit_1);
        tvHourSecond = view.findViewById(net.osmand.plus.R.id.hour_digit_2);
        tvMinFirst = view.findViewById(net.osmand.plus.R.id.min_digit_1);
        tvMinSecond = view.findViewById(net.osmand.plus.R.id.min_digit_2);

        try {
            clockTypeface = android.graphics.Typeface.createFromAsset(context.getAssets(),
                    "fonts/Cross Boxed.ttf");
            if (tvHourFirst != null)
                tvHourFirst.setTypeface(clockTypeface);
            if (tvHourSecond != null)
                tvHourSecond.setTypeface(clockTypeface);
            if (tvMinFirst != null)
                tvMinFirst.setTypeface(clockTypeface);
            if (tvMinSecond != null)
                tvMinSecond.setTypeface(clockTypeface);
        } catch (Exception e) {
            // Fallback to default
        }

        rootView = view;
        updateUI();
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        startTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopTimer();
    }

    @Override
    public void update() {
        updateUI();
    }

    private void updateUI() {
        if (tvHourFirst == null || tvHourSecond == null || tvMinFirst == null || tvMinSecond == null)
            return;

        Date now = new Date();
        String hours = hourFormat.format(now);
        String minutes = minuteFormat.format(now);

        if (hours.length() >= 2) {
            tvHourFirst.setText(String.valueOf(hours.charAt(0)));
            tvHourSecond.setText(String.valueOf(hours.charAt(1)));
        }
        if (minutes.length() >= 2) {
            tvMinFirst.setText(String.valueOf(minutes.charAt(0)));
            tvMinSecond.setText(String.valueOf(minutes.charAt(1)));
        }
    }

    private void startTimer() {
        stopTimer();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
                long now = System.currentTimeMillis();
                long delay = 60000 - (now % 60000);
                handler.postDelayed(this, delay);
            }
        };
        handler.post(updateRunnable);
    }

    private void stopTimer() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }
}
