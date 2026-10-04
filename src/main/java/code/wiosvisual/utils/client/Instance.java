/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleProvider;
import code.wiosvisual.features.module.ModuleRepository;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Instance {
    private static final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<Class<? extends Module>, Module>();
    private static final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<Class<? extends AbstractDraggable>, AbstractDraggable>();

    public static <T extends Module> T get(Class<T> clazz) {
        Module cachedModule = instanceModules.get(clazz);
        if (cachedModule != null) {
            return (T)clazz.cast(cachedModule);
        }
        return (T)((Module)clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> {
            Wiosvisual wiosvisual = Wiosvisual.getInstance();
            if (wiosvisual == null) {
                return null;
            }
            ModuleProvider moduleProvider = wiosvisual.getModuleProvider();
            if (moduleProvider != null) {
                return moduleProvider.get(instance);
            }
            ModuleRepository moduleRepository = wiosvisual.getModuleRepository();
            if (moduleRepository != null) {
                return moduleRepository.modules().stream().filter(m -> instance.isAssignableFrom(m.getClass())).map(instance::cast).findFirst().orElse(null);
            }
            return null;
        })));
    }

    public static <T extends Module> T get(String module) {
        Wiosvisual wiosvisual = Wiosvisual.getInstance();
        if (wiosvisual == null) {
            return null;
        }
        ModuleProvider moduleProvider = wiosvisual.getModuleProvider();
        if (moduleProvider != null) {
            return moduleProvider.get(module);
        }
        ModuleRepository moduleRepository = wiosvisual.getModuleRepository();
        if (moduleRepository != null) {
            return (T)((Module)moduleRepository.modules().stream().filter(m -> m.getName().equalsIgnoreCase(module)).findFirst().orElse(null));
        }
        return null;
    }

    public static <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
        return (T)((AbstractDraggable)clazz.cast(instanceDraggables.computeIfAbsent(clazz, instance -> Wiosvisual.getInstance().getDraggableRepository().get(instance))));
    }

    public static <T extends AbstractDraggable> T getDraggable(String draggable) {
        return Wiosvisual.getInstance().getDraggableRepository().get(draggable);
    }

    private Instance() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
