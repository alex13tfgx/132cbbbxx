/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_2596
 *  net.minecraft.class_2604
 */
package code.wiosvisual.features.impl.misc.creeperFarm;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.jetbrains.annotations.Nullable;

public class CreeperStorage
implements QuickImports {
    Deque<Entity> entityDeque = new ArrayDeque<Entity>();

    public void onTick() {
        this.entityDeque.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
    }

    public void onPacket(PacketEvent packetEvent) {
        EntitySpawnS2CPacket packet;
        Entity entity;
        Packet<?> packet2 = packetEvent.getPacket();
        if (packet2 instanceof EntitySpawnS2CPacket && (entity = CreeperStorage.mc.world.getEntityById((packet = (EntitySpawnS2CPacket)packet2).getEntityId())) != null && entity.isAlive() && !entity.isRemoved() && !this.entityDeque.contains(entity)) {
            this.entityDeque.add(entity);
        }
    }

    @Nullable
    public Entity getTarget() {
        return !this.entityDeque.isEmpty() ? this.getClosestEntity() : null;
    }

    @Nullable
    private Entity getClosestEntity() {
        return this.entityDeque.stream().min(Comparator.comparingDouble(entity -> CreeperStorage.mc.player.distanceTo(entity))).orElse(null);
    }
}
