/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10149
 *  net.minecraft.class_10156
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4587
 *  net.minecraft.class_5944
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package code.wiosvisual.utils.display.render;

import code.wiosvisual.utils.display.atlasfont.providers.ResourceProvider;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.pipeline.WiosShader;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.MemoryStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class HudShine
implements QuickImports {
    private static final float SWEEP_MS = 2200.0f;
    private static final float PAUSE_MS = 1100.0f;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("ShineData", UniformType.UNIFORM_BUFFER)
            .withVertexShader(net.minecraft.util.Identifier.of("mre", "core/hud_shine"))
            .withFragmentShader(net.minecraft.util.Identifier.of("mre", "core/hud_shine"))
            .withBlend(BlendFunction.ADDITIVE)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .withLocation(net.minecraft.util.Identifier.of("mre:pipeline/wios_hud_shine"))
            .build();

    private HudShine() {
    }

    public static void render(MatrixStack matrices, float x, float y, float w, float h, float round) {
        HudShine.render(matrices, x, y, w, h, round, 1.0f, 1.0f, 0.42f);
    }

    public static void render(MatrixStack matrices, float x, float y, float w, float h, float round, float sizeScale, float speedScale, float brightness) {
        if (w <= 2.0f || h <= 2.0f) {
            return;
        }
        double cycle = 3300.0;
        double now = (double)System.currentTimeMillis() * (double)speedScale;
        double t = now % cycle;
        if (t > 2200.0) {
            return;
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float scale = (float)window.getScaleFactor();
        Vector3f pos = matrix.transformPosition(x, y, 0.0f, new Vector3f()).mul(scale);
        Vector3f sz = matrix.getScale(new Vector3f()).mul(scale);
        float scaledWidth = w * sz.x;
        float scaledHeight = h * sz.y;
        float locationX = pos.x;
        float locationY = (float)window.getHeight() - scaledHeight - pos.y;
        float radius = Math.min(round, Math.min(w, h) * 0.5f) * sz.y;
        float m = 8.0f;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(matrix, x - m, y - m, 0.0f);
        buffer.vertex(matrix, x - m, y + h + m, 0.0f);
        buffer.vertex(matrix, x + w + m, y + h + m, 0.0f);
        buffer.vertex(matrix, x + w + m, y - m, 0.0f);
        BuiltBuffer built = buffer.endNullable();
        if (built == null) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer ubo = Std140Builder.onStack(stack, 48)
                    .putVec2(scaledWidth, scaledHeight)
                    .putVec2(locationX, locationY)
                    .putVec4(radius, radius, radius, radius)
                    .putFloat(0.2f * sizeScale)
                    .putFloat(brightness)
                    .putFloat((float)(now % (4.0 * cycle) / 1000.0))
                    .get();
            WiosShader.draw(PIPELINE, built, Map.of("ShineData", ubo));
        }
    }
}
