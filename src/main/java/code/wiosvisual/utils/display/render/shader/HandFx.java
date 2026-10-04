package code.wiosvisual.utils.display.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/**
 * Пост-эффекты руки от первого лица (Hand Shader, Ember Hands). Кадр копируется дважды:
 * до и после отрисовки руки. Разница двух кадров и есть маска руки, поэтому глубина не нужна.
 */
public final class HandFx {
    private static final int FLOATS = 24;
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);

    public static final RenderPipeline HAND_SHADER = build("hand_shader");

    private static GpuTexture beforeTexture;
    private static GpuTextureView beforeView;
    private static GpuTexture afterTexture;
    private static GpuTextureView afterView;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer data;
    private static int width;
    private static int height;
    private static boolean beforeValid;

    private HandFx() {}

    private static RenderPipeline build(String fragment) {
        return RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                        .withLocation(Identifier.of("wiosvisual", "pipeline/" + fragment))
                        .withVertexShader(Identifier.of("wiosvisual", "core/handfx"))
                        .withFragmentShader(Identifier.of("wiosvisual", "core/" + fragment))
                        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                        .withUniform("HandFxData", UniformType.UNIFORM_BUFFER)
                        .withSampler("Sampler0")
                        .withSampler("Sampler1")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(false)
                        .withCull(false)
                        .build());
    }

    private static boolean ready() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null || client.getFramebuffer().getColorAttachment() == null) {
            return false;
        }
        int w = client.getFramebuffer().textureWidth;
        int h = client.getFramebuffer().textureHeight;
        if (w <= 0 || h <= 0) {
            return false;
        }
        if (data == null) {
            data = MemoryUtil.memAlloc(FLOATS * 4);
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "wiosvisual:handfx_dummy", GpuBuffer.USAGE_VERTEX, dummy);
            MemoryUtil.memFree(dummy);
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "wiosvisual:handfx_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, FLOATS * 4);
        }
        if (beforeTexture == null || w != width || h != height) {
            closeTextures();
            beforeTexture = RenderSystem.getDevice().createTexture(() -> "wiosvisual:handfx_before", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, w, h, 1, 1);
            beforeView = RenderSystem.getDevice().createTextureView(beforeTexture);
            afterTexture = RenderSystem.getDevice().createTexture(() -> "wiosvisual:handfx_after", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, w, h, 1, 1);
            afterView = RenderSystem.getDevice().createTextureView(afterTexture);
            width = w;
            height = h;
        }
        return true;
    }

    /** Кадр прямо перед отрисовкой руки. */
    public static void captureBefore() {
        beforeValid = false;
        if (!ready()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(client.getFramebuffer().getColorAttachment(), beforeTexture, 0, 0, 0, 0, 0, width, height);
        beforeValid = true;
    }

    /** Кадр сразу после руки (или после предыдущего эффекта руки). */
    public static void captureAfter() {
        if (!beforeValid || !ready()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(client.getFramebuffer().getColorAttachment(), afterTexture, 0, 0, 0, 0, 0, width, height);
    }

    /** Накладывает эффект на главный кадр; params — 6 векторов vec4, описание в шейдерах. */
    public static void draw(RenderPipeline pipeline, float[] params) {
        if (!beforeValid || afterView == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        data.clear();
        for (int i = 0; i < FLOATS; i++) {
            data.putFloat(i < params.length ? params[i] : 0.0f);
        }
        data.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), data);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "wiosvisual:handfx_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.bindTexture("Sampler0", beforeView, sampler);
            pass.bindTexture("Sampler1", afterView, sampler);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setUniform("HandFxData", uniformBuffer);
            pass.draw(0, 6);
        }
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    private static void closeTextures() {
        if (beforeView != null) {
            beforeView.close();
            beforeView = null;
        }
        if (beforeTexture != null) {
            beforeTexture.close();
            beforeTexture = null;
        }
        if (afterView != null) {
            afterView.close();
            afterView = null;
        }
        if (afterTexture != null) {
            afterTexture.close();
            afterTexture = null;
        }
    }
}
