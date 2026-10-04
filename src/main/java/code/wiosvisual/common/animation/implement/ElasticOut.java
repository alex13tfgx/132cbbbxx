/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.implement;

import code.wiosvisual.common.animation.Animation;

public class ElasticOut
extends Animation {
    @Override
    public double calculation(double value) {
        double x = value / (double)this.ms;
        if (x == 0.0) {
            return 0.0;
        }
        if (x == 1.0) {
            return 1.0;
        }
        double c4 = 2.0943951023931953;
        return Math.pow(2.0, -10.0 * x) * Math.sin((x * 10.0 - 0.75) * c4) + 1.0;
    }
}
