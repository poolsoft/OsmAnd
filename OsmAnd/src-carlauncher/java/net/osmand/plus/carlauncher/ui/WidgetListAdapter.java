package net.osmand.plus.carlauncher.ui;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.carlauncher.widgets.BaseWidget;

import java.util.List;

public class WidgetListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_WIDGET = 0;
    private static final int VIEW_TYPE_ADD = 1;

    private final List<BaseWidget> widgets;
    private final boolean isPortrait;
    private final OnWidgetActionListener actionListener;
    private boolean isEditMode = false;

    public interface OnWidgetActionListener {
        void onWidgetOrderChanged(List<BaseWidget> newOrder);
        void onWidgetRemoved(BaseWidget widget);
        void onWidgetSizeChanged(BaseWidget widget);
        void onAddWidgetClicked();
        void onEditModeRequested(); // Long press triggers this
    }

    public WidgetListAdapter(List<BaseWidget> widgets, boolean isPortrait, OnWidgetActionListener listener) {
        this.widgets = widgets;
        this.isPortrait = isPortrait;
        this.actionListener = listener;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }
    
    public boolean isEditMode() {
        return isEditMode;
    }

    @Override
    public int getItemViewType(int position) {
        if (isEditMode && position == widgets.size()) {
            return VIEW_TYPE_ADD;
        }
        return VIEW_TYPE_WIDGET;
    }

    @Override
    public int getItemCount() {
        // In Edit Mode, +1 for "Add Button"
        return isEditMode ? widgets.size() + 1 : widgets.size();
    }
    
    // Grid Span Logic
    public int getSpanSize(int position) {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            return 1; // Add button takes 1 slot (Small)
        }
        if (position >= widgets.size()) return 1; // Safety
        
        BaseWidget w = widgets.get(position);
        switch (w.getSize()) {
            case LARGE: return 4; // Full Width
            case MEDIUM: return 2; // Half Width
            case SMALL: return 1; // Quarter Width
            default: return 4;
        }
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        // Prevent moving the "Add Button" (last item)
        if (getItemViewType(toPosition) == VIEW_TYPE_ADD || getItemViewType(fromPosition) == VIEW_TYPE_ADD) {
            return false;
        }
        
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                java.util.Collections.swap(widgets, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                java.util.Collections.swap(widgets, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        
        if (actionListener != null) {
            actionListener.onWidgetOrderChanged(widgets);
        }
        return true;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ADD) {
            // "Add Widget" Button View
            android.widget.ImageButton addBtn = new android.widget.ImageButton(parent.getContext());
            addBtn.setImageResource(android.R.drawable.ic_input_add); // Simple + icon
            addBtn.setBackgroundResource(android.R.drawable.btn_default);
            addBtn.setColorFilter(0xFF00FF00); // Green tint
            
            // Layout Params for Add Button - needs to adapt to grid cell
            // In Grid, height should match standard widget height (which varies)
            // Or just a fixed height box.
            
            int height = dpToPx(parent.getContext(), 130); // Standard widget height
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            
            int margin = dpToPx(parent.getContext(), 8);
            params.setMargins(margin, margin, margin, margin);
            addBtn.setLayoutParams(params);
            
            return new AddButtonViewHolder(addBtn);
        }
        
        // Normal Widget Container
        FrameLayout container = new FrameLayout(parent.getContext());
        
        // Visual Styling: Modern Card + Touch Animation
        container.setBackgroundResource(net.osmand.plus.R.drawable.bg_widget_card_modern);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            container.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(
                    parent.getContext(), net.osmand.plus.R.animator.widget_touch_scale));
        }
        
        // Layout Params - Height fixed, Width MATCH_PARENT (controlled by Grid)
        int height = isPortrait ? ViewGroup.LayoutParams.MATCH_PARENT : dpToPx(parent.getContext(), 130);
        // In Portrait grid, height is fixed? Or width fixed? Grid scrolls horizontally?
        // Wait, current design: Landscape -> Vertical Scroll. Portrait -> Horizontal Scroll.
        // If Grid Manager is Vertical (Landscape), we set height fixed.
        // If Grid Manager is Horizontal (Portrait), we set width fixed? No, in Grid width is calculated.
        
        // Let's assume standard cell height for Vertical Grid
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // Width filled by Grid
                dpToPx(parent.getContext(), 140)); // Fixed Height 140dp
        
        int margin = dpToPx(parent.getContext(), 4);
        params.setMargins(margin, margin, margin, margin);
        
        container.setLayoutParams(params);
        container.setPadding(4, 4, 4, 4);
        
        return new WidgetViewHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddButtonViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onAddWidgetClicked();
            });
            return;
        }

        WidgetViewHolder widgetHolder = (WidgetViewHolder) holder;
        BaseWidget widget = widgets.get(position);
        
        // --- Widget Content ---
        android.view.View widgetView = widget.getRootView();
        if (widgetView == null) {
            widgetView = widget.createView();
        }

        if (widgetView != null) {
            if (widgetView.getParent() != null) {
                ((ViewGroup) widgetView.getParent()).removeView(widgetView);
            }
            widgetHolder.container.removeAllViews();
            widgetHolder.container.addView(widgetView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)); // Fill container
            
            // --- Edit Mode Overlay ---
            if (isEditMode) {
                // 1. Delete Button (Top Right)
                android.widget.ImageView deleteBtn = new android.widget.ImageView(widgetHolder.container.getContext());
                deleteBtn.setImageResource(android.R.drawable.ic_delete);
                deleteBtn.setBackgroundColor(0xAAFF0000); // Semi-transparent Red
                deleteBtn.setPadding(8, 8, 8, 8);
                
                FrameLayout.LayoutParams delParams = new FrameLayout.LayoutParams(64, 64);
                delParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                widgetHolder.container.addView(deleteBtn, delParams);
                
                deleteBtn.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onWidgetRemoved(widget);
                });
                
                // 2. Resize Button (Bottom Right)
                android.widget.ImageView resizeBtn = new android.widget.ImageView(widgetHolder.container.getContext());
                resizeBtn.setImageResource(android.R.drawable.ic_menu_crop); // Use crop as resize icon
                resizeBtn.setBackgroundColor(0xAA0000FF); // Semi-transparent Blue
                resizeBtn.setPadding(8, 8, 8, 8);
                
                FrameLayout.LayoutParams resizeParams = new FrameLayout.LayoutParams(64, 64);
                resizeParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
                widgetHolder.container.addView(resizeBtn, resizeParams);
                
                resizeBtn.setOnClickListener(v -> {
                    cycleWidgetSize(widget);
                });
                
                // Scale Effect
                widgetHolder.itemView.setScaleX(0.95f);
                widgetHolder.itemView.setScaleY(0.95f);
            } else {
                widgetHolder.itemView.setScaleX(1.0f);
                widgetHolder.itemView.setScaleY(1.0f);
            }
        }

        // Long Press to Enter Edit Mode
        widgetHolder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null && !isEditMode) {
                 actionListener.onEditModeRequested();
                 return true;
            }
            return false;
        });
    }
    
    private void cycleWidgetSize(BaseWidget widget) {
        // Cycle: S -> M -> L -> S
        switch (widget.getSize()) {
            case SMALL: widget.setSize(BaseWidget.WidgetSize.MEDIUM); break;
            case MEDIUM: widget.setSize(BaseWidget.WidgetSize.LARGE); break;
            case LARGE: widget.setSize(BaseWidget.WidgetSize.SMALL); break;
        }
        notifyItemChanged(widgets.indexOf(widget));
        if (actionListener != null) actionListener.onWidgetSizeChanged(widget);
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
    
    static class AddButtonViewHolder extends RecyclerView.ViewHolder {
        public AddButtonViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
        }
    }
}
