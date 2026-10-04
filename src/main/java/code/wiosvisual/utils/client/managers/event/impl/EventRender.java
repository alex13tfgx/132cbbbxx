/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_10017
 *  net.minecraft.class_1297
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 *  net.minecraft.class_9779
 */
package code.wiosvisual.utils.client.managers.event.impl;

import code.wiosvisual.utils.client.managers.event.impl.EventLayer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public class EventRender
extends EventLayer {

    public static class RenderLabelsEvent<T extends Entity, S extends EntityRenderState>
    extends EventRender {
        S state;

        public S getState() {
            return this.state;
        }

        public RenderLabelsEvent(S state) {
            this.state = state;
        }
    }

    public static class AfterHud
    extends EventRender {
        DrawContext context;
        RenderTickCounter tickCounter;

        public DrawContext getContext() {
            return this.context;
        }

        public RenderTickCounter getTickCounter() {
            return this.tickCounter;
        }

        public AfterHud(DrawContext context, RenderTickCounter tickCounter) {
            this.context = context;
            this.tickCounter = tickCounter;
        }
    }

    public static class BeforeHud
    extends EventRender {
        DrawContext context;
        RenderTickCounter tickCounter;

        public DrawContext getContext() {
            return this.context;
        }

        public RenderTickCounter getTickCounter() {
            return this.tickCounter;
        }

        public BeforeHud(DrawContext context, RenderTickCounter tickCounter) {
            this.context = context;
            this.tickCounter = tickCounter;
        }
    }

    public static class AfterHand
    extends EventRender {
        MatrixStack stack;
        RenderTickCounter tickCounter;

        public MatrixStack getStack() {
            return this.stack;
        }

        public RenderTickCounter getTickCounter() {
            return this.tickCounter;
        }

        public AfterHand(MatrixStack stack, RenderTickCounter tickCounter) {
            this.stack = stack;
            this.tickCounter = tickCounter;
        }
    }
}
