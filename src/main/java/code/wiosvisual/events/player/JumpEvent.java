/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 */
package code.wiosvisual.events.player;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.entity.player.PlayerEntity;

public class JumpEvent
extends EventCancellable {
    private PlayerEntity player;

    public PlayerEntity getPlayer() {
        return this.player;
    }

    public JumpEvent(PlayerEntity player) {
        this.player = player;
    }
}
