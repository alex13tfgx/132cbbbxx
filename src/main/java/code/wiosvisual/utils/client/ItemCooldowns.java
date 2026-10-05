package code.wiosvisual.utils.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Остаток перезарядки любого предмета прямо из менеджера кд игрока.
 * Работает по группам кд, поэтому видит и ванильные предметы (перка, чарка, зелья),
 * и донатные предметы сервера с собственной группой (компонент use_cooldown).
 * Полная длительность группы запоминается в момент установки кд (см. ItemCooldownManagerMixin).
 */
public final class ItemCooldowns {
    private static final Map<Identifier, Integer> DURATIONS = new ConcurrentHashMap<>();

    private ItemCooldowns() {
    }

    public static void onSet(Identifier group, int ticks) {
        if (ticks > 0) {
            DURATIONS.put(group, ticks);
        } else {
            DURATIONS.remove(group);
        }
    }

    public static void onRemove(Identifier group) {
        DURATIONS.remove(group);
    }

    /** Остаток кд в тиках (0 — предмет не на кд). */
    public static float remainingTicks(ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || stack == null || stack.isEmpty()) {
            return 0.0f;
        }
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float progress = manager.getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
        if (progress <= 0.0f) {
            return 0.0f;
        }
        Integer duration = DURATIONS.get(manager.getGroup(stack));
        return duration == null ? 0.0f : progress * duration;
    }

    /** Остаток кд в целых секундах с округлением вверх (0 — не на кд). */
    public static int remainingSeconds(ItemStack stack) {
        float ticks = remainingTicks(stack);
        return ticks <= 0.0f ? 0 : (int)Math.ceil(ticks / 20.0f);
    }

    /**
     * Для предметов, которые рисуются не через ванильный оверлей слота (кастомный хотбар):
     * белая заливка кд снизу вверх, как в ванилле, и жёлтые секунды в левом верхнем углу.
     */
    public static void drawSlot(DrawContext context, ItemStack stack, int x, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || stack == null || stack.isEmpty()) {
            return;
        }
        float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickProgress(true));
        if (progress <= 0.0f) {
            return;
        }
        int top = y + MathHelper.floor(16.0f * (1.0f - progress));
        context.fill(x, top, x + 16, y + 16, 0x7FFFFFFF);
        int seconds = remainingSeconds(stack);
        if (seconds > 0) {
            String text = seconds >= 60 ? seconds / 60 + "m" : String.valueOf(seconds);
            context.drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFFFF55);
        }
    }

    public static void clear() {
        DURATIONS.clear();
    }
}
