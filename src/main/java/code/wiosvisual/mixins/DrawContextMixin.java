/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_5481
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.display.hud.CoolDowns;
import code.wiosvisual.display.hud.HudTheme;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.chat.StringHelper;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.GuiRenderState;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={DrawContext.class})
public class DrawContextMixin {
    @Inject(method={"drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V"}, at={@At(value="HEAD")})
    private void wiosvisual$beforeVanillaTextString(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        GuiRenderState.restoreFontAtlasesNearest();
    }

    @Inject(method={"drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"}, at={@At(value="HEAD")})
    private void wiosvisual$beforeVanillaText(TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        GuiRenderState.restoreFontAtlasesNearest();
    }

    @Inject(method={"drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V"}, at={@At(value="HEAD")})
    private void wiosvisual$beforeVanillaOrderedText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        GuiRenderState.restoreFontAtlasesNearest();
    }

    @Inject(method={"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"}, at={@At(value="HEAD")})
    private void wiosvisual$beforeVanillaShadowString(TextRenderer textRenderer, String text, int x, int y, int color, CallbackInfo ci) {
        GuiRenderState.restoreFontAtlasesNearest();
    }

    @Inject(method={"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"}, at={@At(value="HEAD")})
    private void wiosvisual$beforeVanillaShadowText(TextRenderer textRenderer, Text text, int x, int y, int color, CallbackInfo ci) {
        GuiRenderState.restoreFontAtlasesNearest();
    }

    @Inject(method={"drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V"}, at={@At(value="HEAD")})
    private void wiosvisual$beforeVanillaShadowOrderedText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, CallbackInfo ci) {
        GuiRenderState.restoreFontAtlasesNearest();
    }

    @Inject(method={"drawTextWithBackground(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIII)V"}, at={@At(value="HEAD")})
    private void wiosvisual$beforeVanillaTextBackground(TextRenderer textRenderer, Text text, int x, int y, int width, int color, CallbackInfo ci) {
        GuiRenderState.restoreFontAtlasesNearest();
    }

    /**
     * Перезарядка предмета прямо в слоте: хук общий для хотбара и всех
     * экранов-контейнеров, потому что оверлей слота рисуется одним методом.
     */
    @Inject(method={"drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"}, at={@At(value="RETURN")})
    private void wiosvisual$drawSlotCooldown(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        Hud hud;
        if (!Hud.slotCooldowns.isValue() || (hud = Hud.getInstance()) == null || !hud.isState()) {
            return;
        }
        int seconds = CoolDowns.getRemainingSeconds(stack);
        if (seconds <= 0) {
            return;
        }
        String text = seconds >= 60 ? StringHelper.getDuration(seconds) : String.valueOf(seconds);
        DrawContext context = (DrawContext)((Object)this);
        org.joml.Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(0.0f, 0.0f);
        context.drawCenteredTextWithShadow(textRenderer, text, x + 8, y + 5, DrawContextMixin.wiosvisual$cooldownColor(seconds));
        matrices.popMatrix();
    }

    /**
     * Цвет числа перезарядки в слоте: чем меньше остаток, тем «зеленее».
     * 0с — зелёный, дальше жёлтый и оранжевый, 30с и больше — красный.
     */
    @Unique
    private static int wiosvisual$cooldownColor(int seconds) {
        float t = Math.min(1.0f, Math.max(0.0f, (float)seconds / 30.0f));
        if (t < 0.34f) {
            return ColorAssist.interpolate(ColorAssist.green, ColorAssist.yellow, t / 0.34f);
        }
        if (t < 0.67f) {
            return ColorAssist.interpolate(ColorAssist.yellow, ColorAssist.orange, (t - 0.34f) / 0.33f);
        }
        return ColorAssist.interpolate(ColorAssist.orange, ColorAssist.red, (t - 0.67f) / 0.33f);
    }

}
