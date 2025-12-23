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
 * Stacked Hours and Minutes with bold font.
 */
public class Material3ClockWidget extends BaseWidget {

    private TextView tvHours;
    private TextView tvMinutes;
    private TextView tvDate;
    private android.graphics.Typeface clockTypeface;

    private final SimpleDateFormat hourFormat;
    private final SimpleDateFormat minuteFormat;
    private final SimpleDateFormat dateFormat;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    public Material3ClockWidget(@NonNull Context context) {
        super(context, "clock_material3", "Saat (M3)");
        this.hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
        this.minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
        this.order = 0; // Top position
    }

    @Override
    public View createView() {
        View view = LayoutInflater.from(context).inflate(net.osmand.plus.R.layout.widget_clock_material3, null);

        tvHours = view.findViewById(net.osmand.plus.R.id.clock_hours);
        tvMinutes = view.findViewById(net.osmand.plus.R.id.clock_minutes);
        tvDate = view.findViewById(net.osmand.plus.R.id.clock_date);

        try {
            clockTypeface = android.graphics.Typeface.createFromAsset(context.getAssets(),
                    "fonts/Cross Boxed.ttf");
            if (tvHours != null)
                tvHours.setTypeface(clockTypeface);
            if (tvMinutes != null)
                tvMinutes.setTypeface(clockTypeface);
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
        if (tvHours == null || tvMinutes == null)
            return;

        Date now = new Date();
        tvHours.setText(hourFormat.format(now));
        tvMinutes.setText(minuteFormat.format(now));

        if (tvDate != null) {
            tvDate.setText(dateFormat.format(now));
        }
    }

    private void startTimer() {
        stopTimer();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
                // Schedule next update at the start of the next minute
                long now = System.currentTimeMillis();
                long delay = 60000 - (now % 60000);
                handler.postDelayed(this, delay);
            }
        };
        // Initial run
        handler.post(updateRunnable);
    }

    private void stopTimer() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }
}
