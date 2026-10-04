/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package code.wiosvisual.utils.client.managers.event;

import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.client.managers.event.events.Event;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SafeEvents {
    private static final Logger LOGGER = LogManager.getLogger((String)"Wiosvisual");

    private SafeEvents() {
    }

    public static void run(Runnable action) {
        try {
            action.run();
        }
        catch (Throwable error) {
            if (error instanceof LinkageError || error instanceof VirtualMachineError) {
                LOGGER.warn("Skipped mixin event hook during class reload: {}", (Object)error.toString());
            }
            LOGGER.warn("Mixin event hook failed safely", error);
        }
    }

    public static <E extends Event> E dispatch(Supplier<E> factory) {
        try {
            Event event = (Event)factory.get();
            if (event != null) {
                EventManager.callEvent(event);
            }
            return (E)event;
        }
        catch (Throwable error) {
            if (error instanceof LinkageError || error instanceof VirtualMachineError) {
                LOGGER.warn("Skipped event dispatch during class reload: {}", (Object)error.toString());
            } else {
                LOGGER.warn("Event dispatch failed safely", error);
            }
            return null;
        }
    }

    public static void dispatch(Event event) {
        if (event == null) {
            return;
        }
        SafeEvents.run(() -> EventManager.callEvent(event));
    }

    public static boolean hasListeners(Class<? extends Event> eventClass) {
        try {
            return EventManager.hasListeners(eventClass);
        }
        catch (Throwable error) {
            return false;
        }
    }
}
