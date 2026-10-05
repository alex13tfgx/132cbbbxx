package code.wiosvisual.utils.client.server;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.net.ServerIdentity;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Клиент In-Game API HolyWorld (LiteAPI): https://wiki.holyworld.me/lite/api/protocol
 *
 * Транспорт — plugin-каналы, в пакете голые байты JSON в UTF-8 (без префикса длины).
 * Запрос {id, method, payload} → ответ {id, ok, payload | error, message}, плюс push-события
 * {event, payload} без id.
 *
 * Модуль Feature Control (liteapi:feature-control): после входа отправляем список
 * функций клиента (модулей), сервер отвечает теми, что заблокированы. Заблокированное
 * обязано быть полностью скрыто от игрока — это делает {@link ServerRestrictions}.
 *
 * Fail-closed на {@link #FAIL_OPEN_MS}: сразу после входа, пока сервер с LiteAPI (или HolyWorld
 * по адресу) не прислал блок-лист, заблокированы все функции. Если ответа нет дольше — функции
 * открываются (сервер может держать канал, но не отвечать: так ведёт себя HolyWorld 1.16.5),
 * а проверка продолжает повторяться; пришедший блок-лист применяется сразу. Неудачная проверка (таймаут, INTERNAL_ERROR, RATE_LIMITED, любая
 * другая ошибка) повторяется не чаще раза в {@link #CHECK_COOLDOWN_MS}, с нарастающей паузой.
 */
public final class HolyWorldLiteApi
implements QuickImports {
    /** Стабильный идентификатор клиента: по нему HolyWorld хранит персональные блокировки — не менять между версиями. */
    public static final String CLIENT_ID = "wiosvisual";
    private static final Logger LOGGER = LogManager.getLogger("Wiosvisual");
    private static final long TIMEOUT_SECONDS = 5L;
    /** Rate limit checkFeatures: 1 запрос в 10 секунд на игрока. Это же — минимальная пауза перед повтором. */
    private static final long CHECK_COOLDOWN_MS = 10_500L;
    /** Потолок паузы между повторами неудачной проверки. */
    private static final long RETRY_MAX_MS = 21_000L;
    /** Сколько ждать регистрации канала сервером после входа, прежде чем решить, что LiteAPI там нет. */
    private static final long CHANNEL_WAIT_MS = 15_000L;
    /** Сколько держать всё закрытым в ожидании блок-листа, прежде чем открыть функции. */
    private static final long FAIL_OPEN_MS = 10_000L;
    /** Модули ядра, которые не являются «функциями» и не отправляются на проверку. */
    private static final Set<String> CORE = Set.of("clickgui", "hud", "themeeditor");

    private static final HolyWorldLiteApi INSTANCE = new HolyWorldLiteApi();
    private static boolean registered;

    private final Map<String, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonObject>> eventHandlers = new ConcurrentHashMap<>();
    private volatile Set<String> blocklist = Collections.emptySet();
    /** Последний показанный игроку блок-лист — уведомляем только об изменениях. */
    private Set<String> announced = Collections.emptySet();
    private volatile State state = State.OFFLINE;
    private ClientWorld lastWorld;
    private long joinedAt;
    private long checkDueAt = -1L;
    private long lastCheckAt;
    private boolean checkInFlight;
    /** minecraft:register с нашим каналом уже отправлен в этом мире. */
    private boolean channelAnnounced;
    /** Диагностическая проба связи уже отправлена в этом мире. */
    private boolean probed;
    private int failures;
    /** Растёт при каждом входе/выходе: ответы от прошлого подключения игнорируются. */
    private int session;

    /** Состояние Feature Control на текущем подключении. */
    private enum State {
        /** Не на сервере — блокировать нечего. */
        OFFLINE,
        /** На сервере, блок-листа ещё нет: fail-closed, всё заблокировано. */
        AWAITING,
        /** Блок-лист получен и действует. */
        RECEIVED,
        /** Сервер не ответил за {@link #FAIL_OPEN_MS}: функции открыты, проверка повторяется. */
        TIMED_OUT,
        /** Одиночная игра или сервер без LiteAPI. */
        NO_API
    }

    private HolyWorldLiteApi() {
    }

    public static HolyWorldLiteApi getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------ пакет канала

    /** Пакет LiteAPI: строка JSON, канал задаётся id (у каждого модуля свой). */
    public record JsonPayload(CustomPayload.Id<JsonPayload> type, String json) implements CustomPayload {
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return this.type;
        }

        static PacketCodec<PacketByteBuf, JsonPayload> codec(CustomPayload.Id<JsonPayload> type) {
            return new PacketCodec<>() {
                @Override
                public JsonPayload decode(PacketByteBuf buf) {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return new JsonPayload(type, new String(bytes, StandardCharsets.UTF_8));
                }

                @Override
                public void encode(PacketByteBuf buf, JsonPayload payload) {
                    buf.writeBytes(payload.json().getBytes(StandardCharsets.UTF_8));
                }
            };
        }
    }

    public static final CustomPayload.Id<JsonPayload> FEATURE_CONTROL =
            new CustomPayload.Id<>(Identifier.of("liteapi", "feature-control"));
    /** Соседний модуль LiteAPI — только для диагностики связи (проба несуществующим методом). */
    public static final CustomPayload.Id<JsonPayload> SERVER_INFO =
            new CustomPayload.Id<>(Identifier.of("liteapi", "server-info"));

    /** Регистрирует каналы LiteAPI и события подключения. Вызывать один раз при старте клиента. */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        PacketCodec<PacketByteBuf, JsonPayload> codec = JsonPayload.codec(FEATURE_CONTROL);
        PayloadTypeRegistry.playC2S().register(FEATURE_CONTROL, codec);
        PayloadTypeRegistry.playS2C().register(FEATURE_CONTROL, codec);
        ClientPlayNetworking.registerGlobalReceiver(FEATURE_CONTROL, (payload, context) -> INSTANCE.onMessage(payload.json()));
        PacketCodec<PacketByteBuf, JsonPayload> infoCodec = JsonPayload.codec(SERVER_INFO);
        PayloadTypeRegistry.playC2S().register(SERVER_INFO, infoCodec);
        PayloadTypeRegistry.playS2C().register(SERVER_INFO, infoCodec);
        ClientPlayNetworking.registerGlobalReceiver(SERVER_INFO, (payload, context) -> INSTANCE.onMessage(payload.json()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> INSTANCE.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> INSTANCE.reset());
    }

    // ------------------------------------------------------------------ общий протокол

    /**
     * Вызывает метод LiteAPI. Future завершится payload'ом ответа, ошибкой
     * {@link LiteApiException} (BAD_REQUEST, NOT_FOUND, RATE_LIMITED, INTERNAL_ERROR)
     * или таймаутом через 5 секунд.
     */
    public CompletableFuture<JsonObject> request(CustomPayload.Id<JsonPayload> channel, String method, JsonObject payload) {
        String id = UUID.randomUUID().toString();
        JsonObject envelope = new JsonObject();
        envelope.addProperty("id", id);
        envelope.addProperty("method", method);
        envelope.add("payload", payload != null ? payload : new JsonObject());
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.pending.put(id, future);
        try {
            ClientPlayNetworking.send(new JsonPayload(channel, envelope.toString()));
        } catch (Throwable error) {
            this.pending.remove(id);
            future.completeExceptionally(error);
            return future;
        }
        return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).whenComplete((r, e) -> this.pending.remove(id));
    }

    /** Подписка на push-событие сервера ({event, payload} без id). */
    public void onEvent(String event, Consumer<JsonObject> handler) {
        this.eventHandlers.put(event, handler);
    }

    /** Есть ли канал у текущего сервера (сервер регистрирует его через minecraft:register). */
    public static boolean available(CustomPayload.Id<JsonPayload> channel) {
        try {
            return mc.getNetworkHandler() != null && ClientPlayNetworking.canSend(channel);
        } catch (Throwable error) {
            return false;
        }
    }

    private void onMessage(String json) {
        LOGGER.info("[LiteAPI] ответ сервера: {}", json.length() > 500 ? json.substring(0, 500) + "…" : json);
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception error) {
            LOGGER.warn("[LiteAPI] не JSON от сервера: {}", json);
            return;
        }
        if (root.has("event") && !root.get("event").isJsonNull()) {
            Consumer<JsonObject> handler = this.eventHandlers.get(root.get("event").getAsString());
            JsonObject payload = object(root, "payload");
            if (handler != null) {
                handler.accept(payload);
            } else if (payload.has("blocklist")) {
                // неизвестное событие, но с блок-листом — применяем
                mc.execute(() -> this.applyBlocklist(payload));
            }
            return;
        }
        String id = root.has("id") && !root.get("id").isJsonNull() ? root.get("id").getAsString() : null;
        CompletableFuture<JsonObject> future = id == null ? null : this.pending.remove(id);
        if (future == null) {
            return;
        }
        if (root.has("ok") && root.get("ok").getAsBoolean()) {
            future.complete(object(root, "payload"));
        } else {
            String error = root.has("error") && !root.get("error").isJsonNull() ? root.get("error").getAsString() : "UNKNOWN";
            String message = root.has("message") && !root.get("message").isJsonNull() ? root.get("message").getAsString() : null;
            future.completeExceptionally(new LiteApiException(error, message));
        }
    }

    /** Ошибка из ответа сервера: код из протокола и необязательное описание. */
    public static final class LiteApiException extends RuntimeException {
        public final String code;

        LiteApiException(String code, String message) {
            super(message == null ? code : code + ": " + message);
            this.code = code;
        }
    }

    private static JsonObject object(JsonObject root, String key) {
        JsonElement e = root.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    // ------------------------------------------------------------------ Feature Control

    /** Функция заблокирована сервером HolyWorld прямо сейчас (до первого ответа — заблокировано всё). */
    public static boolean isBlocked(Module module) {
        return switch (INSTANCE.state) {
            case AWAITING -> !CORE.contains(featureId(module));
            case RECEIVED -> INSTANCE.blocklist.contains(featureId(module));
            default -> false;
        };
    }

    /** Идентификатор функции для сервера: имя модуля в нижнем регистре (fullbright, zoom, freelook…). */
    public static String featureId(Module module) {
        return module.getName().toLowerCase(Locale.ROOT);
    }

    private void scheduleCheck(long delayMs) {
        this.checkDueAt = System.currentTimeMillis() + delayMs;
    }

    private void onJoin() {
        this.reset();
        LOGGER.info("[LiteAPI] вход на сервер {}", mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "?");
        this.joinedAt = System.currentTimeMillis();
        // В одиночной игре LiteAPI нет; на сервере до ответа всё закрыто.
        // lastCheckAt не сбрасываем: лимит сервера считается на игрока, а не на подключение.
        this.state = mc.isInSingleplayer() ? State.NO_API : State.AWAITING;
        this.scheduleCheck(0L);
    }

    private void reset() {
        this.session++;
        this.pending.values().forEach(f -> f.cancel(false));
        this.pending.clear();
        this.blocklist = Collections.emptySet();
        this.state = State.OFFLINE;
        this.announced = Collections.emptySet();
        this.checkDueAt = -1L;
        this.checkInFlight = false;
        this.channelAnnounced = false;
        this.probed = false;
        this.failures = 0;
        this.lastWorld = null;
    }

    /** Пауза перед повтором: 10.5 → 21 сек (лимит сервера — 1 запрос в 10 с). */
    private long retryDelay() {
        long delay = CHECK_COOLDOWN_MS << Math.min(this.failures, 3);
        return Math.min(delay, RETRY_MAX_MS);
    }

    /**
     * Проверка запускается после входа и после смены мира (переход лобби → анархия через прокси
     * может сменить сервер за прокси). Ждём, пока сервер зарегистрирует канал, и соблюдаем
     * лимит 1 запрос в 10 секунд.
     */
    @EventHandler
    public void onTick(TickEvent e) {
        if (this.state == State.OFFLINE || this.state == State.NO_API && mc.isInSingleplayer()) {
            return;
        }
        if (this.state == State.AWAITING && System.currentTimeMillis() - this.joinedAt > FAIL_OPEN_MS) {
            this.state = State.TIMED_OUT;
            ServerRestrictions.getInstance().enforceNow();
            LOGGER.warn("[LiteAPI] сервер не прислал блок-лист за {} с — функции открыты, проверка продолжается", FAIL_OPEN_MS / 1000);
            Notifications.getInstance().addList("HolyWorld не ответил на проверку функций " + Formatting.GRAY + "— функции доступны", 3500);
        }
        if (mc.world != this.lastWorld) {
            this.lastWorld = mc.world;
            this.channelAnnounced = false; // прокси мог перекинуть на другой сервер — объявляем канал заново
            this.probed = false;
            this.logServerChannels();
            if (mc.world != null) {
                // Смена мира — возможно, прокси перекинул на другой сервер. Если запрос уже в пути,
                // повтор уйдёт после ответа (onTick не шлёт, пока checkInFlight).
                this.scheduleCheck(0L);
            }
        }
        long now = System.currentTimeMillis();
        boolean channel = available(FEATURE_CONTROL);
        if (this.state == State.NO_API) {
            if (!channel) {
                return;
            }
            // Канал появился позже (или прокси перекинул на сервер с LiteAPI) — снова закрываемся до ответа.
            this.state = State.AWAITING;
            this.joinedAt = now;
            this.scheduleCheck(0L);
        }
        if (!channel) {
            // На адресе HolyWorld остаёмся закрытыми и ждём канал сколько угодно; на прочих серверах считаем, что LiteAPI нет.
            if (this.state == State.AWAITING && !ServerIdentity.isHolyWorld() && now - this.joinedAt > CHANNEL_WAIT_MS) {
                this.state = State.NO_API;
                this.checkDueAt = -1L;
            }
            return;
        }
        if (this.checkInFlight || this.checkDueAt < 0L || now < this.checkDueAt || mc.player == null) {
            return;
        }
        if (now - this.lastCheckAt < CHECK_COOLDOWN_MS) {
            this.checkDueAt = this.lastCheckAt + CHECK_COOLDOWN_MS;
            return;
        }
        this.checkDueAt = -1L;
        this.lastCheckAt = now;
        this.announceChannel();
        this.checkFeatures();
    }

    /**
     * Сервер HolyWorld — Bukkit 1.16.5 за ViaVersion. Bukkit отвечает плагин-сообщением, только если
     * клиент зарегистрировал канал (minecraft:register). Fabric 1.21 шлёт регистрацию в фазе
     * configuration, которой у 1.16.5 нет, — через Via она теряется, и сервер молча не отвечает.
     * Поэтому регистрируем канал сами в play-фазе, как это делает клиент 1.16.5.
     */
    private void announceChannel() {
        if (this.channelAnnounced) {
            return;
        }
        this.channelAnnounced = true;
        try {
            ClientPlayNetworking.send(new RegistrationPayload(RegistrationPayload.REGISTER, List.of(FEATURE_CONTROL.id())));
            LOGGER.info("[LiteAPI] канал {} зарегистрирован (minecraft:register)", FEATURE_CONTROL.id());
        } catch (Throwable error) {
            LOGGER.warn("[LiteAPI] не удалось отправить minecraft:register: {}", error.toString());
        }
    }

    private void logServerChannels() {
        try {
            if (mc.getNetworkHandler() != null) {
                LOGGER.info("[LiteAPI] мир сменился; каналы, объявленные сервером: {}", ClientPlayNetworking.getSendable());
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Диагностика: по протоколу на неизвестный метод сервер обязан ответить NOT_FOUND.
     * Ответ есть — связь работает, сервер игнорирует именно checkFeatures; тишина — LiteAPI
     * на этом сервере не отвечает вообще (или ответы теряются по дороге, например в ViaVersion).
     */
    private void probe() {
        if (this.probed) {
            return;
        }
        this.probed = true;
        for (CustomPayload.Id<JsonPayload> channel : List.of(FEATURE_CONTROL, SERVER_INFO)) {
            if (!available(channel)) {
                LOGGER.info("[LiteAPI] проба: сервер не объявил канал {}", channel.id());
                continue;
            }
            this.request(channel, "wiosProbe", new JsonObject()).whenComplete((r, e) -> {
                Throwable cause = e != null && e.getCause() != null ? e.getCause() : e;
                if (cause == null) {
                    LOGGER.info("[LiteAPI] проба {}: ответ ok {}", channel.id(), r);
                } else if (cause instanceof LiteApiException api) {
                    LOGGER.info("[LiteAPI] проба {}: сервер ответил ошибкой {} — связь работает", channel.id(), api.getMessage());
                } else {
                    LOGGER.info("[LiteAPI] проба {}: нет ответа ({})", channel.id(), cause.toString());
                }
            });
        }
    }

    private void checkFeatures() {
        JsonArray features = new JsonArray();
        for (Module module : modules()) {
            String id = featureId(module);
            if (!CORE.contains(id)) {
                features.add(id);
            }
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("client", CLIENT_ID);
        payload.add("features", features);
        int requestSession = this.session;
        this.checkInFlight = true;
        LOGGER.info("[LiteAPI] checkFeatures → {} функц.: {}", features.size(), features);
        this.request(FEATURE_CONTROL, "checkFeatures", payload).whenComplete((response, error) -> mc.execute(() -> {
            if (requestSession != this.session) {
                return; // ответ относится к прошлому подключению
            }
            this.checkInFlight = false;
            if (error != null) {
                // Любая ошибка — повтор. Прежний блок-лист (или полная блокировка) остаётся в силе.
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                long delay = this.retryDelay();
                this.failures++;
                this.scheduleCheck(delay);
                LOGGER.warn("[LiteAPI] checkFeatures не удался ({}), повтор через {} с", cause.toString(), delay / 1000.0);
                this.probe();
                return;
            }
            this.failures = 0;
            this.applyBlocklist(response);
        }));
    }

    private void applyBlocklist(JsonObject payload) {
        Set<String> list = new HashSet<>();
        JsonElement arr = payload.get("blocklist");
        if (arr != null && arr.isJsonArray()) {
            for (JsonElement el : arr.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    list.add(el.getAsString().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (this.state == State.OFFLINE) {
            return; // пришло после выхода с сервера
        }
        this.blocklist = Collections.unmodifiableSet(list);
        this.state = State.RECEIVED;
        // Заблокированное выключается и пропадает из всех GUI сразу, не дожидаясь тика.
        ServerRestrictions.getInstance().enforceNow();
        LOGGER.info("[LiteAPI] блок-лист HolyWorld: {} функц. {}", list.size(), list);
        this.announce(list);
    }

    /** Сообщает игроку, какие функции сервер запретил (только если список изменился). */
    private void announce(Set<String> list) {
        if (list.equals(this.announced)) {
            return;
        }
        this.announced = list;
        if (list.isEmpty()) {
            Notifications.getInstance().addList("HolyWorld: " + Formatting.GREEN + "все функции разрешены", 3000);
            return;
        }
        List<String> names = new ArrayList<>();
        for (Module module : modules()) {
            if (list.contains(featureId(module))) {
                names.add(module.getName());
            }
        }
        if (names.isEmpty()) {
            names.addAll(list);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        String shown = names.size() <= 5 ? String.join(", ", names)
                : String.join(", ", names.subList(0, 5)) + " и ещё " + (names.size() - 5);
        Notifications.getInstance().addList("HolyWorld убрал " + Formatting.RED + names.size() + Formatting.RESET + " функц.: " + shown, 6000);
        if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("[Wios] HolyWorld запретил функции (" + names.size() + "): ").formatted(Formatting.GRAY)
                    .append(Text.literal(String.join(", ", names)).formatted(Formatting.RED)));
        }
    }

    private static List<Module> modules() {
        Wiosvisual client = Wiosvisual.getInstance();
        if (client == null || client.getModuleRepository() == null) {
            return List.of();
        }
        return client.getModuleRepository().modules();
    }
}
