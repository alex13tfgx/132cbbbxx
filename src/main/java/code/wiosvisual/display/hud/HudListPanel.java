/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.font.FontRenderer;

public final class HudListPanel {
    public static final float HEADER_HEIGHT = 21.0f;
    public static final float ROW_HEIGHT = 11.0f;
    public static final float ROW_LEFT_PAD = 22.0f;
    public static final float ROW_RIGHT_PAD = 12.5f;
    public static final float ROW_CONTENT_GAP = 8.0f;
    public static final float HEADER_RIGHT_PAD = 14.0f;
    public static final float CONTENT_TOP_GAP = 0.0f;
    public static final float CONTENT_BOTTOM_PAD = 3.0f;
    public static final float LEVEL_NAME_GAP = 3.0f;

    private HudListPanel() {
    }

    public static float contentStartY(float panelY) {
        return panelY + 21.0f + 0.0f;
    }

    public static float panelHeight(float contentHeight) {
        return 21.0f + contentHeight + 3.0f;
    }

    public static float headerTextY(FontRenderer font, float panelY, String title) {
        return font.getCenteredY(panelY, 21.0f, title);
    }

    public static float rowTextY(FontRenderer font, float rowY) {
        return font.getCenteredY(rowY, 11.0f);
    }

    public static float rowTextY(FontRenderer font, float rowY, String text) {
        return font.getCenteredY(rowY, 11.0f, text);
    }

    public static float rowIconAlignedTextY(FontRenderer font, float rowY, float iconSize, String text) {
        return font.getCenteredY(HudListPanel.rowIconY(rowY, iconSize), iconSize, text);
    }

    public static float rowBadgeTextY(FontRenderer font, float rowY, float badgeHeight, String text) {
        return font.getCenteredY(HudListPanel.rowBadgeY(rowY, badgeHeight), badgeHeight, text);
    }

    public static float alignedRowTextY(FontRenderer referenceFont, FontRenderer font, float rowY) {
        return HudListPanel.alignedRowTextY(referenceFont, font, rowY, "A");
    }

    public static float alignedRowTextY(FontRenderer referenceFont, FontRenderer font, float rowY, String referenceText) {
        float referenceY = HudListPanel.rowIconAlignedTextY(referenceFont, rowY, 9.0f, referenceText);
        return referenceY + (referenceFont.getStringHeight(referenceText) - font.getStringHeight(referenceText)) / 2.0f;
    }

    public static float rowDotY(float rowY) {
        return rowY + 4.5f;
    }

    public static float rowBadgeY(float rowY, float badgeHeight) {
        return rowY + (11.0f - badgeHeight) / 2.0f;
    }

    public static float rowIconY(float rowY, float iconSize) {
        return rowY + (11.0f - iconSize) / 2.0f;
    }

    public static float rowWidth(FontRenderer nameFont, String name, float middleWidth, FontRenderer rightFont, String rightText) {
        return 22.0f + nameFont.getStringWidth(name) + middleWidth + 8.0f + rightFont.getStringWidth(rightText) + 12.5f;
    }

    public static float headerMinWidth(FontRenderer headerFont, String title) {
        return headerFont.getStringWidth(title) + 14.0f + 9.5f + 8.0f;
    }
}
