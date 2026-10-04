package code.wiosvisual.display.hud;

import code.wiosvisual.utils.theme.ThemeManager;

/**
 * Palette helper ported from the neverlose HUD. The base tones come from that client, the accent is
 * taken from Wiosvisual's own {@link ThemeManager} so the ported elements follow the active theme.
 */
public final class HudTheme {
    // Фон HUD — тёмный (темнее прошлой правки, но не глухой чёрный); акцент остаётся цветом темы.
    private static final Palette BASE = new Palette(HudTheme.rgb(11, 11, 22), HudTheme.rgb(13, 13, 25), HudTheme.rgb(10, 10, 20), HudTheme.rgb(18, 18, 33), HudTheme.rgb(22, 22, 39), HudTheme.rgb(42, 44, 62), HudTheme.rgb(133, 156, 255), HudTheme.rgb(255, 255, 255), HudTheme.rgb(128, 132, 150), HudTheme.rgb(20, 20, 36));

    private HudTheme() {
    }

    private static int rgb(int r, int g, int b) {
        return r << 16 | g << 8 | b;
    }

    /**
     * Последняя собранная палитра и акцент, для которого она считалась. Геттеров цвета
     * одиннадцать и дёргают их все элементы HUD каждый кадр, а сама палитра меняется только
     * при смене цвета темы — пересчитываем только тогда.
     */
    private static Palette cached = BASE;
    private static int cachedAccent = -1;

    private static Palette active() {
        return BASE;
    }

    /**
     * Фиксированный оранжевый акцент бренда (ReallyVisuals) — не зависит от темы клиента.
     * Используется ClickGui и логотипом ватермарки: они всегда оранжевые, как на референсе.
     */
    private static final int FIXED_ACCENT = 0x859CFF;

    public static int fixedAccent(int alpha) {
        return HudTheme.color(FIXED_ACCENT, alpha);
    }

    public static int fixedAccentBright(int alpha) {
        return HudTheme.color(HudTheme.mix(FIXED_ACCENT, 0xFFFFFF, 0.38f), alpha);
    }

    public static int fixedAccentSecondary(int alpha) {
        return HudTheme.color(HudTheme.mix(FIXED_ACCENT, 0x000000, 0.28f), alpha);
    }

    /**
     * Серый акцент HUD-панелей (Potions/Hot Keys/Cooldowns) — отдельно от оранжевого бренда
     * ClickGui/ватермарки, по просьбе «сделай весь Hud серым».
     */
    private static final int HUD_GRAY = 0x859CFF;

    public static int hudGray(int alpha) {
        return HudTheme.color(HUD_GRAY, alpha);
    }

    public static int hudGrayBright(int alpha) {
        return HudTheme.color(HudTheme.mix(HUD_GRAY, 0xFFFFFF, 0.35f), alpha);
    }

    public static int hudGraySecondary(int alpha) {
        return HudTheme.color(HudTheme.mix(HUD_GRAY, 0x000000, 0.25f), alpha);
    }

    /**
     * Цветные иконки панелей (Potions/Hot Keys/Cooldowns) — сама иконка своим цветом,
     * остальной ряд (текст, бейдж) остаётся серым ({@link #hudGray}).
     */
    private static final int POTIONS_ICON = 0x859CFF;
    private static final int HOTKEYS_ICON = 0x859CFF;
    private static final int COOLDOWNS_ICON = 0x859CFF;

    public static int potionsIcon(int alpha) {
        return HudTheme.color(POTIONS_ICON, alpha);
    }

    public static int potionsIconBright(int alpha) {
        return HudTheme.color(HudTheme.mix(POTIONS_ICON, 0xFFFFFF, 0.35f), alpha);
    }

    public static int potionsIconSecondary(int alpha) {
        return HudTheme.color(HudTheme.mix(POTIONS_ICON, 0x000000, 0.25f), alpha);
    }

    public static int hotkeysIcon(int alpha) {
        return HudTheme.color(HOTKEYS_ICON, alpha);
    }

    public static int hotkeysIconBright(int alpha) {
        return HudTheme.color(HudTheme.mix(HOTKEYS_ICON, 0xFFFFFF, 0.35f), alpha);
    }

    public static int hotkeysIconSecondary(int alpha) {
        return HudTheme.color(HudTheme.mix(HOTKEYS_ICON, 0x000000, 0.25f), alpha);
    }

    public static int cooldownsIcon(int alpha) {
        return HudTheme.color(COOLDOWNS_ICON, alpha);
    }

    public static int cooldownsIconBright(int alpha) {
        return HudTheme.color(HudTheme.mix(COOLDOWNS_ICON, 0xFFFFFF, 0.35f), alpha);
    }

    public static int cooldownsIconSecondary(int alpha) {
        return HudTheme.color(HudTheme.mix(COOLDOWNS_ICON, 0x000000, 0.25f), alpha);
    }

    // То же самое для остальных HUD-панелей (CremKeys/CustomHotbar/MusicInfo/TargetHud):
    // свой цветной акцент вместо цвета темы, панель — общий тёмный фон.
    private static final int CREMKEYS_ICON = 0x859CFF;
    private static final int HOTBAR_ICON = 0x859CFF;
    private static final int MUSIC_ICON = 0x859CFF;
    private static final int TARGET_ICON = 0x859CFF;

