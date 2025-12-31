package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class MusicVisualizerView extends View {

    private byte[] mBytes;
    private float[] mPoints;
    private RectF mRect = new RectF();
    private Paint mForePaint = new Paint();
    private int mSpectrumNum = 48; // Bar count
    private boolean mFirst = true;

    public MusicVisualizerView(Context context) {
        super(context);
        init();
    }

    public MusicVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MusicVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBytes = null;
        mForePaint.setStrokeWidth(8f);
        mForePaint.setAntiAlias(true);
        mForePaint.setStyle(Paint.Style.FILL);
    }

    public void updateVisualizer(byte[] fft) {
        if (fft == null) return;
        
        // FFT data comes as real and imaginary parts. We calculate magnitude.
        // We only need the first half (0 to Size/2)
        // mBytes = fft; 
        
        byte[] model = new byte[fft.length / 2 + 1];
        model[0] = (byte) Math.abs(fft[0]);
        for (int i = 2, j = 1; j < mSpectrumNum; ) {
            if (i >= fft.length) break;
            
            model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
            i += 2;
            j++;
        }
        mBytes = model;
        invalidate();
    }

    public void clear() {
        mBytes = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        mRect.set(0, 0, getWidth(), getHeight());

        // Create Gradient if first time
        if (mFirst) {
            // Colors from image: Red (Bottom) -> Orange -> Yellow -> Green -> Blue -> Purple (Top)
            // Or Vertical Bars where Color changes with Height?
            // Image shows vertical bars where bottom is red, top is blue? No, usually horizontal range.
            // Let's do a vertical linear gradient.
            
            int[] colors = {
                Color.parseColor("#FF0000"), // Red
                Color.parseColor("#FFFF00"), // Yellow
                Color.parseColor("#00FF00"), // Green
                Color.parseColor("#00FFFF"), // Cyan
                Color.parseColor("#0000FF"), // Blue
                Color.parseColor("#FF00FF")  // Magenta
            };
            // Bottom to Top
            LinearGradient shader = new LinearGradient(
                    0, getHeight(), 0, 0, 
                    colors, null, Shader.TileMode.CLAMP);
            mForePaint.setShader(shader);
            mFirst = false;
        }

        int spectrumNum = Math.min(mSpectrumNum, mBytes.length);
        float barWidth = getWidth() / (float) spectrumNum;
        float gap = barWidth * 0.2f; // 20% gap
        float effectiveBarWidth = barWidth - gap;

        for (int i = 0; i < spectrumNum; i++) {
            byte rfk = mBytes[i];
            // Normalize byte (-128..127) to 0..255 then scale to height
            // Actually FFT magnitude usually small numbers.
            // Need boost.
            float magnitude = (float) (Math.abs(rfk) * 4); 
            
            float dbValue = (float) (10 * Math.log10(magnitude)); // Decibels?
            // Simple linear scaling for visualizer often looks better for UI
            
            float height = (magnitude / 128f) * getHeight();
            if (height > getHeight()) height = getHeight();
            if (height < 0) height = 0;

            float left = i * barWidth + (gap/2);
            float top = getHeight() - height;
            float right = left + effectiveBarWidth;
            float bottom = getHeight();

            canvas.drawRect(left, top, right, bottom, mForePaint);
        }
    }
}
