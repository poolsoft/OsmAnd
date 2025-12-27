package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * Tum widget'larin base class'i.
 * Her widget bu class'i extend eder.
 */
public abstract class BaseWidget {

    public enum WidgetSize {
        SMALL,  // 1 Column (25%)
        MEDIUM, // 2 Columns (50%)
        LARGE   // 4 Columns (100%)
    }

    protected String instanceId; // Unique UUID
    protected String type;       // "music", "map", etc.
    protected String title;
    protected boolean isVisible;
    protected int order;
    protected WidgetSize size = WidgetSize.LARGE; // Default to Large
    
    protected Context context;
    protected View rootView;

    private boolean isStarted = false;

    public BaseWidget(@NonNull Context context, @NonNull String type, @NonNull String title) {
        this.context = context;
        this.type = type; // The type identifier (e.g. "music")
        this.instanceId = UUID.randomUUID().toString(); // Generate unique instance ID
        this.title = title;
        this.isVisible = true;
        this.order = 0;
    }

    /**
     * Widget view'ini olustur.
     */
    @NonNull
    public abstract View createView();

    /**
     * Widget verilerini guncelle.
     */
    public abstract void update();

    /**
     * Widget baslat (listeners, observers ekle).
     */
    public void onStart() {
        isStarted = true;
    }

    /**
     * Widget durdur (listeners, observers kaldir).
     */
    public void onStop() {
        isStarted = false;
    }

    /**
     * Widget destroy.
     */
    public void onDestroy() {
        if (rootView != null) {
            rootView = null;
        }
    }

    // Getters & Setters

    public String getId() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
        if (rootView != null) {
            rootView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
    
    public WidgetSize getSize() {
        return size;
    }

    public void setSize(WidgetSize size) {
        this.size = size;
    }

    public boolean isStarted() {
        return isStarted;
    }

    @Nullable
    public View getRootView() {
        return rootView;
    }
}
