package net.osmand.plus.carlauncher.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import net.osmand.plus.carlauncher.CarLauncherSettings;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.ClockWidget;
import net.osmand.plus.carlauncher.widgets.DirectionWidget;
import net.osmand.plus.carlauncher.widgets.SpeedWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;
import net.osmand.plus.carlauncher.widgets.NavigationWidget;
import net.osmand.plus.carlauncher.widgets.MusicWidget;

/**
 * Widget paneli fragment.
 * Widget'lari gosterir ve yonetir.
 */
public class WidgetPanelFragment extends Fragment {

    public static final String TAG = "WidgetPanelFragment";

    private LinearLayout widgetContainer;
    private WidgetManager widgetManager;
    private OsmandApplication app;
    private net.osmand.plus.carlauncher.antenna.AntennaMapLayer antennaMapLayer;
    private android.content.BroadcastReceiver antennaReceiver;

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

        boolean isPortrait = getResources()
                .getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;

        ViewGroup rootScroll;
        ViewGroup.LayoutParams scrollParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        if (isPortrait) {
            android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(getContext());
            hScroll.setFillViewport(true);
            rootScroll = hScroll;
        } else {
            ScrollView vScroll = new ScrollView(getContext());
            vScroll.setFillViewport(true);
            rootScroll = vScroll;
        }

        rootScroll.setLayoutParams(scrollParams);
        rootScroll.setBackgroundResource(net.osmand.plus.R.drawable.bg_panel_modern);

        widgetContainer = new LinearLayout(getContext());
        widgetContainer.setOrientation(isPortrait ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        // In portrait, use WRAP_CONTENT for horizontal scroll to work with fixed-width
        // widgets
        widgetContainer.setLayoutParams(new ViewGroup.LayoutParams(
                isPortrait ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT,
                isPortrait ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT));
        widgetContainer.setPadding(2, 0, 2, 16); // Reduced padding, no top padding

        // Long click to manage widgets
        widgetContainer.setOnLongClickListener(v -> {
            showWidgetManagementDialog();
            return true;
        });
        // Also allow clicking on empty space
        rootScroll.setOnLongClickListener(v -> {
            showWidgetManagementDialog();
            return true;
        });

        rootScroll.addView(widgetContainer);
        return rootScroll;
    }

    private void showWidgetManagementDialog() {
        if (getContext() == null || widgetManager == null)
            return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Widget Yonetimi");

        final LinearLayout listLayout = new LinearLayout(getContext());
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(16, 16, 16, 16);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(listLayout);

        builder.setView(scrollView);
        builder.setPositiveButton("Kapat", (dialog, which) -> {
            widgetManager.attachWidgetsToContainer(widgetContainer);
            widgetManager.startAllWidgets();
        });

        final android.app.AlertDialog dialog = builder.create();

        updateDialogList(listLayout, dialog);

        dialog.show();
    }

