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
import net.minecraft.client.texture.AbstractTexture;
import java.util.HashMap;
import java.lang.ref.WeakReference;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public record BuiltTexture(SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float u, float v, float texWidth, float texHeight, int textureId) implements IRenderer
{
    /** Текстуры по id: вызывающий код передаёт {@link #id(class_1044)} вместо сырого GL-id (его больше нет). */
    private static final Map<Integer, WeakReference<AbstractTexture>> TEXTURES = new HashMap<>();

    public static int id(AbstractTexture texture) {
        int id = System.identityHashCode(texture);
        TEXTURES.put(id, new WeakReference<>(texture));
        return id;
    }

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        WeakReference<AbstractTexture> ref = TEXTURES.get(this.textureId);
        AbstractTexture texture = ref == null ? null : ref.get();
        GpuTextureView view;
        try {
            view = texture == null ? null : texture.getGlTextureView();
        } catch (Throwable t) {
            view = null;
        }
        if (view == null) {
            return;
        }
        float width = this.size.width();
        float height = this.size.height();
        float u0 = this.u;
        float v0 = this.v;
        float u1 = this.u + this.texWidth;
        float v1 = this.v + this.texHeight;
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, x, y, z).texture(u0, v0).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).texture(u0, v1).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).texture(u1, v1).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).texture(u1, v0).color(this.color.color4());
        BuiltBuffer built = builder.endNullable();
        if (built == null) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer ubo = Std140Builder.onStack(stack, 64)
                    .putVec2(width, height)
                    .putVec4(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4())
                    .putFloat(this.smoothness)
                    .get();
            WiosShader.draw(SystemRenderPipelines.TEXTURE, built, Map.of("TexData", ubo), Map.of("Sampler0", view));
        }
    }
}
