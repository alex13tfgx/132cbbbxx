package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.features.aura.warp.TurnsConfig;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.interactions.inv.HotbarUtil;
import code.wiosvisual.utils.interactions.inv.SlotSearchResult;
import code.wiosvisual.utils.math.TimerUtils;
import code.wiosvisual.utils.math.task.TaskPriority;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Прокачка клана: ставит факел под ноги и тут же ломает его.
 */
public class AutoClanUpgrade
extends Module {
    private final TimerUtils timer = new TimerUtils();

    public AutoClanUpgrade() {
        super("AutoClanUpgrade", "Auto Clan Upgrade", ModuleCategory.MISC);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (AutoClanUpgrade.mc.player == null || AutoClanUpgrade.mc.world == null || AutoClanUpgrade.mc.interactionManager == null) {
            return;
        }
        boolean hint = AutoClanUpgrade.mc.player.age % 200 == 0;
        if (AutoClanUpgrade.mc.world.getRegistryKey().getValue().getPath().equals("lobby")) {
            if (hint) {
                ChatMessage.brandmessage("Тепнитесь на РТП");
            }
            return;
        }
        SlotSearchResult torch = HotbarUtil.findItemInHotBar(Items.TORCH);
        if (!torch.found()) {
            if (hint) {
                ChatMessage.brandmessage("Нужен факел в хотбаре");
            }
            return;
        }
        BlockPos pos = AutoClanUpgrade.mc.player.getBlockPos().down();
        BlockPos torchPos = pos.up();
        if (!AutoClanUpgrade.mc.world.getBlockState(pos).isSolidBlock(AutoClanUpgrade.mc.world, pos)) {
            return;
        }
        if (!torch.isHolding()) {
            torch.switchTo();
        }
        TurnsConnection.INSTANCE.rotateTo(new Turns(25.0f, 87.5f), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
        if (!this.timer.finished(5L)) {
            return;
        }
        this.timer.reset();
        if (AutoClanUpgrade.mc.world.getBlockState(torchPos).isAir()) {
            AutoClanUpgrade.mc.interactionManager.interactBlock(AutoClanUpgrade.mc.player, Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
        }
        if (AutoClanUpgrade.mc.world.getBlockState(torchPos).getBlock() == Blocks.TORCH) {
            AutoClanUpgrade.mc.interactionManager.attackBlock(torchPos, Direction.WEST);
            AutoClanUpgrade.mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
