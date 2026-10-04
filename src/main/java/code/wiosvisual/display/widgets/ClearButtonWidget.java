/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_4068
 *  net.minecraft.class_6379
 *  net.minecraft.class_6379$class_6380
 *  net.minecraft.class_6382
 */
package code.wiosvisual.display.widgets;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ClearButtonWidget
implements Drawable,
Element,
Selectable {
    private static final Identifier CLEAR_BUTTON_TEXTURE = Identifier.of((String)"minecraft", (String)"textures/clear_button.png");
    private static final Identifier HOVERED_CLEAR_BUTTON_TEXTURE = Identifier.of((String)"minecraft", (String)"textures/hovered_clear_button.png");
    private int x;
    private int y;
    private final int width;
    private final int height;
    private final Runnable onPress;
    private boolean hovered = false;

    public ClearButtonWidget(int x, int y, int width, int height, Runnable onPress) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onPress = onPress;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        Identifier texture = this.hovered ? HOVERED_CLEAR_BUTTON_TEXTURE : CLEAR_BUTTON_TEXTURE;
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, this.x, this.y, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        if (this.hovered && QuickImports.mc.currentScreen != null) {
            context.drawTooltip(QuickImports.mc.textRenderer, (Text)Text.literal((String)"\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c"), mouseX, mouseY);
        }
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height)) {
            this.onPress.run();
            return true;
        }
        return false;
    }

    public void setFocused(boolean focused) {
    }

    public boolean isFocused() {
        return false;
    }

    public Selectable.SelectionType getType() {
        return Selectable.SelectionType.NONE;
    }

    public void appendNarrations(NarrationMessageBuilder builder) {
    }
}
