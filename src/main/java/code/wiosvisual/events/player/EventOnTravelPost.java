/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class EventOnTravelPost
implements Event {
    private Vec3d oldVelocity;

    public Vec3d getOldVelocity() {
        return this.oldVelocity;
    }

    public void setOldVelocity(Vec3d oldVelocity) {
        this.oldVelocity = oldVelocity;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EventOnTravelPost)) {
            return false;
        }
        EventOnTravelPost other = (EventOnTravelPost)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Vec3d this$oldVelocity = this.getOldVelocity();
        Vec3d other$oldVelocity = other.getOldVelocity();
        return !(this$oldVelocity == null ? other$oldVelocity != null : !this$oldVelocity.equals(other$oldVelocity));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EventOnTravelPost;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Vec3d $oldVelocity = this.getOldVelocity();
        result = result * 59 + ($oldVelocity == null ? 43 : $oldVelocity.hashCode());
        return result;
    }

    public String toString() {
        return "EventOnTravelPost(oldVelocity=" + String.valueOf(this.getOldVelocity()) + ")";
    }

    public EventOnTravelPost(Vec3d oldVelocity) {
        this.oldVelocity = oldVelocity;
    }
}
