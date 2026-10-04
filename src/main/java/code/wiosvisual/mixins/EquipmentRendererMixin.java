package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.HitColor;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Красит слои брони в цвет HitColor: сам цвет выставляет ArmorFeatureRendererMixin
 * прямо перед отрисовкой брони конкретного игрока.
 */
@Mixin(value={EquipmentRenderer.class})
public class EquipmentRendererMixin {
    @Redirect(method={"render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void wiosvisual$tintArmor(Model model, MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        if (HitColor.isArmorTintActive()) {
            model.render(matrices, vertexConsumer, light, HitColor.tintArmorOverlay(overlay), HitColor.tintArmorColor(color));
            return;
        }
        model.render(matrices, vertexConsumer, light, overlay, color);
    }
}
