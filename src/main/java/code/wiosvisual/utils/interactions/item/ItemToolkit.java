/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1657
 *  net.minecraft.class_2596
 *  net.minecraft.class_2678
 *  net.minecraft.class_2724
 *  net.minecraft.class_2799
 *  net.minecraft.class_2799$class_2800
 *  net.minecraft.class_2846
 *  net.minecraft.class_2846$class_2847
 */
package code.wiosvisual.utils.interactions.item;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Hand;

public class ItemToolkit
implements QuickImports {
    public static final ItemToolkit INSTANCE = new ItemToolkit();
    public boolean useItem;
    public boolean releaseItem = true;

    public ItemToolkit() {
        Wiosvisual.getInstance().getEventManager();
        EventManager.register(this);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        Packet<?> packet2 = e.getPacket();
        Objects.requireNonNull(packet2);
        Packet<?> packet3 = packet2;
        int n = 0;
        block6: while (true) {
            switch (code.wiosvisual.utils.TypeSwitch.typeSwitch(new Object[]{PlayerActionC2SPacket.class, ClientStatusC2SPacket.class, PlayerRespawnS2CPacket.class, GameJoinS2CPacket.class}, packet3, n)) {
                case 0: {
                    PlayerActionC2SPacket player = (PlayerActionC2SPacket)packet3;
                    if (!player.getAction().equals((Object)PlayerActionC2SPacket.Action.RELEASE_USE_ITEM)) {
                        n = 1;
                        continue block6;
                    }
                    this.releaseItem = true;
                    break block6;
                }
                case 1: {
                    ClientStatusC2SPacket status = (ClientStatusC2SPacket)packet3;
                    if (!status.getMode().equals((Object)ClientStatusC2SPacket.Mode.PERFORM_RESPAWN)) {
                        n = 2;
                        continue block6;
                    }
                    this.releaseItem = true;
                    break block6;
                }
                case 2: {
                    PlayerRespawnS2CPacket respawn = (PlayerRespawnS2CPacket)packet3;
                    this.releaseItem = true;
                    break block6;
                }
                case 3: {
                    GameJoinS2CPacket join = (GameJoinS2CPacket)packet3;
                    this.releaseItem = true;
                    break block6;
                }
            }
            break;
        }
    }

    public void useHand(Hand hand) {
        if (this.releaseItem) {
            ItemToolkit.mc.interactionManager.interactItem((PlayerEntity)ItemToolkit.mc.player, hand);
            this.releaseItem = false;
        }
        this.useItem = true;
    }

    public void setUseItem(boolean useItem) {
        this.useItem = useItem;
    }

    public void setReleaseItem(boolean releaseItem) {
        this.releaseItem = releaseItem;
    }

    public boolean isUseItem() {
        return this.useItem;
    }

    public boolean isReleaseItem() {
        return this.releaseItem;
    }
}
