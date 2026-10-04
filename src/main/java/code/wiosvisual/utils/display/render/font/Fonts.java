/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 */
package code.wiosvisual.utils.display.render.font;

import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.MsdfFontLibrary;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;

public class Fonts {
    private static final Map<FontKey, FontRenderer> fontCache = new HashMap<FontKey, FontRenderer>();
    /**
     * Быстрый путь без аллокации ключа: шрифт запрашивается сотни раз за кадр,
     * а набор пар «размер + начертание» один и тот же. При смене GUI Scale сбрасывается.
     */
    private static final Map<Type, Map<Integer, FontRenderer>> quickCache = new EnumMap<Type, Map<Integer, FontRenderer>>(Type.class);
    private static float quickCacheScale = Float.NaN;

    public static void init() {
        Fonts.clearCache();
    }

    public static void clearCache() {
        fontCache.clear();
        quickCache.clear();
        quickCacheScale = Float.NaN;
        MsdfFontLibrary.clear();
    }

    public static FontRenderer getSize(int size) {
        return Fonts.getSize(size, Type.BOLD);
    }

    public static FontRenderer getSize(int size, Type type) {
        float guiScale = Fonts.currentGuiScale();
        if (guiScale != quickCacheScale) {
            quickCache.clear();
            quickCacheScale = guiScale;
        }
        Map<Integer, FontRenderer> bySize = quickCache.get(type);
        if (bySize == null) {
            bySize = new HashMap<Integer, FontRenderer>();
            quickCache.put(type, bySize);
        }
        FontRenderer cached = bySize.get(size);
        if (cached != null) {
            return cached;
        }
        FontRenderer created = fontCache.computeIfAbsent(new FontKey(size, type, guiScale), k -> Fonts.create(size, type.getType()));
        bySize.put(size, created);
        return created;
    }

    private static float currentGuiScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return 8.0f;
        }
        return Math.max(4.0f, (float)client.getWindow().getScaleFactor()) * 1.5f;
    }

    public static FontRenderer create(float size, String name) {
        return new FontRenderer(name, size / 2.0f);
    }

    public static enum Type {
        DEFAULT("sf_medium"),
        REGULAR("sf_regular"),
        SEMI("sf_semibold"),
        BOLD("sf_bold"),
        ROCKSTAR_REGULAR("SuisseIntl-Regular"),
        ROCKSTAR_MEDIUM("SuisseIntl-Medium"),
        ROCKSTAR_SEMIBOLD("suisseintl-semibold"),
        ICONS("icons"),
        ICONS2("clienticon1"),
        ICONSTYPENEW("icon2"),
        GUIICONS("guiicons"),
        WIOSVISUAL_ICONS("wiosvisualicons"),
        INTER_MEDIUM("icon"),
        SuisseIntlMedium("SuisseIntl-Medium"),
        SuisseIntlRegular("SuisseIntl-Regular"),
        SuisseIntlSemiBold("suisseintl-semibold"),
        ICONSCATEGORY("categoryicons"),
        WAYPOINT_ICONS("waypoint_icons");

        private final String type;

        public String getType() {
            return this.type;
        }

        private Type(String type) {
            this.type = type;
        }
    }

    private record FontKey(int size, Type type, float guiScale) {
    }
}
