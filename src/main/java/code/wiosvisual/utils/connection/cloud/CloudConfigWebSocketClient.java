/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.connection.cloud;

import code.wiosvisual.utils.client.logs.Logger;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class CloudConfigWebSocketClient
extends WebSocketClient {
    private String lastResponse;
    private CountDownLatch responseLatch;

    public CloudConfigWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Logger.info("WebSocket connection opened");
    }

    @Override
    public void onMessage(String message) {
        this.lastResponse = message;
        if (this.responseLatch != null) {
            this.responseLatch.countDown();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Logger.info("WebSocket connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Logger.error("WebSocket error: " + ex.getMessage());
    }

    public String sendAndWaitForResponse(String message) {
        this.responseLatch = new CountDownLatch(1);
        this.lastResponse = null;
        this.send(message);
        try {
            this.responseLatch.await(5L, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Logger.error("Interrupted while waiting for WebSocket response: " + e.getMessage());
        }
        return this.lastResponse;
    }
}
