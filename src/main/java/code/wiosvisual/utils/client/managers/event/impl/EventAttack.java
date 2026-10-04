/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 */
package code.wiosvisual.utils.client.managers.event.impl;

import code.wiosvisual.utils.client.managers.event.impl.EventLayer;
import net.minecraft.entity.Entity;

public class EventAttack
extends EventLayer {
    private Entity entity;
    boolean pre;

    public EventAttack(Entity entity, boolean pre) {
        this.entity = entity;
        this.pre = pre;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public boolean isPre() {
        return this.pre;
    }
}
