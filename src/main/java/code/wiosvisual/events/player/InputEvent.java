/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_10185
 *  net.minecraft.class_3532
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;

public class InputEvent
extends EventCancellable {
    private PlayerInput input;

    public void setJumping(boolean jump) {
        this.input = new PlayerInput(this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), jump, this.input.sneak(), this.input.sprint());
    }

    public void setDirectional(boolean forward, boolean backward, boolean left, boolean right) {
        this.input = new PlayerInput(forward, backward, left, right, this.input.jump(), this.input.sneak(), this.input.sprint());
    }

    public void setSneak(boolean sneak) {
        this.input = new PlayerInput(this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), this.input.jump(), sneak, this.input.sprint());
    }

    public void inputNone() {
        this.input = new PlayerInput(false, false, false, false, false, false, false);
    }

    public int forward() {
        return this.input.forward() ? 1 : (this.input.backward() ? -1 : 0);
    }

    public float sideways() {
        return this.input.left() ? 1.0f : (this.input.right() ? -1.0f : 0.0f);
    }

    public float getForward() {
        return this.input.forward() ? 1.0f : (this.input.backward() ? -1.0f : 0.0f);
    }

    public float getStrafe() {
        return this.input.left() ? 1.0f : (this.input.right() ? -1.0f : 0.0f);
    }

    public boolean isJump() {
        return this.input.jump();
    }

    public boolean isSneak() {
        return this.input.sneak();
    }

    public void setForward(float forward) {
        boolean forwardKey = forward > 0.0f;
        boolean backwardKey = forward < 0.0f;
        this.input = new PlayerInput(forwardKey, backwardKey, this.input.left(), this.input.right(), this.input.jump(), this.input.sneak(), this.input.sprint());
    }

    public void setStrafe(float strafe) {
        boolean leftKey = strafe > 0.0f;
        boolean rightKey = strafe < 0.0f;
        this.input = new PlayerInput(this.input.forward(), this.input.backward(), leftKey, rightKey, this.input.jump(), this.input.sneak(), this.input.sprint());
    }

    public void setJump(boolean jump) {
        this.setJumping(jump);
    }

    public void correctMovement(float correctionYaw, float directionYaw) {
        float forward = this.getForward();
        float sideways = this.getStrafe();
        if (forward == 0.0f && sideways == 0.0f) {
            return;
        }
        double angle = MathHelper.wrapDegrees((double)Math.toDegrees(InputEvent.direction(directionYaw, forward, sideways)));
        float closestForward = 0.0f;
        float closestSideways = 0.0f;
        float closestDifference = Float.MAX_VALUE;
        for (float predictedForward = -1.0f; predictedForward <= 1.0f; predictedForward += 1.0f) {
            for (float predictedSideways = -1.0f; predictedSideways <= 1.0f; predictedSideways += 1.0f) {
                double predictedAngle;
                float difference;
                if (predictedForward == 0.0f && predictedSideways == 0.0f || !((difference = Math.abs(MathHelper.wrapDegrees((float)((float)(angle - (predictedAngle = MathHelper.wrapDegrees((double)Math.toDegrees(InputEvent.direction(correctionYaw, predictedForward, predictedSideways))))))))) < closestDifference)) continue;
                closestDifference = difference;
                closestForward = predictedForward;
                closestSideways = predictedSideways;
            }
        }
        this.setForward(closestForward);
        this.setStrafe(closestSideways);
    }

    private static double direction(float yaw, float movementForward, float movementSideways) {
        if (movementForward < 0.0f) {
            yaw += 180.0f;
        }
        float forward = 1.0f;
        if (movementForward < 0.0f) {
            forward = -0.5f;
        } else if (movementForward > 0.0f) {
            forward = 0.5f;
        }
        if (movementSideways > 0.0f) {
            yaw -= 90.0f * forward;
        }
        if (movementSideways < 0.0f) {
            yaw += 90.0f * forward;
        }
        return Math.toRadians(yaw);
    }

    public PlayerInput getInput() {
        return this.input;
    }

    public void setInput(PlayerInput input) {
        this.input = input;
    }

    public InputEvent(PlayerInput input) {
        this.input = input;
    }
}
