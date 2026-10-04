package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.display.hud.HudTheme;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Пока игрок ест, под прицелом показывается, сколько голода и насыщенности даст еда,
 * что будет после неё и сколько осталось доесть. Это не модуль — работает всегда.
 */
public final class FoodInfo
implements QuickImports {
    private static final int MAX_FOOD = 20;
    private static final float OFFSET_Y = 22.0f;
    private static FoodInfo instance;

    private final FontRenderer mainFont = Fonts.getSize(15, Fonts.Type.SEMI);
    private final FontRenderer smallFont = Fonts.getSize(12, Fonts.Type.DEFAULT);

    private float alpha;
    private long lastFrame = System.currentTimeMillis();
    /** Последняя еда держится, пока плашка гаснет. */
    private ItemStack shownStack = ItemStack.EMPTY;
    private FoodComponent shownFood;
    private float shownProgress;

    private FoodInfo() {
    }

    /** Подписывается на отрисовку один раз при запуске клиента. */
    public static void init() {
        if (instance == null) {
            instance = new FoodInfo();
            EventManager.register(instance);
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        ClientPlayerEntity player = mc.player;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (float)(now - this.lastFrame) / 1000.0f);
        this.lastFrame = now;
        if (player == null) {
            this.alpha = 0.0f;
            return;
        }

        boolean eating = false;
        if (player.isUsingItem()) {
            ItemStack active = player.getActiveItem();
            FoodComponent food = (FoodComponent)active.get(DataComponentTypes.FOOD);
            if (food != null) {
                eating = true;
                this.shownStack = active;
                this.shownFood = food;
                int maxUse = active.getMaxUseTime(player);
                this.shownProgress = maxUse <= 0 ? 0.0f : MathHelper.clamp((float)player.getItemUseTime() / (float)maxUse, 0.0f, 1.0f);
            }
        }
        this.alpha = MathHelper.clamp(this.alpha + (eating ? 10.0f : -6.0f) * dt, 0.0f, 1.0f);
        if (this.alpha <= 0.01f || this.shownFood == null) {
            return;
        }
        this.render(e.getDrawContext(), player, this.shownStack, this.shownFood, this.shownProgress, this.alpha);
    }

    private void render(DrawContext context, ClientPlayerEntity player, ItemStack stack, FoodComponent food, float eatProgress, float a) {
        MatrixStack matrix = GuiCompat.stack(context);
        int nutrition = food.nutrition();
        float saturation = food.saturation();

        String hungerText = "+" + nutrition;
        String hungerLabel = " голод";
        String satText = "  +" + FoodInfo.format(saturation);
        String satLabel = " насыщ.";

        HungerManager hunger = player.getHungerManager();
        int food0 = hunger.getFoodLevel();
        float sat0 = hunger.getSaturationLevel();
        // Как в ванилле: насыщенность не может превысить уровень голода после еды.
        int food1 = Math.min(MAX_FOOD, food0 + nutrition);
        float sat1 = Math.min((float)food1, sat0 + saturation);
        String resultText = "Голод " + food0 + " → " + food1 + "   Насыщ. " + FoodInfo.format(sat0) + " → " + FoodInfo.format(sat1);

        float itemSize = 16.0f;
        float pad = 6.0f;
        float mainW = this.mainFont.getStringWidth(hungerText) + this.smallFont.getStringWidth(hungerLabel) + this.mainFont.getStringWidth(satText) + this.smallFont.getStringWidth(satLabel);
        float resultW = this.smallFont.getStringWidth(resultText);
        float contentW = Math.max(itemSize + 6.0f + mainW, resultW);
        float w = contentW + pad * 2.0f;
        float h = pad + itemSize + 11.0f + 6.0f + pad - 2.0f;

        float x = (float)window.getScaledWidth() / 2.0f - w / 2.0f;
        // Плашка чуть выезжает снизу при появлении.
        float y = (float)window.getScaledHeight() / 2.0f + OFFSET_Y + (1.0f - a) * 6.0f;
        int bgAlpha = (int)(230.0f * a);
        rectangle.render(ShapeProperties.create(matrix, x, y, w, h).round(6.0f).softness(1.0f)
                .color(HudTheme.panelTop(bgAlpha), HudTheme.panelTop(bgAlpha), HudTheme.panelBottom(bgAlpha), HudTheme.panelBottom(bgAlpha)).build());
        Rectangle.flushPending();

        float rowX = x + pad;
        float rowY = y + pad;
        if (a > 0.5f) {
            // Предмет не умеет прозрачность — показываем его, только когда плашка почти видна.
            Render2D.defaultDrawStack(context, stack, rowX, rowY, false, false, 1.0f);
        }
        float textX = rowX + itemSize + 6.0f;
        float textY = rowY + 4.0f;
        int hungerColor = FoodInfo.withAlpha(0xFFFFB347, a);
        int satColor = FoodInfo.withAlpha(0xFFFFE066, a);
        int labelColor = HudTheme.muted((int)(255.0f * a));
        this.mainFont.drawString(matrix, hungerText, textX, textY, hungerColor);
        textX += this.mainFont.getStringWidth(hungerText);
        this.smallFont.drawString(matrix, hungerLabel, textX, textY + 1.5f, labelColor);
        textX += this.smallFont.getStringWidth(hungerLabel);
        this.mainFont.drawString(matrix, satText, textX, textY, satColor);
        textX += this.mainFont.getStringWidth(satText);
        this.smallFont.drawString(matrix, satLabel, textX, textY + 1.5f, labelColor);

        float cursorY = rowY + itemSize + 2.0f;
        this.smallFont.drawString(matrix, resultText, rowX, cursorY + 1.0f, HudTheme.text((int)(220.0f * a)));
        cursorY += 11.0f;
        // Полоска: сколько осталось доесть.
        float barW = w - pad * 2.0f;
        rectangle.render(ShapeProperties.create(matrix, rowX, cursorY + 2.0f, barW, 2.0f).round(1.0f).color(HudTheme.track((int)(200.0f * a))).build());
        if (eatProgress > 0.01f) {
            rectangle.render(ShapeProperties.create(matrix, rowX, cursorY + 2.0f, barW * eatProgress, 2.0f).round(1.0f).color(HudTheme.accent((int)(255.0f * a))).build());
        }
        Rectangle.flushPending();
    }

    private static String format(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static int withAlpha(int color, float a) {
        return ((int)((color >>> 24 & 0xFF) * a) << 24) | (color & 0xFFFFFF);
    }
}
