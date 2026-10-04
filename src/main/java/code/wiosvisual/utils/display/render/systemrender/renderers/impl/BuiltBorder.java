package code.wiosvisual.utils.display.render.systemrender.renderers.impl;

import code.wiosvisual.utils.display.render.pipeline.SystemRenderPipelines;
import code.wiosvisual.utils.display.render.pipeline.WiosShader;
import code.wiosvisual.utils.display.render.systemrender.renderers.IRenderer;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadColorState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.SizeState;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public record BuiltBorder(SizeState size, QuadRadiusState radius, QuadColorState color, float thickness, float internalSmoothness, float externalSmoothness) implements IRenderer
{
    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        float width = this.size.width();
        float height = this.size.height();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());
        BuiltBuffer built = builder.endNullable();
        if (built == null) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer ubo = Std140Builder.onStack(stack, 64)
                    .putVec2(width, height)
                    .putVec4(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4())
                    .putFloat(this.thickness)
                    .putVec2(this.internalSmoothness, this.externalSmoothness)
                    .get();
            WiosShader.draw(SystemRenderPipelines.BORDER, built, Map.of("BorderData", ubo));
        }
    }
}
