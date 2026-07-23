package net.osmand.plus.carlauncher.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Dock kisayol listesinde kaydirma durdugunda, ekranda gorunen ilk ikonun
 * (en ustte veya en solda) yarim kalmasini / kirpilmasini engeller.
 * Gorunen ilk ikonu hizalama baslangicina (Start / Top / Left) tam oturtur.
 */
public class StartSnapHelper extends LinearSnapHelper {

    private OrientationHelper verticalHelper;
    private OrientationHelper horizontalHelper;

    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        int[] out = new int[2];
        if (layoutManager.canScrollHorizontally()) {
            out[0] = distanceToStart(targetView, getHorizontalHelper(layoutManager));
        } else {
            out[0] = 0;
        }

        if (layoutManager.canScrollVertically()) {
            out[1] = distanceToStart(targetView, getVerticalHelper(layoutManager));
        } else {
            out[1] = 0;
        }
        return out;
    }

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            if (layoutManager.canScrollHorizontally()) {
                return findStartView(layoutManager, getHorizontalHelper(layoutManager));
            } else {
                return findStartView(layoutManager, getVerticalHelper(layoutManager));
            }
        }
        return super.findSnapView(layoutManager);
    }

    private int distanceToStart(View targetView, OrientationHelper helper) {
        return helper.getDecoratedStart(targetView) - helper.getStartAfterPadding();
    }

    private View findStartView(RecyclerView.LayoutManager layoutManager, OrientationHelper helper) {
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager llm = (LinearLayoutManager) layoutManager;
            int firstChildPosition = llm.findFirstVisibleItemPosition();

            if (firstChildPosition == RecyclerView.NO_POSITION) {
                return null;
            }

            View firstChild = llm.findViewByPosition(firstChildPosition);
            if (firstChild == null) return null;

            int childStart = helper.getDecoratedStart(firstChild);
            int childEnd = helper.getDecoratedEnd(firstChild);
            int childLength = helper.getDecoratedMeasurement(firstChild);

            if (childEnd >= childLength / 2 && childStart >= 0) {
                return firstChild;
            } else if (llm.findLastCompletelyVisibleItemPosition() == llm.getItemCount() - 1) {
                return null;
            } else {
                return llm.findViewByPosition(firstChildPosition + 1);
            }
        }
        return null;
    }

    private OrientationHelper getVerticalHelper(RecyclerView.LayoutManager layoutManager) {
        if (verticalHelper == null || verticalHelper.getLayoutManager() != layoutManager) {
            verticalHelper = OrientationHelper.createVerticalHelper(layoutManager);
        }
        return verticalHelper;
    }

    private OrientationHelper getHorizontalHelper(RecyclerView.LayoutManager layoutManager) {
        if (horizontalHelper == null || horizontalHelper.getLayoutManager() != layoutManager) {
            horizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager);
        }
        return horizontalHelper;
    }
}
