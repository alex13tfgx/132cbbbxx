package code.wiosvisual.utils.client.discord;

import code.wiosvisual.utils.client.net.ClientRole;
import code.wiosvisual.utils.client.auth.TelegramAuthManager;
import code.wiosvisual.utils.client.net.PresenceService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Discord Rich Presence клиента: висит в фоне всё время работы игры и показывает
 * в профиле Discord, где играет пользователь. Настроек и модуля в ClickGUI нет —
 * презенс включается один раз при старте клиента.
 */
public final class DiscordPresence {
    private static final String APPLICATION_ID = "1541867005316173875";
    private static final String DISCORD_URL = "https://discord.gg/AAVFGXHTBW";
    private static final String DOWNLOAD_URL = "https://discord.gg/AAVFGXHTBW";
    /** Must match an asset name in the app's Rich Presence art assets, not the app icon. */
    /** Роль для неавторизованной сессии. */
    private static final String DEFAULT_ROLE = "Dev" + "";
    /**
     * Art assets у приложения не загружены (список пуст), поэтому ключ вида "logo" не
     * показывал картинку. Вместо ключа отдаём прямую ссылку на иконку приложения —
     * Discord сам проксирует внешние картинки (в ответе это видно как "mp:external/...").
     * Хэш иконки берётся из публичного API при первом статусе; этот — запасной.
     */
    private static final String FALLBACK_ICON_HASH = "f5a58e3b5e01e4b1b3eaf416f93ab339";
    private volatile String largeImage;
    private static final String LARGE_TEXT = "WiosVisuals";
    private static final long POLL_INTERVAL_MS = 1000L;
    private static final long RECONNECT_INTERVAL_MS = 15000L;
    private static final long HEARTBEAT_INTERVAL_MS = 30000L;
    private static final long READY_TIMEOUT_MS = 5000L;
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger("Wiosvisual");

    private static final DiscordPresence INSTANCE = new DiscordPresence();

    private final DiscordIPC ipc = new DiscordIPC();
    private final long sessionStart = System.currentTimeMillis();
    private volatile boolean running;
    private Thread worker;
    private String lastPayload = "";
    private long lastSendTime;
    private long lastConnectAttempt;
    private long connectedAt;

    private DiscordPresence() {
    }

    public static DiscordPresence getInstance() {
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
        this.worker = new Thread(this::loop, "Wiosvisual-DiscordRPC");
        this.worker.setDaemon(true);
        this.worker.start();
        LOGGER.info("Discord RPC: презенс запущен, application id {}", (Object)APPLICATION_ID);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Wiosvisual-DiscordRPC-Shutdown"));
    }

    public synchronized void stop() {
        this.running = false;
        this.ipc.close();
    }

    private void loop() {
        while (this.running) {
            try {
                this.tick();
            }
            catch (Throwable ignored) {
                // Discord мог закрыться посреди отправки — просто ждём следующего цикла.
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (!this.ipc.isConnected()) {
            this.lastPayload = "";
            this.connectedAt = 0L;
            if (now - this.lastConnectAttempt < RECONNECT_INTERVAL_MS) {
                return;
            }
            this.lastConnectAttempt = now;
            this.ipc.connect(APPLICATION_ID);
            return;
        }
        if (this.connectedAt == 0L) {
            this.connectedAt = now;
        }
        // Некоторые сборки Discord не присылают READY — после таймаута шлём статус вслепую.
        if (!this.ipc.isReady() && now - this.connectedAt < READY_TIMEOUT_MS) {
            return;
        }
        JsonObject activity = this.buildActivity();
        String payload = GSON.toJson(activity);
        if (payload.equals(this.lastPayload) && now - this.lastSendTime < HEARTBEAT_INTERVAL_MS) {
            return;
        }
        this.lastPayload = payload;
        this.lastSendTime = now;
        this.ipc.sendActivity(activity);
        LOGGER.info("Discord RPC: статус отправлен — {}", (Object)payload);
    }

    private JsonObject buildActivity() {
        JsonObject activity = new JsonObject();
        activity.addProperty("details", this.buildDetails());
        String state = this.buildState();
        if (!state.isEmpty()) {
            activity.addProperty("state", state);
        }
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", this.sessionStart / 1000L);
        activity.add("timestamps", timestamps);
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", this.largeImage());
        assets.addProperty("large_text", LARGE_TEXT);
        activity.add("assets", assets);
        return activity;
    }

    /** Ссылка на иконку приложения; вызывается из фонового потока RPC, сеть не блокирует игру. */
    private String largeImage() {
        if (this.largeImage != null) {
            return this.largeImage;
        }
        String hash = FALLBACK_ICON_HASH;
        try {
            java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient().send(
                    java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://discord.com/api/v9/applications/" + APPLICATION_ID + "/rpc"))
                            .timeout(java.time.Duration.ofSeconds(5)).build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            JsonObject app = GSON.fromJson(response.body(), JsonObject.class);
            if (app != null && app.has("icon") && !app.get("icon").isJsonNull()) {
                hash = app.get("icon").getAsString();
            }
        } catch (Throwable ignored) {
            // Нет сети — остаётся запасной хэш.
        }
        this.largeImage = "https://cdn.discordapp.com/app-icons/" + APPLICATION_ID + "/" + hash + ".png?size=512";
        return this.largeImage;
    }

    /** Первая строка: сборка и ник, как «Build #5.0 • Ник: gayRR_». */
    private String buildDetails() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String nick = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "unknown";
        String version = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("renderextras")
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("1.0");
        return "Build #" + version + " \u2022 \u041d\u0438\u043a: " + nick;
    }

    /** Вторая строка: сервер и онлайн, как «Сервер: mc.example.su (155 из 2026)». */
    private String buildState() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) {
            return "\u0421\u0435\u0440\u0432\u0435\u0440: \u0433\u043b\u0430\u0432\u043d\u043e\u0435 \u043c\u0435\u043d\u044e";
        }
        if (mc.isIntegratedServerRunning()) {
            return "\u041e\u0434\u0438\u043d\u043e\u0447\u043d\u0430\u044f \u0438\u0433\u0440\u0430";
        }
        ServerInfo server = mc.getCurrentServerEntry();
        String address = server != null ? server.address : null;
        if (address == null || address.isBlank()) {
            return "\u0421\u0435\u0440\u0432\u0435\u0440: \u0441\u0435\u0442\u0435\u0432\u0430\u044f \u0438\u0433\u0440\u0430";
        }
        int online = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 0;
        String text = "\u0421\u0435\u0440\u0432\u0435\u0440: " + address;
        // Максимум берётся из пинга списка серверов; если сервер его не прислал — показываем только онлайн.
        if (server.players != null) {
            int shown = Math.max(online, server.players.online());
            text += " (" + shown + " \u0438\u0437 " + server.players.max() + ")";
        } else if (online > 0) {
            text += " (" + online + ")";
        }
        return text;
    }
}
