/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 */
package code.wiosvisual.utils.display.atlasfont.providers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ResourceProvider {
    private static final Gson GSON = new Gson();

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of((String)"mre", (String)("core/" + name));
    }

    public static JsonObject toJson(Identifier identifier) {
        return JsonParser.parseString((String)ResourceProvider.toString(identifier)).getAsJsonObject();
    }

    public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
        return (T)GSON.fromJson(ResourceProvider.toString(identifier), clazz);
    }

    public static String toString(Identifier identifier) {
        return ResourceProvider.toString(identifier, "\n");
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static String toString(Identifier identifier, String delimiter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            throw new IllegalStateException("MinecraftClient is not initialized yet for resource: " + String.valueOf(identifier));
        }
        ResourceManager resourceManager = client.getResourceManager();
        if (resourceManager == null) {
            throw new IllegalStateException("ResourceManager is not ready yet for resource: " + String.valueOf(identifier));
        }
        try {
            Optional resourceOpt = resourceManager.getResource(identifier);
            if (resourceOpt.isEmpty()) {
                throw new RuntimeException("Resource not found: " + String.valueOf(identifier) + "; Make sure the file exists in assets/" + identifier.getNamespace() + "/" + identifier.getPath());
            }
            try (InputStream inputStream = ((Resource)resourceOpt.get()).getInputStream();){
                String string;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));){
                    string = reader.lines().collect(Collectors.joining(delimiter));
                }
                return string;
            }
        }
        catch (IOException ex) {
            System.err.println("[ResourceProvider] CRITICAL: Failed to load resource: " + String.valueOf(identifier));
            ex.printStackTrace();
            throw new RuntimeException("Failed to load resource: " + String.valueOf(identifier), ex);
        }
    }
}
