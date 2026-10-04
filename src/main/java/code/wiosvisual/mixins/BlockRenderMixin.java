/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1920
 *  net.minecraft.class_2248
 *  net.minecraft.class_2261
 *  net.minecraft.class_2302
 *  net.minecraft.class_2320
 *  net.minecraft.class_2338
 *  net.minecraft.class_2476
 *  net.minecraft.class_2680
 *  net.minecraft.class_3481
 *  net.minecraft.class_4587
 *  net.minecraft.class_4588
 *  net.minecraft.class_5819
 *  net.minecraft.class_776
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.NoRender;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SeagrassBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockRenderManager.class})
public class BlockRenderMixin {
    @Inject(method={"renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void hideVegetation(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideVegetation() && BlockRenderMixin.isVegetationBlock(state)) {
            ci.cancel();
        }
    }

    private static boolean isVegetationBlock(BlockState blockState) {
        if (blockState == null) {
            return false;
        }
        Block block = blockState.getBlock();
        if (block instanceof PlantBlock || block instanceof CropBlock || block instanceof SeagrassBlock || block instanceof TallPlantBlock) {
            return true;
        }
        return blockState.isIn(BlockTags.LEAVES) || blockState.isIn(BlockTags.FLOWERS) || blockState.isIn(BlockTags.SAPLINGS) || blockState.isIn(BlockTags.CROPS);
    }
}
