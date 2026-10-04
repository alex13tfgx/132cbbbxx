package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.Ambience;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.ByteBuffer;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Кастомный туман Ambience: подменяет дальность и цвет в uniform-буфере тумана 1.21.11. */
@Mixin(value={FogRenderer.class})
public class FogRendererMixin {

    @WrapOperation(method={"applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")}, require=0)
    private void wiosvisual$customFog(FogRenderer self, ByteBuffer buffer, int offset, Vector4f color,
                                      float envStart, float envEnd, float renderStart, float renderEnd, float skyEnd, float cloudEnd,
                                      Operation<Void> original) {
        if (!Ambience.customFogActive()) {
            original.call(self, buffer, offset, color, envStart, envEnd, renderStart, renderEnd, skyEnd, cloudEnd);
            return;
        }
        float start = Ambience.customFogStart();
        float end = Ambience.customFogEnd();
        original.call(self, buffer, offset, Ambience.customFogColor(), start, end, start, end, end, end);
    }

    /** Цвет очистки кадра тоже меняем, чтобы небо на горизонте сливалось с туманом. */
    @ModifyReturnValue(method={"applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;"}, at={@At(value="RETURN")}, require=0)
    private Vector4f wiosvisual$fogClearColor(Vector4f original) {
        return Ambience.customFogActive() ? Ambience.customFogColor() : original;
    }
}
