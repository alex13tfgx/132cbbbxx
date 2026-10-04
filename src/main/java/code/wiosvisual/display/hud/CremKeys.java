package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Кейстроки: W A S D и пробел. Нажатая клавиша плавно заливается цветом темы
 * и чуть проседает. Подписи берутся из текущих биндов, а не зашиты как WASD.
 */
public class CremKeys
extends AbstractDraggable {
    private static final float KEY = 22.0f;
    private static final float GAP = 2.0f;
    private static final float SPACE_H = 12.0f;

    private final FontRenderer keyFont = Fonts.getSize(14, Fonts.Type.SEMI);
    /** forward, left, back, right, jump */
    private final float[] press = new float[5];
    private long lastFrame = System.currentTimeMillis();

    public CremKeys() {
        super("CremKeys", 10, 200, (int)(KEY * 3.0f + GAP * 2.0f), (int)(KEY * 2.0f + GAP * 2.0f + SPACE_H), true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    @Override
    public boolean visible() {
        return Hud.interfaceSettings.isSelected("CremKeys") && Hud.getInstance() != null && Hud.getInstance().state;
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!this.visible() || CremKeys.mc.options == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (float)(now - this.lastFrame) / 1000.0f);
        this.lastFrame = now;
        KeyBinding[] binds = new KeyBinding[]{mc.options.forwardKey, mc.options.leftKey, mc.options.backKey, mc.options.rightKey, mc.options.jumpKey};
        // Нажатие заливается быстро, отпускание гаснет мягче.
        for (int i = 0; i < binds.length; ++i) {
            boolean down = binds[i].isPressed();
            float speed = down ? 22.0f : 9.0f;
            this.press[i] = MathHelper.clamp(this.press[i] + (down ? 1.0f : -1.0f) * speed * dt, 0.0f, 1.0f);
        }

        float width = KEY * 3.0f + GAP * 2.0f;
        float height = KEY * 2.0f + GAP * 2.0f + SPACE_H;
        this.setWidth((int)width);
        this.setHeight((int)height);
        MatrixStack matrix = GuiCompat.stack(context);
        float x = this.getX();
        float y = this.getY();

        this.drawKey(matrix, x + KEY + GAP, y, KEY, KEY, this.label(binds[0]), this.press[0]);
        this.drawKey(matrix, x, y + KEY + GAP, KEY, KEY, this.label(binds[1]), this.press[1]);
        this.drawKey(matrix, x + KEY + GAP, y + KEY + GAP, KEY, KEY, this.label(binds[2]), this.press[2]);
        this.drawKey(matrix, x + (KEY + GAP) * 2.0f, y + KEY + GAP, KEY, KEY, this.label(binds[3]), this.press[3]);
        this.drawSpace(matrix, x, y + (KEY + GAP) * 2.0f, width, SPACE_H, this.press[4]);
    }

    private void drawKey(MatrixStack matrix, float x, float y, float w, float h, String label, float t) {
        float e = CremKeys.easeOut(t);
        // Лёгкое «проседание» клавиши при нажатии.
        float inset = 0.8f * e;
        float kx = x + inset;
        float ky = y + inset;
        float kw = w - inset * 2.0f;
        float kh = h - inset * 2.0f;
        this.drawBase(matrix, kx, ky, kw, kh, e, 5.0f);
        int text = CremKeys.lerp(HudTheme.text(225), 0xFFFFFFFF, e);
        this.keyFont.drawCenteredString(matrix, label, kx + kw / 2.0f, ky + kh / 2.0f - 3.5f, text);
    }

    private void drawSpace(MatrixStack matrix, float x, float y, float w, float h, float t) {
        float e = CremKeys.easeOut(t);
        float inset = 0.6f * e;
        float kx = x + inset;
        float ky = y + inset;
        float kw = w - inset * 2.0f;
        float kh = h - inset * 2.0f;
        this.drawBase(matrix, kx, ky, kw, kh, e, 4.0f);
        float lineW = kw * 0.38f;
        int line = CremKeys.lerp(HudTheme.muted(220), 0xFFFFFFFF, e);
        rectangle.render(ShapeProperties.create(matrix, kx + (kw - lineW) / 2.0f, ky + kh / 2.0f - 0.75f, lineW, 1.5f).round(0.75f).color(line).build());
        Rectangle.flushPending();
    }

    private void drawBase(MatrixStack matrix, float x, float y, float w, float h, float e, float round) {
        int top = CremKeys.lerp(HudTheme.panelTop(225), HudTheme.cremkeysIconBright(245), e);
        int bottom = CremKeys.lerp(HudTheme.panelBottom(225), HudTheme.cremkeysIcon(245), e);
        rectangle.render(ShapeProperties.create(matrix, x - 0.5f, y - 0.5f, w + 1.0f, h + 1.0f).round(round + 0.5f).softness(0.5f).color(HudTheme.outline((int)(90.0f + 80.0f * e))).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, w, h).round(round).softness(0.5f).color(top, top, bottom, bottom).build());
        Rectangle.flushPending();
    }

    private String label(KeyBinding bind) {
        String name = bind.getBoundKeyLocalizedText().getString();
        if (name == null || name.isEmpty()) {
            return "?";
        }
        name = name.toUpperCase(Locale.ROOT);
        return name.length() > 3 ? name.substring(0, 3) : name;
    }

    private static float easeOut(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private static int lerp(int c1, int c2, float f) {
        int a = (int)MathHelper.lerp(f, c1 >>> 24 & 0xFF, c2 >>> 24 & 0xFF);
        int r = (int)MathHelper.lerp(f, c1 >> 16 & 0xFF, c2 >> 16 & 0xFF);
        int g = (int)MathHelper.lerp(f, c1 >> 8 & 0xFF, c2 >> 8 & 0xFF);
        int b = (int)MathHelper.lerp(f, c1 & 0xFF, c2 & 0xFF);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
