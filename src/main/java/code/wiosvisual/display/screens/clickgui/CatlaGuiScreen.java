/*
 * ClickGUI в стиле Catla («drop ui»): пять колонок-категорий, модули с раскрывающимися
 * настройками прямо в строке, поиск снизу. Размеры, отступы, радиусы скруглений и цвета
 * взяты один в один из catlavan.gui.clickgui.ClickGuiScreen, рендер переписан на
 * 1.21.4-стек Wios (Rectangle + MSDF-шрифты + scissor класса DrawContext).
 */
package code.wiosvisual.display.screens.clickgui;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.Setting;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.bind.BindCodes;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class CatlaGuiScreen
extends Screen {
    // ── геометрия колонки ────────────────────────────────────────────────────
    private static final float COLUMN_STEP = 130.0f;
    private static final float COLUMN_WIDTH = 124.0f;
    private static final float COLUMN_HEIGHT = 311.0f;
    private static final float GUI_TOTAL_HEIGHT = 345.0f;
    private static final float HEADER_HEIGHT = 30.0f;
    private static final float MOD_LIST_TOP = 30.0f;
    private static final float MOD_LIST_HEIGHT = 281.0f;
    private static final float MOD_ITEM_X = 5.0f;
    private static final float MOD_ITEM_Y = 35.0f;
    private static final float MOD_ITEM_WIDTH = 114.0f;
    private static final float MOD_COLLAPSED_HEIGHT = 25.0f;
    private static final float MOD_TOTAL_HEIGHT = 29.0f;
    private static final float SETTINGS_X = 8.0f;
    private static final float SETTINGS_Y = 29.0f;
    private static final float SETTINGS_WIDTH = 112.0f;
    private static final float CHIP_MAX_WIDTH = 95.0f;

    // ── высоты настроек ──────────────────────────────────────────────────────
    private static final float H_BOOLEAN = 15.0f;
    private static final float H_BIND = 15.0f;
    private static final float H_SLIDER = 25.0f;
    private static final float H_COLOR = 76.0f;
    private static final float H_TEXT = 30.0f;

    // ── скорости анимаций (в единицах «на секунду», как в Catla) ─────────────
    private static final float SPEED_OPEN = 30.0f;
    private static final float SPEED_SEARCH = 18.0f;
    private static final float SPEED_ENABLE = 20.0f;
    private static final float SPEED_EXPAND = 18.0f;
    private static final float SPEED_SCROLL = 20.0f;
    private static final float SPEED_TOGGLE = 18.0f;
    private static final float SPEED_BIND = 16.0f;
    private static final float SPEED_CHIP = 18.0f;

    // ── палитра: глубокий чёрный + насыщенный акцент ─────────────────────────
    private static final int C_BACKDROP = CatlaGuiScreen.rgb(2, 2, 3);
    // светлая тема: корпус чуть серее строк, чтобы белые плашки читались
    private static final int C_COLUMN_TOP = CatlaGuiScreen.rgb(24, 24, 24);
    private static final int C_COLUMN_BOTTOM = CatlaGuiScreen.rgb(17, 17, 17);
    private static final int C_COLUMN_BORDER = CatlaGuiScreen.rgb(45, 45, 45);
    private static final int C_HEADER_TOP = CatlaGuiScreen.rgb(31, 31, 31);
    private static final int C_HEADER_BOTTOM = CatlaGuiScreen.rgb(23, 23, 23);
    private static final int C_MOD_SHADOW = CatlaGuiScreen.rgb(0, 0, 0);
    // плашка модуля белая: тёмный текст поверх, чтобы строка читалась
    private static final int C_MOD_TOP = CatlaGuiScreen.rgb(32, 32, 32);
    private static final int C_MOD_BOTTOM = CatlaGuiScreen.rgb(25, 25, 25);
    private static final int C_MOD_TOP_ACTIVE = CatlaGuiScreen.rgb(41, 41, 41);
    private static final int C_MOD_BOTTOM_ACTIVE = CatlaGuiScreen.rgb(33, 33, 33);
    private static final int C_MOD_BORDER = CatlaGuiScreen.rgb(52, 52, 52);
    private static final int C_MOD_TEXT = CatlaGuiScreen.rgb(238, 238, 240);
    private static final int C_MOD_GLYPH = CatlaGuiScreen.rgb(152, 152, 156);
    private static final int C_MOD_TRACK_OFF = CatlaGuiScreen.rgb(58, 58, 58);
    private static final int C_MOD_KNOB_OFF = CatlaGuiScreen.rgb(132, 132, 136);
    private static final int C_MOD_BAR_OFF = CatlaGuiScreen.rgb(72, 72, 72);
    private static final int C_ACCENT_OFF = CatlaGuiScreen.rgb(150, 150, 154);
    private static final int C_TEXT_DIM = CatlaGuiScreen.rgb(170, 170, 174);
    private static final int C_TEXT_DIMMER = CatlaGuiScreen.rgb(136, 136, 140);
    private static final int C_CHIP_BORDER = CatlaGuiScreen.rgb(52, 52, 52);
    private static final int C_CHIP_BG = CatlaGuiScreen.rgb(29, 29, 29);
    private static final int C_CHIP_BG_LOW = CatlaGuiScreen.rgb(22, 22, 22);
    private static final int C_SLIDER_TRACK = CatlaGuiScreen.rgb(56, 56, 56);
    private static final int C_HINT = CatlaGuiScreen.rgb(142, 142, 146);
    private static final int C_CURSOR = CatlaGuiScreen.rgb(236, 236, 240);
    private static final int C_SCROLL = CatlaGuiScreen.rgb(60, 60, 60);

    private static final String SEARCH_LABEL = "Search: ";
    private static final String SEARCH_HINT = "type module...";
    private static final String TEXT_HINT = "click and type...";
    private static final String BIND_PROMPT = "press to key...";
    private static final String BIND_ELLIPSIS = "...";
    private static final String BIND_NONE = "N/A";

    private static CatlaGuiScreen instance;

    /** Колонки только тех категорий, где реально есть модули: пустые панели не рисуем. */
    private ModuleCategory[] categories = ModuleCategory.values();
    private final float[] scrollTargets;
    private final float[] scrollValues;
    private final Map<Module, Float> enableAnim = new HashMap<Module, Float>();
    private final Map<Module, Float> hoverAnim = new HashMap<Module, Float>();
    private final Map<Module, Float> expandAnim = new HashMap<Module, Float>();
    private final Map<Setting, Float> settingAnim = new HashMap<Setting, Float>();
    private final Map<String, Float> chipAnim = new HashMap<String, Float>();
    private final Set<Module> expanded = new HashSet<Module>();
    private final Set<SliderSettings> draggingSliders = new HashSet<SliderSettings>();
    private final List<ModuleSlot> slots = new ArrayList<ModuleSlot>();
    private final Rectangle rectangle = new Rectangle();

    private ColorSetting draggingPicker;
    private ColorSetting draggingHue;
    private BindSetting bindingSetting;
    /** Текстовое поле настройки, в которое сейчас печатают. */
    private TextSetting editingText;
    private Module bindingModule;
    private boolean searchActive;
    private String searchText = "";
    private float openAnim;
    private float searchGlow;
    private boolean closing;
    private float panelX;
    private float panelY;
    private long lastFrame = System.nanoTime();

    public CatlaGuiScreen() {
        super((Text)Text.literal((String)"drop ui"));
        this.scrollTargets = new float[ModuleCategory.values().length];
        this.scrollValues = new float[ModuleCategory.values().length];
    }

    /** Пересобирает список колонок: категория попадает в GUI, только если в ней есть видимые модули. */
    private void refreshCategories() {
        Wiosvisual client = Wiosvisual.getInstance();
        if (client == null || client.getModuleRepository() == null) {
            return;
        }
        ArrayList<ModuleCategory> active = new ArrayList<ModuleCategory>();
        for (ModuleCategory category : ModuleCategory.values()) {
            for (Module module : client.getModuleRepository().modules()) {
                if (module.getCategory() != category || module.isHidden()) continue;
                active.add(category);
                break;
            }
        }
        if (!active.isEmpty()) {
            this.categories = active.toArray(new ModuleCategory[0]);
        }
    }

    public static CatlaGuiScreen getInstance() {
        if (instance == null) {
            instance = new CatlaGuiScreen();
        }
        return instance;
    }

    /** Точка входа: биндом или из ModuleSwitcher. */
    public void openGui() {
        this.closing = false;
        this.openAnim = 0.0f;
        // Текст поиска не сбрасываем: после повторного открытия GUI найденные модули остаются на экране.
        this.searchActive = false;
        this.bindingSetting = null;
        this.bindingModule = null;
        this.draggingSliders.clear();
        this.draggingPicker = null;
        this.draggingHue = null;
        this.lastFrame = System.nanoTime();
        this.refreshCategories();
        MinecraftClient.getInstance().setScreen((Screen)this);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ── рендер ───────────────────────────────────────────────────────────────

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (float)((double)(now - this.lastFrame) / 1.0E9));
        this.lastFrame = now;
        this.openAnim = CatlaGuiScreen.approach(this.openAnim, this.closing ? 0.0f : 1.0f, SPEED_OPEN, dt);
        if (this.closing && this.openAnim <= 0.01f) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }
        MatrixStack matrices = GuiCompat.stack(context);
        int alpha = (int)(255.0f * this.openAnim);
        int accent = ColorAssist.getClientColor();
        this.refreshCategories();
        this.dropHiddenModules();
        float totalWidth = COLUMN_WIDTH * (float)this.categories.length + 10.0f * (float)(this.categories.length - 1);
        this.panelX = (float)context.getScaledWindowWidth() / 2.0f - totalWidth / 2.0f;
        this.panelY = (float)context.getScaledWindowHeight() / 2.0f - GUI_TOTAL_HEIGHT / 2.0f;
        this.searchGlow = CatlaGuiScreen.approach(this.searchGlow, this.searchActive || !this.searchText.isEmpty() ? 1.0f : 0.0f, SPEED_SEARCH, dt);

        this.renderBackdrop(context, matrices, totalWidth, accent);
        this.slots.clear();
        for (int i = 0; i < this.categories.length; ++i) {
            this.renderColumn(context, matrices, i, mouseX, mouseY, dt, alpha, accent);
        }
        this.renderSearchBar(context, matrices, totalWidth, accent);
        Rectangle.flushPending();
    }

    /**
     * Фона у GUI нет: экран не затемняется, мир виден как есть. Рисуем только мягкую
     * тень вплотную к самим панелям, чтобы они не сливались с ярким миром.
     */
    private void renderBackdrop(DrawContext context, MatrixStack matrices, float totalWidth, int accent) {
        this.glow(matrices, this.panelX - 4.0f, this.panelY - 3.0f, totalWidth + 8.0f, GUI_TOTAL_HEIGHT + 6.0f, 12.0f, 18.0f,
                CatlaGuiScreen.withAlpha(C_MOD_SHADOW, (int)(90.0f * this.openAnim)));
        Rectangle.flushPending();
    }

    private void renderColumn(DrawContext context, MatrixStack matrices, int index, int mouseX, int mouseY, float dt, int alpha, int accent) {
        ModuleCategory category = this.categories[index];
        int slotIndex = category.ordinal();
        float columnX = this.panelX + (float)index * COLUMN_STEP;
        // тень под колонкой
        this.glow(matrices, columnX + 1.0f, this.panelY + 3.0f, COLUMN_WIDTH - 2.0f, COLUMN_HEIGHT, 9.0f, 14.0f, CatlaGuiScreen.withAlpha(C_MOD_SHADOW, (int)(170.0f * this.openAnim)));
        // корпус колонки: вертикальный градиент + тонкая рамка
        this.outlineRect(matrices, columnX, this.panelY, COLUMN_WIDTH, COLUMN_HEIGHT, 9.0f, 1.0f,
                this.fade(C_COLUMN_BORDER), this.fade(C_COLUMN_TOP), this.fade(C_COLUMN_BOTTOM));
        // шапка
        this.vRect(matrices, columnX, this.panelY, COLUMN_WIDTH, HEADER_HEIGHT, 9.0f, this.fade(C_HEADER_TOP), this.fade(C_HEADER_BOTTOM));
        this.rect(matrices, columnX, this.panelY + HEADER_HEIGHT - 10.0f, COLUMN_WIDTH, 10.0f, 0.0f, this.fade(C_HEADER_BOTTOM));
        // акцентная линия-разделитель под шапкой с затуханием к краям
        this.hRect(matrices, columnX + 6.0f, this.panelY + HEADER_HEIGHT - 0.75f, COLUMN_WIDTH - 12.0f, 0.75f, 0.35f,
                CatlaGuiScreen.withAlpha(accent, (int)(20.0f * this.openAnim)), CatlaGuiScreen.withAlpha(accent, (int)(150.0f * this.openAnim)));
        this.glow(matrices, columnX + 20.0f, this.panelY + HEADER_HEIGHT - 2.0f, COLUMN_WIDTH - 40.0f, 2.0f, 1.0f, 8.0f,
                CatlaGuiScreen.withAlpha(accent, (int)(60.0f * this.openAnim)));
        // блик по верхней кромке
        this.rect(matrices, columnX + 8.0f, this.panelY + 0.6f, COLUMN_WIDTH - 16.0f, 0.6f, 0.3f, CatlaGuiScreen.withAlpha(-1, (int)(120.0f * this.openAnim)));
        Rectangle.flushPending();

        // Лого + название единым блоком по центру шапки.
        FontRenderer titleFont = Fonts.getSize(15);
        String title = category.getReadableName();
        float logoSize = 12.0f;
        float titleWidth = titleFont.getStringWidth(title);
        float groupWidth = logoSize + 6.0f + titleWidth;
        float logoX = columnX + COLUMN_WIDTH / 2.0f - groupWidth / 2.0f;
        this.glow(matrices, logoX + 1.0f, this.panelY + 10.0f, logoSize - 2.0f, logoSize - 2.0f, 5.0f, 10.0f, CatlaGuiScreen.withAlpha(accent, (int)((float)alpha * 0.35f)));
        Rectangle.flushPending();
        BackgroundIcons.drawGlowing(context, BackgroundIcons.FEATHER, logoX, this.panelY + 9.0f, logoSize, CatlaGuiScreen.withAlpha(accent, alpha));
        titleFont.drawGradientString(matrices, title, logoX + logoSize + 6.0f, this.panelY + 14.0f,
                CatlaGuiScreen.withAlpha(C_MOD_TEXT, alpha), CatlaGuiScreen.withAlpha(CatlaGuiScreen.lerpColor(C_MOD_TEXT, accent, 0.6f), alpha));

        List<Module> modules = this.modulesOf(category);
        float content = 0.0f;
        for (Module module : modules) {
            content += MOD_TOTAL_HEIGHT + this.moduleSettingsHeight(module) * this.animOf(this.expandAnim, module, this.expanded.contains(module) ? 1.0f : 0.0f, SPEED_EXPAND, dt);
        }
        float minScroll = Math.min(0.0f, MOD_LIST_HEIGHT - 5.0f - content);
        this.scrollTargets[slotIndex] = CatlaGuiScreen.clamp(this.scrollTargets[slotIndex], minScroll, 0.0f);
        this.scrollValues[slotIndex] = CatlaGuiScreen.approach(this.scrollValues[slotIndex], this.scrollTargets[slotIndex], SPEED_SCROLL, dt);
        this.scrollValues[slotIndex] = CatlaGuiScreen.clamp(this.scrollValues[slotIndex], minScroll, 0.0f);

        float offset = this.scrollValues[slotIndex];
        context.enableScissor((int)columnX, (int)(this.panelY + MOD_LIST_TOP), (int)(columnX + COLUMN_WIDTH), (int)(this.panelY + MOD_LIST_TOP + MOD_LIST_HEIGHT));
        for (Module module : modules) {
            float itemX = columnX + MOD_ITEM_X;
            float itemY = this.panelY + offset + MOD_ITEM_Y;
            float enable = this.animOf(this.enableAnim, module, module.isState() ? 1.0f : 0.0f, SPEED_ENABLE, dt);
            float expand = this.animOf(this.expandAnim, module, this.expanded.contains(module) ? 1.0f : 0.0f, SPEED_EXPAND, dt);
            float settingsHeight = this.moduleSettingsHeight(module);
            float openHeight = settingsHeight * expand;
            float boxHeight = MOD_COLLAPSED_HEIGHT + openHeight;
            float step = MOD_TOTAL_HEIGHT + openHeight;
            if (itemY + boxHeight < this.panelY + MOD_LIST_TOP || itemY > this.panelY + MOD_LIST_TOP + MOD_LIST_HEIGHT) {
                offset += step;
                continue;
            }
            this.slots.add(new ModuleSlot(module, itemX, itemY, expand, settingsHeight, openHeight));
            this.renderModule(context, matrices, module, itemX, itemY, boxHeight, enable, expand, openHeight, mouseX, mouseY, dt, accent);
            offset += step;
        }
        Rectangle.flushPending();
        context.disableScissor();

        // тонкий скроллбар справа — только когда список действительно длиннее колонки
        if (minScroll < -0.5f) {
            float track = MOD_LIST_HEIGHT - 12.0f;
            float thumb = Math.max(24.0f, track * (MOD_LIST_HEIGHT / content));
            float progress = minScroll >= 0.0f ? 0.0f : CatlaGuiScreen.clamp(this.scrollValues[slotIndex] / minScroll, 0.0f, 1.0f);
            float barX = columnX + COLUMN_WIDTH - 3.5f;
            float barY = this.panelY + MOD_LIST_TOP + 6.0f;
            this.rect(matrices, barX, barY, 1.5f, track, 0.75f, CatlaGuiScreen.withAlpha(C_SCROLL, (int)(70.0f * this.openAnim)));
            this.rect(matrices, barX, barY + (track - thumb) * progress, 1.5f, thumb, 0.75f, CatlaGuiScreen.withAlpha(accent, (int)(190.0f * this.openAnim)));
            Rectangle.flushPending();
        }
    }

    private void renderModule(DrawContext context, MatrixStack matrices, Module module, float x, float y, float boxHeight, float enable, float expand, float openHeight, int mouseX, int mouseY, float dt, int accent) {
        boolean hovered = CatlaGuiScreen.inRect(mouseX, mouseY, x, y, MOD_ITEM_WIDTH, MOD_COLLAPSED_HEIGHT);
        float hover = this.animOf(this.hoverAnim, module, hovered ? 1.0f : 0.0f, SPEED_TOGGLE, dt);
        float lift = Math.max(enable, hover * 0.6f);
        int top = CatlaGuiScreen.lerpColor(C_MOD_TOP, C_MOD_TOP_ACTIVE, lift);
        int bottom = CatlaGuiScreen.lerpColor(C_MOD_BOTTOM, C_MOD_BOTTOM_ACTIVE, lift);
        int border = CatlaGuiScreen.lerpColor(C_MOD_BORDER, CatlaGuiScreen.lerpColor(C_MOD_BORDER, accent, 0.55f), enable);
        // планка слева: серая у выключенного, акцентная у включённого
        int accentBar = CatlaGuiScreen.lerpColor(C_MOD_BAR_OFF, accent, enable);
        // строка светлая, поэтому текст и значки тёмные
        int nameColor = C_MOD_TEXT;
        int glyphColor = C_MOD_GLYPH;

        // тень + акцентное свечение включённого модуля
        this.glow(matrices, x + 1.0f, y + 2.0f, MOD_ITEM_WIDTH - 2.0f, boxHeight - 1.0f, 6.0f, 8.0f, CatlaGuiScreen.withAlpha(C_MOD_SHADOW, (int)(160.0f * this.openAnim)));
        if (enable > 0.01f) {
            this.glow(matrices, x + 2.0f, y + 2.0f, MOD_ITEM_WIDTH - 4.0f, boxHeight - 4.0f, 6.0f, 12.0f,
                    CatlaGuiScreen.withAlpha(accent, (int)(46.0f * enable * this.openAnim)));
        }
        this.outlineRect(matrices, x, y, MOD_ITEM_WIDTH, boxHeight, 6.0f, 1.0f, this.fade(border), this.fade(top), this.fade(bottom));
        // акцентная планка слева
        this.rect(matrices, x + 1.0f, y + 6.0f, 2.0f, 13.0f, 1.0f, this.fade(accentBar));
        if (enable > 0.01f) {
            this.glow(matrices, x + 1.0f, y + 6.0f, 2.0f, 13.0f, 1.0f, 6.0f, CatlaGuiScreen.withAlpha(accent, (int)(130.0f * enable * this.openAnim)));
        }
        // тумблер-пилюля справа
        float trackX = x + 94.0f;
        float trackY = y + 8.5f;
        float trackW = 16.0f;
        float trackH = 9.0f;
        int trackOff = CatlaGuiScreen.lerpColor(C_MOD_TRACK_OFF, CatlaGuiScreen.rgb(74, 74, 74), hover);
        this.rect(matrices, trackX, trackY, trackW, trackH, trackH / 2.0f, this.fade(trackOff));
        if (enable > 0.01f) {
            this.glow(matrices, trackX - 1.0f, trackY - 1.0f, trackW + 2.0f, trackH + 2.0f, (trackH + 2.0f) / 2.0f, 8.0f,
                    CatlaGuiScreen.withAlpha(accent, (int)(110.0f * enable * this.openAnim)));
            this.vRect(matrices, trackX, trackY, trackW, trackH, trackH / 2.0f,
                    CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 1.15f), (int)(255.0f * enable * this.openAnim)),
                    CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 0.72f), (int)(255.0f * enable * this.openAnim)));
        }
        float knobX = trackX + 4.5f + 7.0f * enable;
        this.circle(matrices, knobX, trackY + trackH / 2.0f, 7.0f, CatlaGuiScreen.withAlpha(C_MOD_SHADOW, (int)(120.0f * this.openAnim)));
        this.circle(matrices, knobX, trackY + trackH / 2.0f, 6.0f, this.fade(CatlaGuiScreen.lerpColor(C_MOD_KNOB_OFF, -1, enable)));
        Rectangle.flushPending();

        if (!this.visibleSettings(module).isEmpty()) {
            // «шеврон» раскрытия: две палочки, складывающиеся в стрелку
            float chevronX = x + 84.0f;
            float chevronY = y + 12.5f;
            float tilt = 1.6f * (1.0f - expand * 2.0f);
            this.rect(matrices, chevronX, chevronY - tilt / 2.0f, 3.5f, 1.2f, 0.6f, this.fade(glyphColor));
            this.rect(matrices, chevronX + 3.0f, chevronY + tilt / 2.0f, 3.5f, 1.2f, 0.6f, this.fade(glyphColor));
            Rectangle.flushPending();
        }
        // В строке — только имя модуля: visibleName у части модулей хранит описание.
        String label = this.bindingModule == module ? BIND_PROMPT : module.getName();
        Fonts.getSize(14).drawString(matrices, label, x + 9.0f, y + 12.0f, this.fade(nameColor));

        if (expand <= 0.001f || openHeight <= 0.0f) {
            return;
        }
        int settingAlpha = (int)(255.0f * expand * this.openAnim);
        // утопленная площадка под настройками + разделитель
        this.rect(matrices, x + 3.0f, y + MOD_COLLAPSED_HEIGHT - 2.0f, MOD_ITEM_WIDTH - 6.0f, openHeight, 5.0f,
                CatlaGuiScreen.withAlpha(CatlaGuiScreen.rgb(26, 26, 26), (int)(235.0f * expand * this.openAnim)));
        this.hRect(matrices, x + 8.0f, y + MOD_COLLAPSED_HEIGHT - 2.0f, MOD_ITEM_WIDTH - 16.0f, 0.7f, 0.35f,
                CatlaGuiScreen.withAlpha(accent, (int)(15.0f * expand * this.openAnim)),
                CatlaGuiScreen.withAlpha(accent, (int)(110.0f * expand * this.openAnim)));
        Rectangle.flushPending();
        context.enableScissor((int)(x + 1.0f), (int)(y + MOD_COLLAPSED_HEIGHT), (int)(x + 1.0f + SETTINGS_WIDTH), (int)(y + MOD_COLLAPSED_HEIGHT + openHeight));
        float cursor = 0.0f;
        for (Setting setting : this.visibleSettings(module)) {
            float height = this.settingHeight(setting);
            if (height <= 0.0f) continue;
            this.renderSetting(context, matrices, setting, x + SETTINGS_X, y + SETTINGS_Y + cursor, settingAlpha, mouseX, mouseY, dt, accent);
            cursor += height;
        }
        Rectangle.flushPending();
        context.disableScissor();
    }

    private void renderSetting(DrawContext context, MatrixStack matrices, Setting setting, float x, float y, int alpha, int mouseX, int mouseY, float dt, int accent) {
        FontRenderer font = Fonts.getSize(14);
        int labelColor = CatlaGuiScreen.withAlpha(C_MOD_TEXT, alpha);
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting)setting;
            float on = this.animOf(this.settingAnim, setting, bool.isValue() ? 1.0f : 0.0f, SPEED_TOGGLE, dt);
            float boxX = x + 90.0f;
            float boxY = y - 4.0f;
            font.drawString(matrices, setting.getName(), x, y, labelColor);
            this.outlineRect(matrices, boxX, boxY, 9.0f, 9.0f, 3.0f, 0.8f,
                    CatlaGuiScreen.withAlpha(CatlaGuiScreen.lerpColor(C_CHIP_BORDER, accent, on), alpha),
                    CatlaGuiScreen.withAlpha(C_CHIP_BG, alpha), CatlaGuiScreen.withAlpha(C_CHIP_BG_LOW, alpha));
            if (on > 0.001f) {
                this.glow(matrices, boxX, boxY, 9.0f, 9.0f, 3.0f, 7.0f, CatlaGuiScreen.withAlpha(accent, (int)((float)alpha * on * 0.55f)));
                this.vRect(matrices, boxX + 1.0f, boxY + 1.0f, 7.0f, 7.0f, 2.2f,
                        CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 1.15f), (int)((float)alpha * on)),
                        CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 0.75f), (int)((float)alpha * on)));
            }
            int tick = (int)((float)alpha * on);
            if (tick > 2) {
                this.rect(matrices, boxX + 3.0f, boxY + 3.5f, 3.5f, 1.4f, 0.7f, CatlaGuiScreen.withAlpha(-1, tick));
            }
            Rectangle.flushPending();
            return;
        }
        if (setting instanceof BindSetting) {
            BindSetting bind = (BindSetting)setting;
            float target = this.bindingSetting == bind ? 1.0f : (bind.getKey() > 0 ? 0.55f : 0.0f);
            float hover = this.animOf(this.settingAnim, setting, target, SPEED_BIND, dt);
            String text = this.bindingSetting == bind ? BIND_ELLIPSIS : CatlaGuiScreen.keyName(bind.getKey());
            FontRenderer small = Fonts.getSize(12);
            float width = Math.max(22.0f, small.getStringWidth(text) + 6.0f);
            float bx = x + 98.0f - width;
            float by = y - 4.0f;
            font.drawString(matrices, setting.getName(), x, y, labelColor);
            this.outlineRect(matrices, bx, by, width, 9.0f, 3.0f, 0.8f,
                    CatlaGuiScreen.withAlpha(CatlaGuiScreen.lerpColor(C_CHIP_BORDER, accent, hover), alpha),
                    CatlaGuiScreen.withAlpha(C_CHIP_BG, alpha), CatlaGuiScreen.withAlpha(C_CHIP_BG_LOW, alpha));
            if (hover > 0.001f) {
                this.vRect(matrices, bx + 0.5f, by + 0.5f, width - 1.0f, 8.0f, 2.6f,
                        CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 1.1f), (int)((float)alpha * hover)),
                        CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 0.7f), (int)((float)alpha * hover)));
            }
            Rectangle.flushPending();
            small.drawString(matrices, text, bx + (width - small.getStringWidth(text)) / 2.0f, y + 1.0f, CatlaGuiScreen.withAlpha(CatlaGuiScreen.lerpColor(C_MOD_TEXT, -1, hover), alpha));
            return;
        }
        if (setting instanceof ColorSetting) {
            ColorSetting color = (ColorSetting)setting;
            float px = x;
            float py = y + 10.0f;
            float pw = 98.5f;
            float ph = 44.0f;
            float hueY = py + ph + 6.0f;
            if (this.draggingPicker == color) {
                color.setSaturation(CatlaGuiScreen.clamp(((float)mouseX - px) / pw, 0.0f, 1.0f));
                color.setBrightness(1.0f - CatlaGuiScreen.clamp(((float)mouseY - py) / ph, 0.0f, 1.0f));
            }
            if (this.draggingHue == color) {
                color.setHue(CatlaGuiScreen.clamp(((float)mouseX - px) / pw, 0.0f, 1.0f));
            }
            font.drawString(matrices, setting.getName(), x, y, labelColor);
            int pure = Color.HSBtoRGB(color.getHue(), 1.0f, 1.0f);
            this.rect(matrices, px - 0.5f, py - 0.5f, pw + 1.0f, ph + 1.0f, 6.0f, CatlaGuiScreen.withAlpha(C_CHIP_BORDER, alpha));
            this.gradientQuad(matrices, px, py, pw, ph, pure, alpha);
            float cx = px + color.getSaturation() * pw;
            float cy = py + (1.0f - color.getBrightness()) * ph;
            this.circle(matrices, cx, cy, 7.0f, CatlaGuiScreen.withAlpha(-1, alpha));
            this.circle(matrices, cx, cy, 5.0f, CatlaGuiScreen.withAlpha(color.getColor(), alpha));
            this.rect(matrices, px - 0.5f, hueY - 0.5f, pw + 1.0f, 9.0f, 4.0f, CatlaGuiScreen.withAlpha(C_CHIP_BORDER, alpha));
            this.hueBar(matrices, px, hueY, pw, 8.0f, alpha);
            float hx = px + color.getHue() * pw;
            this.rect(matrices, hx - 3.0f, hueY - 2.0f, 6.0f, 12.0f, 3.0f, CatlaGuiScreen.withAlpha(-1, alpha));
            this.rect(matrices, hx - 2.0f, hueY - 1.0f, 4.0f, 10.0f, 2.0f, CatlaGuiScreen.withAlpha(pure, alpha));
            Rectangle.flushPending();
            return;
        }
        if (setting instanceof SliderSettings) {
            SliderSettings slider = (SliderSettings)setting;
            float fraction = CatlaGuiScreen.sliderFraction(slider);
            if (this.draggingSliders.contains(slider)) {
                CatlaGuiScreen.applySliderValue(slider, ((float)mouseX - x) / 98.5f);
                fraction = CatlaGuiScreen.sliderFraction(slider);
            }
            String value = CatlaGuiScreen.sliderText(slider);
            font.drawString(matrices, setting.getName(), x, y, labelColor);
            font.drawString(matrices, value, x + 98.0f - font.getStringWidth(value), y, labelColor);
            this.rect(matrices, x, y + 10.0f, 98.5f, 3.5f, 1.75f, CatlaGuiScreen.withAlpha(C_SLIDER_TRACK, alpha));
            if (fraction > 0.001f) {
                this.glow(matrices, x, y + 10.0f, 98.5f * fraction, 3.5f, 1.75f, 6.0f, CatlaGuiScreen.withAlpha(accent, (int)((float)alpha * 0.45f)));
                this.hRect(matrices, x, y + 10.0f, 98.5f * fraction, 3.5f, 1.75f,
                        CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 0.7f), alpha),
                        CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 1.2f), alpha));
            }
            float knobX = x + fraction * 98.5f;
            this.circle(matrices, knobX, y + 11.75f, 8.0f, CatlaGuiScreen.withAlpha(accent, (int)((float)alpha * 0.35f)));
            this.circle(matrices, knobX, y + 11.75f, 6.5f, CatlaGuiScreen.withAlpha(-1, alpha));
            this.circle(matrices, knobX, y + 11.75f, 3.0f, CatlaGuiScreen.withAlpha(accent, alpha));
            Rectangle.flushPending();
            return;
        }
        if (setting instanceof TextSetting) {
            TextSetting text = (TextSetting)setting;
            boolean focused = this.editingText == text;
            float on = this.animOf(this.settingAnim, setting, focused ? 1.0f : 0.0f, SPEED_TOGGLE, dt);
            float boxY = y + 8.0f;
            float boxH = 15.0f;
            font.drawString(matrices, setting.getName(), x, y, labelColor);
            if (on > 0.001f) {
                this.glow(matrices, x, boxY, 98.5f, boxH, 4.0f, 10.0f, CatlaGuiScreen.withAlpha(accent, (int)((float)alpha * on * 0.4f)));
            }
            this.outlineRect(matrices, x, boxY, 98.5f, boxH, 4.0f, 0.8f,
                    CatlaGuiScreen.withAlpha(CatlaGuiScreen.lerpColor(C_CHIP_BORDER, accent, on), alpha),
                    CatlaGuiScreen.withAlpha(C_CHIP_BG, alpha), CatlaGuiScreen.withAlpha(C_CHIP_BG_LOW, alpha));
            Rectangle.flushPending();
            FontRenderer small = Fonts.getSize(13);
            float inner = 92.5f;
            String value = text.getText() == null ? "" : text.getText();
            boolean empty = value.isEmpty();
            String shown = CatlaGuiScreen.trimToWidth(small, empty ? TEXT_HINT : value, inner);
            float textX = x + 4.0f;
            float textY = boxY + boxH / 2.0f - 1.0f;
            context.enableScissor((int)textX, (int)boxY, (int)(textX + inner), (int)(boxY + boxH));
            small.drawString(matrices, shown, textX, textY, CatlaGuiScreen.withAlpha(empty ? C_HINT : C_MOD_TEXT, alpha));
            if (focused && System.currentTimeMillis() / 450L % 2L == 0L) {
                float cursorX = textX + (empty ? 0.0f : small.getStringWidth(shown)) + 1.0f;
                this.rect(matrices, cursorX, boxY + 3.0f, 1.0f, 9.0f, 0.5f, CatlaGuiScreen.withAlpha(C_CURSOR, alpha));
                Rectangle.flushPending();
            }
            context.disableScissor();
            return;
        }
        if (setting instanceof SelectSetting) {
            SelectSetting select = (SelectSetting)setting;
            font.drawString(matrices, setting.getName(), x, y, labelColor);
            List<String> values = select.getList();
            float chipX = 0.0f;
            float chipY = 8.0f;
            for (int i = 0; i < values.size(); ++i) {
                String value = values.get(i);
                float textWidth = font.getStringWidth(value);
                float chipWidth = textWidth + 10.0f;
                if (chipX + chipWidth > CHIP_MAX_WIDTH && chipX > 0.0f) {
                    chipY += 16.5f;
                    chipX = 0.0f;
                }
                float on = this.chipAnimOf(setting, i, select.isSelected(value), dt);
                this.renderChip(matrices, x + chipX, y + chipY, chipWidth, value, textWidth, on, alpha, accent, font);
                chipX += chipWidth + 4.0f;
            }
            Rectangle.flushPending();
            return;
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multi = (MultiSelectSetting)setting;
            font.drawString(matrices, setting.getName(), x, y, labelColor);
            List<String> values = multi.getList();
            float chipX = 0.0f;
            float chipY = 8.0f;
            for (int i = 0; i < values.size(); ++i) {
                String value = values.get(i);
                float textWidth = font.getStringWidth(value);
                float chipWidth = textWidth + 10.0f;
                if (chipX + chipWidth > CHIP_MAX_WIDTH && chipX > 0.0f) {
                    chipY += 16.5f;
                    chipX = 0.0f;
                }
                float on = this.chipAnimOf(setting, i, multi.isSelected(value), dt);
                this.renderChip(matrices, x + chipX, y + chipY, chipWidth, value, textWidth, on, alpha, accent, font);
                chipX += chipWidth + 4.0f;
            }
            Rectangle.flushPending();
        }
    }

    private void renderChip(MatrixStack matrices, float x, float y, float width, String text, float textWidth, float on, int alpha, int accent, FontRenderer font) {
        this.outlineRect(matrices, x, y, width - 1.0f, 13.0f, 6.5f, 0.8f,
                CatlaGuiScreen.withAlpha(CatlaGuiScreen.lerpColor(C_CHIP_BORDER, accent, on), alpha),
                CatlaGuiScreen.withAlpha(C_CHIP_BG, alpha), CatlaGuiScreen.withAlpha(C_CHIP_BG_LOW, alpha));
        if (on > 0.001f) {
            this.glow(matrices, x, y, width - 1.0f, 13.0f, 6.5f, 8.0f, CatlaGuiScreen.withAlpha(accent, (int)((float)alpha * on * 0.4f)));
            this.vRect(matrices, x + 0.5f, y + 0.5f, width - 2.0f, 12.0f, 6.0f,
                    CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 1.15f), (int)((float)alpha * on)),
                    CatlaGuiScreen.withAlpha(CatlaGuiScreen.brightness(accent, 0.7f), (int)((float)alpha * on)));
        }
        Rectangle.flushPending();
        font.drawString(matrices, text, x + (width - textWidth) / 2.0f - 1.0f, y + 6.0f, CatlaGuiScreen.withAlpha(CatlaGuiScreen.lerpColor(C_MOD_TEXT, -1, on), alpha));
    }

    private void renderSearchBar(DrawContext context, MatrixStack matrices, float totalWidth, int accent) {
        float width = COLUMN_WIDTH;
        float x = this.panelX + (totalWidth - width) / 2.0f - 7.5f;
        float y = this.panelY + COLUMN_HEIGHT + 9.0f;
        float glow = CatlaGuiScreen.clamp(0.65f + this.searchGlow * 0.35f, 0.0f, 1.0f);
        int top = CatlaGuiScreen.lerpColor(C_MOD_TOP, C_MOD_TOP_ACTIVE, glow);
        int bottom = CatlaGuiScreen.lerpColor(C_MOD_BOTTOM, C_MOD_BOTTOM_ACTIVE, glow);
        int border = CatlaGuiScreen.lerpColor(C_MOD_BORDER, accent, glow * 0.55f);
        int bar = CatlaGuiScreen.lerpColor(C_ACCENT_OFF, accent, glow);
        this.glow(matrices, x + 1.0f, y + 2.0f, width - 2.0f, 24.0f, 7.0f, 12.0f, CatlaGuiScreen.withAlpha(C_MOD_SHADOW, (int)(160.0f * this.openAnim)));
        if (this.searchGlow > 0.01f) {
            this.glow(matrices, x, y, width, 25.0f, 7.0f, 14.0f, CatlaGuiScreen.withAlpha(accent, (int)(55.0f * this.searchGlow * this.openAnim)));
        }
        this.outlineRect(matrices, x, y, width, 25.0f, 7.0f, 1.0f, this.fade(border), this.fade(top), this.fade(bottom));
        this.rect(matrices, x + 1.0f, y + 6.0f, 2.0f, 13.0f, 1.0f, this.fade(bar));
        Rectangle.flushPending();

        FontRenderer font = Fonts.getSize(14);
        boolean empty = this.searchText.isEmpty();
        String shown = empty ? SEARCH_HINT : this.searchText;
        float labelX = x + 8.0f;
        float labelY = y + 12.0f;
        float textX = labelX + font.getStringWidth(SEARCH_LABEL);
        float space = Math.max(0.0f, width - (textX - x) - 24.0f);
        font.drawString(matrices, SEARCH_LABEL, labelX, labelY, this.fade(C_TEXT_DIM));
        if (space <= 0.0f) {
            return;
        }
        String clipped = CatlaGuiScreen.trimToWidth(font, shown, space);
        context.enableScissor((int)textX, (int)(y + 4.0f), (int)(textX + space), (int)(y + 21.0f));
        font.drawString(matrices, clipped, textX, labelY, this.fade(empty ? C_HINT : CatlaGuiScreen.lerpColor(C_TEXT_DIM, C_MOD_TEXT, glow)));
        if (this.searchActive && System.currentTimeMillis() / 450L % 2L == 0L) {
            float cursorX = textX + font.getStringWidth(clipped) + 1.0f;
            if (cursorX <= textX + space) {
                this.rect(matrices, cursorX, y + 7.0f, 1.0f, 11.0f, 0.5f, this.fade(C_CURSOR));
                Rectangle.flushPending();
            }
        }
        context.disableScissor();
    }

    // ── ввод ─────────────────────────────────────────────────────────────────

    /** Модуль, заблокированный сервером, пропадает мгновенно вместе с открытыми настройками и ожиданием бинда. */
    private void dropHiddenModules() {
        if (this.bindingModule != null && this.bindingModule.isHidden()) {
            this.bindingModule = null;
        }
        boolean changed = false;
        for (java.util.Iterator<Module> it = this.expanded.iterator(); it.hasNext();) {
            Module module = it.next();
            if (!module.isHidden()) continue;
            it.remove();
            this.expandAnim.remove(module);
            changed = true;
        }
        if (changed) {
            this.bindingSetting = null;
            this.editingText = null;
            this.draggingSliders.clear();
            this.draggingPicker = null;
            this.draggingHue = null;
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.closing) {
            return true;
        }
        this.dropHiddenModules();
        if (this.bindingSetting != null) {
            this.bindingSetting.setKey(-100 + button);
            this.bindingSetting = null;
            return true;
        }
        if (this.bindingModule != null) {
            this.bindingModule.setKey(-100 + button);
            this.bindingModule = null;
            return true;
        }
        float totalWidth = COLUMN_WIDTH * (float)this.categories.length + 10.0f * (float)(this.categories.length - 1);
        float searchX = this.panelX + (totalWidth - COLUMN_WIDTH) / 2.0f - 7.5f;
        float searchY = this.panelY + COLUMN_HEIGHT + 9.0f;
        if (CatlaGuiScreen.inRect(mouseX, mouseY, searchX, searchY, COLUMN_WIDTH, 25.0f)) {
            this.searchActive = true;
            if (button == 1 && !this.searchText.isEmpty()) {
                this.searchText = "";
            }
            return true;
        }
        this.searchActive = false;
        this.editingText = null;
        for (ModuleSlot slot : this.slots) {
            if (slot.module.isHidden()) continue;
            if (CatlaGuiScreen.inRect(mouseX, mouseY, slot.x, slot.y, MOD_ITEM_WIDTH, MOD_COLLAPSED_HEIGHT)) {
                if (button == 2) {
                    this.bindingModule = slot.module;
                    this.bindingSetting = null;
                    this.draggingSliders.clear();
                    return true;
                }
                if (button == 0) {
                    slot.module.switchState();
                }
                if (button == 1) {
                    if (!this.expanded.remove(slot.module)) {
                        this.expanded.add(slot.module);
                    }
                }
                return true;
            }
            if (!(slot.expand > 0.01f) || !CatlaGuiScreen.inRect(mouseX, mouseY, slot.x + 1.0f, slot.y + MOD_COLLAPSED_HEIGHT, SETTINGS_WIDTH, slot.openHeight)) continue;
            float cursor = 0.0f;
            for (Setting setting : this.visibleSettings(slot.module)) {
                float height = this.settingHeight(setting);
                if (height <= 0.0f) continue;
                if (this.clickSetting(setting, slot.x + SETTINGS_X, slot.y + SETTINGS_Y + cursor, mouseX, mouseY, button)) {
                    return true;
                }
                cursor += height;
            }
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    private boolean clickSetting(Setting setting, float x, float y, double mouseX, double mouseY, int button) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting)setting;
            if (CatlaGuiScreen.inRect(mouseX, mouseY, x + 90.0f, y - 4.0f, 9.0f, 9.0f)) {
                bool.setValue(!bool.isValue());
                return true;
            }
            return false;
        }
        if (setting instanceof BindSetting) {
            BindSetting bind = (BindSetting)setting;
            String text = this.bindingSetting == bind ? BIND_ELLIPSIS : CatlaGuiScreen.keyName(bind.getKey());
            float width = Math.max(22.0f, Fonts.getSize(12).getStringWidth(text) + 6.0f);
            if (CatlaGuiScreen.inRect(mouseX, mouseY, x + 98.0f - width, y - 4.0f, width, 9.0f)) {
                if (button == 0) {
                    this.bindingSetting = bind;
                } else if (button == 1) {
                    bind.setKey(-1);
                    this.bindingSetting = null;
                }
                return true;
            }
            return false;
        }
        if (setting instanceof ColorSetting) {
            ColorSetting color = (ColorSetting)setting;
            if (button == 0 && CatlaGuiScreen.inRect(mouseX, mouseY, x, y + 10.0f, 98.5f, 44.0f)) {
                this.draggingPicker = color;
                this.draggingHue = null;
                color.setSaturation(CatlaGuiScreen.clamp(((float)mouseX - x) / 98.5f, 0.0f, 1.0f));
                color.setBrightness(1.0f - CatlaGuiScreen.clamp(((float)mouseY - (y + 10.0f)) / 44.0f, 0.0f, 1.0f));
                return true;
            }
            if (button == 0 && CatlaGuiScreen.inRect(mouseX, mouseY, x, y + 60.0f, 98.5f, 8.0f)) {
                this.draggingHue = color;
                this.draggingPicker = null;
                color.setHue(CatlaGuiScreen.clamp(((float)mouseX - x) / 98.5f, 0.0f, 1.0f));
                return true;
            }
            return false;
        }
        if (setting instanceof SliderSettings) {
            SliderSettings slider = (SliderSettings)setting;
            if (CatlaGuiScreen.inRect(mouseX, mouseY, x, y + 10.0f, 98.5f, 3.5f + 4.0f)) {
                this.draggingSliders.add(slider);
                CatlaGuiScreen.applySliderValue(slider, ((float)mouseX - x) / 98.5f);
                return true;
            }
            return false;
        }
        if (setting instanceof TextSetting) {
            TextSetting text = (TextSetting)setting;
            if (CatlaGuiScreen.inRect(mouseX, mouseY, x, y + 8.0f, 98.5f, 15.0f)) {
                this.editingText = text;
                if (button == 1) {
                    text.setText("");
                }
                return true;
            }
            return false;
        }
        if (setting instanceof SelectSetting) {
            SelectSetting select = (SelectSetting)setting;
            return this.clickChips(select.getList(), x, y, mouseX, mouseY, button, index -> select.setSelected(select.getList().get(index)));
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multi = (MultiSelectSetting)setting;
            return this.clickChips(multi.getList(), x, y, mouseX, mouseY, button, index -> {
                String value = multi.getList().get(index);
                ArrayList<String> selected = new ArrayList<String>(multi.getSelected());
                if (!selected.remove(value)) {
                    selected.add(value);
                }
                multi.setSelected(selected);
            });
        }
        return false;
    }

    private boolean clickChips(List<String> values, float x, float y, double mouseX, double mouseY, int button, java.util.function.IntConsumer action) {
        if (button != 0) {
            return false;
        }
        FontRenderer font = Fonts.getSize(14);
        float chipX = 0.0f;
        float chipY = 8.0f;
        for (int i = 0; i < values.size(); ++i) {
            float chipWidth = font.getStringWidth(values.get(i)) + 10.0f;
            if (chipX + chipWidth > CHIP_MAX_WIDTH && chipX > 0.0f) {
                chipY += 16.5f;
                chipX = 0.0f;
            }
            if (CatlaGuiScreen.inRect(mouseX, mouseY, x + chipX, y + chipY, chipWidth - 1.0f, 13.0f)) {
                action.accept(i);
                return true;
            }
            chipX += chipWidth + 4.0f;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click wiosClick) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        this.draggingSliders.clear();
        this.draggingPicker = null;
        this.draggingHue = null;
        return super.mouseReleased(wiosClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (int i = 0; i < this.categories.length; ++i) {
            float columnX = this.panelX + (float)i * COLUMN_STEP;
            if (!CatlaGuiScreen.inRect(mouseX, mouseY, columnX, this.panelY + MOD_LIST_TOP, COLUMN_WIDTH, MOD_LIST_HEIGHT)) continue;
            int slotIndex = this.categories[i].ordinal();
            this.scrollTargets[slotIndex] = this.scrollTargets[slotIndex] + (float)(vertical * 16.0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        this.dropHiddenModules();
        if (this.closing) {
            return true;
        }
        if (this.bindingSetting != null) {
            if (keyCode == 256) {
                this.bindingSetting = null;
                return true;
            }
            if (keyCode == 261 || keyCode == 259) {
                this.bindingSetting.setKey(-1);
                this.bindingSetting = null;
                return true;
            }
            if (keyCode != 343 && keyCode != 347) {
                this.bindingSetting.setKey(keyCode);
            }
            this.bindingSetting = null;
            return true;
        }
        if (this.bindingModule != null) {
            if (keyCode == 256) {
                this.bindingModule = null;
                return true;
            }
            if (keyCode == 261 || keyCode == 259) {
                this.bindingModule.setKey(-1);
                this.bindingModule = null;
                return true;
            }
            if (keyCode != 343 && keyCode != 347) {
                this.bindingModule.setKey(keyCode);
                this.bindingModule = null;
            }
            return true;
        }
        if (this.editingText != null) {
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
                this.editingText = null;
                return true;
            }
            if (keyCode == 259) {
                String current = this.editingText.getText();
                if (current != null && !current.isEmpty()) {
                    this.editingText.setText(current.substring(0, current.length() - 1));
                }
                return true;
            }
            if (keyCode == 261) {
                this.editingText.setText("");
                return true;
            }
            // Ctrl+V: вставка из буфера, иначе длинную команду не набрать удобно.
            if (keyCode == 86 && (net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow(), 341) || net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow(), 345))) {
                String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    this.appendToEditing(clip.replace((char)10, (char)32).replace((char)13, (char)32));
                }
                return true;
            }
            return true;
        }
        if (this.searchActive) {
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
                this.searchActive = false;
                return true;
            }
            if (keyCode == 259) {
                if (!this.searchText.isEmpty()) {
                    this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
                }
                return true;
            }
            if (keyCode == 261) {
                this.searchText = "";
                return true;
            }
        }
        if (keyCode == 256) {
            this.beginClose();
            return true;
        }
        return super.keyPressed(wiosKey);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput wiosChar) {
        char chr = (char) wiosChar.codepoint();
        int modifiers = wiosChar.modifiers();
        this.dropHiddenModules();
        if (this.closing) {
            return true;
        }
        if (this.bindingModule != null || this.bindingSetting != null) {
            return true;
        }
        if (this.editingText != null) {
            if (!Character.isISOControl(chr)) {
                this.appendToEditing(String.valueOf(chr));
            }
            return true;
        }
        if (this.searchActive) {
            if (this.searchText.length() < 64 && !Character.isISOControl(chr)) {
                this.searchText = this.searchText + chr;
            }
            return true;
        }
        return super.charTyped(wiosChar);
    }

    /** Дописывает текст в поле, уважая лимит длины настройки. */
    private void appendToEditing(String addition) {
        if (this.editingText == null) {
            return;
        }
        String current = this.editingText.getText() == null ? "" : this.editingText.getText();
        int max = this.editingText.getMax() > 0 ? this.editingText.getMax() : 256;
        String result = current + addition;
        if (result.length() > max) {
            result = result.substring(0, max);
        }
        this.editingText.setText(result);
    }

    private void beginClose() {
        this.closing = true;
        this.editingText = null;
        this.searchActive = false;
        this.bindingSetting = null;
        this.bindingModule = null;
        this.draggingSliders.clear();
        this.draggingPicker = null;
        this.draggingHue = null;
    }

    // ── данные ───────────────────────────────────────────────────────────────

    private List<Module> modulesOf(ModuleCategory category) {
        Wiosvisual client = Wiosvisual.getInstance();
        if (client == null || client.getModuleRepository() == null) {
            return Collections.emptyList();
        }
        String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        ArrayList<Module> result = new ArrayList<Module>();
        for (Module module : client.getModuleRepository().modules()) {
            if (module.getCategory() != category || module.isHidden()) continue;
            if (!query.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(query) && !module.getVisibleName().toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        return result;
    }

    private List<Setting> visibleSettings(Module module) {
        ArrayList<Setting> result = new ArrayList<Setting>();
        for (Setting setting : module.settings()) {
            if (setting == null || !setting.isVisible()) continue;
            result.add(setting);
        }
        return result;
    }

    private float moduleSettingsHeight(Module module) {
        float total = 0.0f;
        for (Setting setting : this.visibleSettings(module)) {
            total += this.settingHeight(setting);
        }
        return total;
    }

    private float settingHeight(Setting setting) {
        if (setting instanceof BooleanSetting) {
            return H_BOOLEAN;
        }
        if (setting instanceof BindSetting) {
            return H_BIND;
        }
        if (setting instanceof ColorSetting) {
            return H_COLOR;
        }
        if (setting instanceof SliderSettings) {
            return H_SLIDER;
        }
        if (setting instanceof TextSetting) {
            return H_TEXT;
        }
        if (setting instanceof SelectSetting) {
            return this.chipsHeight(((SelectSetting)setting).getList());
        }
        if (setting instanceof MultiSelectSetting) {
            return this.chipsHeight(((MultiSelectSetting)setting).getList());
        }
        return 0.0f;
    }

    private float chipsHeight(List<String> values) {
        FontRenderer font = Fonts.getSize(14);
        float chipX = 0.0f;
        float height = 10.0f;
        for (String value : values) {
            float chipWidth = font.getStringWidth(value) + 10.0f;
            if (chipX + chipWidth > CHIP_MAX_WIDTH && chipX > 0.0f) {
                height += 16.5f;
                chipX = 0.0f;
            }
            chipX += chipWidth + 4.0f;
        }
        return height + 20.0f;
    }

    // ── мелкие помощники ─────────────────────────────────────────────────────

    private float animOf(Map<Module, Float> map, Module key, float target, float speed, float dt) {
        float current = map.getOrDefault(key, Float.valueOf(target)).floatValue();
        float next = CatlaGuiScreen.approach(current, target, speed, dt);
        map.put(key, Float.valueOf(next));
        return next;
    }

    private float animOf(Map<Setting, Float> map, Setting key, float target, float speed, float dt) {
        float current = map.getOrDefault(key, Float.valueOf(target)).floatValue();
        float next = CatlaGuiScreen.approach(current, target, speed, dt);
        map.put(key, Float.valueOf(next));
        return next;
    }

    private float chipAnimOf(Setting setting, int index, boolean selected, float dt) {
        String key = System.identityHashCode(setting) + ":" + index;
        float current = this.chipAnim.getOrDefault(key, Float.valueOf(selected ? 1.0f : 0.0f)).floatValue();
        float next = CatlaGuiScreen.approach(current, selected ? 1.0f : 0.0f, SPEED_CHIP, dt);
        this.chipAnim.put(key, Float.valueOf(next));
        return next;
    }

    private void rect(MatrixStack matrices, float x, float y, float width, float height, float round, int color) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.rectangle.render(ShapeProperties.create(matrices, x, y, width, height).round(round).color(color).build());
    }

    /** Заливка вертикальным градиентом (шейдер round: color1/2 — левый край, color3/4 — правый). */
    private void vRect(MatrixStack matrices, float x, float y, float width, float height, float round, int top, int bottom) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.rectangle.render(ShapeProperties.create(matrices, x, y, width, height).round(round).color(bottom, top, bottom, top).build());
    }

    /** Заливка горизонтальным градиентом. */
    private void hRect(MatrixStack matrices, float x, float y, float width, float height, float round, int left, int right) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.rectangle.render(ShapeProperties.create(matrices, x, y, width, height).round(round).color(left, left, right, right).build());
    }

    /** Градиентная заливка с однопиксельной обводкой. */
    private void outlineRect(MatrixStack matrices, float x, float y, float width, float height, float round, float thickness, int outline, int top, int bottom) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.rectangle.render(ShapeProperties.create(matrices, x, y, width, height).round(round).thickness(thickness).outlineColor(outline).color(bottom, top, bottom, top).build());
    }

    /** Мягкое свечение/тень: тот же прямоугольник с размытым краем. */
    private void glow(MatrixStack matrices, float x, float y, float width, float height, float round, float softness, int color) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.rectangle.render(ShapeProperties.create(matrices, x, y, width, height).round(round).softness(softness).color(color).build());
    }

    private void circle(MatrixStack matrices, float centerX, float centerY, float diameter, int color) {
        this.rect(matrices, centerX - diameter / 2.0f, centerY - diameter / 2.0f, diameter, diameter, diameter / 2.0f, color);
    }

    /** Квадрат насыщенность/яркость: белый → чистый тон по X, чёрная заливка по Y. */
    private void gradientQuad(MatrixStack matrices, float x, float y, float width, float height, int hueColor, int alpha) {
        int steps = 24;
        float stepWidth = width / (float)steps;
        for (int i = 0; i < steps; ++i) {
            float t = (float)i / (float)(steps - 1);
            int column = CatlaGuiScreen.lerpColor(-1, hueColor, t);
            this.rect(matrices, x + (float)i * stepWidth, y, stepWidth + 0.5f, height, 0.0f, CatlaGuiScreen.withAlpha(column, alpha));
        }
        int rows = 16;
        float stepHeight = height / (float)rows;
        for (int i = 0; i < rows; ++i) {
            float t = (float)(i + 1) / (float)rows;
            this.rect(matrices, x, y + (float)i * stepHeight, width, stepHeight + 0.5f, 0.0f, CatlaGuiScreen.withAlpha(0, (int)((float)alpha * t * 0.85f)));
        }
    }

    private void hueBar(MatrixStack matrices, float x, float y, float width, float height, int alpha) {
        int steps = 32;
        float stepWidth = width / (float)steps;
        for (int i = 0; i < steps; ++i) {
            int color = Color.HSBtoRGB((float)i / (float)steps, 1.0f, 1.0f);
            this.rect(matrices, x + (float)i * stepWidth, y, stepWidth + 0.5f, height, 0.0f, CatlaGuiScreen.withAlpha(color, alpha));
        }
    }

    private int fade(int color) {
        return CatlaGuiScreen.withAlpha(color, (int)(255.0f * this.openAnim));
    }

    private static String keyName(int key) {
        if (key <= 0) {
            return BIND_NONE;
        }
        String name = BindCodes.getDisplayName(key);
        return name == null || name.isEmpty() ? BIND_NONE : name;
    }

    private static String trimToWidth(FontRenderer font, String text, float width) {
        String result = text;
        while (!result.isEmpty() && font.getStringWidth(result) > width) {
            result = result.substring(1);
        }
        return result;
    }

    private static float sliderFraction(SliderSettings slider) {
        float span = slider.getMax() - slider.getMin();
        if (span <= 0.0f) {
            return 0.0f;
        }
        return CatlaGuiScreen.clamp((slider.getValue() - slider.getMin()) / span, 0.0f, 1.0f);
    }

    private static void applySliderValue(SliderSettings slider, float fraction) {
        float span = slider.getMax() - slider.getMin();
        float raw = slider.getMin() + span * CatlaGuiScreen.clamp(fraction, 0.0f, 1.0f);
        float step = slider.getStep() <= 0.0f ? 0.01f : slider.getStep();
        float stepped = Math.round(raw / step) * step;
        slider.setValue(CatlaGuiScreen.clamp(stepped, slider.getMin(), slider.getMax()));
    }

    private static String sliderText(SliderSettings slider) {
        if (slider.isInteger()) {
            return String.valueOf(slider.getInt());
        }
        return String.format(Locale.ROOT, "%.2f", Float.valueOf(slider.getValue()));
    }

    private static boolean inRect(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
    }

    private static float approach(float current, float target, float speed, float dt) {
        float factor = CatlaGuiScreen.clamp(speed * dt, 0.0f, 1.0f);
        float next = current + (target - current) * factor;
        if (Math.abs(next - target) < 0.001f) {
            next = target;
        }
        return next;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int rgb(int red, int green, int blue) {
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0xFFFFFF;
    }

    private static int lerpColor(int from, int to, float t) {
        float amount = CatlaGuiScreen.clamp(t, 0.0f, 1.0f);
        int a = (int)((float)(from >> 24 & 0xFF) + (float)((to >> 24 & 0xFF) - (from >> 24 & 0xFF)) * amount);
        int r = (int)((float)(from >> 16 & 0xFF) + (float)((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * amount);
        int g = (int)((float)(from >> 8 & 0xFF) + (float)((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * amount);
        int b = (int)((float)(from & 0xFF) + (float)((to & 0xFF) - (from & 0xFF)) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int brightness(int color, float factor) {
        int r = Math.max(0, Math.min(255, (int)((float)(color >> 16 & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, (int)((float)(color >> 8 & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, (int)((float)(color & 0xFF) * factor)));
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private record ModuleSlot(Module module, float x, float y, float expand, float settingsHeight, float openHeight) {
    }
}
