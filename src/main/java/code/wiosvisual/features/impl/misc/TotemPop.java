package code.wiosvisual.features.impl.misc;

import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.render.WorldLoadEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Totem Pop: считает, сколько тотемов снёс каждый игрок, и сообщает об этом.
 * Статус сущности 35 — сработал тотем, 3 — смерть (счётчик сбрасывается).
 */
public class TotemPop
extends Module {
    private static final byte STATUS_TOTEM = 35;
    private static final byte STATUS_DEATH = 3;

    public final BooleanSetting notify = new BooleanSetting("Уведомление", "Показывать уведомление на экране").setValue(true);
    public final BooleanSetting chat = new BooleanSetting("В чат", "Писать сообщение в чат (видно только вам)").setValue(false);
    public final BooleanSetting self = new BooleanSetting("Свои тотемы", "Сообщать и о своих тотемах").setValue(false);
    public final BooleanSetting deaths = new BooleanSetting("Смерть", "Сообщать, сколько тотемов снёс игрок перед смертью").setValue(true);

    private final Map<UUID, Integer> pops = new HashMap<>();

    public TotemPop() {
        super("TotemPop", "Totem Pop", ModuleCategory.MISC);
        this.setup(this.notify, this.chat, this.self, this.deaths);
    }

    public static TotemPop getInstance() {
        return Instance.get(TotemPop.class);
    }

    /** Сколько тотемов снёс игрок с момента последней смерти. */
    public int getPops(PlayerEntity player) {
        return this.pops.getOrDefault(player.getUuid(), 0);
    }

    @Override
    public void deactivate() {
        this.pops.clear();
        super.deactivate();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        this.pops.clear();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.isSend() || !(e.getPacket() instanceof EntityStatusS2CPacket packet)) {
            return;
        }
        byte status = packet.getStatus();
        if (status != STATUS_TOTEM && status != STATUS_DEATH) {
            return;
        }
        // Пакет приходит из сетевого потока — мир и уведомления трогаем в потоке игры.
        mc.execute(() -> this.handleStatus(packet, status));
    }

    private void handleStatus(EntityStatusS2CPacket packet, byte status) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        boolean isSelf = player == mc.player;
        UUID id = player.getUuid();
        String name = player.getName().getString();
        if (status == STATUS_TOTEM) {
            int count = this.pops.merge(id, 1, Integer::sum);
            if (!isSelf || this.self.isValue()) {
                String who = isSelf ? "Вы снесли" : name + " снёс";
                this.report(who + " " + count + " " + TotemPop.totems(count), Formatting.GOLD);
            }
            return;
        }
        Integer count = this.pops.remove(id);
        if (count != null && count > 0 && this.deaths.isValue() && !isSelf) {
            this.report(name + " умер, снеся " + count + " " + TotemPop.totems(count), Formatting.RED);
        }
    }

    private void report(String text, Formatting color) {
        if (this.notify.isValue()) {
            Notifications.getInstance().addList((Text)Text.literal(text), 2500, null, false, new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING));
        }
        if (this.chat.isValue()) {
            ChatMessage.brandmessage((Text)Text.literal(text).formatted(color));
        }
    }

    /** 1 тотем, 2 тотема, 5 тотемов, 21 тотем. */
    private static String totems(int n) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return "тотемов";
        }
        if (mod10 == 1) {
            return "тотем";
        }
        return mod10 >= 2 && mod10 <= 4 ? "тотема" : "тотемов";
    }
}
