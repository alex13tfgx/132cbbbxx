/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.render;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;

public class EntityColorEvent
extends EventCancellable {
    private int color;

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public EntityColorEvent(int color) {
        this.color = color;
    }
}
