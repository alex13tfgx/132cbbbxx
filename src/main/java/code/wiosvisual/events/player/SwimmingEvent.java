/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.util.math.Vec3d;

public class SwimmingEvent
extends EventCancellable {
    Vec3d vector;

    public void setVector(Vec3d vector) {
        this.vector = vector;
    }

    public Vec3d getVector() {
        return this.vector;
    }

    public SwimmingEvent(Vec3d vector) {
        this.vector = vector;
    }
}
