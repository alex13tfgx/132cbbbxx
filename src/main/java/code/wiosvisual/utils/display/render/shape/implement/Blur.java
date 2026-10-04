package code.wiosvisual.utils.display.render.shape.implement;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.pipeline.RoundRenderer;
import code.wiosvisual.utils.display.render.shape.Shape;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Blur
implements Shape,
QuickImports {
    public Framebuffer input;
    public Vector2f resolution = new Vector2f();

    @Override
    public void render(ShapeProperties shape) {
        // TODO(port 1.21.11): исходный blur сэмплил снимок экрана (mre:core/blur + захват фреймбуфера
        // через удалённый class_276.method_1235). Bind фреймбуфера удалён, поэтому захват экрана нужно
        // переписать на новый framebuffer-copy API. Временный fallback: полупрозрачная тёмная скруглённая
        // панель вместо размытия фона.
        float scale = (float)mc.getWindow().getScaleFactor();
        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0f, new Vector3f()).mul(scale);
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y);
        float softness = shape.getSoftness();
        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX() - softness / 2.0f, shape.getY() - softness / 2.0f, shape.getWidth() + softness, shape.getHeight() + softness);
        int panel = 0x66101014; // полупрозрачный тёмный «фрост»
        RoundRenderer.draw(buffer,
                width, height,
                pos.x, (float)window.getHeight() - height - pos.y,
                round.x, round.y, round.z, round.w,
                0.0f, softness,
                panel, panel, panel, panel, 0,
                1.0f);
    }

    public void setup() {
        // TODO(port 1.21.11): захват экрана для blur отключён (см. render). Оставлено как no-op.
        this.resolution.set((float)mc.getWindow().getFramebufferWidth(), (float)mc.getWindow().getFramebufferHeight());
    }
}
