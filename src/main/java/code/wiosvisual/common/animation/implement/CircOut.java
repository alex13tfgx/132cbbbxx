/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.implement;

import code.wiosvisual.common.animation.Animation;

public class CircOut
extends Animation {
    @Override
    public double calculation(double value) {
        double x = value / (double)this.ms;
        return Math.sqrt(1.0 - Math.pow(x - 1.0, 2.0));
    }
}
