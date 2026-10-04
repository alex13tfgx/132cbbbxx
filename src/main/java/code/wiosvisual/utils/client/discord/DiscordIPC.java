package code.wiosvisual.utils.client.discord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Минимальный клиент Discord IPC: пишет кадры напрямую в локальный сокет Discord,
 * без нативных библиотек и внешних зависимостей.
 *
 * Формат кадра: little-endian int32 opcode, int32 длина payload, затем JSON в UTF-8.
 */
public final class DiscordIPC {
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int PIPE_COUNT = 10;
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger("Wiosvisual");

    /** Windows: именованный канал. */
    private RandomAccessFile pipe;
    /** Linux/macOS: unix-сокет. */
    private SocketChannel socket;
    private Thread reader;
    private volatile boolean connected;
    private volatile boolean ready;

    public boolean isConnected() {
        return this.connected;
    }

    /** Discord ответил READY: до этого момента SET_ACTIVITY игнорируется. */
    public boolean isReady() {
        return this.ready;
    }

    public synchronized boolean connect(String clientId) {
        if (this.connected) {
            return true;
        }
        if (clientId == null || clientId.isBlank()) {
            return false;
        }
        this.ready = false;
        for (int i = 0; i < PIPE_COUNT; ++i) {
            if (!this.open(i)) continue;
            try {
                LOGGER.info("Discord IPC: канал discord-ipc-{} открыт, отправляю handshake", (Object)i);
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", clientId.trim());
                this.writeFrame(OP_HANDSHAKE, GSON.toJson(handshake));
                this.connected = true;
                if (this.pipe != null) {
                    this.readResponse();
                } else {
                    this.startReader();
                }
                return true;
            } catch (IOException error) {
                LOGGER.warn("Discord IPC: handshake на discord-ipc-{} не прошёл", (Object)i, (Object)error);
                this.closeStreams();
            }
        }
        LOGGER.info("Discord IPC: не нашёл запущенный Discord (проверено {} каналов)", (Object)PIPE_COUNT);
        return false;
    }

    public synchronized void sendActivity(JsonObject activity) {
        if (!this.connected) {
            return;
        }
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        if (activity == null) {
            args.add("activity", null);
        } else {
            args.add("activity", activity);
        }
        JsonObject frame = new JsonObject();
        frame.addProperty("cmd", "SET_ACTIVITY");
        frame.add("args", args);
        frame.addProperty("nonce", UUID.randomUUID().toString());
        try {
            this.writeFrame(OP_FRAME, GSON.toJson(frame));
            if (this.pipe != null) {
                this.readResponse();
            }
        } catch (IOException error) {
            this.close();
        }
    }

    public synchronized void close() {
        if (this.connected) {
            try {
                this.writeFrame(OP_CLOSE, "{}");
            } catch (IOException ignored) {
                // Discord мог закрыть канал первым — это нормально.
            }
        }
        this.connected = false;
        this.ready = false;
        this.closeStreams();
    }

