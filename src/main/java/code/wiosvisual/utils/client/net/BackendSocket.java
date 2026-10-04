package code.wiosvisual.utils.client.net;

import code.wiosvisual.utils.client.logs.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

/** Тонкая обёртка над websocket-каналом бэкенда: онлайн-список и чат. */
public class BackendSocket extends WebSocketClient {
    private final Consumer<String> messageConsumer;
    private final Runnable onReady;
    private volatile boolean connecting = true;

    public BackendSocket(URI serverUri, Consumer<String> messageConsumer, Runnable onReady) {
        super(serverUri);
        this.messageConsumer = messageConsumer;
        this.onReady = onReady;
        this.setConnectionLostTimeout(45);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        this.connecting = false;
        Logger.info("[Backend] соединение установлено");
        if (this.onReady != null) {
            this.onReady.run();
        }
    }

    @Override
    public void onMessage(String message) {
        this.messageConsumer.accept(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.connecting = false;
        Logger.info("[Backend] соединение закрыто: " + (reason == null || reason.isBlank() ? code : reason));
    }

    @Override
    public void onError(Exception ex) {
        this.connecting = false;
        Logger.warn("[Backend] ошибка соединения: " + ex.getMessage());
    }

    public boolean isConnectingState() {
        return this.connecting && !this.isOpen();
    }
}
