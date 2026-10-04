/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1299
 *  net.minecraft.class_1922
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_238
 *  net.minecraft.class_265
 *  net.minecraft.class_2680
 *  net.minecraft.class_3545
 */
package code.wiosvisual.common.repository.box;

import code.wiosvisual.events.block.BlockUpdateEvent;
import code.wiosvisual.events.render.WorldLoadEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.math.calc.Calculate;
import code.wiosvisual.utils.math.projection.Projection;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class BoxESPRepository
implements QuickImports,
QuickLogger {
    private final Map<BlockPos, Pair<VoxelShape, Integer>> boxes = new HashMap<BlockPos, Pair<VoxelShape, Integer>>();
    public final Map<EntityType<?>, Integer> entities = new HashMap();
    public final Map<Block, Integer> blocks = new HashMap<Block, Integer>();
    public boolean drawFill = true;

    public BoxESPRepository(EventManager eventManager) {
        EventManager.register(this);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        this.boxes.forEach((pos, pair) -> {
            if (this.drawFill) {
                Render3D.drawShape(pos, (VoxelShape)pair.getLeft(), (Integer)pair.getRight(), 1.0f);
            } else {
                Render3D.drawShapeAlternative(pos, (VoxelShape)pair.getLeft(), (Integer)pair.getRight(), 1.0f, false, false);
            }
        });
        PlayerInteractionHelper.streamEntities().filter(ent -> this.entities.containsKey(ent.getType()) && ent != BoxESPRepository.mc.player).forEach(ent -> {
            int entityColor = this.entities.get(ent.getType());
            int color = entityColor == 0 ? ColorAssist.getClientColor() : entityColor;
            Box box = ent.getBoundingBox().offset(Calculate.interpolate(ent).subtract(new net.minecraft.util.math.Vec3d(ent.getX(), ent.getY(), ent.getRandomBodyY())));
            if (Projection.canSee(box)) {
                Render3D.drawBox(box, color, 1.0f);
            }
        });
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        this.boxes.clear();
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent e) {
        BlockPos pos = e.pos();
        BlockState state = e.state();
        Block block = state.getBlock();
        switch (e.type()) {
            case LOAD: {
                if (!this.blocks.containsKey(block)) break;
                this.putBox(pos, state, block);
                break;
            }
            case UPDATE: {
                if (this.blocks.containsKey(block) && !this.boxes.containsKey(pos)) {
                    this.putBox(pos, state, block);
                }
                if (!this.boxes.containsKey(pos) || !state.isAir() && ((VoxelShape)this.boxes.get(pos).getLeft()).equals(state.getOutlineShape((BlockView)BoxESPRepository.mc.world, pos))) break;
                this.boxes.remove(pos);
                break;
            }
            case UNLOAD: {
                this.boxes.remove(pos);
            }
        }
    }

    private void putBox(BlockPos pos, BlockState state, Block block) {
        VoxelShape shape = state.getOutlineShape((BlockView)BoxESPRepository.mc.world, pos);
        int blockColor = this.blocks.get(block);
        int color = blockColor == 0 ? ColorAssist.replAlpha(state.getMapColor((BlockView)BoxESPRepository.mc.world, (BlockPos)pos).color, 1.0f) : blockColor;
        this.boxes.put(pos, (Pair<VoxelShape, Integer>)new Pair((Object)shape, (Object)color));
    }
}
