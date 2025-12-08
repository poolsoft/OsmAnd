package net.osmand.plus.plugins.rightpanel;

import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmAndApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.utils.AndroidUtils;

public class RightPanelPlugin extends OsmandPlugin {

    private FrameLayout rightPanel;
    private LinearLayout widgetsContainer;

    public RightPanelPlugin(@NonNull OsmAndApplication app) {
        super(app);
    }

    @Override
    public String getId() {
        return "right_panel";
    }

    @Override
    public String getName() {
        return "Right Panel";
    }

    @Override
    public CharSequence getDescription(boolean linksEnabled) {
        return "Right panel with various widgets";
    }

    @Override
    public int getLogoResourceId() {
        return R.drawable.ic_action_settings;
    }

    @Override
    public Drawable getAssetResourceImage() {
        return app.getUIUtilities().getIcon(R.drawable.ic_action_settings);
    }

    @Override
    public boolean init(@NonNull OsmAndApplication app, @Nullable android.app.Activity activity) {
        if (activity instanceof MapActivity) {
            createRightPanel((MapActivity) activity);
        }
        return true;
    }

    private void createRightPanel(MapActivity activity) {
        // Create right panel container
        rightPanel = new FrameLayout(activity);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                (int) (AndroidUtils.getScreenWidth(activity) * 0.3f), // 30% width
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END
        );
        rightPanel.setLayoutParams(panelParams);
        rightPanel.setBackgroundColor(0x80000000); // Semi-transparent background

        // Create widgets container
        widgetsContainer = new LinearLayout(activity);
        widgetsContainer.setOrientation(LinearLayout.VERTICAL);
        widgetsContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Add sample widgets
        addMusicControlWidget(activity);
        addClockWidget(activity);
        addSpeedometerWidget(activity);

        rightPanel.addView(widgetsContainer);

        // Add panel to map view
        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.addView(rightPanel);
        }
    }

    private void addMusicControlWidget(MapActivity activity) {
        TextView musicWidget = new TextView(activity);
        musicWidget.setText("🎵 Music Control");
        musicWidget.setTextColor(0xFFFFFFFF);
        musicWidget.setPadding(16, 16, 16, 16);
        musicWidget.setBackgroundColor(0x40000000);
        musicWidget.setOnClickListener(v -> {
            // Music control logic here
            app.showToastMessage("Music control clicked");
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        musicWidget.setLayoutParams(params);

        widgetsContainer.addView(musicWidget);
    }

    private void addClockWidget(MapActivity activity) {
        TextView clockWidget = new TextView(activity);
        clockWidget.setText("🕐 " + java.text.DateFormat.getTimeInstance().format(new java.util.Date()));
        clockWidget.setTextColor(0xFFFFFFFF);
        clockWidget.setPadding(16, 16, 16, 16);
        clockWidget.setBackgroundColor(0x40000000);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        clockWidget.setLayoutParams(params);

        widgetsContainer.addView(clockWidget);
    }

    private void addSpeedometerWidget(MapActivity activity) {
        TextView speedWidget = new TextView(activity);
        speedWidget.setText("🏎️ Speed: -- km/h");
        speedWidget.setTextColor(0xFFFFFFFF);
        speedWidget.setPadding(16, 16, 16, 16);
        speedWidget.setBackgroundColor(0x40000000);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        speedWidget.setLayoutParams(params);

        widgetsContainer.addView(speedWidget);
    }

    @Override
    public void disable(@NonNull OsmAndApplication app) {
        super.disable(app);
        // Remove panel when disabled
        if (rightPanel != null && rightPanel.getParent() != null) {
            ((ViewGroup) rightPanel.getParent()).removeView(rightPanel);
        }
    }

    // Dummy method to fix CI compilation error
    public boolean setNavDrawerLogo(String imageUri) throws android.os.RemoteException {
        return false;
    }
}
