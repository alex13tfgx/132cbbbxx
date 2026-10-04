package ru.mentions.module.api.settings.impl;

import java.util.function.Supplier;
import ru.mentions.module.api.Module;

public class SliderSetting extends code.wiosvisual.features.module.setting.implement.SliderSettings {
    public SliderSetting(Module parent, String name, float value, float min, float max, float increment) {
        super(name, "");
        this.range(min, max);
        this.step(increment);
        this.setValue(value);
        parent.setup(this);
    }

    public SliderSetting set(Float value) {
        this.setValue(value);
        return this;
    }

    public SliderSetting setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible);
        return this;
    }
}
