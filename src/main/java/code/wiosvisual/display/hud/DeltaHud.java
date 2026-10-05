package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Раскладка панелей HUD как в клиенте Delta: шапка 12.5 с иконкой, тонким разделителем и названием,
 * под ней отдельные плашки-строки высотой 11.5 с шагом 13.5. Строки въезжают слева со сдвигом,
 * ширина панели плавно тянется за содержимым.
 */
public final class DeltaHud {
    public static final float HEADER_H = 12.5f;
    public static final float ROW_H = 11.5f;
    public static final float ROW_STEP = 13.5f;
    public static final float RADIUS = 5.0f;

    private DeltaHud() {
    }

    /** Фон панели Delta: (11,11,22) с 5% основного цвета, альфа 170. */
    public static int bg(float anim) {
        return HudTheme.panelTop(255) & 0xFFFFFF | clampAlpha(170.0f * anim) << 24;
    }

    public static int withAlpha(int color, float alpha) {
        return clampAlpha(((color >>> 24) & 0xFF) * alpha) << 24 | color & 0xFFFFFF;
    }

    private static int clampAlpha(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    public static void panel(Rectangle rect, MatrixStack m, float x, float y, float w, float h, float anim) {
        if (anim <= 0.0f || w <= 0.0f || h <= 0.0f) {
            return;
        }
        int c = bg(anim);
        rect.render(ShapeProperties.create(m, x, y, w, h).round(RADIUS).softness(1.5f).color(c, c, c, c).build());
    }

    /** Тонкий вертикальный разделитель высотой в половину строки. */
    public static void separator(Rectangle rect, MatrixStack m, float x, float y, float rowH, float anim) {
        float h = rowH / 2.0f;
        int c = clampAlpha(200.0f * 0.5f * anim) << 24 | 0xC8C8C8;
        rect.render(ShapeProperties.create(m, x, y + (rowH - h) / 2.0f, 0.75f, h).round(0.0f).color(c, c, c, c).build());
    }

    public interface IconDrawer {
        void draw(DrawContext context, MatrixStack m, float x, float y, float size, float anim);
    }

    public record Row(float anim, String name, String right, int nameColor, IconDrawer icon) {
    }

    /** Список строк под шапкой; хранит плавно меняющуюся ширину. */
    public static final class ListPanel {
        private float width = 0.0f;
        /** Тёмно-серая плашка под правым столбцом (время в Potions). */
        public boolean rightPill;

        public void reset() {
            this.width = 0.0f;
        }

        /** Рисует панель и возвращает {ширина, высота}. */
        public float[] draw(DrawContext context, MatrixStack m, Rectangle rect, float x, float y, Identifier headerIcon, int headerIconColor,
                String title, FontRenderer headerFont, FontRenderer rowFont, List<Row> rows) {
            float target = 14.5f + headerFont.getStringWidth(title) + 7.0f;
            for (Row row : rows) {
                if (row.anim() <= 0.0f) continue;
                float leftPad = row.icon() != null ? 19.0f : 8.0f;
                target = Math.max(target, leftPad + rowFont.getStringWidth(row.name()) + 8.0f + rowFont.getStringWidth(row.right()) + 7.0f + (this.rightPill ? 4.0f : 0.0f));
            }
            this.width = this.width <= 0.0f ? target : this.width + (target - this.width) * 0.25f;
            float w = this.width;

            panel(rect, m, x, y, w, HEADER_H, 1.0f);
            separator(rect, m, x + 13.5f, y, HEADER_H, 1.0f);
            Rectangle.flushPending();
            float textY = headerFont.getCenteredY(y, HEADER_H);
            BackgroundIcons.drawHudHeaderIcon(context, headerIcon, headerFont, title, textY, x + 8.0f, headerIconColor);
            headerFont.drawString(m, title, x + 17.5f, textY, 0xFFFFFFFF);

            float contentY = y + HEADER_H + 3.0f;
            boolean any = false;
            for (Row row : rows) {
                float a = row.anim();
                if (a <= 0.0f) continue;
                any = true;
                float offX = -8.0f * (1.0f - a);
                float drawY = contentY - (1.0f - a);
                panel(rect, m, x + offX, drawY, w, ROW_H, a);
                boolean hasIcon = row.icon() != null;
                if (hasIcon) {
                    separator(rect, m, x + offX + 15.0f, drawY, ROW_H, a);
                }
                float rw = rowFont.getStringWidth(row.right());
                float rx = x + offX + w - (this.rightPill ? 8.5f : 6.0f) - rw;
                if (this.rightPill && rw > 0.0f) {
                    int pill = clampAlpha(215.0f * a) << 24 | 0x1E1E21;
                    rect.render(ShapeProperties.create(m, rx - 3.0f, drawY + 1.75f, rw + 6.0f, ROW_H - 3.5f).round(2.5f).color(pill, pill, pill, pill).build());
                }
                Rectangle.flushPending();
                if (hasIcon) {
                    row.icon().draw(context, m, x + offX + 4.5f, drawY + (ROW_H - 8.0f) / 2.0f, 8.0f, a);
                }
                float ty = rowFont.getCenteredY(drawY, ROW_H);
                rowFont.drawString(m, row.name(), x + offX + (hasIcon ? 19.0f : 8.0f), ty, withAlpha(row.nameColor(), a));
                rowFont.drawString(m, row.right(), rx, ty, withAlpha(0xFFFFFFFF, this.rightPill ? 0.85f * a : 0.55f * a));
                contentY += ROW_STEP * a;
            }
            Rectangle.flushPending();
            float h = any ? contentY - y - 2.0f : HEADER_H;
            return new float[]{w, h};
        }
    }
}
