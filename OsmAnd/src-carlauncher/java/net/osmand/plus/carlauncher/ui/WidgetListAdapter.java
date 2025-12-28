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
        void onEditModeRequested();
    }

    private final List<BaseWidget> widgets;
    private final boolean isPortrait;
    private final OnWidgetActionListener actionListener;
    
    private boolean isEditMode = false;
    private int unitSize = 0; // The height of 1 unit (Screen / 6)

    public WidgetListAdapter(List<BaseWidget> widgets, boolean isPortrait, OnWidgetActionListener actionListener) {
        this.widgets = widgets;
        this.isPortrait = isPortrait;
        this.actionListener = actionListener;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }
    
    public void setUnitSize(int unitSize, boolean isPortrait) {
        this.unitSize = unitSize;
        // Hack: Update local isPortrait if needed, though constructor sets it final. 
        // Ideally remove final or create new adapter. But for now, we rely on Fragment passing correct Unit Size.
        // We do need to know orientation for Width vs Height decision.
        // Let's assume the constructor's isPortrait is correct.
        notifyDataSetChanged();
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
                // UnitSize here is (ScreenWidth / 2) passed from Fragment
                params.width = unitSize - marginTotal; 
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                
                // Add Button should also follow this? Yes.
            } else {
                // Landscape: Vertical Scroll, Height is Dynamic, Width is Full
                // UnitSize is (ScreenHeight / 6)
                
                int multiplier = 2; // Default Small
                
                if (getItemViewType(position) == VIEW_TYPE_ADD) {
                     multiplier = 2; 
                } else {
                    BaseWidget w = widgets.get(position);
                    switch (w.getSize()) {
                        case SMALL: multiplier = 2; break; 
                        case MEDIUM: multiplier = 3; break; 
                        case LARGE: multiplier = 6; break; 
                    }
                }
                
                params.height = (unitSize * multiplier) - marginTotal;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            
            holder.itemView.setLayoutParams(params);
        } else {
             // Fallback if Unit unavailable (shouldn't happen often)
             ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
             params.setMargins(margin, margin, margin, margin);
             holder.itemView.setLayoutParams(params);
        }

        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            holder.itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onAddWidgetClicked();
            });
            return;
        }

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

        // Edit Mode Overlays (Delete Only)
        // Check if overlay exists
        View overlay = holder.container.findViewWithTag("EditOverlay");
        if (isEditMode) {
             if (overlay == null) {
                 int size = dpToPx(holder.itemView.getContext(), 48);
                 int padding = dpToPx(holder.itemView.getContext(), 8);
                 
                 ImageView deleteBtn = new ImageView(holder.itemView.getContext());
                 deleteBtn.setImageResource(android.R.drawable.ic_menu_delete);
                 deleteBtn.setBackgroundColor(0xCCFF0000);
                 deleteBtn.setColorFilter(0xFFFFFFFF);
                 deleteBtn.setPadding(padding, padding, padding, padding);
                 deleteBtn.setTag("EditOverlay");
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                     deleteBtn.setElevation(20);
                 }
                 
                 FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
                 params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                 params.setMargins(padding, padding, padding, padding);
                 
                 holder.container.addView(deleteBtn, params);
                 deleteBtn.setOnClickListener(v -> {
                     if (actionListener != null) actionListener.onWidgetRemoved(widget);
                 });
                 
                 // Scale effect
                 holder.itemView.setScaleX(0.95f);
                 holder.itemView.setScaleY(0.95f);
             }
        } else {
            if (overlay != null) {
                holder.container.removeView(overlay);
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
            }
        }

        // Long Press to Enter Edit Mode
        holder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null && !isEditMode) {
                 actionListener.onEditModeRequested();
                 return true;
            }
            return false;
        });
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
