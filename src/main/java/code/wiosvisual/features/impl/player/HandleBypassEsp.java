/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1923
 *  net.minecraft.class_2596
 *  net.minecraft.class_2672
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package code.wiosvisual.features.impl.player;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleBypassEsp
extends Module {
    private static final Logger log = LoggerFactory.getLogger(HandleBypassEsp.class);
    private final Set<ChunkPos> haha = new HashSet<ChunkPos>();

    public HandleBypassEsp() {
        super("HandleBypassEsp", "Handle Bypass ESP", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (HandleBypassEsp.mc.world == null || HandleBypassEsp.mc.player == null || event.isSend()) {
            return;
        }
        Packet<?> packet2 = event.getPacket();
        if (!(packet2 instanceof ChunkDataS2CPacket)) {
            return;
        }
        ChunkDataS2CPacket packet = (ChunkDataS2CPacket)packet2;
        ChunkPos chunkPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
        if (!this.haha.add(chunkPos)) {
            return;
        }
        mc.execute(() -> {
            for (Entity entity : HandleBypassEsp.mc.world.getEntities()) {
                PlayerEntity player;
                if (!(entity instanceof PlayerEntity) || (player = (PlayerEntity)entity) == HandleBypassEsp.mc.player || !new ChunkPos(player.getBlockPos()).equals((Object)chunkPos)) continue;
                String string = player.getName().getString();
            }
        });
    }

    @Override
    public void deactivate() {
        this.haha.clear();
        super.deactivate();
    }
}
