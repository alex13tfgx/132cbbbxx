/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_10185
 *  net.minecraft.class_1297
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 */
package code.wiosvisual.utils.interactions.simulate;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Simulations
implements QuickImports {
    public static boolean hasPlayerMovement() {
        return Simulations.mc.player.input.getMovementInput().y != 0.0f || Simulations.mc.player.input.getMovementInput().x != 0.0f;
    }

    public static double[] calculateDirection(double distance) {
        return Simulations.calculateDirection(Simulations.mc.player.input.getMovementInput().y, Simulations.mc.player.input.getMovementInput().x, distance);
    }

    public static final boolean moveKeyPressed(int keyNumber) {
        boolean w = Simulations.mc.options.forwardKey.isPressed();
        boolean a = Simulations.mc.options.leftKey.isPressed();
        boolean s = Simulations.mc.options.backKey.isPressed();
        boolean d = Simulations.mc.options.rightKey.isPressed();
        return keyNumber == 0 ? w : (keyNumber == 1 ? a : (keyNumber == 2 ? s : keyNumber == 3 && d));
    }

    public static final boolean w() {
        return Simulations.moveKeyPressed(0);
    }

    public static final boolean a() {
        return Simulations.moveKeyPressed(1);
    }

    public static final boolean s() {
        return Simulations.moveKeyPressed(2);
    }

    public static final boolean d() {
        return Simulations.moveKeyPressed(3);
    }

    public static final float moveYaw(float entityYaw) {
        return entityYaw + (float)(!Simulations.a() || !Simulations.d() || Simulations.w() && Simulations.s() || !Simulations.w() && !Simulations.s() ? (Simulations.w() && Simulations.s() && (!Simulations.a() || !Simulations.d()) && (Simulations.a() || Simulations.d()) ? (Simulations.a() ? -90 : (Simulations.d() ? 90 : 0)) : (Simulations.a() && Simulations.d() && (!Simulations.w() || !Simulations.s()) || Simulations.w() && Simulations.s() && (!Simulations.a() || !Simulations.d()) ? 0 : (!(Simulations.a() || Simulations.d() || Simulations.s()) ? 0 : (Simulations.w() && !Simulations.s() ? 45 : (Simulations.s() && !Simulations.w() ? (!Simulations.a() && !Simulations.d() ? 180 : 135) : (!(!Simulations.w() && !Simulations.s() || Simulations.w() && Simulations.s()) ? 0 : 90))) * (Simulations.a() ? -1 : 1)))) : (Simulations.w() ? 0 : (Simulations.s() ? 180 : 0)));
    }

    public static double[] forward(double d) {
        float f = Simulations.mc.player.input.getMovementInput().y;
        float f2 = Simulations.mc.player.input.getMovementInput().x;
        float f3 = TurnsConnection.INSTANCE.getRotation().getYaw();
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += (float)(f > 0.0f ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += (float)(f > 0.0f ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        double d4 = (double)f * d * d3 + (double)f2 * d * d2;
        double d5 = (double)f * d * d2 - (double)f2 * d * d3;
        return new double[]{d4, d5};
    }

    public static float calculateBodyYaw(float yaw, float prevBodyYaw, double prevX, double prevZ, double currentX, double currentZ, float handSwingProgress) {
        double motionX = currentX - prevX;
        double motionZ = currentZ - prevZ;
        float motionSquared = (float)(motionX * motionX + motionZ * motionZ);
        float bodyYaw = prevBodyYaw;
        float swing = Simulations.mc.player.handSwingProgress;
        if (motionSquared > 0.0025000002f) {
            float movementYaw = (float)MathHelper.atan2((double)motionZ, (double)motionX) * 57.295776f - 90.0f;
            float yawDiff = MathHelper.abs((float)(MathHelper.wrapDegrees((float)yaw) - movementYaw));
            bodyYaw = 95.0f < yawDiff && yawDiff < 265.0f ? movementYaw - 180.0f : movementYaw;
        }
        if (Simulations.mc.player != null && Simulations.mc.player.handSwingProgress - 0.3f > 0.0f) {
            bodyYaw = yaw;
        }
        float deltaYaw = MathHelper.wrapDegrees((float)(bodyYaw - prevBodyYaw));
        bodyYaw = prevBodyYaw + deltaYaw * 0.32f;
        float yawOffsetDiff = MathHelper.wrapDegrees((float)(yaw - bodyYaw));
        float maxHeadRotation = 52.0f;
        if (Math.abs(yawOffsetDiff) > maxHeadRotation) {
            bodyYaw += yawOffsetDiff - (float)MathHelper.sign((double)yawOffsetDiff) * maxHeadRotation;
        }
        return bodyYaw;
    }

    public static double kizdamati() {
        return 1488.0;
    }

    public static String fpsADDS() {
        return null;
    }

    public static double[] calculateDirection(float forward, float sideways, double distance) {
        float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += forward > 0.0f ? -45.0f : 45.0f;
            } else if (sideways < 0.0f) {
                yaw += forward > 0.0f ? 45.0f : -45.0f;
            }
            sideways = 0.0f;
            forward = forward > 0.0f ? 1.0f : -1.0f;
        }
        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = (double)forward * distance * cosYaw + (double)sideways * distance * sinYaw;
        double zMovement = (double)forward * distance * sinYaw - (double)sideways * distance * cosYaw;
        return new double[]{xMovement, zMovement};
    }

    public static double getSpeedSqrt(Entity entity) {
        return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.lastX, entity.lastY, entity.lastZ)));
    }

    public static void setVelocity(double velocity) {
        double[] direction = Simulations.calculateDirection(velocity);
        Objects.requireNonNull(Simulations.mc.player).setVelocity(direction[0], Simulations.mc.player.getVelocity().getY(), direction[1]);
    }

    public static void setVelocity(double velocity, double y) {
        double[] direction = Simulations.calculateDirection(velocity);
        Objects.requireNonNull(Simulations.mc.player).setVelocity(direction[0], y, direction[1]);
    }

    public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
        float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
        double currentYaw = Math.toRadians(MathHelper.wrapDegrees((float)yaw));
        return Math.toDegrees(MathHelper.wrapDegrees((double)((double)optimalYaw - currentYaw)));
    }

    public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
        boolean forwards = input.forward();
        boolean backwards = input.backward();
        boolean left = input.left();
        boolean right = input.right();
        if (dgs >= (double)(-90.0f + deadAngle) && dgs <= (double)(90.0f - deadAngle)) {
            forwards = true;
        } else if (dgs < (double)(-90.0f - deadAngle) || dgs > (double)(90.0f + deadAngle)) {
            backwards = true;
        }
        if (dgs >= (double)(0.0f + deadAngle) && dgs <= (double)(180.0f - deadAngle)) {
            right = true;
        } else if (dgs >= (double)(-180.0f + deadAngle) && dgs <= (double)(0.0f - deadAngle)) {
            left = true;
        }
        return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
        return Simulations.getDirectionalInputForDegrees(input, dgs, 20.0f);
    }

    private Simulations() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
