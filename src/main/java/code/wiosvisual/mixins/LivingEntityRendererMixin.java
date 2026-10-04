/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.class_10042
 *  net.minecraft.class_10055
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_1921
 *  net.minecraft.class_3532
 *  net.minecraft.class_4587
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_465
 *  net.minecraft.class_583
 *  net.minecraft.class_591
 *  net.minecraft.class_922
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.render.EntityColorEvent;
import code.wiosvisual.features.impl.render.BodyOutline;
import code.wiosvisual.features.impl.render.HitColor;
import code.wiosvisual.utils.display.render.esp.OutlineRenderer;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin
implements QuickImports {
    @Shadow
    protected EntityModel<?> model;

    @Shadow
    @Nullable
    protected abstract RenderLayer getRenderLayer(LivingEntityRenderState var1, boolean var2, boolean var3, boolean var4);

    /** Текстура существа — нужна, чтобы завести под ауру собственный полупрозрачный слой. */
    @Shadow
    public abstract Identifier getTexture(LivingEntityRenderState var1);

    @Redirect(method={"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        EntityColorEvent event;
        if (!translucent && state.width == 0.6f && (event = SafeEvents.dispatch(() -> new EntityColorEvent(-1))) != null && event.isCancelled()) {
            translucent = true;
        }
        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Inject(method={"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V")})
    private void Wiosvisual$hidePlayerModel(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        Entity entity;
        EntityModel<?> entityModel;
        PlayerEntityRenderState playerState;
        block6: {
            block5: {
                if (!(state instanceof PlayerEntityRenderState)) break block5;
                playerState = (PlayerEntityRenderState)state;
                entityModel = this.model;
                if (entityModel instanceof PlayerEntityModel) break block6;
            }
            return;
        }
        PlayerEntityModel playerModel = (PlayerEntityModel)entityModel;
        Entity entity2 = entity = LivingEntityRendererMixin.mc.world == null ? null : LivingEntityRendererMixin.mc.world.getEntityById(playerState.id);
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity)entity;
    }

    @Redirect(method={"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l, @Local(ordinal=0, argsOnly=true) LivingEntityRenderState renderState, @Local(ordinal=0, argsOnly=true) VertexConsumerProvider vertexConsumers) {
        EntityColorEvent event;
        LivingEntity tintTarget = LivingEntityRendererMixin.resolveLiving(renderState);
        if (tintTarget != null) {
            HitColor hitColor = HitColor.getInstance();
            if (hitColor != null && hitColor.shouldTint(tintTarget, renderState.hurt)) {
                instance.render(matrixStack, vertexConsumer, i, hitColor.getOverlay(j), hitColor.getTintColor(l));
                return;
            }
        }
        int color = l;
        if (renderState.invisibleToPlayer && (event = SafeEvents.dispatch(() -> new EntityColorEvent(l))) != null) {
            color = event.getColor();
        }
        instance.render(matrixStack, vertexConsumer, i, j, color);
        // Модель ставится в очередь обводки, а рисуется после выгрузки буфера сущностей.
        if (tintTarget != null && BodyOutline.shouldOutline((Entity)tintTarget)) {
            OutlineRenderer.enqueue(instance, matrixStack, BodyOutline.outlineColor(), BodyOutline.outlineThickness());
        }
    }

    /**
     * У PlayerEntityRenderState есть id, поэтому игрока находим по нему; для остальных существ
     * берём того, кого EntityRenderDispatcher рисует прямо сейчас.
     */
    @Unique
    private static LivingEntity resolveLiving(LivingEntityRenderState renderState) {
        if (LivingEntityRendererMixin.mc.world == null) {
            return null;
        }
        if (renderState instanceof PlayerEntityRenderState) {
            PlayerEntityRenderState playerState = (PlayerEntityRenderState)renderState;
            Entity byId = LivingEntityRendererMixin.mc.world.getEntityById(playerState.id);
            return byId instanceof LivingEntity ? (LivingEntity)byId : null;
        }
        Entity current = HitColor.getRenderedEntity();
        return current instanceof LivingEntity ? (LivingEntity)current : null;
    }

    /** Ванилла никогда не рисует свой ник — показываем его от третьего лица, чтобы было видно лого Wios. */
    @Inject(method={"hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void Wiosvisual$showOwnNameTag(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (entity != LivingEntityRendererMixin.mc.player || LivingEntityRendererMixin.mc.options.hudHidden || LivingEntityRendererMixin.mc.options.getPerspective().isFirstPerson() || entity.isInvisible()) {
            return;
        }
        cir.setReturnValue(true);
    }

    private void hidePlayerModel(PlayerEntityModel playerModel) {
        playerModel.head.visible = false;
        playerModel.hat.visible = false;
        playerModel.body.visible = false;
        playerModel.rightArm.visible = false;
        playerModel.leftArm.visible = false;
        playerModel.rightLeg.visible = false;
        playerModel.leftLeg.visible = false;
        playerModel.jacket.visible = false;
        playerModel.leftSleeve.visible = false;
        playerModel.rightSleeve.visible = false;
        playerModel.leftPants.visible = false;
        playerModel.rightPants.visible = false;
    }

    @ModifyExpressionValue(method={"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F")})
    private float lerpAngleDegreesHook(float original, @Local(ordinal=0, argsOnly=true) LivingEntity entity, @Local(ordinal=0, argsOnly=true) float delta) {
        if (LivingEntityRendererMixin.mc.player == null) {
            return original;
        }
        TurnsConnection controller = TurnsConnection.INSTANCE;
        if (entity.equals((Object)LivingEntityRendererMixin.mc.player) && controller.getPreviousRotation().getYaw() != LivingEntityRendererMixin.mc.player.getYaw() && !(LivingEntityRendererMixin.mc.currentScreen instanceof HandledScreen)) {
            return MathHelper.lerpAngleDegrees((float)delta, (float)controller.getPreviousRotation().getYaw(), (float)controller.getRotation().getYaw());
        }
        return original;
    }

    @ModifyExpressionValue(method={"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F")})
    private float getLerpedPitchHook(float original, @Local(ordinal=0, argsOnly=true) LivingEntity entity, @Local(ordinal=0, argsOnly=true) float delta) {
        if (LivingEntityRendererMixin.mc.player == null) {
            return original;
        }
        TurnsConnection controller = TurnsConnection.INSTANCE;
        if (entity.equals((Object)LivingEntityRendererMixin.mc.player) && controller.getPreviousRotation().getPitch() != LivingEntityRendererMixin.mc.player.getPitch() && !(LivingEntityRendererMixin.mc.currentScreen instanceof HandledScreen)) {
            return MathHelper.lerp((float)delta, (float)controller.getPreviousRotation().getPitch(), (float)controller.getRotation().getPitch());
        }
        return original;
    }
}
