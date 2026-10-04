package ru.mentions.module.impl.display.interfaceimpl;

import code.wiosvisual.events.render.DrawEvent;
import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.impl.display.InterFace;
import ru.mentions.theme.ThemeColor;
import ru.mentions.utils.animation.Animation;
import ru.mentions.utils.animation.Easings;
import ru.mentions.utils.animation.satoshi.Direction;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;
import ru.mentions.utils.annotation.IMinecraft;
import ru.mentions.utils.colors.ColorFormatting;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.render.Draw;
import ru.mentions.utils.render.RenderUtil;
import ru.mentions.utils.render.font.Font;
import ru.mentions.utils.render.font.Fonts;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;

import static net.minecraft.client.gui.hud.InGameHud.getEffectTexture;

public class Potions implements IMinecraft {

    private static  float S = 1.0F;

    private static  float H = 16F * S;
    private static  float MIN_W = 44F * S;
    private static  float RADIUS = 7F * S;

    private static  float TITLE_TEXT = 7F * S;
    private static  float TITLE_ICON = 5F * S;
    private static  float ROW_TEXT = 6.5F * S;
    private static  float ICON = 7F * S;

    private static  float TITLE_ICON_X = 5F * S;
    private static  float TITLE_ICON_Y = 5.5F * S;
    private static  float TITLE_TEXT_X = 12.5F * S;
    private static  float TITLE_TEXT_Y = 3.4F * S;

    private static  float ROW_HEIGHT = 14F * S;
    private static  float ROW_BASE_W = 27F * S;
    private static  float ROW_PADDING_X = 5F * S;
    private static  float ROW_START_Y = 5F * S;
    private static  float ROW_ANIM_OFFSET = 15F * S;

    private static  float ICON_PADDING = 4F * S;
    private static  float SEP_PADDING_X = 4F * S;
    private static  float SEP_OFFSET_Y = 3.2F * S;
    private static  float SCROLL_OFFSET_Y = 4F * S;

    private ru.mentions.utils.animation.satoshi.Animation animation1 = new EaseInOutQuad(300,1);
    private ru.mentions.utils.animation.satoshi.Animation animation2 = new EaseInOutQuad(300,1);

    private final Map<String, EffectData> displayedEffects = new LinkedHashMap<>();

