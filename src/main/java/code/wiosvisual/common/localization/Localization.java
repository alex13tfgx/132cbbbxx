/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.reflect.TypeToken
 *  net.minecraft.class_2960
 *  net.minecraft.class_3298
 */
package code.wiosvisual.common.localization;

import code.wiosvisual.common.localization.Language;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public class Localization
implements QuickImports {
    private static final Map<Language, Map<String, String>> cache = new ConcurrentHashMap<Language, Map<String, String>>();

    public static String get(String key) {
        Map<String, String> translations = cache.computeIfAbsent(Language.ENG, Localization::loadTranslations);
        return translations.getOrDefault(key, key);
    }

    private static Map<String, String> loadTranslations(Language language) {
        try {
            Identifier identifier = Identifier.of((String)("translations/" + language.getFile() + ".json"));
            InputStream stream = ((Resource)mc.getResourceManager().getResource(identifier).get()).getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return (Map)gson.fromJson((Reader)reader, new TypeToken<Map<String, String>>(){}.getType());
        }
        catch (Exception exception) {
            return new ConcurrentHashMap<String, String>();
        }
    }
}
