package code.wiosvisual.mixins;

import code.wiosvisual.utils.display.render.pipeline.WiosShader;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Раз в кадр, в ТОЧНОЙ той же точке, где движок ротирует свой Dynamic Transforms UBO
 * (RenderSystem.flipFrame, сразу после смены буферов) — освобождает слоты кольца
 * WiosShader (см. WiosShader#endFrame). Делать это раньше в кадре опасно: часть кастомных
 * draw-вызовов этого же кадра уже могла получить слайсы из кольца.
 */
@Mixin(value = {RenderSystem.class})
public abstract class RenderSystemFrameMixin {
    @Inject(method = "flipFrame", at = {@At(value = "TAIL")})
    private static void wiosvisual$endFrame(CallbackInfo ci) {
        WiosShader.endFrame();
    }
}
