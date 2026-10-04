package code.wiosvisual.utils.display.render.shape.implement;

import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.pipeline.WiosShader;
import code.wiosvisual.utils.display.render.shape.Shape;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public class Arc
implements Shape,
QuickImports {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("ArcData", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/arc")
            .withFragmentShader("core/arc")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .withLocation(net.minecraft.util.Identifier.of("minecraft:pipeline/wios_arc"))
            .build();

    @Override
    public void render(ShapeProperties shape) {
        float scale = (float)mc.getWindow().getScaleFactor();
        float alpha = 1.0f;
        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0f, new Vector3f()).mul(scale);
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y);
        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight());
        BuiltBuffer built = buffer.endNullable();
        if (built == null) {
            return;
        }
        int c1 = shape.getColor().x;
        int c2 = shape.getColor().y;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer ubo = Std140Builder.onStack(stack, 64)
                    .putVec4(ColorAssist.redf(c1), ColorAssist.greenf(c1), ColorAssist.bluef(c1), ColorAssist.alphaf(ColorAssist.multAlpha(c1, alpha)))
                    .putVec4(ColorAssist.redf(c2), ColorAssist.greenf(c2), ColorAssist.bluef(c2), ColorAssist.alphaf(ColorAssist.multAlpha(c2, alpha)))
                    .putVec2(width, height)
                    .putVec2(pos.x, (float)window.getHeight() - height - pos.y)
                    .putFloat(round.x)
                    .putFloat(shape.getThickness())
                    .putFloat(shape.getStart())
                    .putFloat(shape.getEnd())
                    .get();
            WiosShader.draw(PIPELINE, built, Map.of("ArcData", ubo));
        }
    }
}
