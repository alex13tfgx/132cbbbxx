/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10142
 *  net.minecraft.class_10149
 *  net.minecraft.class_10156
 *  net.minecraft.class_1058
 *  net.minecraft.class_1799
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 *  net.minecraft.class_5944
 *  net.minecraft.class_9801
 *  net.minecraft.class_9848
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 */
package code.wiosvisual.utils.display.render.geometry;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.item.ItemRender;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2DQuad;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import com.mojang.blaze3d.systems.RenderSystem;
import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class Render2D
implements QuickImports {
    private static final List<Render2DQuad> QUAD = new ArrayList<Render2DQuad>();

    public static void onRender(DrawContext context) {
        Render2D.flushQuads(GuiCompat.stack(context));
    }

    public static void flushQuads(MatrixStack matrix) {
        if (QUAD.isEmpty()) {
            return;
        }
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        QUAD.forEach(quad -> drawEngine.quad(matrix4f, buffer, quad.x(), quad.y(), quad.width(), quad.height(), quad.color()));
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, false, false), buffer);
        QUAD.clear();
    }

    public static void defaultDrawStack(DrawContext context, ItemStack stack, float x, float y, boolean rect, boolean drawItemInSlot, float scale) {
        Render2D.defaultDrawStack(context, stack, x, y, rect, drawItemInSlot, scale, 0.0f);
    }

    public static void defaultDrawStack(DrawContext context, ItemStack stack, float x, float y, boolean rect, boolean drawItemInSlot, float scale, float z) {
        int count;
        if (Rectangle.isBatching()) {
            Rectangle.flushPending();
        }
        MatrixStack matrix = GuiCompat.stack(context);
        matrix.push();
        matrix.translate(x + 1.0f, y + 1.0f, z);
        matrix.scale(scale, scale, 1.0f);
        context.drawItem(stack, 0, 0);
        if (drawItemInSlot && (count = stack.getCount()) > 1) {
            String countText = String.valueOf(count);
            FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
            float textWidth = font.getStringWidth(countText);
            float textX = 16.0f - textWidth - 1.0f;
            float textY = 16.0f - font.getStringHeight(countText) - 1.0f;
            font.drawString(matrix, countText, textX - 0.5f, textY - 0.5f, -16777216);
            font.drawString(matrix, countText, textX + 0.5f, textY - 0.5f, -16777216);
            font.drawString(matrix, countText, textX - 0.5f, textY + 0.5f, -16777216);
            font.drawString(matrix, countText, textX + 0.5f, textY + 0.5f, -16777216);
            font.drawString(matrix, countText, textX, textY, -1);
        }
        if (drawItemInSlot && code.wiosvisual.features.impl.render.Hud.slotCooldowns.isValue()) {
            code.wiosvisual.utils.client.ItemCooldowns.drawSlot(context, stack, 0, 0);
        }
        matrix.pop();
    }

    public static void rectangleWithMask(Matrix4f matrix4f, float x, float y, float w, float h, float round, int color, int texture) {
        // TODO(port 1.21.11): использовал кастомный шейдер core/sampler_texture/sampler_texture с
        // uniform'ами size/location/texSize/color/round (система ShaderProgram удалена). Метод нигде
        // не вызывается — при необходимости перевести на WiosShader+UBO по образцу RoundRenderer.
    }

    public static void drawStack(MatrixStack matrix, ItemStack stack, float x, float y, boolean rect, float scale) {
        if (Rectangle.isBatching()) {
            Rectangle.flushPending();
        }
        float posX = x + 1.0f;
        float posY = y + 1.0f;
        float padding = 1.0f;
        matrix.push();
        matrix.translate(posX, posY, 0.0f);
        if (rect) {
            blur.render(ShapeProperties.create(matrix, -padding, -padding, 16.0f * scale + padding * 2.0f, 16.0f * scale + padding * 2.0f).round(1.5f).color(ColorAssist.HALF_BLACK).build());
        }
        matrix.scale(scale, scale, 1.0f);
        ItemRender.drawItem(matrix, stack, 0.0f, 0.0f, true, true);
        matrix.pop();
    }

    public static void drawTexture(DrawContext context, Identifier id, float x, float y, int size) {
        MatrixStack matrix = GuiCompat.stack(context);
        if (id != null) {
            matrix.push();
            matrix.translate(x, y, 0.0f);
            matrix.scale((float)size, (float)size, 1.0f);
            Render2D.drawTexture(matrix, id, 0, 0, 1.0f, 1.0f, size, size, size, size, size, size, -1);
            matrix.translate(-x, -y, 0.0f);
            matrix.pop();
        }
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1.0f, Math.max(0.0f, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * opacity));
    }

    public static int applyOpacity(int color_int, float opacity) {
        opacity = Math.min(1.0f, Math.max(0.0f, opacity));
        Color color = new Color(color_int);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * opacity)).getRGB();
    }

    public static void drawTexture(DrawContext context, Identifier id, float x, float y, float size, float round, int uvSize, int regionSize, int textureSize, int backgroundColor) {
        Render2D.drawTexture(context, id, x, y, size, round, uvSize, regionSize, textureSize, backgroundColor, -1);
    }

    public static void drawTexture(DrawContext context, Identifier id, float x, float y, float size, float round, int uvSize, int regionSize, int textureSize, int backgroundColor, int color) {
        if (Rectangle.isBatching()) {
            Rectangle.flushPending();
        }
        MatrixStack matrix = GuiCompat.stack(context);
        rectangle.render(ShapeProperties.create(matrix, x, y, size, size).round(round).color(backgroundColor).build());
        if (id != null) {
            matrix.push();
            matrix.translate(x, y, 0.0f);
            matrix.scale(size, size, 1.0f);
            Render2D.drawTexture(matrix, id, 0, 0, 1.0f, 1.0f, uvSize, uvSize, regionSize, regionSize, textureSize, textureSize, color);
            matrix.translate(-x, -y, 0.0f);
            matrix.pop();
        }
    }

    public static void drawSprite(MatrixStack matrix, Sprite sprite, float x, float y, float width, int height) {
        if (width != 0.0f && height != 0) {
            Render2D.drawTexturedQuad(matrix, sprite.getAtlasId(), x, x + width, y, y + (float)height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), -1);
        }
    }

    public static void drawSprite(MatrixStack matrix, Sprite sprite, float x, float y, float width, int height, int color) {
        if (width != 0.0f && height != 0) {
            Render2D.drawTexturedQuad(matrix, sprite.getAtlasId(), x, x + width, y, y + (float)height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color);
        }
    }

    public static void drawTexture(MatrixStack matrix, Identifier texture, int x, int y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        Render2D.drawTexture(matrix, texture, x, (float)x + width, y, (float)y + height, 0.0f, regionWidth, regionHeight, u, v, textureWidth, textureHeight, color);
    }

    public static void drawTexture(MatrixStack matrix, Identifier texture, float x1, float x2, float y1, float y2, float z, int regionWidth, int regionHeight, float u, float v, int textureWidth, int textureHeight, int color) {
        Render2D.drawTexturedQuad(matrix, texture, x1, x2, y1, y2, (u + 0.0f) / (float)textureWidth, (u + (float)regionWidth) / (float)textureWidth, (v + 0.0f) / (float)textureHeight, (v + (float)regionHeight) / (float)textureHeight, color);
    }

    public static void drawSpriteColored(MatrixStack matrix, Sprite sprite, float x, float y, float width, float height, int colorTL, int colorBL, int colorBR, int colorTR) {
        if (Rectangle.isBatching()) {
            Rectangle.flushPending();
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        buffer.vertex(matrix4f, x, y, 0.0f).texture(sprite.getMinU(), sprite.getMinV()).color(colorTL);
        buffer.vertex(matrix4f, x, y + height, 0.0f).texture(sprite.getMinU(), sprite.getMaxV()).color(colorBL);
        buffer.vertex(matrix4f, x + width, y + height, 0.0f).texture(sprite.getMaxU(), sprite.getMaxV()).color(colorBR);
        buffer.vertex(matrix4f, x + width, y, 0.0f).texture(sprite.getMaxU(), sprite.getMinV()).color(colorTR);
        WiosRender.draw(WiosRender.posTexColor(sprite.getAtlasId(), VertexFormat.DrawMode.QUADS, false, false), buffer);
    }

    public static void drawTexturedQuad(MatrixStack matrix, Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2, int color) {
        if (Rectangle.isBatching()) {
            Rectangle.flushPending();
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        buffer.vertex(matrix4f, x1, y1, 0.0f).texture(u1, v1).color(color);
        buffer.vertex(matrix4f, x1, y2, 0.0f).texture(u1, v2).color(color);
        buffer.vertex(matrix4f, x2, y2, 0.0f).texture(u2, v2).color(color);
        buffer.vertex(matrix4f, x2, y1, 0.0f).texture(u2, v1).color(color);
        WiosRender.draw(WiosRender.posTexColor(texture, VertexFormat.DrawMode.QUADS, false, false), buffer);
    }

    public static void drawCircle(MatrixStack matrix, float x, float y, float radius, int color) {
        int segments = 16;
        float angleStep = (float)(Math.PI * 2 / (double)segments);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; ++i) {
            float angle1 = (float)i * angleStep;
            float angle2 = (float)(i + 1) * angleStep;
            float x1 = x + radius * (float)Math.cos(angle1);
            float y1 = y + radius * (float)Math.sin(angle1);
            float x2 = x + radius * (float)Math.cos(angle2);
            float y2 = y + radius * (float)Math.sin(angle2);
            buffer.vertex(matrix4f, x, y, 0.0f).color(color);
            buffer.vertex(matrix4f, x1, y1, 0.0f).color(color);
            buffer.vertex(matrix4f, x2, y2, 0.0f).color(color);
            buffer.vertex(matrix4f, x, y, 0.0f).color(color);
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, false, false), buffer);
    }

    public static void drawCircleOutline(MatrixStack matrix, float x, float y, float radius, float thickness, int color) {
        int segments = 48;
        float angleStep = (float)(Math.PI * 2 / (double)segments);
        float inner = Math.max(0.0f, radius - thickness);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; ++i) {
            float angle1 = (float)i * angleStep;
            float angle2 = (float)(i + 1) * angleStep;
            float cos1 = (float)Math.cos(angle1);
            float sin1 = (float)Math.sin(angle1);
            float cos2 = (float)Math.cos(angle2);
            float sin2 = (float)Math.sin(angle2);
            float ox1 = x + radius * cos1;
            float oy1 = y + radius * sin1;
            float ox2 = x + radius * cos2;
            float oy2 = y + radius * sin2;
            float ix1 = x + inner * cos1;
            float iy1 = y + inner * sin1;
            float ix2 = x + inner * cos2;
            float iy2 = y + inner * sin2;
            buffer.vertex(matrix4f, ox1, oy1, 0.0f).color(color);
            buffer.vertex(matrix4f, ox2, oy2, 0.0f).color(color);
            buffer.vertex(matrix4f, ix2, iy2, 0.0f).color(color);
            buffer.vertex(matrix4f, ix1, iy1, 0.0f).color(color);
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, false, false), buffer);
    }

    public static void endBuilding(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null) {
            // TODO(port 1.21.11): метод не используется; формат/шейдер буфера неизвестны — по умолчанию POSITION_COLOR.
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, false, false), builtBuffer);
        }
    }

    public static void drawLine(MatrixStack matrix, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4f) {
            return;
        }
        float half = thickness * 0.5f;
        float nx = -dy / length * half;
        float ny = dx / length * half;
        int finalColor = ColorAssist.multAlpha(color, 1.0f);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix4f, x1 + nx, y1 + ny, 0.0f).color(finalColor);
        buffer.vertex(matrix4f, x1 - nx, y1 - ny, 0.0f).color(finalColor);
        buffer.vertex(matrix4f, x2 - nx, y2 - ny, 0.0f).color(finalColor);
        buffer.vertex(matrix4f, x2 + nx, y2 + ny, 0.0f).color(finalColor);
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.QUADS, false, false), buffer);
    }

    public static void drawQuad(float x, float y, float width, float height, int color) {
        QUAD.add(new Render2DQuad(x, y, width, height, ColorAssist.multAlpha(color, 1.0f)));
    }

    private Render2D() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
