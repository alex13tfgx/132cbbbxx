/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_4587
 */
package code.wiosvisual.utils.display.render;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.systemrender.builders.Builder;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadColorState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.SizeState;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class BackgroundIcons {
    public static final float HUD_ICON_SIZE = 9.5f;
    public static final float SIDEBAR_ICON_SIZE = 14.0f;
    public static final float ESP_CATEGORY_ICON_SIZE = 9.0f;
    public static final Identifier ESP = BackgroundIcons.icon("esp.png");
    public static final Identifier POTIONS = BackgroundIcons.icon("potions.png");
    public static final Identifier KEYBINDS = BackgroundIcons.icon("keybinds.png");
    public static final Identifier STAFFLIST = BackgroundIcons.icon("stafflist.png");
    public static final Identifier COOLDOWNS = BackgroundIcons.icon("cooldowns.png");
    public static final Identifier BACKGROUND = BackgroundIcons.icon("background.png");
    public static final Identifier MENU_WALLPAPER = BackgroundIcons.icon("menubg_mono.png");
    public static final Identifier LOGO = BackgroundIcons.icon("logo.png");
    public static final Identifier FEATHER = BackgroundIcons.icon("feather.png");
    public static final Identifier SINGLE = BackgroundIcons.icon("1.png");
    public static final Identifier MULTI = BackgroundIcons.icon("2.png");
    public static final Identifier SOLO = BackgroundIcons.icon("solo.png");
    public static final Identifier MULTIPLAYER = BackgroundIcons.icon("multiplayer.png");
    public static final Identifier SETTINGS = BackgroundIcons.icon("settings.png");
    public static final Identifier ACCOUNT = BackgroundIcons.icon("account.png");
    public static final Identifier EXIT = BackgroundIcons.icon("exit.png");
    public static final Identifier TELEGRAM = BackgroundIcons.icon("telegram.png");
    public static final Identifier YOUTUBE = BackgroundIcons.icon("youtube.png");
    public static final Identifier DISCORD = BackgroundIcons.icon("discord.png");
    /** Белые силуэты логотипов для кнопок соцсетей в меню: тонируются цветом кнопки. */
    public static final Identifier SOCIAL_TELEGRAM = BackgroundIcons.icon("social_tg.png");
    public static final Identifier SOCIAL_YOUTUBE = BackgroundIcons.icon("social_yt.png");
    public static final Identifier SOCIAL_DISCORD = BackgroundIcons.icon("social_ds.png");
    /** Иконки водяного знака: лого клиента, кружок-разделитель, режим игры, FPS, пинг. */
    public static final Identifier WM_LOGO = BackgroundIcons.icon("wm_logo.png");
    /** Светлая неоновая кромка логотипа: рисуется белым поверх логотипа в цвете темы. */
    public static final Identifier WM_LOGO_EDGE = BackgroundIcons.icon("wm_logo_edge.png");
    public static final Identifier WM_RING = BackgroundIcons.icon("wm_ring.png");
    public static final Identifier WM_MODE = BackgroundIcons.icon("wm_mode.png");
    public static final Identifier WM_FPS = BackgroundIcons.icon("wm_fps.png");
    public static final Identifier WM_PING = BackgroundIcons.icon("wm_ping.png");

    private BackgroundIcons() {
    }

    public static Identifier icon(String fileName) {
        return Identifier.of((String)"wiosvisual", (String)("background/" + fileName));
    }

    public static void draw(DrawContext context, Identifier texture, float x, float y, float size, int color) {
        // Плашки рисуются отложенно: без сброса очереди фон ляжет поверх иконки.
        Rectangle.flushPending();
        MatrixStack matrices = GuiCompat.stack(context);
        int glId = code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture.id(MinecraftClient.getInstance().getTextureManager().getTexture(texture));
        ((BuiltTexture)Builder.texture().size(new SizeState(size, size)).radius(QuadRadiusState.NO_ROUND).color(new QuadColorState(color)).smoothness(1.0f).texture(0.0f, 0.0f, 1.0f, 1.0f, glId).build()).render(matrices.peek().getPositionMatrix(), x, y, 0.0f);
    }

    /**
     * Иконка со свечением в цвете темы: та же текстура несколько раз рисуется
     * увеличенной и полупрозрачной, поэтому вокруг лого появляется мягкий ореол,
     * а сверху ложится чёткая иконка. Свечение всегда идёт цветом {@code color}.
     */
    public static void drawGlowing(DrawContext context, Identifier texture, float x, float y, float size, int color) {
        int baseAlpha = ColorAssist.alpha(color);
        for (int i = 3; i >= 1; --i) {
            float grow = (float)i * 1.7f;
            int glowAlpha = Math.round((float)baseAlpha * (0.055f * (float)(4 - i)));
            BackgroundIcons.draw(context, texture, x - grow / 2.0f, y - grow / 2.0f, size + grow, ColorAssist.replAlpha(color, glowAlpha));
        }
        BackgroundIcons.draw(context, texture, x, y, size, color);
    }

    /**
     * Знак клиента перед названием модуля в ClickGui: тот же силуэт, что и в вотермарке HUD,
     * поэтому он тонируется цветом темы, а alpha ведёт анимация списка.
     */
    public static void drawModuleLogo(DrawContext context, float x, float y, float size, int alpha) {
        int color = ColorAssist.replAlpha(ColorAssist.getHudColor(), Math.max(0, Math.min(255, alpha)));
        BackgroundIcons.draw(context, KEYBINDS, x, y, size, color);
    }

    /**
     * Свечение с регулируемой силой: {@code strength} растягивает ореол и поднимает его яркость,
     * поэтому на 1.0 картинка совпадает с {@link #drawGlowing}, а выше — заметно ярче.
     */
    public static void drawGlowing(DrawContext context, Identifier texture, float x, float y, float size, int color, float strength) {
        int baseAlpha = ColorAssist.alpha(color);
        for (int i = 4; i >= 1; --i) {
            float grow = (float)i * 1.7f * strength;
            int glowAlpha = Math.round((float)baseAlpha * (0.055f * (float)(5 - i)) * strength);
            BackgroundIcons.draw(context, texture, x - grow / 2.0f, y - grow / 2.0f, size + grow, ColorAssist.replAlpha(color, Math.min(255, glowAlpha)));
        }
        BackgroundIcons.draw(context, texture, x, y, size, color);
    }

    /**
     * Аватарка клиента: текстура рисуется в истинных цветах (белый множитель) со
     * скруглёнными углами, поэтому квадратный логотип превращается в круглый значок,
     * как аватар в вотермарках вроде CatLean. {@code radius} задаёт скругление.
     */
    public static void drawAvatar(DrawContext context, Identifier texture, float x, float y, float size, float radius, int alpha) {
        Rectangle.flushPending();
        MatrixStack matrices = GuiCompat.stack(context);
        int glId = code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture.id(MinecraftClient.getInstance().getTextureManager().getTexture(texture));
        int color = ColorAssist.replAlpha(-1, Math.max(0, Math.min(255, alpha)));
        ((BuiltTexture)Builder.texture().size(new SizeState(size, size)).radius(new QuadRadiusState(radius)).color(new QuadColorState(color)).smoothness(1.0f).texture(0.0f, 0.0f, 1.0f, 1.0f, glId).build()).render(matrices.peek().getPositionMatrix(), x, y, 0.0f);
    }

    /** Доля головы в листе скина: 8x8 пикселей из 64 — и для лица, и для шляпы. */
    private static final float SKIN_CELL = 8.0f / 64.0f;

    /**
     * Круглая голова игрока: из листа скина берётся лицо (8,8) и поверх — слой шляпы
     * (40,8), оба обрезаются шейдером по радиусу, поэтому квадратная текстура
     * превращается в круглую аватарку.
     */
    public static void drawSkinHead(DrawContext context, Identifier texture, float x, float y, float size, int alpha) {
        Rectangle.flushPending();
        MatrixStack matrices = GuiCompat.stack(context);
        int glId = code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture.id(MinecraftClient.getInstance().getTextureManager().getTexture(texture));
        int color = ColorAssist.replAlpha(-1, Math.max(0, Math.min(255, alpha)));
        float radius = size / 2.0f;
        // Лицо, затем слой шляпы поверх: у скинов с капюшоном иначе видно только лоб.
        BackgroundIcons.drawSkinLayer(matrices, glId, color, x, y, size, radius, SKIN_CELL, SKIN_CELL);
        BackgroundIcons.drawSkinLayer(matrices, glId, color, x, y, size, radius, SKIN_CELL * 5.0f, SKIN_CELL);
    }

    private static void drawSkinLayer(MatrixStack matrices, int glId, int color, float x, float y, float size, float radius, float u, float v) {
        ((BuiltTexture)Builder.texture().size(new SizeState(size, size)).radius(new QuadRadiusState(radius)).color(new QuadColorState(color)).smoothness(1.0f).texture(u, v, SKIN_CELL, SKIN_CELL, glId).build()).render(matrices.peek().getPositionMatrix(), x, y, 0.0f);
    }

    public static void drawSmall(DrawContext context, Identifier texture, float x, float y, int color) {
        BackgroundIcons.draw(context, texture, x, y, 9.5f, color);
    }

    public static void drawSidebar(DrawContext context, Identifier texture, float x, float y, int color) {
        BackgroundIcons.draw(context, texture, x, y, 14.0f, color);
    }

    public static void drawEspCategory(DrawContext context, Identifier texture, float x, float y, int color) {
        BackgroundIcons.draw(context, texture, x, y, 9.0f, color);
    }

    public static void drawSmallCentered(DrawContext context, Identifier texture, float centerX, float y, int color) {
        BackgroundIcons.drawSmall(context, texture, centerX - 4.75f, y, color);
    }

    public static void drawHudHeaderIcon(DrawContext context, Identifier texture, FontRenderer font, String label, float textY, float centerX, int color) {
        float iconY = textY + (font.getStringHeight(label) - 9.5f) / 2.0f;
        BackgroundIcons.drawGlowing(context, texture, centerX - 4.75f, iconY, 9.5f, color);
    }
}
