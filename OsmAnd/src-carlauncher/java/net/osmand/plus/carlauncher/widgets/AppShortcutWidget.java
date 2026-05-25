package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * Workspace grid'inde uygulama kisayollarini (Spotify, Haritalar vb.) 
 * 1x1 boyutunda gostermek ve baslatmak icin ozel widget sinifi.
 *
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class AppShortcutWidget extends BaseWidget {

    private final String packageName;

    public AppShortcutWidget(@NonNull Context context, @NonNull String packageName, @NonNull String label) {
        super(context, "shortcut_" + packageName, label);
        this.packageName = packageName;
        this.spanX = 1;
        this.spanY = 1;
        this.size = WidgetSize.SMALL;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public void setSize(WidgetSize size) {
        // Kisayollar her zaman 1x1 kalir, boyut degisikligini yoksayiyoruz.
        this.size = WidgetSize.SMALL;
        this.spanX = 1;
        this.spanY = 1;
    }

    @NonNull
    @Override
    public View createView() {
        Context ctx = getContext();
        if (ctx == null) {
            ctx = context;
        }

        // Ana Yerlesim (Ortalanmis Dikey Kutu)
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        
        int padding = dpToPx(8);
        layout.setPadding(padding, padding, padding, padding);

        // Uygulama Ikonu
        ImageView iconView = new ImageView(ctx);
        int iconSize = dpToPx(48); // Surus esnasinda gorunur ve rahat tiklanir 48dp boyut
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.bottomMargin = dpToPx(6);
        iconView.setLayoutParams(iconLp);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        try {
            Drawable icon = ctx.getPackageManager().getApplicationIcon(packageName);
            iconView.setImageDrawable(icon);
        } catch (Exception e) {
            iconView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        layout.addView(iconView);

        // Uygulama Adi (Kucuk ve Net)
        TextView labelView = new TextView(ctx);
        labelView.setText(title);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(10); // Okunabilir kucuk punto
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        labelView.setLayoutParams(labelLp);
        layout.addView(labelView);

        // Tiklama Dinleyicisi (Uygulamayi baslatir)
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Eger edit modundaysak uygulamayi baslatma (tasimaya engel olmamak icin)
                if (net.osmand.plus.carlauncher.widgets.WorkspacePageAdapter.isEditMode) {
                    return;
                }
                try {
                    Intent launchIntent = v.getContext().getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        v.getContext().startActivity(launchIntent);
                    } else {
                        Toast.makeText(v.getContext(), 
                                title + " baslatilamadi.", 
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), 
                            "Hata: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        rootView = layout;
        return layout;
    }

    @Override
    public void update() {
        // Kisayollarin dinamik veri guncelleme ihtiyaci yoktur.
    }

    private int dpToPx(int dp) {
        Context ctx = getContext();
        if (ctx == null) {
            ctx = context;
        }
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
