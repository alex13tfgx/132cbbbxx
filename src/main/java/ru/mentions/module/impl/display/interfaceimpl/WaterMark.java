package ru.mentions.module.impl.display.interfaceimpl;

import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.impl.display.InterFace;
import ru.mentions.theme.ThemeColor;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.render.Draw;
import ru.mentions.utils.render.RenderUtil;
import ru.mentions.utils.render.RollingText;
import ru.mentions.utils.render.font.Font;
import ru.mentions.utils.render.font.Fonts;

public class WaterMark implements element {

    private static float S = 1.0F;

    private static float TEXT = 5.5F * S;
    private static float ICON = 5F * S;

    private static float H = 16F * S;
    private static float RADIUS = 7F * S;

    private static float PAD = 6F * S;
    private static float GAP = 6F * S;
    private static float INNER = 3F * S;
    private static float SEP_W = 0.5F;

    private static float LOGO_W = 17F * S;

    private static float ICON_Y = 5.5F * S;
    private static float TEXT_Y = 4.4F * S;
    private static float SEP_Y = 4.5F * S;

    private static boolean centered;

    /** Позиция ватермарки уже загружена из сохранения: центрировать при первом показе не нужно. */
    public static void markPlaced() {
        centered = true;
    }

    private final RollingText fpsText = new RollingText(3F);
    private final RollingText pingText = new RollingText(3F);

    @Override
    public void onRender(DragSetting dragSetting, InterFace interFace) {

        S = InterFace.getInstance().sizeHud.getValue() * 1.1F;
        TEXT = 5.5F * S;
        ICON = 5F * S;
        H = 16F * S;
        RADIUS = 7F * S;
        PAD = 6F * S;
        GAP = 6F * S;
        INNER = 3F * S;
        SEP_W = 0.5F;
        LOGO_W = 17F * S;
        ICON_Y = 5.5F * S;
        TEXT_Y = 4.4F * S;
        SEP_Y = 4.5F * S;

        float x = dragSetting.position.x;
        float y = dragSetting.position.y;

        Font fonts = Fonts.sf_medium;

        String user = "User";

        int pings = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                pings = entry.getLatency();
            }
        }

        fpsText.set(String.valueOf(mc.getCurrentFps()));
        pingText.set(String.valueOf(pings));

        float userW = fonts.getWidth(user, TEXT);
        float fpsW = fpsText.width(fonts, TEXT) + fonts.getWidth("fps", TEXT);
        float msW = pingText.width(fonts, TEXT) + fonts.getWidth("ms", TEXT);

        float userIconW = Fonts.mentions_2.getWidth("N", ICON);
        float fpsIconW = Fonts.fps.getWidth("\uE000", ICON);
        float msIconW = Fonts.ping.getWidth("\uE000", ICON);

        float userBlockW = userIconW + INNER + userW;
        float fpsBlockW = fpsIconW + INNER + fpsW;
        float msBlockW = msIconW + INNER + msW;

        float totalW = PAD * 2F + LOGO_W + userBlockW + fpsBlockW + msBlockW + GAP * 6F + SEP_W * 3F;

        HudPanel.fill(x, y, totalW, H, 1, RADIUS,
                ColorUtil.replAlpha(ru.mentions.theme.ThemeColor.getHudBackgroundColor(), InterFace.getInstance().alphaHUD.getValue()));
        Draw.outline(x, y, totalW, H, 0.75F, ThemeColor.getOutlineColor(0.08F), RADIUS);

        float cursor = x + PAD;

        // \u041B\u043E\u0433\u043E\u0442\u0438\u043F Wios \u0432\u043C\u0435\u0441\u0442\u043E \u0433\u043B\u0438\u0444\u0430 mentions: \u0446\u0432\u0435\u0442\u043D\u0430\u044F \u043E\u0441\u043D\u043E\u0432\u0430 + \u0441\u0432\u0435\u0442\u043B\u0430\u044F \u043A\u0440\u043E\u043C\u043A\u0430 \u0441\u0432\u0435\u0440\u0445\u0443.
        float logoSize = Math.round(15F * S / 2F) * 2F;
        float logoX = snap(cursor + LOGO_W / 2F - logoSize / 2F);
        float logoY = snap(y + H / 2F - logoSize / 2F);
        RenderUtil.Images.texture(code.wiosvisual.utils.display.render.BackgroundIcons.icon("wm_icon64.png"), logoX, logoY, logoSize, logoSize, ColorUtil.getClientColor(1));
        cursor += LOGO_W + GAP;

        drawSeparator(cursor, y);
        cursor += SEP_W + GAP;

        icon("wm_user.png", cursor + 0.0552F * ICON, y + ICON_Y - 0.14375F * ICON, 0.8906F * ICON, 1.1875F * ICON, ColorUtil.getClientColor(1));
        fonts.draw(user, cursor + userIconW + INNER, y + TEXT_Y, TEXT, ThemeColor.getTextColor());
        cursor += userBlockW + GAP;

        drawSeparator(cursor, y);
        cursor += SEP_W + GAP;

        icon("wm_fps_g.png", cursor, y + ICON_Y - 0.05F * ICON, 1.5258F * ICON, ICON, ThemeColor.getHudColor());
        drawValue(fonts, fpsText, "fps", cursor + fpsIconW + INNER, y + TEXT_Y);
        cursor += fpsBlockW + GAP;

        drawSeparator(cursor, y);
        cursor += SEP_W + GAP;

        icon("wm_ping_g.png", cursor, y + ICON_Y - 0.05F * ICON, 1.2596F * ICON, ICON, ThemeColor.getHudColor());
        drawValue(fonts, pingText, "ms", cursor + msIconW + INNER, y + TEXT_Y);

        if (!centered) {
            float centerX = (mc.getWindow().getFramebufferWidth() / 2F - totalW) / 2F;
            dragSetting.targetPosition.set(centerX, 6F);
            dragSetting.position.set(centerX, 6F);
            centered = true;
        }

        dragSetting.size.set(totalW, H);
    }

    private void drawSeparator(float x, float y) {
        RenderUtil.Render2D.rect(x, y + SEP_Y, SEP_W, H - SEP_Y * 2F, ThemeColor.getSeparatorColor(), 0.25F);
    }

    private float drawValue(Font font, RollingText value, String suffix, float x, float y) {
        value.draw(font, x, y, TEXT, ThemeColor.getTextColor());

        float width = value.width(font, TEXT);

        font.draw(suffix, x + width, y, TEXT, ColorUtil.getColor(200));

        return width + font.getWidth(suffix, TEXT);
    }

    /** Привязка к физическим пикселям: виртуальная единица HUD равна двум пикселям экрана. */
    private static float snap(float value) {
        return Math.round(value * 2F) / 2F;
    }

    /** Иконка как готовая картинка с кромкой в один пиксель, вместо сглаженного SDF-глифа: чёткая и без мыла. */
    private static void icon(String texture, float x, float y, float w, float h, int color) {
        float sw = Math.round(w * 2F) / 2F;
        float sh = Math.round(h * 2F) / 2F;
        RenderUtil.Images.texture(code.wiosvisual.utils.display.render.BackgroundIcons.icon(texture), snap(x), snap(y), sw, sh, color);
    }
}
