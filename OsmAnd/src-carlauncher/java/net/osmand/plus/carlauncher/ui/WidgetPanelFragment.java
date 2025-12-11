package net.osmand.plus.carlauncher.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.ClockWidget;
import net.osmand.plus.carlauncher.widgets.DirectionWidget;
import net.osmand.plus.carlauncher.widgets.SpeedWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;

/**
 * Widget paneli fragment.
 * Widget'lari gosterir ve yonetir.
 */
public class WidgetPanelFragment extends Fragment {

    public static final String TAG = "WidgetPanelFragment";

    private LinearLayout widgetContainer;
    private WidgetManager widgetManager;
    private OsmandApplication app;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            app = (OsmandApplication) getContext().getApplicationContext();
            widgetManager = new WidgetManager(getContext(), app);
            initializeWidgets();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        widgetContainer = new LinearLayout(getContext());
        widgetContainer.setOrientation(LinearLayout.VERTICAL);
        widgetContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        widgetContainer.setPadding(16, 16, 16, 16);
        widgetContainer.setBackgroundColor(0xCC1A1A1A);

        return widgetContainer;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Widget config yukle
        if (widgetManager != null) {
            widgetManager.loadWidgetConfig();
            widgetManager.attachWidgetsToContainer(widgetContainer);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (widgetManager != null) {
            widgetManager.startAllWidgets();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (widgetManager != null) {
            widgetManager.stopAllWidgets();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (widgetManager != null) {
            widgetManager.saveWidgetConfig();
        }
    }

    /**
     * Widget'lari initialize et.
     */
    private void initializeWidgets() {
        if (widgetManager == null || app == null)
            return;

        // Saat widget (her zaman en ustte)
        widgetManager.addWidget(new ClockWidget(getContext()));

        // Hiz widget
        widgetManager.addWidget(new SpeedWidget(getContext(), app));

        // Yon widget
        widgetManager.addWidget(new DirectionWidget(getContext(), app));

        // Muzik widget
        widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.MusicWidget(getContext(), app));

        // Gelecekte: Daha fazla widget eklenecek
        // widgetManager.addWidget(new AltitudeWidget(getContext(), app));
        // widgetManager.addWidget(new NavigationWidget(getContext(), app));
    }

    /**
     * Widget manager'i al (ayarlar icin).
     */
    public WidgetManager getWidgetManager() {
        return widgetManager;
    }
}
