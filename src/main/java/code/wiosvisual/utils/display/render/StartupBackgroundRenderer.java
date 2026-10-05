/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1921
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 */
package code.wiosvisual.utils.display.render;

import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class StartupBackgroundRenderer {
    /** Обои рисуются в родных цветах, поэтому тинт белый; затемняет их уже DIM. */
    private static final int NO_TINT = -1;
    /** Затемнение поверх обоев: иначе белый текст ванильных экранов теряется на светлом небе. */
    private static final int DIM = 0x66000000;
    public static final Identifier BACKGROUND = BackgroundIcons.MENU_WALLPAPER;
    public static final Identifier LOGO = BackgroundIcons.FEATHER;
    private static final int BG_TEX_WIDTH = 1920;
    private static final int BG_TEX_HEIGHT = 1080;
    private static final int LOGO_TEX_SIZE = 128;

    private StartupBackgroundRenderer() {
    }

    public static void drawStartupBackdrop(DrawContext context, int width, int height) {
        StartupBackgroundRenderer.drawBackground(context, width, height);
    }

    public static void drawBackground(DrawContext context, int width, int height) {
        StartupBackgroundRenderer.drawTexture(context, BACKGROUND, 0, 0, width, height, BG_TEX_WIDTH, BG_TEX_HEIGHT);
    }

    public static void drawBackground(DrawContext context, float x, float y, float renderWidth, float renderHeight, float alpha) {
        StartupBackgroundRenderer.drawCover(context, (int)x, (int)y, (int)renderWidth, (int)renderHeight);
    }

    /**
     * Обои по принципу cover: кадр обрезается по UV, а не растягивается, поэтому картинка
     * не плывёт ни в окне, ни в полноэкранном режиме.
     */
    private static void drawCover(DrawContext context, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        // Фон совпадает с фоном главного меню.
        Identifier texture = ru.mentions.screen.MainMenuScreen.backgroundTexture();
        int texW = (int)ru.mentions.screen.MainMenuScreen.backgroundWidth();
        int texH = (int)ru.mentions.screen.MainMenuScreen.backgroundHeight();
        float scale = Math.max((float)width / (float)texW, (float)height / (float)texH);
        int regionWidth = Math.max(1, Math.round((float)width / scale));
        int regionHeight = Math.max(1, Math.round((float)height / scale));
        float u = (float)(texW - regionWidth) / 2.0f;
        float v = (float)(texH - regionHeight) / 2.0f;
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, texW, texH, NO_TINT);
        context.fill(x, y, x + width, y + height, DIM);
    }

    /**
     * Экран загрузки: чёрно-белые обои (cover) и тонкая белая полоса прогресса по центру снизу.
     */
    public static void drawLoader(DrawContext context, int width, int height, float progress, float alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Identifier texture = ru.mentions.screen.MainMenuScreen.loaderTexture();
        int texW = (int)ru.mentions.screen.MainMenuScreen.loaderWidth();
        int texH = (int)ru.mentions.screen.MainMenuScreen.loaderHeight();
        float scale = Math.max((float)width / (float)texW, (float)height / (float)texH);
        int regionWidth = Math.max(1, Math.round((float)width / scale));
        int regionHeight = Math.max(1, Math.round((float)height / scale));
        float u = (float)(texW - regionWidth) / 2.0f;
        float v = (float)(texH - regionHeight) / 2.0f;
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, 0, 0, u, v, width, height, regionWidth, regionHeight, texW, texH, NO_TINT);
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        int barW = Math.min(220, width / 3);
        int barX = (width - barW) / 2;
        int barY = Math.round(height * 0.82f);
        int fillW = Math.round(barW * Math.max(0.0f, Math.min(1.0f, progress)));
        context.fill(barX, barY, barX + barW, barY + 2, (a * 40 / 255) << 24 | 0xFFFFFF);
        if (fillW > 0) {
            context.fill(barX, barY, barX + fillW, barY + 2, a << 24 | 0xFFFFFF);
        }
    }

    /** Client logo, centred on {@code centerX} and drawn as a {@code size} square. */
    public static void drawLogo(DrawContext context, float centerX, float y, float size, float alpha) {
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, LOGO, (int)(centerX - size / 2.0f), (int)y, 0.0f, 0.0f, (int)size, (int)size, LOGO_TEX_SIZE, LOGO_TEX_SIZE, LOGO_TEX_SIZE, LOGO_TEX_SIZE, ThemeManager.primaryColor.getColor());
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        StartupBackgroundRenderer.drawCover(context, x, y, width, height);
    }
}
