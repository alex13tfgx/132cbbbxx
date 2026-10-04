/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;

public class DeathScreenEvent
implements Event {
    private int ticksSinceDeath;

    public int getTicksSinceDeath() {
        return this.ticksSinceDeath;
    }

    public DeathScreenEvent(int ticksSinceDeath) {
        this.ticksSinceDeath = ticksSinceDeath;
    }
}
