/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1675
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_3959
 *  net.minecraft.class_3959$class_242
 *  net.minecraft.class_3959$class_3960
 *  net.minecraft.class_3965
 *  net.minecraft.class_3966
 *  net.minecraft.class_638
 *  net.minecraft.class_746
 */
package code.wiosvisual.utils.features.aura.utils;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class RaycastAngle
implements QuickImports {
    public static BlockHitResult raycast(double range, Turns angle, boolean includeFluids) {
        return RaycastAngle.raycast(Objects.requireNonNull(RaycastAngle.mc.player).getCameraPosVec(1.0f), range, angle, includeFluids);
    }

    public static BlockHitResult raycast(Vec3d vec, double range, Turns angle, boolean includeFluids) {
        Entity entity = RaycastAngle.mc.cameraEntity;
        if (entity == null) {
            return null;
        }
        Vec3d rotationVec = angle.toVector();
        Vec3d end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        ClientWorld world = RaycastAngle.mc.world;
        if (world == null) {
            return null;
        }
        RaycastContext.FluidHandling fluidHandling = includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;
        RaycastContext context = new RaycastContext(vec, end, RaycastContext.ShapeType.OUTLINE, fluidHandling, entity);
        return world.raycast(context);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType) {
        return RaycastAngle.raycast(start, end, shapeType, (Entity)RaycastAngle.mc.player);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType, Entity entity) {
        return RaycastAngle.raycast(start, end, shapeType, RaycastContext.FluidHandling.NONE, entity);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, Entity entity) {
        return RaycastAngle.mc.world.raycast(new RaycastContext(start, end, shapeType, fluidHandling, entity));
    }

    public static EntityHitResult raytraceEntity(double range, Turns angle, Predicate<Entity> filter) {
        ClientPlayerEntity entity = RaycastAngle.mc.player;
        if (entity == null) {
            return null;
        }
        Vec3d cameraVec = entity.getCameraPosVec(1.0f);
        Vec3d rotationVec = angle.toVector();
        Vec3d vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0);
        return ProjectileUtil.raycast((Entity)entity, (Vec3d)cameraVec, (Vec3d)vec3d3, (Box)box, e -> !e.isSpectator() && filter.test((Entity)e), (double)(range * range));
    }

    public static boolean rayTrace(double range, Box box) {
        return RaycastAngle.rayTrace(TurnsConnection.INSTANCE.getRotation().toVector(), range - 0.25, box);
    }

    public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
        Vec3d cameraVec = Objects.requireNonNull(RaycastAngle.mc.player).getEyePos();
        return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
    }

    private RaycastAngle() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
