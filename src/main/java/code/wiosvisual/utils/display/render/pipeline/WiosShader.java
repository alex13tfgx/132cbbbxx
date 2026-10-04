package code.wiosvisual.utils.display.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BuiltBuffer;

/**
 * Отрисовка произвольной геометрии кастомным {@link RenderPipeline} с пользовательскими UBO.
 *
 * <p>До 1.21.6 кастомные шейдеры (core/round, arc, blur…) грузились как {@code ShaderProgram} и
 * получали uniform'ы поштучно через {@code shader.getUniform("name").set(...)}. В 1.21.6 это
 * удалено: uniform'ы передаются через std140-UBO, объявленные в пайплайне ({@code withUniform})
 * и привязанные в {@link RenderPass#setUniform}. Стандартный путь {@code RenderLayer.method_60895}
 * умеет привязывать только DynamicTransforms/Projection, поэтому для кастомных UBO нужен свой
 * проход — он реализован здесь по образцу {@code class_1921#method_60895}.
 *
 * <p>Собери UBO через {@link com.mojang.blaze3d.buffers.Std140Builder} и передай сюда карту
 * {@code имя блока -> байты}. Пайплайн должен объявлять эти же блоки через {@code withUniform}.
 */
public final class WiosShader {

    private WiosShader() {}

    /**
     * Рисует собранный буфер кастомным пайплайном.
     *
     * @param pipeline       кастомный {@link RenderPipeline} (формат/режим/блоки объявлены в нём)
     * @param built          собранная геометрия ({@code bufferBuilder.method_60800()})
     * @param customUniforms std140-данные пользовательских UBO по имени блока (может быть null)
     */
    public static void draw(RenderPipeline pipeline, BuiltBuffer built, Map<String, ByteBuffer> customUniforms) {
        draw(pipeline, built, customUniforms, null);
    }

    /**
     * Рисует собранный буфер кастомным пайплайном с привязкой текстур-сэмплеров.
     *
     * @param samplers имя сэмплера -> {@link GpuTextureView} (может быть null)
     */
    public static void draw(RenderPipeline pipeline, BuiltBuffer built, Map<String, ByteBuffer> customUniforms,
                            Map<String, GpuTextureView> samplers) {
        if (built == null) {
            return;
        }
        // DynamicTransforms из текущей модельно-видовой матрицы (ColorModulator = белый).
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
                RenderSystem.getModelViewMatrix(),
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                new Vector3f(),
                new Matrix4f());

