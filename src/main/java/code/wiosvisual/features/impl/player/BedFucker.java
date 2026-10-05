/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1922
 *  net.minecraft.class_2244
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_265
 *  net.minecraft.class_2680
 */
package code.wiosvisual.features.impl.player;

import code.wiosvisual.events.player.RotationUpdateEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.TurnsConfig;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.math.task.TaskPriority;
import java.util.Comparator;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class BedFucker
extends Module {
    private BlockPos targetPos;
    private VoxelShape targetShape;
    private final BooleanSetting rotateSetting = new BooleanSetting("\u041f\u043e\u0432\u043e\u0440\u043e\u0442", "\u041f\u043e\u0432\u043e\u0440\u0430\u0447\u0438\u0432\u0430\u0442\u044c\u0441\u044f \u043a \u043a\u0440\u043e\u0432\u0430\u0442\u0438").setValue(true);
    private final SliderSettings radiusSetting = new SliderSettings("\u0420\u0430\u0434\u0438\u0443\u0441", "\u041b\u043e\u043c\u0430\u0435\u0442 \u043a\u0440\u043e\u0432\u0430\u0442\u0438 \u0432 \u0440\u0430\u0434\u0438\u0443\u0441\u0435").setValue(4.0f).range(1, 6);

    public BedFucker() {
        super("BedFucker", "Bed Breaker", ModuleCategory.PLAYER);
        this.setup(this.rotateSetting, this.radiusSetting);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (this.targetPos != null && this.targetShape != null && !this.targetShape.isEmpty()) {
            Render3D.drawShape(this.targetPos, this.targetShape, ColorAssist.getClientColor(), 2.0f);
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() != 0 || BedFucker.mc.player == null || BedFucker.mc.world == null || BedFucker.mc.interactionManager == null) {
            return;
        }
        this.targetPos = BedFucker.mc.player.getBlockPos();
        int radius = this.radiusSetting.getInt();
        this.targetPos = PlayerInteractionHelper.getCube(BedFucker.mc.player.getBlockPos(), radius, radius, true).stream().filter(this::isBed).min(Comparator.comparingDouble(p -> p.toCenterPos().squaredDistanceTo(new net.minecraft.util.math.Vec3d(BedFucker.mc.player.getX(), BedFucker.mc.player.getY(), BedFucker.mc.player.getZ())))).orElse(null);
        if (this.targetPos != null) {
            if (this.rotateSetting.isValue()) {
                TurnsConnection.INSTANCE.rotateTo(MathAngle.calculateAngle(this.targetPos.toCenterPos()), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
            BlockState state = BedFucker.mc.world.getBlockState(this.targetPos);
            this.targetShape = state.getOutlineShape((BlockView)BedFucker.mc.world, this.targetPos);
            BedFucker.mc.interactionManager.updateBlockBreakingProgress(this.targetPos, Direction.UP);
            BedFucker.mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            this.targetShape = null;
        }
    }

    private boolean isBed(BlockPos pos) {
        BlockState state = BedFucker.mc.world.getBlockState(pos);
        return state.getBlock() instanceof BedBlock;
    }
}
