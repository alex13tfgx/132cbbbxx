/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2558
 *  net.minecraft.class_2558$class_2559
 *  net.minecraft.class_2561
 *  net.minecraft.class_5892
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;

public class DeathCoords
extends Module {
    public DeathCoords() {
        super("DeathCoords", "Death Coords", ModuleCategory.MISC);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof DeathMessageS2CPacket) {
            String text = String.format("%.0f %.0f %.0f", DeathCoords.mc.player.getX(), DeathCoords.mc.player.getY(), DeathCoords.mc.player.getZ());
            ChatMessage.send((Text)Text.literal((String)text).styled(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(text))));
        }
    }
}
