package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockWidget extends BaseWidget {

    private TextView clockText;
    private TextView dateText;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private Runnable updateRunnable;

    public ClockWidget(@NonNull Context context) {
        super(context, "clock", "Saat");
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        this.order = 0;
    }

    @NonNull
    @Override
    public View createView() {

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(24, 48, 24, 48);

        // Arka plan seffaf (panel background kullan)
        container.setBackgroundColor(Color.TRANSPARENT);
        // container.setElevation(8f); // Golgeyi kaldir (flat design)

        // Saat
        clockText = new TextView(context);
        clockText.setTextColor(context.getResources().getColor(net.osmand.plus.R.color.cyber_text_primary));
        clockText.setTextSize(64); // Buyuk modern font
        clockText.setGravity(Gravity.CENTER);
        clockText.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL)); // Thin font
        // clockText.setShadowLayer(6, 0, 0, Color.BLACK); // Golge kaldir

        // Tarih
        dateText = new TextView(context);
        dateText.setTextColor(context.getResources().getColor(net.osmand.plus.R.color.cyber_text_secondary));
        dateText.setTextSize(18);
        dateText.setGravity(Gravity.CENTER);
        dateText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        container.addView(clockText);
        container.addView(dateText);

        rootView = container;
        update();

        return rootView;
    }

    @Override
    public void update() {
        if (clockText != null) {

            String currentTime = timeFormat.format(new Date());
            clockText.setText(currentTime);

            String currentDate = dateFormat.format(new Date());
            dateText.setText(currentDate);

            // Fade animasyonu
            AlphaAnimation anim = new AlphaAnimation(0.7f, 1.0f);
            anim.setDuration(300);
            clockText.startAnimation(anim);
            dateText.startAnimation(anim);
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
                        rootView.postDelayed(this, 1000);
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
