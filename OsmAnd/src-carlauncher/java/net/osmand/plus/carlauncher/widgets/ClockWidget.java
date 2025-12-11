package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Saat widget - Dijital saat gosterir.
 */
public class ClockWidget extends BaseWidget {

    private TextView clockText;
    private SimpleDateFormat timeFormat;
    private Runnable updateRunnable;

    public ClockWidget(@NonNull Context context) {
        super(context, "clock", "Saat");
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.order = 0; // En ustte
    }

    @NonNull
    @Override
    public View createView() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(16, 24, 16, 16);

        clockText = new TextView(context);
        clockText.setTextColor(0xFFFFFFFF);
        clockText.setTextSize(48);
        clockText.setGravity(Gravity.CENTER);

        container.addView(clockText);

        rootView = container;
        update();

        return rootView;
    }

    @Override
    public void update() {
        if (clockText != null) {
            String currentTime = timeFormat.format(new Date());
            clockText.setText(currentTime);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startUpdating();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdating();
    }

    private void startUpdating() {
        if (updateRunnable == null && rootView != null) {
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    update();
                    if (rootView != null && isStarted()) {
                        rootView.postDelayed(this, 1000); // Her saniye
                    }
                }
            };
            rootView.post(updateRunnable);
        }
    }

    private void stopUpdating() {
        if (updateRunnable != null && rootView != null) {
            rootView.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }
}
