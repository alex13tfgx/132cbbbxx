/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package code.wiosvisual.utils.client.auth;

import code.wiosvisual.utils.client.net.BackendConfig;

import code.wiosvisual.utils.client.auth.TelegramAuthManager;
import code.wiosvisual.utils.client.logs.Logger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class TelegramAuthService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();

    private TelegramAuthService() {
    }

    public static SessionInitResponse createSession() throws IOException, InterruptedException {
        return TelegramAuthService.createSession("login");
    }

    public static SessionInitResponse createSession(String mode) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        String deviceId = TelegramAuthManager.getDeviceId();
        String hwid = TelegramAuthManager.getHardwareId();
        String clientName = "Wiosvisual";
        String clientVersion = "2.1.2";
        String authMode = mode == null || mode.isBlank() ? "login" : mode.toLowerCase();
        payload.addProperty("device_id", deviceId);
        payload.addProperty("hwid", hwid);
        payload.addProperty("auth_mode", authMode);
        payload.addProperty("client_name", clientName);
        payload.addProperty("client_version", clientVersion);
        Logger.info("[TelegramAuthService] createSession baseUrl=" + TelegramAuthService.getBaseUrl() + ", auth_mode=" + authMode + ", client=" + clientName + ", version=" + clientVersion);
        String query = "?device_id=" + TelegramAuthService.encode(deviceId) + "&hwid=" + TelegramAuthService.encode(hwid) + "&auth_mode=" + TelegramAuthService.encode(authMode) + "&client_name=" + TelegramAuthService.encode(clientName) + "&client_version=" + TelegramAuthService.encode(clientVersion);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(TelegramAuthService.requireBaseUrl() + "/api/client-auth/session" + query)).timeout(Duration.ofSeconds(10L)).header("Content-Type", "application/json").header("X-Device-Id", deviceId).header("X-HWID", hwid).POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        JsonObject json = TelegramAuthService.sendJson(request);
        String code = TelegramAuthService.getString(json, "code");
        String authUrl = TelegramAuthService.getString(json, "auth_url");
        if (code.isBlank() || authUrl.isBlank()) {
            throw new IOException("Session response does not contain code/auth_url: " + String.valueOf(json));
        }
        return new SessionInitResponse(code, authUrl, json.has("expires_in") ? json.get("expires_in").getAsLong() : 180L);
    }

    public static SessionStatusResponse fetchStatus(String code) throws IOException, InterruptedException {
        Logger.info("[TelegramAuthService] fetchStatus baseUrl=" + TelegramAuthService.getBaseUrl() + ", code=" + code);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(TelegramAuthService.requireBaseUrl() + "/api/client-auth/session/" + code)).timeout(Duration.ofSeconds(10L)).GET().build();
        JsonObject json = TelegramAuthService.sendJson(request);
        JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
        return new SessionStatusResponse(TelegramAuthService.getString(json, "status"), TelegramAuthService.getString(json, "message"), TelegramAuthService.getFirstNonBlank(json, "reason", "status_reason"), user.has("telegram_id") ? TelegramAuthService.getString(user, "telegram_id") : null, user.has("username") ? TelegramAuthService.getString(user, "username") : null, user.has("display_name") ? TelegramAuthService.getString(user, "display_name") : null, user.has("role") ? TelegramAuthService.getString(user, "role") : null, user.has("subscription_label") ? TelegramAuthService.getString(user, "subscription_label") : null);
    }

    public static RegisterResponse registerWithCode(String code, String login, String password) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        payload.addProperty("login", login);
        payload.addProperty("password", password);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(TelegramAuthService.requireBaseUrl() + "/api/client-auth/register")).timeout(Duration.ofSeconds(10L)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        JsonObject json = TelegramAuthService.sendJson(request);
        JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
        return new RegisterResponse(TelegramAuthService.getString(json, "status"), TelegramAuthService.getString(json, "message"), user.has("login") ? TelegramAuthService.getString(user, "login") : null);
    }

    public static TelegramAuthManager.AuthSession loginWithCredentials(String login, String password) throws IOException, InterruptedException {
        String deviceId = TelegramAuthManager.getDeviceId();
        String hwid = TelegramAuthManager.getHardwareId();
        String safeLogin = login == null ? "" : login.trim();
        String safePassword = password == null ? "" : password;
        JsonObject payload = new JsonObject();
        payload.addProperty("login", safeLogin);
        payload.addProperty("username", safeLogin);
        payload.addProperty("password", safePassword);
        payload.addProperty("device_id", deviceId);
        payload.addProperty("hwid", hwid);
        Logger.info("[TelegramAuthService] loginWithCredentials baseUrl=" + TelegramAuthService.getBaseUrl() + ", loginLen=" + safeLogin.length() + ", passwordLen=" + safePassword.length() + ", deviceId=" + deviceId);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(TelegramAuthService.requireBaseUrl() + "/api/client-auth/login")).timeout(Duration.ofSeconds(10L)).header("Content-Type", "application/json").header("X-Device-Id", deviceId).header("X-HWID", hwid).header("X-Login", safeLogin).header("X-Username", safeLogin).header("X-Password", safePassword).POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        JsonObject json = TelegramAuthService.sendJson(request);
        String status = TelegramAuthService.getString(json, "status");
        String reason = TelegramAuthService.getFirstNonBlank(json, "reason", "status_reason");
        String message = TelegramAuthService.getString(json, "message");
        if (!"APPROVED".equalsIgnoreCase(status)) {
            throw new IOException(TelegramAuthService.buildDeniedMessage(status, reason, message));
        }
        JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
        return new TelegramAuthManager.AuthSession(true, TelegramAuthService.readUid(user), TelegramAuthService.getString(user, "telegram_id"), TelegramAuthService.readUsername(user), TelegramAuthService.getString(user, "display_name"), TelegramAuthService.getString(user, "role"), TelegramAuthService.getString(user, "subscription_label"), TelegramAuthService.getString(json, "token"));
    }

    public static TelegramAuthManager.AuthSession fetchProfile(String token) throws IOException, InterruptedException {
        JsonObject user;
        if (token == null || token.isBlank()) {
            return null;
        }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(TelegramAuthService.requireBaseUrl() + "/api/auth/me")).timeout(Duration.ofSeconds(8L)).header("Authorization", "Bearer " + token.trim()).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        JsonObject json = JsonParser.parseString((String)response.body()).getAsJsonObject();
        JsonObject jsonObject = user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : null;
        if (user == null) {
            return null;
        }
        return new TelegramAuthManager.AuthSession(true, TelegramAuthService.readUid(user), TelegramAuthService.readUid(user), TelegramAuthService.readUsername(user), TelegramAuthService.readUsername(user), TelegramAuthService.getString(user, "role"), user.has("hasSubscription") && user.get("hasSubscription").getAsBoolean() ? "FREE" : "NONE", token.trim());
    }

    private static TelegramAuthManager.AuthSession sessionFromClientAuth(JsonObject json) {
        JsonObject user = json.has("user") && json.get("user").isJsonObject() ? json.getAsJsonObject("user") : new JsonObject();
        return new TelegramAuthManager.AuthSession(true, TelegramAuthService.readUid(user), TelegramAuthService.getString(user, "telegram_id"), TelegramAuthService.readUsername(user), TelegramAuthService.getString(user, "display_name"), TelegramAuthService.getString(user, "role"), TelegramAuthService.getString(user, "subscription_label"), TelegramAuthService.getString(json, "token"));
    }

    private static String readUid(JsonObject user) {
        if (user.has("uid") && !user.get("uid").isJsonNull()) {
            return String.valueOf(user.get("uid").getAsInt());
        }
        String telegramId = TelegramAuthService.getString(user, "telegram_id");
        if (!telegramId.isBlank()) {
            return telegramId;
        }
        return TelegramAuthService.getString(user, "id");
    }

    private static String readUsername(JsonObject user) {
        String username = TelegramAuthService.getString(user, "username");
        if (!username.isBlank()) {
            return username;
        }
        return TelegramAuthService.getString(user, "login");
    }

    public static TelegramAuthManager.AuthSession restoreSession(String deviceId, String hwid) throws IOException, InterruptedException {
        Logger.info("[TelegramAuthService] restoreSession baseUrl=" + TelegramAuthService.getBaseUrl() + ", deviceId=" + deviceId);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(TelegramAuthService.requireBaseUrl() + "/api/client-auth/device/" + deviceId + "?hwid=" + TelegramAuthService.encode(hwid))).timeout(Duration.ofSeconds(6L)).header("X-HWID", hwid).GET().build();
        JsonObject json = TelegramAuthService.sendJson(request);
        String status = TelegramAuthService.getString(json, "status");
        if (!"APPROVED".equalsIgnoreCase(status)) {
            String reason = TelegramAuthService.getFirstNonBlank(json, "reason", "status_reason");
            String message = TelegramAuthService.getString(json, "message");
            Logger.info("[TelegramAuthService] restoreSession denied: " + TelegramAuthService.buildDeniedMessage(status, reason, message));
            return null;
        }
        return TelegramAuthService.sessionFromClientAuth(json);
    }

    public static String getBaseUrl() {
        return BackendConfig.baseUrl();
    }

    private static String requireBaseUrl() throws IOException {
        String base = BackendConfig.baseUrl();
        if (base.isEmpty()) {
            throw new IOException("Адрес бэкенда не настроен: заполните Wiosvisual/files/" + BackendConfig.FILE_NAME);
        }
        return base;
    }

    public static boolean isConfigured() {
        return BackendConfig.isConfigured();
    }

    private static JsonObject sendJson(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        Logger.info("[TelegramAuthService] HTTP " + response.statusCode() + " from " + String.valueOf(request.uri()) + ": " + response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonElement element = JsonParser.parseString((String)response.body());
        if (!element.isJsonObject()) {
            throw new IOException("Invalid JSON response");
        }
        return element.getAsJsonObject();
    }

    private static String getString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static String getFirstNonBlank(JsonObject json, String ... keys) {
        for (String key : keys) {
            String value = TelegramAuthService.getString(json, key);
            if (value.isBlank()) continue;
            return value;
        }
        return "";
    }

    private static String buildDeniedMessage(String status, String reason, String message) {
        String normalizedReason;
        if (message != null && !message.isBlank()) {
            return message;
        }
        String string = normalizedReason = reason == null ? "" : reason.trim().toUpperCase();
        if ("HWID_MISMATCH".equals(normalizedReason)) {
            return "HWID mismatch. Request HWID reset in Telegram bot.";
        }
        if (status != null && !status.isBlank()) {
            if (!normalizedReason.isBlank()) {
                return status + ": " + normalizedReason;
            }
            return status;
        }
        if (!normalizedReason.isBlank()) {
            return normalizedReason;
        }
        return "Login denied";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record SessionInitResponse(String code, String authUrl, long expiresInSeconds) {
    }

    public record SessionStatusResponse(String status, String message, String reason, String telegramId, String username, String displayName, String role, String subscriptionLabel) {
    }

    public record RegisterResponse(String status, String message, String login) {
    }
}
