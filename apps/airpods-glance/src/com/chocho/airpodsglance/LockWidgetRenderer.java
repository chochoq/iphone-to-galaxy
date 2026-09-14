package com.chocho.airpodsglance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.widget.RemoteViews;

/** Original monochrome artwork for the Samsung tiny lock-screen host. No copied assets. */
public final class LockWidgetRenderer {
    private LockWidgetRenderer() {}

    public static RemoteViews render(Context context, LockWidgetState state, float width, float height) {
        float density = context.getResources().getDisplayMetrics().density;
        int pixels = Math.max(56, Math.min(384, Math.round(Math.min(width, height) * density)));
        Bitmap bitmap = artwork(state, pixels, context.getResources().getConfiguration().fontScale);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.airpods_lock_widget);
        views.setImageViewBitmap(R.id.lock_widget_image, bitmap);
        String description = state.description();
        if (state.observed > 0) description += ", " + android.text.format.DateFormat.format("M월 d일 HH:mm", state.observed) + " 확인";
        description += ". 눌러서 다음 잔량 보기: " + state.nextLabel();
        views.setContentDescription(R.id.lock_widget_image, description);
        return views;
    }

    public static Bitmap artwork(LockWidgetState state, int pixels, float fontScale) {
        pixels = Math.max(56, Math.min(384, pixels));
        if (!Float.isFinite(fontScale) || fontScale <= 0) fontScale = 1;
        Bitmap bitmap = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(pixels / 56f, pixels / 56f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0x55ffffff);
        canvas.drawCircle(28, 20.5f, 17.5f, paint);
        if (state.percent != null && state.percent > 0) {
            paint.setColor(Color.WHITE);
            canvas.drawArc(new RectF(10.5f, 3, 45.5f, 38), -90, state.percent * 3.6f, false, paint);
        }
        partIcon(canvas, paint, state.part);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(14 * Math.max(1, Math.min(1.3f, fontScale)));
        fit(paint, state.value(), 35);
        float lineHeight = paint.descent() - paint.ascent();
        if (lineHeight > 14) paint.setTextSize(paint.getTextSize() * 14 / lineHeight);
        float valueWidth = paint.measureText(state.value());
        float valueX = state.percent == null ? 28 : 33;
        canvas.drawText(state.value(), valueX, 48 - (paint.ascent() + paint.descent()) / 2, paint);
        if (state.percent != null) {
            float x = valueX - valueWidth / 2 - 6;
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(.85f); paint.setColor(0xbbffffff);
            canvas.drawCircle(x, 48, 3.2f, paint);
            canvas.drawLine(x, 46, x, 48, paint); canvas.drawLine(x, 48, x + 1.5f, 49, paint);
        }
        return bitmap;
    }

    private static void partIcon(Canvas canvas, Paint paint, int part) {
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.7f); paint.setStrokeCap(Paint.Cap.ROUND);
        if (part == 2) {
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(new RectF(17, 13, 39, 28), 4.5f, 4.5f, paint);
            canvas.drawLine(17.5f, 18.5f, 38.5f, 18.5f, paint);
            paint.setStyle(Paint.Style.FILL); canvas.drawCircle(28, 23, 1, paint);
        } else {
            for (int side = 0; side < 2; side++) {
                canvas.save(); if (side == 1) canvas.scale(-1, 1, 28, 20.5f);
                paint.setStyle(Paint.Style.FILL); paint.setColor(side == part ? Color.WHITE : 0x55ffffff);
                // Original outline with an open inner notch, so it works on transparent backgrounds.
                Path bud = new Path();
                bud.moveTo(24, 13); bud.cubicTo(20, 9, 15, 11, 15, 15);
                bud.cubicTo(15, 18, 17, 20, 20.5f, 20); bud.lineTo(20.5f, 28);
                bud.cubicTo(20.5f, 31, 24.5f, 31, 24.5f, 28); bud.lineTo(24.5f, 16);
                bud.cubicTo(24.5f, 15, 23, 15, 22, 16); bud.lineTo(21, 17);
                bud.cubicTo(19, 17, 18, 15.5f, 19, 14); bud.cubicTo(20, 12.8f, 22, 13, 23, 14);
                bud.close(); canvas.drawPath(bud, paint); canvas.restore();
            }
        }
    }

    private static void fit(Paint paint, String text, float width) {
        float measured = paint.measureText(text);
        if (measured > width) paint.setTextSize(paint.getTextSize() * width / measured);
    }
}
