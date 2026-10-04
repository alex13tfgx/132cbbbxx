/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.module;

public enum ModuleCategory {
    RENDER("Visuals"),
    PLAYER("Player"),
    MISC("Utilities"),
    CUSTOMIZATION("Customization"),
    COSMETICS("Cosmetics"),
    CONFIGS("Конфигурации");

    final String readableName;

    private ModuleCategory(String readableName) {
        this.readableName = readableName;
    }

    public String getReadableName() {
        return this.readableName;
    }
}
