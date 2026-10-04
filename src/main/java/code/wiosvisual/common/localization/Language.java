/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.localization;

import java.util.HashMap;

public enum Language {
    ENG("en_US"),
    RUS("ru_RU");

    private final String file;
    private final HashMap<String, String> strings = new HashMap();

    public String getFile() {
        return this.file;
    }

    public HashMap<String, String> getStrings() {
        return this.strings;
    }

    private Language(String file) {
        this.file = file;
    }
}
