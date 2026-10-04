/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  net.minecraft.class_1309
 *  net.minecraft.class_1671
 *  net.minecraft.class_243
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.player.FireworkEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={FireworkRocketEntity.class})
public class FireworkRocketEntityMixin
implements QuickImports {
    @Shadow
    @Nullable
    private LivingEntity shooter;

    @WrapOperation(method={"tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")})
    public Vec3d getRotationVectorHook(LivingEntity instance, Operation<Vec3d> original) {
        if (this.shooter == FireworkRocketEntityMixin.mc.player) {
            return TurnsConnection.INSTANCE.getMoveRotation().toVector();
        }
        return (Vec3d)original.call(new Object[]{instance});
    }

    @WrapOperation(method={"tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;", ordinal=0)})
    public Vec3d getVelocityHook(LivingEntity instance, Operation<Vec3d> original) {
        if (this.shooter == FireworkRocketEntityMixin.mc.player) {
            Vec3d velocity = (Vec3d)original.call(new Object[]{instance});
            FireworkEvent event = SafeEvents.dispatch(() -> new FireworkEvent(velocity));
            return event != null ? event.getVector() : velocity;
        }
        return (Vec3d)original.call(new Object[]{instance});
    }
}
