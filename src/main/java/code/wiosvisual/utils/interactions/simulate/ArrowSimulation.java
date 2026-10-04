/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1667
 *  net.minecraft.class_1675
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_1937
 *  net.minecraft.class_238
 *  net.minecraft.class_239
 *  net.minecraft.class_239$class_240
 *  net.minecraft.class_243
 *  net.minecraft.class_3959
 *  net.minecraft.class_3959$class_242
 *  net.minecraft.class_3959$class_3960
 *  net.minecraft.class_3965
 *  net.minecraft.class_3966
 *  net.minecraft.class_638
 */
package code.wiosvisual.utils.interactions.simulate;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class ArrowSimulation
implements QuickImports {
    public final ClientWorld world;
    public Vec3d pos;
    public Vec3d velocity;
    public final boolean collideEntities;
    public boolean inGround = false;

    public ArrowSimulation(ClientWorld world, Vec3d pos, Vec3d velocity) {
        this(world, pos, velocity, true);
    }

    public ArrowSimulation(ClientWorld world, Vec3d pos, Vec3d velocity, boolean collideEntities) {
        this.world = world;
        this.pos = pos;
        this.velocity = velocity;
        this.collideEntities = collideEntities;
    }

    public HitResult tick() {
        if (this.inGround) {
            return null;
        }
        Vec3d newPos = this.pos.add(this.velocity);
        double drag = this.isTouchingWater() ? 0.6 : 0.99;
        this.velocity = this.velocity.multiply(drag);
        this.velocity = new Vec3d(this.velocity.x, this.velocity.y - (double)0.05f, this.velocity.z);
        HitResult hitResult = this.updateCollision(this.pos, newPos);
        if (hitResult != null) {
            this.pos = hitResult.getPos();
            this.inGround = true;
            return hitResult;
        }
        this.pos = newPos;
        return null;
    }

    private HitResult updateCollision(Vec3d pos, Vec3d newPos) {
        double size;
        EntityHitResult entityHitResult;
        ClientWorld world = this.world;
        ArrowEntity arrowEntity = new ArrowEntity((World)this.world, this.pos.x, this.pos.y, this.pos.z, new ItemStack((ItemConvertible)Items.ARROW), null);
        BlockHitResult blockHitResult = world.raycast(new RaycastContext(pos, newPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)arrowEntity));
        if (this.collideEntities && (entityHitResult = ProjectileUtil.getEntityCollision((World)this.world, (net.minecraft.entity.projectile.ProjectileEntity)arrowEntity, (Vec3d)pos, (Vec3d)newPos, (Box)new Box(-(size = 0.45), -size, -size, size, size, size).offset(pos).stretch(newPos.subtract(pos)).expand(1.0), entity -> {
            if (!entity.isSpectator() && entity.isAlive() && (entity.canHit() || entity != ArrowSimulation.mc.player && entity == arrowEntity)) {
                return !arrowEntity.isConnectedThroughVehicle(entity);
            }
            return false;
        })) != null && entityHitResult.getType() != HitResult.Type.MISS) {
            return entityHitResult;
        }
        if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
            return blockHitResult;
        }
        return null;
    }

    private boolean isTouchingWater() {
        return false;
    }
}
