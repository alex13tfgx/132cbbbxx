/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;

public class EventNoSlow
extends EventCancellable {
    public boolean start;

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EventNoSlow)) {
            return false;
        }
        EventNoSlow other = (EventNoSlow)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return this.isStart() == other.isStart();
    }

    protected boolean canEqual(Object other) {
        return other instanceof EventNoSlow;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        result = result * 59 + (this.isStart() ? 79 : 97);
        return result;
    }

    public boolean isStart() {
        return this.start;
    }

    public String toString() {
        return "EventNoSlow(start=" + this.isStart() + ")";
    }

    public EventNoSlow(boolean start) {
        this.start = start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }
}
