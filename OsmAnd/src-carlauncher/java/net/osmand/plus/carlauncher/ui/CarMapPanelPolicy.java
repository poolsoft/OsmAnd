package net.osmand.plus.carlauncher.ui;

import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

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

        updateMargins(topPanel, compactMargin, 0, compactMargin, null);
        updateMargins(bottomPanel, 0, null, 0, 0);
        clearOuterEdgePadding(topPanel, true);
        clearOuterEdgePadding(bottomPanel, false);
    }

    private static void updateMargins(View view, int start, Integer top, int end, Integer bottom) {
        if (view == null || !(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params)) return;
        int resolvedTop = top != null ? top : params.topMargin;
        int resolvedBottom = bottom != null ? bottom : params.bottomMargin;
        if (params.getMarginStart() == start && params.getMarginEnd() == end
                && params.topMargin == resolvedTop && params.bottomMargin == resolvedBottom) {
            return;
        }
        params.setMarginStart(start);
        params.setMarginEnd(end);
        params.topMargin = resolvedTop;
        params.bottomMargin = resolvedBottom;
        view.setLayoutParams(params);
    }

    private static void clearOuterEdgePadding(View view, boolean topEdge) {
        if (view == null) return;
        int top = topEdge ? 0 : view.getPaddingTop();
        int bottom = topEdge ? view.getPaddingBottom() : 0;
        if (top != view.getPaddingTop() || bottom != view.getPaddingBottom()) {
            view.setPaddingRelative(view.getPaddingStart(), top, view.getPaddingEnd(), bottom);
        }
    }
}
