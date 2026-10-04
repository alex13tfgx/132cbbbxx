package code.wiosvisual.utils.display.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Пайплайны для подсистемы systemrender (меню/иконки), шейдеры в namespace {@code mre}.
 * Кастомные uniform'ы переданы через std140-UBO (см. переписанные .vsh/.fsh под mre/shaders/core).
 */
public final class SystemRenderPipelines {

    private SystemRenderPipelines() {}

    private static RenderPipeline.Builder base(String loc) {
        return RenderPipeline.builder()
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withLocation(net.minecraft.util.Identifier.of("mre:pipeline/" + loc));
    }

    private static Identifier sh(String name) {
        return Identifier.of("mre", "core/" + name);
    }

    public static final RenderPipeline RECTANGLE = base("rectangle")
            .withVertexShader(sh("rectangle")).withFragmentShader(sh("rectangle"))
            .withUniform("RectData", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .build();

    public static final RenderPipeline BORDER = base("border")
            .withVertexShader(sh("border")).withFragmentShader(sh("border"))
            .withUniform("BorderData", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .build();

    public static final RenderPipeline TEXTURE = base("texture")
            .withVertexShader(sh("texture")).withFragmentShader(sh("texture"))
            .withUniform("TexData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build();

    public static final RenderPipeline BLUR = base("blur")
            .withVertexShader(sh("blur")).withFragmentShader(sh("blur"))
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .build();

    public static final RenderPipeline MSDF = base("msdf_font")
            .withVertexShader(sh("msdf_font")).withFragmentShader(sh("msdf_font"))
            .withUniform("MsdfData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build();

    // Процедурные оверлеи блоков (BlockOverlay): общий vsh, разные fsh, UBO BlockData{Time,Opacity}.
    private static RenderPipeline block(String frag) {
        return RenderPipeline.builder()
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("BlockData", UniformType.UNIFORM_BUFFER)
                .withVertexShader(sh("block_overlay_shader"))
                .withFragmentShader(sh(frag))
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                .withLocation(net.minecraft.util.Identifier.of("mre:pipeline/wios_" + frag))
                .build();
    }

    public static final RenderPipeline BLOCK_OVERLAY = block("block_overlay_shader");
    public static final RenderPipeline BLOCK_PLASMA = block("block_plasma");
    public static final RenderPipeline BLOCK_HEX = block("block_hex");
}
