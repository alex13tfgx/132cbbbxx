package code.wiosvisual.mixins;

import code.wiosvisual.emotions.ClientBadgeRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Рисует лого Wios слева от ника над головой игрока, если он тоже играет с Wios.
 */
@Mixin(value={EntityRenderer.class})
public abstract class EntityRendererNameBadgeMixin {
    @Unique
    private static final Identifier WIOS_BADGE = Identifier.of("mre", "wios_badge.png");
    @Unique
    private static final float BADGE_SIZE = 8.0f;
    @Unique
    private static final float BADGE_GAP = 2.0f;

    // renderLabelIfPresent: перед первым TextRenderer.draw матрица уже повернута к камере и отмасштабирована
    @Inject(method={"method_3926(Lnet/minecraft/class_10017;Lnet/minecraft/class_2561;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_327;method_27522(Lnet/minecraft/class_2561;FFIZLorg/joml/Matrix4f;Lnet/minecraft/class_4597;Lnet/minecraft/class_327$class_6415;II)I", ordinal=0)}, require=0)
    private void Wiosvisual$renderClientBadge(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci, @Local(ordinal=0) Matrix4f matrix) {
        // только основной ник игрока, а не строка со счётом под ним
        if (!(state instanceof PlayerEntityRenderState) || text != state.displayName) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return;
        }
        Entity entity = mc.world.getEntityById(((PlayerEntityRenderState)state).id);
        if (!(entity instanceof PlayerEntity) || !ClientBadgeRenderer.hasCompatibleClient((PlayerEntity)entity)) {
            return;
        }
        float textWidth = mc.textRenderer.getWidth(text);
        float x1 = -textWidth / 2.0f - 1.0f - BADGE_GAP;
        float x0 = x1 - BADGE_SIZE;
        float y0 = "deadmau5".equals(text.getString()) ? -10.0f : 0.0f;
        float y1 = y0 + BADGE_SIZE;
        boolean seeThrough = !state.sneaking;
        if (seeThrough) {
            // TODO(port 1.21.11): RenderLayer.getText/getTextSeeThrough (method_23030/23028) удалены — значок клиента не рисуется.
            // TODO(port 1.21.11): RenderLayer.getText/getTextSeeThrough (method_23030/23028) удалены — значок клиента не рисуется.
        } else {
            // TODO(port 1.21.11): RenderLayer.getText/getTextSeeThrough (method_23030/23028) удалены — значок клиента не рисуется.
        }
    }

    @Unique
    private static void drawBadge(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float x1, float y1, int alpha, int light) {
        buffer.vertex(matrix, x0, y0, 0.0f).color(255, 255, 255, alpha).texture(0.0f, 0.0f).light(light);
        buffer.vertex(matrix, x0, y1, 0.0f).color(255, 255, 255, alpha).texture(0.0f, 1.0f).light(light);
        buffer.vertex(matrix, x1, y1, 0.0f).color(255, 255, 255, alpha).texture(1.0f, 1.0f).light(light);
        buffer.vertex(matrix, x1, y0, 0.0f).color(255, 255, 255, alpha).texture(1.0f, 0.0f).light(light);
    }
}
