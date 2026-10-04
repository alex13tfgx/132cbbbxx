/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 */
package code.wiosvisual.display.screens.altscreen;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class Alt
extends Screen
implements QuickImports {
    public static Alt INSTANCE = new Alt();
    public int x;
    public int y;
    public int width;
    public int height;

    public void initialize() {
    }

    public Alt() {
        super(Text.of((String)"Alt"));
        this.initialize();
    }

    public void tick() {
        super.tick();
    }

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.x = window.getScaledWidth() / 2 - 200;
        this.y = window.getScaledHeight() / 2 - 125;
        this.width = window.getScaledWidth();
        this.height = window.getScaledHeight();
        super.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        return super.keyPressed(wiosKey);
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
