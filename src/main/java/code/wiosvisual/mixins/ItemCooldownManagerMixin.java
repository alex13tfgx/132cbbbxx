package code.wiosvisual.mixins;

import code.wiosvisual.utils.client.ItemCooldowns;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Запоминает полную длительность кд каждой группы, чтобы показывать остаток в слотах. */
@Mixin(ItemCooldownManager.class)
public class ItemCooldownManagerMixin {

    @Inject(method={"set(Lnet/minecraft/util/Identifier;I)V"}, at={@At(value="HEAD")})
    private void wiosvisual$onSet(Identifier group, int duration, CallbackInfo ci) {
        ItemCooldowns.onSet(group, duration);
    }

    @Inject(method={"remove(Lnet/minecraft/util/Identifier;)V"}, at={@At(value="HEAD")})
    private void wiosvisual$onRemove(Identifier group, CallbackInfo ci) {
        ItemCooldowns.onRemove(group);
    }
}
