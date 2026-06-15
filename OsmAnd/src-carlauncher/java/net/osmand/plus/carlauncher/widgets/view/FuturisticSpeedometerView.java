package net.osmand.plus.carlauncher.widgets.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class FuturisticSpeedometerView extends View {

    private Paint bgArcPaint;
    private Paint progressArcPaint;
    private Paint glowPaint;
    private Paint tickPaint;
    private Paint textSpeedPaint;
    private Paint textUnitPaint;
    private Paint pulsePaint;
    
    private RectF arcBounds;
    private RectF outerBounds;
    
    private float currentSpeed = 0f;
    private float targetSpeed = 0f;
    private float maxSpeed = 240f;
    
    private ValueAnimator speedAnimator;
    private ValueAnimator pulseAnimator;
    private float pulsePhase = 0f;

    public FuturisticSpeedometerView(Context context) {
        super(context);
        init();
    }

    public FuturisticSpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null); 
        
        bgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgArcPaint.setStyle(Paint.Style.STROKE);
        bgArcPaint.setStrokeWidth(dpToPx(4));
        bgArcPaint.setColor(Color.parseColor("#152036"));
        bgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        progressArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressArcPaint.setStyle(Paint.Style.STROKE);
        progressArcPaint.setStrokeWidth(dpToPx(16));
        progressArcPaint.setStrokeCap(Paint.Cap.ROUND);
        
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dpToPx(16));
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setMaskFilter(new BlurMaskFilter(dpToPx(24), BlurMaskFilter.Blur.NORMAL));
        
        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(dpToPx(2));
        
        textSpeedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSpeedPaint.setColor(Color.WHITE);
        textSpeedPaint.setTextAlign(Paint.Align.CENTER);
        textSpeedPaint.setTextSize(dpToPx(84));
        textSpeedPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        textSpeedPaint.setShadowLayer(dpToPx(10), 0, 0, Color.parseColor("#00B4DB"));

        textUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textUnitPaint.setColor(Color.parseColor("#00E5FF"));
        textUnitPaint.setTextAlign(Paint.Align.CENTER);
        textUnitPaint.setTextSize(dpToPx(20));
        textUnitPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textUnitPaint.setLetterSpacing(0.2f);
        
        arcBounds = new RectF();
        outerBounds = new RectF();

        // Pulsing (Nefes Alma / Radar) efekti
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulsePhase = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = dpToPx(50);
        arcBounds.set(padding, padding, w - padding, h - padding);
        outerBounds.set(padding - dpToPx(30), padding - dpToPx(30), w - padding + dpToPx(30), h - padding + dpToPx(30));
        
        // Renk Gecisi: Cyan -> Mavi -> Mor -> Pembe
        int[] colors = {
            Color.parseColor("#00E5FF"), 
            Color.parseColor("#2979FF"), 
            Color.parseColor("#D500F9"), 
            Color.parseColor("#FF1744")
        };
        float[] positions = {0f, 0.4f, 0.7f, 1f};
        
        // Aciya gore SweepGradient
        SweepGradient gradient = new SweepGradient(w / 2f, h / 2f, colors, positions);
        // Gradient'in baslangic noktasini cevir
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setRotate(135f, w / 2f, h / 2f);
        gradient.setLocalMatrix(matrix);

        progressArcPaint.setShader(gradient);
        glowPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float startAngle = 135f;
        float sweepAngle = 270f;
        
        // 1. Radar Nabiz Efekti (Pulse)
        float maxRadius = outerBounds.width() / 2f;
        float minRadius = arcBounds.width() / 2f - dpToPx(16);
        float currentRadius = minRadius + (maxRadius - minRadius) * pulsePhase;
        int alpha = (int) (255 * (1f - pulsePhase) * 0.4f); // Saydamlasarak genisler
        pulsePaint.setColor(Color.parseColor("#00E5FF"));
        pulsePaint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, currentRadius, pulsePaint);

        // 2. Dis Cizgili Kadran (Tick marks)
        float radius = outerBounds.width() / 2f;
        int tickCount = 60;
        for (int i = 0; i <= tickCount; i++) {
            float angle = startAngle + (i * sweepAngle / tickCount);
            double rad = Math.toRadians(angle);
            float startX, startY, endX, endY;

            if (i % 5 == 0) {
                // Kalin / Uzun Cizgiler
                tickPaint.setStrokeWidth(dpToPx(2.5f));
                tickPaint.setColor(Color.parseColor("#8800E5FF"));
                startX = cx + (float) Math.cos(rad) * (radius - dpToPx(12));
                startY = cy + (float) Math.sin(rad) * (radius - dpToPx(12));
            } else {
                // Ince Cizgiler
                tickPaint.setStrokeWidth(dpToPx(1f));
                tickPaint.setColor(Color.parseColor("#3300E5FF"));
                startX = cx + (float) Math.cos(rad) * (radius - dpToPx(6));
                startY = cy + (float) Math.sin(rad) * (radius - dpToPx(6));
            }
            endX = cx + (float) Math.cos(rad) * radius;
            endY = cy + (float) Math.sin(rad) * radius;
            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }

        // 3. Arka Plan Yayi
        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, bgArcPaint);
        
        // 4. Ilerleyis Yayi (Hiz = 0 iken bile ufak bir mavi isilti)
        float progressSweep = (currentSpeed / maxSpeed) * sweepAngle;
        if (progressSweep < 2f) progressSweep = 2f; // Her zaman hafif bir parlaklik ve renk olsun
        if (progressSweep > sweepAngle) progressSweep = sweepAngle;
        
        canvas.drawArc(arcBounds, startAngle, progressSweep, false, glowPaint);
        canvas.drawArc(arcBounds, startAngle, progressSweep, false, progressArcPaint);
        
        // 5. Metinler
        canvas.drawText(String.valueOf((int) currentSpeed), cx, cy + dpToPx(10), textSpeedPaint);
        canvas.drawText("KM/H", cx, cy + dpToPx(44), textUnitPaint);
    }

    public void setSpeed(float speed) {
        this.targetSpeed = speed;
        if (speedAnimator != null && speedAnimator.isRunning()) {
            speedAnimator.cancel();
        }
        speedAnimator = ValueAnimator.ofFloat(currentSpeed, targetSpeed);
        speedAnimator.setDuration(600);
        speedAnimator.setInterpolator(new android.view.animation.OvershootInterpolator(1.2f));
        speedAnimator.addUpdateListener(animation -> {
            currentSpeed = (float) animation.getAnimatedValue();
            invalidate();
        });
        speedAnimator.start();
    }
    
    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}
