/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10149
 *  net.minecraft.class_10156
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4587
 *  net.minecraft.class_4588
 *  net.minecraft.class_5944
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 */
package code.wiosvisual.utils.display.render.font;

import code.wiosvisual.events.render.TextFactoryEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.atlasfont.msdf.MsdfFont;
import code.wiosvisual.utils.display.atlasfont.providers.ResourceProvider;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.GuiRenderState;
import code.wiosvisual.utils.display.render.pipeline.SystemRenderPipelines;
import code.wiosvisual.utils.display.render.pipeline.WiosShader;
import com.mojang.blaze3d.buffers.Std140Builder;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.MemoryStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

public final class MsdfTextRenderer {
    private static final float THICKNESS = 0.05f;
    private static final float SMOOTHNESS = 0.5f;
    private static final float FLAT_THICKNESS = 0.03f;
    private static final float FLAT_SMOOTHNESS = 0.2f;
    private static final float BASELINE_FACTOR = 0.7f;
    private static final float X_NUDGE = -0.75f;
    private static MsdfFont currentFont;
    private static float currentThickness = 0.05f;
    private static float currentSmoothness = 0.5f;

    private MsdfTextRenderer() {
    }

    public static float visualHeight(float size) {
        return size * 0.7f;
    }

    public static float lineHeight(MsdfFont font, float size) {
        return font.getMetrics().lineHeight() * size;
    }

    public static float getStringWidth(MsdfFont font, float size, String text) {
        text = MsdfTextRenderer.resolveText(text);
        float effectiveThickness = 0.025f * size;
        float width = 0.0f;
        for (TextSegment segment : MsdfTextRenderer.parseSegments(text, -1)) {
            width += font.getLayoutWidth(segment.text(), size, effectiveThickness, 0.0f);
        }
        return width;
    }

    public static float getFlatStringWidth(MsdfFont font, float size, String text) {
        text = MsdfTextRenderer.resolveText(text);
        float effectiveThickness = 0.015f * size;
        float width = 0.0f;
        for (TextSegment segment : MsdfTextRenderer.parseSegments(text, -1)) {
            width += font.getLayoutWidth(segment.text(), size, effectiveThickness, 0.0f);
        }
        return width;
    }

    public static float getStringHeight(MsdfFont font, float size, String text) {
        if ((text = MsdfTextRenderer.resolveText(text)) == null || text.isEmpty()) {
            return MsdfTextRenderer.visualHeight(size);
        }
        String[] lines = text.split("\n", -1);
        if (lines.length <= 1) {
            return MsdfTextRenderer.visualHeight(size);
        }
        return MsdfTextRenderer.lineHeight(font, size) * (float)(lines.length - 1) + MsdfTextRenderer.visualHeight(size);
    }

