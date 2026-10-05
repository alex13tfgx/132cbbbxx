/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10209
 *  net.minecraft.class_1297
 *  net.minecraft.class_239
 *  net.minecraft.class_243
 *  net.minecraft.class_276
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_408
 *  net.minecraft.class_4587
 *  net.minecraft.class_6367
 *  net.minecraft.class_757
 *  net.minecraft.class_9779
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.display.hud.CustomHudOverlay;
import code.wiosvisual.events.render.AspectRatioEvent;
import code.wiosvisual.events.render.FovEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.impl.render.Ambience;
import code.wiosvisual.features.impl.render.HandShader;
import code.wiosvisual.features.impl.render.MotionBlur;
import code.wiosvisual.features.impl.render.NoHurtCam;
import code.wiosvisual.features.impl.render.NoRender;
import code.wiosvisual.features.impl.render.ShaderSky;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.display.render.shader.MotionBlurRenderer;
import code.wiosvisual.utils.display.render.shader.HandFx;
import code.wiosvisual.utils.display.render.shader.SaturationRenderer;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.utils.RaycastAngle;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={GameRenderer.class})
public abstract class GameRendererMixin {
    @Final
    @Shadow
    private MinecraftClient client;
    private Framebuffer wiosvisual$beforeHandFbo = null;
    private Framebuffer wiosvisual$afterHandFbo = null;

    @Shadow
    public abstract float getFarPlaneDistance();

    /** Ортопроекция для немедленного 2D-рендера мода (Rectangle/шрифты/текстуры) на z=0. */
    private static ProjectionMatrix2 wiosvisual$guiProjection;

    /*
     * 1.21.11: экраны и HUD рисуются в фазе "guiExtraction", где ортопроекции ещё нет —
     * GuiRenderer ставит её только в момент своей отложенной отрисовки. Немедленный рендер мода
     * (WiosShader/WiosRender) берёт Projection из RenderSystem, поэтому без этого всё уходило
     * за экран (чёрный экран в меню). Ставим ортопроекцию в пиксели GUI сразу перед extraction.
     */
    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V", shift=At.Shift.AFTER)})
    private void wiosvisual$setupGuiProjection(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (wiosvisual$guiProjection == null) {
            wiosvisual$guiProjection = new ProjectionMatrix2("wiosvisual gui", -1000.0f, 1000.0f, true);
        }
        Window window = this.client.getWindow();
        RenderSystem.setProjectionMatrix(
                wiosvisual$guiProjection.set((float)window.getFramebufferWidth() / (float)window.getScaleFactor(), (float)window.getFramebufferHeight() / (float)window.getScaleFactor()),
                ProjectionType.ORTHOGRAPHIC);
    }

