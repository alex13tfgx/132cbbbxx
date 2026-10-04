package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.interactions.inv.InventorySearch;
import java.util.Optional;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * Меняет нагрудник на элитры и обратно по кнопке. Надевание идёт через окно
 * инвентаря: берём предмет, кладём в слот нагрудника и возвращаем снятое на место.
 * Шаги разнесены по тикам — иначе сервер часто теряет клики, отправленные подряд.
 */
public class ElytraSwap
extends Module {
    /** Слот нагрудника в окне инвентаря игрока. */
    private static final int CHEST_SLOT = 6;

    private final BindSetting key = new BindSetting("Кнопка свапа", "Кнопка, по которой меняются элитры и нагрудник");

    private boolean swapping;
    private int step;
    private boolean clicked;
    private int delay;

    public ElytraSwap() {
        super("ElytraSwap", "Elytra Swap", ModuleCategory.MISC);
        this.setup(this.key);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.world == null || this.swapping || !e.isKeyDown(this.key.getKey())) {
            return;
        }
        this.swapping = true;
        this.step = 0;
        this.clicked = false;
        this.delay = 0;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || !this.swapping) {
            return;
        }
        if (this.delay > 0) {
            --this.delay;
            return;
        }
        switch (this.step) {
            case 0 -> {
                if (!mc.player.getAbilities().creativeMode) {
                    if (!this.wearingElytra()) {
                        if (this.findElytra().isEmpty()) {
                            this.notify("Свап отменён: нет элитр.");
                            this.swapping = false;
                            return;
                        }
                    } else if (this.findChestplate().isEmpty()) {
                        this.notify("Свап отменён: нет нагрудника.");
                        this.swapping = false;
                        return;
                    }
                    if (!(mc.currentScreen instanceof InventoryScreen)) {
                        mc.setScreen(new InventoryScreen(mc.player));
                    }
                    this.delay = 2;
                }
                this.step = 1;
            }
            case 1 -> {
                if (mc.currentScreen instanceof InventoryScreen && !this.clicked) {
                    this.swap();
                    this.clicked = true;
                    this.delay = 2;
                    this.step = 2;
                }
            }
            case 2 -> {
                if (mc.currentScreen instanceof InventoryScreen) {
                    mc.player.closeHandledScreen();
                }
                this.swapping = false;
                this.step = 0;
            }
            default -> this.swapping = false;
        }
    }

    private void swap() {
        if (this.wearingElytra()) {
            this.findChestplate().ifPresent(slot -> {
                this.moveToChest(slot);
                this.notify("Снял элитры.");
            });
            return;
        }
        this.findElytra().ifPresent(slot -> {
            this.moveToChest(slot);
            this.notify("Надел элитры.");
        });
    }

    /** Взять предмет, положить в слот нагрудника, вернуть снятое в исходный слот. */
    private void moveToChest(int slot) {
        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, CHEST_SLOT, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }

    private Optional<Integer> findElytra() {
        return InventorySearch.find(stack -> stack.getItem() == Items.ELYTRA, true, false);
    }

    private Optional<Integer> findChestplate() {
        return InventorySearch.find(ElytraSwap::isChestplate, true, false);
    }

    private static boolean isChestplate(ItemStack stack) {
        if (stack.getItem() == Items.ELYTRA) {
            return false;
        }
        EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private boolean wearingElytra() {
        return mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    private void notify(String message) {
        mc.player.sendMessage(Text.of("Elytra Swap: " + message), true);
    }

    @Override
    public void activate() {
        super.activate();
        this.swapping = false;
        this.step = 0;
        this.clicked = false;
        this.delay = 0;
    }
}
