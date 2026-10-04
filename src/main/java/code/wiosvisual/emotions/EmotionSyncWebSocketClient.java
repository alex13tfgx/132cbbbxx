/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.emotions;

import code.wiosvisual.utils.client.logs.Logger;
import java.net.URI;
import java.util.function.Consumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class EmotionSyncWebSocketClient
extends WebSocketClient {
    private final Consumer<String> messageConsumer;
    private volatile boolean connecting = true;

    public EmotionSyncWebSocketClient(URI serverUri, Consumer<String> messageConsumer) {
        super(serverUri);
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        this.connecting = false;
        Logger.info("Emotion sync connected");
    }

    @Override
    public void onMessage(String message) {
        this.messageConsumer.accept(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.connecting = false;
        Logger.info("Emotion sync closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        this.connecting = false;
        Logger.error("Emotion sync error: " + ex.getMessage());
    }

    public boolean isConnectingState() {
        return this.connecting;
    }
}
