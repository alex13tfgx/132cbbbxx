/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.implement;

import code.wiosvisual.common.animation.Animation;

public class SineInOut
extends Animation {
    @Override
    public double calculation(double value) {
        double x = value / (double)this.ms;
        return -(Math.cos(Math.PI * x) - 1.0) / 2.0;
    }
}
