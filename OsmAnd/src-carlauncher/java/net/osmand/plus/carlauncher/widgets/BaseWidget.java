package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Tum widget'larin base class'i.
 * Her widget bu class'i extend eder.
 */
public abstract class BaseWidget {

    protected String id;
    protected String title;
    protected boolean isVisible;
    protected int order;
    protected Context context;
    protected View rootView;

    public enum WidgetSize {
        SMALL, MEDIUM, LARGE
    }
    protected WidgetSize size = WidgetSize.SMALL;

    private boolean isStarted = false;

    public BaseWidget(@NonNull Context context, @NonNull String id, @NonNull String title) {
        this.context = context;
        this.id = id;
        this.title = title;
        this.isVisible = true;
        this.order = 0;
        this.size = WidgetSize.SMALL;
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
        return id;
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

    public boolean isStarted() {
        return isStarted;
    }

    public WidgetSize getSize() {
        return size;
    }

    public void setSize(WidgetSize size) {
        this.size = size;
    }

    @Nullable
    public View getRootView() {
        return rootView;
    }
}
