package ru.mentions.module.api;

import code.wiosvisual.features.module.ModuleCategory;

/** Категории mentions, сопоставленные с категориями Wios. */
public enum Category {
    COMBAT(ModuleCategory.MISC),
    MOVEMENT(ModuleCategory.PLAYER),
    PLAYER(ModuleCategory.PLAYER),
    OTHER(ModuleCategory.MISC),
    RENDER(ModuleCategory.RENDER);

    public final ModuleCategory wios;

    Category(ModuleCategory wios) {
        this.wios = wios;
    }
}
