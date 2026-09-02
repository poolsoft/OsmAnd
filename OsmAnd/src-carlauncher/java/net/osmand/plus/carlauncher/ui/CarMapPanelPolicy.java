package net.osmand.plus.carlauncher.ui;

import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

/**
 * Launcher-only map HUD policy. OsmAnd exposes one layout mode for all panels;
 * the car UI needs a compact top panel and a full-width bottom panel.
 */
public final class CarMapPanelPolicy implements View.OnLayoutChangeListener {

    private static final float TOP_WIDTH_LANDSCAPE = 0.60f;
    private static final float TOP_WIDTH_PORTRAIT = 0.50f;

    private final MapActivity activity;
    private View hudContainer;
    private View hudLayout;
    private ViewGroup bottomFragmentContainer;
    private View topPanel;
    private View bottomPanel;
    private boolean applying;

    public CarMapPanelPolicy(@NonNull MapActivity activity) {
        this.activity = activity;
    }

    public void attach() {
        hudContainer = activity.findViewById(R.id.map_hud_container);
        hudLayout = activity.findViewById(R.id.map_hud_layout);
        bottomFragmentContainer = activity.findViewById(R.id.bottomFragmentContainer);
        if (hudLayout == null) return;
        topPanel = hudLayout.findViewById(R.id.top_widgets_panel);
        bottomPanel = hudLayout.findViewById(R.id.map_bottom_widgets_panel);

        clearOuterInsets();
        hudLayout.addOnLayoutChangeListener(this);
        if (topPanel != null) topPanel.addOnLayoutChangeListener(this);
        if (bottomPanel != null) bottomPanel.addOnLayoutChangeListener(this);
        hudLayout.post(this::apply);
    }

    public void detach() {
        if (hudLayout != null) {
            hudLayout.removeOnLayoutChangeListener(this);
            if (topPanel != null) topPanel.removeOnLayoutChangeListener(this);
            if (bottomPanel != null) bottomPanel.removeOnLayoutChangeListener(this);
            hudContainer = null;
            hudLayout = null;
            bottomFragmentContainer = null;
            topPanel = null;
            bottomPanel = null;
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        apply();
    }

    private void apply() {
        if (applying || hudLayout == null || hudLayout.getWidth() <= 0) return;
        applying = true;
        try {
            applyPanelBounds();
        } finally {
            applying = false;
        }
    }

    private void applyPanelBounds() {
        clearOuterInsets();

        boolean portrait = activity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        int contentWidth = Math.max(0,
                hudLayout.getWidth() - hudLayout.getPaddingLeft() - hudLayout.getPaddingRight());
        int compactWidth = Math.round(contentWidth
                * (portrait ? TOP_WIDTH_PORTRAIT : TOP_WIDTH_LANDSCAPE));

        // Keep both panels attached to the physical HUD edges. Centering the
        // compact top panel puts turn information over the route arrow.
        layoutPanel(topPanel, 0, 0, compactWidth, true);
        layoutPanel(bottomPanel, 0, hudLayout.getHeight(), contentWidth, false);
    }

    private void clearOuterInsets() {
        if (hudContainer != null) {
            hudContainer.setFitsSystemWindows(false);
            hudContainer.setPadding(0, 0, 0, 0);
        }
        if (hudLayout != null) {
            hudLayout.setPadding(0, 0, 0, 0);
        }
        if (bottomFragmentContainer != null && bottomFragmentContainer.getChildCount() == 0) {
            // InsetsUtils gives this empty host the navigation-bar padding,
            // shrinking the weighted HUD and leaving a large bottom gap.
            bottomFragmentContainer.setPadding(0, 0, 0, 0);
            bottomFragmentContainer.setMinimumHeight(0);
        }
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
