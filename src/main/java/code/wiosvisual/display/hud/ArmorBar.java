/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1304
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_3414
 *  net.minecraft.class_4587
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.display.hud.CustomHotbar;
import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.client.sound.SoundManager;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

public class ArmorBar
extends AbstractDraggable {
    private static final int WARNING_PERCENT = 15;
    private static final int CRITICAL_PERCENT = 5;
    private final List<ItemStack> armorStacks = new ArrayList<ItemStack>();
    private final Map<EquipmentSlot, Item> trackedItems = new EnumMap<EquipmentSlot, Item>(EquipmentSlot.class);
    private final Map<EquipmentSlot, Integer> warningStage = new EnumMap<EquipmentSlot, Integer>(EquipmentSlot.class);
    private final FontRenderer durabilityFont = Fonts.getSize(10, Fonts.Type.DEFAULT);

    public ArmorBar() {
        super("ArmorBar", 0, 0, 150, 24, true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    @Override
    public boolean visible() {
        Hud.getInstance();
        return Hud.interfaceSettings.isSelected("ArmorBar") && Hud.getInstance().state;
    }

    @Override
    public void tick() {
        if (ArmorBar.mc.player == null) {
            return;
        }
        this.armorStacks.clear();
        this.trackArmorSlot(EquipmentSlot.FEET);
        this.trackArmorSlot(EquipmentSlot.LEGS);
        this.trackArmorSlot(EquipmentSlot.CHEST);
        this.trackArmorSlot(EquipmentSlot.HEAD);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!this.visible()) {
            return;
        }
        MatrixStack matrix = GuiCompat.stack(context);
        float slotSize = 20.0f;
        float spacing = 3.0f;
        float padding = 3.0f;
        float durabilityBarHeight = 2.0f;
        float barSpacing = 2.0f;
        float totalWidth = (slotSize + spacing) * 4.0f - spacing + padding * 2.0f;
        float totalHeight = slotSize + padding * 2.0f + barSpacing + durabilityBarHeight;
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();
        if (this.getX() == 0 && this.getY() == 0) {
            // Первое появление: ставим рядом с хотбаром, дальше позицию задаёт игрок в редакторе HUD.
            CustomHotbar.HotbarLayout layout = CustomHotbar.computeLayout(screenWidth, screenHeight);
            float startX = layout.hotbarPanelX() + layout.hotbarPanelWidth() + CustomHotbar.getArmorBarGap();
            if (startX + totalWidth > (float)screenWidth - 1.0f) {
                startX = (float)screenWidth - 1.0f - totalWidth;
            }
            this.setX(Math.max(1, (int)startX));
            this.setY(Math.max(1, (int)layout.hotbarPanelY()));
        }
        this.setWidth((int)totalWidth);
        this.setHeight((int)totalHeight);
        this.setX(Math.max(0, Math.min(this.getX(), screenWidth - (int)totalWidth)));
        this.setY(Math.max(0, Math.min(this.getY(), screenHeight - (int)totalHeight)));
        float x = this.getX();
        float y = this.getY();
        // Фона нет: только предметы брони и полоски прочности.
        for (int i = 0; i < 4; ++i) {
            ItemStack stack;
            float slotX = x + padding + (float)i * (slotSize + spacing);
            float slotY = y + padding;
            ItemStack itemStack = stack = i < this.armorStacks.size() ? this.armorStacks.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) continue;
            float itemSize = 16.0f;
            float itemX = slotX + (slotSize - itemSize) / 2.0f - 1.0f;
            float itemY = slotY + (slotSize - itemSize) / 2.0f;
            Render2D.defaultDrawStack(context, stack, itemX, itemY, false, false, 1.0f);
            if (!stack.isDamageable()) continue;
            int maxDurability = stack.getMaxDamage();
            int currentDurability = maxDurability - stack.getDamage();
            float durabilityPercent = (float)currentDurability / (float)maxDurability;
            float barX = slotX + 2.0f;
            float barY = slotY + slotSize + barSpacing;
            float barWidth = slotSize - 4.0f;
            rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth, durabilityBarHeight).round(1.0f).color(new Color(0, 0, 0, 255).getRGB()).build());
            Color barColor = durabilityPercent > 0.5f ? new Color(76, 187, 23) : (durabilityPercent > 0.25f ? new Color(255, 187, 40) : new Color(255, 51, 51));
            float filledWidth = barWidth * durabilityPercent;
            if (!(filledWidth > 0.5f)) continue;
            rectangle.render(ShapeProperties.create(matrix, barX, barY, filledWidth, durabilityBarHeight).round(1.0f).color(barColor.getRGB()).build());
        }
    }

    private void trackArmorSlot(EquipmentSlot slot) {
        Hud hud;
        ItemStack stack = ArmorBar.mc.player.getEquippedStack(slot);
        this.armorStacks.add(stack);
        if (stack.isEmpty() || !stack.isDamageable()) {
            this.trackedItems.remove(slot);
            this.warningStage.remove(slot);
            return;
        }
        if (this.trackedItems.get(slot) != stack.getItem()) {
            this.trackedItems.put(slot, stack.getItem());
            this.warningStage.put(slot, 0);
        }
        if ((hud = Hud.getInstance()) == null || !hud.notificationSettings.isSelected("\u0421\u043a\u043e\u0440\u043e\u0439 \u043f\u043e\u043b\u043e\u043c\u043a\u0435 \u0431\u0440\u043e\u043d\u0438")) {
            return;
        }
        int percent = Math.round((float)(stack.getMaxDamage() - stack.getDamage()) * 100.0f / (float)stack.getMaxDamage());
        int currentStage = this.warningStage.getOrDefault(slot, 0);
        if (percent > 15) {
            this.warningStage.put(slot, 0);
            return;
        }
        if (percent <= 5 && currentStage < 2) {
            this.notifyArmorState(stack, "\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c: " + stack.getName().getString() + " (" + percent + "%)", SoundManager.DISABLE_MODULE);
            this.warningStage.put(slot, 2);
            return;
        }
        if (currentStage < 1) {
            this.notifyArmorState(stack, "\u0421\u043a\u043e\u0440\u043e \u0441\u043b\u043e\u043c\u0430\u0435\u0442\u0441\u044f: " + stack.getName().getString() + " (" + percent + "%)", null);
            this.warningStage.put(slot, 1);
        }
    }

    private void notifyArmorState(ItemStack stack, String message, SoundEvent sound) {
        Notifications.getInstance().addList((Text)Text.literal((String)message), 3000, (Object)sound, false, stack.copyWithCount(1));
    }
}
