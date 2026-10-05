/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_156
 *  net.minecraft.class_1921
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_425
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.utils.display.render.StartupBackgroundRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={SplashOverlay.class})
public abstract class SplashOverlayMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private boolean reloading;
    @Shadow
    private long reloadCompleteTime;
    @Shadow
    private long reloadStartTime;
    @Shadow
    private float progress;

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void onSplashRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        float opacity = this.wiosvisual$getOverlayOpacity();
        if (opacity <= 0.01f) {
            return;
        }
        StartupBackgroundRenderer.drawLoader(context, context.getScaledWindowWidth(), context.getScaledWindowHeight(), this.progress, opacity);
    }

    @Unique
    private float wiosvisual$getOverlayOpacity() {
        float fadeIn;
        long now = Util.getMeasuringTimeMs();
        float fadeOut = this.reloadCompleteTime > -1L ? (float)(now - this.reloadCompleteTime) / 1000.0f : -1.0f;
        float f = fadeIn = this.reloadStartTime > -1L ? (float)(now - this.reloadStartTime) / 500.0f : -1.0f;
        if (fadeOut >= 1.0f) {
            return 1.0f - MathHelper.clamp((float)(fadeOut - 1.0f), (float)0.0f, (float)1.0f);
        }
        if (this.reloading) {
            return MathHelper.clamp((float)fadeIn, (float)0.0f, (float)1.0f);
        }
        return 1.0f;
    }

    /** Логотип Mojang Studios не рисуем. */
    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"), require=0)
    private void skipMojangLogoDraw(DrawContext context, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        if (SplashOverlay.LOGO.equals((Object)texture)) {
            return;
        }
        context.drawTexture(pipeline, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight, color);
    }

    /** Фирменная красная заливка Mojang и ванильная полоса прогресса пропускаются: остаётся чёрно-белый лоадер Wios. */
    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), require=0)
    private void skipBrandFill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
    }
}
