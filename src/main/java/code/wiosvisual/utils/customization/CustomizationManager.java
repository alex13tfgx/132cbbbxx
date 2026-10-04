/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.customization;

import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.List;

public class CustomizationManager {
    private static final List<ColorSetting> customizationSettings = new ArrayList<ColorSetting>();

    public static List<ColorSetting> getCustomizationSettings() {
        return new ArrayList<ColorSetting>(customizationSettings);
    }

    public static int getSettingsCount() {
        return customizationSettings.size();
    }

    public static boolean hasSettings() {
        return !customizationSettings.isEmpty();
    }

    static {
        customizationSettings.add(ThemeManager.primaryColor);
        customizationSettings.add(ThemeManager.BackgroundGui);
        customizationSettings.add(ThemeManager.offModuleColor);
        customizationSettings.add(ThemeManager.BackgroundSettings);
        customizationSettings.add(ThemeManager.textColor);
    }
}
