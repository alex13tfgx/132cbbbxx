/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1657
 *  net.minecraft.class_1799
 *  net.minecraft.class_1839
 */
package code.wiosvisual.utils.interactions.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;

public final class ItemUseHelper {
    private ItemUseHelper() {
    }

    public static boolean isPriorityUseItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        UseAction action = stack.getUseAction();
        return action == UseAction.EAT || action == UseAction.DRINK || action == UseAction.BOW || action == UseAction.CROSSBOW || action == UseAction.SPYGLASS || action == UseAction.BLOCK;
    }

    public static boolean isConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        UseAction action = stack.getUseAction();
        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    public static boolean isUsingPriorityItem(PlayerEntity player) {
        if (player == null || !player.isUsingItem()) {
            return false;
        }
        return ItemUseHelper.isPriorityUseItem(player.getActiveItem());
    }

    public static ItemStack getStackForHand(PlayerEntity player, Hand hand) {
        if (player == null || hand == null) {
            return ItemStack.EMPTY;
        }
        return player.getStackInHand(hand);
    }
}
