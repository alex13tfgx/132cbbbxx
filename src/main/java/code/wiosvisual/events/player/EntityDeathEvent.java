/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1282
 *  net.minecraft.class_1297
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;

public class EntityDeathEvent
implements Event {
    private final Entity entity;
    private final DamageSource source;

    public EntityDeathEvent(Entity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public DamageSource getSource() {
        return this.source;
    }
}
