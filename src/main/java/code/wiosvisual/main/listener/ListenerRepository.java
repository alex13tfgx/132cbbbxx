/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.main.listener;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.emotions.EmotionWheelManager;
import code.wiosvisual.irc.IrcManager;
import code.wiosvisual.main.listener.Listener;
import code.wiosvisual.main.listener.impl.EventListener;
import code.wiosvisual.utils.client.managers.event.EventManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListenerRepository {
    private final List<Listener> listeners = new ArrayList<Listener>();

    public void setup() {
        this.registerListeners(new EventListener(), EmotionWheelManager.create(), IrcManager.create());
    }

    public void registerListeners(Listener ... listeners) {
        this.listeners.addAll(List.of(listeners));
        Arrays.stream(listeners).forEach(listener -> {
            Wiosvisual.getInstance().getEventManager();
            EventManager.register(listener);
        });
    }

    public List<Listener> getListeners() {
        return this.listeners;
    }
}
