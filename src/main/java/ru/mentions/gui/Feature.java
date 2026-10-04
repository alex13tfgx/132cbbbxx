package ru.mentions.gui;

import code.wiosvisual.display.screens.clickgui.ModuleDescriptions;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.setting.Setting;
import code.wiosvisual.features.module.setting.implement.GroupSetting;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.RadioSetting;
import code.wiosvisual.features.module.setting.implement.SectionSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.server.ServerRestrictions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ru.mentions.utils.animation.satoshi.Animation;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;

/** Модуль Wios в виде, который ожидает ClickGui mentions: анимации карточки, описание, категория и список настроек. */
public final class Feature {
    private final Module module;
    private final GuiCategory category;
    private List<GSetting> settings;
    public final Animation animation1 = new EaseInOutQuad(300, 1);
    public final Animation animation3 = new EaseInOutQuad(300, 1);
    private final Animation animation12 = new EaseInOutQuad(300, 1);
    private final Animation animation14 = new EaseInOutQuad(300, 1);
    private final Animation animation16 = new EaseInOutQuad(300, 1);

    Feature(Module module) {
        this.module = module;
        this.category = GuiCategory.of(module.getCategory());
    }

    public Module module() {
        return this.module;
    }

    public String getName() {
        return this.module.getName();
    }

    public String getBigName() {
        return this.module.getVisibleName();
    }

    public String getDesc() {
        String description = ModuleDescriptions.getDescription(this.module);
        return description == null ? "" : description;
    }

    public GuiCategory getCategory() {
        return this.category;
    }

    public boolean isEnabled() {
        return this.module.isState();
    }

    public void setEnabled(boolean enabled) {
        this.module.setState(enabled);
    }

    public int getKey() {
        return this.module.getKey();
    }

    public void setKey(int key) {
        this.module.setKey(key);
    }

    public Animation getAnimation12() {
        return this.animation12;
    }

    public Animation getAnimation14() {
        return this.animation14;
    }

    public Animation getAnimation16() {
        return this.animation16;
    }

    public synchronized List<GSetting> getSettings() {
        if (this.settings == null) {
            List<GSetting> built = new ArrayList<>();
            for (Setting setting : this.module.settings()) {
                adapt(setting, null, built);
            }
            this.settings = built;
        }
        return this.settings;
    }

    private static java.util.function.Supplier<Boolean> visible(Setting setting, java.util.function.Supplier<Boolean> parent) {
        return () -> setting.isVisible() && (parent == null || parent.get());
    }

    private static void adapt(Setting setting, java.util.function.Supplier<Boolean> parent, List<GSetting> out) {
        java.util.function.Supplier<Boolean> vis = visible(setting, parent);
        String name = setting.getName();
        if (setting instanceof SliderSettings slider) {
            float step = slider.isInteger() ? 1.0f : (slider.getStep() > 0.0f ? slider.getStep() : 0.01f);
            out.add(new SliderSetting(name, slider.getMin(), slider.getMax(), step, slider::getValue, value -> slider.setValue(value), vis));
        } else if (setting instanceof SelectSetting select) {
            out.add(new ModeSetting(name, select::getList, select::getSelected, select::setSelected, vis));
        } else if (setting instanceof RadioSetting radio) {
            out.add(new ModeSetting(name, () -> Arrays.asList(radio.getOptions()), radio::get, radio::set, vis));
        } else if (setting instanceof MultiSelectSetting multi) {
            List<BooleanSetting> chips = new ArrayList<>();
            for (String option : multi.getList()) {
                chips.add(new BooleanSetting(option, () -> multi.isSelected(option), value -> {
                    List<String> selected = new ArrayList<>(multi.getSelected());
                    selected.remove(option);
                    if (value) {
                        selected.add(option);
                    }
                    multi.setSelected(selected);
                }, () -> true));
            }
            out.add(new MultiBooleanSetting(name, chips, vis, Feature::optionForbidden));
        } else if (setting instanceof code.wiosvisual.features.module.setting.implement.BooleanSetting bool) {
            java.util.function.Supplier<Boolean> allowed = () -> vis.get() && !settingForbidden(name);
            out.add(new BooleanSetting(name, bool::isValue, bool::setValue, allowed));
        } else if (setting instanceof code.wiosvisual.features.module.setting.implement.ButtonSetting button) {
            out.add(new ButtonSetting(button.getButtonName() != null ? button.getButtonName() : name, () -> { if (button.getRunnable() != null) button.getRunnable().run(); }, vis));
        } else if (setting instanceof code.wiosvisual.features.module.setting.implement.BindSetting bind) {
            out.add(new BindSetting(name, bind::getKey, bind::setKey, vis));
        } else if (setting instanceof TextSetting text) {
            out.add(new StringSetting(name, text::getText, text::setText, vis));
        } else if (setting instanceof code.wiosvisual.features.module.setting.implement.ColorSetting color) {
            out.add(new ColorSetting(name, color::getColor, color::setColor, vis));
        } else if (setting instanceof GroupSetting group) {
            out.add(new BooleanSetting(name, group::isValue, group::setValue, vis));
            for (Setting sub : group.getSubSettings()) {
                adapt(sub, () -> vis.get() && group.isValue(), out);
            }
        } else if (setting instanceof SectionSetting section) {
            out.add(new BooleanSetting(name, section::isOn, section::setOn, vis));
            for (Setting sub : section.getSubSettings()) {
                adapt(sub, () -> vis.get() && section.isOn(), out);
            }
        }
    }

    /** Пункты NoRender, запрещённые правилами серверов (ServerRestrictions.Feature). */
    private static boolean optionForbidden(String option) {
        return switch (option) {
            case "\u041f\u043b\u043e\u0445\u0438\u0435 \u044d\u0444\u0444\u0435\u043a\u0442\u044b" -> ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.REMOVE_BAD_EFFECTS);
            case "\u0422\u044c\u043c\u0430" -> ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.REMOVE_DARKNESS);
            case "\u0420\u0430\u0441\u0442\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c" -> ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.REMOVE_VEGETATION);
            default -> false;
        };
    }

    /** Отдельные настройки, запрещённые правилами серверов. */
    private static boolean settingForbidden(String name) {
        return "Подсвечивать друзей".equals(name) && ServerRestrictions.isForbiddenHere(ServerRestrictions.Feature.FRIEND_HIGHLIGHT);
    }
}
