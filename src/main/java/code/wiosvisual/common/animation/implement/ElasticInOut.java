/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.implement;

import code.wiosvisual.common.animation.Animation;

public class ElasticInOut
extends Animation {
    @Override
    public double calculation(double value) {
        double x = value / (double)this.ms;
        if (x == 0.0 || x == 1.0) {
            return x;
        }
        double c5 = 1.3962634015954636;
        return x < 0.5 ? -(Math.pow(2.0, 20.0 * x - 10.0) * Math.sin((20.0 * x - 11.125) * c5)) / 2.0 : Math.pow(2.0, -20.0 * x + 10.0) * Math.sin((20.0 * x - 11.125) * c5) / 2.0 + 1.0;
    }
}
