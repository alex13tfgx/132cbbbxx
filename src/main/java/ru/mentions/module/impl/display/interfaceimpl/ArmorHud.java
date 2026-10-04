package ru.mentions.module.impl.display.interfaceimpl;

import code.wiosvisual.events.render.DrawEvent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.impl.display.InterFace;
import ru.mentions.utils.annotation.IMinecraft;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.render.RenderUtil;

/**
 * Hud «Броня»: ряд из четырёх плоских значков (шлем, нагрудник, поножи, ботинки) собственной брони.
 * Цвет значка зависит от материала надетой вещи, почти сломанная краснеет. Без текста и фона;
 * перетаскивается мышью при открытом чате.
 */
public class ArmorHud implements IMinecraft {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public void onRender(DragSetting drag, InterFace interFace, DrawEvent event) {
        if (mc.player == null) {
            return;
        }
        float s = InterFace.getInstance().sizeHud.getValue();
        float unit = 1.3F * s;
        float cell = 12.5F * s;
        drag.active = true;
        drag.size.set(cell * SLOTS.length, 8F * unit);
        float x = drag.position.x;
        float y = drag.position.y;

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc.player.getEquippedStack(SLOTS[i]);
            int color;
            if (stack.isEmpty()) {
                color = ColorUtil.getColor(120, 110, 150, 0.28F);
            } else {
                color = materialColor(stack);
                if (stack.isDamageable()) {
                    int max = stack.getMaxDamage();
                    float left = (float)(max - stack.getDamage()) / (float)max;
                    if (left < 0.25F) {
                        // Почти сломанная вещь краснеет.
                        color = ColorUtil.interpolateColor2(ColorUtil.getColor(255, 70, 70, 1F), color, left / 0.25F);
                    }
                }
            }
            drawIcon(i, x + i * cell + (cell - 8F * unit) / 2F, y, unit, color);
        }
    }

    /** Цвет значка по материалу надетой брони: нетерит, алмаз, железо, золото, кольчуга, кожа, черепаха. */
    private static int materialColor(ItemStack stack) {
        String id = Registries.ITEM.getId(stack.getItem()).getPath();
        if (id.startsWith("netherite")) return ColorUtil.getColor(110, 90, 130, 1F);
        if (id.startsWith("diamond")) return ColorUtil.getColor(70, 225, 215, 1F);
        if (id.startsWith("iron")) return ColorUtil.getColor(210, 212, 218, 1F);
        if (id.startsWith("golden")) return ColorUtil.getColor(255, 210, 70, 1F);
        if (id.startsWith("chainmail")) return ColorUtil.getColor(150, 156, 170, 1F);
        if (id.startsWith("leather")) return ColorUtil.getColor(170, 110, 70, 1F);
        if (id.startsWith("turtle")) return ColorUtil.getColor(80, 190, 80, 1F);
        if (id.startsWith("copper")) return ColorUtil.getColor(220, 130, 90, 1F);
        return ColorUtil.getColor(150, 110, 235, 1F);
    }

    /** Плоские значки частей брони из прямоугольников, сетка 8x8. */
    private static void drawIcon(int slot, float x, float y, float u, int color) {
        switch (slot) {
            case 0 -> {
                rect(x, y, 2, 0, 4, 1, u, color);
                rect(x, y, 1, 1, 6, 2.5F, u, color);
                rect(x, y, 1, 3.5F, 2, 3.5F, u, color);
                rect(x, y, 5, 3.5F, 2, 3.5F, u, color);
                rect(x, y, 3.5F, 3.5F, 1, 2, u, color);
            }
            case 1 -> {
                rect(x, y, 0, 0.5F, 2.6F, 3F, u, color);
                rect(x, y, 5.4F, 0.5F, 2.6F, 3F, u, color);
                rect(x, y, 1.8F, 0, 4.4F, 8F, u, color);
            }
            case 2 -> {
                rect(x, y, 1, 0, 6, 2.2F, u, color);
                rect(x, y, 1, 2.2F, 2.7F, 5.8F, u, color);
                rect(x, y, 4.3F, 2.2F, 2.7F, 5.8F, u, color);
            }
            default -> {
                rect(x, y, 0.8F, 0.5F, 2.6F, 5.5F, u, color);
                rect(x, y, 4.6F, 0.5F, 2.6F, 5.5F, u, color);
                rect(x, y, 0, 6, 3.6F, 2, u, color);
                rect(x, y, 4.4F, 6, 3.6F, 2, u, color);
            }
        }
    }

    private static void rect(float ox, float oy, float dx, float dy, float w, float h, float u, int color) {
        RenderUtil.Render2D.rect(ox + dx * u, oy + dy * u, w * u, h * u, color, 0.6F * u);
    }
}
