/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2586
 */
package code.wiosvisual.events.block;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.block.entity.BlockEntity;

public record BlockEntityProgressEvent(BlockEntity blockEntity, Type type) implements Event
{

    public static enum Type {
        ADD,
        REMOVE;

    }
}
