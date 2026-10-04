/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMaps
 *  net.minecraft.class_124
 *  net.minecraft.class_1268
 *  net.minecraft.class_1291
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1703
 *  net.minecraft.class_1713
 *  net.minecraft.class_1730
 *  net.minecraft.class_1735
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1844
 *  net.minecraft.class_2561
 *  net.minecraft.class_2596
 *  net.minecraft.class_2813
 *  net.minecraft.class_2815
 *  net.minecraft.class_4081
 *  net.minecraft.class_4174
 *  net.minecraft.class_6880
 *  net.minecraft.class_7923
 *  net.minecraft.class_9334
 */
package code.wiosvisual.utils.interactions.inv;

import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.interactions.inv.InventoryFlowManager;
import code.wiosvisual.utils.interactions.item.ItemTask;
import code.wiosvisual.utils.math.calc.Calculate;
import code.wiosvisual.utils.math.script.Script;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class InventoryTask
implements QuickImports {
    private static final Script swapAndUseScript = new Script();

    public static void moveItem(Slot from, int to) {
        if (from != null) {
            InventoryTask.moveItem(from.id, to, false, false);
        }
    }

    public static void moveItem(Slot from, int to, boolean task) {
        InventoryTask.moveItem(from, to, task, false);
    }

    public static void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
        if (from != null) {
            InventoryTask.moveItem(from.id, to, task, updateInventory);
        }
    }

    public static void moveItem(int from, int to, boolean task, boolean updateInventory) {
        if (from == to || from == -1) {
            return;
        }
        int count = Math.toIntExact(InventoryTask.slots().count()) - 10;
        if (from >= count && count == 36) {
            if (task) {
                InventoryFlowManager.addTask(() -> InventoryTask.clickSlot(to, from - count, SlotActionType.SWAP, false));
            } else {
                InventoryTask.clickSlot(to, from - count, SlotActionType.SWAP, false);
            }
            return;
        }
        if (task) {
            InventoryFlowManager.addTask(() -> InventoryTask.moveItem(from, to, updateInventory));
        } else {
            InventoryTask.moveItem(from, to, updateInventory);
        }
    }

    public static void moveItem(int from, int to, boolean updateInventory) {
        InventoryTask.clickSlot(from, 0, SlotActionType.SWAP, false);
        InventoryTask.clickSlot(to, 0, SlotActionType.SWAP, false);
        InventoryTask.clickSlot(from, 0, SlotActionType.SWAP, false);
        if (updateInventory) {
            InventoryTask.updateSlots();
        }
    }

    public static void swapHand(Slot slot, Hand hand, boolean task) {
        InventoryTask.swapHand(slot, hand, task, false);
    }

    public static void swapHand(Slot slot, Hand hand, boolean task, boolean updateInventory) {
        int button;
        if (slot == null || slot.id == -1 || hand.equals((Object)Hand.OFF_HAND) && !(slot.inventory instanceof PlayerInventory) && !(slot.inventory instanceof EnderChestInventory)) {
            return;
        }
        int n = button = hand.equals((Object)Hand.MAIN_HAND) ? InventoryTask.mc.player.getInventory().selectedSlot : 40;
        if (task) {
            InventoryFlowManager.addTask(() -> InventoryTask.swap(slot, button, updateInventory));
        } else {
            InventoryTask.swap(slot, button, updateInventory);
        }
    }

    public static void swap(Slot slot, int button, boolean updateInventory) {
        InventoryTask.clickSlot(slot, button, SlotActionType.SWAP, false);
        if (updateInventory && "Funtime".equals(InventoryFlowManager.INVENTORY_MODE)) {
            InventoryTask.updateSlots();
        }
    }

    public static void swapAndUse(Slot slot, String text, boolean task) {
        if (slot == null) {
            Notifications.getInstance().addList(String.valueOf(Formatting.RED) + text + String.valueOf(Formatting.RESET) + " - \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!", 3000);
            return;
        }
        if (task) {
            InventoryFlowManager.addTask(() -> InventoryTask.swapAndUse(slot, MathAngle.cameraAngle()));
        } else {
            InventoryTask.swapAndUse(slot, MathAngle.cameraAngle());
        }
    }

    public static void swapAndUse(Item item, boolean task) {
        InventoryTask.swapAndUse(item, MathAngle.cameraAngle(), task);
    }

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id == -1 || InventoryTask.mc.interactionManager == null || InventoryTask.mc.player == null) {
            return;
        }
        InventoryTask.mc.interactionManager.clickSlot(InventoryTask.mc.player.currentScreenHandler.syncId, id, button, type, (PlayerEntity)InventoryTask.mc.player);
    }

    public static void switchTo(int slot) {
        if (InventoryTask.mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (InventoryTask.mc.player.getInventory().selectedSlot == slot) {
            return;
        }
        InventoryTask.mc.player.getInventory().selectedSlot = slot;
    }

    public static void swapSlot(Slot targetSlot) {
        InventoryTask.swapSlot(targetSlot, false);
    }

    public static void swapSlot(Slot targetSlot, boolean task) {
        if (targetSlot == null || targetSlot.id == -1 || InventoryTask.mc.player == null) {
            return;
        }
        InventoryTask.mc.player.getInventory().selectedSlot = targetSlot.id;
    }

    public static void swapSlot(Item item) {
        InventoryTask.swapSlot(InventoryTask.getSlot(item));
    }

    public static void swapSlot(Item item, boolean task) {
        InventoryTask.swapSlot(InventoryTask.getSlot(item), task);
    }

    public static void swapAndUse(Item item, String searchName, boolean task) {
        float cooldownProgress = ItemTask.getCooldownProgress(item);
        if (cooldownProgress > 0.0f) {
            String time = Calculate.round(cooldownProgress, 0.1) + "\u0441";
            Notifications.getInstance().addList(String.valueOf(Formatting.RED) + item.getName().getString() + String.valueOf(Formatting.RESET) + " - \u0432 \u043a\u0434 \u0435\u0449\u0435 " + time, 2000);
            return;
        }
        Slot slot = InventoryTask.getSlot((Slot s) -> s.getStack().getItem().equals(item) && InventoryTask.getCleanName(s.getStack().getName()).contains(searchName.toLowerCase()));
        if (slot == null) {
            Notifications.getInstance().addList(String.valueOf(Formatting.RED) + item.getName().getString() + String.valueOf(Formatting.RESET) + " - \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!", 2000);
            return;
        }
        if (task) {
            InventoryFlowManager.addTask(() -> InventoryTask.swapAndUse(slot, MathAngle.cameraAngle()));
        } else {
            InventoryTask.swapAndUse(slot, MathAngle.cameraAngle());
        }
    }

    public static void swapAndUse(Item item, Turns angle, boolean task) {
        float cooldownProgress = ItemTask.getCooldownProgress(item);
        if (cooldownProgress > 0.0f) {
            String time = Calculate.round(cooldownProgress, 0.1) + "\u0441";
            Notifications.getInstance().addList(String.valueOf(Formatting.RED) + item.getName().getString() + String.valueOf(Formatting.RESET) + " - \u0432 \u043a\u0434 \u0435\u0449\u0435 " + time, 2000);
            return;
        }
        Slot slot = InventoryTask.getSlot(item);
        if (slot == null) {
            Notifications.getInstance().addList(String.valueOf(Formatting.RED) + item.getName().getString() + String.valueOf(Formatting.RESET) + " - \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!", 2000);
            return;
        }
        if (task) {
            InventoryFlowManager.addTask(() -> InventoryTask.swapAndUse(slot, angle));
        } else {
            InventoryTask.swapAndUse(slot, angle);
        }
    }

    public static void updateSwapAndUseScript() {
        swapAndUseScript.update();
    }

    public static void swapAndUse(Slot slot, Turns angle) {
        int delay2;
        int delay1;
        if ("Holyworld".equals(InventoryFlowManager.INVENTORY_MODE)) {
            delay1 = 2;
            delay2 = 1;
        } else if ("Funtime".equals(InventoryFlowManager.INVENTORY_MODE)) {
            delay1 = 0;
            delay2 = 1;
        } else {
            delay1 = 0;
            delay2 = 0;
        }
        swapAndUseScript.cleanup().addTickStep(0, () -> InventoryTask.swapHand(slot, Hand.MAIN_HAND, false)).addTickStep(delay1, () -> PlayerInteractionHelper.interactItem(Hand.MAIN_HAND)).addTickStep(delay2, () -> InventoryTask.swapHand(slot, Hand.MAIN_HAND, false, true));
    }

    public static void updateSlots() {
        ScreenHandler screenHandler = InventoryTask.mc.player.currentScreenHandler;
        ItemStack stack = ((Item)Registries.ITEM.get(Calculate.getRandom(0, 100))).getDefaultStack();
        // TODO(port 1.21.11): ClickSlotC2SPacket (class_2813) перешёл на hashed-stacks (class_10938) вместо ItemStack; отправка клика отключена.
    }

    public static void closeScreen(boolean packet) {
        if (packet) {
            InventoryTask.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(InventoryTask.mc.player.currentScreenHandler.syncId));
        } else {
            InventoryTask.mc.player.closeHandledScreen();
        }
    }

    public static void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
        if (slot != null) {
            InventoryTask.clickSlot(slot.id, button, clickType, silent);
        }
    }

    public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        InventoryTask.clickSlot(slotId, buttonId, clickType, silent, false);
    }

    public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent, boolean task) {
        InventoryTask.clickSlot(InventoryTask.mc.player.currentScreenHandler.syncId, slotId, buttonId, clickType, silent, task);
    }

    public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent, boolean task) {
        if (task) {
            InventoryFlowManager.addTask(() -> {
                InventoryTask.mc.interactionManager.clickSlot(windowId, slotId, buttonId, clickType, (PlayerEntity)InventoryTask.mc.player);
                if (silent) {
                    InventoryTask.mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, (PlayerEntity)InventoryTask.mc.player);
                }
            });
        } else {
            InventoryTask.mc.interactionManager.clickSlot(windowId, slotId, buttonId, clickType, (PlayerEntity)InventoryTask.mc.player);
            if (silent) {
                InventoryTask.mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, (PlayerEntity)InventoryTask.mc.player);
            }
        }
    }

    public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        InventoryTask.clickSlot(windowId, slotId, buttonId, clickType, silent, false);
    }

    public static Slot getSlot(Item item) {
        return InventoryTask.getSlot(item, (Slot s) -> true);
    }

    public static Slot getSlot(Item item, Predicate<Slot> filter) {
        return InventoryTask.getSlot(item, Comparator.comparingInt(s -> 0), filter);
    }

    public static Slot getSlot(Predicate<Slot> filter) {
        return InventoryTask.slots().filter(filter).findFirst().orElse(null);
    }

    public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
        return InventoryTask.slots().filter(filter).max(comparator).orElse(null);
    }

    public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
        return InventoryTask.slots().filter(s -> s.getStack().getItem().equals(item)).filter(filter).max(comparator).orElse(null);
    }

    public static Slot getFoodMaxSaturationSlot() {
        return InventoryTask.slots().filter(s -> s.getStack().get(DataComponentTypes.FOOD) != null && !((FoodComponent)s.getStack().get(DataComponentTypes.FOOD)).canAlwaysEat()).max(Comparator.comparingDouble(s -> ((FoodComponent)s.getStack().get(DataComponentTypes.FOOD)).saturation())).orElse(null);
    }

    public static Slot getSlot(List<Item> item) {
        return InventoryTask.slots().filter(s -> item.contains(s.getStack().getItem())).findFirst().orElse(null);
    }

    public static Slot getPotion(RegistryEntry<StatusEffect> effect) {
        return InventoryTask.slots().filter(s -> {
            PotionContentsComponent component = (PotionContentsComponent)s.getStack().get(DataComponentTypes.POTION_CONTENTS);
            if (component == null) {
                return false;
            }
            return StreamSupport.stream(component.getEffects().spliterator(), false).anyMatch(e -> e.getEffectType().equals((Object)effect));
        }).findFirst().orElse(null);
    }

    public static Slot getPotionFromCategory(StatusEffectCategory category) {
        return InventoryTask.slots().filter(s -> {
            ItemStack stack = s.getStack();
            PotionContentsComponent component = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
            if (!stack.getItem().equals(Items.SPLASH_POTION) || component == null) {
                return false;
            }
            StatusEffectCategory category2 = category.equals((Object)StatusEffectCategory.BENEFICIAL) ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
            long effects = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> ((StatusEffect)e.getEffectType().value()).getCategory().equals((Object)category)).count();
            long effects2 = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> ((StatusEffect)e.getEffectType().value()).getCategory().equals((Object)category2)).count();
            return effects >= effects2;
        }).findFirst().orElse(null);
    }

    public static int getInventoryCount(Item item) {
        return IntStream.range(0, 45).filter(i -> Objects.requireNonNull(InventoryTask.mc.player).getInventory().getStack(i).getItem().equals(item)).map(i -> InventoryTask.mc.player.getInventory().getStack(i).getCount()).sum();
    }

    public static int findFreeHotbarSlot() {
        for (int i = 0; i < 9; ++i) {
            if (!InventoryTask.mc.player.getInventory().getStack(i).isEmpty()) continue;
            return i;
        }
        return 8;
    }

    public static int findHotbarSlot(Item item) {
        for (int i = 0; i < 9; ++i) {
            if (!InventoryTask.mc.player.getInventory().getStack(i).isOf(item)) continue;
            return i;
        }
        return -1;
    }

    public static int findInventorySlot(Item item) {
        for (int i = 9; i < 36; ++i) {
            if (!InventoryTask.mc.player.getInventory().getStack(i).isOf(item)) continue;
            return i;
        }
        return -1;
    }

    public static void swapSlots(int fromSlot, int toSlot) {
        if (InventoryTask.mc.interactionManager == null || InventoryTask.mc.player.currentScreenHandler == null) {
            return;
        }
        InventoryTask.mc.interactionManager.clickSlot(InventoryTask.mc.player.currentScreenHandler.syncId, fromSlot, toSlot, SlotActionType.SWAP, (PlayerEntity)InventoryTask.mc.player);
    }

    public static void useItemFromSlotMOMENTALNO(int slot) {
        if (InventoryTask.mc.player == null || InventoryTask.mc.interactionManager == null) {
            return;
        }
        ItemStack stack = InventoryTask.mc.player.getInventory().getStack(slot);
        if (!stack.isEmpty() && !InventoryTask.mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            int prevSlot = InventoryTask.mc.player.getInventory().selectedSlot;
            InventoryTask.mc.player.getInventory().selectedSlot = slot;
            InventoryTask.mc.interactionManager.interactItem((PlayerEntity)InventoryTask.mc.player, Hand.MAIN_HAND);
            InventoryTask.mc.player.swingHand(Hand.MAIN_HAND);
            InventoryTask.mc.player.getInventory().selectedSlot = prevSlot;
        }
    }

    public static int getHotbarItems(List<Item> items) {
        return IntStream.range(0, 9).filter(i -> items.contains(InventoryTask.mc.player.getInventory().getStack(i).getItem())).findFirst().orElse(-1);
    }

    public static int getHotbarSlotId(IntPredicate filter) {
        return IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
    }

    public static int getCount(Predicate<Slot> filter) {
        return InventoryTask.slots().filter(filter).mapToInt(s -> s.getStack().getCount()).sum();
    }

    public static Slot mainHandSlot() {
        long count = InventoryTask.slots().count();
        int i = count == 46L ? 10 : 9;
        return InventoryTask.slots().toList().get(Math.toIntExact(count - (long)i + (long)InventoryTask.mc.player.getInventory().selectedSlot));
    }

    public static boolean isServerScreen() {
        return InventoryTask.slots().toList().size() != 46;
    }

    public static Stream<Slot> slots() {
        return InventoryTask.mc.player.currentScreenHandler.slots.stream();
    }

    public static void selectCompass() {
        Slot slot = InventoryTask.getSlot(Items.COMPASS);
        if (slot != null) {
            InventoryTask.mc.player.getInventory().selectedSlot = slot.id < 9 ? slot.id : 0;
            InventoryTask.swapHand(slot, Hand.MAIN_HAND, false, true);
        }
    }

    public static String getCleanName(Text text) {
        if (text == null) {
            return "";
        }
        String name = text.getString();
        if (name == null) {
            return "";
        }
        return name.replaceAll("\u00a7[0-9a-fk-or]", "").toLowerCase();
    }
}
