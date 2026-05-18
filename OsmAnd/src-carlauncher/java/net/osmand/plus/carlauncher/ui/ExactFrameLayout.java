package net.osmand.plus.carlauncher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * DrawerLayout'un ConstraintLayout icinde AT_MOST/UNSPECIFIED ile olculup 
 * java.lang.IllegalArgumentException: DrawerLayout must be measured with MeasureSpec.EXACTLY
 * hatasi vermesini onleyen ozel FrameLayout sinifi.
 */
public class ExactFrameLayout extends FrameLayout {

    public ExactFrameLayout(Context context) {
        super(context);
    }

    public ExactFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExactFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // Eger ConstraintLayout ara olcum adiminda EXACTLY disinda bir mod verirse,
        // olculeri EXACTLY moduna zorlayarak DrawerLayout'un cokmesini engelliyoruz.
        if (widthMode != MeasureSpec.EXACTLY) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
