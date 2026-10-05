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
import net.minecraft.client.model.Model;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import code.wiosvisual.utils.display.render.state.ILivingStateEntity;
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

    private static final String RENDER = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V";
    private static final String SUBMIT_MODEL = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V";

    @Redirect(method={RENDER}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        EntityColorEvent event;
        if (!translucent && state.width == 0.6f && (event = SafeEvents.dispatch(() -> new EntityColorEvent(-1))) != null && event.isCancelled()) {
            translucent = true;
        }
        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    /** 1.21.11: модель не рисуется сразу, а ставится в очередь submitModel — красим её там (HitColor, цвет невидимок). */
    @Redirect(method={RENDER}, at=@At(value="INVOKE", target=SUBMIT_MODEL))
    private void renderModelHook(OrderedRenderCommandQueue queue, Model model, Object stateObj, MatrixStack matrixStack, RenderLayer layer, int light, int overlay, int color, Sprite sprite, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        LivingEntityRenderState renderState = (LivingEntityRenderState)stateObj;
        LivingEntity tintTarget = ILivingStateEntity.of(renderState);
        if (tintTarget != null) {
            HitColor hitColor = HitColor.getInstance();
            if (hitColor != null && hitColor.shouldTint(tintTarget, renderState.hurt)) {
                queue.submitModel(model, stateObj, matrixStack, layer, light, hitColor.getOverlay(overlay), hitColor.getTintColor(color), sprite, outlineColor, crumbling);
                return;
            }
        }
        EntityColorEvent event;
        int baseColor = color;
        if (renderState.invisibleToPlayer && (event = SafeEvents.dispatch(() -> new EntityColorEvent(baseColor))) != null) {
            color = event.getColor();
        }
        queue.submitModel(model, stateObj, matrixStack, layer, light, overlay, color, sprite, outlineColor, crumbling);
        if (tintTarget != null && model instanceof EntityModel<?> entityModel && BodyOutline.shouldOutline((Entity)tintTarget)) {
            OutlineRenderer.enqueue(entityModel, matrixStack, BodyOutline.outlineColor(), BodyOutline.outlineThickness());
        }
    }

    /** Запоминаем на render-state, из какой сущности он собран (нужно HitColor и броне). */
    @Inject(method={"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"}, at={@At(value="TAIL")})
    private void wiosvisual$linkEntity(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof ILivingStateEntity link) {
            link.wiosvisual$setEntity(entity);
        }
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
