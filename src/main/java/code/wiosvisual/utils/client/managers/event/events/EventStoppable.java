/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.event.events;

import code.wiosvisual.utils.client.managers.event.events.Event;

public abstract class EventStoppable
implements Event {
    private boolean stopped;

    protected EventStoppable() {
    }

    public void stop() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return this.stopped;
    }
}
