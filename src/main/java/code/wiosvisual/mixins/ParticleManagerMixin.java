/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.class_2394
 *  net.minecraft.class_2398
 *  net.minecraft.class_702
 *  net.minecraft.class_703
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.NoRender;
import code.wiosvisual.features.impl.render.TotemParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ParticleManager.class})
public class ParticleManagerMixin {
    /** Защита от рекурсии: частицу тотема пересоздаём с изменённой скоростью через тот же метод. */
    private boolean wiosTotemRedirect;

    @Inject(method={"addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState()) {
            if (noRender.shouldHideSmoke() && (parameters.getType() == ParticleTypes.SMOKE || parameters.getType() == ParticleTypes.LARGE_SMOKE || parameters.getType() == ParticleTypes.CAMPFIRE_COSY_SMOKE || parameters.getType() == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE)) {
                cir.setReturnValue(null);
                return;
            }
            if (noRender.shouldHideAirBubbles() && (parameters.getType() == ParticleTypes.BUBBLE || parameters.getType() == ParticleTypes.BUBBLE_POP || parameters.getType() == ParticleTypes.CURRENT_DOWN)) {
                cir.setReturnValue(null);
                return;
            }
        }
        TotemParticles totem = TotemParticles.activeFor(parameters);
        if (totem == null || this.wiosTotemRedirect) {
            return;
        }
        if (totem.hide.isValue()) {
            cir.setReturnValue(null);
            return;
        }
        double speed = totem.speed.getValue();
        int copies = totem.rollCopies();
        Particle first = null;
        this.wiosTotemRedirect = true;
        try {
            // Количество = сколько частиц рождается на каждую ванильную: облако реально становится гуще или реже.
            for (int n = 0; n < copies; ++n) {
                double jitter = n == 0 ? 1.0 : 0.85 + Math.random() * 0.3;
                Particle particle = ((ParticleManager)(Object)this).addParticle(parameters, x, y, z, velocityX * speed * jitter, velocityY * speed * jitter, velocityZ * speed * jitter);
                totem.apply(particle);
                if (first == null) {
                    first = particle;
                }
            }
            cir.setReturnValue(first);
        }
        finally {
            this.wiosTotemRedirect = false;
        }
    }
}
