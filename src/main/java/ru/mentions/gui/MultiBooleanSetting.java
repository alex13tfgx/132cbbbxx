package ru.mentions.gui;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MultiBooleanSetting extends GSetting {
    private final List<BooleanSetting> values;
    private final Predicate<String> forbidden;

    public MultiBooleanSetting(String name, List<BooleanSetting> values, Supplier<Boolean> visible, Predicate<String> forbidden) {
        super(name, visible);
        this.values = values;
        this.forbidden = forbidden;
    }

    /** Пункты, разрешённые на текущем сервере: запрещённые правилами ServerRestrictions не показываются. */
    public List<BooleanSetting> getValues() {
        return this.values.stream().filter(chip -> !this.forbidden.test(chip.getName())).toList();
    }
}
