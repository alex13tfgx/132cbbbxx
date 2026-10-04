package ru.mentions.module.api.settings.impl;

import java.util.function.Supplier;
import ru.mentions.module.api.Module;

public class ColorSetting extends code.wiosvisual.features.module.setting.implement.ColorSetting {
    public ColorSetting(Module parent, String name) {
        this(parent, name, 0xFFFFFFFF);
    }

    public ColorSetting(Module parent, String name, Integer value) {
        super(name, "");
        this.value(value);
        parent.setup(this);
    }

    public Integer getValue() {
        return this.getColor();
    }

    public ColorSetting set(Integer value) {
        this.setColor(value);
        return this;
    }

    public ColorSetting setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible);
        return this;
    }
}
