package net.osmand.plus.carlauncher.dock;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * App Dock RecyclerView adapter.
 */
public class AppDockAdapter extends RecyclerView.Adapter<AppDockAdapter.ViewHolder> {

    private final Context context;
    private final List<AppShortcut> shortcuts;
    private final OnShortcutListener listener;
    private boolean isEditMode = false;

    public interface OnShortcutListener {
        void onShortcutClick(AppShortcut shortcut);

        void onShortcutLongClick(AppShortcut shortcut);

        void onRemoveClick(AppShortcut shortcut);
    }

    public AppDockAdapter(@NonNull Context context, @NonNull OnShortcutListener listener) {
        this.context = context;
        this.shortcuts = new ArrayList<>();
        this.listener = listener;
    }

    public void setShortcuts(List<AppShortcut> shortcuts) {
        this.shortcuts.clear();
        this.shortcuts.addAll(shortcuts);
        notifyDataSetChanged();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout itemView = new LinearLayout(context);
        itemView.setOrientation(LinearLayout.VERTICAL);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(
                dpToPx(80),
                ViewGroup.LayoutParams.MATCH_PARENT));
        itemView.setGravity(android.view.Gravity.CENTER);
        itemView.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppShortcut shortcut = shortcuts.get(position);
        holder.bind(shortcut);
    }

    @Override
    public int getItemCount() {
        return shortcuts.size();
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView nameView;
        private final ImageView removeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            LinearLayout container = (LinearLayout) itemView;

            // Remove button (edit mode'da gorunur)
            removeButton = new ImageView(context);
            removeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            removeButton.setLayoutParams(new LinearLayout.LayoutParams(
                    dpToPx(20), dpToPx(20))); // Reduced size
            removeButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
            removeButton.setColorFilter(0xFFFF0000);
            removeButton.setVisibility(View.GONE);
            container.addView(removeButton);

            // Icon
            iconView = new ImageView(context);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(
                    dpToPx(36), dpToPx(36))); // Reduced from 48 to 36
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            container.addView(iconView);

            // Name
            nameView = new TextView(context);
            nameView.setTextColor(0xFFFFFFFF);
            nameView.setTextSize(10);
            nameView.setGravity(android.view.Gravity.CENTER);
            nameView.setMaxLines(1);
            nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nameView.setPadding(0, dpToPx(0), 0, 0); // Reduced padding
            container.addView(nameView);
        }

        public void bind(AppShortcut shortcut) {
            iconView.setImageDrawable(shortcut.getIcon());
            nameView.setText(shortcut.getAppName());

            // Edit mode kontrolu
            removeButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            // Click listener
            itemView.setOnClickListener(v -> {
                if (isEditMode) {
                    // Edit mode'da click ignore
                } else {
                    launchApp(shortcut);
                }
            });

            // Long click listener
            itemView.setOnLongClickListener(v -> {
                if (!isEditMode && listener != null) {
                    listener.onShortcutLongClick(shortcut);
                }
                return true;
            });

            // Remove button click
            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(shortcut);
                }
            });
        }

        private void launchApp(AppShortcut shortcut) {
            try {
                Intent intent = context.getPackageManager()
                        .getLaunchIntentForPackage(shortcut.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
