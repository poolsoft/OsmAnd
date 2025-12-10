package net.osmand.plus.carlauncher;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.carlauncher.widgets.WidgetPanelFragment;

/**
 * Ana Car Launcher Activity.
 * %70 harita gorunumu + %30 widget paneli icerir.
 * MapActivity'den extend ederek tam harita islevselligini devralir.
 */
public class CarLauncherActivity extends MapActivity {

    public static final String TAG = "CarLauncherActivity";

    private FrameLayout widgetContainer;
    private WidgetPanelFragment widgetPanelFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Tam ekran modu - status bar ve navigation bar gizle
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Daha da immersive mod icin
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        super.onCreate(savedInstanceState);

        // Widget panelini ekle
        setupWidgetPanel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Immersive modu her resume'da yenile
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Widget panelini harita uzerine overlay olarak ekle.
     * Mevcut MapActivity layout'una eklenir.
     */
    private void setupWidgetPanel() {
        // Widget container'i bul veya olustur
        widgetContainer = findViewById(R.id.car_launcher_widget_container);

        if (widgetContainer == null) {
            // Container yoksa dinamik olarak ekle
            FrameLayout rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                widgetContainer = new FrameLayout(this);
                widgetContainer.setId(R.id.car_launcher_widget_container);

                // %30 genislik - sag tarafta
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        0, // Weight kullanilacak
                        FrameLayout.LayoutParams.MATCH_PARENT);

                // Ekran genisliginin %30'u
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                params.width = (int) (screenWidth * 0.30);
                params.gravity = android.view.Gravity.END;

                widgetContainer.setLayoutParams(params);
                widgetContainer.setBackgroundColor(0xCC000000); // Yari saydam siyah

                rootView.addView(widgetContainer);
            }
        }

        // Widget fragment'i ekle
        if (widgetContainer != null) {
            widgetPanelFragment = (WidgetPanelFragment) getSupportFragmentManager()
                    .findFragmentByTag(WidgetPanelFragment.TAG);

            if (widgetPanelFragment == null) {
                widgetPanelFragment = new WidgetPanelFragment();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(widgetContainer.getId(), widgetPanelFragment, WidgetPanelFragment.TAG);
                ft.commitAllowingStateLoss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Geri tusuna basinca launcher olarak kalsin, uygulamayi kapatmasin.
     */
    @Override
    public void onBackPressed() {
        // Launcher modunda geri tusu home'a donmeli
        // Bos birak - hicbir sey yapma
    }

    /**
     * Widget panelini goster/gizle toggle.
     */
    public void toggleWidgetPanel() {
        if (widgetContainer != null) {
            if (widgetContainer.getVisibility() == View.VISIBLE) {
                widgetContainer.setVisibility(View.GONE);
            } else {
                widgetContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Widget panelinin gorunurluk durumu.
     */
    public boolean isWidgetPanelVisible() {
        return widgetContainer != null && widgetContainer.getVisibility() == View.VISIBLE;
    }
}
