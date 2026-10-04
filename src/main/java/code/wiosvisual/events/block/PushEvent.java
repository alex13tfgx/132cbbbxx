/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.block;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;

public class PushEvent
extends EventCancellable {
    private Type type;

    public Type getType() {
        return this.type;
    }

    public PushEvent(Type type) {
        this.type = type;
    }

    public static enum Type {
        COLLISION,
        BLOCK,
        WATER;

    }
}
