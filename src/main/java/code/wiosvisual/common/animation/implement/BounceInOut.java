/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.implement;

import code.wiosvisual.common.animation.Animation;

public class BounceInOut
extends Animation {
    @Override
    public double calculation(double value) {
        double x = value / (double)this.ms;
        double n1 = 7.5625;
        double d1 = 2.75;
        if (x < 0.5) {
            return (1.0 - this.bounceOut(1.0 - 2.0 * x)) / 2.0;
        }
        return (1.0 + this.bounceOut(2.0 * x - 1.0)) / 2.0;
    }

    private double bounceOut(double x) {
        double n1 = 7.5625;
        double d1 = 2.75;
        if (x < 1.0 / d1) {
            return n1 * x * x;
        }
        if (x < 2.0 / d1) {
            return n1 * (x -= 1.5 / d1) * x + 0.75;
        }
        if (x < 2.5 / d1) {
            return n1 * (x -= 2.25 / d1) * x + 0.9375;
        }
        return n1 * (x -= 2.625 / d1) * x + 0.984375;
    }
}
