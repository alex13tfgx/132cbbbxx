package code.wiosvisual.utils.display.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
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
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

/**
 * Размытие движения камеры для 1.21.11 (GPU-API RenderPass, как {@link HandFx}).
 * Вызывается сразу после WorldRenderer.render — пока в главном фреймбуфере глубина мира.
 * Кадр копируется в текстуру, затем полноэкранный проход усредняет его вдоль скорости пикселя,
 * вычисленной репроекцией глубины матрицами текущего и прошлого кадра.
 */
public final class MotionBlurRenderer {
    /** mat4 InvCurr + mat4 PrevVP + vec4 Params (std140). */
    private static final int BYTES = 16 * 4 * 2 + 4 * 4;
    /** Скачок камеры больше этого (телепорт, смерть, смена мира) — кадр без размытия. */
    private static final double MAX_JUMP = 8.0;

    private static RenderPipeline pipeline;
    private static GpuTexture colorTexture;
    private static GpuTextureView colorView;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer data;
    private static int width;
    private static int height;

    private static boolean hasPrev;
    private static final Matrix4f prevViewProj = new Matrix4f();
    private static Vec3d prevCamPos = Vec3d.ZERO;

    private MotionBlurRenderer() {}

    /** Забыть прошлый кадр (включение/выключение модуля, смена мира). */
    public static void reset() {
        hasPrev = false;
    }

    private static RenderPipeline pipeline() {
        if (pipeline == null) {
            pipeline = RenderPipelines.register(
                    RenderPipeline.builder()
                            .withLocation(Identifier.of("wiosvisual", "pipeline/motionfx"))
                            .withVertexShader(Identifier.of("wiosvisual", "core/handfx"))
                            .withFragmentShader(Identifier.of("wiosvisual", "core/motionfx"))
                            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                            .withUniform("MotionBlurData", UniformType.UNIFORM_BUFFER)
                            .withSampler("Sampler0")
                            .withSampler("Sampler1")
                            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                            .withDepthWrite(false)
                            .withCull(false)
                            .build());
        }
        return pipeline;
    }

    private static boolean ready(MinecraftClient client) {
        if (client.getFramebuffer() == null || client.getFramebuffer().getColorAttachment() == null
                || client.getFramebuffer().getDepthAttachmentView() == null) {
            return false;
        }
        int w = client.getFramebuffer().textureWidth;
        int h = client.getFramebuffer().textureHeight;
        if (w <= 0 || h <= 0) {
            return false;
        }
        if (data == null) {
            data = MemoryUtil.memAlloc(BYTES);
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0);
            dummy.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "wiosvisual:motionfx_dummy", GpuBuffer.USAGE_VERTEX, dummy);
            MemoryUtil.memFree(dummy);
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "wiosvisual:motionfx_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, BYTES);
        }
        if (colorTexture == null || w != width || h != height) {
            if (colorView != null) {
                colorView.close();
            }
            if (colorTexture != null) {
                colorTexture.close();
            }
            colorTexture = RenderSystem.getDevice().createTexture(() -> "wiosvisual:motionfx_color", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, w, h, 1, 1);
            colorView = RenderSystem.getDevice().createTextureView(colorTexture);
            width = w;
            height = h;
        }
        return true;
    }

    /**
     * @param projection   проекция мира этого кадра (с покачиванием камеры)
     * @param view         поворот вида (локальная matrix4f2 в GameRenderer#renderWorld)
     * @param cameraPos    позиция камеры
     * @param strength     1.0 — полное смещение за кадр
     * @param samples      число выборок вдоль движения
     */
    public static void render(Matrix4f projection, Matrix4f view, Vec3d cameraPos, float strength, int samples) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !ready(client)) {
            return;
        }
        // Камера-относительная схема, как у ванильного рендера мира: вид = только поворот.
        Matrix4f viewProj = new Matrix4f(projection).mul(view);
        if (!hasPrev || cameraPos.squaredDistanceTo(prevCamPos) > MAX_JUMP * MAX_JUMP) {
            prevViewProj.set(viewProj);
            prevCamPos = cameraPos;
            hasPrev = true;
            return;
        }
        Vec3d delta = cameraPos.subtract(prevCamPos);
        Matrix4f prev = new Matrix4f(prevViewProj).translate((float)delta.x, (float)delta.y, (float)delta.z);
        Matrix4f invCurr = new Matrix4f(viewProj).invert();
        prevViewProj.set(viewProj);
        prevCamPos = cameraPos;

        data.clear();
        invCurr.get(0, data);
        prev.get(64, data);
        data.putFloat(128, strength);
        data.putFloat(132, Math.max(2, Math.min(100, samples)));
        data.putFloat(136, width);
        data.putFloat(140, height);
        data.position(0).limit(BYTES);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(client.getFramebuffer().getColorAttachment(), colorTexture, 0, 0, 0, 0, 0, width, height);
        encoder.writeToBuffer(uniformBuffer.slice(), data);
        try (RenderPass pass = encoder.createRenderPass(
                () -> "wiosvisual:motionfx_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty())) {
            pass.setPipeline(pipeline());
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.bindTexture("Sampler0", colorView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.bindTexture("Sampler1", client.getFramebuffer().getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            pass.setUniform("MotionBlurData", uniformBuffer);
            pass.draw(0, 6);
        }
    }
}
