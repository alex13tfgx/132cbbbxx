/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.Instance;
import java.util.ArrayList;
import java.util.List;

public class Hud
extends Module {
    public static BooleanSetting slotCooldowns = new BooleanSetting("\u041a\u0414 \u0432 \u0441\u043b\u043e\u0442\u0430\u0445", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u043f\u0435\u0440\u0435\u0437\u0430\u0440\u044f\u0434\u043a\u0443 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430 \u043f\u0440\u044f\u043c\u043e \u0432 \u0441\u043b\u043e\u0442\u0435 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f").setValue(true);
    public static BooleanSetting watermarkStatus = new BooleanSetting("\u0421\u0442\u0430\u0442\u0443\u0441\u044b \u0432 \u0432\u0430\u0442\u0435\u0440\u043c\u0430\u0440\u043a\u0435", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f \u0432 \u0432\u0430\u0442\u0435\u0440\u043c\u0430\u0440\u043a\u0435").setValue(false);
    public static MultiSelectSetting interfaceSettings = new MultiSelectSetting("\u042d\u043b\u0435\u043c\u0435\u043d\u0442\u044b", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u044d\u043b\u0435\u043c\u0435\u043d\u0442\u043e\u0432 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430").value("Watermark", "Target Hud", "Potions", "Cooldowns", "Music Info", "Notifications", "Keybinds", "ArmorBar", "Броня", "TotemBar", "CremKeys", "CustomHotbar", "Inventory", "Online").selected("Watermark", "Target Hud", "Potions", "Notifications", "Броня");
    public static MultiSelectSetting watermarkElements = new MultiSelectSetting("Элементы ватермарки", "Что показывать в ватермарке").value("Лого", "Название", "Ник", "Сервер", "FPS", "Пинг").selected("Лого", "Название", "Ник", "Сервер", "FPS", "Пинг").visible(() -> interfaceSettings.isSelected("Watermark"));
    public static BooleanSetting musicKaraoke =new BooleanSetting("Караоке", "Показывать синхронный текст песни в Music Info").setValue(true).visible(() -> interfaceSettings.isSelected("Music Info"));
    public MultiSelectSetting notificationSettings = new MultiSelectSetting("\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u044f\u0442\u044c \u043e\u0431", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435, \u043a\u043e\u0433\u0434\u0430 \u0431\u0443\u0434\u0443\u0442 \u043f\u043e\u044f\u0432\u043b\u044f\u0442\u044c\u0441\u044f \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f").value("\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043c\u043e\u0434\u0443\u043b\u0435\u0439", "\u041f\u043e\u0434\u043d\u044f\u0442\u0438\u0438 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430", "\u041b\u043e\u043c\u0430\u043d\u0438\u0438 \u0449\u0438\u0442\u0430", "\u041f\u043e\u043b\u0443\u0447\u0435\u043d\u043d\u043e\u043c \u0437\u0435\u043b\u044c\u0435", "\u0417\u0430\u043a\u0430\u043d\u0447\u0438\u0432\u0430\u044e\u0449\u0435\u043c\u0441\u044f \u0437\u0435\u043b\u044c\u0435", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0438 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430", "\u0421\u043d\u043e\u0441\u0435 \u0442\u043e\u0442\u0435\u043c\u0430", "\u0421\u043a\u043e\u0440\u043e\u0439 \u043f\u043e\u043b\u043e\u043c\u043a\u0435 \u0431\u0440\u043e\u043d\u0438", "\u0421\u043a\u043e\u0440\u043e\u043c \u043e\u0442\u043a\u0440\u044b\u0442\u0438\u0438 \u0441\u0443\u043d\u0434\u0443\u043a\u0430").selected("\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043c\u043e\u0434\u0443\u043b\u0435\u0439", "\u041f\u043e\u0434\u043d\u044f\u0442\u0438\u0438 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430", "\u041b\u043e\u043c\u0430\u043d\u0438\u0438 \u0449\u0438\u0442\u0430", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0438 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430", "\u0421\u043d\u043e\u0441\u0435 \u0442\u043e\u0442\u0435\u043c\u0430", "\u0421\u043a\u043e\u0440\u043e\u0439 \u043f\u043e\u043b\u043e\u043c\u043a\u0435 \u0431\u0440\u043e\u043d\u0438", "\u0421\u043a\u043e\u0440\u043e\u043c \u043e\u0442\u043a\u0440\u044b\u0442\u0438\u0438 \u0441\u0443\u043d\u0434\u0443\u043a\u0430").visible(() -> interfaceSettings.isSelected("Notifications"));

    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public Hud() {
        super("Hud", "HUD", ModuleCategory.RENDER);
        this.setup(interfaceSettings, watermarkElements, musicKaraoke, watermarkStatus, this.notificationSettings, slotCooldowns);
        this.setHidden(true);
    }

    /**
     * Старый HUD Wios отключён: его заменяет InterFace. Модуль остаётся в реестре только для того,
     * чтобы остальной код, обращающийся к Hud.getInstance(), не падал; включить его нельзя.
     */
    @Override
    public void setState(boolean state) {
        super.setState(false);
    }

    public boolean isHudElementEnabled(String elementName) {
        return interfaceSettings.getList() != null && interfaceSettings.getList().contains(elementName) && interfaceSettings.isSelected(elementName);
    }

    public void setHudElementEnabled(String elementName, boolean enabled) {
        List<String> values = interfaceSettings.getList();
        if (values == null || !values.contains(elementName)) {
            return;
        }
        ArrayList<String> selectedValues = new ArrayList<String>(interfaceSettings.getSelected());
        if (enabled) {
            if (!selectedValues.contains(elementName)) {
                selectedValues.add(elementName);
            }
        } else {
            selectedValues.remove(elementName);
        }
        interfaceSettings.setSelected(selectedValues);
    }
}
