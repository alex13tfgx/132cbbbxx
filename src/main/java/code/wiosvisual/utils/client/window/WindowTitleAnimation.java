/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.window;


public class WindowTitleAnimation {
    private static WindowTitleAnimation INSTANCE;
    private static final String TITLE = "WiosVisuals 1.21.11 " + "";



    private WindowTitleAnimation() {
    }

    public static WindowTitleAnimation getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WindowTitleAnimation();
        }
        return INSTANCE;
    }

    public void updateTitle() {
    }

    public String getCurrentTitle() {
        return TITLE;
    }
}
