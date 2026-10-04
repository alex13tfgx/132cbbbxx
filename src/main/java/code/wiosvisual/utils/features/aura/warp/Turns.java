/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 */
package code.wiosvisual.utils.features.aura.warp;

import code.wiosvisual.main.client.Api;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.math.calc.Calculate;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Turns {
    public static Turns DEFAULT = new Turns(0.0f, 0.0f);
    private float yaw;
    private float pitch;

    public static Turns fromTargetHead(Vec3d playerPos, Vec3d targetPos, double targetHeight) {
        double headY = targetPos.y + targetHeight * 0.9;
        double deltaX = targetPos.x - playerPos.x;
        double deltaY = headY - (playerPos.y + 1.5);
        double deltaZ = targetPos.z - playerPos.z;
        float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        yaw = MathHelper.wrapDegrees((float)yaw);
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float pitch = (float)Math.toDegrees(-Math.atan2(deltaY, horizontalDistance));
        pitch = MathHelper.clamp((float)pitch, (float)-90.0f, (float)90.0f);
        return new Turns(yaw, pitch);
    }

    public static void smoothResetRotation() {
        if (Api.mc.player == null) {
            TurnsConnection.INSTANCE.clear();
            TurnsConnection.INSTANCE.setRotation(null);
            return;
        }
        Turns currentRotation = TurnsConnection.INSTANCE.getRotation();
        if (currentRotation == null) {
            return;
        }
        float playerYaw = Api.mc.player.getYaw();
        float playerPitch = Api.mc.player.getPitch();
        float resetYaw = 0.2f;
        float resetPitch = 0.2f;
        TurnsConnection.INSTANCE.setRotation(new Turns(resetYaw, resetPitch));
    }

    public Turns adjustSensitivity() {
        double gcd = Calculate.computeGcd();
        Turns previousAngle = TurnsConnection.INSTANCE.getServerAngle();
        float adjustedYaw = this.adjustAxis(this.yaw, previousAngle.yaw, gcd);
        float adjustedPitch = this.adjustAxis(this.pitch, previousAngle.pitch, gcd);
        return new Turns(adjustedYaw, MathHelper.clamp((float)adjustedPitch, (float)-90.0f, (float)90.0f));
    }

    public Turns random(float f) {
        return new Turns(this.yaw + Calculate.getRandom(-f, f), this.pitch + Calculate.getRandom(-f, f));
    }

    private float adjustAxis(float axisValue, float previousValue, double gcd) {
        float delta = axisValue - previousValue;
        return previousValue + (float)Math.round((double)delta / gcd) * (float)gcd;
    }

    public final Vec3d toVector() {
        float f = this.pitch * ((float)Math.PI / 180);
        float g = -this.yaw * ((float)Math.PI / 180);
        float h = MathHelper.cos((float)g);
        float i = MathHelper.sin((float)g);
        float j = MathHelper.cos((float)f);
        float k = MathHelper.sin((float)f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }

    public Turns addYaw(float yaw) {
        return new Turns(this.yaw + yaw, this.pitch);
    }

    public Turns addPitch(float pitch) {
        this.pitch = MathHelper.clamp((float)(this.pitch + pitch), (float)-90.0f, (float)90.0f);
        return this;
    }

    public Turns of(Turns angle) {
        return new Turns(angle.getYaw(), angle.getPitch());
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public String toString() {
        return "Turns(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
    }

    public Turns(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static class VecRotation {
        private final Turns angle;
        private final Vec3d vec;

        public String toString() {
            return "Turns.VecRotation(angle=" + String.valueOf(this.getAngle()) + ", vec=" + String.valueOf(this.getVec()) + ")";
        }

        public Turns getAngle() {
            return this.angle;
        }

        public Vec3d getVec() {
            return this.vec;
        }

        public VecRotation(Turns angle, Vec3d vec) {
            this.angle = angle;
            this.vec = vec;
        }
    }
}
