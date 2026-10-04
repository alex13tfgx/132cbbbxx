package code.wiosvisual.utils.client.net;

import code.wiosvisual.utils.client.logs.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Единственная точка, где клиент узнаёт адрес бэкенда. Ни одного домена в коде нет:
 * адрес берётся из (в порядке приоритета)
 *   1) системного свойства  -Dwiosvisual.backend.url=https://host
 *   2) переменной окружения WIOSVISUAL_BACKEND_URL
 *   3) файла Wiosvisual/files/backend.json  {"baseUrl": "...", "eventsUrl": "..."}
 * Если адрес не задан — все сетевые функции просто выключены, игра работает оффлайн.
 */
public final class BackendConfig {
    public static final String FILE_NAME = "backend.json";
    private static final String PROPERTY_KEY = "wiosvisual.backend.url";
    private static final String ENV_KEY = "WIOSVISUAL_BACKEND_URL";

    private static volatile boolean loaded;
    private static volatile String baseUrl = "";
    private static volatile String eventsUrl = "";

    private BackendConfig() {
    }

    public static synchronized void reload() {
        loaded = false;
        load();
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        String url = trimUrl(System.getProperty(PROPERTY_KEY));
        String events = "";
        if (url.isEmpty()) {
            url = trimUrl(System.getenv(ENV_KEY));
        }
        if (url.isEmpty()) {
            File file = configFile();
            if (file.isFile()) {
                try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    url = trimUrl(readString(json, "baseUrl"));
                    events = trimUrl(readString(json, "eventsUrl"));
                } catch (Exception e) {
                    Logger.warn("[BackendConfig] не удалось прочитать " + file + ": " + e.getMessage());
                }
            }
        }
        baseUrl = url;
        eventsUrl = events;
        if (baseUrl.isEmpty()) {
            Logger.info("[BackendConfig] адрес бэкенда не задан — онлайн, роли и чат отключены");
        } else {
            Logger.info("[BackendConfig] бэкенд: " + baseUrl);
        }
    }

    public static File configFile() {
        File dir = new File("Wiosvisual/files");
        try {
            code.wiosvisual.Wiosvisual client = code.wiosvisual.Wiosvisual.getInstance();
            if (client != null && client.getClientInfoProvider() != null) {
                dir = client.getClientInfoProvider().filesDir();
            }
        } catch (Throwable ignored) {
            // клиент ещё не поднялся — используем путь по умолчанию
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, FILE_NAME);
    }

    /** Базовый HTTP(S)-адрес без завершающего слэша, либо пустая строка. */
    public static String baseUrl() {
        load();
        return baseUrl;
    }

    /** Адрес websocket-канала, выведенный из базового: http -> ws, https -> wss. */
    public static String webSocketUrl() {
        String base = baseUrl();
        if (base.isEmpty()) {
            return "";
        }
        String lower = base.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://")) {
            return "wss://" + base.substring("https://".length()) + "/ws";
        }
        if (lower.startsWith("http://")) {
            return "ws://" + base.substring("http://".length()) + "/ws";
        }
        return base + "/ws";
    }

    /** Необязательный сторонний API ивентов; пусто — модуль работает по скорборду. */
    public static String eventsUrl() {
        load();
        return eventsUrl;
    }

    public static boolean isConfigured() {
        return !baseUrl().isEmpty();
    }

    public static String endpoint(String path) {
        String base = baseUrl();
        if (base.isEmpty()) {
            return "";
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    private static String readString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static String trimUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