    public static void drawString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color) {
        if ((text = MsdfTextRenderer.resolveText(text)) == null || text.isEmpty()) {
            return;
        }
        MsdfTextRenderer.beginDraw(font);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float drawY = (float)y;
        float cursorX = (float)x;
        for (String line : text.split("\n", -1)) {
            MsdfTextRenderer.drawLine(matrix4f, builder, font, size, line, cursorX, drawY, color);
            drawY += MsdfTextRenderer.lineHeight(font, size);
        }
        MsdfTextRenderer.endDraw(builder);
    }

    public static void drawFlatString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color) {
        if ((text = MsdfTextRenderer.resolveText(text)) == null || text.isEmpty()) {
            return;
        }
        MsdfTextRenderer.beginDraw(font, 0.03f, 0.2f);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float drawY = (float)y;
        float cursorX = (float)x;
        for (String line : text.split("\n", -1)) {
            MsdfTextRenderer.drawLine(matrix4f, builder, font, size, line, cursorX, drawY, color);
            drawY += MsdfTextRenderer.lineHeight(font, size);
        }
        MsdfTextRenderer.endDraw(builder);
    }

    public static void drawGradientString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int colorStart, int colorEnd) {
        if ((text = MsdfTextRenderer.resolveText(text)) == null || text.isEmpty()) {
            return;
        }
        MsdfTextRenderer.beginDraw(font);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float drawY = (float)y;
        float cursorX = (float)x;
        for (String line : text.split("\n", -1)) {
            MsdfTextRenderer.drawGradientLine(matrix4f, builder, font, size, line, cursorX, drawY, colorStart, colorEnd);
            drawY += MsdfTextRenderer.lineHeight(font, size);
        }
        MsdfTextRenderer.endDraw(builder);
    }

    public static void drawRainbowString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color1, int color2) {
        if ((text = MsdfTextRenderer.resolveText(text)) == null || text.isEmpty()) {
            return;
        }
        MsdfTextRenderer.beginDraw(font);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float drawY = (float)y;
        float cursorX = (float)x;
        float effectiveThickness = 0.025f * size;
        for (String line : text.split("\n", -1)) {
            font.applyRainbowGlyphs(matrix4f, (VertexConsumer)builder, line, size, effectiveThickness, 0.0f, cursorX + -0.75f, drawY + size * 0.7f, 0.0f, color1, color2);
            drawY += MsdfTextRenderer.lineHeight(font, size);
        }
        MsdfTextRenderer.endDraw(builder);
    }

    public static void drawStringWithSpacing(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color, float spacing) {
        if ((text = MsdfTextRenderer.resolveText(text)) == null || text.isEmpty()) {
            return;
        }
        MsdfTextRenderer.beginDraw(font);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float drawY = (float)y;
        float cursorX = (float)x;
        float effectiveThickness = 0.025f * size;
        for (String line : text.split("\n", -1)) {
            float lineX = cursorX;
            for (TextSegment segment : MsdfTextRenderer.parseSegments(line, color)) {
                if (segment.text().isEmpty()) continue;
                font.applyGlyphs(matrix4f, (VertexConsumer)builder, segment.text(), size, effectiveThickness, spacing, lineX + -0.75f, drawY + size * 0.7f, 0.0f, segment.color());
                lineX += font.getLayoutWidth(segment.text(), size, effectiveThickness, spacing);
            }
            drawY += MsdfTextRenderer.lineHeight(font, size);
        }
        MsdfTextRenderer.endDraw(builder);
    }

    private static void drawLine(Matrix4f matrix, BufferBuilder builder, MsdfFont font, float size, String line, float x, float y, int defaultColor) {
        float effectiveThickness = 0.025f * size;
        float cursorX = x;
        for (TextSegment segment : MsdfTextRenderer.parseSegments(line, defaultColor)) {
            if (segment.text().isEmpty()) continue;
            font.applyGlyphs(matrix, (VertexConsumer)builder, segment.text(), size, effectiveThickness, 0.0f, cursorX + -0.75f, y + size * 0.7f, 0.0f, segment.color());
            cursorX += font.getLayoutWidth(segment.text(), size, effectiveThickness, 0.0f);
        }
    }

    private static void drawGradientLine(Matrix4f matrix, BufferBuilder builder, MsdfFont font, float size, String line, float x, float y, int colorStart, int colorEnd) {
        if (line.isEmpty()) {
            return;
        }
        float effectiveThickness = 0.025f * size;
        float cursorX = x;
        int length = line.length();
        for (int i = 0; i < length; ++i) {
            char c = line.charAt(i);
            String glyph = String.valueOf(c);
            if (c != ' ') {
                float t = length <= 1 ? 0.0f : (float)i / (float)(length - 1);
                int color = MsdfTextRenderer.interpolateColor(colorStart, colorEnd, t);
                font.applyGlyphs(matrix, (VertexConsumer)builder, glyph, size, effectiveThickness, 0.0f, cursorX + -0.75f, y + size * 0.7f, 0.0f, color);
            }
            cursorX += font.getLayoutWidth(glyph, size, effectiveThickness, 0.0f);
        }
    }

    public static void drawHalfSplitString(MatrixStack matrix, MsdfFont font, float size, String text, double x, double y, int color1, int color2) {
        if ((text = MsdfTextRenderer.resolveText(text)) == null || text.isEmpty()) {
            return;
        }
        String flat = text.replace("\n", "");
        if (flat.isEmpty()) {
            return;
        }
        float totalWidth = MsdfTextRenderer.getStringWidth(font, size, flat);
        float midX = (float)x + totalWidth / 2.0f;
        MsdfTextRenderer.beginDraw(font);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float effectiveThickness = 0.025f * size;
        float cursorX = (float)x;
        float drawY = (float)y + size * 0.7f;
        for (int i = 0; i < flat.length(); ++i) {
            char c = flat.charAt(i);
            String glyph = String.valueOf(c);
            float glyphWidth = font.getLayoutWidth(glyph, size, effectiveThickness, 0.0f);
            if (c != ' ') {
                int color = cursorX + glyphWidth / 2.0f < midX ? color1 : color2;
                font.applyGlyphs(matrix4f, (VertexConsumer)builder, glyph, size, effectiveThickness, 0.0f, cursorX + -0.75f, drawY, 0.0f, color);
            }
            cursorX += glyphWidth;
        }
        MsdfTextRenderer.endDraw(builder);
    }

    private static void beginDraw(MsdfFont font) {
        MsdfTextRenderer.beginDraw(font, 0.05f, 0.5f);
    }

    private static void beginDraw(MsdfFont font, float thickness, float smoothness) {
        // Порт 1.21.11: параметры шейдера msdf_font передаются через UBO при отрисовке (endDraw).
        currentFont = font;
        currentThickness = thickness;
        currentSmoothness = smoothness;
    }

    private static void endDraw(BufferBuilder builder) {
        BuiltBuffer built = builder.endNullable();
        if (built != null && currentFont != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer ubo = Std140Builder.onStack(stack, 64)
                        .putFloat(currentFont.getAtlas().range())
                        .putFloat(currentThickness)
                        .putFloat(currentSmoothness)
                        .putInt(0)
                        .putFloat(0.0f)
                        .putVec4(0.0f, 0.0f, 0.0f, 0.0f)
                        .get();
                WiosShader.draw(SystemRenderPipelines.MSDF, built, Map.of("MsdfData", ubo),
                        Map.of("Sampler0", currentFont.getTextureView()));
            }
        }
        GuiRenderState.restoreFontAtlasesNearest();
    }

    private static String resolveText(String text) {
        TextFactoryEvent event = SafeEvents.dispatch(() -> new TextFactoryEvent(text));
        return event != null ? event.getText() : text;
    }

    private static List<TextSegment> parseSegments(String text, int defaultColor) {
        ArrayList<TextSegment> segments = new ArrayList<TextSegment>();
        StringBuilder current = new StringBuilder();
        int color = defaultColor == -1 ? ColorAssist.getText() : defaultColor;
        boolean colorFormat = false;
        boolean customColor = false;
        StringBuilder customColorBuffer = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '\u00a7') {
                MsdfTextRenderer.flushSegment(segments, current, color);
                colorFormat = true;
                continue;
            }
            if (colorFormat) {
                colorFormat = false;
                char code = Character.toUpperCase(c);
                if (ColorAssist.colorCodes.containsKey(code)) {
                    color = new Color(ColorAssist.colorCodes.get(code)).getRGB();
                    continue;
                }
                if (code != 'R' || defaultColor == -1) continue;
                color = defaultColor;
                continue;
            }
            if (c == '\u23cf') {
                if (customColor) {
                    color = new Color(Integer.parseInt(customColorBuffer.toString())).getRGB();
                    customColorBuffer.setLength(0);
                }
                customColor = !customColor;
                continue;
            }
            if (customColor) {
                customColorBuffer.append(c);
                continue;
            }
            current.append(c);
        }
        MsdfTextRenderer.flushSegment(segments, current, color);
        return segments;
    }

    private static void flushSegment(List<TextSegment> segments, StringBuilder current, int color) {
        if (!current.isEmpty()) {
            segments.add(new TextSegment(current.toString(), color));
            current.setLength(0);
        }
    }

    private static int interpolateColor(int colorStart, int colorEnd, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = colorStart >> 24 & 0xFF;
        int r1 = colorStart >> 16 & 0xFF;
        int g1 = colorStart >> 8 & 0xFF;
        int b1 = colorStart & 0xFF;
        int a2 = colorEnd >> 24 & 0xFF;
        int r2 = colorEnd >> 16 & 0xFF;
        int g2 = colorEnd >> 8 & 0xFF;
        int b2 = colorEnd & 0xFF;
        int a = (int)((float)a1 + (float)(a2 - a1) * t);
        int r = (int)((float)r1 + (float)(r2 - r1) * t);
        int g = (int)((float)g1 + (float)(g2 - g1) * t);
        int b = (int)((float)b1 + (float)(b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private record TextSegment(String text, int color) {
    }
}
