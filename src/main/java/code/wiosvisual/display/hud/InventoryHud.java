/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1799
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class InventoryHud
extends AbstractDraggable {

    private static final InventoryHud INSTANCE = new InventoryHud();
    private static final float SLOT_SIZE = 16.0f;
    private static final float SLOT_GAP = 2.0f;
    private static final float PANEL_PADDING = 5.0f;
    private static final int COLS = 9;
    private static final int ROWS = 3;

    private InventoryHud() {
        super("Inventory", 4, 80, 170, 62, true);
    }

    public static InventoryHud getInstance() {
        return INSTANCE;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean visible() {
        Hud hud = Hud.getInstance();
        if (hud == null) return false;
        if (!hud.isState()) return false;
        if (!Hud.interfaceSettings.isSelected("Inventory")) return false;
        return true;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (InventoryHud.mc.player == null) {
            return;
        }
        MatrixStack matrix = GuiCompat.stack(context);
        float gridWidth = 160.0f;
        float gridHeight = 52.0f;
        float panelWidth = gridWidth + 10.0f;
        float panelHeight = gridHeight + 10.0f;
        this.setWidth(Math.round(panelWidth));
        this.setHeight(Math.round(panelHeight));
        this.clampToScreen();
        float panelX = this.getX();
        float panelY = this.getY();
        float startX = panelX + 5.0f;
        float startY = panelY + 5.0f;
        // Прозрачная тёмная панель и отдельная квадратная ячейка под каждый слот.
        int panelColor = 0x8C141416;
        int cellColor = 0x59000000;
        rectangle.render(ShapeProperties.create(matrix, panelX, panelY, panelWidth, panelHeight).round(3.0f).color(panelColor, panelColor, panelColor, panelColor).build());
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                float cellX = startX + (float)col * 18.0f - 0.5f;
                float cellY = startY + (float)row * 18.0f - 0.5f;
                rectangle.render(ShapeProperties.create(matrix, cellX, cellY, 17.0f, 17.0f).round(1.5f).color(cellColor, cellColor, cellColor, cellColor).build());
            }
        }
        Rectangle.flushPending();
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slot = col + row * 9 + 9;
                float slotX = startX + (float)col * 18.0f;
                float slotY = startY + (float)row * 18.0f;
                ItemStack stack = InventoryHud.mc.player.getInventory().getStack(slot);
                if (stack.isEmpty()) continue;
                Render2D.defaultDrawStack(context, stack, slotX, slotY, false, false, 1.0f);
                // Количество и полоска прочности — ванильным шрифтом, как в обычном инвентаре.
                GuiCompat.stack(context).push();
                GuiCompat.stack(context).translate(slotX + 1.0f, slotY + 1.0f, 0.0f);
                context.drawStackOverlay(InventoryHud.mc.textRenderer, stack, 0, 0);
                GuiCompat.stack(context).pop();
            }
        }
    }

}
