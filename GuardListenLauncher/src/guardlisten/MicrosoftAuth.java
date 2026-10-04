package guardlisten;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * Вход через Microsoft для Minecraft: Java Edition (как у официального лаунчера).
 *
 * Цепочка: Microsoft OAuth (device code) → Xbox Live → XSTS → Minecraft Services →
 * профиль (есть только у владельцев игры). Нужен Client ID собственного приложения
 * Azure, одобренного Mojang для доступа к api.minecraftservices.com.
 */
final class MicrosoftAuth {
    /** Сессия для запуска игры. */
    static final class Session {
        final String name, uuid, token, xuid;
        Session(String name, String uuid, String token, String xuid) {
            this.name = name; this.uuid = uuid; this.token = token; this.xuid = xuid;
        }
    }

    /** Код для ввода на странице Microsoft. */
    static final class DeviceCode {
        final String deviceCode, userCode, url;
        final int interval, expiresIn;
        DeviceCode(String deviceCode, String userCode, String url, int interval, int expiresIn) {
            this.deviceCode = deviceCode; this.userCode = userCode; this.url = url;
            this.interval = interval; this.expiresIn = expiresIn;
        }
    }

    /** Ошибка входа с понятным пользователю текстом. */
    static final class AuthException extends Exception {
        AuthException(String msg) { super(msg); }
    }

    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final String MS = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private final String clientId;
    private final Path file;
    private Map<String, Object> data = new LinkedHashMap<>();

