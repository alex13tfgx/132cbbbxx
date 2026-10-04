package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.Cosmetics;
import code.wiosvisual.features.impl.render.cosmetics.CosmeticCatalog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Накидка из Cosmetics подменяет текстуру плаща своего игрока. */
@Mixin(value={AbstractClientPlayerEntity.class})
public abstract class ClientPlayerCapeMixin {
    @Inject(method={"getSkin()Lnet/minecraft/entity/player/SkinTextures;"}, at={@At(value="RETURN")}, cancellable=true)
    private void Wiosvisual$applyCosmeticCape(CallbackInfoReturnable<SkinTextures> cir) {
        if ((Object)this != MinecraftClient.getInstance().player) {
            return;
        }
        Cosmetics module = Cosmetics.getInstance();
        CosmeticCatalog.Entry cape = module == null ? null : module.selected(CosmeticCatalog.Category.CAPE);
        SkinTextures skin = cir.getReturnValue();
        if (cape == null || skin == null) {
            return;
        }
        net.minecraft.util.Identifier texture = cape.capeTexture();
        if (texture == null) {
            return;
        }
        net.minecraft.util.AssetInfo.SkinAssetInfo asset = new net.minecraft.util.AssetInfo.SkinAssetInfo(texture, "");
        cir.setReturnValue(new SkinTextures(skin.body(), asset, asset, skin.model(), skin.secure()));
    }
}
