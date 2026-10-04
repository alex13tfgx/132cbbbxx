package ru.mentions.module.impl.display.interfaceimpl;

import ru.mentions.Client;
import code.wiosvisual.features.module.Module;
import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.impl.display.InterFace;
import ru.mentions.theme.ThemeColor;
import ru.mentions.utils.animation.Animation;
import ru.mentions.utils.animation.Easings;
import ru.mentions.utils.animation.satoshi.Direction;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.math.Keyboard;
import ru.mentions.utils.render.Draw;
import ru.mentions.utils.render.RenderUtil;
import ru.mentions.utils.render.font.Font;
import ru.mentions.utils.render.font.Fonts;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screen.ChatScreen;

public class KeyBinds implements element {

    private static  float S = 1.0F;

    private static float H = 16F * S;
    private static float MIN_W = 50F * S;
    private static float RADIUS = 7F * S;

    private static float TITLE_TEXT = 7F * S;
    private static float TITLE_ICON = 5F * S;
    private static float ROW_TEXT = 6.5F * S;

    private static float TITLE_ICON_X = 5F * S;
    private static float TITLE_ICON_Y = 5.5F * S;
    private static float TITLE_TEXT_X = 12.5F * S;
    private static float TITLE_TEXT_Y = 3.4F * S;

    private static float ROW_HEIGHT = 14F * S;
    private static float ROW_BASE_W = 27F * S;
    private static float ROW_PADDING_X = 5F * S;
    private static float ROW_START_Y = 5F * S;
    private static float ROW_ANIM_OFFSET = 15F * S;

    private static float ICON_PADDING = 4F * S;
    private static float ICON_OFFSET_Y = 0.9F * S;
    private static float SEP_PADDING_X = 4F * S;
    private static float SEP_OFFSET_Y = 3.2F * S;

    private Animation openAnimation = new Animation();

    private ru.mentions.utils.animation.satoshi.Animation animation1 = new EaseInOutQuad(300, 1);
    private ru.mentions.utils.animation.satoshi.Animation animation2 = new EaseInOutQuad(300, 1);

