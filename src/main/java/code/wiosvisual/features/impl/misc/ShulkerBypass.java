/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_1713
 *  net.minecraft.class_1733
 *  net.minecraft.class_1735
 *  net.minecraft.class_437
 *  net.minecraft.class_465
 *  net.minecraft.class_495
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ShulkerBypass
extends Module {
    public ShulkerBypass() {
        super("ShulkerBypass", "Shulker Bypass", ModuleCategory.MISC);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        Screen screen2 = ShulkerBypass.mc.currentScreen;
        if (!(screen2 instanceof ShulkerBoxScreen)) {
            return;
        }
        ShulkerBoxScreen screen = (ShulkerBoxScreen)screen2;
        if (!(ShulkerBypass.mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
            return;
        }
        Slot hoveredSlot = this.getSlotUnderMouse((HandledScreen<?>)screen);
        if (hoveredSlot == null || !hoveredSlot.hasStack()) {
            return;
        }
        int emptySlot = this.findEmptyHotbarOrInventorySlot();
        if (emptySlot == -1) {
            return;
        }
        ShulkerBypass.mc.interactionManager.clickSlot(ShulkerBypass.mc.player.currentScreenHandler.syncId, hoveredSlot.id, emptySlot, SlotActionType.SWAP, (PlayerEntity)ShulkerBypass.mc.player);
    }

    private Slot getSlotUnderMouse(HandledScreen<?> screen) {
        double mouseX = ShulkerBypass.mc.mouse.getX() * (double)mc.getWindow().getScaledWidth() / (double)mc.getWindow().getWidth();
        double mouseY = ShulkerBypass.mc.mouse.getY() * (double)mc.getWindow().getScaledHeight() / (double)mc.getWindow().getHeight();
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!this.isPointOverSlot(slot, mouseX, mouseY, screen)) continue;
            return slot;
        }
        return null;
    }

    private boolean isPointOverSlot(Slot slot, double pointX, double pointY, HandledScreen<?> screen) {
        int x = screen.x + slot.x;
        int y = screen.y + slot.y;
        return pointX >= (double)(x - 1) && pointX < (double)(x + 16 + 1) && pointY >= (double)(y - 1) && pointY < (double)(y + 16 + 1);
    }

    private int findEmptyHotbarOrInventorySlot() {
        for (Slot slot : ShulkerBypass.mc.player.currentScreenHandler.slots) {
            if (slot.hasStack() || slot.inventory != ShulkerBypass.mc.player.getInventory()) continue;
            return slot.getIndex();
        }
        return -1;
    }
}
