/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4587
 */
package code.wiosvisual.events.render;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.client.util.math.MatrixStack;

public class WorldRenderEvent
implements Event {
    private MatrixStack stack;
    private float partialTicks;

    public WorldRenderEvent(MatrixStack stack, float partialTicks) {
        this.stack = stack;
        this.partialTicks = partialTicks;
    }

    public MatrixStack getStack() {
        return this.stack;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }
}
