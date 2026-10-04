/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.display.render.font;

import code.wiosvisual.utils.display.atlasfont.msdf.MsdfFont;
import java.util.HashMap;
import java.util.Map;

public final class MsdfFontLibrary {
    private static final Map<String, MsdfFont> CACHE = new HashMap<String, MsdfFont>();

    private MsdfFontLibrary() {
    }

    public static MsdfFont get(String resourceName) {
        return CACHE.computeIfAbsent(resourceName, MsdfFontLibrary::load);
    }

    public static MsdfFont resolveType(String typeName) {
        String msdfName = MsdfFontLibrary.mapTypeToMsdf(typeName);
        try {
            return MsdfFontLibrary.get(msdfName);
        }
        catch (RuntimeException primary) {
            for (String fallback : new String[]{"medium", "regular", "bold"}) {
                if (fallback.equals(msdfName)) continue;
                try {
                    return MsdfFontLibrary.get(fallback);
                }
                catch (RuntimeException runtimeException) {
                    // empty catch block
                }
            }
            throw primary;
        }
    }

    private static String mapTypeToMsdf(String typeName) {
        return switch (typeName) {
            case "sf_medium" -> "regular_medium";
            case "sf_regular" -> "regular";
            case "sf_bold" -> "bold";
            case "sf_semibold" -> "regular_semibold";
            case "SuisseIntl-Medium" -> "suisseintlmedium";
            case "SuisseIntl-Regular" -> "suisseintlregular";
            case "suisseintl-semibold", "suisseintlsemibold" -> "suisseintlsemibold";
            case "icons" -> "icons";
            case "clienticon1" -> "clienticon1";
            case "icon2" -> "icolist";
            case "guiicons" -> "clickgui";
            case "wiosvisualicons" -> "wiosvisualicons";
            case "icon" -> "iconmain";
            case "categoryicons" -> "icolist";
            case "waypoint_icons" -> "xentrixicon1";
            default -> typeName;
        };
    }

    private static MsdfFont load(String name) {
        return MsdfFont.builder().name(name).data(name).atlas(name).build();
    }

    public static void clear() {
        CACHE.clear();
    }
}
