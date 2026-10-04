/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10149
 *  net.minecraft.class_10156
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4588
 *  net.minecraft.class_5944
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 */
package code.wiosvisual.utils.display.render.systemrender.renderers.impl;

import code.wiosvisual.utils.display.atlasfont.msdf.MsdfFont;
import code.wiosvisual.utils.display.atlasfont.providers.ColorProvider;
import code.wiosvisual.utils.display.atlasfont.providers.ResourceProvider;
import code.wiosvisual.utils.display.render.systemrender.renderers.IRenderer;
import code.wiosvisual.utils.display.render.pipeline.SystemRenderPipelines;
import code.wiosvisual.utils.display.render.pipeline.WiosShader;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryStack;
import org.joml.Matrix4f;

public record BuiltText(MsdfFont font, String text, float size, float thickness, int color, float smoothness, float spacing, int outlineColor, float outlineThickness, boolean rainbow, int rainbowColor1, int rainbowColor2) implements IRenderer
{

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        boolean outlineEnabled = this.outlineThickness > 0.0f;
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        if (this.rainbow) {
            this.font.applyRainbowGlyphs(matrix, (VertexConsumer)builder, this.text, this.size, (this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing, x, y + this.font.getMetrics().baselineHeight() * this.size, z, this.rainbowColor1, this.rainbowColor2);
        } else {
            this.font.applyGlyphs(matrix, (VertexConsumer)builder, this.text, this.size, (this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing, x, y + this.font.getMetrics().baselineHeight() * this.size, z, this.color);
        }
        BuiltBuffer built = builder.endNullable();
        if (built == null) {
            return;
        }
        float[] outlineComponents = outlineEnabled ? ColorProvider.normalize(this.outlineColor) : new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer ubo = Std140Builder.onStack(stack, 64)
                    .putFloat(this.font.getAtlas().range())
                    .putFloat(this.thickness)
                    .putFloat(this.smoothness)
                    .putInt(outlineEnabled ? 1 : 0)
                    .putFloat(this.outlineThickness)
                    .putVec4(outlineComponents[0], outlineComponents[1], outlineComponents[2], outlineComponents[3])
                    .get();
            WiosShader.draw(SystemRenderPipelines.MSDF, built, Map.of("MsdfData", ubo),
                    Map.of("Sampler0", this.font.getTextureView()));
        }
    }
}
