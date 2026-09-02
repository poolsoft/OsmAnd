package net.osmand.plus.carlauncher.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

/**
 * Clears system insets around the launcher-only {@link CarMapHudLayout}.
 * Panel sizing itself belongs to that layout so OsmAnd refreshes cannot race it.
 */
public final class CarMapPanelPolicy implements View.OnLayoutChangeListener {

    private final MapActivity activity;
    private View hudContainer;
    private View hudLayout;
    private ViewGroup bottomFragmentContainer;

    public CarMapPanelPolicy(@NonNull MapActivity activity) {
        this.activity = activity;
    }

    public void attach() {
        hudContainer = activity.findViewById(R.id.map_hud_container);
        hudLayout = activity.findViewById(R.id.map_hud_layout);
        bottomFragmentContainer = activity.findViewById(R.id.bottomFragmentContainer);
        if (hudLayout == null) return;
        clearOuterInsets();
        hudLayout.addOnLayoutChangeListener(this);
        hudLayout.post(this::clearOuterInsets);
    }

    public void detach() {
        if (hudLayout != null) {
            hudLayout.removeOnLayoutChangeListener(this);
            hudContainer = null;
            hudLayout = null;
            bottomFragmentContainer = null;
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        clearOuterInsets();
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

}
