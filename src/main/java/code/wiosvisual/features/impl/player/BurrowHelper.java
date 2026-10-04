package code.wiosvisual.features.impl.player;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/**
 * Подсветка области, которую снесёт кирка с серверным зачарованием «Бур»:
 * блоки вокруг того, на который смотрит игрок, в плоскости задетой грани.
 */
public class BurrowHelper
extends Module {
    /** Ключевое слово серверного зачарования, при котором рамка вообще имеет смысл. */
    private static final String ENCHANT = "бур";

    private final SliderSettings area = new SliderSettings("Область", "Сторона области в блоках").range(1.0f, 7.0f).step(2.0f).setValue(3.0f);
    private final BooleanSetting perBlock = new BooleanSetting("По блокам", "Обводить каждый блок отдельно, а не всю область целиком").setValue(true);
    private final BooleanSetting skipAir = new BooleanSetting("Без воздуха", "Пропускать пустые клетки").setValue(true);
    private final BooleanSetting fill = new BooleanSetting("Заливка", "Заливать грани полупрозрачным цветом").setValue(false);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Рисовать поверх блоков").setValue(true);
    private final SliderSettings width = new SliderSettings("Толщина", "Толщина обводки").range(1.0f, 4.0f).step(0.25f).setValue(1.5f);

    public static BurrowHelper getInstance() {
        return Instance.get(BurrowHelper.class);
    }

    public BurrowHelper() {
        super("BurrowHelper", "Burrow Helper", ModuleCategory.PLAYER);
        this.setup(this.area, this.perBlock, this.skipAir, this.fill, this.throughWalls, this.width);
    }

    @EventHandler(value=4)
    public void onWorldRender(WorldRenderEvent e) {
        HitResult crosshair;
        ClientPlayerEntity player = BurrowHelper.mc.player;
        if (player == null || BurrowHelper.mc.world == null || !this.hasBurrowEnchant(player)) {
            return;
        }
        if (!((crosshair = BurrowHelper.mc.crosshairTarget) instanceof BlockHitResult) || crosshair.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult hit = (BlockHitResult)crosshair;
        BlockPos center = hit.getBlockPos();
        // Бур сносит плоскость, перпендикулярную грани, в которую бьём.
        Direction.Axis axis = hit.getSide().getAxis();
        int reach = ((int)this.area.getValue() - 1) / 2;
        int color = ColorAssist.getHudColor();
        boolean depth = !this.throughWalls.isValue();

        if (!this.perBlock.isValue()) {
            Render3D.drawBox(this.areaBox(center, axis, reach), color, this.width.getValue(), true, this.fill.isValue(), depth);
            return;
        }
        for (int a = -reach; a <= reach; ++a) {
            for (int b = -reach; b <= reach; ++b) {
                BlockPos pos = BurrowHelper.shift(center, axis, a, b);
                if (this.skipAir.isValue() && BurrowHelper.isEmpty(BurrowHelper.mc.world.getBlockState(pos))) continue;
                Render3D.drawBox(new Box(pos), color, this.width.getValue(), true, this.fill.isValue(), depth);
            }
        }
    }

    /** Вся область одной коробкой: толщина в один блок вдоль оси удара. */
    private Box areaBox(BlockPos center, Direction.Axis axis, int reach) {
        int x = axis == Direction.Axis.X ? 0 : reach;
        int y = axis == Direction.Axis.Y ? 0 : reach;
        int z = axis == Direction.Axis.Z ? 0 : reach;
        return new Box(center).expand((double)x, (double)y, (double)z);
    }

    /** Сдвиг в плоскости, перпендикулярной оси удара. */
    private static BlockPos shift(BlockPos center, Direction.Axis axis, int a, int b) {
        if (axis == Direction.Axis.Y) {
            return center.add(a, 0, b);
        }
        if (axis == Direction.Axis.X) {
            return center.add(0, a, b);
        }
        return center.add(a, b, 0);
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.isReplaceable();
    }

    /** Рамка появляется, только когда в руках предмет с зачарованием «Бур». */
    private boolean hasBurrowEnchant(ClientPlayerEntity player) {
        return BurrowHelper.mentionsEnchant(player.getStackInHand(Hand.MAIN_HAND)) || BurrowHelper.mentionsEnchant(player.getStackInHand(Hand.OFF_HAND));
    }

    /**
     * Серверные зачарования живут не в компоненте enchantments, а строками лора,
     * поэтому просматриваем и название предмета, и его описание.
     */
    private static boolean mentionsEnchant(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getName() != null && stack.getName().getString().toLowerCase().contains(ENCHANT)) {
            return true;
        }
        LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return false;
        }
        for (Text line : lore.lines()) {
            if (line == null || !line.getString().toLowerCase().contains(ENCHANT)) continue;
            return true;
        }
        return false;
    }
}
