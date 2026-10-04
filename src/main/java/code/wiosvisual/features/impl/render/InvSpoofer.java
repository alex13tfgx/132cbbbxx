package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.container.HandledScreenEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.render.GuiCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/**
 * Подсвечивает зелья и стрелы с эффектами в любом инвентаре: баффы зелёным, дебаффы красным.
 * Если у предмета есть и вредные, и полезные эффекты, он считается дебаффом.
 */
public class InvSpoofer
extends Module {
    private static final int BUFF_COLOR = 0x8A55C878;
    private static final int DEBUFF_COLOR = 0x8AD25A5A;

    private final BooleanSetting buffs = new BooleanSetting("Баффы", "Подсвечивать полезные зелья зелёным").setValue(true);
    private final BooleanSetting debuffs = new BooleanSetting("Дебаффы", "Подсвечивать вредные зелья красным").setValue(true);

    public static InvSpoofer getInstance() {
        return Instance.get(InvSpoofer.class);
    }

    public InvSpoofer() {
        super("InvSpoofer", "Inv Spoofer", ModuleCategory.RENDER);
        this.setup(this.buffs, this.debuffs);
    }

    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        Screen current = InvSpoofer.mc.currentScreen;
        if (!(current instanceof HandledScreen) || InvSpoofer.mc.player == null) {
            return;
        }
        HandledScreen screen = (HandledScreen)current;
        ScreenHandler handler = InvSpoofer.mc.player.currentScreenHandler;
        if (handler == null) {
            return;
        }
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = GuiCompat.stack(context);
        int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
        int offsetY = (screen.height - e.getBackgroundHeight()) / 2;
        matrix.push();
        matrix.translate((float)offsetX, (float)offsetY, 0.0f);
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            int kind = this.classify(stack);
            if (kind == 0 || kind == 1 && !this.buffs.isValue() || kind == 2 && !this.debuffs.isValue()) continue;
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, kind == 1 ? BUFF_COLOR : DEBUFF_COLOR);
        }
        matrix.pop();
    }

    /** 0 — не зелье, 1 — только полезные эффекты, 2 — есть вредные. */
    private int classify(ItemStack stack) {
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        boolean potionItem = id.contains("potion") || id.contains("tipped_arrow");
        PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents != null) {
            boolean any = false;
            for (StatusEffectInstance effect : contents.getEffects()) {
                any = true;
                if (effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                    return 2;
                }
            }
            if (any) {
                return 1;
            }
        }
        if (!potionItem) {
            return 0;
        }
        // Серверные зелья часто без настоящих эффектов: баффы и дебаффы только в подписи (lore),
        // красным и зелёным/синим цветом. Смотрим на цвет строк того, что показывает подсказка предмета.
        return this.tooltipKind(stack);
    }

    private final java.util.IdentityHashMap<ItemStack, Integer> tooltipCache = new java.util.IdentityHashMap<>();
    private long cacheStamp;

    private int tooltipKind(ItemStack stack) {
        long now = System.currentTimeMillis();
        if (now - this.cacheStamp > 1500L) {
            this.tooltipCache.clear();
            this.cacheStamp = now;
        }
        Integer cached = this.tooltipCache.get(stack);
        if (cached != null) {
            return cached;
        }
        int result = 0;
        try {
            java.util.List<Text> lines = stack.getTooltip(Item.TooltipContext.create(InvSpoofer.mc.world), InvSpoofer.mc.player, TooltipType.BASIC);
            for (int i = 1; i < lines.size(); i++) {
                int kind = lineKind(lines.get(i));
                if (kind == 2) {
                    result = 2;
                    break;
                }
                if (kind == 1) {
                    result = 1;
                }
            }
        } catch (Throwable ignored) {
            result = 0;
        }
        this.tooltipCache.put(stack, result);
        return result;
    }

    /** 1 — строка зелёная/синяя (бафф), 2 — красная (дебафф), 0 — нейтральная. */
    private static int lineKind(Text line) {
        String text = line.getString();
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'c' || code == '4') return 2;
            if ("a29b31".indexOf(code) >= 0) return 1;
        }
        int rgb = firstColor(line);
        if (rgb < 0) {
            return 0;
        }
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        if (r > g + 50 && r > b + 50) return 2;
        if (g > r + 40 || b > r + 40) return 1;
        return 0;
    }

    private static int firstColor(Text text) {
        TextColor color = text.getStyle().getColor();
        if (color != null) {
            return color.getRgb();
        }
        for (Text sibling : text.getSiblings()) {
            int rgb = firstColor(sibling);
            if (rgb >= 0) {
                return rgb;
            }
        }
        return -1;
    }
}