    public static int cremkeysIcon(int alpha) {
        return HudTheme.color(CREMKEYS_ICON, alpha);
    }

    public static int cremkeysIconBright(int alpha) {
        return HudTheme.color(HudTheme.mix(CREMKEYS_ICON, 0xFFFFFF, 0.35f), alpha);
    }

    public static int hotbarIcon(int alpha) {
        return HudTheme.color(HOTBAR_ICON, alpha);
    }

    public static int musicIcon(int alpha) {
        return HudTheme.color(MUSIC_ICON, alpha);
    }

    public static int musicIconBright(int alpha) {
        return HudTheme.color(HudTheme.mix(MUSIC_ICON, 0xFFFFFF, 0.35f), alpha);
    }

    public static int targetIcon(int alpha) {
        return HudTheme.color(TARGET_ICON, alpha);
    }

    /** Второй цвет градиентов — оттенок цвета темы, как {@code ColorAssist.getHudColor2()}. */
    public static int accentSecondary(int alpha) {
        try {
            return HudTheme.color(ThemeManager.accentShade(ThemeManager.primaryColor.getColor()), alpha);
        }
        catch (Throwable ignored) {
            return HudTheme.accent(alpha);
        }
    }

    public static int panel(int alpha) {
        return HudTheme.color(HudTheme.active().panel, HudTheme.backgroundAlpha(alpha));
    }

    public static int panelTop(int alpha) {
        return HudTheme.color(HudTheme.active().panelTop, HudTheme.backgroundAlpha(alpha));
    }

    public static int panelBottom(int alpha) {
        return HudTheme.color(HudTheme.active().panelBottom, HudTheme.backgroundAlpha(alpha));
    }

    public static int item(int alpha) {
        return HudTheme.color(HudTheme.active().item, HudTheme.backgroundAlpha(alpha));
    }

    public static int itemSoft(int alpha) {
        return HudTheme.color(HudTheme.active().itemSoft, HudTheme.backgroundAlpha(alpha));
    }

    public static int outline(int alpha) {
        return HudTheme.color(HudTheme.active().outline, HudTheme.backgroundAlpha(alpha));
    }

    /**
     * Осветлённый акцент для иконок: чистый цвет темы на тёмной панели читается тускло,
     * поэтому он подмешивается к белому и уходит примерно на треть в светлую сторону.
     */
    public static int accentBright(int alpha) {
        return HudTheme.color(HudTheme.mix(HudTheme.active().accent, 0xFFFFFF, 0.38f), alpha);
    }

    public static int accent(int alpha) {
        return HudTheme.color(HudTheme.active().accent, alpha);
    }

    public static int text(int alpha) {
        return HudTheme.color(HudTheme.active().text, alpha);
    }

    public static int muted(int alpha) {
        return HudTheme.color(HudTheme.active().muted, alpha);
    }

    public static int track(int alpha) {
        return HudTheme.color(HudTheme.active().track, HudTheme.backgroundAlpha(alpha));
    }

    /**
     * Светлая ли сейчас тема клиента — определяется по яркости пользовательского фона GUI.
     * Элементы HUD используют это, чтобы подменить чёрные подложки и белый текст на светлые.
     */
    public static boolean isLight() {
        try {
            int background = ThemeManager.BackgroundGui.getColor();
            int r = background >> 16 & 0xFF;
            int g = background >> 8 & 0xFF;
            int b = background & 0xFF;
            return (r * 299 + g * 587 + b * 114) / 1000 > 140;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Альфа подложек HUD. Подложки всегда непрозрачные: номинальные значения панелей
     * (242 и выше) доводятся до полной непрозрачности. Меньшие значения — это анимация
     * появления или внутренние плашки поверх уже чёрной панели, их оставляем как есть.
     */
    public static int backgroundAlpha(int alpha) {
        // Delta: фон HUD полупрозрачный (альфа 170 из 255), внутренние плашки пропорционально слабее.
        return alpha >= 200 ? 170 : HudTheme.clamp(Math.round(alpha * 0.67f));
    }

    /** Множитель прозрачности подложек: HUD больше не просвечивает. */
    public static float backgroundOpacity() {
        return 1.0f;
    }

    private static int color(int rgb, int alpha) {
        return HudTheme.clamp(alpha) << 24 | rgb & 0xFFFFFF;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int mix(int base, int accent, float factor) {
        int r = (int)((float)(base >> 16 & 0xFF) + (float)((accent >> 16 & 0xFF) - (base >> 16 & 0xFF)) * factor);
        int g = (int)((float)(base >> 8 & 0xFF) + (float)((accent >> 8 & 0xFF) - (base >> 8 & 0xFF)) * factor);
        int b = (int)((float)(base & 0xFF) + (float)((accent & 0xFF) - (base & 0xFF)) * factor);
        return HudTheme.clamp(r) << 16 | HudTheme.clamp(g) << 8 | HudTheme.clamp(b);
    }

    private record Palette(int panel, int panelTop, int panelBottom, int item, int itemSoft, int outline, int accent, int text, int muted, int track) {
    }
}
