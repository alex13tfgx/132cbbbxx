/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_2338
 *  net.minecraft.class_2374
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 *  net.minecraft.class_4184
 *  net.minecraft.class_4604
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector4d
 *  org.joml.Vector4f
 */
package code.wiosvisual.utils.math.projection;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.math.calc.Calculate;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;

public final class Projection
implements QuickImports {
    private static final Vector4f TRANSFORMED = new Vector4f();
    private static final Vector3f PROJECTED = new Vector3f();
    private static final Matrix4f PROJ_MATRIX = new Matrix4f();
    private static final int[] VIEWPORT = new int[4];
    private static final double[] SCREEN_SCRATCH = new double[3];

    @NotNull
    public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        return Projection.worldSpaceToScreenSpace(pos.x, pos.y, pos.z);
    }

    @NotNull
    public static Vec3d worldSpaceToScreenSpace(double x, double y, double z) {
        if (Projection.worldSpaceToScreenSpaceInto(x, y, z, SCREEN_SCRATCH)) {
            return new Vec3d(SCREEN_SCRATCH[0], SCREEN_SCRATCH[1], SCREEN_SCRATCH[2]);
        }
        return new Vec3d(0.0, 0.0, -1.0);
    }

    public static boolean worldSpaceToScreenSpaceInto(double x, double y, double z, double[] out) {
        TRANSFORMED.set((float)x, (float)y, (float)z, 1.0f).mul((Matrix4fc)Render3D.lastWorldSpaceMatrix.getPositionMatrix());
        PROJ_MATRIX.set((Matrix4fc)Render3D.lastProjMat);
        Projection.VIEWPORT[0] = 0;
        Projection.VIEWPORT[1] = 0;
        Projection.VIEWPORT[2] = mc.getWindow().getFramebufferWidth();
        Projection.VIEWPORT[3] = mc.getWindow().getFramebufferHeight();
        PROJ_MATRIX.project(TRANSFORMED.x(), TRANSFORMED.y(), TRANSFORMED.z(), VIEWPORT, PROJECTED);
        double depth = Projection.PROJECTED.z;
        if (depth <= 0.0) {
            return false;
        }
        double scale = mc.getWindow().getScaleFactor();
        out[0] = (double)Projection.PROJECTED.x / scale;
        out[1] = (double)((float)mc.getWindow().getHeight() - Projection.PROJECTED.y) / scale;
        out[2] = depth;
        return true;
    }

    public static double getDistanceToGround() {
        for (double y = Projection.mc.player.getY(); y > 0.0; y -= 0.1) {
            if (Projection.mc.world.getBlockState(Projection.mc.player.getBlockPos().down((int)(Projection.mc.player.getY() - y + 1.0))).isAir()) continue;
            return Projection.mc.player.getY() - y;
        }
        return 256.0;
    }

    public static double getMaxRenderDistanceSq() {
        if (Projection.mc.options == null) {
            return 65536.0;
        }
        double max = (double)((Integer)Projection.mc.options.getViewDistance().getValue()).intValue() * 16.0;
        return max * max;
    }

    public static boolean isWithinRenderDistance(Entity ent) {
        if (ent == null || Projection.mc.player == null) {
            return false;
        }
        return ent.squaredDistanceTo((Entity)Projection.mc.player) <= Projection.getMaxRenderDistanceSq();
    }

    public static Vector4d getVector4D(Entity ent) {
        Vector4d out = new Vector4d();
        return Projection.fillVector4D(ent, out) ? out : null;
    }

    public static boolean fillVector4D(Entity ent, Vector4d out) {
        if (ent == null || !Projection.isWithinRenderDistance(ent)) {
            return false;
        }
        Vec3d pos = Calculate.interpolate(ent);
        Box bb = ent.getBoundingBox();
        double dx = pos.x - ent.getX();
        double dy = pos.y - ent.getY();
        double dz = pos.z - ent.getZ();
        double minX = bb.minX + dx - 0.1;
        double minY = bb.minY + dy - 0.1;
        double minZ = bb.minZ + dz - 0.1;
        double maxX = bb.maxX + dx + 0.1;
        double maxY = bb.maxY + dy + 0.1;
        double maxZ = bb.maxZ + dz + 0.1;
        boolean visible = false;
        visible |= Projection.mergeCornerInto(out, visible, minX, minY, minZ);
        visible |= Projection.mergeCornerInto(out, visible, minX, maxY, minZ);
        visible |= Projection.mergeCornerInto(out, visible, maxX, minY, minZ);
        visible |= Projection.mergeCornerInto(out, visible, maxX, maxY, minZ);
        visible |= Projection.mergeCornerInto(out, visible, minX, minY, maxZ);
        visible |= Projection.mergeCornerInto(out, visible, minX, maxY, maxZ);
        visible |= Projection.mergeCornerInto(out, visible, maxX, minY, maxZ);
        return (visible |= Projection.mergeCornerInto(out, visible, maxX, maxY, maxZ)) && !Projection.cantSee(out);
    }

    private static boolean mergeCornerInto(Vector4d position, boolean hasCorner, double x, double y, double z) {
        if (!Projection.worldSpaceToScreenSpaceInto(x, y, z, SCREEN_SCRATCH)) {
            return hasCorner;
        }
        double screenX = SCREEN_SCRATCH[0];
        double screenY = SCREEN_SCRATCH[1];
        if (!hasCorner) {
            position.x = screenX;
            position.y = screenY;
            position.z = screenX;
            position.w = screenY;
            return true;
        }
        position.x = Math.min(screenX, position.x);
        position.y = Math.min(screenY, position.y);
        position.z = Math.max(screenX, position.z);
        position.w = Math.max(screenY, position.w);
        return true;
    }

    @Deprecated
    @NotNull
    public static Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
        Box axisAlignedBB2 = ent.getBoundingBox();
        Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + pos.x - (double)0.1f, axisAlignedBB2.minY - ent.getY() + pos.y - (double)0.1f, axisAlignedBB2.minZ - ent.getZ() + pos.z - (double)0.1f, axisAlignedBB2.maxX - ent.getX() + pos.x + (double)0.1f, axisAlignedBB2.maxY - ent.getY() + pos.y + (double)0.1f, axisAlignedBB2.maxZ - ent.getZ() + pos.z + (double)0.1f);
        return new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
    }

    public static boolean isInFrontOfCamera(Vec3d worldPos) {
        Camera camera = Projection.mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            return false;
        }
        Vec3d delta = worldPos.subtract(camera.pos);
        if (delta.lengthSquared() < 1.0E-6) {
            return false;
        }
        Vec3d look = Vec3d.fromPolar((float)camera.getPitch(), (float)camera.getYaw());
        return look.dotProduct(delta) > 0.0;
    }

    public static boolean canSee(Vec3d vec3d) {
        Camera camera = Projection.mc.getEntityRenderDispatcher().camera;
        Turns angle = MathAngle.fromVec3d(vec3d.subtract(camera.pos));
        return Math.abs(MathHelper.wrapDegrees((float)(angle.getYaw() - camera.getYaw()))) < 90.0f && Math.abs(MathHelper.wrapDegrees((float)(angle.getPitch() - camera.getPitch()))) < 60.0f || Projection.canSee(new Box(BlockPos.ofFloored((Position)vec3d)));
    }

    public static boolean canSee(Box box) {
        Frustum frustum = Projection.mc.worldRenderer.getCapturedFrustum();
        if (frustum == null) {
            return true;
        }
        return box != null && frustum.isVisible(box);
    }

    public static boolean cantSee(Vector4d vec) {
        return vec == null || vec.x < 0.0 && vec.z < 1.0 || vec.y < 0.0 && vec.w < 1.0;
    }

    public static double centerX(Vector4d vec) {
        return vec.x + (vec.z - vec.x) / 2.0;
    }

    private Projection() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
