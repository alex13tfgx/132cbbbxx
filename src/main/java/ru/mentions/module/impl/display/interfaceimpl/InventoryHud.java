package ru.mentions.module.impl.display.interfaceimpl;

import code.wiosvisual.events.render.DrawEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.impl.display.InterFace;
import ru.mentions.utils.annotation.IMinecraft;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.render.RenderUtil;

/**
 * Hud «Inventory»: 27 слотов основного инвентаря на полупрозрачной тёмной панели,
 * под каждым слотом своя квадратная ячейка. Количество, прочность и кд предмета —
 * ванильным оверлеем слота (туда же встроены жёлтые секунды кд).
 */
public class InventoryHud implements IMinecraft {
    private static final int COLS = 9;
    private static final int ROWS = 3;

    public void onRender(DragSetting drag, InterFace interFace, DrawEvent event) {
        if (mc.player == null) {
            return;
        }
        float s = InterFace.getInstance().sizeHud.getValue();
        float pad = 3F * s;
        float cell = 17F * s;
        float gap = 1F * s;
        float step = cell + gap;
        float w = pad * 2F + COLS * step - gap;
        float h = pad * 2F + ROWS * step - gap;
        drag.active = true;
        drag.size.set(w, h);
        float x = drag.position.x;
        float y = drag.position.y;

        RenderUtil.Render2D.rect(x, y, w, h, ColorUtil.getColor(16, 16, 19, 0.55F), 4F * s);
        int cellColor = ColorUtil.getColor(0, 0, 0, 0.35F);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                RenderUtil.Render2D.rect(x + pad + col * step, y + pad + row * step, cell, cell, cellColor, 2F * s);
            }
        }

        // Предметы — через DrawContext: его координаты = виртуальные * (2 / масштаб GUI).
        DrawContext context = event.getDrawContext();
        float scaleFix = 2F / (float) mc.getWindow().getScaleFactor();
        float item = 15F * s;
        float inset = (cell - item) / 2F;
        Matrix3x2fStack matrices = context.getMatrices();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ItemStack stack = mc.player.getInventory().getStack(9 + row * COLS + col);
                if (stack.isEmpty()) {
                    continue;
                }
                matrices.pushMatrix();
                matrices.translate((x + pad + col * step + inset) * scaleFix, (y + pad + row * step + inset) * scaleFix);
                float k = item / 16F * scaleFix;
                matrices.scale(k, k);
                context.drawItem(stack, 0, 0);
                context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
                matrices.popMatrix();
            }
        }
    }
}
