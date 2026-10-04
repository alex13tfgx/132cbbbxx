/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.keyboard;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;

public class MouseRotationEvent
extends EventCancellable {
    float cursorDeltaX;
    float cursorDeltaY;

    public float getCursorDeltaX() {
        return this.cursorDeltaX;
    }

    public float getCursorDeltaY() {
        return this.cursorDeltaY;
    }

    public void setCursorDeltaX(float cursorDeltaX) {
        this.cursorDeltaX = cursorDeltaX;
    }

    public void setCursorDeltaY(float cursorDeltaY) {
        this.cursorDeltaY = cursorDeltaY;
    }

    public MouseRotationEvent(float cursorDeltaX, float cursorDeltaY) {
        this.cursorDeltaX = cursorDeltaX;
        this.cursorDeltaY = cursorDeltaY;
    }
}