        // Слайсы пользовательских UBO из переиспользуемого кольца (см. uboSlice) — без
        // создания/уничтожения отдельного GpuBuffer на каждый вызов (было главной причиной
        // фризов ClickGui: сотни draw call'ов в кадр, каждый — свой alloc+free GPU-объекта).
        Map<String, GpuBufferSlice> uboSlices = new HashMap<>();
        try {
            if (customUniforms != null) {
                for (Map.Entry<String, ByteBuffer> e : customUniforms.entrySet()) {
                    ByteBuffer padded = padStd140(e.getValue());
                    uboSlices.put(e.getKey(), uboSlice(e.getKey(), padded));
                }
                for (ByteBuffer b : PADDED) {
                    org.lwjgl.system.MemoryUtil.memFree(b);
                }
                PADDED.clear();
            }

            GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (built.getSortedBuffer() == null) {
                RenderSystem.ShapeIndexBuffer shared = RenderSystem.getSequentialBuffer(built.getDrawParameters().mode());
                indexBuffer = shared.getIndexBuffer(built.getDrawParameters().indexCount());
                indexType = shared.getIndexType();
            } else {
                indexBuffer = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.getSortedBuffer());
                indexType = built.getDrawParameters().indexType();
            }

            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
                    ? RenderSystem.outputColorTextureOverride : framebuffer.getColorAttachmentView();
            GpuTextureView depthView = framebuffer.useDepthAttachment
                    ? (RenderSystem.outputDepthTextureOverride != null
                        ? RenderSystem.outputDepthTextureOverride : framebuffer.getDepthAttachmentView())
                    : null;

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                    .createRenderPass(() -> "Wios custom shader draw", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                // Обрезка (ScissorAssist) — как в RenderLayer: иначе содержимое вылезает за панели.
                net.minecraft.client.gl.ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissor.isEnabled()) {
                    pass.enableScissor(scissor.getX(), scissor.getY(), scissor.getWidth(), scissor.getHeight());
                }
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                for (Map.Entry<String, GpuBufferSlice> e : uboSlices.entrySet()) {
                    pass.setUniform(e.getKey(), e.getValue());
                }
                if (samplers != null) {
                    for (Map.Entry<String, GpuTextureView> e : samplers.entrySet()) {
                        if (e.getValue() != null) {
                            // null-сэмплер в 1.21.11 снимает привязку текстуры, а не берёт дефолтный.
                            pass.bindTexture(e.getKey(), e.getValue(),
                                    RenderSystem.getSamplerCache().get(com.mojang.blaze3d.textures.FilterMode.LINEAR));
                        }
                    }
                }
                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(0, 0, built.getDrawParameters().indexCount(), 1);
            }
        } finally {
            built.close();
        }
    }

    /** Один переиспользуемый ring-buffer на имя UBO-блока (размер блока стабилен для имени). */
    private static final Map<String, DynamicUniformStorage<UboBytes>> UBO_POOLS = new HashMap<>();
    private static final Map<String, Integer> UBO_POOL_SIZES = new HashMap<>();

    private static GpuBufferSlice uboSlice(String name, ByteBuffer padded) {
        int size = padded.remaining();
        DynamicUniformStorage<UboBytes> pool = UBO_POOLS.get(name);
        Integer poolSize = UBO_POOL_SIZES.get(name);
        if (pool == null || poolSize == null || poolSize != size) {
            if (pool != null) {
                pool.close();
            }
            pool = new DynamicUniformStorage<>("wios ubo " + name, size, 8);
            UBO_POOLS.put(name, pool);
            UBO_POOL_SIZES.put(name, size);
        }
        return pool.write(new UboBytes(padded));
    }

    /** Раз в кадр отпускает слоты кольца — иначе счётчик записи растёт бесконечно (см. GameRendererMixin). */
    public static void endFrame() {
        for (DynamicUniformStorage<UboBytes> pool : UBO_POOLS.values()) {
            pool.clear();
        }
    }

    /**
     * Копия байтов на JVM-хипе, а не сам {@link ByteBuffer}: исходный буфер (из MemoryStack
     * или memCalloc в padStd140) освобождается сразу после вызова draw(), а class_11280
     * держит ПОСЛЕДНИЙ переданный элемент для дедупликации (сравнивает equals() со
     * следующим) — сравнение уже освобождённого нативного буфера было use-after-free.
     */
    private record UboBytes(byte[] data) implements DynamicUniformStorage.Uploadable {
        UboBytes(ByteBuffer source) {
            this(toArray(source));
        }

        private static byte[] toArray(ByteBuffer source) {
            ByteBuffer dup = source.duplicate();
            byte[] out = new byte[dup.remaining()];
            dup.get(out);
            return out;
        }

        @Override
        public void write(ByteBuffer target) {
            target.put(this.data);
        }
    }

    /**
     * Размер std140-блока драйвер округляет вверх до 16 байт (например, RoundData: 120 -> 128).
     * Привязка через glBindBufferRange диапазона меньше размера блока — недопустима, и draw call
     * молча отбрасывается (кастомные шейдеры ничего не рисовали). Дополняем нулями до кратного 16.
     */
    private static ByteBuffer padStd140(ByteBuffer data) {
        int size = data.remaining();
        int padded = (size + 15) & ~15;
        if (padded == size && size > 0) {
            return data;
        }
        ByteBuffer out = org.lwjgl.system.MemoryUtil.memCalloc(Math.max(padded, 16));
        out.put(data.duplicate()).clear();
        PADDED.add(out);
        return out;
    }

    /** Временные дополненные буферы: освобождаются после загрузки в GPU. */
    private static final java.util.List<ByteBuffer> PADDED = new java.util.ArrayList<>();
}
