package ru.mentions.gui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModeSetting extends GSetting {
    private final Supplier<List<String>> values;
    private final Supplier<String> getter;
    private final Consumer<String> setter;

    public ModeSetting(String name, Supplier<List<String>> values, Supplier<String> getter, Consumer<String> setter, Supplier<Boolean> visible) {
        super(name, visible);
        this.values = values;
        this.getter = getter;
        this.setter = setter;
    }

    public List<String> values() {
        List<String> list = this.values.get();
        return list == null ? List.of() : list;
    }

    public String getValue() {
        return this.getter.get();
    }

    public void set(String value) {
        this.setter.accept(value);
    }
}
