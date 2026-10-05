package code.wiosvisual.utils.display.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;

/**
 * Совместимый рендер-слой для 1.21.11.
 *
 * <p>До 1.21.6 проект рисовал через immediate-mode: {@code RenderSystem.setShader(...)} +
 * ручное состояние ({@code enableBlend/blendFunc/depthMask/enableCull/enableDepthTest}) +
 * {@code BufferRenderer.drawWithGlobalProgram(buffer.end())}. В 1.21.6 всё это удалено:
 * состояние (шейдер, blend, depth, cull) теперь «запекается» в объект {@link RenderPipeline},
 * а произвольный {@link BuiltBuffer BuiltBuffer} рисуется методом {@code RenderLayer#method_60895}.
 *
 * <p>Построение буфера ({@code Tessellator.begin(...)} + эмиссия вершин) не изменилось, поэтому
 * миграция вызова сводится к: убрать {@code setShader}/ручное состояние и заменить
 * {@code BufferRenderer.drawWithGlobalProgram(b.end())} на {@code WiosRender.draw(LAYER, b)},
 * где LAYER подобран под формат/режим/состояние старого вызова.
 */
public final class WiosRender {

    private WiosRender() {}

    private static final Map<String, RenderLayer> LAYERS = new HashMap<>();

    private static RenderPipeline.Builder base(String location) {
        // Аналог ванильного snippet field_60125: матрицы для core-шейдеров.
        return RenderPipeline.builder()
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withLocation(net.minecraft.util.Identifier.of("wiosvisual:pipeline/" + location));
    }

    private static DepthTestFunction depthFn(boolean depthTest) {
        return depthTest ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST;
    }

    private static RenderLayer makeLayer(String name, RenderPipeline pipeline) {
        return RenderLayer.of(name, RenderSetup.builder(pipeline).build());
    }

    // ------------------------------------------------------------------
    //  POSITION_COLOR  (VertexFormats.POSITION_COLOR = class_290.field_1576)
    // ------------------------------------------------------------------
    public static RenderLayer posColor(VertexFormat.DrawMode mode, boolean depthTest, boolean cull) {
        return posColor(mode, depthTest, cull, false);
    }

    public static RenderLayer posColor(VertexFormat.DrawMode mode, boolean depthTest, boolean cull, boolean additive) {
        String key = "pc|" + mode.name() + "|" + depthTest + "|" + cull + "|" + additive;
        return LAYERS.computeIfAbsent(key, k -> {
            RenderPipeline p = base("pc_" + LAYERS.size())
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(additive ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT) // SRC_ALPHA, ONE: как GL-аддитив старых версий, альфа учитывается
                    .withCull(cull)
                    .withDepthTestFunction(depthFn(depthTest))
                    .withDepthWrite(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, mode)
                    .build();
            return makeLayer("wios_pc", p);
        });
    }

    // ------------------------------------------------------------------
    //  Толстые линии (ванильный core/rendertype_lines). В 1.21.11 DrawMode.LINES рисуется
    //  треугольниками: шейдер раздвигает каждую пару вершин в полосу по Normal (направление
    //  линии) и LineWidth. С position_color такие линии вырождаются и не видны вовсе.
    //  Вершина: vertex(...).color(...).normal(dir).lineWidth(px).
    // ------------------------------------------------------------------
    public static RenderLayer lines(boolean depthTest) {
        String key = "lines|" + depthTest;
        return LAYERS.computeIfAbsent(key, k -> {
            RenderPipeline p = base("lines_" + LAYERS.size())
                    .withUniform("Fog", UniformType.UNIFORM_BUFFER)
                    .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                    .withVertexShader("core/rendertype_lines")
                    .withFragmentShader("core/rendertype_lines")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthTestFunction(depthFn(depthTest))
                    .withDepthWrite(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.DrawMode.LINES)
                    .build();
            return makeLayer("wios_lines", p);
        });
    }

    // ------------------------------------------------------------------
    //  POSITION  (VertexFormats.POSITION = class_290.field_1592)
    // ------------------------------------------------------------------
    public static RenderLayer position(VertexFormat.DrawMode mode, boolean depthTest, boolean cull) {
        String key = "pos|" + mode.name() + "|" + depthTest + "|" + cull;
        return LAYERS.computeIfAbsent(key, k -> {
            RenderPipeline p = base("pos_" + LAYERS.size())
                    .withVertexShader("core/position")
                    .withFragmentShader("core/position")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(cull)
                    .withDepthTestFunction(depthFn(depthTest))
                    .withDepthWrite(false)
                    .withVertexFormat(VertexFormats.POSITION, mode)
                    .build();
            return makeLayer("wios_pos", p);
        });
    }

    // ------------------------------------------------------------------
    //  POSITION_TEX_COLOR  (VertexFormats.POSITION_TEXTURE_COLOR = class_290.field_1575)
    //  Текстура (Sampler0) запекается в слой, поэтому кэшируем по идентификатору текстуры.
    // ------------------------------------------------------------------
    public static RenderLayer posTexColor(net.minecraft.util.Identifier texture,
                                         VertexFormat.DrawMode mode,
                                         boolean depthTest, boolean cull) {
        return posTexColor(texture, mode, depthTest, cull, false);
    }

    public static RenderLayer posTexColor(net.minecraft.util.Identifier texture,
                                         VertexFormat.DrawMode mode,
                                         boolean depthTest, boolean cull, boolean additive) {
        String key = "ptc|" + texture + "|" + mode.name() + "|" + depthTest + "|" + cull + "|" + additive;
        return LAYERS.computeIfAbsent(key, k -> {
            RenderPipeline p = base("ptc_" + LAYERS.size())
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(additive ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT)
                    .withCull(cull)
                    .withDepthTestFunction(depthFn(depthTest))
                    .withDepthWrite(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, mode)
                    .build();
            return RenderLayer.of("wios_ptc",
                    RenderSetup.builder(p).texture("Sampler0", texture).build());
        });
    }

    /** Непрозрачный вариант POSITION_TEX_COLOR: пишет глубину, поэтому модели не просвечивают сквозь траву и друг друга. */
    public static RenderLayer posTexColorSolid(net.minecraft.util.Identifier texture, VertexFormat.DrawMode mode) {
        String key = "ptcs|" + texture + "|" + mode.name();
        return LAYERS.computeIfAbsent(key, k -> {
            RenderPipeline p = base("ptcs_" + LAYERS.size())
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, mode)
                    .build();
            return RenderLayer.of("wios_ptcs",
                    RenderSetup.builder(p).texture("Sampler0", texture).build());
        });
    }

    // ------------------------------------------------------------------
    //  Отрисовка
    // ------------------------------------------------------------------

    /** Рисует построенный {@link BufferBuilder BufferBuilder} через слой (безопасно при пустом буфере). */
    public static void draw(RenderLayer layer, BufferBuilder builder) {
        BuiltBuffer built = builder.endNullable();
        if (built != null) {
            layer.draw(built);
        }
    }

    /** Рисует уже собранный {@link BuiltBuffer BuiltBuffer}. */
    public static void draw(RenderLayer layer, BuiltBuffer built) {
        if (built != null) {
            layer.draw(built);
        }
    }
}
