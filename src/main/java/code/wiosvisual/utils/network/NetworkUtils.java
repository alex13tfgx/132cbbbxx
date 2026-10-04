/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2596
 *  net.minecraft.class_310
 */
package code.wiosvisual.utils.network;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public class NetworkUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Set<Packet<?>> silentPackets = new HashSet();

    public static void sendSilentPacket(Packet<?> packet) {
        if (NetworkUtils.mc.player == null || NetworkUtils.mc.player.networkHandler == null) {
            return;
        }
        silentPackets.add(packet);
        NetworkUtils.mc.player.networkHandler.sendPacket(packet);
    }

    public static void sendPacket(Packet<?> packet) {
        if (NetworkUtils.mc.player == null || NetworkUtils.mc.player.networkHandler == null) {
            return;
        }
        NetworkUtils.mc.player.networkHandler.sendPacket(packet);
    }

    public static boolean isSilent(Packet<?> packet) {
        return silentPackets.contains(packet);
    }

    public static void removeSilent(Packet<?> packet) {
        silentPackets.remove(packet);
    }

    public static void clearSilent() {
        silentPackets.clear();
    }
}
