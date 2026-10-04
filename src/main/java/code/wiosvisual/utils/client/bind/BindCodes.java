/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.bind;

import code.wiosvisual.utils.client.chat.StringHelper;

public final class BindCodes {
    public static final int SCROLL_UP = 998;
    public static final int SCROLL_DOWN = 999;

    public static boolean isUnset(int key) {
        return key == -1 || key < 0;
    }

    public static boolean isMouse(int key) {
        return key >= 0 && key <= 7;
    }

    public static boolean isScroll(int key) {
        return key == 998 || key == 999;
    }

    public static boolean canBind(int key) {
        if (key == 256 || BindCodes.isUnset(key)) {
            return false;
        }
        return key != -1;
    }

    public static String getDisplayName(int key) {
        if (BindCodes.isUnset(key)) {
            return "\u041d\u0435 \u0437\u0430\u0434\u0430\u043d\u043e";
        }
        if (key == 998) {
            return "\u041a\u043e\u043b\u0451\u0441\u0438\u043a\u043e \u2191";
        }
        if (key == 999) {
            return "\u041a\u043e\u043b\u0451\u0441\u0438\u043a\u043e \u2193";
        }
        return StringHelper.getBindName(key);
    }

    private BindCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
