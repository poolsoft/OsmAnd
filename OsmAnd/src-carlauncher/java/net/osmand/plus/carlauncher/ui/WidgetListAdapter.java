package net.osmand.plus.carlauncher.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import java.util.Collections;
import java.util.List;

public class WidgetListAdapter extends RecyclerView.Adapter<WidgetListAdapter.WidgetViewHolder> {

    private static final int VIEW_TYPE_WIDGET = 0;
    private static final int VIEW_TYPE_ADD = 1;

    public interface OnWidgetActionListener {
        void onWidgetOrderChanged(List<BaseWidget> newOrder);
        void onWidgetRemoved(BaseWidget widget);
        void onAddWidgetClicked();
        void onWidgetLongClicked(View view, BaseWidget widget);
    }

    private final List<BaseWidget> widgets;
    private final boolean isPortrait;
    private final OnWidgetActionListener actionListener;
    
    private int unitSize = 0; // The height of 1 unit (Screen / 6)

    public WidgetListAdapter(List<BaseWidget> widgets, boolean isPortrait, OnWidgetActionListener actionListener) {
        this.widgets = widgets;
        this.isPortrait = isPortrait;
        this.actionListener = actionListener;
    }

    public void setUnitSize(int unitSize, boolean isPortrait) {
        this.unitSize = unitSize;
        notifyDataSetChanged();
    }
    
    // Check Drag & Drop
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(widgets, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(widgets, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        // DB update now handled in Fragment onDragEnd (clearView)
    }

    public List<BaseWidget> getWidgets() {
        return widgets;
    }

    @NonNull
    @Override
    public WidgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout container = new FrameLayout(parent.getContext());
        
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 0); 
        
        container.setLayoutParams(params);
        return new WidgetViewHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull WidgetViewHolder holder, int position) {
        int margin = dpToPx(holder.itemView.getContext(), 4);
        int marginTotal = margin * 2;

        // Dynamic Sizing
        if (unitSize > 0) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            if (params == null) {
                params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            
            // Set Margins
            params.setMargins(margin, margin, margin, margin);

            if (isPortrait) {
                // Portrait: Horizontal Scroll, Width is Dynamic (Unit), Height is Full
                params.width = unitSize - marginTotal; 
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                // Landscape: Vertical Scroll, Height is Dynamic, Width is Full
                int multiplier = 2; // Default Small
                
                BaseWidget w = widgets.get(position);
                switch (w.getSize()) {
                    case SMALL: multiplier = 2; break; 
                    case MEDIUM: multiplier = 3; break; 
                    case LARGE: multiplier = 6; break; 
                }
                
                params.height = (unitSize * multiplier) - marginTotal;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            
            holder.itemView.setLayoutParams(params);
        } else {
             // Fallback
             ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
             params.setMargins(margin, margin, margin, margin);
             holder.itemView.setLayoutParams(params);
        }

        // Safety Catch
        if (position >= widgets.size()) return;

        BaseWidget widget = widgets.get(position);
        
        // Bind Widget Content
        View widgetView = widget.getRootView();
        if (widgetView == null) widgetView = widget.createView();
        
        if (widgetView != null) {
            if (widgetView.getParent() != null) ((ViewGroup)widgetView.getParent()).removeView(widgetView);
            holder.container.removeAllViews();
            holder.container.addView(widgetView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        }

        // Long Press handled by ItemTouchHelper for Drag & Drop
        // holder.itemView.setOnLongClickListener... REMOVED
    }

        // Long Press handled by ItemTouchHelper for Drag & Drop
        // holder.itemView.setOnLongClickListener... REMOVED
    }

    @Override
    public int getItemCount() {
        return widgets.size(); 
    }
    
    private int dpToPx(android.content.Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static class WidgetViewHolder extends RecyclerView.ViewHolder {
        FrameLayout container;
        public WidgetViewHolder(@NonNull View itemView) {
            super(itemView);
            container = (FrameLayout) itemView;
        }
    }
}
