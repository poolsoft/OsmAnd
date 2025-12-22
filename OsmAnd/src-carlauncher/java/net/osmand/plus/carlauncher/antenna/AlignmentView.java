package net.osmand.plus.carlauncher.antenna;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class AlignmentView extends View {

    private float targetAzimuth = 0;
    private float targetPitch = 0;

    private float currentAzimuth = 0;
    private float currentPitch = 0;

    private final Paint paintCircle;
    private final Paint paintTarget;
    private final Paint paintCurrent;
    private final Paint paintText;
    private final Paint paintLeveler;

    public AlignmentView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setStrokeWidth(8f);
        paintCircle.setColor(Color.WHITE);

        paintTarget = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTarget.setStyle(Paint.Style.ITEM_FIT); // STROKE/FILL
        paintTarget.setStrokeWidth(12f);
        paintTarget.setColor(Color.GREEN);
        paintTarget.setStrokeCap(Paint.Cap.ROUND);

        paintCurrent = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCurrent.setStyle(Paint.Style.STROKE);
        paintCurrent.setStrokeWidth(10f);
        paintCurrent.setColor(Color.RED);
        paintCurrent.setStrokeCap(Paint.Cap.ROUND);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(60f);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintLeveler = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLeveler.setColor(Color.CYAN);
        paintLeveler.setStrokeWidth(20f);
        paintLeveler.setAlpha(150);
    }

    public void setTarget(float azimuth, float pitch) {
        this.targetAzimuth = azimuth;
        this.targetPitch = pitch;
        invalidate();
    }

    public void setCurrent(float azimuth, float pitch) {
        this.currentAzimuth = azimuth;
        this.currentPitch = pitch;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int radius = Math.min(cx, cy) - 50;

        // 1. Compass (Azimuth)
        // Outer Circle
        paintCircle.setColor(Color.DKGRAY);
        canvas.drawCircle(cx, cy, radius, paintCircle);

        // Compass Ticks (N, E, S, W)
        // Adjust canvas rotation based on CURRENT Azimuth so "North" moves
        // If currentAzimuth is 0 (North), North is up.
        // If currentAzimuth is 90 (East), North is Left.

        canvas.save();
        canvas.rotate(-currentAzimuth, cx, cy);

        // Draw North Tick
        paintText.setColor(Color.RED);
        canvas.drawText("N", cx, cy - radius + 60, paintText);

        // Target Azimuth Indicator (Green Line on the circle)
        // Target is relative to North. Since we rotated North to match reality, we
        // rotate Target relative to that.
        // Actually simplest is: Draw everything relative to North, then rotate whole
        // canvas by -currentAzimuth.

        // Draw Target Line
        canvas.save();
        canvas.rotate(targetAzimuth, cx, cy);
        canvas.drawLine(cx, cy - radius + 20, cx, cy - radius - 20, paintTarget); // Little mark
        canvas.drawCircle(cx, cy - radius, 10, paintTarget);
        canvas.restore();

        // Draw Fixed "Phone Forward" Indicator (Red Arrow pointing UP)
        // But wait, we rotated the canvas by -currentAzimuth. So "Up" on screen is
        // actually "Current Azimuth" in the world.
        // So the "Phone Forward" is always "Up" on the screen.
        // If we rotate the world, "Up" on screen is static.

        canvas.restore(); // Back to screen coordinates

        // Phone Heading Indicator (Always Up)
        paintCurrent.setColor(Color.YELLOW);
        canvas.drawLine(cx, cy + 20, cx, cy - radius + 20, paintCurrent);

        // 2. Leveler (Pitch)
        // Vertical bar on the right
        int barX = getWidth() - 50;
        int barHeight = getHeight() / 2;
        int barTop = cy - barHeight / 2;
        int barBottom = cy + barHeight / 2;

        paintLeveler.setColor(Color.DKGRAY);
        paintLeveler.setStrokeWidth(40f);
        canvas.drawLine(barX, barTop, barX, barBottom, paintLeveler);

        // Center (0 degrees horizon)
        paintLeveler.setColor(Color.WHITE);
        paintLeveler.setStrokeWidth(5f);
        canvas.drawLine(barX - 20, cy, barX + 20, cy, paintLeveler);

        // Target Pitch
        float targetY = cy - (targetPitch * 5); // Scale: 1 degree = 5 pixels (approx)
        paintTarget.setStyle(Paint.Style.FILL);
        canvas.drawCircle(barX, targetY, 15, paintTarget);

        // Current Pitch
        float currentY = cy - (currentPitch * 5);
        paintCurrent.setStyle(Paint.Style.FILL);
        paintCurrent.setColor(Color.RED);
        canvas.drawCircle(barX, currentY, 15, paintCurrent);

        // Alignment Feedback
        // Check if aligned
        float azDiff = Math.abs(currentAzimuth - targetAzimuth);
        if (azDiff > 180)
            azDiff = 360 - azDiff;

        float pitchDiff = Math.abs(currentPitch - targetPitch);

        boolean aligned = azDiff < 5 && pitchDiff < 5;

        if (aligned) {
            paintText.setColor(Color.GREEN);
            canvas.drawText("HİZALANDI", cx, cy, paintText);
        }
    }
}
