/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  net.minecraft.class_1297
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.player.BoundingBoxControlEvent;
import code.wiosvisual.events.player.EventOnMovePost;
import code.wiosvisual.events.player.PlayerVelocityStrafeEvent;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.client.stealth.MovementMixinGuard;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Entity.class})
public abstract class EntityMixin
implements QuickImports {
    @Shadow
    private Box boundingBox;
    @Shadow
    public float yaw;
    @Unique
    private final MinecraftClient client = MinecraftClient.getInstance();

    @Redirect(method={"updateVelocity"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        if ((Object)this == EntityMixin.mc.player && MovementMixinGuard.shouldHandleVelocityStrafe()) {
            Vec3d velocity = Entity.movementInputToVelocity((Vec3d)movementInput, (float)speed, (float)yaw);
            PlayerVelocityStrafeEvent event = SafeEvents.dispatch(() -> new PlayerVelocityStrafeEvent(movementInput, speed, yaw, velocity));
            return event != null ? event.getVelocity() : velocity;
        }
        return Entity.movementInputToVelocity((Vec3d)movementInput, (float)speed, (float)yaw);
    }

    @Inject(method={"getBoundingBox"}, at={@At(value="HEAD")}, cancellable=true)
    public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
        if (!MovementMixinGuard.isHitBoxActive()) {
            return;
        }
        BoundingBoxControlEvent event = SafeEvents.dispatch(() -> new BoundingBoxControlEvent(this.boundingBox, (Entity)(Object)this));
        cir.setReturnValue((event != null ? event.getBox() : this.boundingBox));
    }

    @ModifyExpressionValue(method={"move"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;isControlledByPlayer()Z")})
    public boolean isControlledByPlayerHook(boolean original) {
        if ((Object)this == EntityMixin.mc.player && MovementMixinGuard.shouldUseEntityMovementPhysics()) {
            return false;
        }
        return original;
    }

    @Inject(method={"updateVelocity"}, at={@At(value="TAIL")})
    private void onUpdateVelocityPost(float speed, Vec3d movementInput, CallbackInfo ci) {
        Entity me = (Entity)(Object)this;
        if (MinecraftClient.getInstance().player != null && me.getId() == MinecraftClient.getInstance().player.getId()) {
            EventManager.callEvent(new EventOnMovePost(speed, movementInput));
        }
    }
}
