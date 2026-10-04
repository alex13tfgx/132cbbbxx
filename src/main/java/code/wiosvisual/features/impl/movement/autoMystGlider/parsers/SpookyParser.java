/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.class_266
 *  net.minecraft.class_269
 *  net.minecraft.class_337
 *  net.minecraft.class_345
 */
package code.wiosvisual.features.impl.movement.autoMystGlider.parsers;

import code.wiosvisual.utils.client.net.BackendConfig;

import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;

public final class SpookyParser
implements QuickImports {
    private static final int MIN_SECONDS = 120;
    private static int cachedAnarchy = -1;
    private static long cachedRemainingMs = 2000L;

    public static int parseAnarchy() {
        Scoreboard scoreboard = SpookyParser.mc.world.getScoreboard();
        if (scoreboard != null) {
            for (ScoreboardObjective objective : scoreboard.getObjectives()) {
                String string = objective.getDisplayName().getString();
                if (!string.contains("\u0410\u043d\u0430\u0440\u0445\u0438\u044f")) continue;
                String formattedString = string.replaceAll("\\D", "");
                try {
                    return Integer.parseInt(formattedString);
                }
                catch (NumberFormatException e) {
                    e.fillInStackTrace();
                }
            }
        }
        return -1;
    }

    public static boolean isAnarchy(int num) {
        return SpookyParser.parseAnarchy() == num;
    }

    public static boolean isAnarchy(String string) {
        int parsed;
        int cur = SpookyParser.parseAnarchy();
        if (cur == -1) {
            return false;
        }
        try {
            parsed = Integer.parseInt(string);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
        return cur == parsed;
    }

    public static long parseRemainingTime() {
        BossBarHud bossBarHud = SpookyParser.mc.inGameHud.getBossBarHud();
        if (bossBarHud != null) {
            for (Map.Entry entry : bossBarHud.bossBars.entrySet()) {
                ClientBossBar clientBossBar = (ClientBossBar)entry.getValue();
                String time = clientBossBar.getName().getString().replaceAll("\\D", "");
                if (time.isBlank()) continue;
                try {
                    int seconds = Integer.parseInt(time);
                    return Math.max((long)(seconds - 2) * 1000L, 1000L);
                }
                catch (NumberFormatException e) {
                    e.fillInStackTrace();
                }
            }
        }
        return 2000L;
    }

    public static void fetchFromApi() {
        if (BackendConfig.eventsUrl().isEmpty()) {
            return;
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BackendConfig.eventsUrl())).timeout(Duration.ofSeconds(5L)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString((String)response.body()).getAsJsonObject();
                JsonArray events = json.getAsJsonArray("events");
                JsonObject bestEvent = null;
                int bestSeconds = Integer.MAX_VALUE;
                for (JsonElement element : events) {
                    JsonObject event = element.getAsJsonObject();
                    int timeSeconds = event.get("time_seconds").getAsInt();
                    if (timeSeconds < 120 || timeSeconds >= bestSeconds) continue;
                    bestSeconds = timeSeconds;
                    bestEvent = event;
                }
                if (bestEvent != null) {
                    cachedAnarchy = bestEvent.get("anarchy_number").getAsInt();
                    cachedRemainingMs = Math.max((long)(bestSeconds - 2) * 1000L, 1000L);
                    ChatMessage.brandmessage("API: \u0410\u043d\u0430\u0440\u0445\u0438\u044f " + cachedAnarchy + ", \u0438\u0432\u0435\u043d\u0442 \u0447\u0435\u0440\u0435\u0437 " + bestSeconds + " \u0441\u0435\u043a");
                } else {
                    cachedAnarchy = -1;
                    cachedRemainingMs = 30000L;
                    ChatMessage.brandmessage("API: \u041d\u0435\u0442 \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0438\u0445 \u0438\u0432\u0435\u043d\u0442\u043e\u0432 (>= 2 \u043c\u0438\u043d)");
                }
            }
        }
        catch (Exception e) {
            ChatMessage.brandmessage("API: \u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u0438\u0432\u0435\u043d\u0442\u043e\u0432: " + e.getMessage());
            cachedAnarchy = -1;
            cachedRemainingMs = 30000L;
        }
    }

    public static int getCachedAnarchy() {
        return cachedAnarchy;
    }

    public static long getCachedRemainingMs() {
        return cachedRemainingMs;
    }

    private SpookyParser() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