    public void onRender(DragSetting dragSetting, InterFace interFace, DrawEvent eventDisplay) {
        Collection<StatusEffectInstance> currentEffects = mc.player.getStatusEffects();
        displayedEffects.values().forEach(data -> data.active = false);
        S = InterFace.getInstance().sizeHud.getValue();
        H = 16F * S;
        MIN_W = 44F * S;
        RADIUS = 7F * S;
        TITLE_TEXT = 7F * S;
        TITLE_ICON = 5F * S;
        ROW_TEXT = 6.5F * S;
        ICON = 7F * S;
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
        SEP_PADDING_X = 4F * S;
        SEP_OFFSET_Y = 3.2F * S;
        SCROLL_OFFSET_Y = 4F * S;

        for (StatusEffectInstance effect : currentEffects) {
            String rawName = effect.getEffectType().value().getName().getString();
            String name = capitalizeFirst(rawName);
            int amplifier = effect.getAmplifier() + 1;
            String duration = getDurationString(effect);
            boolean isNegative = effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL;
            EffectData data = displayedEffects.computeIfAbsent(name, k -> new EffectData(name, duration, amplifier, isNegative, effect.getDuration(), effect));

            if (!duration.equals(data.duration)) {
                data.prevDuration = data.duration;
                data.duration = duration;
                data.digitAnim.set(0);
                data.digitAnim.run(1, 0.18, Easings.QUAD_OUT);
            }

            data.durationTicks = effect.getDuration();
            data.negative = isNegative;
            data.effectInstance = effect;
            data.active = true;
        }

        boolean isEmpty = displayedEffects.isEmpty();

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

        Fonts.mentions_2.draw("P", x + TITLE_ICON_X, y + TITLE_ICON_Y, TITLE_ICON, ColorUtil.replAlpha(ColorUtil.client(), alpha2));
        font.draw("Potions", x + TITLE_TEXT_X, y + TITLE_TEXT_Y, TITLE_TEXT, ColorUtil.multAlpha(ColorUtil.getColor(240), alpha2));

        List<EffectData> sortedEffects = displayedEffects.values().stream()
                .sorted(Comparator.comparingInt((EffectData data) ->
                        data.name.length() + data.duration.length()
                ).reversed())
                .collect(Collectors.toList());

        // Заголовок «Potions» всегда сверху списка эффектов.
        float header = isEmpty ? 0F : 13F * S;
        float h = 4 * S + header;
        float w = isEmpty ? 0F : TITLE_TEXT_X + font.getWidth("Potions", TITLE_TEXT) + 8F * S;

        List<String> toRemove = new ArrayList<>();

        for (EffectData data : sortedEffects) {
            data.animation.update();
            data.animation.run(data.active ? 1f : 0f, 0.12f, Easings.QUAD_OUT);
            data.digitAnim.update();

            float a = data.animation.get();

            if (a <= 0.01f && !data.active) {
                toRemove.add(data.name);
                continue;
            }

            // Ширина по содержимому: отступ, иконка, название, зазор, таймер, отступ — длинное название растягивает панель, короткое нет.
            float rowW = ROW_PADDING_X * 2F + ICON + 4F * S + font.getWidth(label(data), ROW_TEXT) + 8F * S + font.getWidth(data.duration, ROW_TEXT);

            w = Math.max(w, rowW * a);
            h += ROW_HEIGHT * a;
        }

        // Draw.glow(x, y, w, h - 0.5F, ColorUtil.replAlpha(ColorUtil.getColor(0), alpha * 0.1F), RADIUS, 12, 1);
        HudPanel.fill(x, y, w, h, alpha, RADIUS, ColorUtil.replAlpha(ru.mentions.theme.ThemeColor.getHudBackgroundColor(), alpha * InterFace.getInstance().alphaHUD.getValue()));
        Draw.outline(x, y, w, h, 0.75F, ThemeColor.getOutlineColor(alpha * 0.08F), RADIUS);

        if (header > 0F) {
            Fonts.mentions_2.draw("P", x + TITLE_ICON_X, y + TITLE_ICON_Y, TITLE_ICON, ColorUtil.replAlpha(ColorUtil.client(), alpha));
            font.draw("Potions", x + TITLE_TEXT_X, y + TITLE_TEXT_Y, TITLE_TEXT, ColorUtil.multAlpha(ColorUtil.getColor(240), alpha));
            RenderUtil.Render2D.rect(x + SEP_PADDING_X, y + header - SEP_OFFSET_Y + 1F * S, w - (SEP_PADDING_X * 2), 0.5F,
                    ColorUtil.getColor(255, 0.08F * alpha), 1);
        }

        float offsetY = y + ROW_START_Y + header;
        float offsetY2 = header;

        boolean firstRow = true;

        for (EffectData data : sortedEffects) {

            float a = data.animation.get();
            if (a <= 0.01f && !data.active) continue;

            float addX =0;

            int lvl = data.effectInstance.getAmplifier() + 1;

            boolean bad = data.negative;

            int nameColor = bad ? ColorUtil.getColor(235, 70, 70, alpha * a) : ColorUtil.getColor(240, alpha * a);
            int lvlColor  = bad ? ColorUtil.getColor(170, 55, 55, alpha * a) : ColorUtil.getColor(150, alpha * a);
            int timeColor = data.durationTicks <= 100 ? ColorUtil.getColor(255, 90, 90, alpha * a) : ColorUtil.getColor(235, alpha * a);

            String effectname = data.name + " " + ColorFormatting.getColor(lvlColor) + roman(lvl);

            font.draw(effectname, x + ROW_PADDING_X + ICON + 4F * S - addX, offsetY, ROW_TEXT, nameColor);

            String key = data.duration;
            float timeWidth = font.getWidth(key, ROW_TEXT);

            drawDuration(font, data, x + addX - ROW_PADDING_X + w - timeWidth, offsetY, ROW_TEXT, timeColor);

            drawEffectIcon(eventDisplay, data, x + ROW_PADDING_X, offsetY, alpha * a);

            if (!firstRow) {
                RenderUtil.Render2D.rect(x + SEP_PADDING_X + addX, offsetY - SEP_OFFSET_Y, w - (SEP_PADDING_X * 2) - addX, 0.5F,
                        ColorUtil.getColor(255, 0.05F * alpha * a), 1);
            }

            firstRow = false;

            offsetY += ROW_HEIGHT * a;
            offsetY2 += ROW_HEIGHT * a;
        }

        toRemove.forEach(displayedEffects::remove);

        dragSetting.size.set(ColorUtil.overCol((int) Math.max(w, 20 * S), (int) MIN_W, alpha2),
                ColorUtil.overCol((int) offsetY2, (int) H, alpha2));
    }

