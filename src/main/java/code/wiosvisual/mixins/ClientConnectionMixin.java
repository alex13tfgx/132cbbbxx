/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelPipeline
 *  io.netty.handler.proxy.Socks5ProxyHandler
 *  net.minecraft.class_2535
 *  net.minecraft.class_2547
 *  net.minecraft.class_2596
 *  net.minecraft.class_2598
 *  net.minecraft.class_8762
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.common.proxy.Proxy;
import code.wiosvisual.common.proxy.ProxyServer;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.client.stealth.ModStealth;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.PacketSizeLogger;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientConnection.class})
public class ClientConnectionMixin {
    @Unique
    private static final ThreadLocal<Boolean> wiosvisual$sendingChestBypassPosition = ThreadLocal.withInitial(() -> false);

    @Inject(method={"handlePacket"}, at={@At(value="HEAD")}, cancellable=true)
    private static <T extends PacketListener> void handlePacketPre(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        PacketEvent packetEvent = SafeEvents.dispatch(() -> new PacketEvent(packet, PacketEvent.Type.RECEIVE));
        if (packetEvent != null && packetEvent.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method={"send(Lnet/minecraft/network/packet/Packet;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void sendPre(Packet<?> packet, CallbackInfo info) {
        if (ModStealth.shouldBlockOutgoing(packet)) {
            info.cancel();
            return;
        }
        PacketEvent packetEvent = SafeEvents.dispatch(() -> new PacketEvent(packet, PacketEvent.Type.SEND));
        if (packetEvent != null && packetEvent.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method={"addHandlers"}, at={@At(value="RETURN")})
    private static void addHandlersHook(ChannelPipeline pipeline, NetworkSide side, boolean local, PacketSizeLogger packetSizeLogger, CallbackInfo ci) {
        Proxy proxy = ProxyServer.proxy;
        if (proxy != null && ProxyServer.proxyEnabled && side == NetworkSide.CLIENTBOUND && !local) {
            pipeline.addFirst(new ChannelHandler[]{new Socks5ProxyHandler((SocketAddress)new InetSocketAddress(proxy.getIp(), proxy.getPort()), proxy.username, proxy.password)});
            ProxyServer.lastUsedProxy = proxy;
        }
    }
}
