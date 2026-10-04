/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;

public class SprintEvent
implements Event {
    private boolean sprinting;

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SprintEvent)) {
            return false;
        }
        SprintEvent other = (SprintEvent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        return this.isSprinting() == other.isSprinting();
    }

    protected boolean canEqual(Object other) {
        return other instanceof SprintEvent;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isSprinting() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "SprintEvent(sprinting=" + this.isSprinting() + ")";
    }

    public SprintEvent(boolean sprinting) {
        this.sprinting = sprinting;
    }
}
