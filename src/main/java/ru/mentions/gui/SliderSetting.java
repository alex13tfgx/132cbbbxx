package ru.mentions.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SliderSetting extends GSetting {
    public final float min;
    public final float max;
    public final float increment;
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;

    public SliderSetting(String name, float min, float max, float increment, Supplier<Float> getter, Consumer<Float> setter, Supplier<Boolean> visible) {
        super(name, visible);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.getter = getter;
        this.setter = setter;
    }

    public float getValue() {
        return this.getter.get();
    }

    public void set(float value) {
        this.setter.accept(value);
    }
}
