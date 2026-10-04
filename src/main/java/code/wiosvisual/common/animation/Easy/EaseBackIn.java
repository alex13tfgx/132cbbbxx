/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.Easy;

import code.wiosvisual.common.animation.Easy.Direction;
import code.wiosvisual.common.animation.Easy.EasyAnimService;

public class EaseBackIn
extends EasyAnimService {
    private final float easeAmount;

    public EaseBackIn(int ms, double endPoint, float easeAmount) {
        super(ms, endPoint);
        this.easeAmount = easeAmount;
    }

    public EaseBackIn(int ms, double endPoint, float easeAmount, Direction direction) {
        super(ms, endPoint, direction);
        this.easeAmount = easeAmount;
    }

    @Override
    protected boolean correctOutput() {
        return true;
    }

    @Override
    protected double getEquation(double x) {
        float shrink = this.easeAmount + 1.0f;
        return Math.max(0.0, 1.0 + (double)shrink * Math.pow(x - 1.0, 3.0) + (double)this.easeAmount * Math.pow(x - 1.0, 2.0));
    }
}
