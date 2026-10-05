package ru.mentions.module.impl.display.interfaceimpl;

import code.wiosvisual.events.render.DrawEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import ru.mentions.utils.annotation.IMinecraft;

/**
 * Hud «Броня»: надетая броня ванильными иконками справа от хотбара (шлем, нагрудник, поножи, ботинки).
 * Повреждённая вещь показывает ванильную полоску прочности, пустые слоты пропускаются.
 */
public class ArmorHud implements IMinecraft {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Отступ от правого края хотбара и шаг между вещами — в единицах GUI. */
    private static final int GAP = 24;
    private static final int STEP = 18;

    public void onRender(DrawEvent event) {
        if (mc.player == null) {
            return;
        }
        // Контекст DrawEvent — в обычных координатах GUI, как ванильный хотбар.
        DrawContext context = event.getDrawContext();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int x = width / 2 + 91 + GAP;
        int y = height - 19;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                context.drawItem(stack, x, y);
                context.drawStackOverlay(mc.textRenderer, stack, x, y);
            }
            x += STEP;
        }
    }
}
