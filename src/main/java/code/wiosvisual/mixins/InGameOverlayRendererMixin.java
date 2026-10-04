/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1058
 *  net.minecraft.class_310
 *  net.minecraft.class_3486
 *  net.minecraft.class_3610
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_4603
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={InGameOverlayRenderer.class})
public class InGameOverlayRendererMixin {
    @Inject(method={"renderFireOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void renderFireOverlayHook(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        FluidState fluidState;
        MinecraftClient client;
        NoRender noRender = NoRender.getInstance();
        if (noRender == null || !noRender.isState()) {
            return;
        }
        if (noRender.modeSetting.isSelected("\u041e\u0433\u043e\u043d\u044c")) {
            ci.cancel();
            return;
        }
        if (noRender.shouldHideLavaOverlay() && (client = MinecraftClient.getInstance()) != null && client.player != null && (fluidState = client.player.world.getFluidState(client.player.getBlockPos())).isIn(FluidTags.LAVA)) {
            ci.cancel();
        }
    }

    @Inject(method={"renderInWallOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void renderInWallOverlayHook(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u0431\u043b\u043e\u043a\u043e\u0432")) {
            ci.cancel();
        }
    }

    @Inject(method={"renderUnderwaterOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void renderUnderwaterOverlayHook(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender == null || !noRender.isState() || client == null || client.player == null) {
            return;
        }
        FluidState fluidState = client.player.world.getFluidState(client.player.getBlockPos().up());
        if (fluidState.isIn(FluidTags.WATER) && noRender.shouldHideWaterOverlay()) {
            ci.cancel();
            return;
        }
        if (fluidState.isIn(FluidTags.LAVA) && noRender.shouldHideLavaOverlay()) {
            ci.cancel();
        }
    }
}
