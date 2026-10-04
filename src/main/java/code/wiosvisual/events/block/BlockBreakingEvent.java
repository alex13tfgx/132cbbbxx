/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 */
package code.wiosvisual.events.block;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event
{
}
