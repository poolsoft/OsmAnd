package net.osmand.plus.carlauncher.ui;

import android.content.res.Configuration;
import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.enums.PanelsLayoutMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;

/**
 * Launcher-only map HUD policy. OsmAnd exposes one layout mode for all panels;
 * the car UI needs a compact top panel and a full-width bottom panel.
 */
public final class CarMapPanelPolicy implements View.OnLayoutChangeListener {

    private static final float TOP_WIDTH_LANDSCAPE = 0.60f;
    private static final float TOP_WIDTH_PORTRAIT = 0.50f;

    private final MapActivity activity;
    private View hudLayout;

    public CarMapPanelPolicy(@NonNull MapActivity activity) {
        this.activity = activity;
    }

    public void attach() {
        hudLayout = activity.findViewById(R.id.map_hud_layout);
        if (hudLayout == null) return;

        // Wide is the safer collision model for the bottom route panel. The top
        // panel is narrowed below without replacing any OsmAnd widget.
        OsmandApplication app = (OsmandApplication) activity.getApplication();
        app.getSettings()
                .getPanelsLayoutMode(activity, ScreenLayoutMode.getDefault(activity))
                .set(PanelsLayoutMode.WIDE);

        hudLayout.addOnLayoutChangeListener(this);
        hudLayout.post(this::apply);
    }

    public void detach() {
        if (hudLayout != null) {
            hudLayout.removeOnLayoutChangeListener(this);
            hudLayout = null;
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        apply();
    }

    private void apply() {
        if (hudLayout == null || hudLayout.getWidth() <= 0) return;
        View topPanel = hudLayout.findViewById(R.id.top_widgets_panel);
        View bottomPanel = hudLayout.findViewById(R.id.map_bottom_widgets_panel);

        boolean portrait = activity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        int contentWidth = Math.max(0,
                hudLayout.getWidth() - hudLayout.getPaddingLeft() - hudLayout.getPaddingRight());
        int compactWidth = Math.round(contentWidth
                * (portrait ? TOP_WIDTH_PORTRAIT : TOP_WIDTH_LANDSCAPE));
        int compactMargin = Math.max(0, (contentWidth - compactWidth) / 2);

        layoutPanel(topPanel, hudLayout.getPaddingLeft() + compactMargin,
                hudLayout.getPaddingTop(), compactWidth, true);
        layoutPanel(bottomPanel, hudLayout.getPaddingLeft(),
                hudLayout.getHeight() - hudLayout.getPaddingBottom(), contentWidth, false);
    }

    private static void layoutPanel(View view, int horizontalStart, int verticalEdge,
                                    int width, boolean topEdge) {
        if (view == null) return;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(
                Math.max(0, view.getRootView().getHeight()), View.MeasureSpec.AT_MOST);
        view.measure(widthSpec, heightSpec);
        int height = view.getMeasuredHeight();
        int top = topEdge ? verticalEdge : verticalEdge - height;
        view.layout(horizontalStart, top, horizontalStart + width, top + height);
    }
}
