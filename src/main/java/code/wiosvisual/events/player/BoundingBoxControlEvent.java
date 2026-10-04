/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_238
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

public class BoundingBoxControlEvent
extends EventCancellable {
    public Box box;
    public Entity entity;

    public Box getBox() {
        return this.box;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public void setBox(Box box) {
        this.box = box;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public BoundingBoxControlEvent(Box box, Entity entity) {
        this.box = box;
        this.entity = entity;
    }
}
