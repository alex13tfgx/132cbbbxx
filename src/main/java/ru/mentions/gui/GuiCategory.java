package ru.mentions.gui;

import code.wiosvisual.features.module.ModuleCategory;
import ru.mentions.utils.animation.satoshi.Animation;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;

/** Категории панели ClickGui mentions; соответствуют категориям модулей Wios. */
public enum GuiCategory {
    RENDER("Visuals", "Q", ModuleCategory.RENDER),
    PLAYER("Player", "N", ModuleCategory.PLAYER),
    MISC("Utilities", "L", ModuleCategory.MISC),
    CUSTOMIZATION("Customization", "H", ModuleCategory.CUSTOMIZATION),
    COSMETICS("Cosmetics", "B", ModuleCategory.COSMETICS);

    private final String title;
    private final String icon;
    private final ModuleCategory source;
    public final Animation alphaS = new EaseInOutQuad(300, 1);
    public final Animation alphaS2 = new EaseInOutQuad(300, 1);

    GuiCategory(String title, String icon, ModuleCategory source) {
        this.title = title;
        this.icon = icon;
        this.source = source;
    }

    public String getName() {
        return this.title;
    }

    public String getIcon() {
        return this.icon;
    }

    public static GuiCategory of(ModuleCategory category) {
        for (GuiCategory value : values()) {
            if (value.source == category) {
                return value;
            }
        }
        return MISC;
    }
}