    @Inject(method={"getBasicProjectionMatrix"}, at={@At(value="TAIL")}, cancellable=true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatioEvent aspectRatioEvent = SafeEvents.dispatch(() -> new AspectRatioEvent());
        if (aspectRatioEvent != null && aspectRatioEvent.isCancelled()) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.perspective(fovDegrees * ((float)Math.PI / 180), aspectRatioEvent.getRatio(), 0.05f, this.getFarPlaneDistance());
            cir.setReturnValue(matrix4f);
        }
    }

    @ModifyExpressionValue(method={"getFov"}, at={@At(value="INVOKE", target="Ljava/lang/Integer;intValue()I")})
    private int hookGetFov(int original) {
        FovEvent event = SafeEvents.dispatch(() -> new FovEvent());
        if (event != null && event.isCancelled()) {
            return event.getFov();
        }
        return original;
    }

    // 1.21.11: сразу после WorldRenderer.render — проекция мира ещё стоит, глубина не очищена.
    @Inject(method={"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", shift=At.Shift.AFTER)})
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
        WorldRenderEvent event;
        if (this.client.getEntityRenderDispatcher() == null || this.client.getEntityRenderDispatcher().camera == null) {
            return;
        }
        Matrix4f matrix4f = new Matrix4f().rotation(this.client.getEntityRenderDispatcher().camera.getRotation().conjugate(new org.joml.Quaternionf()));
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);
        matrixStack.translate(this.client.getEntityRenderDispatcher().camera.pos.negate());
        // Порт 1.21.11: RenderSystem.getProjectionMatrix() удалён; берём проекцию у GameRenderer.
        net.minecraft.client.render.GameRenderer wiosGr = (net.minecraft.client.render.GameRenderer)(Object)this;
        float wiosFov = wiosGr.getFov(this.client.getEntityRenderDispatcher().camera, tickCounter.getTickProgress(true), true);
        Render3D.setLastProjMat(wiosGr.getProjectionMatrix(wiosFov));
        Render3D.setLastWorldSpaceMatrix(matrixStack.peek());
        ShaderSky shaderSky = ShaderSky.getInstance();
        if (shaderSky != null && (shaderSky.isState())) {
            shaderSky.renderSkyShader();
        }
        if ((event = SafeEvents.dispatch(() -> new WorldRenderEvent(matrixStack, tickCounter.getTickProgress(false)))) != null) {
            Render3D.onWorldRender(event);
        }
    }

    @Inject(method={"tiltViewWhenHurt"}, at={@At(value="HEAD")}, cancellable=true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoHurtCam.shouldCancelTilt()) {
            ci.cancel();
            return;
        }
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("\u0422\u0440\u044f\u0441\u043a\u0430 \u044d\u043a\u0440\u0430\u043d\u0430")) {
            ci.cancel();
        }
    }

    @Inject(method={"bobView"}, at={@At(value="HEAD")}, cancellable=true)
    private void onBobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoHurtCam.shouldCancelShake()) {
            ci.cancel();
        }
    }

    /**
     * Размытие движения: сразу после отрисовки мира. Дальше ванилла очищает глубину
     * перед рукой (RenderSystem.clear), а без глубины репроекция невозможна.
     * Имена intermediary и remap=false: в проекте лежит готовый refmap, новых имён в нём нет.
     */
    @Inject(method={"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", shift=At.Shift.AFTER)})
    private void wiosvisual$motionBlur(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal=0) Matrix4f projection, @Local(ordinal=1) Matrix4f view) {
        MotionBlur motionBlur = MotionBlur.getInstance();
        if (motionBlur == null || !motionBlur.isState() || this.client.world == null) {
            return;
        }
        MotionBlurRenderer.render(projection, view, this.client.gameRenderer.getCamera().pos, motionBlur.blendFactor(), motionBlur.samples());
    }

    @Inject(method={"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/GameRenderer;renderHand(FZLorg/joml/Matrix4f;)V", shift=At.Shift.BEFORE)})
    private void captureBeforeHand(RenderTickCounter tickCounter, CallbackInfo ci) {
        HandShader handShader = HandShader.getInstance();
        if (handShader == null || !handShader.shouldRender() || this.client.getFramebuffer() == null) {
            return;
        }
        HandFx.captureBefore();
    }

    @Inject(method={"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/GameRenderer;renderHand(FZLorg/joml/Matrix4f;)V", shift=At.Shift.AFTER)})
    private void captureAfterHand(RenderTickCounter tickCounter, CallbackInfo ci) {
        HandShader handShader = HandShader.getInstance();
        if (handShader == null || !handShader.shouldRender() || this.client.getFramebuffer() == null) {
            return;
        }
        HandFx.captureAfter();
        handShader.renderCapturedEffect();
    }

    /** Насыщенность из Ambience: пост-обработка кадра мира до отрисовки HUD. */
    @Inject(method={"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"}, at={@At(value="TAIL")})
    private void wiosvisual$applySaturation(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.client.world == null || !Ambience.saturationActive()) {
            return;
        }
        SaturationRenderer.render(Ambience.saturationLevel(), Ambience.vibranceLevel());
    }

    private void wiosvisual$copyColorFramebuffer(Framebuffer src, Framebuffer dst) {
        // TODO(port 1.21.11): копирование фреймбуфера (method_1235/1233/1240) удалено из API;
        // пост-эффекты руки/огня застаблены, захват экрана отключён.
    }

    /**
     * Кастомный HUD рисуется в фазе "guiExtraction" (как и экраны/оверлеи), ПОКА ортопроекция
     * ещё валидна (см. wiosvisual$setupGuiProjection) и GPU render-pass ещё открыт — сразу
     * после того как ванильные экраны/тосты дописаны в lv4, но ДО настоящего флаша батч-рендера
     * GuiRenderer (class_11228#method_70890). Инжект после флаша (method_70879, просто счётчик
     * кадров) технически срабатывал, но render-pass к тому моменту уже закрыт — рисунки уходили
     * в никуда без единой ошибки. Пишется в intermediary: refmap устарел.
     */
    @Inject(method={"render(Lnet/minecraft/client/render/RenderTickCounter;Z)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.BEFORE)})
    private void wiosvisual$renderCustomHud(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
        if (!this.client.isFinishedLoading() || !tick || this.client.world == null) {
            return;
        }
        if (this.client.currentScreen instanceof ChatScreen) {
            return;
        }
        CustomHudOverlay.render(drawContext, tickCounter, true);
    }
}
