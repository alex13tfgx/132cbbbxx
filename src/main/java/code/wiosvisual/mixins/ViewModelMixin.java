package code.wiosvisual.mixins;

import code.wiosvisual.events.item.HandOffsetEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ViewModel для 1.21.11: тот же HandOffsetEvent, что раньше слал старый миксин, но по новой сигнатуре
 * renderFirstPersonItem (очередь отрисовки class_11659 вместо VertexConsumerProvider).
 */
@Mixin(value={HeldItemRenderer.class})
public class ViewModelMixin {
    @Inject(method={"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"},
            at={@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;push()V", shift=At.Shift.AFTER, ordinal=0)}, require=0)
    private void wiosvisual$handOffset(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack,
                                       float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        SafeEvents.dispatch(() -> new HandOffsetEvent(matrices, stack, hand, player, swingProgress, equipProgress, tickDelta, arm));
        float handScale = code.wiosvisual.features.impl.render.AspectRatio.handScaleX();
        if (handScale != 1.0f) {
            matrices.scale(handScale, 1.0f, 1.0f);
        }
    }
}
