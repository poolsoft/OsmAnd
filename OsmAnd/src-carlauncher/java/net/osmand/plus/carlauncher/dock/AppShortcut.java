package net.osmand.plus.carlauncher.dock;

import android.graphics.drawable.Drawable;

/**
 * Uygulama kisayolu model.
 * Dock'ta gosterilecek uygulama bilgisi.
 */
public class AppShortcut {

    private String packageName;
    private String appName;
    private Drawable icon;
    private int order;

    public AppShortcut(String packageName, String appName, Drawable icon, int order) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.order = order;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
