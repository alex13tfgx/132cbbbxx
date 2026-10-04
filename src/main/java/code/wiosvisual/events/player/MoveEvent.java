/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class MoveEvent
implements Event {
    private Vec3d movement;

    public Vec3d getMovement() {
        return this.movement;
    }

    public void setMovement(Vec3d movement) {
        this.movement = movement;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MoveEvent)) {
            return false;
        }
        MoveEvent other = (MoveEvent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Vec3d this$movement = this.getMovement();
        Vec3d other$movement = other.getMovement();
        return !(this$movement == null ? other$movement != null : !this$movement.equals(other$movement));
    }

    protected boolean canEqual(Object other) {
        return other instanceof MoveEvent;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Vec3d $movement = this.getMovement();
        result = result * 59 + ($movement == null ? 43 : $movement.hashCode());
        return result;
    }

    public String toString() {
        return "MoveEvent(movement=" + String.valueOf(this.getMovement()) + ")";
    }

    public MoveEvent(Vec3d movement) {
        this.movement = movement;
    }
}
