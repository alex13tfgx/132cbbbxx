/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.util.math.Vec3d;

public class EventOnMovePost
implements Event {
    private float speed;
    private Vec3d movementInput;

    public float getSpeed() {
        return this.speed;
    }

    public Vec3d getMovementInput() {
        return this.movementInput;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setMovementInput(Vec3d movementInput) {
        this.movementInput = movementInput;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EventOnMovePost)) {
            return false;
        }
        EventOnMovePost other = (EventOnMovePost)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (Float.compare(this.getSpeed(), other.getSpeed()) != 0) {
            return false;
        }
        Vec3d this$movementInput = this.getMovementInput();
        Vec3d other$movementInput = other.getMovementInput();
        return !(this$movementInput == null ? other$movementInput != null : !this$movementInput.equals(other$movementInput));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EventOnMovePost;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + Float.floatToIntBits(this.getSpeed());
        Vec3d $movementInput = this.getMovementInput();
        result = result * 59 + ($movementInput == null ? 43 : $movementInput.hashCode());
        return result;
    }

    public String toString() {
        return "EventOnMovePost(speed=" + this.getSpeed() + ", movementInput=" + String.valueOf(this.getMovementInput()) + ")";
    }

    public EventOnMovePost(float speed, Vec3d movementInput) {
        this.speed = speed;
        this.movementInput = movementInput;
    }
}
