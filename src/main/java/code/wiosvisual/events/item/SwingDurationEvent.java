/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.item;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;

public class SwingDurationEvent
extends EventCancellable {
    private float animation;

    public float getAnimation() {
        return this.animation;
    }

    public void setAnimation(float animation) {
        this.animation = animation;
    }
}
