/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package code.wiosvisual.utils.client.auth;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.utils.client.auth.HardwareFingerprint;
import code.wiosvisual.utils.client.auth.TelegramAuthService;
import code.wiosvisual.utils.client.logs.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.UUID;

public final class TelegramAuthManager {
    private static final String FILE_NAME = "TelegramAuth.json";
    private static volatile AuthSession activeSession;
    private static volatile String deviceId;
    private static volatile String hardwareId;
    private static volatile String savedLogin;
    private static volatile String savedPassword;
    private static volatile String authToken;
    private static volatile boolean initialized;

    private TelegramAuthManager() {
    }

    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        TelegramAuthManager.loadState();
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        }
        if ((hardwareId = HardwareFingerprint.computeStableHwid()) == null || hardwareId.isBlank()) {
            hardwareId = UUID.nameUUIDFromBytes(("Wiosvisual-HWID:" + deviceId).getBytes(StandardCharsets.UTF_8)).toString();
        }
        savedLogin = TelegramAuthManager.normalize(savedLogin);
        savedPassword = TelegramAuthManager.normalize(savedPassword);
        authToken = TelegramAuthManager.normalize(authToken);
        initialized = true;
        if (!TelegramAuthManager.restorePersistedSession()) {
            activeSession = AuthSession.guest();
        }
        TelegramAuthManager.saveState();
    }

    public static synchronized boolean restorePersistedSession() {
        TelegramAuthManager.ensureInitialized();
        try {
            AuthSession fromToken;
            if (!authToken.isBlank() && (fromToken = TelegramAuthService.fetchProfile(authToken)) != null && fromToken.authorized()) {
                activeSession = fromToken;
                TelegramAuthManager.saveState();
                return true;
            }
            AuthSession restored = TelegramAuthService.restoreSession(deviceId, hardwareId);
            if (restored != null && restored.authorized()) {
                activeSession = restored;
                if (restored.token() != null && !restored.token().isBlank()) {
                    authToken = restored.token();
                }
                TelegramAuthManager.saveState();
                return true;
            }
        }
        catch (Exception e) {
            Logger.warn("[TelegramAuthManager] restore failed: " + e.getMessage());
        }
        activeSession = AuthSession.guest();
        TelegramAuthManager.saveState();
        return false;
    }

    public static boolean isAuthorized() {
        TelegramAuthManager.ensureInitialized();
        return activeSession != null && activeSession.authorized();
    }

    public static AuthSession getActiveSession() {
        TelegramAuthManager.ensureInitialized();
        return activeSession;
    }

    public static synchronized void authorize(AuthSession session) {
        TelegramAuthManager.ensureInitialized();
        AuthSession authSession = activeSession = session == null ? AuthSession.guest() : session;
        if (activeSession.token() != null && !activeSession.token().isBlank()) {
            authToken = activeSession.token();
        }
        TelegramAuthManager.saveState();
        Wiosvisual.getInstance().syncSiteIdentity();
    }

    public static synchronized void clear() {
        TelegramAuthManager.ensureInitialized();
        activeSession = AuthSession.guest();
        authToken = "";
        TelegramAuthManager.saveState();
        Wiosvisual.getInstance().syncSiteIdentity();
    }

    public static synchronized void setSavedCredentials(String login, String password) {
        TelegramAuthManager.ensureInitialized();
        savedLogin = TelegramAuthManager.normalize(login);
        savedPassword = TelegramAuthManager.normalize(password);
        TelegramAuthManager.saveState();
    }

    public static String getSavedLogin() {
        TelegramAuthManager.ensureInitialized();
        return savedLogin == null ? "" : savedLogin;
    }

    public static String getSavedPassword() {
        TelegramAuthManager.ensureInitialized();
        return savedPassword == null ? "" : savedPassword;
    }

    public static String getAuthToken() {
        TelegramAuthManager.ensureInitialized();
        return authToken == null ? "" : authToken;
    }

    public static String getDeviceId() {
        TelegramAuthManager.ensureInitialized();
        return deviceId;
    }

    public static String getHardwareId() {
        TelegramAuthManager.ensureInitialized();
        return hardwareId;
    }

    public static String getDisplayNameOrFallback(String fallback) {
        AuthSession session = TelegramAuthManager.getActiveSession();
        if (session != null && session.authorized()) {
            // Показываем ник аккаунта — тот же, что на сайте и в Discord RPC.
            if (session.displayName() != null && !session.displayName().isBlank()) {
                return session.displayName();
            }
            if (session.username() != null && !session.username().isBlank()) {
                return session.username();
            }
        }
        return fallback;
    }

    public static String getRoleOrFallback(String fallback) {
        AuthSession session = TelegramAuthManager.getActiveSession();
        if (session != null && session.authorized() && session.role() != null && !session.role().isBlank()) {
            return session.role();
        }
        return fallback;
    }

    public static String getUidOrFallback(String fallback) {
        AuthSession session = TelegramAuthManager.getActiveSession();
        if (session != null && session.authorized()) {
            if (session.uid() != null && !session.uid().isBlank()) {
                return session.uid();
            }
            if (session.telegramId() != null && !session.telegramId().isBlank()) {
                return session.telegramId();
            }
        }
        return fallback;
    }

    public static String getIdentifierOrFallback(String fallback) {
        return TelegramAuthManager.getUidOrFallback(fallback);
    }

    public static String getSubscriptionLabel() {
        AuthSession session = TelegramAuthManager.getActiveSession();
        if (session != null && session.authorized() && session.subscriptionLabel() != null && !session.subscriptionLabel().isBlank()) {
            return session.subscriptionLabel();
        }
        return TelegramAuthManager.isAuthorized() ? "FREE" : "NONE";
    }

    public static boolean isBetaUser() {
        AuthSession session = TelegramAuthManager.getActiveSession();
        if (session == null || !session.authorized()) {
            return false;
        }
        String role = session.role() == null ? "" : session.role().trim().toUpperCase();
        return role.contains("BETA") || role.contains("ADMIN") || role.contains("OWNER");
    }

    private static File getStateFile() {
        try {
            File filesDir = Wiosvisual.getInstance().getClientInfoProvider().filesDir();
            if (!filesDir.exists()) {
                filesDir.mkdirs();
            }
            return new File(filesDir, FILE_NAME);
        }
        catch (Exception e) {
            File fallbackDir = new File("Wiosvisual/files");
            if (!fallbackDir.exists()) {
                fallbackDir.mkdirs();
            }
            return new File(fallbackDir, FILE_NAME);
        }
    }

    private static void saveState() {
        try {
            File file = TelegramAuthManager.getStateFile();
            JsonObject json = new JsonObject();
            json.addProperty("deviceId", TelegramAuthManager.normalize(deviceId));
            json.addProperty("savedLogin", TelegramAuthManager.normalize(savedLogin));
            json.addProperty("savedPassword", TelegramAuthManager.normalize(savedPassword));
            json.addProperty("authToken", TelegramAuthManager.normalize(authToken));
            Files.writeString(file.toPath(), (CharSequence)json.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception e) {
            Logger.warn("[TelegramAuthManager] Failed to save state: " + e.getMessage());
        }
    }

    private static void loadState() {
        savedLogin = "";
        savedPassword = "";
        authToken = "";
        deviceId = null;
        hardwareId = null;
        activeSession = AuthSession.guest();
        File file = TelegramAuthManager.getStateFile();
        if (!file.exists()) {
            return;
        }
        try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString((String)raw).getAsJsonObject();
            deviceId = TelegramAuthManager.readString(json, "deviceId");
            savedLogin = TelegramAuthManager.normalize(TelegramAuthManager.readString(json, "savedLogin"));
            savedPassword = TelegramAuthManager.normalize(TelegramAuthManager.readString(json, "savedPassword"));
            authToken = TelegramAuthManager.normalize(TelegramAuthManager.readString(json, "authToken"));
        }
        catch (IOException e) {
            Logger.warn("[TelegramAuthManager] Failed to read state: " + e.getMessage());
        }
        catch (Exception e) {
            Logger.warn("[TelegramAuthManager] Invalid state file format: " + e.getMessage());
        }
    }

    private static String readString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return "";
        }
        return json.get(key).getAsString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record AuthSession(boolean authorized, String uid, String telegramId, String username, String displayName, String role, String subscriptionLabel, String token) {
        public static AuthSession guest() {
            return new AuthSession(false, "", "", "", "", "Guest", "NONE", "");
        }
    }
}
