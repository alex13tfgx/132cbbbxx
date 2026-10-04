package ru.mentions.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BindSetting extends GSetting {
    public final boolean allowMouse = true;
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;

    public BindSetting(String name, Supplier<Integer> getter, Consumer<Integer> setter, Supplier<Boolean> visible) {
        super(name, visible);
        this.getter = getter;
        this.setter = setter;
    }

    public int get() {
        return this.getter.get();
    }

    public void set(int key) {
        this.setter.accept(key);
    }
}
