package net.osmand.plus.carlauncher.widgets.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;

/**
 * Premium Duzenleme Modu icin Widget Sarmalayici Sinif.
 * Duzenleme modundayken titreme animasyonu, kenar cizgileri,
 * tasima (Drag & Drop), boyutlandirma (Resize), ayar ve silme butonlari sunar.
 * Butun kontrol butonlari Canvas ile jilet keskinliginde programatik cizilmistir.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class WorkspaceWidgetFrame extends FrameLayout {

    private final BaseWidget widget;
    private final WorkspaceCellLayout parentLayout;
    private final FragmentManager fragmentManager;
    private final Runnable onWidgetsChanged;

    private boolean isEditMode = false;
    private ObjectAnimator shakeAnimator;

    private FrameLayout overlayContainer;
    private DragHandleView dragHandle;
    private ResizeHandleView resizeHandle;
    private DeleteButtonView deleteBtn;
    private ConfigButtonView configBtn;
    private DoneButtonView doneBtn;

    private Paint borderPaint;
    private RectF borderRect;

    public WorkspaceWidgetFrame(@NonNull Context context, 
                                @NonNull BaseWidget widget, 
                                @NonNull WorkspaceCellLayout parentLayout,
                                @NonNull FragmentManager fragmentManager,
                                @NonNull Runnable onWidgetsChanged) {
        super(context);
        this.widget = widget;
        this.parentLayout = parentLayout;
        this.fragmentManager = fragmentManager;
        this.onWidgetsChanged = onWidgetsChanged;
        
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        // Kenar cizimi icin Paint nesnesi (Neon Mavi Kesikli)
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(2));
        borderPaint.setColor(0xFF00E5FF); // Neon Turkuaz/Mavi
        borderPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(6), dpToPx(4)}, 0));

        borderRect = new RectF();

        // Edit Arayuzu Butonlari ve Tutamaclari
        overlayContainer = new FrameLayout(context);
        overlayContainer.setClipChildren(false);
        overlayContainer.setClipToPadding(false);
        overlayContainer.setVisibility(GONE);
        overlayContainer.setClickable(true); // Dokunmalari asil widget'a gecirmeyip yutar
        overlayContainer.setFocusable(true);

        // 1. Drag Handle (Tasima Tutamaci - Orta Bolge)
        dragHandle = new DragHandleView(context);
        int handleSize = dpToPx(48); // Genis dokunma alani
        LayoutParams dragParams = new LayoutParams(handleSize, handleSize);
        dragParams.gravity = Gravity.CENTER;
        dragHandle.setLayoutParams(dragParams);
        dragHandle.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ClipData data = ClipData.newPlainText("widget_id", widget.getId());
                    DragShadowBuilder shadowBuilder = new DragShadowBuilder(WorkspaceWidgetFrame.this);
                    startDragAndDrop(data, shadowBuilder, WorkspaceWidgetFrame.this, 0);
                    
                    // Surukleme baslarken kendi gorunurlugumuzu hafif yari saydam yapalim
                    setAlpha(0.3f);
                    return true;
                }
                return false;
            }
        });
        overlayContainer.addView(dragHandle);

        // 2. Resize Handle (Boyutlandirma Tutamaci - Sag Alt Kose)
        resizeHandle = new ResizeHandleView(context);
        int resizeSize = dpToPx(44); // Genis dokunma alani
        LayoutParams resizeParams = new LayoutParams(resizeSize, resizeSize);
        resizeParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        resizeHandle.setLayoutParams(resizeParams);
        resizeHandle.setOnTouchListener(new OnTouchListener() {
            private float initialX, initialY;
            private int initialSpanX, initialSpanY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getRawX();
                        initialY = event.getRawY();
                        initialSpanX = widget.getSpanX();
                        initialSpanY = widget.getSpanY();
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialX;
                        float deltaY = event.getRawY() - initialY;

                        int cellWidth = parentLayout.getWidth() / 4;
                        int cellHeight = parentLayout.getHeight() / 4;

                        if (cellWidth > 0 && cellHeight > 0) {
                            int gridDeltaX = Math.round(deltaX / cellWidth);
                            int gridDeltaY = Math.round(deltaY / cellHeight);

                            int newSpanX = Math.max(1, Math.min(4 - widget.getCellX(), initialSpanX + gridDeltaX));
                            int newSpanY = Math.max(1, Math.min(4 - widget.getCellY(), initialSpanY + gridDeltaY));

                            if (newSpanX != widget.getSpanX() || newSpanY != widget.getSpanY()) {
                                if (canWidgetFitAt(widget.getPageIndex(), widget.getCellX(), widget.getCellY(), newSpanX, newSpanY)) {
                                    widget.setSpanX(newSpanX);
                                    widget.setSpanY(newSpanY);

                                    // WorkspaceCellLayout LayoutParams guncelle
                                    WorkspaceCellLayout.LayoutParams lp = (WorkspaceCellLayout.LayoutParams) getLayoutParams();
                                    lp.spanX = newSpanX;
                                    lp.spanY = newSpanY;
                                    setLayoutParams(lp);
                                    parentLayout.requestLayout();
                                }
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        WidgetManager.getInstance(getContext()).saveWidgetConfig();
                        if (onWidgetsChanged != null) {
                            onWidgetsChanged.run();
                        }
                        return true;
                }
                return false;
            }
        });
        overlayContainer.addView(resizeHandle);

        // Ust Sag Kose Kontrol Paneli (Ayarlar ve Silme Yan Yana)
        LinearLayout topControls = new LinearLayout(context);
        topControls.setOrientation(LinearLayout.HORIZONTAL);
        LayoutParams topParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topParams.gravity = Gravity.TOP | Gravity.RIGHT;
        topParams.topMargin = dpToPx(4);
        topParams.rightMargin = dpToPx(4);
        topControls.setLayoutParams(topParams);

        int btnSize = dpToPx(44); // Genis dokunma alani

        // 3. Config Button (Ayarlar Butonu)
        configBtn = new ConfigButtonView(context);
        LinearLayout.LayoutParams configLp = new LinearLayout.LayoutParams(btnSize, btnSize);
        configLp.rightMargin = dpToPx(4);
        configBtn.setLayoutParams(configLp);
        configBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (widget.isConfigurable()) {
                    widget.openConfig(fragmentManager);
                }
            }
        });
        topControls.addView(configBtn);

        // 4. Delete Button (Kapat/Sil Butonu)
        deleteBtn = new DeleteButtonView(context);
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(btnSize, btnSize);
        deleteBtn.setLayoutParams(deleteLp);
        deleteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                widget.setVisible(false);
                WidgetManager.getInstance(getContext()).saveWidgetConfig();
                
                // Sayfadaki diger gorunur widget'lari kontrol et (Son widget silindiyse Duzenleme Modunu kapat)
                java.util.List<BaseWidget> list = WidgetManager.getInstance(getContext()).getAllWidgets();
                boolean hasVisibleWidgetsOnPage = false;
                for (BaseWidget w : list) {
                    if (w.isVisible() && w.getPageIndex() == widget.getPageIndex()) {
                        hasVisibleWidgetsOnPage = true;
                        break;
                    }
                }
                if (!hasVisibleWidgetsOnPage) {
                    net.osmand.plus.carlauncher.widgets.WorkspacePageAdapter.isEditMode = false;
                }
                
                if (onWidgetsChanged != null) {
                    onWidgetsChanged.run();
                }
                Toast.makeText(getContext(), widget.getTitle() + " kaldirildi.", Toast.LENGTH_SHORT).show();
            }
        });
        topControls.addView(deleteBtn);

        // 5. Done Button (Duzenlemeyi Bitir / Tamam Butonu - Sol Alt Kose)
        doneBtn = new DoneButtonView(context);
        int doneSize = dpToPx(44); // Genis dokunma alani
        LayoutParams doneParams = new LayoutParams(doneSize, doneSize);
        doneParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        doneBtn.setLayoutParams(doneParams);
        doneBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.post(() -> {
                    net.osmand.plus.carlauncher.widgets.WorkspacePageAdapter.exitEditMode();
                    Toast.makeText(getContext(), "Duzenlemeler Kaydedildi.", Toast.LENGTH_SHORT).show();
                });
            }
        });
        overlayContainer.addView(doneBtn);

        overlayContainer.addView(topControls);
        addView(overlayContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public BaseWidget getWidget() {
        return widget;
    }

    public void setWidgetView(View widgetView) {
        // Temizlik: overlayContainer disindaki eski widget view'larini kaldirir
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child != overlayContainer) {
                removeViewAt(i);
            }
        }
        // Widget gorunumunu 0. indekse ekle (en alta)
        addView(widgetView, 0, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // overlayContainer'in her zaman en ustte oldugundan emin ol
        overlayContainer.bringToFront();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (editMode) {
            overlayContainer.setVisibility(VISIBLE);
            // Sistem widget'lari yuksek elevasyona sahip olabildigi icin overlayContainer'i yukseltelim
            overlayContainer.setElevation(dpToPx(30));
            overlayContainer.bringToFront(); // Butonlari her zaman en ust katmana tasir
            configBtn.setVisibility(widget.isConfigurable() ? VISIBLE : GONE);
            if (doneBtn != null) {
                doneBtn.setVisibility(VISIBLE);
            }
            startShakeAnimation();
        } else {
            overlayContainer.setVisibility(GONE);
            overlayContainer.setElevation(0);
            stopShakeAnimation();
            setAlpha(1.0f);
        }
        invalidate();
    }

    private void startShakeAnimation() {
        if (shakeAnimator == null) {
            shakeAnimator = ObjectAnimator.ofFloat(this, "rotation", -0.6f, 0.6f);
            shakeAnimator.setDuration(160);
            shakeAnimator.setRepeatCount(ValueAnimator.INFINITE);
            shakeAnimator.setRepeatMode(ValueAnimator.REVERSE);
        }
        if (!shakeAnimator.isRunning()) {
            shakeAnimator.start();
        }
    }

    private void stopShakeAnimation() {
        if (shakeAnimator != null && shakeAnimator.isRunning()) {
            shakeAnimator.cancel();
            setRotation(0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isEditMode) {
            // Neon cerceve ciz
            int padding = dpToPx(2);
            borderRect.set(padding, padding, getWidth() - padding, getHeight() - padding);
            canvas.drawRoundRect(borderRect, dpToPx(8), dpToPx(8), borderPaint);
        }
    }

    private boolean canWidgetFitAt(int pageIndex, int cellX, int cellY, int spanX, int spanY) {
        if (cellX < 0 || cellX + spanX > 4 || cellY < 0 || cellY + spanY > 4) {
            return false;
        }

        java.util.List<BaseWidget> list = WidgetManager.getInstance(getContext()).getAllWidgets();
        boolean[][] occupied = new boolean[4][4];
        for (BaseWidget w : list) {
            if (w != widget && w.isVisible() && w.getPageIndex() == pageIndex) {
                int cx = w.getCellX();
                int cy = w.getCellY();
                int sx = w.getSpanX();
                int sy = w.getSpanY();
                if (cx >= 0 && cx + sx <= 4 && cy >= 0 && cy + sy <= 4) {
                    for (int x = cx; x < cx + sx; x++) {
                        for (int y = cy; y < cy + sy; y++) {
                            occupied[x][y] = true;
                        }
                    }
                }
            }
        }

        for (int x = cellX; x < cellX + spanX; x++) {
            for (int y = cellY; y < cellY + spanY; y++) {
                if (occupied[x][y]) {
                    return false;
                }
            }
        }
        return true;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ==========================================
    // PREMIUM CANVAS TABANLI CUSTOM VIEW SINIFLARI
    // ==========================================

    /**
     * Kirmizi Carpi (X) Kapat/Sil Butonu.
     */
    private static class DeleteButtonView extends View {
        private final Paint bgPaint;
        private final Paint iconPaint;
        private final float density;

        public DeleteButtonView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            setClickable(true);
            setFocusable(true);

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(0xEE222222); // Koyu gri yari saydam arkaplan

            iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeWidth(2.5f * density);
            iconPaint.setColor(0xFFFF3333); // Neon Kirmizi
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = 15f * density; // 30dp cap

            // Daire arkaplani ciz
            canvas.drawCircle(cx, cy, radius, bgPaint);

            // Carpi isaretini ciz (X)
            float size = 5f * density;
            canvas.drawLine(cx - size, cy - size, cx + size, cy + size, iconPaint);
            canvas.drawLine(cx + size, cy - size, cx - size, cy + size, iconPaint);
        }
    }

    /**
     * Premium Modern Ayarlar (⚙️) Butonu.
     */
    private static class ConfigButtonView extends View {
        private final Paint bgPaint;
        private final Paint iconPaint;
        private final float density;

        public ConfigButtonView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            setClickable(true);
            setFocusable(true);

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(0xEE222222);

            iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeWidth(2f * density);
            iconPaint.setColor(0xFFFFB300); // Premium Sari/Turuncu
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = 15f * density;

            canvas.drawCircle(cx, cy, radius, bgPaint);

            // Modern Ayar/Slider simgesi (2 yatay cizgi ve uzerinde kucuk daireler)
            float lineY1 = cy - 4f * density;
            float lineY2 = cy + 4f * density;
            float startX = cx - 6f * density;
            float endX = cx + 6f * density;

            // Cizgiler
            canvas.drawLine(startX, lineY1, endX, lineY1, iconPaint);
            canvas.drawLine(startX, lineY2, endX, lineY2, iconPaint);

            // Kaydirici daireler
            Paint circlePaint = new Paint(iconPaint);
            circlePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx - 2f * density, lineY1, 2.5f * density, circlePaint);
            canvas.drawCircle(cx + 2f * density, lineY2, 2.5f * density, circlePaint);
        }
    }

    /**
     * Launcher3 Tarzi 6 Noktali Tasima (Drag) Butonu.
     */
    private static class DragHandleView extends View {
        private final Paint bgPaint;
        private final Paint dotPaint;
        private final float density;

        public DragHandleView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            setClickable(true);
            setFocusable(true);

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(0xEE222222);

            dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = 18f * density; // 36dp cap

            canvas.drawCircle(cx, cy, radius, bgPaint);

            // 6 surukleme noktasini cizelim
            float dotRadius = 2.2f * density;
            float spacingX = 5f * density;
            float spacingY = 5f * density;

            float x1 = cx - spacingX / 2f;
            float x2 = cx + spacingX / 2f;
            float y1 = cy - spacingY;
            float y2 = cy;
            float y3 = cy + spacingY;

            canvas.drawCircle(x1, y1, dotRadius, dotPaint);
            canvas.drawCircle(x2, y1, dotRadius, dotPaint);
            canvas.drawCircle(x1, y2, dotRadius, dotPaint);
            canvas.drawCircle(x2, y2, dotRadius, dotPaint);
            canvas.drawCircle(x1, y3, dotRadius, dotPaint);
            canvas.drawCircle(x2, y3, dotRadius, dotPaint);
        }
    }

    /**
     * Neon Mavi Resize Handle.
     */
    private static class ResizeHandleView extends View {
        private final Paint bgPaint;
        private final Paint iconPaint;
        private final float density;

        public ResizeHandleView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            setClickable(true);
            setFocusable(true);

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(0xEE222222);

            iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeWidth(2.2f * density);
            iconPaint.setColor(0xFF00E5FF); // Neon Mavi
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = 15f * density; // 30dp cap

            canvas.drawCircle(cx, cy, radius, bgPaint);

            // Resize L seklinde kucuk ok cizimi
            float size = 4.5f * density;
            canvas.drawLine(cx - size, cy + size, cx + size, cy + size, iconPaint); // Yatay
            canvas.drawLine(cx + size, cy - size, cx + size, cy + size, iconPaint); // Dikey
            canvas.drawLine(cx - size, cy - size, cx + size, cy + size, iconPaint); // Kosegen
        }
    }

    /**
     * Yesil Onay (Done) Butonu.
     * Tiklandiginda edit modundan cikar.
     * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
     */
    private static class DoneButtonView extends View {
        private final Paint bgPaint;
        private final Paint iconPaint;
        private final float density;
        private boolean isPressed = false;

        public DoneButtonView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            setClickable(true);
            setFocusable(true);

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(0xEE222222); // Koyu gri yari saydam arkaplan

            iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeWidth(3f * density); // Daha net ve kalin cizgi
            iconPaint.setColor(0xFF00E676); // Neon Yesil
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isPressed = true;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (isPressed) {
                        isPressed = false;
                        invalidate();
                        performClick(); // Tiklama olayini tetikle
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    isPressed = false;
                    invalidate();
                    return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = 16f * density; // 32dp cap

            // Basildiginda renk ve boyut degisimi (Premium geri bildirim)
            if (isPressed) {
                bgPaint.setColor(0xFF00E676); // Arkaplan yesil olur
                iconPaint.setColor(0xFF222222); // Simge koyu gri olur
                radius = 13.5f * density; // Kuculme efekti
            } else {
                bgPaint.setColor(0xEE222222);
                iconPaint.setColor(0xFF00E676);
            }

            // Daire arkaplani ciz
            canvas.drawCircle(cx, cy, radius, bgPaint);

            // Onay (Checkmark) isaretini ciz
            float x1 = cx - 5f * density;
            float y1 = cy - 0.5f * density;
            float x2 = cx - 1.5f * density;
            float y2 = cy + 3.5f * density;
            float x3 = cx + 5.5f * density;
            float y3 = cy - 3.5f * density;

            canvas.drawLine(x1, y1, x2, y2, iconPaint);
            canvas.drawLine(x2, y2, x3, y3, iconPaint);
        }
    }
}
