/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.events.chat;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;

public class TabCompleteEvent
extends EventCancellable {
    public final String prefix;
    public String[] completions;

    public TabCompleteEvent(String prefix) {
        this.prefix = prefix;
        this.completions = null;
    }
}
