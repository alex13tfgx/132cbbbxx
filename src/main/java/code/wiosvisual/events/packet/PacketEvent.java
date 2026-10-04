/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 */
package code.wiosvisual.events.packet;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.network.packet.Packet;

public class PacketEvent
extends EventCancellable {
    private Packet<?> packet;
    private Type type;

    public boolean isSend() {
        return this.type.equals((Object)Type.SEND);
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public Type getType() {
        return this.type;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public PacketEvent(Packet<?> packet, Type type) {
        this.packet = packet;
        this.type = type;
    }

    public static enum Type {
        SEND,
        RECEIVE;

    }
}
