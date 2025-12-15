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
        // Modern Kart Yapisi (FrameLayout)
        FrameLayout rootFrame = new FrameLayout(context);

        // Kart Boyutlandirmasi (Android Auto benzeri)
        // Genislik ve yukseklik parent tarafindan belirlenecek ama padding verelim
        rootFrame.setPadding(16, 16, 16, 16);

        // Arka Plan (Yari seffaf siyah kart)
        // Shape Drawable'i kodla olusturabiliriz veya xml kullanabiliriz.
        // Basitlik adina kodla yapalim.
        android.graphics.drawable.GradientDrawable activeBg = new android.graphics.drawable.GradientDrawable();
        activeBg.setColor(Color.parseColor("#CC111111")); // %80 Siyah
        activeBg.setCornerRadius(24f); // Yuvarlak koseler
        activeBg.setStroke(2, Color.parseColor("#33FFFFFF")); // Ince beyaz cerceve
        rootFrame.setBackground(activeBg);

        // Icerik Konteyneri (Dikey)
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(16, 16, 16, 16); // Icerik kenar boslugu
        contentLayout.setLayoutParams(params);

        // --- IKON ---
        // Android sistem ikonlarindan bir saat ikonu veya benzeri bulalim.
        // Eger yoksa metin tabanli ikon yapabiliriz.
        android.widget.ImageView iconView = new android.widget.ImageView(context);
        iconView.setImageResource(android.R.drawable.ic_menu_recent_history); // Saat ikonu alternatifi
        iconView.setColorFilter(Color.LTGRAY);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(64, 64);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconParams.bottomMargin = 8;
        contentLayout.addView(iconView, iconParams);

        // --- SAAT ---
        clockText = new TextView(context);
        clockText.setTextColor(Color.WHITE);
        clockText.setTextSize(48); // Buyuk, okunakli
        clockText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); // Modern font
        clockText.setGravity(Gravity.CENTER);
        clockText.setIncludeFontPadding(false);
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
