/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1675
 *  net.minecraft.class_238
 *  net.minecraft.class_239
 *  net.minecraft.class_241
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 *  net.minecraft.class_3959
 *  net.minecraft.class_3959$class_242
 *  net.minecraft.class_3959$class_3960
 *  net.minecraft.class_3966
 */
package code.wiosvisual.utils.features.aura.utils;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;

public final class MathAngle
implements QuickImports {
    public static Turns fromVec2f(Vec2f vector2f) {
        return new Turns(vector2f.y, vector2f.x);
    }

    public static float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees((float)(a - b));
    }

    public static Turns fromVec3d(Vec3d vector) {
        return new Turns((float)MathHelper.wrapDegrees((double)(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0)), (float)MathHelper.wrapDegrees((double)Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z)))));
    }

    public static Turns calculateDelta(Turns start, Turns end) {
        float deltaYaw = MathHelper.wrapDegrees((float)(end.getYaw() - start.getYaw()));
        float deltaPitch = MathHelper.wrapDegrees((float)(end.getPitch() - start.getPitch()));
        return new Turns(deltaYaw, deltaPitch);
    }

    public static Turns calculateAngle(Vec3d to) {
        if (MathAngle.mc.player == null) {
            return new Turns(0.0f, 0.0f);
        }
        return MathAngle.fromVec3d(to.subtract(MathAngle.mc.player.getEyePos()));
    }

    public static Turns pitch(float pitch) {
        if (MathAngle.mc.player == null) {
            return new Turns(0.0f, pitch);
        }
        return new Turns(MathAngle.mc.player.getYaw(), pitch);
    }

    public static Turns cameraAngle() {
        if (MathAngle.mc.player == null) {
            return new Turns(0.0f, 0.0f);
        }
        return new Turns(MathAngle.mc.player.getYaw(), MathAngle.mc.player.getPitch());
    }

    public static boolean rayTrace(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
        double maxDistance;
        Box entityArea;
        Vec3d rotationVector;
        Vec3d endPoint;
        EntityHitResult ehr;
        if (MathAngle.mc.player == null || MathAngle.mc.world == null) {
            return false;
        }
        HitResult result = MathAngle.rayTrace(distance, yaw, pitch);
        Vec3d startPoint = new net.minecraft.util.math.Vec3d(MathAngle.mc.player.getX(), MathAngle.mc.player.getY(), MathAngle.mc.player.getRandomBodyY()).add(0.0, (double)MathAngle.mc.player.getEyeHeight(MathAngle.mc.player.getPose()), 0.0);
        double distancePow2 = Math.pow(distance, 2.0);
        if (result != null) {
            distancePow2 = startPoint.squaredDistanceTo(result.getPos());
        }
        if ((ehr = ProjectileUtil.raycast((Entity)MathAngle.mc.player, (Vec3d)startPoint, (Vec3d)(endPoint = startPoint.add(rotationVector = MathAngle.getRotationVector(pitch, yaw).multiply((double)distance))), (Box)(entityArea = MathAngle.mc.player.getBoundingBox().stretch(rotationVector).expand(1.0, 1.0, 1.0)), e -> !e.isSpectator() && e.canHit() && e == entity, (double)(maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2.0))))) != null) {
            double targetHeight;
            double minY;
            double minHitY;
            boolean wallBehindEntity;
            boolean allowedWallDistance = startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(wallDistance, 2.0);
            boolean wallMissing = result == null;
            boolean bl = wallBehindEntity = startPoint.squaredDistanceTo(ehr.getPos()) < distancePow2;
            if (startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(distance, 2.0) && ehr.getPos().y >= (minHitY = (minY = entity.getY()) + (targetHeight = (double)entity.getHeight()) * 0.3)) {
                return ehr.getEntity() == entity;
            }
        }
        return false;
    }

    public static float getGCDValue() {
        double sensitivity = (Double)MathAngle.mc.options.getMouseSensitivity().getValue();
        double value = sensitivity * 0.6 + 0.2;
        double result = Math.pow(value, 3.0) * 0.8;
        return (float)result * 0.15f;
    }

    public static HitResult rayTrace(double dst, float yaw, float pitch) {
        if (MathAngle.mc.player == null || MathAngle.mc.world == null) {
            return null;
        }
        Vec3d vec3d = MathAngle.mc.player.getCameraPosVec(1.0f);
        Vec3d vec3d2 = MathAngle.getRotationVector(pitch, yaw);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return MathAngle.mc.world.raycast(new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, (Entity)MathAngle.mc.player));
    }

    @NotNull
    public static Vec3d getRotationVector(float yaw, float pitch) {
        return new Vec3d((double)(MathHelper.sin((float)(-pitch * ((float)Math.PI / 180))) * MathHelper.cos((float)(yaw * ((float)Math.PI / 180)))), (double)(-MathHelper.sin((float)(yaw * ((float)Math.PI / 180)))), (double)(MathHelper.cos((float)(-pitch * ((float)Math.PI / 180))) * MathHelper.cos((float)(yaw * ((float)Math.PI / 180)))));
    }

    public static Vec3d getClosestVec(Entity entity) {
        if (MathAngle.mc.player == null || entity == null) {
            return Vec3d.ZERO;
        }
        Vec3d eyePosVec = MathAngle.mc.player.getEyePos();
        Box boundingBox = entity.getBoundingBox();
        return new Vec3d(MathHelper.clamp((double)eyePosVec.x, (double)boundingBox.minX, (double)boundingBox.maxX), MathHelper.clamp((double)eyePosVec.y, (double)boundingBox.minY, (double)boundingBox.maxY), MathHelper.clamp((double)eyePosVec.z, (double)boundingBox.minZ, (double)boundingBox.maxZ));
    }

    public static double getDistanceEyePos(Entity target) {
        if (MathAngle.mc.player == null || target == null) {
            return 0.0;
        }
        Vec3d closestHitboxPoint = MathAngle.getClosestVec(target);
        return MathAngle.mc.player.getEyePos().distanceTo(closestHitboxPoint);
    }

    public static double getStrictDistance(Entity entity) {
        return MathAngle.getDistanceEyePos(entity);
    }

    private MathAngle() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
