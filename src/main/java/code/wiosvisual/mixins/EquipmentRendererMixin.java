package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.HitColor;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Красит слои брони в цвет HitColor: сам цвет выставляет ArmorFeatureRendererMixin
 * прямо перед отрисовкой брони конкретного существа. В 1.21.11 слои ставятся в очередь
 * через RenderCommandQueue#submitModel — цвет и overlay подменяются там.
 */
@Mixin(value={EquipmentRenderer.class})
public class EquipmentRendererMixin {
    @Redirect(method={"render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V"},
            at=@At(value="INVOKE", target="Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private void wiosvisual$tintArmor(RenderCommandQueue queue, Model model, Object state, MatrixStack matrices, RenderLayer layer, int light, int overlay, int color, Sprite sprite, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        if (HitColor.isArmorTintActive()) {
            overlay = HitColor.tintArmorOverlay(overlay);
            color = HitColor.tintArmorColor(color);
        }
        queue.submitModel(model, state, matrices, layer, light, overlay, color, sprite, outlineColor, crumbling);
    }
}