    private boolean open(int index) {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                File file = new File("\\\\.\\pipe\\discord-ipc-" + index);
                this.pipe = new RandomAccessFile(file, "rw");
                return true;
            }
            Path path = Path.of(DiscordIPC.unixSocketDir(), "discord-ipc-" + index);
            if (!path.toFile().exists()) {
                return false;
            }
            this.socket = SocketChannel.open(StandardProtocolFamily.UNIX);
            this.socket.connect(UnixDomainSocketAddress.of(path));
            return true;
        } catch (Throwable error) {
            this.closeStreams();
            return false;
        }
    }

    private static String unixSocketDir() {
        for (String key : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) continue;
            return value;
        }
        return "/tmp";
    }

    private void writeFrame(int opcode, String payload) throws IOException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode);
        buffer.putInt(data.length);
        buffer.put(data);
        if (this.pipe != null) {
            this.pipe.write(buffer.array());
            return;
        }
        if (this.socket != null) {
            ByteBuffer out = ByteBuffer.wrap(buffer.array());
            while (out.hasRemaining()) {
                this.socket.write(out);
            }
            return;
        }
        throw new IOException("Discord IPC is not open");
    }

    /**
     * Ответы Discord нужно вычитывать, иначе буфер канала переполнится и запись повиснет.
     * Содержимое не используется — важен только факт закрытия соединения.
     */
    private void startReader() {
        this.reader = new Thread(() -> {
            byte[] header = new byte[8];
            try {
                while (this.connected) {
                    if (!this.readFully(header)) break;
                    int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
                    int opcode = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
                    byte[] body = new byte[Math.max(0, length)];
                    if (length > 0 && !this.readFully(body)) break;
                    if (opcode == OP_CLOSE) break;
                    if (length > 0) {
                        this.handleFrame(new String(body, StandardCharsets.UTF_8));
                    }
                }
            } catch (Throwable error) {
                LogManager.getLogger("Wiosvisual").debug("Discord IPC reader stopped", error);
            }
            this.connected = false;
            this.ready = false;
            this.closeStreams();
            LOGGER.info("Discord IPC: соединение закрыто");
        }, "Wiosvisual-DiscordIPC");
        this.reader.setDaemon(true);
        this.reader.start();
    }

    /**
     * Windows: именованный канал открыт без OVERLAPPED, поэтому висящий в другом потоке
     * read намертво блокирует write. Ответ на каждый свой кадр вычитываем тут же, в том
     * же потоке — заодно канал не забивается ответами Discord.
     */
    private void readResponse() {
        byte[] header = new byte[8];
        try {
            if (!this.readFully(header)) {
                this.connected = false;
                this.ready = false;
                this.closeStreams();
                return;
            }
            int opcode = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
            int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
            byte[] body = new byte[Math.max(0, length)];
            if (length > 0 && !this.readFully(body)) {
                this.connected = false;
                this.ready = false;
                this.closeStreams();
                return;
            }
            if (opcode == OP_CLOSE) {
                LOGGER.info("Discord IPC: Discord закрыл соединение — {}", (Object)new String(body, StandardCharsets.UTF_8));
                this.connected = false;
                this.ready = false;
                this.closeStreams();
                return;
            }
            if (length > 0) {
                this.handleFrame(new String(body, StandardCharsets.UTF_8));
            }
        }
        catch (IOException error) {
            LOGGER.warn("Discord IPC: не смог прочитать ответ", (Object)error);
            this.connected = false;
            this.ready = false;
            this.closeStreams();
        }
    }

    /** Из ответов нам нужны событие READY и профиль владельца Discord из него. */
    private void handleFrame(String payload) {
        try {
            JsonObject frame = GSON.fromJson(payload, JsonObject.class);
            if (frame != null && frame.has("evt") && !frame.get("evt").isJsonNull()
                    && "READY".equals(frame.get("evt").getAsString())) {
                this.ready = true;
                LOGGER.info("Discord IPC: READY, презенс можно отправлять");
                this.readUser(frame);
            }
        }
        catch (Throwable ignored) {
            // Мусор в кадре не должен ронять читающий поток.
        }
    }

    /** READY несёт профиль запущенного Discord: оттуда берём аватарку для GUI. */
    private void readUser(JsonObject frame) {
        try {
            JsonObject data = frame.getAsJsonObject("data");
            if (data == null) {
                return;
            }
            JsonObject user = data.getAsJsonObject("user");
            if (user == null) {
                return;
            }
            String id = string(user, "id");
            String avatar = string(user, "avatar");
            String name = string(user, "global_name");
            if (name.isEmpty()) {
                name = string(user, "username");
            }
            DiscordAvatar.onUser(id, avatar, name);
        }
        catch (Throwable ignored) {
            // Профиль — не критичная часть: без него просто не будет аватарки.
        }
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private boolean readFully(byte[] target) throws IOException {
        if (this.pipe != null) {
            int read = 0;
            while (read < target.length) {
                int count = this.pipe.read(target, read, target.length - read);
                if (count == -1) {
                    return false;
                }
                read += count;
            }
            return true;
        }
        if (this.socket == null) {
            return false;
        }
        ByteBuffer buffer = ByteBuffer.wrap(target);
        while (buffer.hasRemaining()) {
            if (this.socket.read(buffer) == -1) {
                return false;
            }
        }
        return true;
    }

    private void closeStreams() {
        try {
            if (this.pipe != null) {
                this.pipe.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException ignored) {
        }
        this.pipe = null;
        this.socket = null;
    }
}
