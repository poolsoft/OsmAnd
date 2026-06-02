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

    // Jest ve Uzun Basim Durumlari (Türkçe karakter yok)
    private final android.os.Handler gestureHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable longClickRunnable;
    private boolean isLongClickTriggered = false;

    // Custom Menu Overlay
    private FrameLayout menuOverlayView;
    private WindowManager.LayoutParams menuParams;

    private boolean isFullScreenMap = false;
    private boolean isInPipMode = false;

    private final android.content.BroadcastReceiver assistantReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("net.osmand.carlauncher.ACTION_SHOW_ASSISTANT_MENU".equals(action)) {
                showOverlayMenuFromDock();
            } else if ("net.osmand.carlauncher.ACTION_LAYOUT_TOGGLE".equals(action)) {
                // Layout degistiginde buton gosterim durumunu guncelle (Turkce karakter yok)
                gestureHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateButtonState();
                    }
                }, 150);
            }
        }
    };

    private CarFloatingButtonManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);

        // Alıcı kaydı (Türkçe karakter yok)
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction("net.osmand.carlauncher.ACTION_SHOW_ASSISTANT_MENU");
        filter.addAction("net.osmand.carlauncher.ACTION_LAYOUT_TOGGLE");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.context.registerReceiver(assistantReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            this.context.registerReceiver(assistantReceiver, filter);
        }

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this.context)
                .registerReceiver(assistantReceiver, filter);
    }

    public static synchronized CarFloatingButtonManager getInstance(Context context) {
        if (instance == null) {
            instance = new CarFloatingButtonManager(context);
        }
        return instance;
    }

    public void setAppInForeground(boolean foreground) {
        this.isAppInForeground = foreground;
        updateButtonState();
    }

    public void setInPipMode(boolean pip) {
        this.isInPipMode = pip;
        updateButtonState();
    }

    public void setFullScreenMap(boolean fullScreen) {
        this.isFullScreenMap = fullScreen;
        updateButtonState();
    }

    public void updateButtonState() {
        CarLauncherSettings settings = new CarLauncherSettings(context);
        boolean enabled = settings.isFloatingButtonEnabled();

        if (enabled && !isInPipMode) {
            // Akıllı Görünürlük Kontrolü ( Landscape + Foreground + Split panelde GIZLE ) (Turkce karakter yok)
            boolean isLandscape = context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            if (isAppInForeground && isLandscape && !isFullScreenMap) {
                hideButton();
            } else {
                showButton();
            }
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
                            isLongClickTriggered = false;

                            // 500ms basili tutuldugunda uzun basim tetiklenir (parmak kaldirmayi beklemez)
                            longClickRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (!isDragging) {
                                        isLongClickTriggered = true;
                                        onButtonLongClicked();
                                    }
                                }
                            };
                            gestureHandler.postDelayed(longClickRunnable, 500);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float deltaX = event.getRawX() - initialTouchX;
                            float deltaY = event.getRawY() - initialTouchY;
                            if (Math.abs(deltaX) > 15 || Math.abs(deltaY) > 15) {
                                isDragging = true;
                                gestureHandler.removeCallbacks(longClickRunnable);
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
                            gestureHandler.removeCallbacks(longClickRunnable);
                            if (!isDragging) {
                                if (!isLongClickTriggered) {
                                    onButtonClicked();
                                }
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
        hideCustomOverlayMenu();
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
            bringAppToForeground();
        } else {
            // On planda tek tiklandiginda: Desktop modu toggle yapar (Kisa Basim)
            Intent intent = new Intent("net.osmand.carlauncher.ACTION_DESKTOP_TOGGLE");
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
            
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(new Intent("net.osmand.carlauncher.ACTION_DESKTOP_TOGGLE"));
        }
    }

    private void onButtonLongClicked() {
        if (!isAppInForeground) {
            bringAppToForeground();
        } else {
            // On planda uzun basildiginda: Custom popup menu ac
            showOverlayMenu();
        }
    }

    private void bringAppToForeground() {
        Intent intent = new Intent(context, net.osmand.plus.activities.MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    private void showOverlayMenu() {
        if (floatingView == null) return;
        
        if (menuOverlayView != null) {
            hideCustomOverlayMenu();
            return;
        }

        // Custom FrameLayout menu karti
        menuOverlayView = new FrameLayout(context);
        
        // Premium koyu tema: #111115 arka plan, #3D63FF neon mavi cerceve
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(0xF4111115); // Koyu premium renk
        bg.setStroke(dpToPx(1), 0xFF3D63FF); // Modern mavi kenarlik
        bg.setCornerRadius(dpToPx(12));
        menuOverlayView.setBackground(bg);
        
        int padding = dpToPx(8);
        menuOverlayView.setPadding(padding, padding, padding, padding);

        android.widget.LinearLayout content = new android.widget.LinearLayout(context);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        menuOverlayView.addView(content, contentLp);

        // Menü elemanları (Türkçe karakter yok!)
        addMenuItem(content, "Gorunumu Degistir", "net.osmand.carlauncher.ACTION_LAYOUT_TOGGLE");
        addMenuItem(content, "Masaustu Modu (Desktop)", "net.osmand.carlauncher.ACTION_DESKTOP_TOGGLE");
        addMenuItem(content, "Car Launcher Ayarlari", "net.osmand.carlauncher.ACTION_OPEN_SETTINGS");

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        menuParams = new WindowManager.LayoutParams(
                dpToPx(240),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.TOP | Gravity.START;

        // Butonun yaninda konumlandir
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (params.x > screenWidth / 2) {
            // Buton sagda ise: menuyu sola ac
            menuParams.x = params.x - dpToPx(250);
        } else {
            // Buton solda ise: menuyu saga ac
            menuParams.x = params.x + dpToPx(60);
        }
        menuParams.y = params.y;

        // Disari dokunuldugunda kapanmasi icin touch listener
        menuOverlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    hideCustomOverlayMenu();
                    return true;
                }
                return false;
            }
        });

        try {
            windowManager.addView(menuOverlayView, menuParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showOverlayMenuFromDock() {
        if (menuOverlayView != null) {
            hideCustomOverlayMenu();
            return;
        }

        // Custom FrameLayout menu karti
        menuOverlayView = new FrameLayout(context);
        
        // Premium koyu tema: #111115 arka plan, #3D63FF neon mavi cerceve
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(0xF4111115); // Koyu premium renk
        bg.setStroke(dpToPx(1), 0xFF3D63FF); // Modern mavi kenarlik
        bg.setCornerRadius(dpToPx(12));
        menuOverlayView.setBackground(bg);
        
        int padding = dpToPx(8);
        menuOverlayView.setPadding(padding, padding, padding, padding);

        android.widget.LinearLayout content = new android.widget.LinearLayout(context);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        menuOverlayView.addView(content, contentLp);

        // Menü elemanları (Türkçe karakter yok!)
        addMenuItem(content, "Gorunumu Degistir", "net.osmand.carlauncher.ACTION_LAYOUT_TOGGLE");
        addMenuItem(content, "Masaustu Modu (Desktop)", "net.osmand.carlauncher.ACTION_DESKTOP_TOGGLE");
        addMenuItem(content, "Car Launcher Ayarlari", "net.osmand.carlauncher.ACTION_OPEN_SETTINGS");

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        menuParams = new WindowManager.LayoutParams(
                dpToPx(240),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        
        // Dock sag altta oldugu icin menuyu de sag altta konumlandiriyoruz
        menuParams.gravity = Gravity.BOTTOM | Gravity.END;
        menuParams.x = dpToPx(20);
        menuParams.y = dpToPx(70);

        // Disari dokunuldugunda kapanmasi icin touch listener
        menuOverlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    hideCustomOverlayMenu();
                    return true;
                }
                return false;
            }
        });

        try {
            windowManager.addView(menuOverlayView, menuParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMenuItem(android.widget.LinearLayout container, String title, final String action) {
        final android.widget.TextView tv = new android.widget.TextView(context);
        tv.setText(title);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        tv.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        
        final GradientDrawable itemBg = new GradientDrawable();
        itemBg.setShape(GradientDrawable.RECTANGLE);
        itemBg.setCornerRadius(dpToPx(8));
        itemBg.setColor(0x00000000);
        tv.setBackground(itemBg);

        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        itemBg.setColor(0x223D63FF); // Parmak basildiginda mavi hover rengi
                        tv.setBackground(itemBg);
                        break;
                    case MotionEvent.ACTION_UP:
                        itemBg.setColor(0x00000000);
                        tv.setBackground(itemBg);
                        
                        // Broadcast yayini gonder
                        Intent intent = new Intent(action);
                        intent.setPackage(context.getPackageName());
                        context.sendBroadcast(intent);
                        
                        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(new Intent(action));

                        hideCustomOverlayMenu();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        itemBg.setColor(0x00000000);
                        tv.setBackground(itemBg);
                        break;
                }
                return true;
            }
        });

        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dpToPx(2), 0, dpToPx(2));
        container.addView(tv, lp);
    }

    private void hideCustomOverlayMenu() {
        if (menuOverlayView != null) {
            try {
                windowManager.removeView(menuOverlayView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            menuOverlayView = null;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
