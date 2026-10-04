package code.wiosvisual.utils.display.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/** Полноэкранный проход процедурного неба: рисуется после мира и только там, где глубина осталась 1.0. */
public class ShaderSkyPipeline {
    private static final Identifier PIPELINE_ID = Identifier.of("wiosvisual", "pipeline/shader_sky");
    private static final Identifier SHADER = Identifier.of("wiosvisual", "core/shader_sky");
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final int BUFFER_SIZE = 64 + 16 * 4;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("SkyData", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;

    private void ensureInitialized() {
        if (this.dataBuffer != null) return;
        this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);
        ByteBuffer dummy = MemoryUtil.memAlloc(4);
        dummy.putInt(0);
        dummy.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "wiosvisual:shader_sky_dummy", GpuBuffer.USAGE_VERTEX, dummy);
        MemoryUtil.memFree(dummy);
        this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "wiosvisual:shader_sky_uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, BUFFER_SIZE);
    }

    public void draw(Matrix4f invViewProj, float time, float intensity, float speed, float scale, float stars, float mode, int color1, int color2) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null) return;
        this.ensureInitialized();
        ru.mentions.utils.render.DrawBatcher.flushPending();

        this.dataBuffer.clear();
        invViewProj.get(this.dataBuffer);
        this.dataBuffer.position(64);
        this.dataBuffer.putFloat(time).putFloat(intensity).putFloat(speed).putFloat(scale);
        this.dataBuffer.putFloat(stars).putFloat(mode).putFloat(0f).putFloat(0f);
        this.putColor(color1);
        this.putColor(color2);
        this.dataBuffer.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);
        var dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass pass = encoder.createRenderPass(() -> "wiosvisual:shader_sky_pass",
                client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(), OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);
            pass.setVertexBuffer(0, this.dummyVertexBuffer);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setUniform("SkyData", this.uniformBuffer);
            pass.draw(0, 3);
        }
    }

    private void putColor(int c) {
        this.dataBuffer.putFloat(((c >> 16) & 0xFF) / 255.0f).putFloat(((c >> 8) & 0xFF) / 255.0f)
                .putFloat((c & 0xFF) / 255.0f).putFloat(1.0f);
    }
}
