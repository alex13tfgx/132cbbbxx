/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1713
 *  net.minecraft.class_1735
 *  net.minecraft.class_1792
 *  net.minecraft.class_465
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.container.HandledScreenEvent;
import code.wiosvisual.events.item.ClickSlotEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.mixins.HandledScreenInvoker;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.interactions.inv.InventoryTask;
import code.wiosvisual.utils.math.time.StopWatch;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ItemScroller
extends Module {
    private final StopWatch stopWatch = new StopWatch();
    private final SliderSettings scrollerSetting = new SliderSettings("Задержка", "Выберите задержку прокрутки предметов").setValue(0.0f).range(0, 200);

    public ItemScroller() {
        super("ItemScroller", "Item Scroller", ModuleCategory.MISC);
        this.setup(this.scrollerSetting);
    }

    /**
     * Клик выполняем через сам экран, а не через ClientPlayerInteractionManager#clickSlot:
     * креативный инвентарь обрабатывает слоты собственным onMouseClick, поэтому прямой
     * clickSlot там просто ничего не переносил (и был ненадёжен в локальном мире).
     */
    private boolean click(Slot slot, int button, SlotActionType actionType) {
        HandledScreen screen;
        if (slot == null || !(ItemScroller.mc.currentScreen instanceof HandledScreen) || (screen = (HandledScreen)ItemScroller.mc.currentScreen) == null) {
            return false;
        }
        ((HandledScreenInvoker)((Object)screen)).wiosvisual$onMouseClick(slot, slot.id, button, actionType);
        return true;
    }

    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        SlotActionType actionType;
        Slot hoverSlot = e.getSlotHover();
        actionType = PlayerInteractionHelper.isKey(ItemScroller.mc.options.dropKey) ? SlotActionType.THROW : (PlayerInteractionHelper.isKey(ItemScroller.mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null);
        if (!PlayerInteractionHelper.isKey(ItemScroller.mc.options.sneakKey) || PlayerInteractionHelper.isKey(ItemScroller.mc.options.sprintKey) || hoverSlot == null || !hoverSlot.hasStack() || actionType == null) {
            return;
        }
        // Задержка 0 — переносим каждый кадр, без искусственной паузы.
        float delay = this.scrollerSetting.getValue();
        if (delay > 0.0f && !this.stopWatch.every(delay)) {
            return;
        }
        this.click(hoverSlot, actionType == SlotActionType.THROW ? 1 : 0, actionType);
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        int slotId = e.getSlotId();
        if (ItemScroller.mc.player == null || slotId < 0 || slotId >= ItemScroller.mc.player.currentScreenHandler.slots.size()) {
            return;
        }
        Slot slot = ItemScroller.mc.player.currentScreenHandler.getSlot(slotId);
        if (slot.getStack().isEmpty()) {
            return;
        }
        Item item = slot.getStack().getItem();
        if (!PlayerInteractionHelper.isKey(ItemScroller.mc.options.sneakKey) || !PlayerInteractionHelper.isKey(ItemScroller.mc.options.sprintKey) || !this.stopWatch.every(50.0)) {
            return;
        }
        // Список фиксируем заранее: onMouseClick меняет содержимое слотов прямо во время обхода.
        List<Slot> targets = InventoryTask.slots().filter(s -> !s.getStack().isEmpty() && s.getStack().getItem().equals(item) && s.inventory.equals((Object)slot.inventory)).toList();
        for (Slot target : targets) {
            this.click(target, 1, e.getActionType());
        }
    }
}
