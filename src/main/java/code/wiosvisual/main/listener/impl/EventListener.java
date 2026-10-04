/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 *  net.minecraft.class_2848
 *  net.minecraft.class_2848$class_2849
 *  net.minecraft.class_2868
 */
package code.wiosvisual.main.listener.impl;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.events.item.UsingItemEvent;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.main.listener.Listener;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.packet.network.Network;
import code.wiosvisual.utils.interactions.inv.InventoryFlowManager;
import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class EventListener
implements Listener {
    public static boolean serverSprint;
    public static int selectedSlot;

    @EventHandler
    public void onTick(TickEvent e) {
        Network.tick();
        InventoryFlowManager.tick();
        Wiosvisual.getInstance().getDraggableRepository().draggable().forEach(AbstractDraggable::tick);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        Packet<?> packet2 = e.getPacket();
        Objects.requireNonNull(packet2);
        Packet<?> packet3 = packet2;
        int n = 0;
        switch (code.wiosvisual.utils.TypeSwitch.typeSwitch(new Object[]{ClientCommandC2SPacket.class, UpdateSelectedSlotC2SPacket.class}, packet3, n)) {
            case 0: {
                ClientCommandC2SPacket command = (ClientCommandC2SPacket)packet3;
                serverSprint = switch (command.getMode()) {
                    case ClientCommandC2SPacket.Mode.START_SPRINTING -> true;
                    case ClientCommandC2SPacket.Mode.STOP_SPRINTING -> false;
                    default -> serverSprint;
                };
                break;
            }
            case 1: {
                UpdateSelectedSlotC2SPacket slot = (UpdateSelectedSlotC2SPacket)packet3;
                selectedSlot = slot.getSelectedSlot();
                break;
            }
        }
        Network.packet(e);
        Wiosvisual.getInstance().getDraggableRepository().draggable().forEach(drag -> drag.packet(e));
    }

}
