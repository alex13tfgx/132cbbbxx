package ru.mentions.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.mentions.utils.animation.satoshi.Animation;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;

public class ColorSetting extends GSetting {
    public boolean pickerOpen;
    public final Animation pickerAnim = new EaseInOutQuad(220, 1);
    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;

    public ColorSetting(String name, Supplier<Integer> getter, Consumer<Integer> setter, Supplier<Boolean> visible) {
        super(name, visible);
        this.getter = getter;
        this.setter = setter;
    }

    public int getValue() {
        return this.getter.get();
    }

    public void set(int color) {
        this.setter.accept(color);
    }
}
