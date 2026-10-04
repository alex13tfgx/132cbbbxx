/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_1792
 *  net.minecraft.class_243
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package code.wiosvisual.mixins;

import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={Item.class})
public class ItemMixin {
    @Redirect(method={"raycast"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/player/PlayerEntity;getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d raycastHook(PlayerEntity player, float pitch, float yaw) {
        if (player == null) {
            return new Vec3d(0.0, 0.0, 1.0);
        }
        return TurnsConnection.INSTANCE.getRotation().toVector();
    }
}
