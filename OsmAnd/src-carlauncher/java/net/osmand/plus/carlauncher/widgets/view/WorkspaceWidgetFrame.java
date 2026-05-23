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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.carlauncher.widgets.BaseWidget;
import net.osmand.plus.carlauncher.widgets.WidgetManager;

/**
 * Premium Duzenleme Modu icin Widget Sarmalayici Sinif.
 * Duzenleme modundayken titreme animasyonu, kenar cizgileri,
 * tasima (Drag & Drop), boyutlandirma (Resize), ayar ve silme butonlari sunar.
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
    private ImageView dragHandle;
    private ImageView resizeHandle;
    private ImageView deleteBtn;
    private ImageView configBtn;

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

        // 1. Drag Handle (Tasima Tutamaci - Orta Bolge)
        dragHandle = new ImageView(context);
        dragHandle.setImageResource(android.R.drawable.ic_menu_directions);
        dragHandle.setColorFilter(0xEEFFFFFF);
        dragHandle.setBackgroundResource(android.R.drawable.alert_light_frame);
        int handleSize = dpToPx(36);
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
        resizeHandle = new ImageView(context);
        resizeHandle.setImageResource(android.R.drawable.ic_menu_crop);
        resizeHandle.setColorFilter(0xFF00E5FF);
        resizeHandle.setBackgroundResource(android.R.drawable.alert_light_frame);
        int resizeSize = dpToPx(28);
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

        // 3. Delete Button (Kapat/Sil Butonu - Sol Ust Kose)
        deleteBtn = new ImageView(context);
        deleteBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        deleteBtn.setColorFilter(0xFFFF3333); // Neon Kirmizi
        deleteBtn.setBackgroundResource(android.R.drawable.alert_light_frame);
        int btnSize = dpToPx(28);
        LayoutParams deleteParams = new LayoutParams(btnSize, btnSize);
        deleteParams.gravity = Gravity.TOP | Gravity.LEFT;
        deleteBtn.setLayoutParams(deleteParams);
        deleteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                widget.setVisible(false);
                WidgetManager.getInstance(getContext()).saveWidgetConfig();
                if (onWidgetsChanged != null) {
                    onWidgetsChanged.run();
                }
                Toast.makeText(getContext(), widget.getTitle() + " kaldirildi.", Toast.LENGTH_SHORT).show();
            }
        });
        overlayContainer.addView(deleteBtn);

        // 4. Config Button (Ayarlar Butonu - Sag Ust Kose)
        configBtn = new ImageView(context);
        configBtn.setImageResource(android.R.drawable.ic_menu_preferences);
        configBtn.setColorFilter(0xFFFFB300); // Premium Sari/Turuncu
        configBtn.setBackgroundResource(android.R.drawable.alert_light_frame);
        LayoutParams configParams = new LayoutParams(btnSize, btnSize);
        configParams.gravity = Gravity.TOP | Gravity.RIGHT;
        configBtn.setLayoutParams(configParams);
        configBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (widget.isConfigurable()) {
                    widget.openConfig(fragmentManager);
                }
            }
        });
        overlayContainer.addView(configBtn);

        addView(overlayContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (editMode) {
            overlayContainer.setVisibility(VISIBLE);
            configBtn.setVisibility(widget.isConfigurable() ? VISIBLE : GONE);
            startShakeAnimation();
        } else {
            overlayContainer.setVisibility(GONE);
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
}
