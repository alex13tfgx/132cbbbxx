/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_4587
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public class HeldItemRendererEvent
implements Event {
    public Hand hand;
    public MatrixStack matrix;

    public HeldItemRendererEvent(Hand hand, MatrixStack matrix) {
        this.hand = hand;
        this.matrix = matrix;
    }

    public Hand getHand() {
        return this.hand;
    }

    public MatrixStack getMatrix() {
        return this.matrix;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    public void setMatrix(MatrixStack matrix) {
        this.matrix = matrix;
    }
}
