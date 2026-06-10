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

    public static final int TYPE_CLASSIC = 0;
    public static final int TYPE_GLOW_PEAK = 1;
    public static final int TYPE_NEON_MODERN = 2;
    private int visualizerType = TYPE_NEON_MODERN;
    private int dominantColor = 0;

    public void setDominantColor(int color) {
        if (color != 0) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            // Eger renk cok donukse doygunlugu (Saturation) yukseltiyoruz (Turkce karakter yok)
            if (hsv[1] > 0.05f) {
                hsv[1] = Math.max(hsv[1], 0.85f);
            }
            // Parlakligi (Value) her zaman en yuksek seviyede tutuyoruz ki parlasin (Turkce karakter yok)
            hsv[2] = Math.max(hsv[2], 0.90f);
            this.dominantColor = Color.HSVToColor(hsv);
        } else {
            this.dominantColor = 0;
        }
        this.mFirst = true; // Force shader recreation
        postInvalidate();
    }

    private int getDarkerColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    private int getLighterColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, Math.round(Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, Math.round(Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, Math.round(Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.argb(a, r, g, b);
    }

    private byte[] mBytes;
    private float[] mPoints;
    private RectF mRect = new RectF();
    private Paint mForePaint = new Paint();
    private Paint mPeakPaint = new Paint();
    private int mSpectrumNum = 48; // Bar count
    private boolean mFirst = true;

    private float[] mPeaks;
    private long[] mPeakTimes;

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

        mPeakPaint.setAntiAlias(true);
        mPeakPaint.setStyle(Paint.Style.FILL);
        mPeakPaint.setColor(Color.WHITE);

        // Son secilen tipi SharedPreferences uzerinden yukluyoruz
        try {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("music_visualizer_prefs", Context.MODE_PRIVATE);
            visualizerType = prefs.getInt("visualizer_type", TYPE_NEON_MODERN);
        } catch (Exception e) {
            visualizerType = TYPE_NEON_MODERN;
        }

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                visualizerType = (visualizerType + 1) % 3;
                try {
                    android.content.SharedPreferences.Editor editor = getContext().getSharedPreferences("music_visualizer_prefs", Context.MODE_PRIVATE).edit();
                    editor.putInt("visualizer_type", visualizerType);
                    editor.apply();
                } catch (Exception e) {
                    // ignore
                }
                mFirst = true; // Paint ayarlari yeniden yapilsin
                invalidate();
            }
        });

        // Golge ve parilti efektlerinin cizilmesi icin yazilimsal katman destegi (Turkce karakter yok)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void updateVisualizer(byte[] fft) {
        if (fft == null) return;
        
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

        mPeakPaint.setColor(dominantColor != 0 ? getLighterColor(dominantColor, 0.5f) : Color.WHITE);

        // Tipine gore gradient olusturuluyor
        if (mFirst) {
            int heightVal = getHeight() > 0 ? getHeight() : 100;
            if (dominantColor != 0) {
                // Alt taraf yari saydam, ust taraf ise canli neon renk (Turkce karakter yok)
                int startColor = Color.argb(38, Color.red(dominantColor), Color.green(dominantColor), Color.blue(dominantColor));
                int endColor = dominantColor;
                int[] colors = {startColor, endColor};
                LinearGradient shader = new LinearGradient(
                        0, heightVal, 0, 0, 
                        colors, null, Shader.TileMode.CLAMP);
                mForePaint.setShader(shader);
                mForePaint.setShadowLayer(15f, 0f, 0f, dominantColor);
            } else if (visualizerType == TYPE_NEON_MODERN) {
                // Neon Modern icin Cyan -> Mavi gecisi (Turkce karakter yok)
                int[] colors = {
                    Color.parseColor("#0044FF"), // Alt kisim: Koyu Mavi (Turkce karakter yok)
                    Color.parseColor("#00FFFF")  // Ust kisim: Parlak Cyan (Turkce karakter yok)
                };
                LinearGradient shader = new LinearGradient(
                        0, heightVal, 0, 0, 
                        colors, null, Shader.TileMode.CLAMP);
                mForePaint.setShader(shader);
                mForePaint.setShadowLayer(15f, 0f, 0f, Color.parseColor("#00FFFF"));
            } else {
                // Klasik ve Glow Peak icin cok renkli gradient (Turkce karakter yok)
                int[] colors = {
                    Color.parseColor("#FF0000"), // Kirmizi (Turkce karakter yok)
                    Color.parseColor("#FFFF00"), // Sari (Turkce karakter yok)
                    Color.parseColor("#00FF00"), // Yesil (Turkce karakter yok)
                    Color.parseColor("#00FFFF"), // Turkuaz
                    Color.parseColor("#0000FF"), // Mavi (Turkce karakter yok)
                    Color.parseColor("#FF00FF")  // Mor
                };
                LinearGradient shader = new LinearGradient(
                        0, heightVal, 0, 0, 
                        colors, null, Shader.TileMode.CLAMP);
                mForePaint.setShader(shader);
                mForePaint.clearShadowLayer();
            }
            mFirst = false;
        }

        int spectrumNum = Math.min(mSpectrumNum, mBytes.length);
        float barWidth = getWidth() / (float) spectrumNum;
        
        // Neon modda barlari daha ince ve aralari acik yapiyoruz
        float gapRatio = (visualizerType == TYPE_NEON_MODERN) ? 0.35f : 0.20f;
        float gap = barWidth * gapRatio;
        float effectiveBarWidth = barWidth - gap;

        // Peak dizilerini kontrol edip ilklendiriyoruz
        if (visualizerType == TYPE_GLOW_PEAK) {
            if (mPeaks == null || mPeaks.length != spectrumNum) {
                mPeaks = new float[spectrumNum];
                mPeakTimes = new long[spectrumNum];
                long now = System.currentTimeMillis();
                for (int i = 0; i < spectrumNum; i++) {
                    mPeaks[i] = 0;
                    mPeakTimes[i] = now;
                }
            }
        }

        long now = System.currentTimeMillis();

        for (int i = 0; i < spectrumNum; i++) {
            byte rfk = mBytes[i];
            float magnitude = (float) (Math.abs(rfk) * 4); 
            float height = (magnitude / 128f) * getHeight();
            if (height > getHeight()) height = getHeight();
            if (height < 0) height = 0;

            float left = i * barWidth + (gap/2);
            float top = getHeight() - height;
            float right = left + effectiveBarWidth;
            float bottom = getHeight();

            if (visualizerType == TYPE_NEON_MODERN) {
                // Yuvarlatilmis barlar (Capsule bar)
                canvas.drawRoundRect(left, top, right, bottom, effectiveBarWidth / 2f, effectiveBarWidth / 2f, mForePaint);
            } else if (visualizerType == TYPE_GLOW_PEAK) {
                // Klasik bar cizimi
                canvas.drawRect(left, top, right, bottom, mForePaint);

                // Peak hesabi ve decay (sonumleme) islemleri
                if (height >= mPeaks[i]) {
                    mPeaks[i] = height;
                    mPeakTimes[i] = now;
                } else {
                    float elapsed = (now - mPeakTimes[i]) / 1000f; // Saniye
                    float decay = elapsed * getHeight() * 0.6f; // Saniyede ekranin yuzde 60'i kadar dussun
                    mPeaks[i] = Math.max(0, mPeaks[i] - decay);
                    mPeakTimes[i] = now;
                }

                // Peak bar cizimi
                float peakTop = getHeight() - mPeaks[i];
                float peakBottom = peakTop + 6f; // 6px kalinlik
                if (peakTop < getHeight() - 6f) {
                    canvas.drawRect(left, peakTop, right, peakBottom, mPeakPaint);
                }
            } else {
                // Klasik Mod
                canvas.drawRect(left, top, right, bottom, mForePaint);
            }
        }
        
        // Glow Peak modunda peak noktalarinin yumusak dususu icin surekli invalidate tetikliyoruz
        if (visualizerType == TYPE_GLOW_PEAK) {
            postInvalidateDelayed(16); // Yaklasik 60 FPS guncelleme
        }
    }
}
