/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  net.minecraft.class_1657
 *  net.minecraft.class_1664
 *  net.minecraft.class_243
 *  net.minecraft.class_742
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.block.PushEvent;
import code.wiosvisual.events.player.PlayerTravelEvent;
import code.wiosvisual.events.player.SwimmingEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PlayerEntity.class})
public abstract class PlayerEntityMixin
implements QuickImports {
    @Inject(method={"isPushedByFluids"}, at={@At(value="HEAD")}, cancellable=true)
    public void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        PushEvent event = SafeEvents.dispatch(() -> new PushEvent(PushEvent.Type.WATER));
        if (event != null && event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method={"travel"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")})
    public Vec3d travelHook(Vec3d vec3d) {
        SwimmingEvent event = SafeEvents.dispatch(() -> new SwimmingEvent(vec3d));
        return event != null ? event.getVector() : vec3d;
    }

    @Inject(method={"travel"}, at={@At(value="HEAD")}, cancellable=true)
    private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
        if (PlayerEntityMixin.mc.player == null) {
            return;
        }
        PlayerTravelEvent event = SafeEvents.dispatch(() -> new PlayerTravelEvent(movementInput, true));
        if (event != null && event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"travel"}, at={@At(value="RETURN")}, cancellable=true)
    private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
        if (PlayerEntityMixin.mc.player == null) {
            return;
        }
        PlayerTravelEvent event = SafeEvents.dispatch(() -> new PlayerTravelEvent(movementInput, false));
        if (event == null || event.isCancelled()) {
            // empty if block
        }
    }
}
