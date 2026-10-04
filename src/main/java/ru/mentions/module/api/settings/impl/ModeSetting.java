package ru.mentions.module.api.settings.impl;

import java.util.function.Supplier;
import ru.mentions.module.api.Module;

public class ModeSetting extends code.wiosvisual.features.module.setting.implement.SelectSetting {
    public ModeSetting(Module parent, String name, String... values) {
        super(name, "");
        this.value(values);
        this.selected(values[0]);
        parent.setup(this);
    }

    public boolean is(String value) {
        return this.isSelected(value);
    }

    public String getValue() {
        return this.getSelected();
    }

    public ModeSetting set(String value) {
        this.setSelected(value);
        return this;
    }

    public ModeSetting setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible);
        return this;
    }
}
