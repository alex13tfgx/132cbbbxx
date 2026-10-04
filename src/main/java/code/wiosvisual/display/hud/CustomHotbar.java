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

import code.wiosvisual.features.impl.misc.FastSwap;

import code.wiosvisual.common.animation.Animation;
import code.wiosvisual.common.animation.Direction;
import code.wiosvisual.common.animation.implement.Decelerate;
import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class CustomHotbar
extends AbstractDraggable {
    private static final float STATUS_BAR_Y_OFFSET = 39.0f;
    private static final float SLOT_SIZE = 20.0f;
    private static final float SLOT_SPACING = 2.0f;
    private static final float PANEL_PADDING = 3.0f;
    private static final int HOTBAR_SLOTS = 9;
    private static final float OFFHAND_GAP = 6.0f;
    private final Map<Integer, Animation> slotAnimations = new HashMap<Integer, Animation>();

    public static HotbarLayout computeLayout(int screenWidth, int screenHeight) {
        float rightEdge;
        float slotsWidth = 196.0f;
        float panelHeight = 26.0f;
        float hotbarPanelWidth = 6.0f + slotsWidth;
        float hotbarPanelX = ((float)screenWidth - hotbarPanelWidth) / 2.0f;
        float hotbarPanelY = (float)screenHeight - panelHeight - 2.0f;
        float offhandPanelWidth = 26.0f;
        float offhandPanelX = hotbarPanelX - 6.0f - offhandPanelWidth;
        if (offhandPanelX < 1.0f) {
            float shift = 1.0f - offhandPanelX;
            offhandPanelX += shift;
            hotbarPanelX += shift;
        }
        if ((rightEdge = hotbarPanelX + hotbarPanelWidth) > (float)screenWidth - 1.0f) {
            float shift = rightEdge - ((float)screenWidth - 1.0f);
            offhandPanelX -= shift;
            hotbarPanelX -= shift;
        }
        return new HotbarLayout(offhandPanelX, hotbarPanelX, hotbarPanelY, hotbarPanelWidth, panelHeight);
    }

    public static float getArmorBarGap() {
        return 6.0f;
    }

    public CustomHotbar() {
        super("CustomHotbar", 0, 0, 197, 24, false);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
        for (int i = 0; i < 9; ++i) {
            this.slotAnimations.put(i, new Decelerate().setMs(200).setValue(0.0));
        }
    }

    @Override
    public boolean visible() {
        Hud.getInstance();
        return Hud.interfaceSettings.isSelected("CustomHotbar") && Hud.getInstance().state;
    }

    @Override
    public void tick() {
        if (CustomHotbar.mc.player == null) {
            return;
        }
        int currentSlot = CustomHotbar.mc.player.getInventory().selectedSlot;
        for (int i = 0; i < 9; ++i) {
            Animation anim = this.slotAnimations.get(i);
            if (i == currentSlot) {
                anim.setDirection(Direction.FORWARDS);
                continue;
            }
            anim.setDirection(Direction.BACKWARDS);
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!this.visible() || CustomHotbar.mc.player == null) {
            return;
        }
        MatrixStack matrix = GuiCompat.stack(context);
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();
        HotbarLayout layout = CustomHotbar.computeLayout(screenWidth, screenHeight);
        float offhandPanelX = layout.offhandPanelX();
        float hotbarPanelX = layout.hotbarPanelX();
        float hotbarPanelY = layout.hotbarPanelY();
        float hotbarPanelWidth = layout.hotbarPanelWidth();
        float panelHeight = layout.panelHeight();
        float offhandPanelWidth = 26.0f;
        float offhandPanelY = hotbarPanelY;
        this.setX((int)offhandPanelX);
        this.setY((int)hotbarPanelY);
        this.setWidth((int)(hotbarPanelX + hotbarPanelWidth - offhandPanelX));
        this.setHeight((int)panelHeight);
        this.clampToScreen();
        this.drawExperienceLevel(matrix, screenWidth, screenHeight);
        this.drawPanelBackground(matrix, offhandPanelX, offhandPanelY, offhandPanelWidth, panelHeight);
        this.drawOffhand(context, matrix, offhandPanelX + 3.0f, offhandPanelY + 3.0f, 20.0f);
        this.drawPanelBackground(matrix, hotbarPanelX, hotbarPanelY, hotbarPanelWidth, panelHeight);
        int selectedSlot = CustomHotbar.mc.player.getInventory().selectedSlot;
        float rowY = hotbarPanelY + 3.0f;
        float hotbarStartX = hotbarPanelX + 3.0f;
        FontRenderer slotNumberFont = Fonts.getSize(20, Fonts.Type.SuisseIntlMedium);
        for (int i = 0; i < 9; ++i) {
            float animValue = this.slotAnimations.get(i).getOutput().floatValue();
            float slotX = hotbarStartX + (float)i * 22.0f;
            float slotY = rowY;
            boolean isSelected = i == selectedSlot;
            float offset = animValue * -2.0f;
            matrix.push();
            matrix.translate(slotX + 10.0f, (slotY += offset) + 10.0f, 0.0f);
            float scale = 1.0f + animValue * 0.1f;
            matrix.scale(scale, scale, 1.0f);
            matrix.translate(-(slotX + 10.0f), -(slotY + 10.0f), 0.0f);
            if (isSelected) {
                int alpha = (int)(255.0f * (0.3f + animValue * 0.7f));
                rectangle.render(ShapeProperties.create(matrix, slotX, slotY, 20.0, 20.0).round(4.0f).thickness(0.5f).outlineColor(ColorAssist.multAlpha(HudTheme.hotbarIcon(255), alpha)).softness(2.5f).color(ThemeManager.BackgroundSettings.getColor() | 0xFF000000).build());
            } else {
                rectangle.render(ShapeProperties.create(matrix, slotX, slotY, 20.0, 20.0).round(4.0f).color(ThemeManager.BackgroundSettings.getColor()).build());
            }
            ItemStack stack = CustomHotbar.mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                Render2D.defaultDrawStack(context, stack, slotX + 2.0f, slotY + 2.0f, true, true, 1.0f);
                FastSwap.drawBindLabel(context, stack, slotX + 2.0f, slotY + 2.0f);
            } else {
                String slotNumber = String.valueOf(i + 1);
                int textAlpha = isSelected ? 200 : 80;
                float slotCenterX = slotX + 10.0f;
                slotNumberFont.drawCenteredBoth(matrix, slotNumber, slotCenterX, slotY, 20.0f, new Color(255, 255, 255, textAlpha).getRGB());
            }
            matrix.pop();
        }
    }

    private void drawPanelBackground(MatrixStack matrix, float x, float y, float width, float height) {
        Hud.getInstance();
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(5.0f).softness(2.0f).color(HudTheme.panelTop(242), HudTheme.panelTop(242), HudTheme.panelBottom(242), HudTheme.panelBottom(242)).build());
    }

    private void drawOffhand(DrawContext context, MatrixStack matrix, float slotX, float slotY, float slotSize) {
        rectangle.render(ShapeProperties.create(matrix, slotX, slotY, slotSize, slotSize).round(4.0f).thickness(0.5f).outlineColor(ColorAssist.multAlpha(HudTheme.hotbarIcon(255), 120.0f)).softness(2.5f).color(ThemeManager.BackgroundSettings.getColor()).build());
        ItemStack offhand = CustomHotbar.mc.player.getOffHandStack();
        if (!offhand.isEmpty()) {
            Render2D.defaultDrawStack(context, offhand, slotX + 2.0f, slotY + 2.0f, true, true, 1.0f);
        } else {
            FontRenderer iconFont = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);
            String label = "L";
            iconFont.drawCenteredBoth(matrix, label, slotX + slotSize / 2.0f, slotY, slotSize, new Color(255, 255, 255, 70).getRGB());
        }
    }

    private void drawExperienceLevel(MatrixStack matrix, int screenWidth, int screenHeight) {
        int xpLevel = CustomHotbar.mc.player.experienceLevel;
        if (xpLevel <= 0) {
            return;
        }
        FontRenderer levelFont = Fonts.getSize(16, Fonts.Type.SuisseIntlMedium);
        String levelText = String.valueOf(xpLevel);
        float levelBoxY = (float)screenHeight - 39.0f;
        levelFont.drawCenteredBoth(matrix, levelText, (float)screenWidth / 2.0f, levelBoxY, 9.0f, new Color(126, 252, 80).getRGB());
    }

    public record HotbarLayout(float offhandPanelX, float hotbarPanelX, float hotbarPanelY, float hotbarPanelWidth, float panelHeight) {
    }
}
