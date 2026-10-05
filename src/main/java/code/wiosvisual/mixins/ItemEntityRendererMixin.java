package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.ItemPhysic;
import java.util.WeakHashMap;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemPhysic для 1.21.11: ItemEntityRenderer#render(state, matrices, queue, camera) делает
 * translate(подпрыгивание) → multiply(вращение по Y) → render(...). Подменяем оба шага:
 * на земле предмет лежит плашмя без подпрыгивания, в полёте крутится.
 */
@Mixin(value={ItemEntityRenderer.class})
public abstract class ItemEntityRendererMixin {
    @Unique
    private static final WeakHashMap<ItemEntityRenderState, Boolean> wiosvisual$groundState = new WeakHashMap<>();

    @Inject(method={"updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V"}, at={@At(value="HEAD")})
    private void wiosvisual$captureGroundState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        wiosvisual$groundState.put(state, entity.isOnGround());
    }

    @Unique
    private static boolean wiosvisual$onGround(ItemEntityRenderState state) {
        return wiosvisual$groundState.getOrDefault(state, false);
    }

    /** Плоский предмет (не блок): у модели почти нет толщины по Z. */
    @Unique
    private static boolean wiosvisual$flatModel(ItemEntityRenderState state) {
        return state.itemRenderState.getModelBoundingBox().getLengthZ() <= 0.0625;
    }

    @Redirect(method={"render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
            at=@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal=0))
    private void wiosvisual$translate(MatrixStack matrices, float x, float y, float z, ItemEntityRenderState state, MatrixStack m, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        ItemPhysic physic = ItemPhysic.getInstance();
        if (physic == null || !physic.isState()) {
            matrices.translate(x, y, z);
            return;
        }
        Box box = state.itemRenderState.getModelBoundingBox();
        float lift = physic.groundOffset.getValue();
        if (wiosvisual$onGround(state) && physic.isFlatOnGround()) {
            // плоский предмет после поворота на 90° лежит толщиной вверх — поднимаем на половину толщины
            float half = (float)box.getLengthZ() / 2.0f;
            matrices.translate(x, wiosvisual$flatModel(state) ? half + lift : (float)-box.minY + lift, z);
        } else {
            // без подпрыгивания: центр модели на высоте её половины
            matrices.translate(x, (float)-box.minY + lift, z);
        }
    }

    @Redirect(method={"render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
            at=@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V", ordinal=0))
    private void wiosvisual$rotate(MatrixStack matrices, Quaternionfc rotation, ItemEntityRenderState state, MatrixStack m, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        ItemPhysic physic = ItemPhysic.getInstance();
        if (physic == null || !physic.isState()) {
            matrices.multiply(rotation);
            return;
        }
        // постоянный «случайный» поворот вокруг Y у каждого предмета, без ванильного кручения
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.uniqueOffset * 57.2958f % 360.0f));
        if (wiosvisual$onGround(state) && physic.isFlatOnGround()) {
            if (wiosvisual$flatModel(state)) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
            }
        } else {
            float spin = (state.age * physic.spinSpeed.getValue() + state.uniqueOffset * 360.0f) % 360.0f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin));
        }
    }
}
