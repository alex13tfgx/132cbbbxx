package code.wiosvisual.features.impl.customization;

import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.theme.ThemeManager;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Готовые темы клиента: цвета не подбираются вручную, а выбираются из списка.
 * Фон у всех тем чёрный, отличается только акцентный цвет.
 */
public class ThemeEditor
extends Module {
    /** Тема → акцентный цвет (ARGB). Порядок сохраняется в выпадающем списке. */
    private static final Map<String, Integer> THEMES = new LinkedHashMap<String, Integer>();

    static {
        THEMES.put("Фиолетовая", 0xFF9650F5);
        THEMES.put("Синяя", 0xFF4682FF);
        THEMES.put("Голубая", 0xFF3CBEFF);
        THEMES.put("Бирюзовая", 0xFF28C8BE);
        THEMES.put("Зелёная", 0xFF46C864);
        THEMES.put("Красная", 0xFFEB3C46);
        THEMES.put("Оранжевая", 0xFFFF8C28);
        THEMES.put("Розовая", 0xFFF05AAA);
        THEMES.put("Золотая", 0xFFE6B93C);
        THEMES.put("Белая", 0xFFE6E6EB);
    }

    private static final int BACKGROUND = 0xFF000000;
    private static final int PANEL = 0xFF0C0C0D;
    private static final int OFF_MODULE = 0xFF161618;
    private static final int TEXT = 0xFFFFFFFF;

    private static final String CUSTOM = "Custom";

    public final SelectSetting theme = new SelectSetting("Тема", "Готовая тема клиента или Custom со своими цветами").value(withCustom()).selected("Фиолетовая");
    public final ColorSetting customAccent = new ColorSetting("Свой акцент", "Акцентный цвет темы Custom").value(0xFF9650F5).visible(() -> this.theme.isSelected(CUSTOM));
    public final ColorSetting customBackground = new ColorSetting("Свой фон", "Цвет фона темы Custom").value(0xFF000000).visible(() -> this.theme.isSelected(CUSTOM));

    private static String[] withCustom() {
        java.util.List<String> names = new java.util.ArrayList<String>(THEMES.keySet());
        names.add(CUSTOM);
        return names.toArray(new String[0]);
    }

    private int accentOf(String name) {
        return CUSTOM.equals(name) ? this.customAccent.getColor() : THEMES.getOrDefault(name, 0);
    }

    private int backgroundOf(String name) {
        return CUSTOM.equals(name) ? this.customBackground.getColor() : BACKGROUND;
    }

    private static boolean known(String name) {
        return CUSTOM.equals(name) || THEMES.containsKey(name);
    }

    private String appliedTheme;
    private int checkTicks;

    public static ThemeEditor getInstance() {
        return Instance.get(ThemeEditor.class);
    }

    public ThemeEditor() {
        super("ThemeEditor", "Theme", ModuleCategory.CUSTOMIZATION);
        this.setup(this.theme, this.customAccent, this.customBackground);
        this.setState(true);
    }

    @Override
    public void deactivate() {
        // Тема действует всегда: модуль выключать нечего.
        this.setState(true);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        String selected = this.theme.getSelected();
        if (selected == null) {
            return;
        }
        // Старые сохранённые темы при загрузке конфига перезаписывают цвета — раз в секунду
        // проверяем, что они совпадают с выбранной готовой темой.
        boolean drifted = ++this.checkTicks >= 20 && !this.matches(selected);
        if (this.checkTicks >= 20) {
            this.checkTicks = 0;
        }
        if (drifted || !selected.equals(this.appliedTheme)) {
            this.apply(selected);
        }
    }

    private boolean matches(String name) {
        return !known(name) || near(ThemeManager.primaryColor.getColor(), this.accentOf(name)) && near(ThemeManager.BackgroundGui.getColor(), this.backgroundOf(name))
                && near(ThemeManager.BackgroundSettings.getColor(), PANEL) && near(ThemeManager.offModuleColor.getColor(), OFF_MODULE);
    }

    /** Цвет хранится в HSB, поэтому после записи он может сдвинуться на единицу-другую по каналу. */
    private static boolean near(int a, int b) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF)) > 3) {
                return false;
            }
        }
        return true;
    }

    private void apply(String name) {
        if (!known(name)) {
            return;
        }
        this.appliedTheme = name;
        ThemeManager.primaryColor.setColor(this.accentOf(name));
        ThemeManager.BackgroundGui.setColor(this.backgroundOf(name));
        ThemeManager.BackgroundSettings.setColor(PANEL);
        ThemeManager.offModuleColor.setColor(OFF_MODULE);
        ThemeManager.textColor.setColor(TEXT);
    }
}
