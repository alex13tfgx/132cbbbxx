package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.TotemParticles;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * При настройке частиц «Минимум» ванилла не создаёт частицы тотема вовсе, и TotemParticles
 * нечего настраивать. Пока модуль включён, частицы тотема спавнятся принудительно.
 */
@Mixin(value={WorldRenderer.class})
public class WorldRendererParticleMixin {
    @Inject(method={"method_8563(Lnet/minecraft/class_2394;ZZDDDDDD)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void forceTotemParticles(ParticleEffect parameters, boolean force, boolean canSpawnOnMinimal, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (force) {
            return;
        }
        TotemParticles totem = TotemParticles.activeFor(parameters);
        if (totem == null || totem.hide.isValue()) {
            return;
        }
        ci.cancel();
    }
}