    @Override
    public void onRender(DragSetting dragSetting, InterFace interFace) {
        S = InterFace.getInstance().sizeHud.getValue();
        H = 16F * S;
        MIN_W = 50F * S;
        RADIUS = 7F * S;
        TITLE_TEXT = 7F * S;
        TITLE_ICON = 5F * S;
        ROW_TEXT = 6.5F * S;
        TITLE_ICON_X = 5F * S;
        TITLE_ICON_Y = 5.5F * S;
        TITLE_TEXT_X = 12.5F * S;
        TITLE_TEXT_Y = 3.4F * S;
        ROW_HEIGHT = 14F * S;
        ROW_BASE_W = 27F * S;
        ROW_PADDING_X = 5F * S;
        ROW_START_Y = 5F * S;
        ROW_ANIM_OFFSET = 15F * S;
        ICON_PADDING = 4F * S;
        ICON_OFFSET_Y = 0.9F * S;
        SEP_PADDING_X = 4F * S;
        SEP_OFFSET_Y = 3.2F * S;

        List<code.wiosvisual.features.module.Module> modules = code.wiosvisual.Wiosvisual.getInstance().getModuleRepository().modules().stream()
                .filter(m -> m.getKey() > 0
                        && !m.isHidden()
                        && !m.getName().equalsIgnoreCase("ClickGui")
                        && (m.isState() || m.getAnimation().getOutput() > 0))
                .sorted(Comparator.comparingInt((code.wiosvisual.features.module.Module m) ->
                        m.getName().length() + Keyboard.keyName(m.getKey()).length()
                ).reversed())
                .collect(Collectors.toList());

        boolean isEmpty = modules.isEmpty();

        float x = dragSetting.position.x;
        float y = dragSetting.position.y;

        boolean closeCondition = isEmpty && !(mc.currentScreen instanceof ChatScreen);

        animation1.setDirection(closeCondition ? Direction.BACKWARDS : Direction.FORWARDS);
        animation2.setDirection((mc.currentScreen instanceof ChatScreen) && isEmpty ? Direction.FORWARDS : Direction.BACKWARDS);

        dragSetting.active = !closeCondition;

        float alpha = animation1.getOutput();

        if (closeCondition && alpha == 0.0F) return;

        float alpha2 = animation2.getOutput();

        // Draw.glow(x, y, MIN_W, H, ColorUtil.replAlpha(ColorUtil.getColor(0), alpha2 * 0.1F), RADIUS, 12, 1);
        HudPanel.fill(x, y, MIN_W, H, alpha2, RADIUS, ColorUtil.replAlpha(ru.mentions.theme.ThemeColor.getHudBackgroundColor(), alpha2 * InterFace.getInstance().alphaHUD.getValue()));
        Draw.outline(x, y, MIN_W, H, 0.75F, ThemeColor.getOutlineColor(alpha2 * 0.08F), RADIUS);

        Font font = Fonts.sf_regular;
        Font cat = Fonts.mentions_2;

        cat.draw("E", x + TITLE_ICON_X, y + TITLE_ICON_Y, TITLE_ICON, ColorUtil.replAlpha(ColorUtil.client(), alpha2));
        font.draw("Key binds", x + TITLE_TEXT_X, y + TITLE_TEXT_Y, TITLE_TEXT, ColorUtil.multAlpha(ColorUtil.getColor(240), alpha2));

        float h = 4 * S;
        float w = 0;

        for (Module m : modules) {
            ru.mentions.utils.other.ModuleAnimations.of(m, 0).setDirection(m.isState() ? Direction.FORWARDS : Direction.BACKWARDS);
            float mAnim = m.getAnimation().getOutput().floatValue();
            if (mAnim <= 0) continue;

            String name = ru.mentions.utils.other.ModuleAnimations.icon(m.getCategory());
            float rowW = ROW_BASE_W + font.getWidth(m.getVisibleName(), ROW_TEXT)
                    + font.getWidth(Keyboard.keyName(m.getKey()), ROW_TEXT)
                    + cat.getWidth(name, ROW_TEXT);

            w = Math.max(w, rowW * mAnim);
            h += ROW_HEIGHT * mAnim;
        }

        // Draw.glow(x, y, w, h - 0.5F, ColorUtil.replAlpha(ColorUtil.getColor(0), alpha * 0.1F), RADIUS, 12, 1);
        HudPanel.fill(x, y, w, h, alpha, RADIUS, ColorUtil.replAlpha(ru.mentions.theme.ThemeColor.getHudBackgroundColor(), alpha * InterFace.getInstance().alphaHUD.getValue()));
        Draw.outline(x, y, w, h, 0.75F, ThemeColor.getOutlineColor(alpha * 0.08F), RADIUS);

        float offsetY = y + ROW_START_Y;
        float offsetY2 = 0;

        for (Module m : modules) {

            float mAnim = m.getAnimation().getOutput().floatValue();
            if (mAnim <= 0) continue;

            float addX = 0;
            String name = ru.mentions.utils.other.ModuleAnimations.icon(m.getCategory());

            font.draw(m.getVisibleName(), x + ROW_PADDING_X - addX, offsetY, ROW_TEXT, ColorUtil.getColor(240, alpha * mAnim));

            String key = Keyboard.keyName(m.getKey());

            float keyWidth = font.getWidth(key, ROW_TEXT);
            float iconWidth = cat.getWidth(name, ROW_TEXT);

            font.draw(key, x + addX - ROW_PADDING_X + w - keyWidth - iconWidth - ICON_PADDING, offsetY, ROW_TEXT, ColorUtil.getColor(200, alpha * mAnim));

            cat.draw(name, x + addX - ROW_PADDING_X + w - iconWidth, offsetY + ICON_OFFSET_Y, ROW_TEXT, ColorUtil.replAlpha(ColorUtil.client(), alpha * mAnim));

            if (modules.getFirst() != m) {
                RenderUtil.Render2D.rect(x + SEP_PADDING_X + addX, offsetY - SEP_OFFSET_Y, w - (SEP_PADDING_X * 2) - addX, 0.5F,
                        ColorUtil.getColor(255, 0.05F * alpha * mAnim), 1);
            }

            offsetY += ROW_HEIGHT * mAnim;
            offsetY2 += ROW_HEIGHT * mAnim;
        }

        dragSetting.size.set(ColorUtil.overCol((int) Math.max(w, 20 * S), (int) MIN_W, alpha2),
                ColorUtil.overCol((int) offsetY2, (int) H, alpha2));
    }
}
