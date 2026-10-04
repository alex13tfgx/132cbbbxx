/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  net.minecraft.class_1268
 *  net.minecraft.class_1282
 *  net.minecraft.class_1291
 *  net.minecraft.class_1293
 *  net.minecraft.class_1294
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_6880
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.block.PushEvent;
import code.wiosvisual.events.item.SwingDurationEvent;
import code.wiosvisual.events.player.EntityDeathEvent;
import code.wiosvisual.events.player.EventOnTravelPost;
import code.wiosvisual.events.player.JumpEvent;
import code.wiosvisual.features.impl.render.NoRender;
import code.wiosvisual.features.impl.render.SwingAnimations;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.features.aura.warp.TurnsConstructor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LivingEntity.class})
public abstract class LivingEntityMixin {
    @Shadow
    public float bodyYaw;
    @Shadow
    private int handSwingTicks;
    @Shadow
    protected boolean handSwinging;

    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> var1);

    @Shadow
    @Nullable
    public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> var1);

    @Inject(method={"hasStatusEffect"}, at={@At(value="HEAD")}, cancellable=true)
    private void onHasStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || (Object)this != client.player) {
            return;
        }
        NoRender noRender = NoRender.getInstance();
        if (noRender == null || !noRender.shouldHideBadEffects()) {
            return;
        }
        StatusEffect status = (StatusEffect)effect.value();
        if (status == StatusEffects.NAUSEA || status == StatusEffects.BLINDNESS) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method={"getStuckArrowCount"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetStuckArrowCount(CallbackInfoReturnable<Integer> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideStuckArrows()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method={"getStingerCount"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetStingerCount(CallbackInfoReturnable<Integer> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideStuckArrows()) {
            cir.setReturnValue(0);
        }
    }

    @Shadow
    public abstract boolean isInSwimmingPose();

    @Inject(method={"isPushable"}, at={@At(value="HEAD")}, cancellable=true)
    public void isPushable(CallbackInfoReturnable<Boolean> infoReturnable) {
        PushEvent event = SafeEvents.dispatch(() -> new PushEvent(PushEvent.Type.COLLISION));
        if (event != null && event.isCancelled()) {
            infoReturnable.setReturnValue(false);
        }
    }

    @Redirect(method={"calcGlidingVelocity"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float hookModifyFallFlyingPitch(LivingEntity instance) {
        if ((Object)this != MinecraftClient.getInstance().player) {
            return instance.getPitch();
        }
        TurnsConnection rotationManager = TurnsConnection.INSTANCE;
        Turns rotation = rotationManager.getRotation();
        TurnsConstructor configurable = rotationManager.getCurrentRotationPlan();
        if (rotation == null || configurable == null || !configurable.isMoveCorrection() || configurable.isChangeLook()) {
            return instance.getPitch();
        }
        return rotation.getPitch();
    }

    @Redirect(method={"calcGlidingVelocity"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookModifyFallFlyingRotationVector(LivingEntity original) {
        if ((Object)this != MinecraftClient.getInstance().player) {
            return original.getRotationVector();
        }
        TurnsConnection rotationManager = TurnsConnection.INSTANCE;
        Turns rotation = rotationManager.getRotation();
        TurnsConstructor configurable = rotationManager.getCurrentRotationPlan();
        if (rotation == null || configurable == null || !configurable.isMoveCorrection() || configurable.isChangeLook()) {
            return original.getRotationVector();
        }
        return rotation.toVector();
    }

    @Inject(method={"jump"}, at={@At(value="HEAD")}, cancellable=true)
    private void jump(CallbackInfo info) {
        JumpEvent event;
        Object livingEntityMixin = this;
        if (livingEntityMixin instanceof ClientPlayerEntity && (event = SafeEvents.dispatch(() -> LivingEntityMixin.lambda$jump$1((ClientPlayerEntity)livingEntityMixin))) != null && event.isCancelled()) {
            info.cancel();
        }
    }

    @ModifyExpressionValue(method={"jump"}, at={@At(value="NEW", target="(DDD)Lnet/minecraft/util/math/Vec3d;")})
    private Vec3d hookFixRotation(Vec3d original) {
        if (MinecraftClient.getInstance().player == null || (Object)this != MinecraftClient.getInstance().player) {
            return original;
        }
        float yaw = TurnsConnection.INSTANCE.getMoveRotation().getYaw() * ((float)Math.PI / 180);
        return new Vec3d((double)(-MathHelper.sin((float)yaw) * 0.2f), 0.0, (double)(MathHelper.cos((float)yaw) * 0.2f));
    }

    @ModifyExpressionValue(method={"calcGlidingVelocity"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getPitch()F")})
    private float hookModifyFallFlyingPitch(float original) {
        if (MinecraftClient.getInstance().player == null || (Object)this != MinecraftClient.getInstance().player) {
            return original;
        }
        return TurnsConnection.INSTANCE.getMoveRotation().getPitch();
    }

    @ModifyExpressionValue(method={"calcGlidingVelocity"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")})
    private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
        if (MinecraftClient.getInstance().player == null || (Object)this != MinecraftClient.getInstance().player) {
            return original;
        }
        return TurnsConnection.INSTANCE.getMoveRotation().toVector();
    }

    @Inject(method={"calcGlidingVelocity"}, at={@At(value="RETURN")}, cancellable=true)
    private void onCalcGlidingVelocityReturn(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
        if (MinecraftClient.getInstance().player == null || (Object)this != MinecraftClient.getInstance().player) {
            return;
        }
        if (!EventManager.hasListeners(EventOnTravelPost.class)) {
            return;
        }
        EventOnTravelPost event = SafeEvents.dispatch(() -> new EventOnTravelPost((Vec3d)cir.getReturnValue()));
        if (event != null) {
            cir.setReturnValue(event.getOldVelocity());
        }
    }

    @Inject(method={"getHandSwingDuration"}, at={@At(value="HEAD")}, cancellable=true)
    private void swingProgressHook(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this != MinecraftClient.getInstance().player) {
            return;
        }
        SwingDurationEvent event = SafeEvents.dispatch(() -> new SwingDurationEvent());
        if (event != null && event.isCancelled()) {
            cir.setReturnValue(((int)event.getAnimation()));
        }
    }

    @Inject(method={"swingHand(Lnet/minecraft/util/Hand;)V"}, at={@At(value="HEAD")})
    private void forceSwingRestartOnRetrigger(Hand hand, CallbackInfo ci) {
        this.forceSwingRestartOnRetrigger();
    }

    @Inject(method={"swingHand(Lnet/minecraft/util/Hand;Z)V"}, at={@At(value="HEAD")})
    private void forceSwingRestartOnRetrigger(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        this.forceSwingRestartOnRetrigger();
    }

    @Unique
    private void forceSwingRestartOnRetrigger() {
        if (MinecraftClient.getInstance().player == null || (Object)this != MinecraftClient.getInstance().player) {
            return;
        }
        SwingAnimations swings = SwingAnimations.getInstance();
        if (swings == null || !swings.shouldRestartSwingOnRetrigger()) {
            return;
        }
        // Пока ломается блок, игра вызывает swingHand каждый тик. Сброс взмаха на каждом
        // вызове держал бы анимацию на первом кадре — рука стояла бы на месте. Во время
        // добычи оставляем ванильное правило: новый взмах после половины предыдущего.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
            return;
        }
        if (this.handSwinging) {
            this.handSwingTicks = -1;
        }
    }

    @Inject(method={"onDeath"}, at={@At(value="HEAD")})
    private void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        SafeEvents.dispatch(() -> new EntityDeathEvent((Entity)entity, source));
    }

    @Inject(method={"handleStatus"}, at={@At(value="HEAD")})
    private void handleStatus(byte status, CallbackInfo ci) {
        if (status == 3) {
            LivingEntity entity = (LivingEntity)(Object)this;
            SafeEvents.dispatch(() -> new EntityDeathEvent((Entity)entity, null));
        }
    }

    private static /* synthetic */ JumpEvent lambda$jump$1(ClientPlayerEntity player) {
        return new JumpEvent((PlayerEntity)player);
    }
}
