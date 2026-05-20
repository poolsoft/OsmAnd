package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import net.osmand.plus.R;
import net.osmand.plus.carlauncher.CarLauncherSettings;

/**
 * Ekranda suzulen tasinabilir yardimci buton (Floating Button) yoneticisi.
 * Uygulama arka plandayken on plana ceker, on plandayken ise acilir menu gosterir.
 */
public class CarFloatingButtonManager {

    private static CarFloatingButtonManager instance;
    private final Context context;
    private final WindowManager windowManager;
    
    private FrameLayout floatingView;
    private WindowManager.LayoutParams params;
    private boolean isAdded = false;
    private boolean isAppInForeground = false;

    // Surukleme durumlari
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private boolean isDragging = false;
    private long touchStartTime;

    private CarFloatingButtonManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    public static synchronized CarFloatingButtonManager getInstance(Context context) {
        if (instance == null) {
            instance = new CarFloatingButtonManager(context);
        }
        return instance;
    }

    public void setAppInForeground(boolean foreground) {
        this.isAppInForeground = foreground;
    }

    public void updateButtonState() {
        CarLauncherSettings settings = new CarLauncherSettings(context);
        boolean enabled = settings.isFloatingButtonEnabled();

        if (enabled) {
            showButton();
        } else {
            hideButton();
        }
    }

    public void showButton() {
        if (isAdded) return;

        // Overlay izni kontrolu (Android M ve uzeri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                return; // İzin yoksa sessizce cik
            }
        }

        try {
            createFloatingView();

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            params = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );

            // Ekranin sag orta kisminda baslat
            params.gravity = Gravity.TOP | Gravity.START;
            
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            params.x = screenWidth - dpToPx(70);
            params.y = screenHeight / 2 - dpToPx(26);

            floatingView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            touchStartTime = System.currentTimeMillis();
                            isDragging = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float deltaX = event.getRawX() - initialTouchX;
                            float deltaY = event.getRawY() - initialTouchY;
                            if (Math.abs(deltaX) > 15 || Math.abs(deltaY) > 15) {
                                isDragging = true;
                            }
                            params.x = (int) (initialX + deltaX);
                            params.y = (int) (initialY + deltaY);
                            
                            // Ekran sinirlarindan tasmasini onle
                            if (params.x < 0) params.x = 0;
                            if (params.y < 0) params.y = 0;

                            if (isAdded) {
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (!isDragging || (System.currentTimeMillis() - touchStartTime) < 200) {
                                onButtonClicked();
                            }
                            return true;
                    }
                    return false;
                }
            });

            windowManager.addView(floatingView, params);
            isAdded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideButton() {
        if (!isAdded || floatingView == null) return;
        try {
            windowManager.removeView(floatingView);
            isAdded = false;
            floatingView = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFloatingView() {
        floatingView = new FrameLayout(context);
        int size = dpToPx(52);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        floatingView.setLayoutParams(lp);

        // Arka plan: Premium koyu daire ve mavi kenarlik
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xDD181824); // Yari transparan premium koyu lacivert
        bg.setStroke(dpToPx(2), 0xFF3D63FF); // Modern mavi kenarlik
        floatingView.setBackground(bg);

        // İkon: Ortalanmis dashboard grid
        ImageView icon = new ImageView(context);
        int padding = dpToPx(14);
        icon.setPadding(padding, padding, padding, padding);
        icon.setImageResource(R.drawable.dashboard_grid);
        icon.setColorFilter(0xFFFFFFFF);
        
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        iconLp.gravity = Gravity.CENTER;
        floatingView.addView(icon, iconLp);
    }

    private void onButtonClicked() {
        if (!isAppInForeground) {
            // Arka plandaysa: Uygulamayi en on plana getir
            Intent intent = new Intent(context, net.osmand.plus.activities.MapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        } else {
            // On plandaysa: PopupMenu ac
            showOverlayMenu();
        }
    }

    private void showOverlayMenu() {
        if (floatingView == null) return;
        
        // PopupMenu AppCompat temali bir baglam gerektirir
        Context themeContext = new android.view.ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_InputMethod);
        PopupMenu popup = new PopupMenu(themeContext, floatingView);
        
        popup.getMenu().add(0, 1, 0, "Gorunumu Degistir (Buyuk/Kucuk Panel)");
        popup.getMenu().add(0, 2, 1, "Masaustu Modu (Desktop)");
        popup.getMenu().add(0, 3, 2, "Ayarlar");

        popup.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent();
            switch (item.getItemId()) {
                case 1:
                    intent.setAction("net.osmand.carlauncher.ACTION_LAYOUT_TOGGLE");
                    context.sendBroadcast(intent);
                    return true;
                case 2:
                    intent.setAction("net.osmand.carlauncher.ACTION_DESKTOP_TOGGLE");
                    context.sendBroadcast(intent);
                    return true;
                case 3:
                    intent.setAction("net.osmand.carlauncher.ACTION_OPEN_SETTINGS");
                    context.sendBroadcast(intent);
                    return true;
            }
            return false;
        });

        popup.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
