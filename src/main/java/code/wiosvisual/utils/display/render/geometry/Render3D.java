/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10017
 *  net.minecraft.class_10142
 *  net.minecraft.class_10156
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_2338
 *  net.minecraft.class_238
 *  net.minecraft.class_2382
 *  net.minecraft.class_243
 *  net.minecraft.class_265
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_3532
 *  net.minecraft.class_3545
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_7833
 *  net.minecraft.class_897
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.joml.Vector4i
 *  org.lwjgl.opengl.GL11
 */
package code.wiosvisual.utils.display.render.geometry;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.math.calc.Calculate;
import code.wiosvisual.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;

public final class Render3D
implements QuickImports {
    private static final Map<VoxelShape, Pair<List<Box>, List<Line>>> SHAPE_OUTLINES = new HashMap<VoxelShape, Pair<List<Box>, List<Line>>>();
    private static final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<VoxelShape, List<Box>>();
    public static final List<Texture> TEXTURE_DEPTH = new ArrayList<Texture>();
    public static final List<Texture> TEXTURE = new ArrayList<Texture>();
    public static final List<Line> LINE_DEPTH = new ArrayList<Line>();
    public static final List<Line> LINE = new ArrayList<Line>();
    public static final List<Quad> QUAD_DEPTH = new ArrayList<Quad>();
    public static final List<Quad> QUAD = new ArrayList<Quad>();
    public static Matrix4f lastProjMat = new Matrix4f();
    public static MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();
    private static final Identifier captureId = Identifier.of((String)"textures/capture1.png");
    private static final Identifier bloom = Identifier.of((String)"textures/bloom.png");
    private static final Identifier snow = Identifier.of((String)"textures/show1.png");
    public static final List<Crystal> crystalList = new ArrayList<Crystal>();
    private static float espValue = 1.0f;
    private static float espSpeed = 1.0f;
    private static float prevEspValue;
    private static float circleStep;
    private static boolean flipSpeed;

    public static void drawEntity(Entity entity, Vec3d pos, float yaw, int alpha, MatrixStack matrices, float tickDelta) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity livingEntity = (LivingEntity)entity;
        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.scale(1.0f, 1.0f, 1.0f);
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        if (renderer != null) {
            int light = renderer.getLight((Entity)livingEntity, tickDelta);
            VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
            // TODO(port 1.21.11): EntityRenderer.render теперь (class_10017, class_4587, class_11659, class_12075);
            // рендер сущности ESP через новый render-command-queue не портирован.
            vertexConsumers.draw();
        }
        matrices.pop();
    }

    public static void onWorldRender(WorldRenderEvent e) {
        Set<Float> widths;
        Set<Identifier> identifiers;
        if (!TEXTURE.isEmpty()) {
            identifiers = TEXTURE.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            identifiers.forEach(id -> {
                BufferBuilder textureBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> Render3D.quadTexture(tex.entry, textureBuffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                WiosRender.draw(WiosRender.posTexColor(id, VertexFormat.DrawMode.QUADS, false, false), textureBuffer);
            });
            TEXTURE.clear();
        }
        if (!TEXTURE_DEPTH.isEmpty()) {
            identifiers = TEXTURE_DEPTH.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            identifiers.forEach(id -> {
                BufferBuilder textureDepthBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE_DEPTH.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> Render3D.quadTexture(tex.entry, textureDepthBuffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                WiosRender.draw(WiosRender.posTexColor(id, VertexFormat.DrawMode.QUADS, true, false), textureDepthBuffer);
            });
            TEXTURE_DEPTH.clear();
        }
        if (!LINE.isEmpty()) {
            GL11.glEnable((int)2881);
            widths = LINE.stream().map(line -> Float.valueOf(line.width)).collect(Collectors.toCollection(LinkedHashSet::new));
            widths.forEach(width -> {
                BufferBuilder lineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
                LINE.stream().filter(line -> line.width == width.floatValue()).forEach(line -> Render3D.vertexLine(line.entry, (VertexConsumer)lineBuffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.LINES, false, false), lineBuffer);
            });
            LINE.clear();
            GL11.glDisable((int)2881);
        }
        if (!QUAD.isEmpty()) {
            BufferBuilder quadBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD.forEach(quad -> Render3D.vertexQuad(quad.entry, (VertexConsumer)quadBuffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, false, false), quadBuffer);
            QUAD.clear();
        }
        if (!LINE_DEPTH.isEmpty()) {
            GL11.glEnable((int)2881);
            widths = LINE_DEPTH.stream().map(line -> Float.valueOf(line.width)).collect(Collectors.toCollection(LinkedHashSet::new));
            widths.forEach(width -> {
                BufferBuilder lineDepthBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
                LINE_DEPTH.stream().filter(line -> line.width == width.floatValue()).forEach(line -> Render3D.vertexLine(line.entry, (VertexConsumer)lineDepthBuffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.LINES, true, false), lineDepthBuffer);
            });
            LINE_DEPTH.clear();
            GL11.glDisable((int)2881);
        }
        if (!QUAD_DEPTH.isEmpty()) {
            BufferBuilder quadDepthBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD_DEPTH.forEach(quad -> Render3D.vertexQuad(quad.entry, (VertexConsumer)quadDepthBuffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, true, false), quadDepthBuffer);
            QUAD_DEPTH.clear();
        }
    }

    public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        Render3D.drawShape(blockPos, voxelShape, color, width, true, false);
    }

    public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        if (!SHAPE_BOXES.containsKey(voxelShape)) {
            SHAPE_BOXES.put(voxelShape, voxelShape.getBoundingBoxes());
        }
        for (Box box : SHAPE_BOXES.get(voxelShape)) {
            Box offsetBox = box.offset(blockPos);
            if (!Projection.canSee(offsetBox)) continue;
            Render3D.drawBox(offsetBox, color, width, true, fill, depth);
        }
    }

    public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Render3D.drawShapeAlternative(blockPos, voxelShape, color, width, fill, true, depth);
    }

    public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean lines, boolean depth) {
        if (!Projection.canSee(new Box(blockPos))) {
            return;
        }
        if (!SHAPE_OUTLINES.containsKey(voxelShape)) {
            ArrayList<Line> edgeLines = new ArrayList<Line>();
            voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> edgeLines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0.0f)));
            SHAPE_OUTLINES.put(voxelShape, new Pair<List<Box>, List<Line>>(voxelShape.getBoundingBoxes(), edgeLines));
        }
        Vec3d vec3d = Vec3d.of((Vec3i)blockPos);
        Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.get(voxelShape);
        if (fill) {
            pair.getLeft().forEach(box -> Render3D.drawBox(box.offset(vec3d), color, width, false, true, depth));
        }
        if (lines) {
            pair.getRight().forEach(line -> Render3D.drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
        }
    }

    public static void drawBox(Box box, int color, float width) {
        Render3D.drawBox(box, color, width, true, true, false);
    }

    public static void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        Render3D.drawBox(null, box, color, width, line, fill, depth);
    }

    public static void drawBox(MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        box = box.expand(0.001);
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        if (fill) {
            int fillColor = ColorAssist.multAlpha(color, 0.1f);
            Render3D.drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            Render3D.drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            Render3D.drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            Render3D.drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            Render3D.drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            Render3D.drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
        }
        if (line) {
            Render3D.drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            Render3D.drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            Render3D.drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            Render3D.drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            Render3D.drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            Render3D.drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            Render3D.drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            Render3D.drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            Render3D.drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            Render3D.drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            Render3D.drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            Render3D.drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
        }
    }

    public static void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
        Render3D.vertexLine(matrices.peek(), buffer, start.toVector3f(), end.toVector3f(), startColor, endColor);
    }

    public static void vertexLine(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor) {
        if (entry == null) {
            entry = lastWorldSpaceMatrix;
        }
        Vector3f vec = Render3D.getNormal(start, end);
        buffer.vertex(entry, start).color(startColor);
        buffer.vertex(entry, end).color(endColor);
    }

    public static void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color) {
        Render3D.vertexQuad(entry, buffer, vec1.toVector3f(), vec2.toVector3f(), vec3.toVector3f(), vec4.toVector3f(), color);
    }

    public static void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
        if (entry == null) {
            entry = lastWorldSpaceMatrix;
        }
        buffer.vertex(entry, vec1).color(color);
        buffer.vertex(entry, vec2).color(color);
        buffer.vertex(entry, vec3).color(color);
        buffer.vertex(entry, vec4).color(color);
    }

    public static void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        buffer.vertex(entry, x, y + height, 0.0f).texture(0.0f, 0.0f).color(color.x);
        buffer.vertex(entry, x + width, y + height, 0.0f).texture(0.0f, 1.0f).color(color.y);
        buffer.vertex(entry, x + width, y, 0.0f).texture(1.0f, 1.0f).color(color.w);
        buffer.vertex(entry, x, y, 0.0f).texture(1.0f, 0.0f).color(color.z);
    }

    public static Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f((Vector3fc)start).sub((Vector3fc)end);
        float sqrt = MathHelper.sqrt((float)normal.lengthSquared());
        return normal.div(sqrt);
    }

    public static void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        Render3D.drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }

    public static void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        Render3D.drawLine(null, start, end, color, color, width, depth);
    }

    public static void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(entry, start, end, colorStart, colorEnd, width);
        if (depth) {
            LINE_DEPTH.add(line);
        } else {
            LINE.add(line);
        }
    }

    public static void drawCircleQuad(BufferBuilder buffer, MatrixStack.Entry entry, Vec3d center, double radius, int color, int segments) {
        buffer.vertex(entry, (float)center.x, (float)center.y, (float)center.z).color(color);
        for (int i = 0; i <= segments; ++i) {
            double angle = Math.PI * 2 * (double)i / (double)segments;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            buffer.vertex(entry, (float)(center.x + dx), (float)center.y, (float)(center.z + dz)).color(color);
        }
    }

    public static void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Render3D.drawQuad(null, x, y, w, z, color, depth);
    }

    public static void drawQuad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Quad quad = new Quad(entry, x, y, w, z, color);
        if (depth) {
            QUAD_DEPTH.add(quad);
        } else {
            QUAD.add(quad);
        }
    }

    public static void drawTexture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth) {
        Texture texture = new Texture(entry, id, x, y, width, height, color);
        if (depth) {
            TEXTURE_DEPTH.add(texture);
        } else {
            TEXTURE.add(texture);
        }
    }

    private Render3D() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void setLastProjMat(Matrix4f lastProjMat) {
        Render3D.lastProjMat = lastProjMat;
    }

    public static void setLastWorldSpaceMatrix(MatrixStack.Entry lastWorldSpaceMatrix) {
        Render3D.lastWorldSpaceMatrix = lastWorldSpaceMatrix;
    }

    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
    }

    public record Quad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
    }

    public record Texture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {
    }

    private static class Crystal {
        private final Entity entity;
        private final Vec3d position;
        private final Vec3d rotation;
        private final float size;
        private final float rotationSpeed;

        public Crystal(Entity entity, Vec3d position, Vec3d rotation) {
            this.entity = entity;
            this.position = position;
            this.rotation = rotation;
            this.size = 0.09f;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5);
        }

        public void render(MatrixStack ms) {
            ms.push();
            ms.translate(this.position.x, this.position.y, this.position.z);
            float pulsation = 1.0f + (float)(Math.sin((double)System.currentTimeMillis() / 500.0) * (double)0.1f);
            ms.scale(pulsation, pulsation, pulsation);
            float selfRotation = (float)(System.currentTimeMillis() % 36000L) / 100.0f * this.rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)this.rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)this.rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)this.rotation.z));
            int baseColor = ColorAssist.fade(90);
            this.drawCrystal(ms, baseColor, 0.2f, true);
            this.drawCrystal(ms, baseColor, 0.3f, true);
            this.drawCrystal(ms, baseColor, 0.8f, false);
            ms.pop();
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled) {
            Vec3d v2;
            Vec3d v1;
            int i;
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            float s = this.size;
            float h_prism = this.size * 1.0f;
            float h_pyramid = this.size * 1.5f;
            int numSides = 8;
            ArrayList<Vec3d> topVertices = new ArrayList<Vec3d>();
            ArrayList<Vec3d> bottomVertices = new ArrayList<Vec3d>();
            for (int i2 = 0; i2 < numSides; ++i2) {
                float angle = (float)(Math.PI * 2 * (double)i2 / (double)numSides);
                float x = (float)((double)s * Math.cos(angle));
                float z = (float)((double)s * Math.sin(angle));
                topVertices.add(new Vec3d((double)x, (double)(h_prism / 2.0f), (double)z));
                bottomVertices.add(new Vec3d((double)x, (double)(-h_prism / 2.0f), (double)z));
            }
            Vec3d vTop = new Vec3d(0.0, (double)(h_prism / 2.0f + h_pyramid), 0.0);
            Vec3d vBottom = new Vec3d(0.0, (double)(-h_prism / 2.0f - h_pyramid), 0.0);
            int finalColor = ColorAssist.setAlpha(baseColor, (int)(alpha * 255.0f));
            for (i = 0; i < numSides; ++i) {
                v1 = (Vec3d)bottomVertices.get(i);
                v2 = (Vec3d)bottomVertices.get((i + 1) % numSides);
                Vec3d v3 = (Vec3d)topVertices.get((i + 1) % numSides);
                Vec3d v4 = (Vec3d)topVertices.get(i);
                this.drawQuad(ms, bufferBuilder, v1, v2, v3, v4, finalColor, filled);
            }
            for (i = 0; i < numSides; ++i) {
                v1 = (Vec3d)topVertices.get(i);
                v2 = (Vec3d)topVertices.get((i + 1) % numSides);
                this.drawTriangle(ms, bufferBuilder, vTop, v1, v2, finalColor, filled);
            }
            for (i = 0; i < numSides; ++i) {
                v1 = (Vec3d)bottomVertices.get(i);
                v2 = (Vec3d)bottomVertices.get((i + 1) % numSides);
                this.drawTriangle(ms, bufferBuilder, vBottom, v2, v1, finalColor, filled);
            }
            WiosRender.draw(WiosRender.posColor(filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES, true, false), bufferBuilder);
        }

        private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            if (filled) {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            } else {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            }
        }

        private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color, boolean filled) {
            if (filled) {
                this.drawTriangle(ms, bb, v1, v2, v3, color, true);
                this.drawTriangle(ms, bb, v1, v3, v4, color, true);
            } else {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v4.x, (float)v4.y, (float)v4.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v4.x, (float)v4.y, (float)v4.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            }
        }
    }
}
