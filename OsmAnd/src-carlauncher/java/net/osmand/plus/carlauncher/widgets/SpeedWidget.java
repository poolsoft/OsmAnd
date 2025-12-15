package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter;
import android.graphics.Typeface;

/**
 * Hiz widget - GPS hizini gosterir.
 */
public class SpeedWidget extends BaseWidget implements OsmAndLocationProvider.OsmAndLocationListener {

    private TextView labelText;
    private TextView speedText;
    private final OsmandApplication app;

    public SpeedWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "speed", "Hiz");
        this.app = app;
        this.order = 1;
    }

    @NonNull
    @Override
    public View createView() {
        // Modern Kart Yapisi (FrameLayout)
        android.widget.FrameLayout rootFrame = new android.widget.FrameLayout(context);
        rootFrame.setPadding(16, 16, 16, 16);

        // Arka Plan (XML Kaynagi)
        rootFrame.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card);

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(16, 16, 16, 16);
        contentLayout.setLayoutParams(params);

        // --- IKON (Hiz Gostergesi) ---
        android.widget.ImageView iconView = new android.widget.ImageView(context);
        // Sistem ikonu olarak pusula veya benzeri bir sey bulalim, yoksa
        // 'ic_menu_mylocation'
        iconView.setImageResource(android.R.drawable.ic_menu_compass);
        iconView.setColorFilter(android.graphics.Color.parseColor("#00FF00")); // Neon Yesil Ikon
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(56, 56);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconParams.bottomMargin = 0;
        contentLayout.addView(iconView, iconParams);

        // --- BASLIK (HIZ) ---
        labelText = new TextView(context);
        labelText.setText("KM/H"); // Birim olarak degistirdik
        labelText.setTextColor(android.graphics.Color.LTGRAY);
        labelText.setTextSize(14);
        labelText.setTypeface(Typeface.DEFAULT_BOLD);
        labelText.setGravity(Gravity.CENTER);

        // --- HIZ DEGERI (7-Segment Stili) ---
        speedText = new TextView(context);
        // Neon Yesil / Cyan rengi
        speedText.setTextColor(android.graphics.Color.parseColor("#00FFFF")); // Cyan
        speedText.setTextSize(64); // Dev boyut
        speedText.setGravity(Gravity.CENTER);
        speedText.setText("--");
        // Monospace, dijital saat gorunumu verir
        speedText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        // Golge efekti (Glow/Neon etkisi)
        speedText.setShadowLayer(10, 0, 0, android.graphics.Color.parseColor("#008888"));

        contentLayout.addView(speedText);
        contentLayout.addView(labelText); // Birimi alta aldik

        rootFrame.addView(contentLayout);
        rootView = rootFrame;
        return rootView;
    }

    @Override
    public void update() {
        // Konum guncellemesi ile otomatik cagrilir
    }

    @Override
    public void updateLocation(Location location) {
        if (speedText != null && location != null) {
            if (location.hasSpeed()) {
                String speed = OsmAndFormatter.getFormattedSpeed(location.getSpeed(), app);
                speedText.post(() -> speedText.setText(speed));
            } else {
                speedText.post(() -> speedText.setText("--"));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (app != null) {
            app.getLocationProvider().addLocationListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (app != null) {
            app.getLocationProvider().removeLocationListener(this);
        }
    }
}
