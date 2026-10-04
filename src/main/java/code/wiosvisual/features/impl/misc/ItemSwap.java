package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Меняет предмет в левой руке между сферой и тотемом по кнопке.
 * Если нужный предмет лежит в хотбаре — выбираем его слот, отправляем «поменять руки»
 * и возвращаем слот обратно. Если только в инвентаре — меняем кликом SWAP в слот
 * левой руки. На время свапа гасим движение: на части серверов свап на ходу
 * засчитывается как подозрительный.
 */
public class ItemSwap
extends Module {
    private static final String SPHERE = "Сфера";
    private static final String TOTEM = "Тотем";
    /** Номер кнопки у клика SWAP, означающий слот левой руки. */
    private static final int OFFHAND_BUTTON = 40;
    private static final long FREEZE_MS = 260L;

    private final SelectSetting from = new SelectSetting("Менять с", "Какой предмет считается текущим").value(SPHERE, TOTEM).selected(SPHERE);
    private final SelectSetting to = new SelectSetting("Менять на", "На какой предмет менять").value(SPHERE, TOTEM).selected(TOTEM);
    private final BindSetting key = new BindSetting("Кнопка свапа", "Кнопка, по которой меняется предмет в левой руке");

    private boolean pendingInventorySwap;
    private boolean hotbarSwap;
    private int hotbarStep;
    private int stepDelay;
    private String wanted = "";
    private int previousSlot = -1;
    private int targetSlot = -1;
    private long freezeUntil;

    public ItemSwap() {
        super("ItemSwap", "Item Swap", ModuleCategory.MISC);
        this.setup(this.from, this.to, this.key);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.world == null || !e.isKeyDown(this.key.getKey())) {
            return;
        }
        this.freezeUntil = System.currentTimeMillis() + FREEZE_MS;
        this.wanted = this.wantedItem();
        Optional<Integer> inHotbar = ItemSwap.findIn(stack -> ItemSwap.matches(stack, this.wanted), 0, 9);
        if (inHotbar.isPresent()) {
            this.startHotbarSwap(inHotbar.get());
            return;
        }
        this.pendingInventorySwap = true;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) {
            return;
        }
        if (System.currentTimeMillis() < this.freezeUntil) {
            this.freezeMovement();
        }
        if (this.hotbarSwap) {
            this.tickHotbarSwap();
        }
        if (this.pendingInventorySwap) {
            this.inventorySwap();
        }
    }

    private void freezeMovement() {
        mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
        if (mc.options == null) {
            return;
        }
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    private void startHotbarSwap(int slot) {
        this.previousSlot = mc.player.getInventory().selectedSlot;
        this.targetSlot = slot;
        this.hotbarSwap = true;
        this.hotbarStep = 0;
        this.stepDelay = 0;
    }

    private void tickHotbarSwap() {
        if (this.stepDelay > 0) {
            --this.stepDelay;
        }
        switch (this.hotbarStep) {
            case 0 -> {
                mc.player.getInventory().selectedSlot = this.targetSlot;
                this.stepDelay = 2;
                this.hotbarStep = 1;
            }
            case 1 -> {
                mc.player.networkHandler.sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                this.stepDelay = 2;
                this.hotbarStep = 2;
            }
            default -> {
                mc.player.getInventory().selectedSlot = this.previousSlot;
                this.notifySwapped();
                this.hotbarSwap = false;
                this.hotbarStep = 0;
            }
        }
    }

    private void inventorySwap() {
        this.pendingInventorySwap = false;
        Optional<Integer> slot = ItemSwap.findIn(stack -> ItemSwap.matches(stack, this.wanted), 9, 36);
        if (slot.isEmpty()) {
            mc.player.sendMessage(Text.literal("§cНе найден предмет для свапа!"), true);
            return;
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.get(), OFFHAND_BUTTON, SlotActionType.SWAP, mc.player);
        this.notifySwapped();
    }

    private void notifySwapped() {
        mc.player.sendMessage(Text.literal("§fСвапнул на ").append(mc.player.getOffHandStack().getName()), true);
    }

    /** Если в левой руке уже «с чего менять» — меняем на второй предмет, иначе на первый. */
    private String wantedItem() {
        String first = this.from.getSelected();
        return ItemSwap.matches(mc.player.getOffHandStack(), first) ? this.to.getSelected() : first;
    }

    private static boolean matches(ItemStack stack, String item) {
        if (stack.isEmpty()) {
            return false;
        }
        if (SPHERE.equals(item)) {
            return stack.getItem() == Items.PLAYER_HEAD;
        }
        if (TOTEM.equals(item)) {
            return stack.getItem() == Items.TOTEM_OF_UNDYING;
        }
        return false;
    }

    private static Optional<Integer> findIn(Predicate<ItemStack> predicate, int from, int to) {
        for (int i = from; i < to; ++i) {
            if (predicate.test(mc.player.getInventory().getStack(i))) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    @Override
    public void activate() {
        super.activate();
        this.pendingInventorySwap = false;
        this.hotbarSwap = false;
        this.hotbarStep = 0;
        this.stepDelay = 0;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (mc.player != null && mc.currentScreen instanceof InventoryScreen) {
            mc.player.closeHandledScreen();
        }
        this.hotbarSwap = false;
        this.pendingInventorySwap = false;
    }
}
