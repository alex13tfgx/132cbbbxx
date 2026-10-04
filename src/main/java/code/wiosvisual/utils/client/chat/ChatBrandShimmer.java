package code.wiosvisual.utils.client.chat;

import code.wiosvisual.utils.display.color.ColorAssist;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;

/**
 * Переливающийся префикс клиента в чате — как «WiosAlpha» в ватермарке. Чат хранит
 * готовый текст, поэтому цвет нельзя «запечь» при отправке: символы префикса помечены
 * вставкой BRAND_MARKER в стиле, а при каждой отрисовке строки чата их цвет считается
 * заново — градиент между цветами темы и бегущий по буквам блик.
 */
public final class ChatBrandShimmer {
    /** Метка символов префикса. Это вставка по Shift+клику — вставит само название, безвредно. */
    public static final String BRAND_MARKER = "WiosVisualsAlpha";

    /** Как у ватермарки: период пробега блика, ширина вспышки и её сила. */
    private static final long SWEEP_MS = 2200L;
    private static final float SWEEP_SIGMA = 0.0578f;
    private static final float SWEEP_STRENGTH = 0.9f;

    private ChatBrandShimmer() {
    }

    /**
     * Оборачивает строку чата: символы с меткой получают цвет перелива, остальные
     * проходят как есть. Строки без префикса не меняются.
     */
    public static OrderedText wrap(OrderedText line) {
        if (line == null) {
            return null;
        }
        return visitor -> {
            int total = BRAND_MARKER.length();
            int from = ColorAssist.getHudColor();
            int to = ColorAssist.getHudColor2();
            float phase = (float)(System.currentTimeMillis() % SWEEP_MS) / (float)SWEEP_MS;
            int[] index = new int[]{0};
            return line.accept((charIndex, style, codePoint) -> {
                if (!BRAND_MARKER.equals(style.getInsertion())) {
                    return visitor.accept(charIndex, style, codePoint);
                }
                float centre = ((float)index[0] + 0.5f) / (float)total;
                index[0]++;
                int base = ColorAssist.lerp(centre, from, to);
                // Гауссова вспышка, зациклённая по времени: светлый блик бежит по названию.
                float delta = centre - phase;
                delta -= (float)Math.floor(delta + 0.5f);
                float highlight = (float)Math.exp(-(delta * delta) / SWEEP_SIGMA);
                int color = ColorAssist.lerp(highlight * SWEEP_STRENGTH, base, -1) & 0xFFFFFF;
                Style shimmer = style.withColor(color);
                return visitor.accept(charIndex, shimmer, codePoint);
            });
        };
    }
}
