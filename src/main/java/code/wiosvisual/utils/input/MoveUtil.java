/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_10185
 *  net.minecraft.class_1294
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 */
package code.wiosvisual.utils.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public class MoveUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static double getMotion() {
        if (MoveUtil.mc.player == null) {
            return 0.0;
        }
        Vec3d velocity = MoveUtil.mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    public static void setMotion(double speed) {
        if (MoveUtil.mc.player == null) {
            return;
        }
        float forward = MoveUtil.mc.player.input.getMovementInput().y;
        float sideways = MoveUtil.mc.player.input.getMovementInput().x;
        float yaw = MoveUtil.mc.player.getYaw();
        if (forward == 0.0f && sideways == 0.0f) {
            MoveUtil.mc.player.setVelocity(0.0, MoveUtil.mc.player.getVelocity().y, 0.0);
            return;
        }
        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw -= (float)(forward > 0.0f ? 45 : -45);
            } else if (sideways < 0.0f) {
                yaw += (float)(forward > 0.0f ? 45 : -45);
            }
            sideways = 0.0f;
            forward = forward > 0.0f ? 1.0f : -1.0f;
        }
        double rad = Math.toRadians(yaw + 90.0f);
        double motionX = (double)forward * speed * Math.cos(rad) + (double)sideways * speed * Math.sin(rad);
        double motionZ = (double)forward * speed * Math.sin(rad) - (double)sideways * speed * Math.cos(rad);
        MoveUtil.mc.player.setVelocity(motionX, MoveUtil.mc.player.getVelocity().y, motionZ);
    }

    public static void strafe() {
        MoveUtil.strafe(MoveUtil.getMotion());
    }

    public static void strafe(double speed) {
        if (MoveUtil.mc.player == null) {
            return;
        }
        float forward = MoveUtil.mc.player.input.getMovementInput().y;
        float sideways = MoveUtil.mc.player.input.getMovementInput().x;
        float yaw = MoveUtil.mc.player.getYaw();
        if (forward == 0.0f && sideways == 0.0f) {
            MoveUtil.mc.player.setVelocity(0.0, MoveUtil.mc.player.getVelocity().y, 0.0);
            return;
        }
        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw -= (float)(forward > 0.0f ? 45 : -45);
            } else if (sideways < 0.0f) {
                yaw += (float)(forward > 0.0f ? 45 : -45);
            }
            sideways = 0.0f;
            forward = forward > 0.0f ? 1.0f : -1.0f;
        }
        double rad = Math.toRadians(yaw + 90.0f);
        MoveUtil.mc.player.setVelocity((double)forward * speed * Math.cos(rad) + (double)sideways * speed * Math.sin(rad), MoveUtil.mc.player.getVelocity().y, (double)forward * speed * Math.sin(rad) - (double)sideways * speed * Math.cos(rad));
    }

    public static boolean isMoving() {
        if (MoveUtil.mc.player == null) {
            return false;
        }
        return MoveUtil.mc.player.input.getMovementInput().y != 0.0f || MoveUtil.mc.player.input.getMovementInput().x != 0.0f;
    }

    public static double getDirection() {
        if (MoveUtil.mc.player == null) {
            return 0.0;
        }
        float forward = MoveUtil.mc.player.input.getMovementInput().y;
        float sideways = MoveUtil.mc.player.input.getMovementInput().x;
        float yaw = MoveUtil.mc.player.getYaw();
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        float strafe = 1.0f;
        if (forward < 0.0f) {
            strafe = -0.5f;
        } else if (forward > 0.0f) {
            strafe = 0.5f;
        }
        if (sideways > 0.0f) {
            yaw -= 90.0f * strafe;
        }
        if (sideways < 0.0f) {
            yaw += 90.0f * strafe;
        }
        return Math.toRadians(yaw);
    }

    public static double getBaseSpeed() {
        int amplifier;
        if (MoveUtil.mc.player == null) {
            return 0.2873;
        }
        double baseSpeed = 0.2873;
        if (MoveUtil.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            amplifier = MoveUtil.mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (double)(amplifier + 1);
        }
        if (MoveUtil.mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            amplifier = MoveUtil.mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            baseSpeed *= 1.0 - 0.15 * (double)(amplifier + 1);
        }
        return baseSpeed;
    }

    public static void jump() {
        if (MoveUtil.mc.player == null) {
            return;
        }
        MoveUtil.mc.player.jump();
    }

    public static void setSpeed(double speed, float yaw) {
        if (MoveUtil.mc.player == null) {
            return;
        }
        double rad = Math.toRadians(yaw + 90.0f);
        MoveUtil.mc.player.setVelocity(speed * Math.cos(rad), MoveUtil.mc.player.getVelocity().y, speed * Math.sin(rad));
    }

    public static void addMotion(double speed) {
        if (MoveUtil.mc.player == null) {
            return;
        }
        float yaw = MoveUtil.mc.player.getYaw();
        double rad = Math.toRadians(yaw + 90.0f);
        Vec3d velocity = MoveUtil.mc.player.getVelocity();
        MoveUtil.mc.player.setVelocity(velocity.x + speed * Math.cos(rad), velocity.y, velocity.z + speed * Math.sin(rad));
    }

    public static double getSpeedBPS() {
        return MoveUtil.getMotion() * 20.0;
    }

    public static boolean isOnGround() {
        return MoveUtil.mc.player != null && MoveUtil.mc.player.isOnGround();
    }

    public static Vec3d predictPosition(int ticks) {
        if (MoveUtil.mc.player == null) {
            return Vec3d.ZERO;
        }
        Vec3d pos = new net.minecraft.util.math.Vec3d(MoveUtil.mc.player.getX(), MoveUtil.mc.player.getY(), MoveUtil.mc.player.getZ());
        Vec3d velocity = MoveUtil.mc.player.getVelocity();
        for (int i = 0; i < ticks; ++i) {
            pos = pos.add(velocity);
            velocity = velocity.multiply(0.91, 0.98, 0.91);
        }
        return pos;
    }

    public static PlayerInput getPlayerInput(float angleDiff) {
        boolean forward = false;
        boolean back = false;
        boolean left = false;
        boolean right = false;
        if ((double)angleDiff >= -22.5 && (double)angleDiff < 22.5) {
            forward = true;
        } else if ((double)angleDiff >= 22.5 && (double)angleDiff < 67.5) {
            forward = true;
            right = true;
        } else if ((double)angleDiff >= 67.5 && (double)angleDiff < 112.5) {
            right = true;
        } else if ((double)angleDiff >= 112.5 && (double)angleDiff < 157.5) {
            back = true;
            right = true;
        } else if ((double)angleDiff >= -67.5 && (double)angleDiff < -22.5) {
            forward = true;
            left = true;
        } else if ((double)angleDiff >= -112.5 && (double)angleDiff < -67.5) {
            left = true;
        } else if ((double)angleDiff >= -157.5 && (double)angleDiff < -112.5) {
            back = true;
            left = true;
        } else {
            back = true;
        }
        return new PlayerInput(forward, back, left, right, false, false, false);
    }

    public static float getdir() {
        if (MoveUtil.mc.player == null) {
            return -1.0f;
        }
        float forward = MoveUtil.mc.player.input.getMovementInput().y;
        float sideways = MoveUtil.mc.player.input.getMovementInput().x;
        if (forward == 0.0f && sideways == 0.0f) {
            return -1.0f;
        }
        float yaw = MoveUtil.mc.player.getYaw();
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        float strafe = 1.0f;
        if (forward < 0.0f) {
            strafe = -0.5f;
        } else if (forward > 0.0f) {
            strafe = 0.5f;
        }
        if (sideways > 0.0f) {
            yaw -= 90.0f * strafe;
        }
        if (sideways < 0.0f) {
            yaw += 90.0f * strafe;
        }
        return yaw;
    }
}
