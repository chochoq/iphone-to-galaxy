package com.chocho.airpodsglance;

/** Pure accessibility policy for decorative product motion. */
public final class ProductMotionPolicy {
    private ProductMotionPolicy() {}

    public static boolean shouldAnimate(float animatorDurationScale) {
        return animatorDurationScale > 0f;
    }
}
