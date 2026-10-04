/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 *  net.minecraft.class_310
 */
package code.wiosvisual.main.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public interface Api {
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket(packet);
    }
}
