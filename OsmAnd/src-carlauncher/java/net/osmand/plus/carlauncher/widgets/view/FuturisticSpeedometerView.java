package net.osmand.plus.carlauncher.widgets.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class FuturisticSpeedometerView extends View {

    private Paint arcBgPaint;
    private Paint arcProgressPaint;
    private Paint glowPaint;
    private Paint textSpeedPaint;
    private Paint textUnitPaint;
    
    private RectF arcBounds;
    
    private float currentSpeed = 0f;
    private float targetSpeed = 0f;
    private float maxSpeed = 240f;
    
    private ValueAnimator speedAnimator;

    public FuturisticSpeedometerView(Context context) {
        super(context);
        init();
    }

    public FuturisticSpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // BlurMaskFilter cogu cihazda LayerType.SOFTWARE gerektirir (Hardware Acceleration'da bazi grafik hatalarini onlemek icin)
        setLayerType(LAYER_TYPE_SOFTWARE, null); 
        
        arcBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcBgPaint.setStyle(Paint.Style.STROKE);
        arcBgPaint.setStrokeWidth(dpToPx(12));
        arcBgPaint.setColor(Color.parseColor("#1AFFFFFF"));
        arcBgPaint.setStrokeCap(Paint.Cap.ROUND);

        arcProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcProgressPaint.setStyle(Paint.Style.STROKE);
        arcProgressPaint.setStrokeWidth(dpToPx(12));
        arcProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dpToPx(12));
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setMaskFilter(new BlurMaskFilter(dpToPx(16), BlurMaskFilter.Blur.NORMAL));
        
        textSpeedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSpeedPaint.setColor(Color.WHITE);
        textSpeedPaint.setTextAlign(Paint.Align.CENTER);
        textSpeedPaint.setTextSize(dpToPx(72));
        textSpeedPaint.setFakeBoldText(true);

        textUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textUnitPaint.setColor(Color.parseColor("#888888"));
        textUnitPaint.setTextAlign(Paint.Align.CENTER);
        textUnitPaint.setTextSize(dpToPx(18));
        
        arcBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = dpToPx(32);
        arcBounds.set(padding, padding, w - padding, h - padding);
        
        // Neon Mavi/Cyan Glow Efekti (Sweep Gradient)
        int[] colors = {Color.parseColor("#00B4DB"), Color.parseColor("#0083B0"), Color.parseColor("#00B4DB")};
        SweepGradient gradient = new SweepGradient(w / 2f, h / 2f, colors, null);
        arcProgressPaint.setShader(gradient);
        glowPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        
        // Cizim Acilari (Start: 135 derece, Sweep: 270 derece -> Klasik kadran)
        float startAngle = 135f;
        float sweepAngle = 270f;
        
        // 1. Arka Plan Yayi Ciz (Saydam Siyahimsi)
        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, arcBgPaint);
        
        // 2. Ilerleyis Yayi Hesapla
        float progressSweep = (currentSpeed / maxSpeed) * sweepAngle;
        if (progressSweep > sweepAngle) progressSweep = sweepAngle;
        
        if (progressSweep > 0) {
            // Glow Efekti (Parlama icin once arkaya bulanik yayi ciz)
            canvas.drawArc(arcBounds, startAngle, progressSweep, false, glowPaint);
            // Ana Yayi Ciz (Parlamanin ustune net yay)
            canvas.drawArc(arcBounds, startAngle, progressSweep, false, arcProgressPaint);
        }
        
        // 3. Hiz Metni (Ortaya)
        canvas.drawText(String.valueOf((int) currentSpeed), cx, cy + dpToPx(16), textSpeedPaint);
        canvas.drawText("km/h", cx, cy + dpToPx(48), textUnitPaint);
    }

    public void setSpeed(float speed) {
        this.targetSpeed = speed;
        if (speedAnimator != null && speedAnimator.isRunning()) {
            speedAnimator.cancel();
        }
        
        speedAnimator = ValueAnimator.ofFloat(currentSpeed, targetSpeed);
        speedAnimator.setDuration(400); // Akici animasyon hizi
        speedAnimator.setInterpolator(new DecelerateInterpolator());
        speedAnimator.addUpdateListener(animation -> {
            currentSpeed = (float) animation.getAnimatedValue();
            invalidate(); // Her fremede yeniden ciz
        });
        speedAnimator.start();
    }
    
    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}
