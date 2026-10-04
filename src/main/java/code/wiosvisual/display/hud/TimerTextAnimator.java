/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4587
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.util.math.MatrixStack;

public final class TimerTextAnimator {
    private static final long ANIM_MS = 170L;
    private static final float SLIDE_PX = 5.0f;
    private static final Map<String, Entry> ENTRIES = new HashMap<String, Entry>();
    private static final ThreadLocal<char[]> CHAR_BUF = ThreadLocal.withInitial(() -> new char[1]);

    private TimerTextAnimator() {
    }

    private static String singleCharString(char c) {
        char[] buf = CHAR_BUF.get();
        buf[0] = c;
        return new String(buf);
    }

    public static float getAnimatedWidth(FontRenderer font, String key, String text) {
        String current;
        Entry entry = ENTRIES.get(key);
        String previous = entry != null ? entry.previous : text;
        String string = current = entry != null ? entry.current : text;
        if (previous.length() != current.length()) {
            return font.getStringWidth(current);
        }
        int maxLen = Math.max(previous.length(), current.length());
        float width = 0.0f;
        for (int i = 0; i < maxLen; ++i) {
            char oldChar = i < previous.length() ? previous.charAt(i) : (char)'\u0000';
            char newChar = i < current.length() ? current.charAt(i) : (char)'\u0000';
            float oldW = oldChar == '\u0000' ? 0.0f : font.getStringWidth(TimerTextAnimator.singleCharString(oldChar));
            float newW = newChar == '\u0000' ? 0.0f : font.getStringWidth(TimerTextAnimator.singleCharString(newChar));
            width += Math.max(oldW, newW);
        }
        return width;
    }

    public static void draw(FontRenderer font, MatrixStack matrix, String key, String text, float x, float y, int color) {
        long now = System.currentTimeMillis();
        Entry entry = ENTRIES.computeIfAbsent(key, k -> new Entry(text));
        if (!text.equals(entry.current)) {
            entry.previous = text.length() != entry.current.length() ? text : entry.current;
            entry.current = text;
            entry.changedAt = now;
        }
        entry.lastUse = now;
        TimerTextAnimator.cleanup(now);
        String previous = entry.previous;
        String current = entry.current;
        if (previous.length() != current.length()) {
            font.drawString(matrix, current, x, y, color);
            return;
        }
        float progress = Math.min(1.0f, (float)(now - entry.changedAt) / 170.0f);
        if (progress >= 1.0f) {
            font.drawString(matrix, current, x, y, color);
            return;
        }
        int baseAlpha = color >>> 24 & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }
        int maxLen = Math.max(previous.length(), current.length());
        float cursor = x;
        for (int i = 0; i < maxLen; ++i) {
            boolean animate;
            char oldChar = i < previous.length() ? previous.charAt(i) : (char)'\u0000';
            char newChar = i < current.length() ? current.charAt(i) : (char)'\u0000';
            float oldW = oldChar == '\u0000' ? 0.0f : font.getStringWidth(TimerTextAnimator.singleCharString(oldChar));
            float newW = newChar == '\u0000' ? 0.0f : font.getStringWidth(TimerTextAnimator.singleCharString(newChar));
            float charW = Math.max(oldW, newW);
            boolean bl = animate = Character.isDigit(oldChar) && Character.isDigit(newChar) && oldChar != newChar && progress < 1.0f;
            if (animate) {
                int oldAlpha = (int)((float)baseAlpha * (1.0f - progress));
                int newAlpha = (int)((float)baseAlpha * progress);
                font.drawString(matrix, TimerTextAnimator.singleCharString(oldChar), cursor, y - progress * 5.0f, ColorAssist.replAlpha(color, oldAlpha));
                font.drawString(matrix, TimerTextAnimator.singleCharString(newChar), cursor, y + (1.0f - progress) * 5.0f, ColorAssist.replAlpha(color, newAlpha));
            } else if (newChar != '\u0000') {
                font.drawString(matrix, TimerTextAnimator.singleCharString(newChar), cursor, y, color);
            }
            cursor += charW;
        }
    }

    private static void cleanup(long now) {
        if (ENTRIES.size() < 512) {
            return;
        }
        ENTRIES.entrySet().removeIf(e -> now - ((Entry)e.getValue()).lastUse > 10000L);
    }

    private static final class Entry {
        private String previous;
        private String current;
        private long changedAt;
        private long lastUse;

        private Entry(String text) {
            this.previous = text;
            this.current = text;
            this.lastUse = this.changedAt = System.currentTimeMillis();
        }
    }
}
