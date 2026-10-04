/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10149
 *  net.minecraft.class_10156
 *  net.minecraft.class_2338
 *  net.minecraft.class_238
 *  net.minecraft.class_265
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_5944
 *  net.minecraft.class_9801
 */
package code.wiosvisual.utils.display.render.geometry;

import code.wiosvisual.utils.display.atlasfont.providers.ResourceProvider;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.display.render.pipeline.SystemRenderPipelines;
import code.wiosvisual.utils.display.render.pipeline.WiosShader;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.system.MemoryStack;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class ShaderBox3D {
    private static final float UV_SCALE = 4.0f;

    private ShaderBox3D() {
    }

    public static boolean drawShaderShape(VoxelShape shape, ShaderType shaderType, int color, float time, float opacity) {
        return ShaderBox3D.drawShaderShape(shape, null, shaderType, color, time, opacity, Render3D.lastWorldSpaceMatrix);
    }

    public static boolean drawShaderShape(VoxelShape shape, ShaderType shaderType, int color, float time, float opacity, MatrixStack.Entry matrixEntry) {
        return ShaderBox3D.drawShaderShape(shape, null, shaderType, color, time, opacity, matrixEntry);
    }

    public static boolean drawShaderShape(VoxelShape shape, BlockPos origin, ShaderType shaderType, int color, float time, float opacity, MatrixStack.Entry matrixEntry) {
        if (shape == null || shape.isEmpty() || matrixEntry == null) {
            return false;
        }
        double ox = origin != null ? (double)origin.getX() : 0.0;
        double oy = origin != null ? (double)origin.getY() : 0.0;
        double oz = origin != null ? (double)origin.getZ() : 0.0;
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (Box box : shape.getBoundingBoxes()) {
            Box worldBox = box.offset(ox, oy, oz).expand(0.0025);
            ShaderBox3D.addBox(builder, matrixEntry, worldBox, color);
        }
        BuiltBuffer built = builder.endNullable();
        if (built == null) {
            return false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer ubo = Std140Builder.onStack(stack, 16)
                    .putFloat(time)
                    .putFloat(opacity)
                    .get();
            WiosShader.draw(shaderType.pipeline, built, Map.of("BlockData", ubo));
        }
        return true;
    }


    private static void addBox(BufferBuilder builder, MatrixStack.Entry entry, Box box, int color) {
        ShaderBox3D.addQuad(builder, entry, 0, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        ShaderBox3D.addQuad(builder, entry, 0, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, color);
        ShaderBox3D.addQuad(builder, entry, 1, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, color);
        ShaderBox3D.addQuad(builder, entry, 1, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        ShaderBox3D.addQuad(builder, entry, 2, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, box.minX, box.minY, box.minZ, color);
        ShaderBox3D.addQuad(builder, entry, 2, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, color);
    }

    private static void addQuad(BufferBuilder builder, MatrixStack.Entry entry, int plane, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        builder.vertex(entry, (float)x1, (float)y1, (float)z1).texture(ShaderBox3D.planeU(plane, x1, y1, z1), ShaderBox3D.planeV(plane, x1, y1, z1)).color(color);
        builder.vertex(entry, (float)x2, (float)y2, (float)z2).texture(ShaderBox3D.planeU(plane, x2, y2, z2), ShaderBox3D.planeV(plane, x2, y2, z2)).color(color);
        builder.vertex(entry, (float)x3, (float)y3, (float)z3).texture(ShaderBox3D.planeU(plane, x3, y3, z3), ShaderBox3D.planeV(plane, x3, y3, z3)).color(color);
        builder.vertex(entry, (float)x4, (float)y4, (float)z4).texture(ShaderBox3D.planeU(plane, x4, y4, z4), ShaderBox3D.planeV(plane, x4, y4, z4)).color(color);
    }

    private static float planeU(int plane, double x, double y, double z) {
        if (plane == 2) {
            return (float)(z * 4.0);
        }
        return (float)(x * 4.0);
    }

    private static float planeV(int plane, double x, double y, double z) {
        if (plane == 0) {
            return (float)(z * 4.0);
        }
        return (float)(y * 4.0);
    }

    public static enum ShaderType {
        SHADER(SystemRenderPipelines.BLOCK_OVERLAY),
        PLASMA(SystemRenderPipelines.BLOCK_PLASMA),
        HEX(SystemRenderPipelines.BLOCK_HEX);

        private final RenderPipeline pipeline;

        private ShaderType(RenderPipeline pipeline) {
            this.pipeline = pipeline;
        }
    }
}
