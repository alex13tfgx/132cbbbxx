/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 *  net.minecraft.class_2767
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

public class SoundsRemove
extends Module {
    public static SoundsRemove getInstance() {
        return Instance.get(SoundsRemove.class);
    }

    public SoundsRemove() {
        super("SoundsRemove", "Sounds Remove", ModuleCategory.MISC);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!this.isState() || e.isSend()) {
            return;
        }
        Packet<?> packet2 = e.getPacket();
        if (!(packet2 instanceof PlaySoundS2CPacket)) {
            return;
        }
        PlaySoundS2CPacket packet = (PlaySoundS2CPacket)packet2;
        String soundId = packet.getSound().getIdAsString();
        if (soundId != null && soundId.contains("entity.wither")) {
            e.setCancelled(true);
        }
    }
}
