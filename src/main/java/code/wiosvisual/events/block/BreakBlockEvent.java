/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2338
 */
package code.wiosvisual.events.block;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.util.math.BlockPos;

public record BreakBlockEvent(BlockPos blockPos) implements Event
{
}