    MicrosoftAuth(String clientId, Path root) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.file = root.resolve("msa.json");
        try {
            if (Files.exists(file)) data = Json.obj(Json.parse(Files.readString(file)));
        } catch (Exception ignored) { }
    }

    /** Вход через Microsoft включён (прописан Client ID) — без него игра не запустится. */
    boolean configured() { return !clientId.isEmpty(); }

    /** Привязан ли аккаунт (есть refresh-токен и профиль). */
    boolean linked() { return Json.str(data, "refresh") != null && Json.str(data, "name") != null; }

    String name() { return Json.str(data, "name"); }

    void logout() {
        data = new LinkedHashMap<>();
        try { Files.deleteIfExists(file); } catch (IOException ignored) { }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, Json.write(data));
        } catch (IOException ignored) { }
    }

    // ---------------- шаг 1: код устройства ----------------
    DeviceCode requestCode() throws Exception {
        Map<String, Object> r = postForm(MS + "devicecode", Map.of("client_id", clientId, "scope", SCOPE));
        if (Json.str(r, "device_code") == null) throw new AuthException(msError(r, "Microsoft не выдал код входа"));
        String url = Json.str(r, "verification_uri") != null ? Json.str(r, "verification_uri") : "https://www.microsoft.com/link";
        return new DeviceCode(Json.str(r, "device_code"), Json.str(r, "user_code"), url,
                (int) Math.max(1, Json.num(r, "interval")), (int) Math.max(60, Json.num(r, "expires_in")));
    }

    /**
     * Ждёт, пока пользователь введёт код на странице Microsoft, затем проходит всю цепочку.
     * @param cancelled проверяется между опросами — для кнопки «Отмена»
     */
    Session waitForLogin(DeviceCode dc, java.util.function.BooleanSupplier cancelled) throws Exception {
        long deadline = System.currentTimeMillis() + dc.expiresIn * 1000L;
        int interval = dc.interval;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);
            if (cancelled.getAsBoolean()) throw new AuthException("Вход отменён");
            Map<String, Object> r = postForm(MS + "token", Map.of(
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id", clientId,
                    "device_code", dc.deviceCode));
            String err = Json.str(r, "error");
            if (err == null) return finish(r);
            switch (err) {
                case "authorization_pending": continue;
                case "slow_down": interval += 5; continue;
                case "authorization_declined": throw new AuthException("Вход отклонён в Microsoft");
                case "expired_token": throw new AuthException("Код истёк — попробуйте ещё раз");
                default: throw new AuthException(msError(r, "Ошибка Microsoft: " + err));
            }
        }
        throw new AuthException("Код истёк — попробуйте ещё раз");
    }

    /**
     * Сессия для запуска: токен Minecraft живёт ~24 ч, поэтому при необходимости
     * тихо обновляется по refresh-токену, без повторного ввода кода.
     */
    Session session() throws Exception {
        if (!linked()) throw new AuthException("Войдите через Microsoft в разделе «Аккаунт»");
        long exp = (long) Json.num(data, "mcExpires");
        if (Json.str(data, "mcToken") != null && System.currentTimeMillis() < exp - 10 * 60_000L) {
            return new Session(name(), Json.str(data, "uuid"), Json.str(data, "mcToken"), Json.str(data, "xuid"));
        }
        Map<String, Object> r = postForm(MS + "token", Map.of(
                "grant_type", "refresh_token",
                "client_id", clientId,
                "refresh_token", Json.str(data, "refresh"),
                "scope", SCOPE));
        if (Json.str(r, "access_token") == null) {
            logout();
            throw new AuthException("Сессия Microsoft истекла — войдите заново в разделе «Аккаунт»");
        }
        return finish(r);
    }

    // ---------------- шаги 2–5: Xbox → XSTS → Minecraft → профиль ----------------
    private Session finish(Map<String, Object> msToken) throws Exception {
        String msAccess = Json.str(msToken, "access_token");
        String refresh = Json.str(msToken, "refresh_token");

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("AuthMethod", "RPS");
        props.put("SiteName", "user.auth.xboxlive.com");
        props.put("RpsTicket", "d=" + msAccess);
        Map<String, Object> xblReq = new LinkedHashMap<>();
        xblReq.put("Properties", props);
        xblReq.put("RelyingParty", "http://auth.xboxlive.com");
        xblReq.put("TokenType", "JWT");
        Resp xbl = postJson("https://user.auth.xboxlive.com/user/authenticate", xblReq, null);
        String xblToken = Json.str(xbl.json, "Token");
        if (xblToken == null) throw new AuthException("Xbox Live не принял вход (код " + xbl.status + ")");

        Map<String, Object> xprops = new LinkedHashMap<>();
        xprops.put("SandboxId", "RETAIL");
        xprops.put("UserTokens", List.of(xblToken));
        Map<String, Object> xstsReq = new LinkedHashMap<>();
        xstsReq.put("Properties", xprops);
        xstsReq.put("RelyingParty", "rp://api.minecraftservices.com/");
        xstsReq.put("TokenType", "JWT");
        Resp xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsReq, null);
        String xstsToken = Json.str(xsts.json, "Token");
        if (xstsToken == null) throw new AuthException(xstsError(xsts));
        Map<String, Object> xui = Json.obj(Json.list(Json.obj(xsts.json.get("DisplayClaims")).get("xui")).isEmpty() ? null
                : Json.list(Json.obj(xsts.json.get("DisplayClaims")).get("xui")).get(0));
        String uhs = Json.str(xui, "uhs");
        String xuid = Json.str(xui, "xid") != null ? Json.str(xui, "xid") : "0";

        Map<String, Object> mcReq = new LinkedHashMap<>();
        mcReq.put("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        Resp mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcReq, null);
        String mcToken = Json.str(mc.json, "access_token");
        if (mcToken == null) {
            if (mc.status == 403) throw new AuthException("Приложение лаунчера ещё не одобрено Mojang для входа в Minecraft");
            throw new AuthException("Minecraft не принял вход (код " + mc.status + ")");
        }

        Resp prof = get("https://api.minecraftservices.com/minecraft/profile", mcToken);
        if (prof.status == 404 || Json.str(prof.json, "name") == null)
            throw new AuthException("На этом аккаунте нет Minecraft: Java Edition");

        String oldRefresh = Json.str(data, "refresh");
        data = new LinkedHashMap<>();
        data.put("refresh", refresh != null ? refresh : oldRefresh);
        data.put("name", Json.str(prof.json, "name"));
        data.put("uuid", Json.str(prof.json, "id"));
        data.put("xuid", xuid);
        data.put("mcToken", mcToken);
        data.put("mcExpires", System.currentTimeMillis() + Json.num(mc.json, "expires_in") * 1000L);
        save();
        return new Session(Json.str(prof.json, "name"), Json.str(prof.json, "id"), mcToken, xuid);
    }

    private static String xstsError(Resp r) {
        long code = Json.num(r.json, "XErr");
        if (code == 2148916233L) return "У аккаунта Microsoft нет профиля Xbox — создайте его на xbox.com и повторите";
        if (code == 2148916235L) return "Xbox Live недоступен в вашей стране";
        if (code == 2148916236L || code == 2148916237L) return "Аккаунту нужна проверка возраста на xbox.com";
        if (code == 2148916238L) return "Детский аккаунт: нужно добавить его в семью Microsoft";
        return "Xbox Live отказал в доступе (код " + (code != 0 ? code : r.status) + ")";
    }

    private static String msError(Map<String, Object> r, String fallback) {
        String d = Json.str(r, "error_description");
        if (d != null && d.contains("AADSTS700016")) return "Неверный Client ID приложения Microsoft";
        if (d != null && d.contains("AADSTS7000218")) return "В приложении Azure не включены public client flows";
        return fallback;
    }

    // ---------------- HTTP ----------------
    private static final class Resp {
        final int status; final Map<String, Object> json;
        Resp(int status, Map<String, Object> json) { this.status = status; this.json = json; }
    }

    private static Map<String, Object> postForm(String url, Map<String, String> form) throws IOException, InterruptedException {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (b.length() > 0) b.append('&');
            b.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(b.toString())).build(), HttpResponse.BodyHandlers.ofString());
        return parse(r.body());
    }

    private static Resp postJson(String url, Object body, String bearer) throws IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)));
        if (bearer != null) rb.header("Authorization", "Bearer " + bearer);
        HttpResponse<String> r = HTTP.send(rb.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), parse(r.body()));
    }

    private static Resp get(String url, String bearer) throws IOException, InterruptedException {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + bearer)
                .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), parse(r.body()));
    }

    private static Map<String, Object> parse(String body) {
        try { return Json.obj(Json.parse(body == null || body.isBlank() ? "{}" : body.trim())); }
        catch (Exception e) { return new LinkedHashMap<>(); }
    }
}
