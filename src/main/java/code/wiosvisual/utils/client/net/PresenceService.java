package code.wiosvisual.utils.client.net;

import code.wiosvisual.utils.client.auth.TelegramAuthManager;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.logs.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Держит связь с бэкендом: раз в 20 секунд шлёт «я в игре», получает список онлайна
 * с ролями и общий чат. Без настроенного адреса (см. {@link BackendConfig}) сервис
 * не делает ни одного запроса.
 */
public final class PresenceService {
    private static final PresenceService INSTANCE = new PresenceService();
    private static final Gson GSON = new Gson();
    private static final long PRESENCE_INTERVAL_MS = 20_000L;
    private static final long RECONNECT_INTERVAL_MS = 10_000L;
    private static final String CLIENT_VERSION = "2.1.2";

    private final List<OnlineUser> onlineUsers = Collections.synchronizedList(new ArrayList<>());
    private volatile ClientRole selfRole = ClientRole.USER;
    private volatile boolean running;
    private volatile boolean chatInGame = true;
    private volatile long lastPresenceSentAt;
    private volatile long nextConnectAttemptAt;
    private volatile String lastPresenceKey = "";
    private BackendSocket socket;
    private Thread worker;

    private PresenceService() {
    }

    public static PresenceService getInstance() {
        return INSTANCE;
    }

    public static void init() {
        INSTANCE.start();
    }

    public synchronized void start() {
        if (this.running) {
            return;
        }
        this.running = true;
        this.worker = new Thread(this::loop, "Wiosvisual-Presence");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public synchronized void stop() {
        this.running = false;
        this.closeSocket();
        this.onlineUsers.clear();
    }

    /** Переподключиться после логина/выхода — токен меняется, соединение тоже. */
    public synchronized void reconnect() {
        this.closeSocket();
        this.nextConnectAttemptAt = 0L;
    }

    public List<OnlineUser> getOnlineUsers() {
        synchronized (this.onlineUsers) {
            return new ArrayList<>(this.onlineUsers);
        }
    }

    public int getOnlineCount() {
        return this.onlineUsers.size();
    }

    public ClientRole getSelfRole() {
        return this.selfRole;
    }

    public boolean isConnected() {
        return this.socket != null && this.socket.isOpen();
    }

    public boolean isChatInGame() {
        return this.chatInGame;
    }

    public void setChatInGame(boolean chatInGame) {
        this.chatInGame = chatInGame;
    }

    public boolean sendChat(String text) {
        if (text == null || text.isBlank() || !this.isConnected()) {
            return false;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "chat");
        payload.addProperty("text", text.trim());
        this.socket.send(GSON.toJson((JsonElement) payload));
        return true;
    }

    private void loop() {
        while (this.running) {
            try {
                this.tick();
            } catch (Throwable error) {
                Logger.warn("[Presence] " + error.getMessage());
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void tick() {
        if (!BackendConfig.isConfigured() || !TelegramAuthManager.isAuthorized()) {
            this.closeSocket();
            this.onlineUsers.clear();
            return;
        }
        this.ensureConnected();
        if (!this.isConnected()) {
            return;
        }
        long now = System.currentTimeMillis();
        String presenceKey = this.currentIngameName() + "|" + this.currentServer();
        boolean changed = !presenceKey.equals(this.lastPresenceKey);
        if (!changed && now - this.lastPresenceSentAt < PRESENCE_INTERVAL_MS) {
            return;
        }
        this.lastPresenceKey = presenceKey;
        this.lastPresenceSentAt = now;
        this.sendPresence();
    }

    private void ensureConnected() {
        if (this.socket != null && (this.socket.isOpen() || this.socket.isConnectingState())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextConnectAttemptAt) {
            return;
        }
        this.nextConnectAttemptAt = now + RECONNECT_INTERVAL_MS;
        String token = TelegramAuthManager.getAuthToken();
        String base = BackendConfig.webSocketUrl();
        if (base.isEmpty() || token.isBlank()) {
            return;
        }
        try {
            String url = base + "?source=client&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            this.socket = new BackendSocket(URI.create(url), this::handleMessage, this::sendPresence);
            this.socket.connect();
        } catch (Exception e) {
            Logger.warn("[Presence] не удалось подключиться: " + e.getMessage());
        }
    }

    private void closeSocket() {
        BackendSocket current = this.socket;
        this.socket = null;
        this.lastPresenceKey = "";
        if (current != null) {
            try {
                current.close();
            } catch (Exception ignored) {
                // соединение уже мертво
            }
        }
    }

    private void sendPresence() {
        if (!this.isConnected()) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "presence");
        payload.addProperty("ingameName", this.currentIngameName());
        payload.addProperty("server", this.currentServer());
        payload.addProperty("version", CLIENT_VERSION);
        this.socket.send(GSON.toJson((JsonElement) payload));
    }

    private void handleMessage(String raw) {
        try {
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "";
            switch (type) {
                case "hello" -> this.readSelf(json);
                case "online" -> this.readOnline(json);
                case "chat" -> this.readChat(json.getAsJsonObject("message"), false);
                case "chat_history" -> {
                    // историю в игровой чат не льём — она нужна только сайту
                }
                default -> {
                }
            }
        } catch (Exception e) {
            Logger.warn("[Presence] некорректный пакет: " + e.getMessage());
        }
    }

    private void readSelf(JsonObject json) {
        if (json.has("user") && json.get("user").isJsonObject()) {
            JsonObject user = json.getAsJsonObject("user");
            this.selfRole = ClientRole.of(user.has("role") ? user.get("role").getAsString() : null);
        }
    }

    private void readOnline(JsonObject json) {
        if (!json.has("users") || !json.get("users").isJsonArray()) {
            return;
        }
        JsonArray array = json.getAsJsonArray("users");
        List<OnlineUser> parsed = new ArrayList<>(array.size());
        String selfLogin = TelegramAuthManager.getSavedLogin();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject user = element.getAsJsonObject();
            OnlineUser row = new OnlineUser(
                    string(user, "login"),
                    string(user, "displayName"),
                    ClientRole.of(string(user, "role")),
                    string(user, "ingameName"),
                    string(user, "server"));
            parsed.add(row);
            if (!selfLogin.isBlank() && selfLogin.equalsIgnoreCase(row.login())) {
                this.selfRole = row.role();
            }
        }
        parsed.sort(Comparator.comparingInt((OnlineUser user) -> -user.role().weight())
                .thenComparing(OnlineUser::nameForList, String.CASE_INSENSITIVE_ORDER));
        synchronized (this.onlineUsers) {
            this.onlineUsers.clear();
            this.onlineUsers.addAll(parsed);
        }
    }

    private void readChat(JsonObject message, boolean history) {
        if (message == null || (!this.chatInGame && !history)) {
            return;
        }
        String author = string(message, "displayName");
        String text = string(message, "text");
        if (author.isBlank() || text.isBlank()) {
            return;
        }
        ClientRole role = ClientRole.of(string(message, "role"));
        MutableText line = Text.literal("")
                .append(Text.literal("[" + role.label() + "] ")
                        .setStyle(Style.EMPTY.withColor(role.color() & 0xFFFFFF)))
                .append(Text.literal(author).formatted(Formatting.WHITE))
                .append(Text.literal(": ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(text).formatted(Formatting.GRAY));
        ChatMessage.send(line);
    }

    private String currentIngameName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSession() == null) {
            return "";
        }
        return mc.getSession().getUsername();
    }

    private String currentServer() {
        // Нормализацию адреса держим в ServerIdentity: там же разбор режимов HolyWorld.
        return ServerIdentity.describeCurrent();
    }

    private static String string(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
}
