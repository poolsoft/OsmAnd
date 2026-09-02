package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.MapHudLayout;

/**
 * Car Launcher HUD with independent vertical panel modes: compact at the top
 * and full width at the bottom. The bounds are applied after OsmAnd finishes
 * its own layout pass, so later widget refreshes cannot restore core margins.
 */
public class CarMapHudLayout extends MapHudLayout {

    public CarMapHudLayout(@NonNull Context context) {
        super(context);
    }

    public CarMapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CarMapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        applyCarPanelBounds();
    }

    private void applyCarPanelBounds() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        View topPanel = findViewById(R.id.top_widgets_panel);
        View bottomPanel = findViewById(R.id.map_bottom_widgets_panel);
        int compactWidth = Math.round(width * getTopPanelWidthRatio(width));

        layoutPanel(topPanel, 0, 0, compactWidth, true);
        layoutPanel(bottomPanel, 0, height, width, false);
    }

    private float getTopPanelWidthRatio(int widthPx) {
        float widthDp = widthPx / getResources().getDisplayMetrics().density;
        if (widthDp < 500f) return 0.60f;
        if (widthDp < 720f) return 0.55f;
        return 0.50f;
    }

    private static void layoutPanel(@Nullable View panel, int horizontalStart,
                                    int verticalEdge, int width, boolean topEdge) {
        if (panel == null) return;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(
                Math.max(0, panel.getRootView().getHeight()), View.MeasureSpec.AT_MOST);
        panel.measure(widthSpec, heightSpec);
        int panelHeight = panel.getMeasuredHeight();
        int panelTop = topEdge ? verticalEdge : verticalEdge - panelHeight;
        panel.layout(horizontalStart, panelTop,
                horizontalStart + width, panelTop + panelHeight);
    }
}
