/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.class_1313
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_638
 *  net.minecraft.class_742
 *  net.minecraft.class_744
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.block.PushEvent;
import code.wiosvisual.events.container.CloseScreenEvent;
import code.wiosvisual.events.player.EventNoSlow;
import code.wiosvisual.events.player.MotionEvent;
import code.wiosvisual.events.player.MoveEvent;
import code.wiosvisual.events.player.PlayerTravelEvent;
import code.wiosvisual.events.player.PostMotionEvent;
import code.wiosvisual.events.player.PostTickEvent;
import code.wiosvisual.events.player.SprintEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.client.stealth.MovementMixinGuard;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.interactions.inv.InventoryFlowManager;
import code.wiosvisual.utils.interactions.inv.InventoryTick;
import code.wiosvisual.utils.interactions.simulate.Simulations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin
extends AbstractClientPlayerEntity {
    @Final
    @Shadow
    protected MinecraftClient client;
    @Shadow
    public Input input;
    @Unique
    private double silentBodyPrevX = 0.0;
    @Unique
    private double silentBodyPrevZ = 0.0;
    private float prevBodyYaw = 0.0f;
    private boolean initialized = false;

    @Shadow
    public abstract float getPitch(float var1);

    @Shadow
    public abstract float getYaw(float var1);

    @Shadow
    protected abstract void autoJump(float var1, float var2);

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    public void tick(CallbackInfo info) {
        if (this.client.player != null && this.client.world != null) {
            SafeEvents.dispatch(() -> new TickEvent());
        }
        InventoryTick.update();
    }

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    public void onTick(CallbackInfo ci) {
        if (!this.initialized && QuickImports.mc.player != null) {
            this.silentBodyPrevX = QuickImports.mc.player.getX();
            this.silentBodyPrevZ = QuickImports.mc.player.getZ();
            this.prevBodyYaw = QuickImports.mc.player.getBodyYaw();
            this.initialized = true;
        }
    }

    @Inject(method={"tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift=At.Shift.AFTER)})
    public void postTick(CallbackInfo callbackInfo) {
        if (this.client.player != null && this.client.world != null) {
            SafeEvents.dispatch(() -> new PostTickEvent());
        }
    }

    @Inject(method={"tickMovement"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/input/Input;tick()V", shift=At.Shift.AFTER)})
    private void onInputTick(CallbackInfo ci) {
        if (QuickImports.mc.player == null) {
            return;
        }
        SafeEvents.dispatch(() -> new PlayerTravelEvent(Vec3d.ZERO, false));
    }

    @ModifyExpressionValue(method={"sendMovementPackets", "tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F")})
    private float hookSilentRotationYaw(float original) {
        if (QuickImports.mc.player != null && TurnsConnection.INSTANCE.getCurrentAngle() != null) {
            float newBodyYaw;
            float currentYaw = TurnsConnection.INSTANCE.getRotation().getYaw();
            this.prevBodyYaw = newBodyYaw = Simulations.calculateBodyYaw(currentYaw, this.prevBodyYaw, this.silentBodyPrevX, this.silentBodyPrevZ, QuickImports.mc.player.getX(), QuickImports.mc.player.getZ(), QuickImports.mc.player.handSwingProgress);
            this.silentBodyPrevX = QuickImports.mc.player.getX();
            this.silentBodyPrevZ = QuickImports.mc.player.getZ();
            QuickImports.mc.player.setBodyYaw(newBodyYaw);
            return currentYaw;
        }
        return original;
    }

    @ModifyExpressionValue(method={"sendMovementPackets", "tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F")})
    private float hookSilentRotationPitch(float original) {
        if (QuickImports.mc.player == null || TurnsConnection.INSTANCE.getCurrentAngle() == null) {
            return original;
        }
        return TurnsConnection.INSTANCE.getRotation().getPitch();
    }

    @Inject(method={"closeHandledScreen"}, at={@At(value="HEAD")}, cancellable=true)
    private void closeHandledScreenHook(CallbackInfo info) {
        CloseScreenEvent event = SafeEvents.dispatch(() -> new CloseScreenEvent(this.client.currentScreen));
        if (event != null && event.isCancelled()) {
            info.cancel();
        }
    }

    @Redirect(method={"tickMovement"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require=0)
    private boolean tickMovementHook(ClientPlayerEntity player) {
        if (!MovementMixinGuard.shouldHandleNoSlowRedirect()) {
            return player.isUsingItem();
        }
        EventNoSlow a = SafeEvents.dispatch(() -> new EventNoSlow(false));
        if (a != null && a.isCancelled()) {
            return false;
        }
        return player.isUsingItem();
    }

    @Inject(method={"sendMovementPackets"}, at={@At(value="HEAD")}, cancellable=true)
    private void preMotion(CallbackInfo ci) {
        if (!MovementMixinGuard.shouldHandleMotionEvent()) {
            return;
        }
        MotionEvent event = SafeEvents.dispatch(() -> new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYaw(1.0f), this.getPitch(1.0f), this.isOnGround()));
        if (event == null) {
            return;
        }
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        this.setPosition(event.getX(), event.getY(), event.getZ());
        this.setYaw(event.getYaw());
        this.setPitch(event.getPitch());
    }

    @Inject(method={"move"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V")}, cancellable=true)
    public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if (!MovementMixinGuard.shouldHandleMoveEvent()) {
            return;
        }
        MoveEvent event = SafeEvents.dispatch(() -> new MoveEvent(movement));
        double d = this.getX();
        double e = this.getZ();
        super.move(movementType, event != null ? event.getMovement() : movement);
        this.autoJump((float)(this.getX() - d), (float)(this.getZ() - e));
        ci.cancel();
    }

    @Inject(method={"sendMovementPackets"}, at={@At(value="RETURN")}, cancellable=true)
    private void postMotion(CallbackInfo ci) {
        PostMotionEvent postMotionEvent;
        if (MovementMixinGuard.shouldHandlePostMotionEvent() && (postMotionEvent = SafeEvents.dispatch(() -> new PostMotionEvent())) != null && postMotionEvent.isCancelled()) {
            ci.cancel();
        }
        InventoryFlowManager.postMotion();
    }

    @Inject(method={"pushOutOfBlocks"}, at={@At(value="HEAD")}, cancellable=true)
    public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        PushEvent event = SafeEvents.dispatch(() -> new PushEvent(PushEvent.Type.BLOCK));
        if (event != null && event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"shouldStopSprinting"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z")}, cancellable=true)
    public void shouldStopSprintingHook(CallbackInfoReturnable<Boolean> cir) {
    }

    @Inject(method={"canStartSprinting"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z")}, cancellable=true)
    public void canStartSprintingHook(CallbackInfoReturnable<Boolean> cir) {
    }

    // 1.21.11: canSprint() стал canSprint(boolean) и вызывается из нескольких мест — правим результат у источника.
    @Inject(method={"canSprint(Z)Z"}, at={@At(value="RETURN")}, cancellable=true)
    private void hookCanSprint(boolean allowFlying, CallbackInfoReturnable<Boolean> cir) {
        boolean original = cir.getReturnValueZ();
        SprintEvent event = SafeEvents.dispatch(() -> new SprintEvent(original));
        if (event != null && event.isSprinting() != original) {
            cir.setReturnValue(event.isSprinting());
        }
    }

    // 1.21.11: поиск цели под прицелом переехал из GameRenderer.findCrosshairTarget сюда (статический).
    @Redirect(method={"getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private static net.minecraft.util.hit.HitResult wiosvisual$hookRaycast(net.minecraft.entity.Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || instance != client.player) {
            return instance.raycast(maxDistance, tickDelta, includeFluids);
        }
        return code.wiosvisual.utils.features.aura.utils.RaycastAngle.raycast(maxDistance, TurnsConnection.INSTANCE.getRotation(), includeFluids);
    }

    @Redirect(method={"getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d wiosvisual$hookRotationVector(net.minecraft.entity.Entity instance, float tickDelta) {
        if (MinecraftClient.getInstance().player == null) {
            return instance.getRotationVec(tickDelta);
        }
        return TurnsConnection.INSTANCE.getRotation().toVector();
    }
}
