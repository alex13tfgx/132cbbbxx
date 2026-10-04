/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2338
 *  net.minecraft.class_2586
 *  net.minecraft.class_2591
 *  net.minecraft.class_2680
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.block.BlockEntityProgressEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockEntity.class})
public class BlockEntityMixin {
    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    public void initHook(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        if (blockEntity != null) {
            SafeEvents.dispatch(() -> new BlockEntityProgressEvent(blockEntity, BlockEntityProgressEvent.Type.ADD));
        }
    }

    @Inject(method={"markRemoved"}, at={@At(value="HEAD")})
    private void markRemovedHook(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        if (blockEntity != null) {
            SafeEvents.dispatch(() -> new BlockEntityProgressEvent(blockEntity, BlockEntityProgressEvent.Type.REMOVE));
        }
    }
}
