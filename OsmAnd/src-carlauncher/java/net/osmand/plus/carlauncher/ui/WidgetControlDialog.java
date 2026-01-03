package net.osmand.plus.carlauncher.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;
import net.osmand.plus.carlauncher.widgets.WidgetRegistry;

import java.util.ArrayList;
import java.util.List;

public class WidgetControlDialog extends DialogFragment {

    private WidgetManager widgetManager;
    private WidgetControlAdapter adapter;
    private List<BaseWidget> editingList;
    private Runnable onDismissCallback;

    public void setWidgetManager(WidgetManager widgetManager) {
        this.widgetManager = widgetManager;
    }

    public void setOnDismissCallback(Runnable onDismissCallback) {
        this.onDismissCallback = onDismissCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_widget_control, container, false);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (widgetManager == null) {
            dismiss();
            return;
        }

        // Initialize editing list (Copy of visible widgets)
        editingList = new ArrayList<>(widgetManager.getVisibleWidgets());

        RecyclerView recyclerView = view.findViewById(R.id.control_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new WidgetControlAdapter(editingList, new WidgetControlAdapter.OnControlActionListener() {
            @Override
            public void onDeleteClicked(BaseWidget widget, int position) {
                editingList.remove(position);
                adapter.notifyItemRemoved(position);
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                 if (touchHelper != null) touchHelper.startDrag(viewHolder);
            }
        });
        recyclerView.setAdapter(adapter);

        // Setup Drag & Drop
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // No swipe
            }

            @Override
            public boolean isLongPressDragEnabled() {
                 return false; // Handle only
            }
        };
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        // Buttons
        view.findViewById(R.id.btn_add_new).setOnClickListener(v -> showAddWidgetDialog());
        view.findViewById(R.id.btn_save_close).setOnClickListener(v -> saveAndClose());
        
        View closeBtn = view.findViewById(R.id.btn_close_dialog);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dismiss());
        }

        // Setup Density Spinner
        setupDensitySpinner(view);
    }
    
    private android.widget.Spinner slotSpinner;
    
    private void setupDensitySpinner(View root) {
        slotSpinner = root.findViewById(R.id.spinner_slot_count);
        if (slotSpinner == null) return;
        
        final Integer[] items = new Integer[]{3, 4, 5, 6};
        android.widget.ArrayAdapter<Integer> spinnerAdapter = new android.widget.ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, items);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        slotSpinner.setAdapter(spinnerAdapter);
        
        // Load Pref
        android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        int savedSlots = prefs.getInt("widget_slot_count", 3);
        
        // Find index
        for (int i = 0; i < items.length; i++) {
            if (items[i] == savedSlots) {
                slotSpinner.setSelection(i);
                break;
            }
        }
    }
    
    private ItemTouchHelper touchHelper;

    private void showAddWidgetDialog() {
        if (getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Widget Ekle");

        List<WidgetRegistry.WidgetEntry> available = WidgetRegistry.getAvailableWidgets();
        String[] names = new String[available.size()];
        for (int i = 0; i < available.size(); i++) {
            names[i] = available.get(i).displayName;
        }

        builder.setItems(names, (dialog, which) -> {
            WidgetRegistry.WidgetEntry entry = available.get(which);
            // Use createUniqueWidget to ensure every instance has a unique ID (e.g. music_12345)
            BaseWidget newWidget = WidgetRegistry.createUniqueWidget(getContext(), (OsmandApplication) getContext().getApplicationContext(), entry.typeId);
            if (newWidget != null) {
                // Default Size
                newWidget.setSize(BaseWidget.WidgetSize.SMALL);
                newWidget.setVisible(true);
                
                editingList.add(newWidget);
                adapter.notifyItemInserted(editingList.size() - 1);
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    private void saveAndClose() {
        // Push changes to Manager
        widgetManager.updateVisibleOrder(editingList);
        
        // Save Density Pref
        if (slotSpinner != null && getContext() != null) {
            Integer selected = (Integer) slotSpinner.getSelectedItem();
            if (selected != null) {
                android.preference.PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit().putInt("widget_slot_count", selected).apply();
            }
        }
        
        dismiss();
        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }
}
