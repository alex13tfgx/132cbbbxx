/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 *  net.minecraft.class_7439
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.packet.network.Network;
import java.util.Arrays;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AutoTpAccept
extends Module {
    private final String[] teleportMessages = new String[]{"has requested teleport", "\u043f\u0440\u043e\u0441\u0438\u0442 \u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c\u0441\u044f", "\u0445\u043e\u0447\u0435\u0442 \u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c\u0441\u044f \u043a \u0432\u0430\u043c", "\u043f\u0440\u043e\u0441\u0438\u0442 \u043a \u0432\u0430\u043c \u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c\u0441\u044f"};
    private boolean canAccept;
    private final BooleanSetting friendSetting = new BooleanSetting("\u0422\u043e\u043b\u044c\u043a\u043e \u0434\u0440\u0443\u0437\u044c\u044f", "\u0411\u0443\u0434\u0435\u0442 \u043f\u0440\u0438\u043d\u0438\u043c\u0430\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u044b \u0442\u043e\u043b\u044c\u043a\u043e \u043e\u0442 \u0434\u0440\u0443\u0437\u0435\u0439").setValue(false);

    public AutoTpAccept() {
        super("AutoTpAccept", "Auto TP Accept", ModuleCategory.MISC);
        this.setup(this.friendSetting);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        Packet<?> packet2 = e.getPacket();
        if (packet2 instanceof GameMessageS2CPacket) {
            boolean validPlayer;
            GameMessageS2CPacket m = (GameMessageS2CPacket)packet2;
            String message = m.content().getString();
            boolean bl = validPlayer = !this.friendSetting.isValue() || FriendUtils.getFriends().stream().anyMatch(s -> message.contains(s.getName()));
            if (this.isTeleportMessage(message)) {
                this.canAccept = validPlayer;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!Network.isPvp() && this.canAccept) {
            AutoTpAccept.mc.player.networkHandler.sendChatCommand("tpaccept");
            this.canAccept = false;
        }
    }

    private boolean isTeleportMessage(String message) {
        return Arrays.stream(this.teleportMessages).map(String::toLowerCase).anyMatch(message::contains);
    }
}
