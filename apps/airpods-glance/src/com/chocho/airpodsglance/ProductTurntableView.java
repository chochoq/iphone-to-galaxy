package com.chocho.airpodsglance;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.widget.ImageView;

/** Starts an animated WebP only while its overlay view is attached. */
public final class ProductTurntableView extends ImageView {
    private final boolean motionEnabled;

    public ProductTurntableView(Context context, int animatedResource, int staticResource,
                                String description) {
        super(context);
        float animatorScale = 1f;
        try {
            animatorScale = Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        } catch (RuntimeException ignored) { }
        motionEnabled = ProductMotionPolicy.shouldAnimate(animatorScale);
        setImageResource(motionEnabled ? animatedResource : staticResource);
        setScaleType(ScaleType.FIT_CENTER);
        setContentDescription(description);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!motionEnabled) return;
        post(new Runnable() {
            @Override public void run() {
                Drawable drawable = getDrawable();
                if (drawable instanceof Animatable) ((Animatable) drawable).start();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        Drawable drawable = getDrawable();
        if (drawable instanceof Animatable) ((Animatable) drawable).stop();
        super.onDetachedFromWindow();
    }
}
