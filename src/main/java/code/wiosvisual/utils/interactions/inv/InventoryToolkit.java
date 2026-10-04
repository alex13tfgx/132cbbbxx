/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1657
 *  net.minecraft.class_1713
 *  net.minecraft.class_1743
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_2596
 *  net.minecraft.class_2645
 *  net.minecraft.class_2868
 *  net.minecraft.class_7204
 */
package code.wiosvisual.utils.interactions.inv;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.inv.InventoryResult;
import code.wiosvisual.utils.interactions.inv.InventoryTick;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public final class InventoryToolkit
implements QuickImports {
    private static int cachedSlot = -1;

    public static int getItemSlot(Item input) {
        for (ItemStack stack : java.util.List.of(InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET))) {
            if (stack.getItem() != input || stack.getDamage() >= 430) continue;
            return -2;
        }
        int slot = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = InventoryToolkit.mc.player.getInventory().getStack(i);
            if (stack.getItem() != input || stack.getDamage() >= 430) continue;
            slot = i;
            break;
        }
        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    public static void moveTo(int syncId, ItemStack stack, int slot) {
        if (Objects.isNull(stack)) {
            return;
        }
        InventoryToolkit.mc.interactionManager.clickSlot(syncId, InventoryToolkit.mc.player.getInventory().getSlotWithStack(stack), 0, SlotActionType.PICKUP, (PlayerEntity)InventoryToolkit.mc.player);
        InventoryToolkit.mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, (PlayerEntity)InventoryToolkit.mc.player);
    }

    public static InventoryResult findInInventory(Searcher searcher) {
        if (InventoryToolkit.mc.player != null) {
            for (ItemStack stack : java.util.List.of(InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET))) {
                if (!searcher.isValid(stack)) continue;
                return new InventoryResult(-2, true, stack);
            }
            for (int i = 40; i >= 0; --i) {
                ItemStack stack;
                stack = InventoryToolkit.mc.player.getInventory().getStack(i);
                if (!searcher.isValid(stack)) continue;
                if (i < 9) {
                    i += 36;
                }
                return new InventoryResult(i, true, stack);
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInInventory(List<Item> items) {
        return InventoryToolkit.findInInventory(stack -> items.contains(stack.getItem()));
    }

    public static InventoryResult findItemInInventory(Item ... items) {
        return InventoryToolkit.findItemInInventory(Arrays.asList(items));
    }

    public static int getAxe() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = InventoryToolkit.mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof AxeItem)) continue;
            return i;
        }
        return -1;
    }

    public static InventoryResult findItemInHotBar(Item item) {
        if (InventoryToolkit.mc.player == null) {
            return InventoryResult.notFound();
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = InventoryToolkit.mc.player.getInventory().getStack(i);
            if (stack.getItem() != item || stack.getDamage() >= 430) continue;
            return new InventoryResult(i, true, stack);
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInInventory(Item item) {
        if (InventoryToolkit.mc.player == null) {
            return InventoryResult.notFound();
        }
        for (ItemStack stack : java.util.List.of(InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS), InventoryToolkit.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET))) {
            if (stack.getItem() != item || stack.getDamage() >= 430) continue;
            return new InventoryResult(-2, true, stack);
        }
        for (int i = 36; i >= 0; --i) {
            ItemStack stack;
            stack = InventoryToolkit.mc.player.getInventory().getStack(i);
            if (stack.getItem() != item || stack.getDamage() >= 430) continue;
            if (i < 9) {
                i += 36;
            }
            return new InventoryResult(i, true, stack);
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findInHotBar(Searcher searcher) {
        if (InventoryToolkit.mc.player != null) {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = InventoryToolkit.mc.player.getInventory().getStack(i);
                if (!searcher.isValid(stack) || stack.getDamage() >= 430) continue;
                return new InventoryResult(i, true, stack);
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInHotBar(List<Item> items) {
        return InventoryToolkit.findInHotBar(stack -> items.contains(stack.getItem()));
    }

    public static InventoryResult findItemInHotBar(Item ... items) {
        return InventoryToolkit.findItemInHotBar(Arrays.asList(items));
    }

    public static void saveSlot() {
        cachedSlot = InventoryToolkit.mc.player.getInventory().selectedSlot;
    }

    public static void returnSlot() {
        if (cachedSlot != -1) {
            InventoryToolkit.switchTo(cachedSlot);
        }
        cachedSlot = -1;
    }

    public static void use(int slot) {
        if (slot != 1 && slot != 4 && slot != 21) {
            return;
        }
        if (slot < 9) {
            int n3 = InventoryToolkit.mc.player.getInventory().selectedSlot;
            InventoryToolkit.mc.player.getInventory().selectedSlot = slot - 1;
            mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot - 1));
            InventoryTick.schelude(() -> {
                InventoryToolkit.mc.interactionManager.interactItem((PlayerEntity)InventoryToolkit.mc.player, Hand.MAIN_HAND);
                InventoryToolkit.mc.player.swingHand(Hand.MAIN_HAND);
            }, 2);
            InventoryTick.schelude(() -> {
                InventoryToolkit.mc.player.getInventory().selectedSlot = n3;
                mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(n3));
            }, 2);
        } else {
            InventoryToolkit.mc.player.setSprinting(false);
            InventoryTick.schelude(() -> InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, slot, InventoryToolkit.mc.player.getInventory().selectedSlot, SlotActionType.SWAP, (PlayerEntity)InventoryToolkit.mc.player), 2);
            InventoryTick.schelude(() -> {
                InventoryToolkit.mc.interactionManager.interactItem((PlayerEntity)InventoryToolkit.mc.player, Hand.MAIN_HAND);
                InventoryToolkit.mc.player.swingHand(Hand.MAIN_HAND);
            }, 2);
            InventoryTick.schelude(() -> InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, slot, InventoryToolkit.mc.player.getInventory().selectedSlot, SlotActionType.SWAP, (PlayerEntity)InventoryToolkit.mc.player), 4);
            InventoryTick.schelude(() -> mc.getNetworkHandler().sendPacket((Packet)new CloseScreenS2CPacket(InventoryToolkit.mc.player.currentScreenHandler.syncId)), 2);
        }
    }

    public static void switchTo(int slot) {
        if (InventoryToolkit.mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (InventoryToolkit.mc.player.getInventory().selectedSlot == slot) {
            return;
        }
        InventoryToolkit.mc.player.getInventory().selectedSlot = slot;
    }

    public static void switchToSilent(int slot) {
        if (InventoryToolkit.mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || InventoryToolkit.mc.world == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket(packetCreator.predict(0));
    }

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void clickSlot(int id) {
        if (id == -1 || InventoryToolkit.mc.interactionManager == null || InventoryToolkit.mc.player == null) {
            return;
        }
        InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, id, 0, SlotActionType.PICKUP, (PlayerEntity)InventoryToolkit.mc.player);
    }

    public static void clickSlot(int id, SlotActionType type) {
        if (id == -1 || InventoryToolkit.mc.interactionManager == null || InventoryToolkit.mc.player == null) {
            return;
        }
        InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, id, 0, type, (PlayerEntity)InventoryToolkit.mc.player);
    }

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id == -1 || InventoryToolkit.mc.interactionManager == null || InventoryToolkit.mc.player == null) {
            return;
        }
        InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, id, button, type, (PlayerEntity)InventoryToolkit.mc.player);
    }

    public static ItemStack byItem(Item item) {
        for (int i = 0; i < InventoryToolkit.mc.player.getInventory().size(); ++i) {
            ItemStack itemStack = InventoryToolkit.mc.player.getInventory().getStack(i);
            if (!itemStack.getItem().equals(item) || itemStack.getDamage() >= 430) continue;
            return itemStack;
        }
        return null;
    }

    public static boolean quickMoveFromTo(int from, int to) {
        if (from == -1 || to == -1 || InventoryToolkit.mc.interactionManager == null || InventoryToolkit.mc.player == null) {
            return false;
        }
        InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, (PlayerEntity)InventoryToolkit.mc.player);
        InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, to, 0, SlotActionType.PICKUP, (PlayerEntity)InventoryToolkit.mc.player);
        InventoryToolkit.mc.interactionManager.clickSlot(InventoryToolkit.mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, (PlayerEntity)InventoryToolkit.mc.player);
        return true;
    }

    public static int getSlotWithStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < InventoryToolkit.mc.player.getInventory().size(); ++i) {
            ItemStack invStack = InventoryToolkit.mc.player.getInventory().getStack(i);
            if (!ItemStack.areEqual((ItemStack)invStack, (ItemStack)stack)) continue;
            return i;
        }
        return -1;
    }

    public static interface Searcher {
        public boolean isValid(ItemStack var1);
    }
}
