package code.wiosvisual.utils.display.render.draw;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.draw.DrawEngine;
import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4i;

public class DrawEngineImpl
implements DrawEngine,
QuickImports {
    @Override
    public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height) {
        buffer.vertex(matrix4f, x, y, 0.0f);
        buffer.vertex(matrix4f, x, y + height, 0.0f);
        buffer.vertex(matrix4f, x + width, y + height, 0.0f);
        buffer.vertex(matrix4f, x + width, y, 0.0f);
    }

    @Override
    public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color) {
        buffer.vertex(matrix4f, x, y, 0.0f).color(color);
        buffer.vertex(matrix4f, x, y + height, 0.0f).color(color);
        buffer.vertex(matrix4f, x + width, y + height, 0.0f).color(color);
        buffer.vertex(matrix4f, x + width, y, 0.0f).color(color);
    }

    @Override
    public void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix4f, x, y + height, 0.0f).color(color);
        buffer.vertex(matrix4f, x + width, y + height, 0.0f).color(color);
        buffer.vertex(matrix4f, x + width, y, 0.0f).color(color);
        buffer.vertex(matrix4f, x, y, 0.0f).color(color);
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, false, false), buffer);
    }

    @Override
    public void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        buffer.vertex(entry, x, y + height, 0.0f).texture(0.0f, 0.0f).color(color.x);
        buffer.vertex(entry, x + width, y + height, 0.0f).texture(0.0f, 1.0f).color(color.y);
        buffer.vertex(entry, x + width, y, 0.0f).texture(1.0f, 1.0f).color(color.w);
        buffer.vertex(entry, x, y, 0.0f).texture(1.0f, 0.0f).color(color.z);
    }
}
