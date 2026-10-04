/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_5250
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package code.wiosvisual.utils.client.managers.event;

import code.wiosvisual.features.module.exception.ModuleException;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.events.Event;
import code.wiosvisual.utils.client.managers.event.events.EventStoppable;
import code.wiosvisual.utils.client.managers.event.types.Priority;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EventManager
implements QuickImports {
    private static final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new HashMap<Class<? extends Event>, List<MethodData>>();
    private static final Logger LOGGER = LogManager.getLogger((String)"Wiosvisual");

    public static void register(Object object) {
        if (!EventManager.canInspectEventHandlers(object)) {
            return;
        }
        Method[] methods = EventManager.getDeclaredEventHandlerMethods(object);
        if (methods == null) {
            return;
        }
        for (Method method : methods) {
            if (EventManager.isMethodBad(method)) continue;
            EventManager.register(method, object);
        }
    }

    public void register(Object object, Class<? extends Event> eventClass) {
        if (!EventManager.canInspectEventHandlers(object)) {
            return;
        }
        Method[] methods = EventManager.getDeclaredEventHandlerMethods(object);
        if (methods == null) {
            return;
        }
        for (Method method : methods) {
            if (EventManager.isMethodBad(method, eventClass)) continue;
            EventManager.register(method, object);
        }
    }

    public static boolean canInspectEventHandlers(Object object) {
        if (object == null) {
            return false;
        }
        try {
            object.getClass().getDeclaredMethods();
            return true;
        }
        catch (Throwable error) {
            LOGGER.error("Skipping broken event handler source {}", (Object)object.getClass().getName(), (Object)error);
            return false;
        }
    }

    private static Method[] getDeclaredEventHandlerMethods(Object object) {
        try {
            return object.getClass().getDeclaredMethods();
        }
        catch (Throwable error) {
            LOGGER.error("Failed to inspect event handlers for {}", (Object)object.getClass().getName(), (Object)error);
            return null;
        }
    }

    public void unregister(Object object) {
        for (List<MethodData> dataList : REGISTRY_MAP.values()) {
            dataList.removeIf(data -> data.source().equals(object));
        }
        EventManager.cleanMap(true);
    }

    public void unregister(Object object, Class<? extends Event> eventClass) {
        if (REGISTRY_MAP.containsKey(eventClass)) {
            REGISTRY_MAP.get(eventClass).removeIf(data -> data.source().equals(object));
            EventManager.cleanMap(true);
        }
    }

    private static void register(Method method, Object object) {
        Class<? extends Event> indexClass = (Class<? extends Event>)method.getParameterTypes()[0];
        final MethodData data = new MethodData(object, method, method.getAnnotation(EventHandler.class).value());
        if (!data.target().isAccessible()) {
            data.target().setAccessible(true);
        }
        if (REGISTRY_MAP.containsKey(indexClass)) {
            if (!REGISTRY_MAP.get(indexClass).contains(data)) {
                REGISTRY_MAP.get(indexClass).add(data);
                EventManager.sortListValue(indexClass);
            }
        } else {
            REGISTRY_MAP.put(indexClass, (List<MethodData>)new CopyOnWriteArrayList<MethodData>(){
                private static final long serialVersionUID = 666L;
                {
                    this.add(data);
                }
            });
        }
    }

    public void removeEntry(Class<? extends Event> indexClass) {
        Iterator<Map.Entry<Class<? extends Event>, List<MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();
        while (mapIterator.hasNext()) {
            if (!mapIterator.next().getKey().equals(indexClass)) continue;
            mapIterator.remove();
            break;
        }
    }

    public static void cleanMap(boolean onlyEmptyEntries) {
        Iterator<Map.Entry<Class<? extends Event>, List<MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();
        while (mapIterator.hasNext()) {
            if (onlyEmptyEntries && !mapIterator.next().getValue().isEmpty()) continue;
            mapIterator.remove();
        }
    }

    private static void sortListValue(Class<? extends Event> indexClass) {
        CopyOnWriteArrayList<MethodData> sortedList = new CopyOnWriteArrayList<MethodData>();
        for (byte priority : Priority.VALUE_ARRAY) {
            for (MethodData data : REGISTRY_MAP.get(indexClass)) {
                if (data.priority() != priority) continue;
                sortedList.add(data);
            }
        }
        REGISTRY_MAP.put(indexClass, sortedList);
    }

    private static boolean isMethodBad(Method method) {
        return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventHandler.class);
    }

    private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
        return EventManager.isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
    }

    public static Event callEvent(Event event) {
        try {
            return EventManager.callEventInternal(event);
        }
        catch (LinkageError | VirtualMachineError error) {
            LOGGER.warn("Skipped event dispatch during class reload: {}", (Object)(event != null ? event.getClass().getName() : "null"), (Object)error);
            return event;
        }
        catch (Throwable error) {
            LOGGER.warn("Event dispatch failed safely: {}", (Object)(event != null ? event.getClass().getName() : "null"), (Object)error);
            return event;
        }
    }

    private static Event callEventInternal(Event event) {
        List<MethodData> dataList = REGISTRY_MAP.get(event.getClass());
        if (dataList != null) {
            for (MethodData data : dataList) {
                EventStoppable stoppable;
                EventManager.invoke(data, event);
                if (!(event instanceof EventStoppable) || !(stoppable = (EventStoppable)event).isStopped()) continue;
                break;
            }
        }
        return event;
    }

    private static void invoke(MethodData data, Event argument) {
        try {
            data.target().invoke(data.source(), argument);
        }
        catch (IllegalAccessException e) {
            LOGGER.error("Illegal access to event handler {} on {} for {}", (Object)data.target().getName(), (Object)data.source().getClass().getName(), (Object)argument, (Object)e);
        }
        catch (IllegalArgumentException e) {
            LOGGER.error("Illegal arguments for event handler {} on {} for {}", (Object)data.target().getName(), (Object)data.source().getClass().getName(), (Object)argument, (Object)e);
        }
        catch (InvocationTargetException e) {
            Throwable cause;
            Throwable throwable = cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ModuleException) {
                ModuleException moduleException = (ModuleException)cause;
                EventManager.logModuleException(moduleException);
            } else {
                LOGGER.error("Exception in event handler {} on {} for {}", (Object)data.target().getName(), (Object)data.source().getClass().getName(), (Object)argument, (Object)cause);
            }
        }
        catch (Throwable t) {
            LOGGER.error("Unexpected error in event handler {} on {} for {}", (Object)data.target().getName(), (Object)data.source().getClass().getName(), (Object)argument, (Object)t);
        }
    }

    private static void logModuleException(ModuleException moduleException) {
        MutableText message = Text.literal((String)("[" + moduleException.getModuleName() + "] ")).append((Text)Text.literal((String)moduleException.getMessage()).formatted(Formatting.RED));
        if (mc != null && EventManager.mc.player != null && EventManager.mc.inGameHud != null) {
            EventManager.mc.inGameHud.getChatHud().addMessage((Text)message);
        } else {
            LOGGER.warn("[{}] {}", (Object)moduleException.getModuleName(), (Object)moduleException.getMessage());
        }
    }

    public static boolean hasListeners(Class<? extends Event> eventClass) {
        List<MethodData> listeners = REGISTRY_MAP.get(eventClass);
        return listeners != null && !listeners.isEmpty();
    }

    private record MethodData(Object source, Method target, byte priority) {
    }
}
