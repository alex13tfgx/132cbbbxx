/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1921
 *  net.minecraft.class_266
 *  net.minecraft.class_310
 *  net.minecraft.class_329
 *  net.minecraft.class_332
 *  net.minecraft.class_9779
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.misc.FastSwap;
import code.wiosvisual.features.impl.render.CrossHair;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.features.impl.render.NoRender;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={InGameHud.class})
public abstract class InGameHudMixin
implements QuickImports {
    @Final
    @Shadow
    private MinecraftClient client;

    @Unique
    private Hud getHud() {
        return Hud.getInstance();
    }

    @Shadow
    protected abstract void renderStatusBars(DrawContext var1);

    @Shadow
    protected abstract void renderMountHealth(DrawContext var1);

    @Inject(method={"renderCrosshair"}, at={@At(value="FIELD", target="Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;")}, cancellable=true)
    public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrossHair crossHair = CrossHair.getInstance();
        if (crossHair.isState()) {
            crossHair.onRenderCrossHair();
            ci.cancel();
        }
    }

    @Inject(at={@At(value="HEAD")}, method={"renderStatusEffectOverlay"}, cancellable=true)
    public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Hud hud = this.getHud();
        if (hud != null && hud.isState()) {
            if (Hud.interfaceSettings.isSelected("Potions")) {
                ci.cancel();
            }
        }
    }

    @Inject(method={"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideScoreboard()) {
            ci.cancel();
            return;
        }
        Hud hud = this.getHud();
        if (hud != null && hud.isState()) {
            if (Hud.interfaceSettings.isSelected("Score Board")) {
                ci.cancel();
            }
        }
    }

    @Inject(method={"renderVignetteOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideVignette()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderTitleAndSubtitle"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideTitles()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderNauseaOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderNauseaOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideBadEffects()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderPortalOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideBadEffects()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderOverlayMessage"}, at={@At(value="HEAD")}, cancellable=true)
    private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        block2: {
            block3: {
                Hud hud = this.getHud();
                if (hud == null || !hud.isState()) break block2;
                if (Hud.interfaceSettings.isSelected("HotBar")) break block3;
                if (!Hud.interfaceSettings.isSelected("CustomHotbar")) break block2;
            }
            ci.cancel();
        }
    }

    @Inject(method={"renderExperienceLevel"}, at={@At(value="HEAD")}, cancellable=true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        block2: {
            block3: {
                Hud hud = this.getHud();
                if (hud == null || !hud.isState()) break block2;
                if (Hud.interfaceSettings.isSelected("HotBar")) break block3;
                if (!Hud.interfaceSettings.isSelected("CustomHotbar")) break block2;
            }
            ci.cancel();
        }
    }

    /** Подписи кнопок FastSwap над предметами ванильного хотбара. */
    @Inject(method={"renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"}, at={@At(value="TAIL")})
    private void wiosvisual$fastSwapBindLabels(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.client.player == null) {
            return;
        }
        int left = context.getScaledWindowWidth() / 2 - 90;
        int top = context.getScaledWindowHeight() - 16 - 3;
        for (int i = 0; i < 9; ++i) {
            FastSwap.drawBindLabel(context, this.client.player.getInventory().getStack(i), left + i * 20 + 2 - 1, top);
        }
    }

    @Inject(method={"renderMainHud"}, at={@At(value="HEAD")}, cancellable=true)
    private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        block4: {
            block5: {
                Hud hud = this.getHud();
                if (hud == null || !hud.isState()) break block4;
                if (Hud.interfaceSettings.isSelected("HotBar")) break block5;
                if (!Hud.interfaceSettings.isSelected("CustomHotbar")) break block4;
            }
            context.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, InGameHud.HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 1, 1);
            if (this.client.interactionManager != null && this.client.interactionManager.hasStatusBars()) {
                this.renderStatusBars(context);
            }
            this.renderMountHealth(context);
            ci.cancel();
        }
    }
}
