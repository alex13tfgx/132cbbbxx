/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1922
 *  net.minecraft.class_2338$class_2339
 *  net.minecraft.class_243
 *  net.minecraft.class_4184
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.render.CameraEvent;
import code.wiosvisual.events.render.CameraPositionEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.features.aura.warp.Turns;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Camera.class})
public abstract class CameraMixin {
    @Shadow
    private Vec3d pos;
    @Shadow
    @Final
    private BlockPos.Mutable blockPos;
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;

    @Shadow
    public abstract void setRotation(float var1, float var2);

    @Shadow
    protected abstract void moveBy(float var1, float var2, float var3);

    @Shadow
    protected abstract float clipToSpace(float var1);

    @Inject(method={"update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void updateHook(net.minecraft.world.World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        ClientPlayerEntity player;
        CameraEvent event = SafeEvents.dispatch(() -> new CameraEvent(false, 4.0f, new Turns(this.yaw, this.pitch)));
        if (event == null) {
            return;
        }
        Turns angle = event.getAngle();
        if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity && !(player = (ClientPlayerEntity)focusedEntity).isSleeping() && thirdPerson) {
            float pitch = inverseView ? -angle.getPitch() : angle.getPitch();
            float yaw = angle.getYaw() - (float)(inverseView ? 180 : 0);
            float distance = event.getDistance();
            this.setRotation(yaw, pitch);
            this.moveBy(event.isCameraClip() ? -distance : -this.clipToSpace(distance), 0.0f, 0.0f);
            ci.cancel();
        }
    }

    @Redirect(method={"update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void redirectSetRotation(Camera instance, float yaw, float pitch, net.minecraft.world.World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        CameraEvent event = SafeEvents.dispatch(() -> new CameraEvent(false, 4.0f, new Turns(yaw, pitch)).rotationStage(true));
        float newYaw = yaw;
        float newPitch = pitch;
        if (event != null && event.isCancelled() && event.getAngle() != null) {
            newYaw = event.getAngle().getYaw();
            newPitch = event.getAngle().getPitch();
        }
        if (thirdPerson && inverseView) {
            newYaw += 180.0f;
            newPitch = -newPitch;
        }
        instance.setRotation(newYaw, newPitch);
    }

    @Inject(method={"setPos(Lnet/minecraft/util/math/Vec3d;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void posHook(Vec3d pos, CallbackInfo ci) {
        Vec3d originalPos = pos;
        CameraPositionEvent event = SafeEvents.dispatch(() -> new CameraPositionEvent(originalPos));
        pos = event != null ? event.getPos() : originalPos;
        this.pos = pos;
        this.blockPos.set(pos.x, pos.y, pos.z);
        ci.cancel();
    }
}
