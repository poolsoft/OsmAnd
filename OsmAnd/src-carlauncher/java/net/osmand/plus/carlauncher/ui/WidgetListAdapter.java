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
    private boolean isHorizontalScroll; // Replaces isPortrait
    private boolean isMetroMode = false;
    private final OnWidgetActionListener actionListener;
    private int unitSize = 0;

    public WidgetListAdapter(List<BaseWidget> widgets, boolean isHorizontalScroll, OnWidgetActionListener actionListener) {
        this.widgets = widgets;
        this.isHorizontalScroll = isHorizontalScroll;
        this.actionListener = actionListener;
    }

    public BaseWidget getWidgetAt(int position) {
        if (position >= 0 && position < widgets.size()) {
            return widgets.get(position);
        }
        return null;
    }

    public void setUnitSize(int unitSize, boolean isHorizontalScroll) {
        this.unitSize = unitSize;
        this.isHorizontalScroll = isHorizontalScroll;
        notifyDataSetChanged();
    }
    
    public void setMetroMode(boolean isMetro) {
        this.isMetroMode = isMetro;
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

    @Override
    public int getItemViewType(int position) {
        if (position == widgets.size()) {
            return VIEW_TYPE_ADD;
        }
        return VIEW_TYPE_WIDGET;
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
        
        // --- 1. Sizing Calculation ---
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.setMargins(margin, margin, margin, margin);

        boolean isAddButton = getItemViewType(position) == VIEW_TYPE_ADD;
        
        if (unitSize > 0) {
             if (isMetroMode) {
                // METRO MODE
                int widthMult = 1;
                int heightMult = 1;
                
                if (!isAddButton) {
                    BaseWidget w = widgets.get(position);
                    switch (w.getSize()) {
                        case MEDIUM: widthMult = 2; heightMult = 1; break;
                        case LARGE:  widthMult = 2; heightMult = 2; break;
                        default: widthMult = 1; heightMult = 1; break;
                    }
                }
                // Add Button is always 1x1
                
                params.width = (unitSize * widthMult) - marginTotal;
                params.height = (unitSize * heightMult) - marginTotal;
                
            } else {
                // CLASSIC MODE
                if (isHorizontalScroll) {
                    params.width = unitSize - marginTotal; 
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else {
                     int multiplier = 1;
                     if (!isAddButton) { // Add button always 1x in Classic Vertical
                         // Wait, Classic Vertical (Landscape) -> Height varies? 
                         // Before we used Multiplier logic for widget size
                         // Let's stick to simple 1x for Add Button
                         multiplier = 1; // Simplify Add Button
                         
                         // For Widgets:
                         BaseWidget w = widgets.get(position);
                         switch (w.getSize()) {
                            case MEDIUM: multiplier = 2; break; 
                            case LARGE: multiplier = 3; break; 
                            default: multiplier = 1; break;
                        }
                     }
                     params.height = (unitSize * multiplier) - marginTotal;
                     params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
            }
        }
        holder.itemView.setLayoutParams(params);

        // --- 2. Content Binding ---
        holder.container.removeAllViews();
        
        if (isAddButton) {
            // Create Add Button View
            ImageView iv = new ImageView(holder.itemView.getContext());
            iv.setImageResource(net.osmand.plus.R.drawable.ic_action_plus); // Standard Plus Icon
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setBackgroundColor(0x80000000); // Semi-transparent black background
            
            // Padding
            int p = dpToPx(holder.itemView.getContext(), 16);
            iv.setPadding(p, p, p, p);
            
            holder.container.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
            holder.itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onAddWidgetClicked();
            });
            holder.itemView.setOnLongClickListener(null);
            
        } else {
            // Bind Widget
            BaseWidget widget = widgets.get(position);
            View widgetView = widget.getRootView();
            if (widgetView == null) widgetView = widget.createView();
            
            if (widgetView != null) {
                if (widgetView.getParent() != null) ((ViewGroup)widgetView.getParent()).removeView(widgetView);
                holder.container.addView(widgetView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            
            holder.itemView.setOnClickListener(null); // Widgets handle their own clicks usually or we can add edit mode
            holder.itemView.setOnLongClickListener(v -> {
                if (actionListener != null) actionListener.onWidgetLongClicked(v, widget);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return widgets.size() + 1; // +1 for Add Button 
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
