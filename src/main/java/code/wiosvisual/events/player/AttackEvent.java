/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.entity.Entity;

public class AttackEvent
extends EventCancellable {
    private Entity entity;

    public AttackEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
}
