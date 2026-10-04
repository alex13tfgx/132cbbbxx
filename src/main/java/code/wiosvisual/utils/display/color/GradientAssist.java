/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_5250
 */
package code.wiosvisual.utils.display.color;

import code.wiosvisual.utils.display.color.ColorAssist;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GradientAssist {
    public static int[] getGradientColors(String itemName) {
        switch (itemName) {
            case "\u0414\u0435\u0437\u043e\u0440\u0438\u0435\u043d\u0442\u0430\u0446\u0438\u044f": {
                return new int[]{ColorAssist.toColor("#00FF6A"), ColorAssist.toColor("#2761F5"), ColorAssist.toColor("#B80081")};
            }
            case "\u0411\u043e\u0436\u044c\u044f \u0430\u0443\u0440\u0430": {
                return new int[]{ColorAssist.toColor("#FBFF00"), ColorAssist.toColor("#FF9873"), ColorAssist.toColor("#FF9873")};
            }
            case "\u041f\u043b\u0430\u0441\u0442": {
                return new int[]{ColorAssist.toColor("#3B005E"), ColorAssist.toColor("#8900DB"), ColorAssist.toColor("#8900DB")};
            }
            case "\u0422\u0440\u0430\u043f\u043a\u0430": {
                return new int[]{ColorAssist.toColor("#8F0000"), ColorAssist.toColor("#DE0000"), ColorAssist.toColor("#FF0000")};
            }
            case "\u041e\u0433\u043d\u0435\u043d\u043d\u044b\u0439 \u0441\u043c\u0435\u0440\u0447": {
                return new int[]{ColorAssist.toColor("#ED0000"), ColorAssist.toColor("#FF682B"), ColorAssist.toColor("#FF682B")};
            }
            case "\u0421\u043d\u0435\u0436\u043e\u043a \u0437\u0430\u043c\u043e\u0440\u043e\u0437\u043a\u0430": {
                return new int[]{ColorAssist.toColor("#2BFFCE"), ColorAssist.toColor("#00E6B0"), ColorAssist.toColor("#00E6B0")};
            }
            case "\u042f\u0432\u043d\u0430\u044f \u043f\u044b\u043b\u044c": {
                return new int[]{ColorAssist.toColor("#00FFFA"), ColorAssist.toColor("#00FF95"), ColorAssist.toColor("#00FF95")};
            }
            case "\u0417\u0435\u043b\u044c\u0435 \u0433\u043d\u0435\u0432\u0430": {
                return new int[]{ColorAssist.toColor("#00799E"), ColorAssist.toColor("#00FFFF")};
            }
            case "\u0417\u0435\u043b\u044c\u0435 \u0440\u0430\u0434\u0438\u0430\u0446\u0438\u0438": {
                return new int[]{ColorAssist.toColor("#8A007D"), ColorAssist.toColor("#FF00E8")};
            }
            case "\u0417\u0435\u043b\u044c\u0435 \u0430\u0441\u0441\u0430\u0441\u0438\u043d\u0430": {
                return new int[]{ColorAssist.toColor("#D99A00"), ColorAssist.toColor("#FFEE00")};
            }
            case "\u0417\u0435\u043b\u044c\u0435 \u043f\u0430\u043b\u0430\u0434\u0438\u043d\u0430": {
                return new int[]{ColorAssist.toColor("#00821F"), ColorAssist.toColor("#00FF3D")};
            }
            case "\u0417\u0435\u043b\u044c\u0435 \u043a\u0438\u043b\u043b\u0435\u0440\u0430": {
                return new int[]{ColorAssist.toColor("#9C0000"), ColorAssist.toColor("#FF0F0F")};
            }
            case "\u0425\u043b\u043e\u043f\u0443\u0448\u043a\u0430": {
                return new int[]{ColorAssist.toColor("#CC4E00"), ColorAssist.toColor("#FFA100")};
            }
            case "\u0421\u0432\u044f\u0442\u0430\u044f \u0432\u043e\u0434\u0430": {
                return new int[]{ColorAssist.toColor("#319C00"), ColorAssist.toColor("#4AEB00")};
            }
            case "\u0421\u043d\u043e\u0442\u0432\u043e\u0440\u043d\u043e\u0435": {
                return new int[]{ColorAssist.toColor("#D48600"), ColorAssist.toColor("#FFE600")};
            }
        }
        return new int[]{ColorAssist.getText()};
    }

    public static MutableText applyGradientToText(String text, int[] colors, boolean addStarPrefix) {
        Object fullText;
        MutableText result = Text.empty();
        Object object = fullText = addStarPrefix ? "[\u2605] " + text : text;
        if (colors.length == 2 && addStarPrefix) {
            result.append((Text)Text.literal((String)"[\u2605] ").formatted(Formatting.RESET).styled(style -> style.withColor(colors[0])));
            result.append((Text)Text.literal((String)text).formatted(Formatting.RESET).styled(style -> style.withColor(colors[1])));
        } else if (colors.length == 0 || colors.length == 1 && colors[0] == ColorAssist.getText()) {
            result.append((Text)Text.literal((String)fullText).formatted(Formatting.RESET));
        } else {
            int length = ((String)fullText).length();
            int colorCount = colors.length;
            for (int i = 0; i < length; ++i) {
                float progress = (float)i / (float)(length - 1);
                int colorIndex = (int)(progress * (float)(colorCount - 1));
                int color1 = colors[colorIndex];
                int color2 = colors[Math.min(colorIndex + 1, colorCount - 1)];
                float segmentProgress = progress * (float)(colorCount - 1) - (float)colorIndex;
                int interpolatedColor = ColorAssist.getTrueRainbowColor(color1, color2);
                result.append((Text)Text.literal((String)String.valueOf(((String)fullText).charAt(i))).formatted(Formatting.RESET).styled(style -> style.withColor(interpolatedColor)));
            }
        }
        return result;
    }
}
