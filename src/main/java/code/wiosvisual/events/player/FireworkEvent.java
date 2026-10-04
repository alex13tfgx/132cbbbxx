/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class FireworkEvent
implements Event {
    public Vec3d vector;

    public FireworkEvent(Vec3d vector) {
        this.vector = vector;
    }

    public Vec3d getVector() {
        return this.vector;
    }

    public void setVector(Vec3d vector) {
        this.vector = vector;
    }
}
