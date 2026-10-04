package code.wiosvisual.features.impl.misc;

import code.wiosvisual.display.hud.DynamicIsland;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

/**
 * Помощник для гриф-серверов: автопочинка, спуф ресурспака, авто-команды
 * и биндовое использование серверных предметов-способностей.
 */
public class Assist
extends Module {
    private static final String LEFT = "Левая";
    private static final String RIGHT = "Правая";

    private final BooleanSetting spoof = new BooleanSetting("Спуф рп", "Принимать ресурспак не скачивая его").setValue(false);
    private final BooleanSetting closeMenu = new BooleanSetting("Закрывать меню", "Автоматически закрывает меню при заходе").setValue(false);
    private final BooleanSetting autoFix = new BooleanSetting("Автопочинка", "Отправляет /fix all, когда снаряжение изношено").setValue(false);
    private final BooleanSetting warnArmor = new BooleanSetting("Поломка брони", "Предупреждает, когда броня на грани поломки").setValue(true);
    private final BooleanSetting autoZako = new BooleanSetting("Авто /zako", "Автоматически вводит /zako").setValue(false);
    private final SelectSetting hand = new SelectSetting("Рука", "Какой рукой использовать предмет с бинда").value(LEFT, RIGHT).selected(LEFT);

    private final BindSetting dezorent = new BindSetting("Дезориентация", "Око Эндера");
    private final BindSetting trap = new BindSetting("Трапка", "Незеритовый лом");
    private final BindSetting smerch = new BindSetting("Огненный смерч", "Огненный шар");
    private final BindSetting plast = new BindSetting("Пласт", "Сушёные водоросли");
    private final BindSetting aura = new BindSetting("Божья аура", "Мембрана фантома");
    private final BindSetting pilb = new BindSetting("Явная пыль", "Сахар");
    private final BindSetting stan = new BindSetting("Стан", "Незеритовая звезда");
    private final BindSetting snow = new BindSetting("Ком снега", "Снежок");
    private final BindSetting bomb = new BindSetting("Взрывная штучка", "Огненный шар");
    private final BindSetting hwTrap = new BindSetting("Трапка (HW)", "Лопнувший плод хоруса");
    private final BindSetting boomTrap = new BindSetting("Взрывная трапка", "Осколок призмарина");
    private final BindSetting gool = new BindSetting("Прощальный гул", "Звезда фейерверка");
    private final BindSetting backpack = new BindSetting("Рюкзак", "Пурпурный шалкер");

    private long lastFixAt;
    private long lastZakoAt;
    private boolean zakoPending;
    private boolean armorWarned;
    private int restoreSlot = -1;

    public static Assist getInstance() {
        return Instance.get(Assist.class);
    }

    public Assist() {
        super("Assist", "Assist", ModuleCategory.MISC);
        this.setup(this.spoof, this.closeMenu, this.autoFix, this.warnArmor, this.autoZako, this.hand, this.dezorent, this.trap, this.smerch, this.plast, this.aura, this.pilb, this.stan, this.snow, this.bomb, this.hwTrap, this.boomTrap, this.gool, this.backpack);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (Assist.mc.player == null || Assist.mc.world == null || Assist.mc.interactionManager == null) {
            return;
        }
        this.restoreHeldSlot();
        if (this.warnArmor.isValue()) {
            this.checkArmor();
        }
        if (this.autoFix.isValue()) {
            this.tryAutoFix();
        }
        if (this.autoZako.isValue() && this.zakoPending && System.currentTimeMillis() - this.lastZakoAt > 500L) {
            this.zakoPending = false;
            this.sendCommand("zako");
        }
        if (this.closeMenu.isValue() && Assist.mc.currentScreen != null && Assist.mc.currentScreen.getTitle() != null && Assist.mc.currentScreen.getTitle().getString().contains("Меню")) {
            Assist.mc.player.closeHandledScreen();
        }
    }

    /** Ответ сервера ловим пакетом: по нему понятно, когда команду можно повторить. */
    @EventHandler
    public void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (!this.autoZako.isValue() || !(packet instanceof GameStateChangeS2CPacket)) {
            return;
        }
        this.zakoPending = true;
        this.lastZakoAt = System.currentTimeMillis();
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.action() != 1 || Assist.mc.player == null) {
            return;
        }
        int key = e.key();
        this.tryBind(key, this.dezorent, "ender_eye");
        this.tryBind(key, this.trap, "netherite_scrap");
        this.tryBind(key, this.smerch, "fire_charge");
        this.tryBind(key, this.plast, "dried_kelp");
        this.tryBind(key, this.aura, "phantom_membrane");
        this.tryBind(key, this.pilb, "sugar");
        this.tryBind(key, this.stan, "nether_star");
        this.tryBind(key, this.snow, "snowball");
        this.tryBind(key, this.bomb, "fire_charge");
        this.tryBind(key, this.hwTrap, "popped_chorus_fruit");
        this.tryBind(key, this.boomTrap, "prismarine_shard");
        this.tryBind(key, this.gool, "firework_star");
        this.tryBind(key, this.backpack, "magenta_shulker_box");
    }

    private void tryBind(int key, BindSetting bind, String itemId) {
        if (bind.getKey() == -1 || bind.getKey() != key) {
            return;
        }
        this.useItem(Assist.item(itemId));
    }

    /**
     * Предмет ищется в хотбаре, слот выбирается на один тик и возвращается обратно:
     * так способность срабатывает, не сбивая текущий слот.
     */
    private void useItem(Item item) {
        if (item == null || Assist.mc.player == null || Assist.mc.interactionManager == null) {
            return;
        }
        int current = Assist.mc.player.getInventory().selectedSlot;
        int found = -1;
        for (int slot = 0; slot < 9; ++slot) {
            if (Assist.mc.player.getInventory().getStack(slot).getItem() != item) continue;
            found = slot;
            break;
        }
        if (found == -1) {
            return;
        }
        if (found != current) {
            Assist.mc.player.getInventory().selectedSlot = found;
            this.restoreSlot = current;
        }
        Hand usedHand = this.hand.isSelected(RIGHT) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Assist.mc.interactionManager.interactItem((PlayerEntity)Assist.mc.player, usedHand);
    }

    private void restoreHeldSlot() {
        if (this.restoreSlot >= 0) {
            Assist.mc.player.getInventory().selectedSlot = this.restoreSlot;
            this.restoreSlot = -1;
        }
    }

    /** Одно предупреждение на просадку: пока броню не починят, оно не повторяется. */
    private void checkArmor() {
        float worst = 1.0f;
        for (net.minecraft.entity.EquipmentSlot wiosSlot : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET}) {
            ItemStack stack = Assist.mc.player.getEquippedStack(wiosSlot);
            if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) continue;
            float left = (float)(stack.getMaxDamage() - stack.getDamage()) / (float)stack.getMaxDamage();
            worst = Math.min(worst, left);
        }
        if (worst < 0.36f) {
            if (!this.armorWarned) {
                this.armorWarned = true;
                DynamicIsland.addNotification("Ваша броня на грани поломки", DynamicIsland.NotificationType.WARNING);
            }
        } else {
            this.armorWarned = false;
        }
    }

    private void tryAutoFix() {
        if (System.currentTimeMillis() - this.lastFixAt < 5000L) {
            return;
        }
        for (int slot = 0; slot < Assist.mc.player.getInventory().size(); ++slot) {
            ItemStack stack = Assist.mc.player.getInventory().getStack(slot);
            if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) continue;
            float left = (float)(stack.getMaxDamage() - stack.getDamage()) / (float)stack.getMaxDamage();
            if (!(left < 0.5f)) continue;
            this.lastFixAt = System.currentTimeMillis();
            this.sendCommand("fix all");
            return;
        }
    }

    private void sendCommand(String command) {
        ServerPlayNetworkHandler ignored;
        if (Assist.mc.player == null || Assist.mc.player.networkHandler == null) {
            return;
        }
        Assist.mc.player.networkHandler.sendChatCommand(command);
    }

    /** Предметы берём из реестра по строковому id: так не зависим от полей маппингов. */
    private static Item item(String id) {
        return (Item)Registries.ITEM.get(Identifier.of((String)("minecraft:" + id)));
    }
}
