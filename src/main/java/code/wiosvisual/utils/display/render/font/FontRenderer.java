/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_2583
 *  net.minecraft.class_4587
 */
package code.wiosvisual.utils.display.render.font;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.utils.display.atlasfont.msdf.MsdfFont;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.MsdfFontLibrary;
import code.wiosvisual.utils.display.render.font.MsdfTextRenderer;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.display.scissor.ScissorAssist;
import code.wiosvisual.utils.math.calc.Calculate;
import java.awt.Font;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FontRenderer
implements QuickImports {
    private final String fontTypeName;
    private final float displaySize;
    private final Font font;
    private MsdfFont msdfFont;

    public FontRenderer(String fontTypeName, float displaySize) {
        this.fontTypeName = fontTypeName;
        this.displaySize = displaySize;
        this.font = new Font("SansSerif", 0, Math.max(1, Math.round(displaySize)));
    }

    private MsdfFont resolvedFont() {
        if (this.msdfFont == null) {
            this.msdfFont = MsdfFontLibrary.resolveType(this.fontTypeName);
        }
        return this.msdfFont;
    }

    public void drawText(MatrixStack matrix, Text text, double x, double y) {
        StringBuilder sb = new StringBuilder();
        this.findStyle(sb, text);
        this.drawString(matrix, sb.toString(), x, y, ColorAssist.getText());
    }

    public void findStyle(StringBuilder sb, Text component) {
        Style style = component.getStyle();
        if (component.getSiblings().isEmpty()) {
            if (style.getColor() != null) {
                sb.append(ColorAssist.formatting(style.getColor().getRgb()));
            }
            sb.append(component.getString()).append(Formatting.RESET);
        } else {
            component.getWithStyle(style).forEach(text -> this.findStyle(sb, (Text)text));
        }
    }

    public void drawStringWithScroll(MatrixStack matrix, String text, double x, double y, float width, int color) {
        String separation = "  |  ";
        float textWidth = this.getStringWidth(text + separation);
        if (textWidth - width < 10.0f) {
            this.drawString(matrix, text, x, y, color);
            return;
        }
        // Бегущая строка обрезается по ширине блока, иначе длинный заголовок
        // уезжает за пределы элемента и растягивается на весь экран.
        float lineHeight = this.getStringHeight(text);
        ScissorAssist scissor = Wiosvisual.getInstance() == null ? null : Wiosvisual.getInstance().getScissorManager();
        if (scissor == null) {
            this.drawString(matrix, text + separation + text, x - (double)Calculate.textScrolling(textWidth), y, color);
            return;
        }
        Rectangle.flushPending();
        scissor.push(matrix.peek().getPositionMatrix(), (float)x, (float)y - 1.0f, width, lineHeight + 3.0f);
        try {
            this.drawString(matrix, text + separation + text, x - (double)Calculate.textScrolling(textWidth), y, color);
            Rectangle.flushPending();
        }
        finally {
            // Без finally упавшая отрисовка оставила бы обрезку включённой,
            // и весь последующий интерфейс резался бы/мигал.
            scissor.pop();
        }
    }

    public void drawString(MatrixStack matrix, String text, double x, double y, int color) {
        Rectangle.flushPending();
        MsdfTextRenderer.drawString(matrix, this.resolvedFont(), this.displaySize, text, x, y, color);
    }

    public void drawFlatString(MatrixStack matrix, String text, double x, double y, int color) {
        Rectangle.flushPending();
        MsdfTextRenderer.drawFlatString(matrix, this.resolvedFont(), this.displaySize, text, x, y, color);
    }

    public void drawFlatVerticallyCentered(MatrixStack stack, String s, double x, double y, float height, int color) {
        this.drawFlatString(stack, s, x, this.getCenteredY((float)y, height, s), color);
    }

    public void drawStringWithSpacing(MatrixStack matrix, String text, double x, double y, int color, float spacing) {
        Rectangle.flushPending();
        MsdfTextRenderer.drawStringWithSpacing(matrix, this.resolvedFont(), this.displaySize, text, x, y, color, spacing);
    }

    public void drawGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd) {
        Rectangle.flushPending();
        MsdfTextRenderer.drawGradientString(matrix, this.resolvedFont(), this.displaySize, text, x, y, colorStart, colorEnd);
    }

    public void drawRainbowString(MatrixStack matrix, String text, double x, double y, int color1, int color2) {
        Rectangle.flushPending();
        MsdfTextRenderer.drawRainbowString(matrix, this.resolvedFont(), this.displaySize, text, x, y, color1, color2);
    }

    public void drawHalfSplitString(MatrixStack matrix, String text, double x, double y, int color1, int color2) {
        Rectangle.flushPending();
        MsdfTextRenderer.drawHalfSplitString(matrix, this.resolvedFont(), this.displaySize, text, x, y, color1, color2);
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
        this.drawString(stack, s, x - (double)(this.getStringWidth(s) / 2.0f), y, color);
    }

    public void drawVerticallyCentered(MatrixStack stack, String s, double x, double y, float height, int color) {
        this.drawString(stack, s, x, this.getCenteredY((float)y, height, s), color);
    }

    public void drawCenteredBoth(MatrixStack stack, String s, double centerX, double y, float height, int color) {
        this.drawCenteredString(stack, s, centerX, this.getCenteredY((float)y, height, s), color);
    }

    public float getCenteredY(float boxY, float boxHeight) {
        return boxY + (boxHeight - this.getStringHeight("A")) / 2.0f;
    }

    public float getCenteredY(float boxY, float boxHeight, String sample) {
        String text = sample == null || sample.isEmpty() ? "A" : sample;
        return boxY + (boxHeight - this.getStringHeight(text)) / 2.0f;
    }

    public float getStringWidth(Text text) {
        return text != null ? this.getStringWidth(text.getString()) : 0.0f;
    }

    public float getStringWidth(String text) {
        return MsdfTextRenderer.getStringWidth(this.resolvedFont(), this.displaySize, text);
    }

    public float getStringHeight(Text text) {
        return this.getStringHeight(text.getString());
    }

    public float getStringHeight(String text) {
        return MsdfTextRenderer.getStringHeight(this.resolvedFont(), this.displaySize, text);
    }

    public String trimToWidth(String text, int maxWidth, boolean backwards) {
        return backwards ? this.trimToWidthBackwards(text, maxWidth) : this.trimToWidth(text, maxWidth);
    }

    public String trimToWidth(String text, int maxWidth) {
        return text.substring(0, this.getTrimmedLength(text, maxWidth));
    }

    private int getTrimmedLength(String text, int maxWidth) {
        int length = 0;
        int i = 1;
        while (i <= text.length()) {
            if (this.getStringWidth(text.substring(0, i)) > (float)maxWidth) {
                return Math.max(0, i - 1);
            }
            length = i++;
        }
        return length;
    }

    public String trimToWidthBackwards(String text, int maxWidth) {
        for (int i = text.length(); i >= 0; --i) {
            if (!(this.getStringWidth(text.substring(i)) <= (float)maxWidth)) continue;
            return text.substring(i);
        }
        return "";
    }

    public String getFontTypeName() {
        return this.fontTypeName;
    }

    public float getDisplaySize() {
        return this.displaySize;
    }

    public Font getFont() {
        return this.font;
    }

    public MsdfFont getMsdfFont() {
        return this.msdfFont;
    }
}
