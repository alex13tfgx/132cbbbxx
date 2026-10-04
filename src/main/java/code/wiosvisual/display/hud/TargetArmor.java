package code.wiosvisual.display.hud;

import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.render.GuiCompat;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/**
 * Hud «Броня»: прочность брони цели (того, на кого наведён прицел / кто в плашке Target Hud).
 * При открытом чате показывает броню самого игрока, чтобы элемент можно было перетащить.
 */
public class TargetArmor
extends AbstractDraggable {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final float ROW_H = 16.0f;
    private static final float PAD = 4.0f;
    private static final float WIDTH = 58.0f;
    private final FontRenderer font = Fonts.getSize(11, Fonts.Type.SuisseIntlMedium);

    public TargetArmor() {
        super("Броня", 10, 200, (int)WIDTH, (int)(ROW_H * 4.0f + PAD * 2.0f), true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    @Override
    public boolean visible() {
        Hud hud = Hud.getInstance();
        return hud != null && hud.state && Hud.interfaceSettings.isSelected("Броня");
    }

    private LivingEntity resolveTarget() {
        LivingEntity target = TargetHud.getInstance().getCurrentTarget();
        if (target == null && PlayerInteractionHelper.isChat(TargetArmor.mc.currentScreen)) {
            target = TargetArmor.mc.player;
        }
        return target;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!this.visible() || TargetArmor.mc.player == null) {
            return;
        }
        LivingEntity target = this.resolveTarget();
        if (target == null) {
            return;
        }
        MatrixStack matrix = GuiCompat.stack(context);
        float height = ROW_H * 4.0f + PAD * 2.0f;
        this.setWidth((int)WIDTH);
        this.setHeight((int)height);
        float x = this.getX();
        float y = this.getY();
        rectangle.render(ShapeProperties.create(matrix, x, y, WIDTH, height).round(5.0f).color(ThemeManager.BackgroundGui.getColor()).build());
        Rectangle.flushPending();
        for (int i = 0; i < SLOTS.length; ++i) {
            ItemStack stack = target.getEquippedStack(SLOTS[i]);
            float rowY = y + PAD + i * ROW_H;
            String text = "—";
            int color = 0x66FFFFFF;
            if (!stack.isEmpty()) {
                Render2D.defaultDrawStack(context, stack, x + PAD, rowY, false, false, 0.8f);
                if (stack.isDamageable()) {
                    int max = stack.getMaxDamage();
                    int percent = Math.max(0, Math.round((float)(max - stack.getDamage()) * 100.0f / (float)max));
                    text = percent + "%";
                    color = percent > 50 ? 0xFFFFFFFF : (percent > 25 ? 0xFFFFBB28 : 0xFFFF3333);
                } else {
                    text = "100%";
                    color = 0xFFFFFFFF;
                }
            }
            this.font.drawString(matrix, text, x + PAD + 20.0f, this.font.getCenteredY(rowY, 13.0f), color);
        }
    }
}
