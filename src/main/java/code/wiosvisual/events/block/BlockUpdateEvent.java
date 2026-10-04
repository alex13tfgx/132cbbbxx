/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2338
 *  net.minecraft.class_2680
 */
package code.wiosvisual.events.block;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public record BlockUpdateEvent(BlockState state, BlockPos pos, Type type) implements Event
{

    public static enum Type {
        LOAD,
        UNLOAD,
        UPDATE;

    }
}
