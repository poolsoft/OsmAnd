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
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(net.osmand.plus.R.drawable.bg_glass_panel);

        widgetContainer = new LinearLayout(getContext());
        widgetContainer.setOrientation(LinearLayout.VERTICAL);
        widgetContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        widgetContainer.setPadding(24, 24, 24, 24);
        
        // Long click to manage widgets
        widgetContainer.setOnLongClickListener(v -> {
            showWidgetManagementDialog();
            return true;
        });
        // Also allow clicking on empty space
        scrollView.setOnLongClickListener(v -> {
            showWidgetManagementDialog();
            return true;
        });

        scrollView.addView(widgetContainer);
        return scrollView;
    }

    private void showWidgetManagementDialog() {
        if (getContext() == null || widgetManager == null) return;

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
