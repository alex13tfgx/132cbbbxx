package ru.mentions.module.impl.display.interfaceimpl;

import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.render.RenderUtil;

/**
 * Фон панелей HUD: плотная матовая заливка без размытия и стекла. Прозрачность только у анимации
 * появления; цвет берётся из переданного (с учётом настройки прозрачности он игнорируется).
 */
public final class HudPanel {
    private HudPanel() {
    }

    public static void fill(float x, float y, float width, float height, float alpha, float radius, int tint) {
        // Фон полностью непрозрачный: слайдер прозрачности не учитывается, только анимация появления.
        int color = ColorUtil.replAlpha(tint & 0xFFFFFF | 0xFF000000, Math.max(0.0F, Math.min(1.0F, alpha)));
        RenderUtil.Render2D.rect(x, y, width, height, color, radius);
    }

    /** Совместимость со старым вызовом glass(…): лишние параметры стекла не нужны. */
    public static void fill(float x, float y, float width, float height, float alpha, float radius, int tint,
                            float unused1, float unused2, float unused3, float unused4) {
        fill(x, y, width, height, alpha, radius, tint);
    }
}