    private void updateDialogList(final LinearLayout listLayout, final android.app.AlertDialog dialog) {
        listLayout.removeAllViews();
        java.util.List<net.osmand.plus.carlauncher.widgets.BaseWidget> widgets = widgetManager.getAllWidgets();

        for (int i = 0; i < widgets.size(); i++) {
            final net.osmand.plus.carlauncher.widgets.BaseWidget w = widgets.get(i);
            final int index = i;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);
            row.setBackgroundResource(android.R.drawable.list_selector_background);

            // Visibility Checkbox
            android.widget.CheckBox cb = new android.widget.CheckBox(getContext());
            cb.setChecked(w.isVisible());
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                widgetManager.setWidgetVisible(w.getId(), isChecked);
                widgetManager.saveWidgetConfig();
            });
            row.addView(cb);

            // Name
            android.widget.TextView nameView = new android.widget.TextView(getContext());
            nameView.setText(w.getTitle());
            nameView.setTextSize(16);
            nameView.setTextColor(0xFFFFFFFF);
            nameView.setPadding(16, 0, 16, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            row.addView(nameView, params);

            // Up Button
            if (index > 0) {
                android.widget.ImageButton upBtn = new android.widget.ImageButton(getContext());
                upBtn.setImageResource(android.R.drawable.arrow_up_float);
                upBtn.setBackgroundColor(0x00000000);
                upBtn.setPadding(24, 24, 24, 24); // Increase touch area
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(96, 96); // Bigger button
                btnParams.setMargins(8, 0, 8, 0);
                upBtn.setLayoutParams(btnParams);

                upBtn.setOnClickListener(v -> {
                    widgetManager.moveWidget(index, index - 1);
                    updateDialogList(listLayout, dialog);
                });
                row.addView(upBtn);
            } else {
                // Empty placeholder to align
                android.view.View spacer = new android.view.View(getContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(96, 96));
                row.addView(spacer);
            }

            // Down Button
            if (index < widgets.size() - 1) {
                android.widget.ImageButton downBtn = new android.widget.ImageButton(getContext());
                downBtn.setImageResource(android.R.drawable.arrow_down_float);
                downBtn.setBackgroundColor(0x00000000);
                downBtn.setPadding(24, 24, 24, 24); // Increase touch area
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(96, 96); // Bigger button
                btnParams.setMargins(8, 0, 8, 0);
                downBtn.setLayoutParams(btnParams);

                downBtn.setOnClickListener(v -> {
                    widgetManager.moveWidget(index, index + 1);
                    updateDialogList(listLayout, dialog);
                });
                row.addView(downBtn);
            }

            listLayout.addView(row);
        }
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

        // Add Map Layer (Antenna Visualization)
        if (getActivity() instanceof net.osmand.plus.activities.MapActivity) {
            net.osmand.plus.activities.MapActivity mapActivity = (net.osmand.plus.activities.MapActivity) getActivity();
            if (antennaMapLayer == null) {
                antennaMapLayer = new net.osmand.plus.carlauncher.antenna.AntennaMapLayer(mapActivity.getMapView());
            }
            if (!mapActivity.getMapLayers().getLayers().contains(antennaMapLayer)) {
                mapActivity.getMapLayers().addLayer(antennaMapLayer);
            }
        }

        // Register Receiver for Picker Actions
        antennaReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                if (net.osmand.plus.carlauncher.widgets.AntennaWidget.ACTION_PICK_ANTENNA_POINT
                        .equals(intent.getAction())) {
                    String type = intent
                            .getStringExtra(net.osmand.plus.carlauncher.widgets.AntennaWidget.EXTRA_POINT_TYPE);
                    if (antennaMapLayer != null) {
                        antennaMapLayer.setPickingMode(type);
                    }
                }
            }
        };

        if (getContext() != null) {
            // Note: Context.RECEIVER_EXPORTED or equivalent might be needed for newer
            // Android, but local broadcast is preferred.
            // Using standard registration for now.
            android.content.IntentFilter filter = new android.content.IntentFilter(
                    net.osmand.plus.carlauncher.widgets.AntennaWidget.ACTION_PICK_ANTENNA_POINT);
            getContext().registerReceiver(antennaReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (widgetManager != null) {
            widgetManager.stopAllWidgets();
        }

        // Remove Map Layer
        if (getActivity() instanceof net.osmand.plus.activities.MapActivity && antennaMapLayer != null) {
            ((net.osmand.plus.activities.MapActivity) getActivity()).getMapView().removeLayer(antennaMapLayer);
        }

        // Unregister Receiver
        if (getContext() != null && antennaReceiver != null) {
            try {
                getContext().unregisterReceiver(antennaReceiver);
            } catch (Exception e) {
                // Ignore
            }
            antennaReceiver = null;
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

        // Anten Widget
        widgetManager.addWidget(new net.osmand.plus.carlauncher.widgets.AntennaWidget(getContext(), app));

        // Navigasyon widget
        widgetManager.addWidget(new NavigationWidget(getContext(), app));

        // Muzik widget
        widgetManager.addWidget(new MusicWidget(getContext(), app));

        // Gelecekte: Daha fazla widget eklenecek
        // widgetManager.addWidget(new AltitudeWidget(getContext(), app));
    }

    /**
     * Widget manager'i al (ayarlar icin).
     */
    public WidgetManager getWidgetManager() {
        return widgetManager;
    }
}
