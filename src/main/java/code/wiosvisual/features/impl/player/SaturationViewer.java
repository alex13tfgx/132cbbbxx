package code.wiosvisual.features.impl.player;

import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Предпросмотр еды: если в руке съедобный предмет, над полосой голода рисуется второй ряд
 * яблок: сколько единиц голода он восстановит. Позиции повторяют ванильную полосу голода.
 */
public class SaturationViewer
extends Module {
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");

    private final BooleanSetting onlyWhenHungry = new BooleanSetting("Только когда голоден", "Не показывать, если полоса голода уже полная").setValue(false);

    public static SaturationViewer getInstance() {
        return Instance.get(SaturationViewer.class);
    }

    public SaturationViewer() {
        super("SaturationViewer", "Saturation Viewer", ModuleCategory.PLAYER);
        this.setup(this.onlyWhenHungry);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        PlayerEntity player = SaturationViewer.mc.player;
        if (player == null || SaturationViewer.mc.world == null || SaturationViewer.mc.options.hudHidden) {
            return;
        }
        FoodComponent food = this.heldFood(player);
        if (food == null) {
            return;
        }
        int level = player.getHungerManager().getFoodLevel();
        if (this.onlyWhenHungry.isValue() && level >= 20) {
            return;
        }
        int after = Math.min(20, level + food.nutrition());
        int gain = after - level;
        if (gain <= 0) {
            return;
        }

        DrawContext context = e.getDrawContext();
        int right = context.getScaledWindowWidth() / 2 + 91;
        // Вторая строка: на один ряд (10 px) выше ванильной полосы голода.
        int y = context.getScaledWindowHeight() - 39 - 10;
        int color = 0xB0FFFFFF;
        // Иконка i (0..9) занимает единицы голода 2i+1 и 2i+2; рисуем то, что добавит еда.
        for (int i = 0; i < 10; i++) {
            int low = i * 2 + 1;
            int high = low + 1;
            int x = right - i * 8 - 9;
            if (after >= high && level < high) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_FULL, x, y, 9, 9, color);
            } else if (after >= low && level < low) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_HALF, x, y, 9, 9, color);
            }
        }
    }

    private FoodComponent heldFood(PlayerEntity player) {
        FoodComponent food = (FoodComponent)player.getMainHandStack().get(DataComponentTypes.FOOD);
        return food != null ? food : (FoodComponent)player.getOffHandStack().get(DataComponentTypes.FOOD);
    }
}
