package code.wiosvisual.mixins;

import code.wiosvisual.utils.display.render.StartupBackgroundRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Paints the client background behind every out-of-world screen (options, resource packs, language
 * and so on) instead of the vanilla panorama. Ванильное размытие мира за внутриигровыми экранами
 * (инвентарь, чат, пауза) отключено — фон остаётся чётким.
 */
@Mixin(value={Screen.class})
public class ScreenBackgroundMixin {
    /** Screen#applyBlur — ванильный блюр мира за экранами; полностью отключаем. */
    @Inject(method={"applyBlur"}, at={@At(value="HEAD")}, cancellable=true)
    private void wiosvisual$noScreenBlur(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method={"renderDarkening(Lnet/minecraft/client/gui/DrawContext;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void wiosvisual$replacePanorama(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world != null) {
            return;
        }
        StartupBackgroundRenderer.drawBackground(context, 0.0f, 0.0f, (float)context.getScaledWindowWidth(), (float)context.getScaledWindowHeight(), 1.0f);
        ci.cancel();
    }
}
