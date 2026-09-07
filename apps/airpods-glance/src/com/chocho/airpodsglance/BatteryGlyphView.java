package com.chocho.airpodsglance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Small iOS-like battery outline whose fill reflects a real component value. */
public final class BatteryGlyphView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final BatteryComponent component;
    private final boolean charging;
    private final int trackColor;

    public BatteryGlyphView(Context context, BatteryComponent component,
                            boolean charging, int trackColor) {
        super(context);
        this.component = component;
        this.charging = charging;
        this.trackColor = trackColor;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(dp(29), widthMeasureSpec),
                resolveSize(dp(14), heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = dpFloat(1.4f);
        float right = getWidth() - dpFloat(3.5f);
        RectF shell = new RectF(stroke, stroke, right - stroke, getHeight() - stroke);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(trackColor);
        canvas.drawRoundRect(shell, dpFloat(2.6f), dpFloat(2.6f), paint);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(right, getHeight() * 0.34f,
                        getWidth(), getHeight() * 0.66f),
                dpFloat(1.2f), dpFloat(1.2f), paint);

        if (component == null || component.percent == null) return;
        float innerLeft = shell.left + dpFloat(2f);
        float innerTop = shell.top + dpFloat(2f);
        float innerRight = shell.right - dpFloat(2f);
        float fillRight = innerLeft + (innerRight - innerLeft) * component.percent / 100f;
        paint.setColor(Color.rgb(52, 199, 89));
        canvas.drawRoundRect(new RectF(innerLeft, innerTop,
                        Math.max(innerLeft + dpFloat(1.5f), fillRight),
                        shell.bottom - dpFloat(2f)),
                dpFloat(1.5f), dpFloat(1.5f), paint);

        if (charging) {
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpFloat(1.15f));
            paint.setStrokeCap(Paint.Cap.SQUARE);
            float centerX = (shell.left + shell.right) / 2f;
            float centerY = getHeight() / 2f;
            android.graphics.Path bolt = new android.graphics.Path();
            bolt.moveTo(centerX + dpFloat(1f), centerY - dpFloat(4f));
            bolt.lineTo(centerX - dpFloat(2f), centerY);
            bolt.lineTo(centerX + dpFloat(1f), centerY);
            bolt.lineTo(centerX - dpFloat(1f), centerY + dpFloat(4f));
            canvas.drawPath(bolt, paint);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dpFloat(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
