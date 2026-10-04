/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_2350
 *  net.minecraft.class_238
 *  net.minecraft.class_239
 *  net.minecraft.class_241
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 *  net.minecraft.class_3959
 *  net.minecraft.class_3959$class_242
 *  net.minecraft.class_3959$class_3960
 */
package code.wiosvisual.utils.features.rotate;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.rotate.Rotation;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;

public final class RotationUtils
implements QuickImports {
    public static HitResult rayTrace(double dst, float yaw, float pitch) {
        Vec3d vec3d = RotationUtils.mc.player.getCameraPosVec(1.0f);
        Vec3d vec3d2 = RotationUtils.getRotationVector(pitch, yaw);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return RotationUtils.mc.world.raycast(new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, (Entity)RotationUtils.mc.player));
    }

    static Vec3d getBestVector(Entity entity) {
        Vec3d eyePos = RotationUtils.mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double step = 0.1;
        Vec3d bestVec = null;
        double closestDistance = Double.MAX_VALUE;
        for (double x = box.minX; x <= box.maxX; x += step) {
            for (double y = box.minY; y <= box.maxY; y += step) {
                for (double z = box.minZ; z <= box.maxZ; z += step) {
                    Vec3d sample = new Vec3d(x, y, z);
                    double dist = eyePos.distanceTo(sample);
                    if (!(dist < closestDistance)) continue;
                    closestDistance = dist;
                    bestVec = sample;
                }
            }
        }
        return bestVec;
    }

    public static Rotation fromVec3d(Vec3d vector) {
        return new Rotation((float)MathHelper.wrapDegrees((double)(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0)), (float)MathHelper.wrapDegrees((double)Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z)))));
    }

    @NotNull
    public static Vec3d getRotationVector(float yaw, float pitch) {
        return new Vec3d((double)(MathHelper.sin((float)(-pitch * ((float)Math.PI / 180))) * MathHelper.cos((float)(yaw * ((float)Math.PI / 180)))), (double)(-MathHelper.sin((float)(yaw * ((float)Math.PI / 180)))), (double)(MathHelper.cos((float)(-pitch * ((float)Math.PI / 180))) * MathHelper.cos((float)(yaw * ((float)Math.PI / 180)))));
    }

    public static Vec2f getRotations(Entity entity) {
        return RotationUtils.getRotations(entity.getX(), entity.getY(), entity.getZ());
    }

    public static Vec2f getRotations(Vec3d vec3d) {
        return RotationUtils.getRotations(vec3d.x, vec3d.y, vec3d.z);
    }

    public static Vec2f getRotations(double x, double y, double z) {
        double deltaX = x - RotationUtils.mc.player.getX();
        double deltaY = y - RotationUtils.mc.player.getEyeY();
        double deltaZ = z - RotationUtils.mc.player.getZ();
        double distance = MathHelper.sqrt((float)((float)(deltaX * deltaX + deltaZ * deltaZ)));
        float yaw = (float)(MathHelper.atan2((double)deltaZ, (double)deltaX) * 57.29577951308232 - 90.0);
        float pitch = (float)(-MathHelper.atan2((double)deltaY, (double)distance) * 57.29577951308232);
        return new Vec2f(yaw, pitch);
    }

    public static float[] getRotations(Direction direction) {
        float[] fArray;
        switch (direction) {
            default: {
                throw new MatchException(null, null);
            }
            case DOWN: {
                float[] fArray2 = new float[2];
                fArray2[0] = RotationUtils.mc.player.getYaw();
                fArray = fArray2;
                fArray2[1] = 90.0f;
                break;
            }
            case UP: {
                float[] fArray3 = new float[2];
                fArray3[0] = RotationUtils.mc.player.getYaw();
                fArray = fArray3;
                fArray3[1] = -90.0f;
                break;
            }
            case NORTH: {
                float[] fArray4 = new float[2];
                fArray4[0] = 180.0f;
                fArray = fArray4;
                fArray4[1] = RotationUtils.mc.player.getPitch();
                break;
            }
            case SOUTH: {
                float[] fArray5 = new float[2];
                fArray5[0] = 0.0f;
                fArray = fArray5;
                fArray5[1] = RotationUtils.mc.player.getPitch();
                break;
            }
            case WEST: {
                float[] fArray6 = new float[2];
                fArray6[0] = 90.0f;
                fArray = fArray6;
                fArray6[1] = RotationUtils.mc.player.getPitch();
                break;
            }
            case EAST: {
                float[] fArray7 = new float[2];
                fArray7[0] = -90.0f;
                fArray = fArray7;
                fArray7[1] = RotationUtils.mc.player.getPitch();
            }
        }
        return fArray;
    }

    public static float[] correctRotation(float[] rotations) {
        rotations[0] = rotations[0] - rotations[0] % GCDUtil.getGCDValue();
        rotations[1] = rotations[1] - rotations[1] % GCDUtil.getGCDValue();
        return new float[]{rotations[0], rotations[1]};
    }

    public static float getFixRotate(float rot) {
        return RotationUtils.getDeltaMouse(rot) * GCDUtil.getGCDValue();
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / GCDUtil.getGCDValue());
    }

    private RotationUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
