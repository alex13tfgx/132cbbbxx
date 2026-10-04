/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 *  net.minecraft.class_2828$class_2829
 *  net.minecraft.class_5892
 */
package code.wiosvisual.features.impl.player;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.DeathScreenEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.packet.network.Network;
import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;

public class AutoRespawn
extends Module {
    private final SelectSetting modeSetting = new SelectSetting("\u0420\u0435\u0436\u0438\u043c", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435, \u0447\u0442\u043e \u0431\u0443\u0434\u0435\u0442 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c\u0441\u044f").value("\u0424\u0430\u043d\u0422\u0430\u0439\u043c \u0432\u043e\u0437\u0432\u0440\u0430\u0442", "\u041e\u0431\u044b\u0447\u043d\u044b\u0439");

    public AutoRespawn() {
        super("AutoRespawn", "Auto Respawn", ModuleCategory.PLAYER);
        this.setup(this.modeSetting);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        Packet<?> packet2 = e.getPacket();
        Objects.requireNonNull(packet2);
        Packet<?> packet3 = packet2;
        int n = 0;
        block3: while (true) {
            switch (code.wiosvisual.utils.TypeSwitch.typeSwitch(new Object[]{DeathMessageS2CPacket.class}, packet3, n)) {
                case 0: {
                    DeathMessageS2CPacket message = (DeathMessageS2CPacket)packet3;
                    if (!Network.getWorldType().equals("lobby") || !this.modeSetting.isSelected("\u0424\u0430\u043d\u0422\u0430\u0439\u043c \u0432\u043e\u0437\u0432\u0440\u0430\u0442")) {
                        n = 1;
                        continue block3;
                    }
                    AutoRespawn.mc.player.networkHandler.sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(1448.0, 1337.0, 228.0, false, false));
                    AutoRespawn.mc.player.requestRespawn();
                    AutoRespawn.mc.player.closeScreen();
                    break block3;
                }
            }
            break;
        }
    }

    @EventHandler
    public void onDeathScreen(DeathScreenEvent e) {
        if (this.modeSetting.isSelected("\u041e\u0431\u044b\u0447\u043d\u044b\u0439")) {
            AutoRespawn.mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
