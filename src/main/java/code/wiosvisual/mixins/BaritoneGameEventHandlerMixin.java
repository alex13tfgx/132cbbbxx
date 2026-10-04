/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  baritone.api.event.events.BlockInteractEvent
 *  baritone.api.event.events.BlockInteractEvent$Type
 *  baritone.api.event.events.RenderEvent
 *  baritone.api.event.events.RotationMoveEvent
 *  baritone.api.utils.Rotation
 *  baritone.event.GameEventHandler
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.utils.Rotation;
import baritone.event.GameEventHandler;
import code.wiosvisual.utils.interactions.path.BaritoneHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GameEventHandler.class})
public class BaritoneGameEventHandlerMixin {
    @Inject(method={"onRenderPass"}, at={@At(value="HEAD")}, cancellable=true)
    private void wiosvisual$suppressRender(RenderEvent event, CallbackInfo ci) {
        if (BaritoneHelper.isSilentModeActive()) {
            ci.cancel();
        }
    }

    @Inject(method={"onPlayerRotationMove"}, at={@At(value="TAIL")})
    private void wiosvisual$suppressRotationMove(RotationMoveEvent event, CallbackInfo ci) {
        if (!BaritoneHelper.isSilentModeActive()) {
            return;
        }
        Rotation original = event.getOriginal();
        event.setYaw(original.getYaw());
        event.setPitch(original.getPitch());
    }

    @Inject(method={"onBlockInteract"}, at={@At(value="HEAD")}, cancellable=true)
    private void wiosvisual$preventBlockBreak(BlockInteractEvent event, CallbackInfo ci) {
        if (BaritoneHelper.isSilentModeActive() && event.getType() == BlockInteractEvent.Type.START_BREAK) {
            ci.cancel();
        }
    }
}
