package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.ItemPhysic;
import java.util.WeakHashMap;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ItemEntityRenderer.class})
public abstract class ItemEntityRendererMixin {
    @Unique
    private static final WeakHashMap<ItemEntityRenderState, Boolean> wiosvisual$groundState = new WeakHashMap();
    @Unique
    private ItemEntityRenderState wiosvisual$currentState;

    @Inject(method={"updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V"}, at={@At(value="HEAD")})
    private void wiosvisual$captureGroundState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        wiosvisual$groundState.put(state, entity.isOnGround());
    }

    @Redirect(method={"method_3996(Lnet/minecraft/class_10039;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_4587;method_46416(FFF)V", ordinal=0))
    private void wiosvisual$redirectTranslate(MatrixStack matrices, float x, float y, float z, ItemEntityRenderState state, MatrixStack matricesArg, VertexConsumerProvider vertexConsumers, int light) {
        this.wiosvisual$currentState = state;
        ItemPhysic itemPhysic = ItemPhysic.getInstance();
        if (itemPhysic != null && itemPhysic.isState() && itemPhysic.isFlatOnGround()) {
            matrices.translate(x, itemPhysic.groundOffset.getValue(), z);
        } else {
            matrices.translate(x, y, z);
        }
    }

    @Redirect(method={"method_3996(Lnet/minecraft/class_10039;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/class_916;method_56858(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;ILnet/minecraft/class_10428;Lnet/minecraft/class_5819;)V"))
    private void wiosvisual$redirectRenderStack(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ItemStackEntityRenderState stackState, Random random) {
        ItemPhysic itemPhysic = ItemPhysic.getInstance();
        ItemEntityRenderState state = this.wiosvisual$currentState;
        if (itemPhysic != null && itemPhysic.isState() && state != null) {
            float age = state.age;
            float offset = state.uniqueOffset;
            boolean onGround = wiosvisual$groundState.getOrDefault(state, false);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-ItemEntity.getRotation(age, offset)));
            if (onGround && itemPhysic.isFlatOnGround()) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
            } else {
                float itemRotation = (age * itemPhysic.spinSpeed.getValue() + offset * 360.0f) % 360.0f;
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(itemRotation));
            }
        }
        // TODO(port 1.21.11): ItemEntityRenderer.renderStack теперь (class_4587,class_11659,int,class_10428,class_5819); физика предмета не перерисовывается.
    }
}
