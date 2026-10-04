/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TotemBar
extends AbstractDraggable {
    private int totemCount = 0;

    public TotemBar() {
        super("TotemBar", 10, 150, 29, 36, true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    @Override
    public boolean visible() {
        Hud.getInstance();
        return Hud.interfaceSettings.isSelected("TotemBar") && Hud.getInstance().state;
    }

    @Override
    public void tick() {
        if (TotemBar.mc.player == null) {
            this.totemCount = 0;
            return;
        }
        this.totemCount = 0;
        for (int i = 0; i < TotemBar.mc.player.getInventory().size(); ++i) {
            ItemStack stack = TotemBar.mc.player.getInventory().getStack(i);
            if (stack.getItem() != Items.TOTEM_OF_UNDYING) continue;
            this.totemCount += stack.getCount();
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!this.visible()) {
            return;
        }
        MatrixStack matrix = GuiCompat.stack(context);
        float slotSize = 26.0f;
        float padding = 4.0f;
        float textHeight = 12.0f;
        float totalWidth = slotSize + padding * 2.0f;
        float totalHeight = slotSize + textHeight + padding * 3.0f;
        this.setWidth((int)totalWidth);
        this.setHeight((int)totalHeight);
        float x = this.getX();
        float y = this.getY();
        Hud.getInstance();
        rectangle.render(ShapeProperties.create(matrix, x, y, totalWidth, totalHeight).round(5.0f).softness(2.0f).color(HudTheme.panelTop(242), HudTheme.panelTop(242), HudTheme.panelBottom(242), HudTheme.panelBottom(242)).build());
        float slotX = x + padding;
        float slotY = y + padding;
        rectangle.render(ShapeProperties.create(matrix, slotX, slotY, slotSize, slotSize).round(4.0f).color(ThemeManager.BackgroundSettings.getColor()).build());
        ItemStack totemStack = new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING);
        float itemSize = 16.0f;
        float itemX = slotX + (slotSize - itemSize) / 2.0f - 1.0f;
        float itemY = slotY + (slotSize - itemSize) / 2.0f - 1.0f;
        Render2D.defaultDrawStack(context, totemStack, itemX, itemY, false, false, 1.0f);
        FontRenderer countFont = Fonts.getSize(21, Fonts.Type.SuisseIntlMedium);
        String countText = String.valueOf(this.totemCount);
        float textWidth = countFont.getStringWidth(countText);
        float countTextHeight = countFont.getStringHeight(countText);
        float textX = x + totalWidth / 2.0f - textWidth / 2.0f;
        float textY = slotY + slotSize + padding + (textHeight - countTextHeight) / 2.0f - 1.5f;
        int textColor = this.totemCount == 0 ? new Color(255, 85, 75, 255).getRGB() : (this.totemCount < 5 ? new Color(255, 170, 0, 255).getRGB() : ThemeManager.textColor.getColor());
        countFont.drawString(matrix, countText, textX, textY, textColor);
    }
}
