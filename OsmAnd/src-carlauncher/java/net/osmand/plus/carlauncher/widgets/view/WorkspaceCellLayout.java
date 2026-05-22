package net.osmand.plus.carlauncher.widgets.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Cok Sayfali Premium Workspace icin Ozel 4x4 Izgara Yerlesim Sinifi (CellLayout).
 * GridLayout'in buzusme ve hizalama hatalarini tamamen ortadan kaldirir.
 * Ekran alanini milimetrik olarak 4x4 esit hucreye boler ve widget'lari
 * cellX, cellY, spanX, spanY degerlerine gore piksel hassasiyetinde yerlestirir.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public class WorkspaceCellLayout extends ViewGroup {

    private int marginPx = 0;

    public WorkspaceCellLayout(Context context) {
        super(context);
        init(context);
    }

    public WorkspaceCellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WorkspaceCellLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Hucreler arasi default margin (6dp)
        float density = context.getResources().getDisplayMetrics().density;
        marginPx = Math.round(6 * density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int usableWidth = widthSize - paddingLeft - paddingRight;
        int usableHeight = heightSize - paddingTop - paddingBottom;

        // 4x4 Izgara hucre genislik ve yukseklikleri
        int cellWidth = usableWidth / 4;
        int cellHeight = usableHeight / 4;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                
                // Widget hucre kaplama boyutlari
                int childWidth = cellWidth * lp.spanX - (2 * marginPx);
                int childHeight = cellHeight * lp.spanY - (2 * marginPx);

                // Eksi degerleri onle
                if (childWidth < 0) childWidth = 0;
                if (childHeight < 0) childHeight = 0;

                int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
                int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
                
                child.measure(childWidthSpec, childHeightSpec);
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        
        int usableWidth = (r - l) - paddingLeft - getPaddingRight();
        int usableHeight = (b - t) - paddingTop - getPaddingBottom();

        int cellWidth = usableWidth / 4;
        int cellHeight = usableHeight / 4;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                // Koordinatlari cellX ve cellY hucrelerine gore piksel bazli hesapla
                int left = paddingLeft + (lp.cellX * cellWidth) + marginPx;
                int top = paddingTop + (lp.cellY * cellHeight) + marginPx;
                int right = left + (lp.spanX * cellWidth) - (2 * marginPx);
                int bottom = top + (lp.spanY * cellHeight) - (2 * marginPx);

                child.layout(left, top, right, bottom);
            }
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * WorkspaceCellLayout'a ozel LayoutParams sinifi.
     * cellX, cellY, spanX, spanY koordinatlarini tasir.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int cellX = 0;
        public int cellY = 0;
        public int spanX = 1;
        public int spanY = 1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            if (source instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) source;
                this.cellX = lp.cellX;
                this.cellY = lp.cellY;
                this.spanX = lp.spanX;
                this.spanY = lp.spanY;
            }
        }
    }
}
