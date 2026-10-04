/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10142
 *  net.minecraft.class_10156
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_2561
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_898
 *  net.minecraft.class_9801
 *  org.apache.logging.log4j.LogManager
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.lwjgl.opengl.GL11
 */
package code.wiosvisual.display.screens;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.emotions.EmotionType;
import code.wiosvisual.emotions.EmotionWheelManager;
import code.wiosvisual.features.impl.misc.Globals;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.lwjgl.opengl.GL11;

public class EmotionWheelScreen
extends Screen
implements QuickImports {
    private static final EmotionType[] WHEEL_SLOTS = EmotionType.values();
    private static final float CENTER_Y_OFFSET = -6.0f;
    private static final float INNER_RING_R = 72.0f;
    private static final float OUTER_RING_R = 178.0f;
    private static final int SECTOR_BASE = -870703064;
    private static final int SECTOR_HOVER = -399163316;
    private static final int DIVIDER = 0x55FFFFFF;
    private final EmotionWheelManager manager;
    private int hoveredSlot = -1;

    public EmotionWheelScreen(EmotionWheelManager manager) {
        super((Text)Text.empty());
        this.manager = manager;
    }

    public boolean shouldPause() {
        return false;
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int i;
        float centerX = (float)this.width / 2.0f;
        float centerY = (float)this.height / 2.0f + -6.0f;
        this.hoveredSlot = this.findHoveredSlot(mouseX, mouseY, centerX, centerY);
        EmotionType preview = this.getHoveredEmotion() != null ? this.getHoveredEmotion() : EmotionType.HELLO;
        this.manager.setPreviewEmotion(preview);
        context.fill(0, 0, this.width, this.height, 0x7C000000);
        float sectorSize = EmotionWheelScreen.sectorSizeDeg();
        float startOffset = -90.0f;
        for (i = 0; i < WHEEL_SLOTS.length; ++i) {
            float start = startOffset + (float)i * sectorSize;
            float end = start + sectorSize;
            int color = i == this.hoveredSlot ? -399163316 : -870703064;
            this.drawRingSector(context, centerX, centerY, 72.0f, 178.0f, start, end, color);
        }
        for (i = 0; i < WHEEL_SLOTS.length; ++i) {
            this.drawRadialLine(context, centerX, centerY, 72.0f, 178.0f, startOffset + (float)i * sectorSize, 0x55FFFFFF);
        }
        this.renderCenterHub(context, centerX, centerY);
        if (EmotionWheelScreen.mc.player != null) {
            this.drawEntityPreview(context, centerX, centerY + 8.0f, 36.0f, 180.0f, 6.0f, (LivingEntity)EmotionWheelScreen.mc.player);
        }
        this.renderInnerText(context, centerX, centerY, preview);
        this.renderSectorLabels(context, centerX, centerY, startOffset, sectorSize);
        Fonts.getSize(11, Fonts.Type.SuisseIntlMedium).drawCenteredString(GuiCompat.stack(context), "\u0423\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0439 " + this.getBindName() + " \u0438 \u043e\u0442\u043f\u0443\u0441\u0442\u0438 \u043d\u0430 \u043d\u0443\u0436\u043d\u043e\u0439 \u044d\u043c\u043e\u0446\u0438\u0438", centerX, centerY + 178.0f + 36.0f, -1800947253);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCenterHub(DrawContext context, float centerX, float centerY) {
        float hub = 70.0f;
        rectangle.render(ShapeProperties.create(GuiCompat.stack(context), centerX - hub, centerY - hub, hub * 2.0f, hub * 2.0f).round(hub).thickness(1.0f).outlineColor(0x44FFFFFF).color(-14538704).build());
    }

    private void renderInnerText(DrawContext context, float centerX, float centerY, EmotionType preview) {
        MatrixStack matrices = GuiCompat.stack(context);
        FontRenderer titleFont = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
        FontRenderer descFont = Fonts.getSize(10, Fonts.Type.SuisseIntlMedium);
        float textY = centerY + 34.0f;
        titleFont.drawCenteredString(matrices, preview.getTitle(), centerX, textY, -1);
        EmotionWheelScreen.drawWrappedCenteredText(matrices, descFont, preview.getDescription(), centerX, textY + 12.0f, 118.0f, -1177888794);
    }

    private void renderSectorLabels(DrawContext context, float centerX, float centerY, float startOffset, float sectorSize) {
        MatrixStack matrices = GuiCompat.stack(context);
        FontRenderer font = Fonts.getSize(14, Fonts.Type.SuisseIntlSemiBold);
        float labelRadius = 135.0f;
        float maxWidth = (float)(Math.PI * 2 * (double)labelRadius * ((double)sectorSize / 360.0) * (double)0.88f);
        for (int i = 0; i < WHEEL_SLOTS.length; ++i) {
            float midDeg = startOffset + (float)i * sectorSize + sectorSize * 0.5f;
            double rad = Math.toRadians(midDeg);
            float lx = centerX + (float)Math.cos(rad) * labelRadius;
            float ly = centerY + (float)Math.sin(rad) * labelRadius;
            int color = i == this.hoveredSlot ? -1 : -985857;
            EmotionWheelScreen.drawSectorLabel(font, matrices, WHEEL_SLOTS[i].getTitle(), lx, ly, maxWidth, color);
        }
    }

    private static void drawSectorLabel(FontRenderer font, MatrixStack matrices, String text, float lx, float ly, float maxWidth, int color) {
        float lineHeight = font.getStringHeight("Ag") + 2.0f;
        if (font.getStringWidth(text) <= maxWidth) {
            float visualH = font.getStringHeight(text);
            float drawY = ly - visualH / 2.0f + 3.0f;
            font.drawCenteredString(matrices, text, lx, drawY, color);
            return;
        }
        int split = text.indexOf(32);
        if (split > 0) {
            String line1 = text.substring(0, split);
            String line2 = EmotionWheelScreen.fitLabel(font, text.substring(split + 1), maxWidth);
            if (font.getStringWidth(line1) <= maxWidth && font.getStringWidth(line2) <= maxWidth) {
                float totalH = lineHeight * 2.0f;
                float topY = ly - totalH / 2.0f + 3.0f;
                font.drawCenteredString(matrices, line1, lx, topY, color);
                font.drawCenteredString(matrices, line2, lx, topY + lineHeight, color);
                return;
            }
        }
        String fitted = EmotionWheelScreen.fitLabel(font, text, maxWidth);
        float fittedH = font.getStringHeight(fitted);
        font.drawCenteredString(matrices, fitted, lx, ly - fittedH / 2.0f + 3.0f, color);
    }

    private static String fitLabel(FontRenderer font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 1 && font.getStringWidth(trimmed + "\u2026") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "\u2026";
    }

    private static float sectorSizeDeg() {
        return 360.0f / (float)WHEEL_SLOTS.length;
    }

    private int findHoveredSlot(double mouseX, double mouseY, float centerX, float centerY) {
        float dx = (float)mouseX - centerX;
        float dy = (float)mouseY - centerY;
        float dist = MathHelper.sqrt((float)(dx * dx + dy * dy));
        if (dist < 72.0f || dist > 178.0f) {
            return -1;
        }
        float angleDeg = (float)((Math.toDegrees(Math.atan2(dy, dx)) + 360.0) % 360.0);
        float normalized = (angleDeg + 90.0f) % 360.0f;
        float sectorSize = EmotionWheelScreen.sectorSizeDeg();
        int slot = (int)(normalized / sectorSize);
        return Math.min(slot, WHEEL_SLOTS.length - 1);
    }

    private void drawRingSector(DrawContext context, float cx, float cy, float innerR, float outerR, float startDeg, float endDeg, int color) {
        Matrix4f matrix = GuiCompat.stack(context).peek().getPositionMatrix();
        boolean cullWasEnabled = GL11.glIsEnabled((int)2884);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        int segments = 48;
        float startRad = (float)Math.toRadians(startDeg);
        float endRad = (float)Math.toRadians(endDeg);
        float step = (endRad - startRad) / (float)segments;
        for (int i = 0; i < segments; ++i) {
            float a1 = startRad + step * (float)i;
            float a2 = startRad + step * (float)(i + 1);
            float cos1 = MathHelper.cos((float)a1);
            float sin1 = MathHelper.sin((float)a1);
            float cos2 = MathHelper.cos((float)a2);
            float sin2 = MathHelper.sin((float)a2);
            float ox1 = cx + outerR * cos1;
            float oy1 = cy + outerR * sin1;
            float ox2 = cx + outerR * cos2;
            float oy2 = cy + outerR * sin2;
            float ix1 = cx + innerR * cos1;
            float iy1 = cy + innerR * sin1;
            float ix2 = cx + innerR * cos2;
            float iy2 = cy + innerR * sin2;
            buffer.vertex(matrix, ox1, oy1, 0.0f).color(color);
            buffer.vertex(matrix, ox2, oy2, 0.0f).color(color);
            buffer.vertex(matrix, ix2, iy2, 0.0f).color(color);
            buffer.vertex(matrix, ox1, oy1, 0.0f).color(color);
            buffer.vertex(matrix, ix2, iy2, 0.0f).color(color);
            buffer.vertex(matrix, ix1, iy1, 0.0f).color(color);
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.TRIANGLES, false, false), buffer);
        if (cullWasEnabled) {
        } else {
        }
    }

    private void drawRadialLine(DrawContext context, float cx, float cy, float innerR, float outerR, float angleDeg, int color) {
        float y1;
        float sin;
        float y2;
        float dy;
        float x1;
        float rad = (float)Math.toRadians(angleDeg);
        float cos = MathHelper.cos((float)rad);
        float x2 = cx + outerR * cos;
        float dx = x2 - (x1 = cx + innerR * cos);
        float length = MathHelper.sqrt((float)(dx * dx + (dy = (y2 = cy + outerR * (sin = MathHelper.sin((float)rad))) - (y1 = cy + innerR * sin)) * dy));
        if (length <= 0.001f) {
            return;
        }
        float lineWidth = 1.0f;
        float perpX = -dy / length * lineWidth;
        float perpY = dx / length * lineWidth;
        Matrix4f matrix = GuiCompat.stack(context).peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x1 + perpX, y1 + perpY, 0.0f).color(color);
        buffer.vertex(matrix, x2 + perpX, y2 + perpY, 0.0f).color(color);
        buffer.vertex(matrix, x1 - perpX, y1 - perpY, 0.0f).color(color);
        buffer.vertex(matrix, x1 - perpX, y1 - perpY, 0.0f).color(color);
        buffer.vertex(matrix, x2 + perpX, y2 + perpY, 0.0f).color(color);
        buffer.vertex(matrix, x2 - perpX, y2 - perpY, 0.0f).color(color);
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.TRIANGLES, false, false), buffer);
    }

    private static void drawWrappedCenteredText(MatrixStack matrices, FontRenderer font, String text, float centerX, float y, float maxWidth, int color) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (font.getStringWidth(text) <= maxWidth) {
            font.drawCenteredString(matrices, text, centerX, y, color);
            return;
        }
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float lineY = y;
        float lineHeight = font.getStringHeight("Ag") + 2.0f;
        for (String word : words) {
            String candidate;
            String string = candidate = line.isEmpty() ? word : String.valueOf(line) + " " + word;
            if (font.getStringWidth(candidate) > maxWidth && !line.isEmpty()) {
                font.drawCenteredString(matrices, line.toString(), centerX, lineY, color);
                line = new StringBuilder(word);
                lineY += lineHeight;
                continue;
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            font.drawCenteredString(matrices, line.toString(), centerX, lineY, color);
        }
    }

    public void close() {
        this.manager.closeWheel(false);
    }

    public EmotionType getHoveredEmotion() {
        if (this.hoveredSlot < 0 || this.hoveredSlot >= WHEEL_SLOTS.length) {
            return null;
        }
        return WHEEL_SLOTS[this.hoveredSlot];
    }

    private String getBindName() {
        Globals globals = Instance.get(Globals.class);
        if (globals == null) {
            return "\u043a\u043d\u043e\u043f\u043a\u0443";
        }
        int key = globals.getEmotionWheelBind().getKey();
        if (key == -1) {
            return "\u043a\u043d\u043e\u043f\u043a\u0443";
        }
        String translation = InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
        return translation == null || translation.isBlank() ? "\u043a\u043d\u043e\u043f\u043a\u0443" : translation;
    }

    private void drawEntityPreview(DrawContext context, float x, float y, float size, float yaw, float pitch, LivingEntity entity) {
        float wrappedYaw;
        MatrixStack matrices = GuiCompat.stack(context);
        matrices.push();
        matrices.translate((double)x, (double)y, 1050.0);
        matrices.scale(1.0f, 1.0f, -1.0f);
        matrices.scale(size, size, size);
        float pitchRad = (float)Math.toRadians(pitch);
        Quaternionf cameraRotation = new Quaternionf().rotateX(-pitchRad);
        Quaternionf entityRotation = new Quaternionf().rotateZ((float)Math.PI).mul((Quaternionfc)cameraRotation);
        matrices.multiply(entityRotation);
        float prevBodyYaw = entity.bodyYaw;
        float prevYaw = entity.getYaw();
        float prevPitch = entity.getPitch();
        float prevHeadYaw = entity.headYaw;
        float prevPrevHeadYaw = entity.lastHeadYaw;
        float prevPrevBodyYaw = entity.lastBodyYaw;
        float prevPrevYaw = entity.lastYaw;
        float prevPrevPitch = entity.lastPitch;
        float bodyYaw = wrappedYaw = MathHelper.wrapDegrees((float)yaw);
        float modelPitch = -pitch * 0.9f;
        entity.bodyYaw = bodyYaw;
        entity.lastBodyYaw = bodyYaw;
        entity.setYaw(bodyYaw);
        entity.lastYaw = bodyYaw;
        entity.setPitch(modelPitch);
        entity.lastPitch = modelPitch;
        entity.headYaw = bodyYaw;
        entity.lastHeadYaw = bodyYaw;
        // TODO(port 1.21.11): EntityRenderDispatcher переработан на render-states (method_62424/method_24196/
        // method_3948 удалены/сменили сигнатуру). Превью сущности в колесе эмоций временно отключено.
        entity.bodyYaw = prevBodyYaw;
        entity.lastBodyYaw = prevPrevBodyYaw;
        entity.setYaw(prevYaw);
        entity.lastYaw = prevPrevYaw;
        entity.setPitch(prevPitch);
        entity.lastPitch = prevPrevPitch;
        entity.headYaw = prevHeadYaw;
        entity.lastHeadYaw = prevPrevHeadYaw;
        matrices.pop();
    }
}
