package ru.mentions.module.api.settings.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import ru.mentions.module.api.Module;

public class MultiBooleanSetting extends code.wiosvisual.features.module.setting.implement.MultiSelectSetting {
    public MultiBooleanSetting(Module parent, String name, BooleanSetting... values) {
        super(name, "");
        List<String> names = new ArrayList<>();
        List<String> on = new ArrayList<>();
        for (BooleanSetting setting : values) {
            names.add(setting.getName());
            if (setting.getValue()) {
                on.add(setting.getName());
            }
        }
        this.value(names.toArray(new String[0]));
        this.selected(on.toArray(new String[0]));
        parent.setup(this);
    }

    public boolean getValue(String name) {
        return this.isSelected(name);
    }

    public MultiBooleanSetting setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible);
        return this;
    }
}
