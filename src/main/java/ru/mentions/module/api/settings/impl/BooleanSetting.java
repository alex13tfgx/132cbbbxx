package ru.mentions.module.api.settings.impl;

import java.util.function.Supplier;
import ru.mentions.module.api.Module;

public class BooleanSetting extends code.wiosvisual.features.module.setting.implement.BooleanSetting {
    public BooleanSetting(String name, Boolean value) {
        super(name, "");
        this.setValue(value);
    }

    public BooleanSetting(Module parent, String name, Boolean value) {
        this(name, value);
        parent.setup(this);
    }

    public static BooleanSetting of(String name, Boolean value) {
        return new BooleanSetting(name, value);
    }

    public Boolean getValue() {
        return this.isValue();
    }

    public BooleanSetting set(Boolean value) {
        this.setValue(value);
        return this;
    }

    public BooleanSetting setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible);
        return this;
    }
}
