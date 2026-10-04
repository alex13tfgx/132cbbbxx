package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.Ambience;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Клиентская подмена погоды для модуля Ambience: перехватывает силу дождя
 * ({@code getRainGradient}) и грозы ({@code getThunderGradient}) у мира.
 */
@Mixin(value={World.class})
public class WorldWeatherMixin {
    @Inject(method={"getRainGradient"}, at={@At(value="RETURN")}, cancellable=true)
    private void wiosvisual$rainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (!Ambience.overridesWeather()) {
            return;
        }
        cir.setReturnValue(Float.valueOf(Ambience.rainGradient(cir.getReturnValueF())));
    }

    @Inject(method={"getThunderGradient"}, at={@At(value="RETURN")}, cancellable=true)
    private void wiosvisual$thunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (!Ambience.overridesWeather()) {
            return;
        }
        cir.setReturnValue(Float.valueOf(Ambience.thunderGradient(cir.getReturnValueF())));
    }
}
