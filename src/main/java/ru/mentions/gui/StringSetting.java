package ru.mentions.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringSetting extends GSetting {
    private final Supplier<String> getter;
    private final Consumer<String> setter;

    public StringSetting(String name, Supplier<String> getter, Consumer<String> setter, Supplier<Boolean> visible) {
        super(name, visible);
        this.getter = getter;
        this.setter = setter;
    }

    public String getValue() {
        String value = this.getter.get();
        return value == null ? "" : value;
    }

    public void set(String value) {
        this.setter.accept(value);
    }

    public boolean isOnlyNumber() {
        return false;
    }
}
