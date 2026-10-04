package ru.mentions.utils.other;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import java.util.Map;
import java.util.WeakHashMap;
import ru.mentions.utils.animation.satoshi.Animation;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;

/** Анимации списков интерфейса (ArrayList, KeyBinds) и глифы категорий mentions для модулей Wios. */
public final class ModuleAnimations {
    private static final Map<Module, Animation[]> STORE = new WeakHashMap<>();

    private ModuleAnimations() {}

    /** Анимация модуля по слоту: 0 — KeyBinds, 15 — ArrayList. */
    public static synchronized Animation of(Module module, int slot) {
        Animation[] slots = STORE.computeIfAbsent(module, m -> new Animation[]{new EaseInOutQuad(300, 1), new EaseInOutQuad(300, 1)});
        return slots[slot == 0 ? 0 : 1];
    }

    /** Глиф иконки категории в шрифте иконок (KeyBinds). */
    public static String icon(ModuleCategory category) {
        return switch (category) {
            case PLAYER -> "N";
            case RENDER, COSMETICS -> "Q";
            default -> "L";
        };
    }

    /** Глиф иконки уведомления о переключении модуля. */
    public static String notifyGlyph(ModuleCategory category) {
        return switch (category) {
            case PLAYER -> "n";
            case RENDER, COSMETICS -> "u";
            default -> "l";
        };
    }
}
