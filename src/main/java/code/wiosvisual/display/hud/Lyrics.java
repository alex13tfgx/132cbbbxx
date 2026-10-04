package code.wiosvisual.display.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Синхронизированные тексты песен для караоке в Music Info. Берутся с открытого
 * сервиса LRCLIB по исполнителю и названию трека; результат кэшируется на сессию.
 * Названия из браузера (YouTube, VK) перед поиском очищаются от мусора.
 */
final class Lyrics {
    record Line(long timeMs, String text) {
    }

    private static final Logger LOGGER = LogManager.getLogger("Wiosvisual");
    private static final Pattern TIMESTAMP = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");
    /** После сетевой ошибки повторяем запрос не раньше чем через это время. */
    private static final long RETRY_MS = 30000L;

    /** Скобки с пометками вроде «(Official Video)», «[Lyrics]», «(Премьера клипа, 2024)». */
    private static final Pattern NOISE = Pattern.compile(
            "\\s*[(\\[\u3010][^)\\]\u3011]*(official|video|audio|lyric|visuali[sz]er|clip|m/v|\\bmv\\b|\\bhd\\b|4k|\\bhq\\b|remaster|премьер|клип|текст|альбом)[^)\\]\u3011]*[)\\]\u3011]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FEAT = Pattern.compile("\\s+\\(?(ft\\.?|feat\\.?|featuring)\\s+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DASH = Pattern.compile("\\s+[-\u2013\u2014|]\\s+");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final Map<String, List<Line>> cache = new ConcurrentHashMap<String, List<Line>>();
    private final Map<String, Long> retryAt = new ConcurrentHashMap<String, Long>();

    /** Строки текста или null, пока текст грузится. Пустой список — текста у трека нет. */
    List<Line> get(String rawArtist, String rawTitle, long durationSec) {
        String[] clean = Lyrics.normalize(rawArtist, rawTitle);
        String artist = clean[0];
        String title = clean[1];
        if (title.isEmpty()) {
            return List.of();
        }
        String key = artist + " | " + title;
        List<Line> lines = this.cache.get(key);
        if (lines != null) {
            return lines;
        }
        long now = System.currentTimeMillis();
        Long next = this.retryAt.get(key);
        if (next == null || now >= next) {
            // Метка ставится сразу: пока идёт запрос, повторно его не шлём.
            this.retryAt.put(key, now + RETRY_MS);
            this.fetch(key, artist, title, durationSec);
        }
        return null;
    }

    /**
     * Сначала точный поиск по исполнителю и названию; если не нашлось (частый случай для
     * YouTube и VK, где исполнитель — это канал или пусто) — общий поиск по строке.
     */
    private void fetch(String key, String artist, String title, long durationSec) {
        String exact = artist.isEmpty() ? null
                : "https://lrclib.net/api/search?track_name=" + encode(title) + "&artist_name=" + encode(artist);
        String loose = "https://lrclib.net/api/search?q=" + encode((artist + " " + title).trim());
        this.request(exact != null ? exact : loose, durationSec).thenCompose(lines -> {
            if (lines != null && lines.isEmpty() && exact != null) {
                return this.request(loose, durationSec);
            }
            return CompletableFuture.completedFuture(lines);
        }).thenAccept(lines -> {
            // null — сетевая ошибка: не кэшируем, повторим позже.
            if (lines != null) {
                this.cache.put(key, lines);
                LOGGER.info("[Karaoke] {}: {} lines", key, lines.size());
            }
        });
    }

    private CompletableFuture<List<Line>> request(String url, long durationSec) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Wiosvisual MusicInfo")
                .GET().build();
        return this.http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> {
            if (response.statusCode() == 404) {
                return List.<Line>of();
            }
            if (response.statusCode() != 200) {
                LOGGER.warn("[Karaoke] LRCLIB answered {}", response.statusCode());
                return null;
            }
            return pick(response.body(), durationSec);
        }).exceptionally(error -> {
            LOGGER.warn("[Karaoke] LRCLIB request failed: {}", error.toString());
            return null;
        });
    }

    /** Из результатов поиска берём синхронизированный текст с самой близкой длительностью. */
    private static List<Line> pick(String body, long durationSec) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonArray()) {
                return List.of();
            }
            JsonObject best = null;
            double bestDelta = Double.MAX_VALUE;
            for (JsonElement element : root.getAsJsonArray()) {
                JsonObject item = element.getAsJsonObject();
                if (!item.has("syncedLyrics") || item.get("syncedLyrics").isJsonNull()) continue;
                double duration = item.has("duration") && !item.get("duration").isJsonNull() ? item.get("duration").getAsDouble() : 0.0;
                double delta = durationSec > 0L && duration > 0.0 ? Math.abs(duration - (double)durationSec) : 0.0;
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = item;
                }
            }
            return best == null ? List.of() : parse(best.get("syncedLyrics").getAsString());
        } catch (Exception error) {
            return List.of();
        }
    }

    /**
     * Приводит «грязные» названия из браузера к виду для поиска текста:
     * «Artist - Song (Official Video)» на канале «ArtistVEVO» → «Artist», «Song».
     */
    static String[] normalize(String artist, String title) {
        String a = artist == null ? "" : artist.trim();
        String t = title == null ? "" : title.trim();
        t = NOISE.matcher(t).replaceAll("").trim();
        String[] parts = DASH.split(t, 2);
        if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
            // В заголовке уже есть исполнитель — канал или пустое поле ему не нужны.
            a = parts[0].trim();
            // Хвост вида «Song | Label» отбрасываем.
            t = DASH.split(parts[1].trim(), 2)[0].trim();
        }
        a = a.replaceAll("(?i)\\s*-\\s*topic$", "").replaceAll("(?i)vevo$", "").replaceAll("(?i)\\s*official$", "").trim();
        t = FEAT.matcher(t).replaceAll("").replaceAll("[\"\u00ab\u00bb]", "").trim();
        return new String[]{a, t};
    }

    /** Формат LRC: «[мм:сс.хх] строка», у строки может быть несколько меток времени. */
    static List<Line> parse(String lrc) {
        List<Line> lines = new ArrayList<Line>();
        for (String raw : lrc.split("\\r?\\n")) {
            Matcher matcher = TIMESTAMP.matcher(raw);
            List<Long> times = new ArrayList<Long>();
            int textStart = 0;
            while (matcher.find() && matcher.start() == textStart) {
                long minutes = Long.parseLong(matcher.group(1));
                double seconds = Double.parseDouble(matcher.group(2));
                times.add(minutes * 60000L + Math.round(seconds * 1000.0));
                textStart = matcher.end();
            }
            String text = raw.substring(textStart).trim();
            for (long time : times) {
                lines.add(new Line(time, text.isEmpty() ? "\u266a" : text));
            }
        }
        lines.sort(Comparator.comparingLong(Line::timeMs));
        return List.copyOf(lines);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
