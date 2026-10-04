package ru.mentions.theme;

import code.wiosvisual.display.hud.HudTheme;
import code.wiosvisual.utils.theme.ThemeManager;
import ru.mentions.utils.colors.ColorUtil;

/** Цвета интерфейса mentions, привязанные к теме HUD Wios. */
public final class ThemeColor {

    private ThemeColor() {}

    /** Акцент выбранной темы Wios (ThemeEditor меняет ThemeManager.primaryColor). */
    private static int accent() {
        // Фиолетовый акцент HUD, как на референсе (PulseVisuals): не зависит от выбранной темы.
        return 0xFF8A63FF;
    }

    public static int getVisualColor() {
        return accent();
    }

    public static int getHudColor() {
        return accent();
    }

    public static int getHudColor(float alpha) {
        return ColorUtil.replAlpha(accent(), alpha);
    }

    public static int getTextColor() {
        return HudTheme.text(255);
    }

    public static int getDarkTextColor() {
        return HudTheme.muted(255);
    }

    /** Почти чёрный фон панелей HUD (Potions, Key Binds, Target Hud, Watermark, Music, уведомления). */
    public static int getHudBackgroundColor() {
        return 0xFF0E0D11;
    }

    public static int getBackgroundColor() {
        return HudTheme.panel(255);
    }

    public static int getOutlineColor() {
        return HudTheme.outline(255);
    }

    public static int getSeparatorColor() {
        return ColorUtil.multAlpha(HudTheme.text(255), 0.1F);
    }

    public static int getLightBackgroundColor() {
        return HudTheme.item(255);
    }

    public static float getOpacity() {
        return HudTheme.backgroundOpacity();
    }

    public static int getBlur() {
        return 0;
    }

    public static boolean getShadow() {
        return false;
    }

    public static boolean getDistortion() {
        return false;
    }

    public static int getSeparatorColor(float alpha) {
        return ColorUtil.multAlpha(HudTheme.text(255), 0.1F * alpha);
    }

    public static int getTextColor(float alpha) {
        return ColorUtil.replAlpha(HudTheme.text(255), alpha);
    }

    public static int getLightBackgroundColor(float alpha) {
        return ColorUtil.multAlpha(HudTheme.item(255), alpha);
    }

    public static int getOutlineColor(float alpha) {
        return ColorUtil.multAlpha(HudTheme.outline(255), alpha);
    }

    public static int getDarkTextColor(float alpha) {
        return ColorUtil.replAlpha(HudTheme.muted(255), alpha);
    }

    public static int getBackgroundColor(float alpha) {
        return ColorUtil.replAlpha(HudTheme.panel(255), alpha);
    }
}
