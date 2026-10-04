package ru.mentions.gui;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.features.module.Module;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Список модулей Wios для ClickGui mentions. */
public final class Features {
    private static final Map<Module, Feature> CACHE = new IdentityHashMap<>();
    private static List<Feature> all = new ArrayList<>();
    private static int knownSize = -1;

    private Features() {}

    public static synchronized List<Feature> all() {
        List<Module> modules = Wiosvisual.getInstance().getModuleRepository().modules();
        if (modules.size() != knownSize) {
            List<Feature> list = new ArrayList<>();
            for (Module module : modules) {
                if (module.getName().equals("ClickGui") || module.getName().equals("Hud")) {
                    continue;
                }
                list.add(CACHE.computeIfAbsent(module, Feature::new));
            }
            all = list;
            knownSize = modules.size();
        }
        return all.stream().filter(f -> !f.module().isHidden()).toList();
    }
}
