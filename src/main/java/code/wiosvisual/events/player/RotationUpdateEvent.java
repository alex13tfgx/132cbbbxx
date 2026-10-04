/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;

public class RotationUpdateEvent
implements Event {
    byte type;

    public byte getType() {
        return this.type;
    }

    public RotationUpdateEvent(byte type) {
        this.type = type;
    }
}
