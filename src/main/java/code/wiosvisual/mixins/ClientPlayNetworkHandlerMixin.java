/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2666
 *  net.minecraft.class_2672
 *  net.minecraft.class_2680
 *  net.minecraft.class_2818
 *  net.minecraft.class_2826
 *  net.minecraft.class_634
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.block.BlockUpdateEvent;
import code.wiosvisual.events.chat.ChatEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.path.BaritoneHelper;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPlayNetworkHandler.class})
public class ClientPlayNetworkHandlerMixin
implements QuickImports {
    @Inject(method={"onChunkData"}, at={@At(value="RETURN")})
    private void onChunkDataHook(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (ClientPlayNetworkHandlerMixin.mc.world == null) {
            return;
        }
        this.scanChunk(ClientPlayNetworkHandlerMixin.mc.world.getChunk(packet.getChunkX(), packet.getChunkZ()), BlockUpdateEvent.Type.LOAD);
    }

    @Inject(method={"onUnloadChunk"}, at={@At(value="HEAD")})
    private void onUnloadChunkHook(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        if (ClientPlayNetworkHandlerMixin.mc.world == null) {
            return;
        }
        this.scanChunk(ClientPlayNetworkHandlerMixin.mc.world.getChunk(packet.pos().x, packet.pos().z), BlockUpdateEvent.Type.UNLOAD);
    }

    @Inject(method={"sendChatMessage(Ljava/lang/String;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void sendChatMessage(String string, CallbackInfo ci) {
        if (BaritoneHelper.handleChatCommand(string)) {
            ci.cancel();
            return;
        }
        ChatEvent event = SafeEvents.dispatch(() -> new ChatEvent(string));
        if (event != null && event.isCancelled()) {
            ci.cancel();
        }
    }

    @Unique
    private void scanChunk(WorldChunk worldChunk, BlockUpdateEvent.Type type) {
        if (worldChunk == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            int startX = worldChunk.getPos().getStartX();
            int startZ = worldChunk.getPos().getStartZ();
            ArrayList<CompletableFuture<Void>> sectionFutures = new ArrayList<CompletableFuture<Void>>();
            int sectionIndex = 0;
            while (sectionIndex <= worldChunk.getHighestNonEmptySection()) {
                int finalSectionIndex = sectionIndex++;
                sectionFutures.add(CompletableFuture.runAsync(() -> {
                    ChunkSection section = worldChunk.getSection(finalSectionIndex);
                    for (int sectionY = 0; sectionY < 16; ++sectionY) {
                        int y = finalSectionIndex + (worldChunk.getBottomY() >> 4) << 4 | sectionY;
                        for (int x = 0; x < 16; ++x) {
                            for (int z = 0; z < 16; ++z) {
                                BlockState blockState = type.equals((Object)BlockUpdateEvent.Type.LOAD) ? section.getBlockState(x, sectionY, z) : Blocks.AIR.getDefaultState();
                                BlockPos pos = new BlockPos(startX | x, y, startZ | z);
                                SafeEvents.dispatch(() -> new BlockUpdateEvent(blockState, pos, type));
                            }
                        }
                    }
                }));
            }
            CompletableFuture.allOf(sectionFutures.toArray(new CompletableFuture[0])).join();
        });
    }
}
