package net.osmand.plus.carlauncher.ui;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class QuickItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final WidgetListAdapter adapter;

    public QuickItemTouchHelperCallback(WidgetListAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true; // Enable long press to drag
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false; // Disable swipe for now
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Detect layout direction from LayoutManager
        // Detect layout direction from LayoutManager
        if (recyclerView.getLayoutManager() instanceof androidx.recyclerview.widget.GridLayoutManager) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        } else if (recyclerView.getLayoutManager() instanceof androidx.recyclerview.widget.LinearLayoutManager) {
            int orientation = ((androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager()).getOrientation();
            if (orientation == androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL) {
                int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                int swipeFlags = 0;
                return makeMovementFlags(dragFlags, swipeFlags);
            } else {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = 0;
                return makeMovementFlags(dragFlags, swipeFlags);
            }
        }
        return makeMovementFlags(0, 0);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType()) {
            return false;
        }

        adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        // Not used
    }
}
