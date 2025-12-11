package net.osmand.plus.carlauncher.overlay;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

/**
 * Overlay window yoneticisi.
 * Uygulamalari harita uzerinde floating window olarak acar.
 */
public class OverlayWindowManager {

    private final Context context;
    private final WindowManager windowManager;
    private View overlayView;
    private boolean isShowing = false;

    public OverlayWindowManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Overlay window goster.
     */
    public void showOverlay(String packageName) {
        if (isShowing) {
            hideOverlay();
        }

        // Overlay layout olustur
        overlayView = createOverlayView(packageName);

        // Window parametreleri
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dpToPx(400), // Genislik
                dpToPx(300), // Yukseklik
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dpToPx(16);
        params.y = dpToPx(100);

        windowManager.addView(overlayView, params);
        isShowing = true;

        // Suruklenebilir yap
        makeDraggable(overlayView, params);
    }

    /**
     * Overlay view olustur.
     */
    private View createOverlayView(String packageName) {
        FrameLayout container = new FrameLayout(context);
        container.setBackgroundColor(0xDD000000);
        container.setPadding(8, 8, 8, 8);

        // Close button
        ImageButton closeButton = new ImageButton(context);
        closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeButton.setBackgroundColor(0x00000000);
        closeButton.setColorFilter(0xFFFFFFFF);

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(48, 48);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeButton.setLayoutParams(closeParams);
        closeButton.setOnClickListener(v -> hideOverlay());

        container.addView(closeButton);

        // TODO: Uygulamayi buraya embed et (WebView veya custom view)
        // Simdilik placeholder

        return container;
    }

    /**
     * Overlay gizle.
     */
    public void hideOverlay() {
        if (isShowing && overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
            isShowing = false;
        }
    }

    /**
     * Suruklenebilir yap.
     */
    private void makeDraggable(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (initialTouchX - event.getRawX());
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(view, params);
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Overlay gosteriliyor mu?
     */
    public boolean isShowing() {
        return isShowing;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
