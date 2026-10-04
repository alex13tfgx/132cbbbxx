/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1280
 *  net.minecraft.class_1282
 *  net.minecraft.class_1294
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_1743
 *  net.minecraft.class_1748
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1810
 *  net.minecraft.class_1821
 *  net.minecraft.class_1829
 *  net.minecraft.class_2248
 *  net.minecraft.class_2596
 *  net.minecraft.class_2868
 *  net.minecraft.class_5134
 *  net.minecraft.class_7923
 *  net.minecraft.class_9285
 *  net.minecraft.class_9285$class_9287
 *  net.minecraft.class_9334
 */
package code.wiosvisual.utils.interactions.inv;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.inv.SlotSearchResult;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

public final class HotbarUtil
implements QuickImports {
    private static int cachedSlot = -1;

    public static int getItemCount(Item item) {
        if (HotbarUtil.mc.player == null) {
            return 0;
        }
        int counter = 0;
        for (int i = 0; i < HotbarUtil.mc.player.getInventory().size(); ++i) {
            ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i);
            if (!stack.isOf(item)) continue;
            counter += stack.getCount();
        }
        return counter;
    }

    public static SlotSearchResult getAxe() {
        return HotbarUtil.findBest(itemStack -> itemStack.isIn(ItemTags.AXES), false);
    }

    public static SlotSearchResult getAxeHotBar() {
        return HotbarUtil.findBest(itemStack -> itemStack.isIn(ItemTags.AXES), true);
    }

    public static SlotSearchResult getPickAxe() {
        return HotbarUtil.findBest(itemStack -> itemStack.isIn(ItemTags.PICKAXES), false);
    }

    public static SlotSearchResult getPickAxeHotbar() {
        return HotbarUtil.getPickAxeHotBar();
    }

    public static SlotSearchResult getPickAxeHotBar() {
        return HotbarUtil.findBest(itemStack -> itemStack.isIn(ItemTags.PICKAXES), true);
    }

    public static SlotSearchResult getSword() {
        return HotbarUtil.findBest(itemStack -> itemStack.isIn(ItemTags.SWORDS), false);
    }

    public static SlotSearchResult getSwordHotBar() {
        return HotbarUtil.findBest(itemStack -> itemStack.isIn(ItemTags.SWORDS), true);
    }

    public static SlotSearchResult getSkull() {
        return HotbarUtil.findInHotBar(stack -> stack.isOf(Items.SKELETON_SKULL) || stack.isOf(Items.WITHER_SKELETON_SKULL) || stack.isOf(Items.CREEPER_HEAD) || stack.isOf(Items.PLAYER_HEAD) || stack.isOf(Items.ZOMBIE_HEAD));
    }

    public static int getElytra() {
        if (HotbarUtil.mc.player == null) {
            return -1;
        }
        ItemStack chestStack = HotbarUtil.mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        if (chestStack.isOf(Items.ELYTRA) && chestStack.getDamage() < chestStack.getMaxDamage() - 1) {
            return -2;
        }
        for (int i = 0; i < 36; ++i) {
            ItemStack stack;
            stack = HotbarUtil.mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.ELYTRA) || stack.getDamage() >= stack.getMaxDamage() - 1) continue;
            return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    public static SlotSearchResult findInHotBar(Searcher searcher) {
        if (HotbarUtil.mc.player != null) {
            if (searcher.isValid(HotbarUtil.mc.player.getOffHandStack())) {
                return SlotSearchResult.inOffhand(HotbarUtil.mc.player.getOffHandStack());
            }
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i);
                if (!searcher.isValid(stack)) continue;
                return new SlotSearchResult(i, true, stack);
            }
        }
        return SlotSearchResult.notFound();
    }

    public static SlotSearchResult findItemInHotBar(List<Item> items) {
        return HotbarUtil.findInHotBar(stack -> items.contains(stack.getItem()));
    }

    public static SlotSearchResult findItemInHotBar(Item ... items) {
        return HotbarUtil.findItemInHotBar(Arrays.asList(items));
    }

    public static SlotSearchResult findInInventory(Searcher searcher) {
        if (HotbarUtil.mc.player != null) {
            for (int i = 35; i >= 0; --i) {
                ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i);
                if (!searcher.isValid(stack)) continue;
                return new SlotSearchResult(i, true, stack);
            }
        }
        return SlotSearchResult.notFound();
    }

    public static SlotSearchResult findItemInInventory(List<Item> items) {
        return HotbarUtil.findInInventory(stack -> items.contains(stack.getItem()));
    }

    public static SlotSearchResult findItemInInventory(Item ... items) {
        return HotbarUtil.findItemInInventory(Arrays.asList(items));
    }

    public static SlotSearchResult findBlockInHotBar(@NotNull List<Block> blocks) {
        return HotbarUtil.findItemInHotBar(blocks.stream().map(Block::asItem).toList());
    }

    public static SlotSearchResult findBlockInHotBar(Block ... blocks) {
        return HotbarUtil.findItemInHotBar(Arrays.stream(blocks).map(Block::asItem).toList());
    }

    public static SlotSearchResult findBlockInInventory(@NotNull List<Block> blocks) {
        return HotbarUtil.findItemInInventory(blocks.stream().map(Block::asItem).toList());
    }

    public static SlotSearchResult findBlockInInventory(Block ... blocks) {
        return HotbarUtil.findItemInInventory(Arrays.stream(blocks).map(Block::asItem).toList());
    }

    public static void saveSlot() {
        if (HotbarUtil.mc.player != null) {
            cachedSlot = HotbarUtil.mc.player.getInventory().selectedSlot;
        }
    }

    public static void returnSlot() {
        if (cachedSlot != -1) {
            HotbarUtil.switchTo(cachedSlot);
        }
        cachedSlot = -1;
    }

    public static void saveAndSwitchTo(int slot) {
        HotbarUtil.saveSlot();
        HotbarUtil.switchTo(slot);
    }

    public static void switchTo(int slot) {
        if (HotbarUtil.mc.player == null || mc.getNetworkHandler() == null || slot < 0 || slot > 8) {
            return;
        }
        if (HotbarUtil.mc.player.getInventory().selectedSlot == slot) {
            return;
        }
        HotbarUtil.mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void switchToSilent(int slot) {
        if (HotbarUtil.mc.player == null || mc.getNetworkHandler() == null || slot < 0 || slot > 8) {
            return;
        }
        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
    }

    public static SlotSearchResult getAntiWeaknessItem() {
        if (HotbarUtil.mc.player == null) {
            return SlotSearchResult.notFound();
        }
        ItemStack mainHand = HotbarUtil.mc.player.getMainHandStack();
        if (mainHand.isIn(ItemTags.SWORDS) || mainHand.isIn(ItemTags.PICKAXES) || mainHand.isIn(ItemTags.AXES) || mainHand.isIn(ItemTags.SHOVELS)) {
            return new SlotSearchResult(HotbarUtil.mc.player.getInventory().selectedSlot, true, HotbarUtil.mc.player.getMainHandStack());
        }
        return HotbarUtil.findInHotBar(stack -> stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.AXES) || stack.isIn(ItemTags.SHOVELS));
    }

    public static float getHitDamage(@NotNull ItemStack weapon, PlayerEntity entity) {
        if (HotbarUtil.mc.player == null || HotbarUtil.mc.world == null) {
            return 0.0f;
        }
        float baseDamage = HotbarUtil.getBaseAttackDamage(weapon);
        if (HotbarUtil.mc.player.fallDistance > 0.0f) {
            baseDamage += baseDamage / 2.0f;
        }
        if (HotbarUtil.mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int strength = Objects.requireNonNull(HotbarUtil.mc.player.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            baseDamage += 3.0f * (float)strength;
        }
        return DamageUtil.getDamageLeft((LivingEntity)entity, (float)baseDamage, (DamageSource)HotbarUtil.mc.world.getDamageSources().generic(), (float)entity.getArmor(), (float)((float)entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS)));
    }

    public static SlotSearchResult findBedInHotBar() {
        return HotbarUtil.findInHotBar(stack -> stack.getItem() instanceof BedItem);
    }

    public static SlotSearchResult findBed() {
        return HotbarUtil.findInInventory(stack -> stack.getItem() instanceof BedItem);
    }

    public static Item getItem(String name) {
        if (name == null) {
            return Items.AIR;
        }
        String normalized = name.toLowerCase();
        for (Block block : Registries.BLOCK) {
            if (!block.getTranslationKey().replace("block.minecraft.", "").equals(normalized)) continue;
            return Item.fromBlock((Block)block);
        }
        for (Item item : Registries.ITEM) {
            if (!item.getTranslationKey().replace("item.minecraft.", "").equals(normalized)) continue;
            return item;
        }
        return Items.DIRT;
    }

    public static int getBedsCount() {
        if (HotbarUtil.mc.player == null) {
            return 0;
        }
        int counter = 0;
        for (int i = 0; i < HotbarUtil.mc.player.getInventory().size(); ++i) {
            ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof BedItem)) continue;
            counter += stack.getCount();
        }
        return counter;
    }

    private static SlotSearchResult findBest(Searcher searcher, boolean hotbarOnly) {
        if (HotbarUtil.mc.player == null) {
            return SlotSearchResult.notFound();
        }
        int bestSlot = -1;
        float bestDamage = 0.0f;
        int end = hotbarOnly ? 8 : 35;
        for (int i = 0; i <= end; ++i) {
            float damage;
            ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i);
            if (!searcher.isValid(stack) || !((damage = HotbarUtil.getBaseAttackDamage(stack)) > bestDamage)) continue;
            bestDamage = damage;
            bestSlot = i;
        }
        return bestSlot == -1 ? SlotSearchResult.notFound() : new SlotSearchResult(bestSlot, true, HotbarUtil.mc.player.getInventory().getStack(bestSlot));
    }

    private static float getBaseAttackDamage(ItemStack stack) {
        AttributeModifiersComponent component = (AttributeModifiersComponent)stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        double damage = 1.0;
        for (AttributeModifiersComponent.Entry entry : component.modifiers()) {
            if (!entry.attribute().equals((Object)EntityAttributes.ATTACK_DAMAGE)) continue;
            damage += entry.modifier().value();
        }
        return (float)damage;
    }

    public static boolean isHolding(Item item) {
        return HotbarUtil.mc.player != null && (HotbarUtil.mc.player.getMainHandStack().isOf(item) || HotbarUtil.mc.player.getOffHandStack().isOf(item));
    }

    public static Hand getHand(Item item) {
        if (HotbarUtil.mc.player == null) {
            return null;
        }
        if (HotbarUtil.mc.player.getOffHandStack().isOf(item)) {
            return Hand.OFF_HAND;
        }
        if (HotbarUtil.mc.player.getMainHandStack().isOf(item)) {
            return Hand.MAIN_HAND;
        }
        return null;
    }

    private HotbarUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static interface Searcher {
        public boolean isValid(ItemStack var1);
    }
}
