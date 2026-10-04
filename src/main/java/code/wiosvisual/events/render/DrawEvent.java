/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_332
 */
package code.wiosvisual.events.render;

import code.wiosvisual.utils.client.managers.event.events.Event;
import code.wiosvisual.utils.display.render.draw.DrawEngine;
import net.minecraft.client.gui.DrawContext;

public class DrawEvent
implements Event {
    private DrawContext drawContext;
    private DrawEngine drawEngine;
    private float partialTicks;

    public DrawContext getDrawContext() {
        return this.drawContext;
    }

    public DrawEngine getDrawEngine() {
        return this.drawEngine;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public DrawEvent(DrawContext drawContext, DrawEngine drawEngine, float partialTicks) {
        this.drawContext = drawContext;
        this.drawEngine = drawEngine;
        this.partialTicks = partialTicks;
    }
}
