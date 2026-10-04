package ru.mentions.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanSetting extends GSetting {
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public BooleanSetting(String name, Supplier<Boolean> getter, Consumer<Boolean> setter, Supplier<Boolean> visible) {
        super(name, visible);
        this.getter = getter;
        this.setter = setter;
    }

    public boolean getValue() {
        return this.getter.get();
    }

    public void set(boolean value) {
        this.setter.accept(value);
    }
}
