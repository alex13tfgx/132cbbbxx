/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.module;

import code.wiosvisual.features.module.Module;
import java.util.List;

public class ModuleProvider {
    private final List<Module> modules;

    public <T extends Module> T get(String name) {
        return (T)((Module)this.modules.stream().filter(module -> module.getName().equalsIgnoreCase(name)).map(module -> module).findFirst().orElse(null));
    }

    public <T extends Module> T get(Class<T> clazz) {
        return (T)((Module)this.modules.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null));
    }

    public List<Module> getModules() {
        return this.modules;
    }

    public ModuleProvider(List<Module> modules) {
        this.modules = modules;
    }
}
