/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.reflect.TypeToken
 *  net.fabricmc.loader.api.FabricLoader
 */
package code.wiosvisual.common.proxy;

import code.wiosvisual.common.proxy.Proxy;
import code.wiosvisual.common.proxy.ProxyServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ProxyServerConfig.json");
    public static HashMap<String, Proxy> accounts = new HashMap();
    public static String lastPlayerName = "";

    public static void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH, new LinkOption[0])) {
                Path parent = CONFIG_PATH.getParent();
                if (parent != null) {
                    Files.createDirectories(parent, new FileAttribute[0]);
                }
                Files.createFile(CONFIG_PATH, new FileAttribute[0]);
                return;
            }
            String configString = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            if (configString.isEmpty()) {
                return;
            }
            JsonObject configJson = JsonParser.parseString((String)configString).getAsJsonObject();
            if (configJson.has("proxy-enabled")) {
                ProxyServer.proxyEnabled = configJson.get("proxy-enabled").getAsBoolean();
            }
            Type type = new TypeToken<HashMap<String, Proxy>>(){}.getType();
            accounts = (HashMap)new Gson().fromJson(configJson.get("accounts"), type);
            if (accounts == null) {
                accounts = new HashMap();
            }
            if (configJson.has("current-proxy") && (ProxyServer.proxy = (Proxy)new Gson().fromJson(configJson.get("current-proxy"), Proxy.class)) == null) {
                ProxyServer.proxy = new Proxy();
            }
            if (configJson.has("last-used-proxy") && (ProxyServer.lastUsedProxy = (Proxy)new Gson().fromJson(configJson.get("last-used-proxy"), Proxy.class)) == null) {
                ProxyServer.lastUsedProxy = new Proxy();
            }
            if (configJson.has("last-player-name")) {
                lastPlayerName = configJson.get("last-player-name").getAsString();
            }
        }
        catch (Exception e) {
            System.out.println("Error reading ProxyServerConfig.json file");
            e.printStackTrace();
        }
    }

    public static void setDefaultProxy(Proxy proxy) {
        accounts.put("", proxy);
    }

    public static void saveConfig() {
        try {
            Gson gson = new Gson();
            JsonElement accountsJsonObject = gson.toJsonTree(accounts);
            JsonObject configJson = new JsonObject();
            configJson.addProperty("proxy-enabled", Boolean.valueOf(ProxyServer.proxyEnabled));
            configJson.add("accounts", accountsJsonObject);
            configJson.add("current-proxy", gson.toJsonTree((Object)ProxyServer.proxy));
            configJson.add("last-used-proxy", gson.toJsonTree((Object)ProxyServer.lastUsedProxy));
            configJson.addProperty("last-player-name", lastPlayerName);
            Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(CONFIG_PATH, (CharSequence)gsonPretty.toJson((JsonElement)configJson), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            System.out.println("Error writing ProxyServerConfig.json file");
            e.printStackTrace();
        }
    }
}
