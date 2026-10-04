/*
 * Прячет ванильные хитбоксы (F3+B), пока включён HitboxCustomizer —
 * чтобы на экране оставалась только наша обводка.
 *
 * 1.21.11: хитбоксы рисует отдельный отладочный рендерер EntityHitboxDebugRenderer
 * (class_12155), по одной сущности в drawHitbox (method_75432). Старый
 * EntityRenderDispatcher#shouldRenderHitboxes (method_3958) удалён — прежний инжект
 * молча не применялся, и белые ванильные коробки рисовались поверх наших.
 * Имена в intermediary: refmap устарел.
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.HitboxCustomizer;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EntityHitboxDebugRenderer.class})
public abstract class HitboxSuppressMixin {
    @Inject(method={"drawHitbox(Lnet/minecraft/entity/Entity;FZ)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void Wiosvisual$hideVanillaHitbox(Entity entity, float tickDelta, boolean server, CallbackInfo ci) {
        if (HitboxCustomizer.suppressesVanillaHitboxes()) {
            ci.cancel();
        }
    }
}
