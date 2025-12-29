package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
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
        super(context, "classic_clock", "Klasik Saat");
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        this.order = 0;
    }

    public ClockWidget(@NonNull Context context, net.osmand.plus.OsmandApplication app) {
        this(context);
    }

    @NonNull
    @Override
    public View createView() {
        // Modern Kart Yapisi (FrameLayout)
        FrameLayout rootFrame = new FrameLayout(context);

        // Kart Boyutlandirmasi (Android Auto benzeri)
        // Genislik ve yukseklik parent tarafindan belirlenecek ama padding verelim
        rootFrame.setPadding(0, 0, 0, 0);

        // Arka Plan (XML Kaynagi)
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_modern);

        // Icerik Konteyneri (Dikey)
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(4, 4, 4, 4); // Icerik kenar boslugu
        contentLayout.setLayoutParams(params);

        // --- IKON ---
        // Kaldirildi

        // --- SAAT ---
        clockText = new TextView(context);
        clockText.setTextColor(Color.WHITE);
        clockText.setTextSize(60); // Increase size for 7-segment readability
        clockText.setGravity(Gravity.CENTER);
        clockText.setIncludeFontPadding(false);

        try {
            Typeface digitalFont = Typeface.createFromAsset(context.getAssets(), "fonts/curved-seven-segment.ttf");
            clockText.setTypeface(digitalFont);
        } catch (Exception e) {
            // Fallback
            clockText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        }

        contentLayout.addView(clockText);

        // --- TARIH ---
        dateText = new TextView(context);
        dateText.setTextColor(Color.parseColor("#AAAAAA")); // Gri
        dateText.setTextSize(16);
        dateText.setGravity(Gravity.CENTER);
        dateText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        // textCaps = true yapilabilir ama Java'da gerek yok, formatter halleder.
        contentLayout.addView(dateText);

        rootFrame.addView(contentLayout);

        rootView = rootFrame;
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
