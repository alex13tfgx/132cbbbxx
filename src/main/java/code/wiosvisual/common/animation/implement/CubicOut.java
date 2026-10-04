/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.implement;

import code.wiosvisual.common.animation.Animation;

public class CubicOut
extends Animation {
    @Override
    public double calculation(double value) {
        double x = value / (double)this.ms;
        return 1.0 - Math.pow(1.0 - x, 3.0);
    }
}
