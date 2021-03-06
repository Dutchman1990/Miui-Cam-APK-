package com.android.camera.aosp_porting.animation;

import android.view.animation.Interpolator;

public class CubicEaseInOutInterpolator implements Interpolator {
    public float getInterpolation(float f) {
        f *= 2.0f;
        if (f < 1.0f) {
            return ((0.5f * f) * f) * f;
        }
        f -= 2.0f;
        return (((f * f) * f) + 2.0f) * 0.5f;
    }
}
