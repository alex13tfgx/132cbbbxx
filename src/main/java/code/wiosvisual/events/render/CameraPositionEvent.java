/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 */
package code.wiosvisual.events.render;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class CameraPositionEvent
implements Event {
    private Vec3d pos;

    public Vec3d getPos() {
        return this.pos;
    }

    public void setPos(Vec3d pos) {
        this.pos = pos;
    }

    public CameraPositionEvent(Vec3d pos) {
        this.pos = pos;
    }
}