    private void drawDuration(Font font, EffectData data, float x, float y, float size, int color) {
        float t = data.digitAnim.get();

        String now = data.duration;
        String was = data.prevDuration == null ? now : data.prevDuration;

        float cx = x;

        for (int i = 0; i < now.length(); i++) {
            String ch = now.substring(i, i + 1);

            int j = was.length() - (now.length() - i);
            String old = j >= 0 && j < was.length() ? was.substring(j, j + 1) : null;

            if (t >= 1F || ch.equals(old)) {
                font.draw(ch, cx, y, size, color);
            } else {
                font.draw(ch, cx, y + SCROLL_OFFSET_Y * (1 - t), size, ColorUtil.multAlpha(color, t));

                if (old != null) font.draw(old, cx, y - SCROLL_OFFSET_Y * t, size, ColorUtil.multAlpha(color, 1 - t));
            }

            cx += font.getWidth(ch, size);
        }
    }

    private String label(EffectData data) {
        int lvl = data.effectInstance.getAmplifier() + 1;
        return data.name + " " + roman(lvl);
    }

    private void drawEffectIcon(DrawEvent eventDisplay, EffectData data, float x, float y, float alpha) {
        Identifier effectTex = getEffectTexture(data.effectInstance.getEffectType());

        var matrices = eventDisplay.getDrawContext().getMatrices();

        matrices.pushMatrix();
        matrices.translate(x, y);

        matrices.scale(ICON / 12F, ICON / 12F);

        eventDisplay.getDrawContext().drawGuiTexture(RenderPipelines.GUI_TEXTURED, effectTex,
                0, 0, 12, 12, ColorUtil.getColor(255, alpha));

        matrices.popMatrix();
    }

    private String getDurationString(StatusEffectInstance effect) {
        if (effect.isInfinite()) return "∞";
        int t = effect.getDuration() / 20;
        return String.format("%d:%02d", t / 60, t % 60);
    }

    private static class EffectData {
        String name;
        String duration;
        String prevDuration;
        final Animation digitAnim = new Animation();
        Animation animation;
        boolean active;
        boolean negative;
        int durationTicks;
        int lvl;
        StatusEffectInstance effectInstance;

        public EffectData(String name, String duration, int lvl, boolean negative, int durationTicks, StatusEffectInstance effectInstance) {
            this.name = name;
            this.duration = duration;
            this.prevDuration = duration;
            this.lvl = lvl;
            this.negative = negative;
            this.durationTicks = durationTicks;
            this.animation = new Animation();
            this.active = true;
            this.effectInstance = effectInstance;
        }
    }

    /** Первая буква названия — заглавная, даже если перед ней стоят коды цвета (§x) или пробелы. */
    public static String capitalizeFirstPublic(String text) {
        return capitalizeFirst(text);
    }

    static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        boolean done = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!done && c == '§' && i + 1 < text.length()) {
                out.append(c).append(text.charAt(++i));
                continue;
            }
            if (!done && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                done = true;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /** Уровень эффекта римскими цифрами: II, III, IV... Уровни выше десяти пишутся обычным числом. */
    private static String roman(int level) {
        String[] table = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level <= table.length ? table[level - 1] : String.valueOf(level);
    }
}
