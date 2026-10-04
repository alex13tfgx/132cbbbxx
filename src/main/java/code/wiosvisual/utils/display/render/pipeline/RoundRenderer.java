package code.wiosvisual.utils.display.render.pipeline;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;

/**
 * Отрисовка скруглённого прямоугольника (шейдер core/round) на новом рендер-конвейере 1.21.11.
 *
 * <p>Кастомные uniform'ы (цвета углов, радиусы, размеры, обводка) передаются через std140-UBO
 * {@code RoundData}, объявленный в {@link #PIPELINE} и заполняемый {@link Std140Builder}.
 */
public final class RoundRenderer {

    private RoundRenderer() {}

    /** Размер блока RoundData в байтах (std140): 6*vec4 + 2*vec2 + 2*float, с запасом до 128. */
    private static final int ROUND_UBO_SIZE = 128;

    public static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("RoundData", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/round")
            .withFragmentShader("core/round")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .withLocation(net.minecraft.util.Identifier.of("minecraft:pipeline/wios_round"))
            .build();

    /**
     * Рисует один скруглённый прямоугольник. Геометрия уже собрана в {@code buffer}
     * (четыре вершины POSITION в системе координат экрана), остальное — параметры шейдера.
     * Цвета — упакованные ARGB; {@code alpha} — общий множитель прозрачности (замена
     * удалённого {@code RenderSystem.getShaderColor()[3]}).
     */
    public static void draw(BufferBuilder buffer,
                            float sizeX, float sizeY,
                            float locX, float locY,
                            float rx, float ry, float rz, float rw,
                            float thickness, float softness,
                            int color1, int color2, int color3, int color4, int outlineColor,
                            float alpha) {
        BuiltBuffer built = buffer.endNullable();
        if (built == null) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder b = Std140Builder.onStack(stack, ROUND_UBO_SIZE);
            putColor(b, color1, alpha);
            putColor(b, color2, alpha);
            putColor(b, color3, alpha);
            putColor(b, color4, alpha);
            putColor(b, outlineColor, alpha);
            b.putVec4(rx, ry, rz, rw)
                    .putVec2(sizeX, sizeY)
                    .putVec2(locX, locY)
                    .putFloat(thickness)
                    .putFloat(softness);
            WiosShader.draw(PIPELINE, built, Map.of("RoundData", b.get()));
        }
    }

    private static void putColor(Std140Builder b, int argb, float alpha) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float bl = (argb & 0xFF) / 255f;
        float a = ((argb >>> 24) & 0xFF) / 255f * alpha;
        b.putVec4(r, g, bl, a);
    }
}
