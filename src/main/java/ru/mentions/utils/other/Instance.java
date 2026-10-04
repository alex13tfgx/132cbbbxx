package ru.mentions.utils.other;

import ru.mentions.module.api.Module;

/** Доступ к модулям mentions через реестр модулей Wios. */
public final class Instance {

    private Instance() {}

    public static <T extends Module> T get(Class<T> clazz) {
        return code.wiosvisual.utils.client.Instance.get(clazz);
    }
}
