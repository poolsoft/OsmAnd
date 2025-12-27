package net.osmand.plus.carlauncher.ui;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.carlauncher.widgets.BaseWidget;

import java.util.List;

public class WidgetListAdapter extends RecyclerView.Adapter<WidgetListAdapter.WidgetViewHolder> {

    private final List<BaseWidget> widgets;
    private final boolean isPortrait;

    public WidgetListAdapter(List<BaseWidget> widgets, boolean isPortrait) {
        this.widgets = widgets;
        this.isPortrait = isPortrait;
    }

    @NonNull
    @Override
    public WidgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a FrameLayout container for the widget
        FrameLayout container = new FrameLayout(parent.getContext());
        
        // Layout params will be set in onBind based on widget or orientation?
        // Actually, the ItemView itself needs params.
        
        ViewGroup.MarginLayoutParams params;
        if (isPortrait) {
             // Portrait: Horizontal List -> Fixed Width items usually? or Full Width?
             // Previous code: width=130dp, height=MATCH_PARENT for Portrait Horizontal list?
             // Let's assume user wants "List" behavior.
             // If Portrait Mode 0 is a vertical list? No, check WidgetPanelFragment:
             // isPortrait ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL
             // So Portrait = Horizontal Scroll. Landscape = Vertical Scroll.
             
             if (isPortrait) {
                 // Horizontal Item
                 int width = dpToPx(parent.getContext(), 130);
                 params = new ViewGroup.MarginLayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
                 int margin = dpToPx(parent.getContext(), 4);
                 params.setMargins(margin, margin, margin, margin);
             } else {
                 // Vertical Item (Landscape)
                 params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                 int margin = dpToPx(parent.getContext(), 4);
                 params.setMargins(margin, margin, margin, margin);
             }
        } else {
             // Landscape: Vertical List
             params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
             int margin = dpToPx(parent.getContext(), 4);
             params.setMargins(margin, margin, margin, margin);
        }
        
        container.setLayoutParams(params);
        container.setPadding(4, 4, 4, 4); // Inner padding
        return new WidgetViewHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull WidgetViewHolder holder, int position) {
        BaseWidget widget = widgets.get(position);
        android.view.View widgetView = widget.getRootView();
        
        if (widgetView == null) {
            widgetView = widget.createView();
        }

        if (widgetView != null) {
            // Detach from old parent
            if (widgetView.getParent() != null) {
                ((ViewGroup) widgetView.getParent()).removeView(widgetView);
            }

            // Clean container
            holder.container.removeAllViews();

            // Add to container
            // For List Mode, we generally expect the widget to control its content height, 
            // but fill the container width (in vertical mode).
            holder.container.addView(widgetView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            
            // Note: If Widget's root view is CardView, it might have specific logic.
        }
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

        public WidgetViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            container = (FrameLayout) itemView;
        }
    }
}
