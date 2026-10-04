package guardlisten;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Discord Rich Presence лаунчера: пишет кадры напрямую в именованный канал Discord
 * (\\.\pipe\discord-ipc-N), без нативных библиотек.
 * Кадр: little-endian int32 opcode, int32 длина, затем JSON в UTF-8.
 */
final class Discord {
    static final String APP_ID = "1541867005316173875";
    private static final String DISCORD_URL = "https://discord.gg/AAVFGXHTBW";
    /** Хэш иконки приложения на момент написания — запасной вариант, если API недоступно. */
    private static final String FALLBACK_ICON = "f5a58e3b5e01e4b1b3eaf416f93ab339";
    private static final int OP_HANDSHAKE = 0, OP_FRAME = 1, OP_CLOSE = 2;

    private RandomAccessFile pipe;
    private volatile boolean enabled = true;
    private volatile String details = "", state = "";
    private volatile boolean dirty = true;
    private final long start = System.currentTimeMillis() / 1000L;
    private String iconUrl;
    private long lastAttempt, lastSend;
    private Thread worker;

    /** Обновляет текст статуса (можно звать из любого потока). */
    void set(String details, String state) {
        if (Objects.equals(details, this.details) && Objects.equals(state, this.state)) return;
        this.details = details;
        this.state = state;
        dirty = true;
    }

    private volatile boolean paused;

    /** Пауза на время игры: статус показывает сам клиент WiosVisuals. */
    void setPaused(boolean p) {
        paused = p;
        dirty = true;
    }

    void setEnabled(boolean on) {
        enabled = on;
        dirty = true;
    }

    boolean enabled() { return enabled; }

    synchronized void start() {
        if (worker != null) return;
        worker = new Thread(this::loop, "gl-discord-rpc");
        worker.setDaemon(true);
        worker.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    private void loop() {
        while (true) {
            try { tick(); } catch (Throwable ignored) { close(); }
            try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
        }
    }

    private void tick() throws IOException {
        long now = System.currentTimeMillis();
        if (!enabled || paused) {
            if (pipe != null) { send(null); close(); }
            return;
        }
        if (pipe == null) {
            if (now - lastAttempt < 15_000) return;
            lastAttempt = now;
            if (!connect()) return;
            dirty = true;
        }
        if (!dirty && now - lastSend < 30_000) return; // периодический «пульс», чтобы статус не протух
        dirty = false;
        lastSend = now;
        send(activity());
    }

    private boolean connect() {
        if (iconUrl == null) iconUrl = resolveIcon();
        for (int i = 0; i < 10; i++) {
            try {
                pipe = new RandomAccessFile(new File("\\\\.\\pipe\\discord-ipc-" + i), "rw");
            } catch (Exception e) {
                pipe = null;
                continue;
            }
            try {
                Map<String, Object> hs = new LinkedHashMap<>();
                hs.put("v", 1);
                hs.put("client_id", APP_ID);
                write(OP_HANDSHAKE, Json.write(hs));
                if (read()) return true; // пришёл READY
            } catch (IOException ignored) { }
            close();
        }
        return false;
    }

    /** Иконка приложения по прямой ссылке — Discord сам проксирует внешние картинки. */
    private static String resolveIcon() {
        String hash = FALLBACK_ICON;
        try {
            Map<String, Object> app = Json.obj(Json.parse(Installer.get("https://discord.com/api/v9/applications/" + APP_ID + "/rpc")));
            if (Json.str(app, "icon") != null) hash = Json.str(app, "icon");
        } catch (Exception ignored) { }
        return "https://cdn.discordapp.com/app-icons/" + APP_ID + "/" + hash + ".png?size=512";
    }

    private Map<String, Object> activity() {
        Map<String, Object> a = new LinkedHashMap<>();
        if (!details.isEmpty()) a.put("details", details);
        if (!state.isEmpty()) a.put("state", state);
        Map<String, Object> ts = new LinkedHashMap<>();
        ts.put("start", start);
        a.put("timestamps", ts);
        Map<String, Object> assets = new LinkedHashMap<>();
        assets.put("large_image", iconUrl);
        assets.put("large_text", "WiosVisuals Premium");
        a.put("assets", assets);
        Map<String, Object> btn = new LinkedHashMap<>();
        btn.put("label", "Discord сервер");
        btn.put("url", DISCORD_URL);
        a.put("buttons", List.of(btn));
        return a;
    }

    private synchronized void send(Map<String, Object> activity) throws IOException {
        if (pipe == null) return;
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("pid", ProcessHandle.current().pid());
        args.put("activity", activity);
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("cmd", "SET_ACTIVITY");
        frame.put("args", args);
        frame.put("nonce", UUID.randomUUID().toString());
        write(OP_FRAME, Json.write(frame));
        if (!read()) close();
    }

    /** Сразу убирает статус (например, когда стартует игра со своим статусом). */
    void clearNow() {
        try { send(null); } catch (Exception ignored) { }
        dirty = true;
    }

    private void write(int op, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer b = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(op).putInt(data.length).put(data);
        pipe.write(b.array());
    }

    /**
     * Канал открыт без OVERLAPPED, поэтому ответ на каждый кадр читаем сразу в этом же потоке.
     * @return false, если Discord закрыл соединение или вернул ошибку рукопожатия
     */
    private boolean read() throws IOException {
        byte[] head = new byte[8];
        pipe.readFully(head);
        ByteBuffer hb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
        int op = hb.getInt(0), len = hb.getInt(4);
        byte[] body = new byte[Math.max(0, Math.min(len, 1 << 20))];
        pipe.readFully(body);
        lastResponse = new String(body, StandardCharsets.UTF_8);
        return op != OP_CLOSE;
    }

    /** Последний ответ Discord (для отладки). */
    volatile String lastResponse = "";

    synchronized void close() {
        try { if (pipe != null) pipe.close(); } catch (IOException ignored) { }
        pipe = null;
    }
}
