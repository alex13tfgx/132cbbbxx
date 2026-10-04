/*
 * ClickGUI в раскладке Polairis CsGui: сайдбар с иконками категорий и головой игрока,
 * шапка с названием раздела и поиском по центру, модули карточками в три колонки
 * (каждый модуль встаёт в самую короткую колонку), настройки раскрыты прямо в карточке.
 *
 * Всё рисуется в фиксированном дизайн-пространстве: одна единица = два физических пикселя,
 * эталон 960×540 — как у CsGui. Поэтому окно одинакового размера при любом GUI scale.
 * Метрики и цвета взяты из CsGui/CsStyle/Cs*Setting.
 */
package code.wiosvisual.display.screens.clickgui;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.display.screens.clickgui.ModuleDescriptions;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.Setting;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ButtonSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.GroupSetting;
import code.wiosvisual.features.module.setting.implement.MultiSelectSetting;
import code.wiosvisual.features.module.setting.implement.RadioSetting;
import code.wiosvisual.features.module.setting.implement.SectionSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.bind.BindCodes;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.display.scissor.ScissorAssist;
import code.wiosvisual.utils.theme.ThemeManager;
import code.wiosvisual.utils.config.ConfigManager;
import com.mojang.blaze3d.systems.RenderSystem;

import java.io.File;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class WiosGuiScreen
extends Screen {
    // ── геометрия CsGui (в дизайн-единицах, умножается на uiScale) ──────────────
    private static final float REF_W = 960.0f;
    private static final float REF_H = 540.0f;
    private static final float PANEL_W = 790.0f;
    private static final float PANEL_H = 475.0f;
    private static final float ROUNDING = 16.0f;
    private static final float HEADER_H = 60.0f;
    private static final int COLUMNS = 3;
    private static final float CONTENT_PAD = 13.0f;
    private static final float SCROLLBAR_W = 4.0f;
    private static final float SCROLLBAR_GAP = 4.0f;
    private static final float SEARCH_H = 24.0f;
    private static final float SEARCH_ICON = 9.5f;
    private static final float SEARCH_PAD = 5.0f;
    private static final float SEARCH_ROUND = 8.0f;
    private static final float SEARCH_MIN_W = 95.0f;
    private static final float SEARCH_RATIO = 0.23f;
    private static final float SEARCH_RESERVE = 96.0f;
    private static final float CARD_PAD = 10.0f;
    private static final float CARD_ROUND = 9.0f;
    /** Строка модуля: имя + описание в две строки (стиль ReallyVisuals), а не одна плашка. */
    private static final float MODULE_H = 30.0f;
    private static final float GAP_X = 10.0f;
    private static final float GAP_Y = 10.0f;
    private static final float TOGGLE_W = 22.0f * 0.92f;
    private static final float TOGGLE_H = 13.0f * 0.92f;
    private static final float SETTINGS_GAP = 3.0f;
    private static final float HEADER_TO_SETTINGS = 1.0f;
    private static final float SETTINGS_BOTTOM = 4.0f;
    private static final float SETTINGS_INSET = 5.0f;
    private static final float ROW_PAD = 2.5f;
    private static final float ROW_ICON = 8.0f;
    private static final float ROW_ICON_GAP = 3.0f;
    private static final float PICKER_H = 52.0f;
    /** Общий масштаб меню относительно CsGui: чуть меньше, чтобы окно «отодвинулось». */
    private static final float UI_ZOOM = 0.84f;
    /**
     * Модули, которые Wios держит включёнными всегда (их deactivate() сразу вызывает setState(true)).
     * Переключатель у них не рисуем: он бы оставался синим после клика. Сюда же попадает
     * любой модуль, который не выключился по клику из меню.
     */
    private static final java.util.Set<String> ALWAYS_ON = new java.util.HashSet<>(
            java.util.Arrays.asList("Hud", "ClickGui", "NoRender", "Globals", "ThemeEditor", "Emotions"));

    /** Оранжевый акцент ReallyVisuals — запасной, если тема клиента недоступна. */
    private static final int DEFAULT_ACCENT = 0xFFF2F2F4;
    /** Акцент меню: переключатели, чипы, значения слайдеров. Каждый кадр берётся из темы. */
    private static int ACCENT = DEFAULT_ACCENT;
    private static final int C_LABEL = 0xFFCED0D5;
    private static final int C_GRAY = 0xFF878D99;
    private static final int C_DIM = 0xFF5C6270;
    /** Избранные модули (звёздочка на карточке); хранится по имени. */
    private static final java.util.Set<String> FAVORITES = new java.util.HashSet<>();
    private boolean showFavorites;
    private boolean showAll = true;
    /** Модули с раскрытыми настройками: по умолчанию все свёрнуты, ЛКМ по карточке раскрывает. */
    private static final java.util.Set<Module> EXPANDED = new java.util.HashSet<>();
    private static final int C_ICON = 0xFF8A8E98;
    private static final int[] SWATCHES = {0xFF7EB6FF, 0xFFFFD700, 0xFF8673FA, 0xFFC5C6C8, 0xFFC34040, 0xFF98D8C8};
    private static final String SEARCH_HINT = "Поиск...";

    // ── скорости анимаций (единиц в секунду) ────────────────────────────────────
    private static final float SPEED_OPEN = 5.5f;
    private static final float SPEED_HOVER = 6.0f;
    private static final float SPEED_TOGGLE = 5.5f;
    private static final float SPEED_SCROLL = 6.5f;
    private static final float SPEED_DROP = 7.0f;
    private static final float SPEED_EXPAND = 4.5f;
    private static final float SPEED_CONTENT = 4.5f;

    private static WiosGuiScreen instance;

    private final Rectangle rectangle = new Rectangle();
    private final Map<Object, Float> hoverAnim = new HashMap<>();
    private final Map<Object, Float> toggleAnim = new HashMap<>();
    private final Map<Object, Float> expandAnim = new HashMap<>();
    private final Map<Object, Float> dropAnim = new HashMap<>();
    private final Map<ModuleCategory, Float> scroll = new HashMap<>();
    private final Map<ModuleCategory, Float> scrollTarget = new HashMap<>();
    private final Map<Object, Float> catAnim = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();

    private ModuleCategory[] categories = ModuleCategory.values();
    private ModuleCategory active = ModuleCategory.RENDER;
    private Module bindingModule;
    private Setting bindingSetting;
    private Setting editingText;
    private Setting openSelect;
    private ColorSetting openPicker;
    private SliderSettings draggingSlider;
    private float dragTrackX;
    private float dragTrackW;
    private ColorSetting draggingSquare;
    private ColorSetting draggingHue;
    private float pickX;
    private float pickY;
    private float pickSize;
    private boolean draggingScrollbar;
    private float sbTrackY;
    private float sbTrackH;
    private float sbThumbH;
    private float sbGrab;
    private float sbMaxScroll;
    private boolean searchFocused;
    private String searchText = "";
    private boolean closing;
    private float openAnim;
    private float contentAnim = 1.0f;
    private float barY = -1.0f;
    private long lastFrame = System.nanoTime();

    /** Кадровые величины: пересчитываются в начале каждого рендера. */
    private float s = 1.0f;
    private float factor = 1.0f;
    private float viewX;
    private float viewY;
    private float viewW;
    private float viewH;

    public WiosGuiScreen() {
        super(Text.literal("wios ui"));
    }

    public static WiosGuiScreen getInstance() {
        if (instance == null) {
            instance = new WiosGuiScreen();
        }
        return instance;
    }

    /** Точка входа: биндом или из ModuleSwitcher. */
    public void openGui() {
        this.closing = false;
        this.openAnim = 0.0f;
        this.contentAnim = 0.0f;
        this.searchFocused = false;
        this.searchText = "";
        this.bindingModule = null;
        this.bindingSetting = null;
        this.editingText = null;
        this.openSelect = null;
        this.openPicker = null;
        this.draggingSlider = null;
        this.draggingSquare = null;
        this.draggingHue = null;
        this.draggingScrollbar = false;
        this.lastFrame = System.nanoTime();
        SMOOTHED.clear();
        this.refreshCategories();
        MinecraftClient.getInstance().setScreen(this);
    }

    /** Открывает GUI сразу с поиском по имени модуля (например, из HudElementsToolbar). */
    public void presetSearch(String query) {
        this.searchText = query == null ? "" : query;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ── рендер ─────────────────────────────────────────────────────────────────

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (float)((now - this.lastFrame) / 1.0E9));
        this.lastFrame = now;
        this.openAnim = approach(this.openAnim, this.closing ? 0.0f : 1.0f, SPEED_OPEN, dt);
        // Экспонента никогда не доходит до единицы: без доводки меню вечно остаётся
        // уменьшенным на доли процента, и текст с линиями ложатся между пикселями.
        if (!this.closing && this.openAnim > 0.995f) {
            this.openAnim = 1.0f;
        }
        if (this.closing && this.openAnim <= 0.02f) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }
        this.contentAnim = approach(this.contentAnim, 1.0f, SPEED_CONTENT, dt);
        // Фиксированный оранжевый акцент (как у ватермарки и HUD-панелей Potions/Hot Keys/Cooldowns),
        // а не цвет пользовательской темы — единый брендинг ReallyVisuals-стиля по всему клиенту.
        ACCENT = DEFAULT_ACCENT;
        this.refreshCategories();
        this.hits.clear();

        MatrixStack matrices = GuiCompat.stack(context);
        float a = easeOut(this.openAnim);
        int guiW = context.getScaledWindowWidth();
        int guiH = context.getScaledWindowHeight();

        // Мир за меню просто затемняется: размытие клиента на весь экран даёт крупные пиксельные квадраты.
        this.rect(matrices, 0.0f, 0.0f, guiW, guiH, 0.0f, rgba(0, 0, 0, Math.round(150 * a)));
        Rectangle.flushPending();

        this.factor = (float)(2.0 / MinecraftClient.getInstance().getWindow().getScaleFactor());
        float dw = guiW / this.factor;
        float dh = guiH / this.factor;
        this.s = clamp(Math.min(dw / REF_W, dh / REF_H), 0.62f, 1.55f) * UI_ZOOM;
        float mx = mouseX / this.factor;
        float my = mouseY / this.factor;
        this.lastMouseX = mx;
        this.lastMouseY = my;

        matrices.push();
        matrices.scale(this.factor, this.factor, 1.0f);
        // Открытие: лёгкое увеличение из 94% вокруг центра.
        if (a < 1.0f) {
            float zoom = 0.94f + 0.06f * a;
            matrices.translate(dw / 2.0f, dh / 2.0f, 0.0f);
            matrices.scale(zoom, zoom, 1.0f);
            matrices.translate(-dw / 2.0f, -dh / 2.0f, 0.0f);
        }

        float pw = Math.min(PANEL_W * this.s, Math.max(280.0f, dw - 32.0f));
        float ph = Math.min(PANEL_H * this.s, Math.max(220.0f, dh - 32.0f));
        float x = Math.round((dw - pw) / 2.0f);
        float y = Math.round((dh - ph) / 2.0f);

        this.renderPanel(matrices, x, y, pw, ph, a);
        this.renderTitle(matrices, x, y, a);
        this.renderSearch(matrices, x, y, pw, mx, my, dt, a);
        this.renderSidebar(context, matrices, x, y, ph, mx, my, dt, a);
        this.renderModules(context, matrices, x, y, pw, ph, mx, my, dt, a);
        Rectangle.flushPending();
        matrices.pop();
    }

    private float rounding() {
        return ROUNDING * clamp(this.s, 0.85f, 1.2f);
    }

    private float miniW() {
        return 155.0f * this.s;
    }

    private float headerH() {
        return HEADER_H * this.s;
    }

    /** Фон окна: мягкая тень, тёмно-синяя панель, слегка светлее область контента и тонкий разделитель. */
    private void renderPanel(MatrixStack m, float x, float y, float pw, float ph, float a) {
        float r = this.rounding();
        this.glow(m, x - 10.0f, y - 8.0f, pw + 20.0f, ph + 20.0f, r + 10.0f, 34.0f, rgba(0, 0, 0, Math.round(150 * a)));
        Rectangle.flushPending();
        this.outline(m, x, y, pw, ph, r, 0.8f, rgba(255, 255, 255, Math.round(16 * a)), rgba(12, 12, 13, Math.round(240 * a)));
        Rectangle.flushPending();

        float miniW = this.miniW();
        float inset = 0.85f;
        float ri = Math.max(0.0f, r - inset);
        this.clipped(m, x + miniW, y, pw - miniW, ph, () -> this.rect(m, x + inset, y + inset, pw - inset * 2.0f, ph - inset * 2.0f, ri, rgba(12, 12, 13, 0)));
        float vPad = Math.max(r * 0.7f, 7.0f);
        
        
        Rectangle.flushPending();
    }

    private static final String[] SECTION_NAMES = {"Client categories", "Settings"};
    private static final ModuleCategory[] NAV_ORDER = {ModuleCategory.PLAYER, ModuleCategory.RENDER, ModuleCategory.MISC,
            ModuleCategory.COSMETICS, ModuleCategory.CUSTOMIZATION, ModuleCategory.CONFIGS};

    private static int sectionOf(ModuleCategory category) {
        return switch (category) {
            case PLAYER, RENDER, MISC, COSMETICS -> 0;
            default -> 1;
        };
    }

    private static String navName(ModuleCategory category) {
        return switch (category) {
            case PLAYER -> "Combat";
            case RENDER -> "Render";
            case MISC -> "Misc";
            case COSMETICS -> "Cosmetics";
            case CUSTOMIZATION -> "Themes";
            case CONFIGS -> "Configs";
        };
    }

    private static String navIcon(ModuleCategory category) {
        return switch (category) {
            case PLAYER -> "combat";
            case RENDER -> "render";
            case MISC -> "misc";
            case COSMETICS -> "sparkles";
            case CUSTOMIZATION -> "palette";
            case CONFIGS -> "doc";
        };
    }

    /** Заголовок шапки: иконка и название раздела. */
    private void renderTitle(MatrixStack m, float x, float y, float a) {
        float p = easeOut(this.contentAnim);
        float ta = a * p;
        float mid = y + this.headerH() / 2.0f;
        float icon = 12.0f * this.s;
        float cx = this.contentLeft(x) + (1.0f - p) * 10.0f * this.s;
        FontRenderer font = font(10.0f, true);
        float textY = font.getCenteredY(mid - 7.0f, 14.0f);
        String iconName = this.showFavorites ? "star" : this.showAll ? "sparkles" : navIcon(this.active);
        String name = this.showFavorites ? "Favorites" : this.showAll ? "All" : navName(this.active);
        this.dz(m, iconName, cx, mid - icon / 2.0f, icon, withAlpha(C_GRAY, Math.round(255 * ta)));
        cx += icon + 7.0f * this.s;
        font.drawString(m, name, cx, textY, rgba(228, 232, 242, Math.round(255 * ta)));
    }

    private float[] searchRect(float x, float y, float pw) {
        float h = SEARCH_H * this.s;
        float contentW = pw - this.miniW();
        float w = Math.min(258.0f * this.s, contentW * 0.48f);
        return new float[]{x + pw - 12.0f * this.s - w, y + (this.headerH() - h) / 2.0f, w, h};
    }

    private void renderSearch(MatrixStack m, float x, float y, float pw, float mx, float my, float dt, float a) {
        float[] r = this.searchRect(x, y, pw);
        float sx = r[0], sy = r[1], sw = r[2], sh = r[3];
        float hover = this.anim(this.hoverAnim, "search", inside(mx, my, sx, sy, sw, sh) ? 1.0f : 0.0f, SPEED_HOVER, dt);
        float open = this.searchFocused || !this.searchText.isEmpty() ? 1.0f : 0.0f;
        int fill = rgba(255, 255, 255, Math.round((7 + 4 * open + 3 * hover) * a));
        int border = this.searchFocused ? withAlpha(ACCENT, Math.round(150 * a)) : rgba(255, 255, 255, Math.round((16 + 8 * hover) * a));
        this.outline(m, sx, sy, sw, sh, SEARCH_ROUND * this.s, 0.85f, border, fill);
        Rectangle.flushPending();

        float iconSize = 10.0f * this.s;
        this.dz(m, "search", sx + 9.0f * this.s, sy + (sh - iconSize) / 2.0f, iconSize,
                withAlpha(this.searchFocused ? ACCENT : C_GRAY, Math.round(255 * a)));
        float textX = sx + 9.0f * this.s + iconSize + 7.0f * this.s;
        FontRenderer font = font(9.0f, false);
        if (open < 0.5f) {
            font.drawString(m, SEARCH_HINT, textX, font.getCenteredY(sy, sh), withAlpha(C_DIM, Math.round(255 * a)));
            float kb = 10.0f * this.s;
            float kbX = sx + sw - 10.0f * this.s - kb;
            this.dz(m, "keyboard", kbX, sy + (sh - kb) / 2.0f, kb, withAlpha(C_DIM, Math.round(255 * a)));
            FontRenderer hint = font(7.0f, true);
            String keys = "CTRL + F";
            hint.drawString(m, keys, kbX - 6.0f * this.s - hint.getStringWidth(keys), hint.getCenteredY(sy, sh), withAlpha(C_DIM, Math.round(255 * a)));
        } else {
            float closeSize = 8.0f * this.s;
            float closeX = sx + sw - closeSize - 10.0f * this.s;
            String shown = this.searchText;
            float maxW = closeX - textX - 4.0f * this.s;
            while (!shown.isEmpty() && font.getStringWidth(shown) > maxW) {
                shown = shown.substring(1);
            }
            font.drawString(m, shown, textX, font.getCenteredY(sy, sh), rgba(232, 234, 241, Math.round(255 * a)));
            if (this.searchFocused && System.currentTimeMillis() / 500L % 2L == 0L) {
                this.rect(m, textX + font.getStringWidth(shown) + 1.0f, sy + sh / 2.0f - 4.0f * this.s, 0.8f, 8.0f * this.s, 0.0f, rgba(232, 234, 241, Math.round(255 * a)));
                Rectangle.flushPending();
            }
            this.icon(m, "cross", closeX, sy + (sh - closeSize) / 2.0f, closeSize, rgba(170, 100, 100, Math.round(220 * a)));
            this.hits.add(new Hit(closeX - 2.0f, sy, closeSize + 6.0f, sh, false, button -> {
                this.searchText = "";
                this.searchFocused = false;
            }));
        }
        this.hits.add(new Hit(sx, sy, sw, sh, false, button -> {
            this.searchFocused = true;
            this.editingText = null;
        }));

        // Звезда слева от поиска: показывает только избранные модули.
        float star = 14.0f * this.s;
        float starX = sx - 21.0f * this.s - star / 2.0f;
        float starY = sy + (sh - star) / 2.0f;
        float starHover = this.anim(this.hoverAnim, "favBtn", inside(mx, my, starX, starY, star, star) ? 1.0f : 0.0f, SPEED_HOVER, dt);
        int starColor = this.showFavorites ? ACCENT : lerpColor(0xFFBBBFCB, 0xFFFFFFFF, starHover);
        this.dz(m, "star", starX, starY, star, withAlpha(starColor, Math.round(255 * a)));
        this.hits.add(new Hit(starX - 3.0f, starY - 3.0f, star + 6.0f, star + 6.0f, false, button -> this.showFavorites = !this.showFavorites));
    }

    /** Сайдбар: отдельная тёмная плашка, заголовок, секции с пунктами (первый — «All» со счётчиком), снизу профиль. */
    private void renderSidebar(DrawContext context, MatrixStack m, float x, float y, float ph, float mx, float my, float dt, float a) {
        float miniW = this.miniW();
        float S = this.s;
        float padX = 12.0f * S;
        float itemW = miniW - padX * 2.0f - 4.0f * S;
        float itemH = 22.0f * S;
        float pitch = 25.0f * S;

        this.outline(m, x + 6.0f * S, y + 6.0f * S, miniW - 6.0f * S, ph - 12.0f * S, this.rounding() * 0.7f, 0.8f,
                rgba(255, 255, 255, Math.round(9 * a)), rgba(19, 19, 20, Math.round(238 * a)));
        Rectangle.flushPending();

        FontRenderer titleFont = font(11.0f, true);
        this.dz(m, "sparkles", x + 18.0f * S, y + 24.0f * S, 12.0f * S, withAlpha(ACCENT, Math.round(255 * a)));
        titleFont.drawString(m, "Wios", x + 35.0f * S, titleFont.getCenteredY(y + 20.0f * S, 20.0f * S), rgba(240, 240, 244, Math.round(255 * a)));

        int total = 0;
        Wiosvisual client = Wiosvisual.getInstance();
        if (client != null && client.getModuleRepository() != null) {
            for (Module module : client.getModuleRepository().modules()) {
                if (!module.isHidden()) ++total;
            }
        }

        List<Object[]> items = new ArrayList<>();
        items.add(new Object[]{"All", "sparkles", null, 0});
        for (ModuleCategory category : NAV_ORDER) {
            if (Arrays.asList(this.categories).contains(category)) {
                items.add(new Object[]{navName(category), navIcon(category), category, sectionOf(category)});
            }
        }

        FontRenderer labelFont = font(7.4f, false);
        FontRenderer itemFont = font(9.2f, true);
        FontRenderer badgeFont = font(7.2f, true);
        float iconSize = 12.0f * S;
        float cursor = y + 56.0f * S;
        for (int sec = 0; sec < SECTION_NAMES.length; ++sec) {
            boolean any = false;
            for (Object[] item : items) {
                if ((Integer)item[3] != sec) continue;
                if (!any) {
                    any = true;
                    labelFont.drawString(m, SECTION_NAMES[sec], x + 18.0f * S, labelFont.getCenteredY(cursor, 10.0f * S), withAlpha(C_DIM, Math.round(255 * a)));
                    cursor += 16.0f * S;
                }
                String name = (String)item[0];
                ModuleCategory category = (ModuleCategory)item[2];
                float iy = cursor;
                cursor += pitch;
                boolean selected = !this.showFavorites && (category == null ? this.showAll : (!this.showAll && category == this.active));
                boolean hovered = inside(mx, my, x + padX, iy, itemW, itemH);
                float sel = this.anim(this.catAnim, name, selected ? 1.0f : 0.0f, SPEED_TOGGLE, dt);
                float hover = this.anim(this.hoverAnim, "nav" + name, hovered ? 1.0f : 0.0f, SPEED_HOVER, dt);
                float fillA = Math.max(sel * 16.0f, hover * 7.0f * (1.0f - sel));
                if (fillA > 0.2f) {
                    this.rect(m, x + padX, iy, itemW, itemH, 7.0f * S, rgba(255, 255, 255, Math.round(fillA * a)));
                    Rectangle.flushPending();
                }
                int color = withAlpha(lerpColor(lerpColor(0xFF9096A2, 0xFFD0D3DA, hover), 0xFFFFFFFF, sel), Math.round(255 * a));
                this.dz(m, (String)item[1], x + padX + 8.0f * S, iy + (itemH - iconSize) / 2.0f, iconSize, color);
                float textX = x + padX + 8.0f * S + iconSize + 7.0f * S;
                if (category == null) {
                    String count = String.valueOf(total);
                    float bw = badgeFont.getStringWidth(count) + 9.0f * S;
                    float bh = 12.0f * S;
                    float bx = x + padX + itemW - bw - 6.0f * S;
                    this.rect(m, bx, iy + (itemH - bh) / 2.0f, bw, bh, bh / 2.0f, rgba(255, 255, 255, Math.round(18 * a)));
                    Rectangle.flushPending();
                    badgeFont.drawString(m, count, bx + (bw - badgeFont.getStringWidth(count)) / 2.0f, badgeFont.getCenteredY(iy + (itemH - bh) / 2.0f, bh), withAlpha(0xFFD0D3DA, Math.round(255 * a)));
                    drawClipped(m, itemFont, name, textX, itemFont.getCenteredY(iy, itemH), bx - textX - 4.0f * S, color);
                } else {
                    drawClipped(m, itemFont, name, textX, itemFont.getCenteredY(iy, itemH), x + padX + itemW - textX - 4.0f * S, color);
                }
                this.hits.add(new Hit(x + padX, iy, itemW, itemH, false, button -> {
                    this.showFavorites = false;
                    if (category == null) {
                        if (!this.showAll) {
                            this.showAll = true;
                            this.searchText = "";
                            this.searchFocused = false;
                            this.openSelect = null;
                            this.openPicker = null;
                            this.contentAnim = 0.0f;
                        }
                    } else {
                        this.showAll = false;
                        this.selectCategory(category);
                    }
                }));
            }
            if (any) cursor += 12.0f * S;
        }

        // Профиль внизу: круглый аватар с буквой, ник и подпись.
        float face = 24.0f * S;
        float faceX = x + 16.0f * S;
        float faceY = y + ph - 28.0f * S - face / 2.0f;
        AbstractClientPlayerEntity player = MinecraftClient.getInstance().player;
        String nick = player == null ? "Player" : player.getName().getString();
        this.outline(m, faceX, faceY, face, face, face / 2.0f, 0.8f, rgba(255, 255, 255, Math.round(22 * a)), rgba(10, 10, 11, Math.round(255 * a)));
        Rectangle.flushPending();
        FontRenderer letterFont = font(9.5f, true);
        String letter = nick.isEmpty() ? "P" : nick.substring(0, 1).toUpperCase(Locale.ROOT);
        letterFont.drawString(m, letter, faceX + (face - letterFont.getStringWidth(letter)) / 2.0f, letterFont.getCenteredY(faceY, face), rgba(240, 240, 244, Math.round(255 * a)));
        float textX = faceX + face + 8.0f * S;
        float textW = miniW - (textX - x) - 14.0f * S;
        FontRenderer nickFont = font(9.0f, true);
        FontRenderer roleFont = font(7.5f, false);
        drawClipped(m, nickFont, nick, textX, nickFont.getCenteredY(faceY, face * 0.5f), textW, rgba(240, 242, 248, Math.round(255 * a)));
        drawClipped(m, roleFont, "Main Menu", textX, roleFont.getCenteredY(faceY + face * 0.5f, face * 0.5f), textW, withAlpha(C_DIM, Math.round(255 * a)));
    }

    /** Иконка 64×64 из assets/wiosvisual/icons/dz, сглаженная и подкрашенная цветом. */
    private void dz(MatrixStack m, String name, float x, float y, float size, int color) {
        if (size <= 0.0f || (color >>> 24) == 0) return;
        Rectangle.flushPending();
        Identifier id = Identifier.of("wiosvisual", "icons/dz/" + name + ".png");
        Render2D.drawTexturedQuad(m, id, x, x + size, y, y + size, 0.0f, 1.0f, 0.0f, 1.0f, color);
    }

    private float contentLeft(float x) {
        return x + this.miniW() + 1.5f + CONTENT_PAD * this.s;
    }

    /** Три колонки карточек: каждая следующая встаёт в самую короткую колонку. */
    private void renderModules(DrawContext context, MatrixStack m, float x, float y, float pw, float ph, float mx, float my, float dt, float a) {
        float r = this.rounding();
        float miniW = this.miniW();
        float headerH = this.headerH();
        float pad = CONTENT_PAD * this.s;
        float areaX = x + miniW + 1.5f;
        float areaY = y + headerH + 1.5f;
        float areaW = pw - miniW - 1.5f - r * 0.45f;
        float areaH = ph - headerH - 1.5f - r * 0.45f;
        this.viewX = areaX + pad;
        this.viewY = areaY + pad;
        this.viewW = areaW - pad * 2.0f - SCROLLBAR_W - SCROLLBAR_GAP;
        this.viewH = areaH - pad * 2.0f;

        if (this.active == ModuleCategory.CONFIGS && !this.showAll && this.searchText.isEmpty() && !this.showFavorites) {
            this.renderConfigsPage(m, mx, my, dt, a);
            return;
        }

        List<Module> modules = this.modules();
        if (modules.isEmpty()) {
            FontRenderer font = font(10.0f, true);
            String title = this.searchText.isEmpty() ? "No modules" : "Nothing found";
            font.drawString(m, title, this.viewX + (this.viewW - font.getStringWidth(title)) / 2.0f, this.viewY + this.viewH * 0.42f,
                    rgba(146, 150, 162, Math.round(255 * a)));
            return;
        }
        float moduleW = (this.viewW - GAP_X * this.s * (COLUMNS - 1)) / COLUMNS;
        float baseH = MODULE_H * this.s;
        float gapY = GAP_Y * this.s;
        float innerW = moduleW - SETTINGS_INSET * this.s * 2.0f;

        float[] heights = new float[modules.size()];
        float[] cols = new float[COLUMNS];
        for (int i = 0; i < modules.size(); ++i) {
            float ex = this.anim(this.expandAnim, modules.get(i), 1.0f, SPEED_EXPAND, dt);
            float settingsH = ex > 0.004f ? this.settingsHeight(modules.get(i), innerW, dt) * (ex * ex * (3.0f - 2.0f * ex)) : 0.0f;
            heights[i] = settingsH > 0.0f ? baseH + HEADER_TO_SETTINGS * this.s + settingsH : baseH;
            int col = pickColumn(cols);
            cols[col] += heights[i] + gapY;
        }
        float total = Math.max(0.0f, maxOf(cols) - gapY);
        float maxScroll = Math.max(0.0f, total - this.viewH);
        this.sbMaxScroll = maxScroll;
        float target = clamp(this.scrollTarget.getOrDefault(this.scrollKey(), 0.0f), 0.0f, maxScroll);
        this.scrollTarget.put(this.scrollKey(), target);
        float offset = clamp(approach(this.scroll.getOrDefault(this.scrollKey(), 0.0f), target, SPEED_SCROLL, dt), 0.0f, maxScroll);
        if (Math.abs(offset - target) < 0.15f) offset = target;
        this.scroll.put(this.scrollKey(), offset);

        float fade = easeOut(this.contentAnim);
        float slide = (1.0f - fade) * 10.0f * this.s;
        ScissorAssist scissor = Wiosvisual.getInstance() == null ? null : Wiosvisual.getInstance().getScissorManager();
        if (scissor != null) {
            Rectangle.flushPending();
            scissor.push(m.peek().getPositionMatrix(), this.viewX, this.viewY, this.viewW, this.viewH);
        }
        Arrays.fill(cols, this.viewY + slide - offset);
        float ma = a * Math.max(0.15f, fade);
        for (int i = 0; i < modules.size(); ++i) {
            Module module = modules.get(i);
            int col = pickColumn(cols);
            float h = heights[i];
            float cx = this.viewX + col * (moduleW + GAP_X * this.s);
            float cy = cols[col];
            cols[col] += h + gapY;
            if (cy + h < this.viewY - 24.0f || cy > this.viewY + this.viewH + 24.0f) {
                continue;
            }
            this.renderCard(m, module, cx, cy, moduleW, h, innerW, mx, my, dt, ma);
        }
        if (scissor != null) {
            Rectangle.flushPending();
            scissor.pop();
        }

        // Скроллбар: почти невидимая дорожка и серый бегунок.
        if (maxScroll > 0.5f) {
            float tx = this.viewX + this.viewW + SCROLLBAR_GAP;
            float thumbH = Math.max(24.0f, this.viewH * (this.viewH / (this.viewH + maxScroll)));
            float thumbY = this.viewY + offset / maxScroll * Math.max(1.0f, this.viewH - thumbH);
            this.sbTrackY = this.viewY;
            this.sbTrackH = this.viewH;
            this.sbThumbH = thumbH;
            this.rect(m, tx, this.viewY, SCROLLBAR_W, this.viewH, SCROLLBAR_W / 2.0f, rgba(165, 165, 168, Math.round(10 * a)));
            this.rect(m, tx, thumbY, SCROLLBAR_W, thumbH, SCROLLBAR_W / 2.0f, rgba(255, 255, 255, Math.round((this.draggingScrollbar ? 70 : 40) * a)));
            Rectangle.flushPending();
            float grabY = thumbY;
            this.hits.add(new Hit(tx - 2.0f, this.viewY, SCROLLBAR_W + 4.0f, this.viewH, false, button -> {
                this.draggingScrollbar = true;
                float mouse = this.lastMouseY;
                this.sbGrab = mouse >= grabY && mouse <= grabY + thumbH ? mouse - grabY : thumbH / 2.0f;
                this.dragScrollbar(mouse);
            }));
        }
    }

    private float lastMouseY;

    private ModuleCategory scrollKey() {
        return this.showAll ? null : this.active;
    }

    /**
     * Страница «Конфигурации»: список сохранённых конфигов вместо сетки модулей.
     * Активная (последняя загруженная/сохранённая) строка — сплошная акцентная заливка,
     * остальные — тёмные плашки; справа у каждой — сохранить/загрузить/удалить.
     */
    private void renderConfigsPage(MatrixStack m, float mx, float my, float dt, float a) {
        List<String> configs = new ArrayList<>(Arrays.asList(ConfigManager.getAvailableConfigs()));
        Collections.sort(configs);
        String active = ConfigManager.getActiveConfig();

        float rowH = 34.0f * this.s;
        float gapY = 7.0f * this.s;
        float pad = 10.0f * this.s;
        float total = configs.size() * (rowH + gapY);
        float maxScroll = Math.max(0.0f, total - gapY - this.viewH);
        float target = clamp(this.scrollTarget.getOrDefault(ModuleCategory.CONFIGS, 0.0f), 0.0f, maxScroll);
        this.scrollTarget.put(ModuleCategory.CONFIGS, target);
        float offset = clamp(approach(this.scroll.getOrDefault(ModuleCategory.CONFIGS, 0.0f), target, SPEED_SCROLL, dt), 0.0f, maxScroll);
        this.scroll.put(ModuleCategory.CONFIGS, offset);

        ScissorAssist scissor = Wiosvisual.getInstance() == null ? null : Wiosvisual.getInstance().getScissorManager();
        if (scissor != null) {
            Rectangle.flushPending();
            scissor.push(m.peek().getPositionMatrix(), this.viewX, this.viewY, this.viewW, this.viewH);
        }

        FontRenderer nameFont = font(10.0f, true);
        FontRenderer metaFont = font(7.5f, false);
        float cy = this.viewY - offset;
        for (String name : configs) {
            if (cy + rowH >= this.viewY - 12.0f && cy <= this.viewY + this.viewH + 12.0f) {
                this.renderConfigRow(m, name, name.equals(active), this.viewX, cy, this.viewW, rowH, nameFont, metaFont, mx, my, a);
            }
            cy += rowH + gapY;
        }
        if (configs.isEmpty()) {
            String title = "Нет сохранённых конфигов";
            FontRenderer font = font(10.0f, true);
            font.drawString(m, title, this.viewX + (this.viewW - font.getStringWidth(title)) / 2.0f, this.viewY + this.viewH * 0.42f,
                    rgba(146, 150, 162, Math.round(255 * a)));
        }
        if (scissor != null) {
            Rectangle.flushPending();
            scissor.pop();
        }

        if (maxScroll > 0.5f) {
            float tx = this.viewX + this.viewW + SCROLLBAR_GAP;
            float thumbH = Math.max(24.0f, this.viewH * (this.viewH / (this.viewH + maxScroll)));
            float thumbY = this.viewY + offset / maxScroll * Math.max(1.0f, this.viewH - thumbH);
            this.rect(m, tx, this.viewY, SCROLLBAR_W, this.viewH, SCROLLBAR_W / 2.0f, rgba(165, 165, 168, Math.round(10 * a)));
            this.rect(m, tx, thumbY, SCROLLBAR_W, thumbH, SCROLLBAR_W / 2.0f, rgba(255, 255, 255, Math.round(40 * a)));
            Rectangle.flushPending();
        }
    }

    private void renderConfigRow(MatrixStack m, String name, boolean isActive, float x, float y, float w, float h,
                                  FontRenderer nameFont, FontRenderer metaFont, float mx, float my, float a) {
        boolean hovered = inside(mx, my, x, y, w, h) && this.inView(mx, my);
        float hover = this.anim(this.hoverAnim, "cfg" + name, hovered ? 1.0f : 0.0f, SPEED_HOVER, 1.0f / 60.0f);
        int bg = isActive
                ? withAlpha(ACCENT, Math.round(255 * a))
                : rgba(255, 255, 255, Math.round((6 + 6 * hover) * a));
        this.rect(m, x, y, w, h, CARD_ROUND, bg);
        Rectangle.flushPending();

        int textColor = isActive ? 0xFFFFFFFF : rgba(238, 239, 244, Math.round(255 * a));
        int metaColor = isActive ? rgba(255, 255, 255, Math.round(210 * a)) : rgba(150, 153, 162, Math.round(215 * a));
        drawClipped(m, nameFont, name, x + CARD_PAD * this.s, nameFont.getCenteredY(y + 7.5f * this.s, 12.0f), w * 0.55f, textColor);

        String meta = this.configMeta(name, isActive);
        drawClipped(m, metaFont, meta, x + CARD_PAD * this.s, metaFont.getCenteredY(y + h - 12.5f * this.s, 10.0f), w * 0.6f, metaColor);

        // Иконки справа: сохранить (перезаписать) / загрузить / удалить.
        float iconBox = 18.0f * this.s;
        float iconGap = 4.0f * this.s;
        float actionsX = x + w - CARD_PAD * this.s - iconBox * 3.0f - iconGap * 2.0f;
        int iconColor = isActive ? 0xFFFFFFFF : rgba(190, 193, 200, Math.round(255 * a));
        int iconBg = isActive ? 0x22FFFFFF : rgba(255, 255, 255, Math.round(10 * a));

        this.configActionIcon(m, actionsX, y + (h - iconBox) / 2.0f, iconBox, iconColor, iconBg, mx, my, a, "save",
                () -> ConfigManager.saveConfig(name));
        this.configActionIcon(m, actionsX + iconBox + iconGap, y + (h - iconBox) / 2.0f, iconBox, iconColor, iconBg, mx, my, a, "load",
                () -> ConfigManager.loadConfig(name));
        this.configActionIcon(m, actionsX + (iconBox + iconGap) * 2.0f, y + (h - iconBox) / 2.0f, iconBox, iconColor, iconBg, mx, my, a, "delete",
                () -> ConfigManager.deleteConfig(name));
    }

    private String configMeta(String name, boolean isActive) {
        File file = new File(Wiosvisual.getInstance().getClientInfoProvider().configsDir(), name + ".json");
        String ago = file.exists() ? WiosGuiScreen.timeAgo(System.currentTimeMillis() - file.lastModified()) : "—";
        return isActive ? "Сохранено " + ago + " • Активно" : "Сохранено " + ago;
    }

    private static String timeAgo(long deltaMs) {
        long minutes = Math.max(0L, deltaMs / 60000L);
        if (minutes < 1L) {
            return "только что";
        }
        if (minutes < 60L) {
            return minutes + " " + WiosGuiScreen.pluralMinutes(minutes) + " назад";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + " " + WiosGuiScreen.pluralHours(hours) + " назад";
        }
        long days = hours / 24L;
        return days + " " + WiosGuiScreen.pluralDays(days) + " назад";
    }

    private static String pluralMinutes(long n) {
        return WiosGuiScreen.pluralRu(n, "минуту", "минуты", "минут");
    }

    private static String pluralHours(long n) {
        return WiosGuiScreen.pluralRu(n, "час", "часа", "часов");
    }

    private static String pluralDays(long n) {
        return WiosGuiScreen.pluralRu(n, "день", "дня", "дней");
    }

    private static String pluralRu(long n, String one, String few, String many) {
        long mod10 = n % 10;
        long mod100 = n % 100;
        if (mod10 == 1L && mod100 != 11L) {
            return one;
        }
        if (mod10 >= 2L && mod10 <= 4L && (mod100 < 10L || mod100 >= 20L)) {
            return few;
        }
        return many;
    }

    /** Кружок с иконкой действия (save/load/delete) справа строки конфига. */
    private void configActionIcon(MatrixStack m, float x, float y, float size, int iconColor, int bg, float mx, float my, float a,
                                   String kind, Runnable action) {
        boolean hovered = inside(mx, my, x, y, size, size);
        float hover = this.anim(this.hoverAnim, "cfgIcon" + kind + x, hovered ? 1.0f : 0.0f, SPEED_HOVER, 1.0f / 60.0f);
        this.rect(m, x, y, size, size, size / 2.0f, lerpColor(bg, rgba(255, 255, 255, Math.round(60 * a)), hover));
        Rectangle.flushPending();
        float glyph = size * 0.42f;
        float gx = x + (size - glyph) / 2.0f;
        float gy = y + (size - glyph) / 2.0f;
        switch (kind) {
            case "save" -> {
                this.outline(m, gx, gy, glyph, glyph, 1.5f * this.s, 1.0f, iconColor, 0);
                this.rect(m, gx + glyph * 0.2f, gy, glyph * 0.6f, glyph * 0.35f, 0.6f, iconColor);
                Rectangle.flushPending();
            }
            case "load" -> {
                this.rect(m, gx + glyph / 2.0f - 0.7f * this.s, gy, 1.4f * this.s, glyph * 0.65f, 0.6f, iconColor);
                this.rect(m, gx, gy + glyph * 0.5f, glyph, glyph * 0.18f, 0.6f, iconColor);
                Rectangle.flushPending();
                // Стрелка вниз — два коротких штриха галочкой.
                this.rect(m, gx + glyph * 0.5f - 0.6f * this.s, gy + glyph * 0.35f, 1.2f * this.s, glyph * 0.32f, 0.6f, iconColor);
            }
            default -> this.icon(m, "cross", gx, gy, glyph, iconColor);
        }
        Rectangle.flushPending();
        this.hits.add(new Hit(x, y, size, size, false, button -> action.run()));
    }

    /**
     * Строка модуля в стиле ReallyVisuals: без рамки-карточки, имя жирным сверху,
     * описание серым под ним, тумблер прижат к правому краю колонки.
     */
    private void renderCard(MatrixStack m, Module module, float x, float y, float w, float h, float innerW, float mx, float my, float dt, float a) {
        float baseH = MODULE_H * this.s;
        float on = this.anim(this.toggleAnim, module, module.isState() ? 1.0f : 0.0f, SPEED_TOGGLE, dt);
        boolean hoveredRow = inside(mx, my, x, y, w, baseH) && this.inView(mx, my);
        float hover = this.anim(this.hoverAnim, "row" + module.getName(), hoveredRow ? 1.0f : 0.0f, SPEED_HOVER, dt);

        // Карточка: тёмная плашка с тонкой рамкой; на ховере чуть светлее.
        int cardBorder = lerpColor(rgba(255, 255, 255, Math.round((7 + 6 * hover) * a)), rgba(255, 255, 255, Math.round(24 * a)), on);
        int cardFill = lerpColor(rgba(20 + Math.round(3 * hover), 20 + Math.round(3 * hover), 21 + Math.round(3 * hover), Math.round(255 * a)),
                rgba(24, 24, 25, Math.round(255 * a)), on);
        this.outline(m, x, y, w, h, CARD_ROUND * this.s, 0.8f, cardBorder, cardFill);
        Rectangle.flushPending();

        float pad = CARD_PAD * this.s;
        float rowMid = y + 15.0f * this.s;
        float tw = 25.0f * this.s;
        float th = 14.0f * this.s;
        boolean locked = ALWAYS_ON.contains(module.getClass().getSimpleName()) || ALWAYS_ON.contains(module.getName());
        float toggleX = x + w - tw - pad;
        if (!locked) {
            this.drawToggle(m, toggleX, rowMid - th / 2.0f, tw, th, on, a);
        }

        // Звезда избранного слева от переключателя.
        float starSize = 10.0f * this.s;
        float starX = (locked ? x + w - pad : toggleX - 8.0f * this.s) - starSize;
        float starY = rowMid - starSize / 2.0f;
        boolean fav = FAVORITES.contains(module.getName());
        boolean starHover = inside(mx, my, starX - 2.0f, starY - 2.0f, starSize + 4.0f, starSize + 4.0f) && this.inView(mx, my);
        int starColor = fav ? ACCENT : (starHover ? 0xFFE3E6EF : 0xFFBBBFCB);
        this.dz(m, "star", starX, starY, starSize, withAlpha(starColor, Math.round(255 * a)));

        float reserve = x + w - starX + 6.0f * this.s;
        boolean binding = this.bindingModule == module;
        int key = module.getKey();
        final float[] bindRect = new float[4];
        {
            String shown = binding ? "..." : key > 0 ? BindCodes.getDisplayName(key) : "None";
            FontRenderer keyFont = font(8.0f, true);
            float boxH = 14.0f * this.s;
            float boxW = Math.max(boxH, keyFont.getStringWidth(shown) + 8.0f * this.s);
            float bindX = starX - boxW - 6.0f * this.s;
            float bindY = rowMid - boxH / 2.0f;
            reserve = x + w - bindX + 6.0f * this.s;
            bindRect[0] = bindX; bindRect[1] = bindY; bindRect[2] = boxW; bindRect[3] = boxH;
            this.outline(m, bindX, bindY, boxW, boxH, 4.0f * this.s, 0.9f, rgba(255, 255, 255, Math.round((binding ? 45 : 18) * a)),
                    binding ? withAlpha(ACCENT, Math.round(255 * a)) : rgba(165, 165, 168, Math.round(12 * a)));
            Rectangle.flushPending();
            keyFont.drawString(m, shown, bindX + (boxW - keyFont.getStringWidth(shown)) / 2.0f, keyFont.getCenteredY(bindY, boxH), rgba(240, 242, 248, Math.round(255 * a)));
            this.hits.add(new Hit(bindX, bindY, boxW, boxH, true, button -> {
                if (button == 1) {
                    module.setKey(-1);
                    this.bindingModule = null;
                } else {
                    this.bindingModule = module;
                    this.bindingSetting = null;
                }
            }));
        }

        float textW = Math.max(8.0f * this.s, w - pad - reserve);
        FontRenderer nameFont = font(10.5f, true);
        drawClipped(m, nameFont, cardTitle(module), x + pad, nameFont.getCenteredY(rowMid - 7.0f * this.s, 14.0f * this.s), textW,
                rgba(204, 208, 216, Math.round(255 * a)));

        String description = "";
        if (false) {
            FontRenderer descFont = font(8.2f, false);
            float descMid = y + 34.0f * this.s;
            drawClipped(m, descFont, description, x + pad, descFont.getCenteredY(descMid - 5.0f * this.s, 10.0f * this.s), w - pad * 2.0f,
                    rgba(112, 116, 126, Math.round(255 * a)));
        }

        // Строка целиком: ЛКМ включает модуль, СКМ — назначить бинд, ПКМ — снять бинд.
        this.hits.add(new Hit(x, y, w, baseH, true, button -> {
            if (button == 2) {
                this.bindingModule = module;
                this.bindingSetting = null;
            } else if (button == 1) {
                if (inside(this.lastMouseX, this.lastMouseY, bindRect[0], bindRect[1], bindRect[2], bindRect[3])) {
                    module.setKey(-1);
                } else if (!visibleSettings(module).isEmpty() && !EXPANDED.remove(module)) {
                    EXPANDED.add(module);
                }
            }
        }));
        if (!locked) {
            this.hits.add(new Hit(toggleX - 3.0f, rowMid - th / 2.0f - 3.0f, tw + 6.0f, th + 6.0f, true, button -> {
                if (button != 0) return;
                boolean wasOn = module.isState();
                module.switchState();
                if (wasOn && module.isState()) {
                    // Модуль включил себя обратно — дальше показываем его как неотключаемый.
                    ALWAYS_ON.add(module.getName());
                }
            }));
        }

        this.hits.add(new Hit(starX - 2.0f, starY - 2.0f, starSize + 4.0f, starSize + 4.0f, true, button -> {
            if (button == 0 && !FAVORITES.remove(module.getName())) {
                FAVORITES.add(module.getName());
            }
        }));

        if (h > baseH + 2.0f) {
            this.rect(m, x + CARD_PAD * this.s, y + baseH - 0.5f, w - CARD_PAD * this.s * 2.0f, 0.8f, 0.0f, rgba(165, 165, 168, Math.round(10 * a)));
            Rectangle.flushPending();
            float sa = a * clamp(this.expandAnim.getOrDefault(module, 1.0f) * 1.4f - 0.2f, 0.0f, 1.0f);
            float sx = x + SETTINGS_INSET * this.s;
            this.clipped(m, x, y + baseH, w, h - baseH, () -> {
                float sy = y + baseH + HEADER_TO_SETTINGS * this.s;
                for (Setting setting : visibleSettings(module)) {
                    float sh = this.settingHeight(setting, innerW);
                    this.renderSetting(m, setting, sx, sy, innerW, sh, mx, my, dt, sa);
                    sy += sh + SETTINGS_GAP * this.s;
                }
            });
        }
    }

    // ── настройки ──────────────────────────────────────────────────────────────

    private float settingsHeight(Module module, float innerW, float dt) {
        float total = 0.0f;
        for (Setting setting : visibleSettings(module)) {
            if (setting instanceof SelectSetting || setting instanceof RadioSetting) {
                this.anim(this.dropAnim, setting, this.openSelect == setting ? 1.0f : 0.0f, SPEED_DROP, dt);
            }
            if (setting instanceof ColorSetting) {
                this.anim(this.dropAnim, setting, this.openPicker == setting ? 1.0f : 0.0f, SPEED_DROP, dt);
            }
            total += this.settingHeight(setting, innerW) + SETTINGS_GAP * this.s;
        }
        return total > 0.0f ? total + SETTINGS_BOTTOM * this.s : 0.0f;
    }

    private float settingHeight(Setting setting, float width) {
        if (setting instanceof SliderSettings) {
            return 28.0f * this.s;
        }
        if (setting instanceof SelectSetting select) {
            return this.dropdownHeight(setting, select.getList().size());
        }
        if (setting instanceof RadioSetting radio) {
            return this.dropdownHeight(setting, radio.getOptions().length);
        }
        if (setting instanceof MultiSelectSetting multi) {
            return this.chipLayout(multi, 0.0f, 0.0f, width, null);
        }
        if (setting instanceof ColorSetting) {
            return (8.5f + 3.0f + 13.0f + 1.0f) * this.s + (PICKER_H + 4.0f) * this.s * this.dropAnim.getOrDefault(setting, 0.0f);
        }
        if (setting instanceof TextSetting) {
            return (8.5f + 3.0f + 15.0f + 1.0f) * this.s;
        }
        if (setting instanceof ButtonSetting) {
            return 18.0f * this.s;
        }
        return 16.0f * this.s;
    }

    private float dropdownHeight(Setting setting, int options) {
        float collapsed = (8.5f + 3.0f + 18.0f + 1.0f) * this.s;
        float ep = this.dropAnim.getOrDefault(setting, 0.0f);
        return ep <= 0.01f ? collapsed : collapsed + (2.0f + options * 17.0f + 6.0f) * this.s * ep;
    }

    private void renderSetting(MatrixStack m, Setting setting, float x, float y, float w, float h, float mx, float my, float dt, float a) {
        int alpha = Math.round(255 * a);
        if (setting instanceof BooleanSetting bool) {
            this.renderCheckRow(m, setting, bool.isValue(), x, y, w, mx, my, dt, alpha, () -> bool.setValue(!bool.isValue()));
        } else if (setting instanceof GroupSetting group) {
            this.renderCheckRow(m, setting, group.isValue(), x, y, w, mx, my, dt, alpha, () -> group.setValue(!group.isValue()));
        } else if (setting instanceof SectionSetting section) {
            this.renderCheckRow(m, setting, section.isOn(), x, y, w, mx, my, dt, alpha, section::toggle);
        } else if (setting instanceof SliderSettings slider) {
            this.renderSlider(m, slider, x, y, w, mx, my, dt, alpha);
        } else if (setting instanceof SelectSetting select) {
            this.renderDropdown(m, setting, select.getSelected(), select.getList(), select::setSelected, x, y, w, mx, my, dt, alpha);
        } else if (setting instanceof RadioSetting radio) {
            this.renderDropdown(m, setting, radio.getValue(), Arrays.asList(radio.getOptions()), radio::setValue, x, y, w, mx, my, dt, alpha);
        } else if (setting instanceof MultiSelectSetting multi) {
            this.renderMulti(m, multi, x, y, w, mx, my, dt, alpha);
        } else if (setting instanceof ColorSetting color) {
            this.renderColor(m, color, x, y, w, mx, my, dt, alpha);
        } else if (setting instanceof BindSetting bind) {
            this.renderBind(m, bind, x, y, w, alpha);
        } else if (setting instanceof TextSetting text) {
            this.renderText(m, text, x, y, w, alpha);
        } else if (setting instanceof ButtonSetting button) {
            this.renderButton(m, button, x, y, w, mx, my, dt, alpha);
        } else {
            FontRenderer font = font(8.5f, false);
            drawClipped(m, font, setting.getName(), x + ROW_PAD * this.s, font.getCenteredY(y, h), w, withAlpha(C_LABEL, alpha));
        }
    }

    private float labelX(float x) {
        return x + (ROW_PAD + ROW_ICON + ROW_ICON_GAP) * this.s;
    }

    /** Булева настройка: иконка-пилюля слева, иконка клавиатуры и квадратный чекбокс справа. */
    private void renderCheckRow(MatrixStack m, Setting setting, boolean value, float x, float y, float w, float mx, float my, float dt, int alpha, Runnable toggle) {
        float h = 16.0f * this.s;
        boolean hovered = inside(mx, my, x, y, w, h) && this.inView(mx, my);
        float hp = this.anim(this.hoverAnim, setting, hovered ? 1.0f : 0.0f, SPEED_HOVER, dt);
        float p = this.anim(this.toggleAnim, setting, value ? 1.0f : 0.0f, SPEED_TOGGLE, dt);

        // Иконка типа — маленькая пилюля переключателя.
        float iw = 9.5f * this.s;
        float ih = 5.5f * this.s;
        float ix = x + ROW_PAD * this.s - 0.5f * this.s;
        float iy = y + (h - ih) / 2.0f;
        this.outline(m, ix, iy, iw, ih, ih / 2.0f, 1.1f, withAlpha(C_ICON, alpha), 0);
        float dot = ih - 2.8f * this.s;
        this.rect(m, ix + 1.4f * this.s, iy + (ih - dot) / 2.0f, dot, dot, dot / 2.0f, withAlpha(C_ICON, alpha));

        float box = 12.5f * this.s * (1.0f + 0.03f * hp);
        float right = x + w - ROW_PAD * this.s;
        float boxX = right - 12.5f * this.s + (12.5f * this.s - box) / 2.0f;
        float boxY = y + (h - box) / 2.0f;
        int boxColor = lerpColor(rgba(255, 255, 255, Math.round(alpha * (0.10f + 0.05f * hp))), withAlpha(ACCENT, Math.round(alpha * 0.95f)), p);
        this.outline(m, boxX, boxY, box, box, 3.5f * this.s, 0.7f,
                rgba(255, 255, 255, Math.round(alpha * (0.16f + 0.08f * hp) * (1.0f - p))), boxColor);
        Rectangle.flushPending();
        if (p > 0.05f) {
            float inset = box * 0.16f;
            this.icon(m, "checkmark", boxX + inset, boxY + inset, box - inset * 2.0f, rgba(14, 14, 15, Math.round(alpha * p)));
        }
        float keySize = 9.0f * this.s;
        float keyX = boxX - 4.5f * this.s - keySize;
        this.icon(m, "keyboard", keyX, y + (h - keySize) / 2.0f, keySize, lerpColor(withAlpha(C_ICON, alpha), withAlpha(ACCENT, alpha), hp * 0.55f));

        FontRenderer font = font(8.5f, false);
        float lx = this.labelX(x);
        drawClipped(m, font, setting.getName(), lx, font.getCenteredY(y, h), Math.max(0.0f, keyX - lx - 6.0f * this.s),
                lerpColor(withAlpha(C_LABEL, alpha), withAlpha(ACCENT, alpha), p));
        this.hits.add(new Hit(x, y, w, h, true, button -> {
            if (button == 0) toggle.run();
        }));
    }

    /** Слайдер: название и значение сверху, дорожка 3px с делениями, подписи min / mid / max. */
    private void renderSlider(MatrixStack m, SliderSettings slider, float x, float y, float w, float mx, float my, float dt, int alpha) {
        float rowX = this.labelX(x);
        float rowW = Math.max(8.0f * this.s, w - (ROW_PAD + ROW_ICON + ROW_ICON_GAP) * this.s - ROW_PAD * this.s);
        float trackH = 3.0f * this.s;
        float barY = y + 15.0f * this.s;
        boolean dragging = this.draggingSlider == slider;
        if (dragging) {
            this.dragTrackX = rowX;
            this.dragTrackW = rowW;
        }
        boolean hovered = inside(mx, my, rowX, barY - 6.0f * this.s, rowW, trackH + 14.0f * this.s) && this.inView(mx, my);
        float hp = this.anim(this.hoverAnim, slider, hovered || dragging ? 1.0f : 0.0f, SPEED_HOVER, dt);
        float range = Math.max(1.0E-4f, slider.getMax() - slider.getMin());
        float p = this.anim(this.toggleAnim, slider, clamp((slider.getValue() - slider.getMin()) / range, 0.0f, 1.0f), dragging ? 60.0f : 18.0f, dt);

        float iconSize = ROW_ICON * this.s;
        this.icon(m, "arrow_horizontal", x + ROW_PAD * this.s, y + (13.0f * this.s - iconSize) / 2.0f + this.s, iconSize, withAlpha(C_ICON, alpha));
        FontRenderer name = font(8.5f, false);
        FontRenderer value = font(8.0f, true);
        String valueText = formatValue(slider.getValue(), slider.getStep(), slider.isInteger());
        float vw = value.getStringWidth(valueText);
        drawClipped(m, name, slider.getName(), rowX, y + 1.5f * this.s, Math.max(8.0f * this.s, rowW - vw - 8.0f * this.s), withAlpha(C_LABEL, alpha));
        value.drawString(m, valueText, rowX + rowW - vw, y + 2.0f * this.s, withAlpha(ACCENT, alpha));

        this.rect(m, rowX, barY, rowW, trackH, trackH / 2.0f, rgba(165, 165, 168, Math.round(alpha * 0.12f)));
        float step = slider.getStep();
        if (step > 0.0f) {
            int count = Math.round(range / step);
            if (count > 0 && count <= 50) {
                int tick = rgba(165, 165, 168, Math.round(alpha * 0.14f));
                for (int i = 1; i < count; ++i) {
                    this.rect(m, rowX + rowW * i / count - 0.5f, barY + 0.6f * this.s, 1.0f, trackH - 1.2f * this.s, 0.0f, tick);
                }
            }
        }
        float fw = rowW * p;
        if (fw > 0.5f) {
            this.rect(m, rowX, barY, fw, trackH, trackH / 2.0f, lerpColor(withAlpha(ACCENT, alpha), withAlpha(ACCENT, Math.round(alpha * 0.85f)), hp));
        }
        float kr = 4.0f * this.s + this.s * hp + (dragging ? 0.6f * this.s : 0.0f);
        float kx = rowX + fw;
        float ky = barY + trackH / 2.0f;
        this.rect(m, kx - kr - 1.0f, ky - kr - 1.0f, (kr + 1.0f) * 2.0f, (kr + 1.0f) * 2.0f, kr + 1.0f, rgba(0, 0, 0, Math.round(alpha * 0.2f)));
        this.rect(m, kx - kr, ky - kr, kr * 2.0f, kr * 2.0f, kr, rgba(245, 246, 250, alpha));
        Rectangle.flushPending();

        FontRenderer small = font(7.0f, false);
        int lc = rgba(150, 154, 166, Math.round(alpha * 0.85f));
        float ly = barY + trackH + 3.5f * this.s;
        String minS = formatValue(slider.getMin(), step, slider.isInteger());
        String midS = formatValue(slider.getMin() + range / 2.0f, step, slider.isInteger());
        String maxS = formatValue(slider.getMax(), step, slider.isInteger());
        small.drawString(m, minS, rowX, ly, lc);
        small.drawString(m, midS, rowX + rowW / 2.0f - small.getStringWidth(midS) / 2.0f, ly, lc);
        small.drawString(m, maxS, rowX + rowW - small.getStringWidth(maxS), ly, lc);

        this.hits.add(new Hit(rowX, barY - 6.0f * this.s, rowW, trackH + 14.0f * this.s, true, button -> {
            if (button == 0) {
                this.draggingSlider = slider;
                this.dragTrackX = rowX;
                this.dragTrackW = rowW;
                this.dragSlider(this.lastMouseX);
            }
        }));
    }

    private float lastMouseX;

    /** Выпадающий список: рамка с иконкой списка и стрелкой, раскрывается плавно внутрь карточки. */
    private void renderDropdown(MatrixStack m, Setting setting, String current, List<String> options, java.util.function.Consumer<String> apply,
            float x, float y, float w, float mx, float my, float dt, int alpha) {
        float pad = 2.0f * this.s;
        float rowX = x + pad;
        float rowW = Math.max(8.0f * this.s, w - pad * 2.0f);
        FontRenderer name = font(8.5f, false);
        drawClipped(m, name, setting.getName(), rowX, y, rowW, withAlpha(C_LABEL, alpha));
        float boxY = y + (8.5f + 3.0f) * this.s;
        float boxH = 18.0f * this.s;
        float ep = this.dropAnim.getOrDefault(setting, 0.0f);
        boolean hovered = inside(mx, my, rowX, boxY, rowW, boxH) && this.inView(mx, my);
        float hp = this.anim(this.hoverAnim, setting, hovered || this.openSelect == setting ? 1.0f : 0.0f, SPEED_HOVER, dt);
        int border = lerpColor(lerpColor(rgba(165, 165, 168, Math.round(alpha * 0.16f)), rgba(165, 165, 168, Math.round(alpha * 0.42f)), hp),
                withAlpha(ACCENT, Math.round(alpha * 0.55f)), ep);
        float radius = 3.0f * this.s;
        this.outline(m, rowX, boxY, rowW, boxH, radius, 0.9f * this.s, border, 0);
        Rectangle.flushPending();
        float iconSize = 11.0f * this.s;
        float arrow = 10.0f * this.s;
        float ix = rowX + 7.0f * this.s;
        this.icon(m, "enum", ix, boxY + (boxH - iconSize) / 2.0f, iconSize, withAlpha(0xFF8E94A0, alpha));
        float ax = rowX + rowW - 7.0f * this.s - arrow;
        this.icon(m, "arrow", ax, boxY + (boxH - arrow) / 2.0f, arrow, lerpColor(withAlpha(0xFF8E94A0, alpha), withAlpha(ACCENT, alpha), ep));
        FontRenderer valueFont = font(8.5f, true);
        float tx = ix + iconSize + 5.0f * this.s;
        drawClipped(m, valueFont, current == null ? "" : current, tx, valueFont.getCenteredY(boxY, boxH), Math.max(6.0f * this.s, ax - tx - 6.0f * this.s),
                withAlpha(0xFFE6E8EE, alpha));
        this.hits.add(new Hit(rowX, boxY, rowW, boxH, true, button -> {
            if (button == 0) {
                this.openSelect = this.openSelect == setting ? null : setting;
            }
        }));

        if (ep > 0.02f) {
            float dropY = boxY + boxH + 2.0f * this.s;
            float rowH = 17.0f * this.s;
            float dPad = 3.0f * this.s;
            float dropH = options.size() * rowH + dPad * 2.0f;
            int dropA = Math.round(alpha * ep);
            this.outline(m, rowX, dropY, rowW, dropH * ep, radius, 0.9f * this.s, rgba(255, 255, 255, Math.min(48, Math.round(44 * (dropA / 255.0f)))), 0);
            Rectangle.flushPending();
            FontRenderer optionFont = font(8.5f, true);
            for (int i = 0; i < options.size(); ++i) {
                float ry = dropY + dPad + i * rowH;
                if (ry + rowH * 0.3f > dropY + dropH * ep) break;
                String option = options.get(i);
                boolean selected = option.equalsIgnoreCase(current);
                boolean rowHover = inside(mx, my, rowX, ry, rowW, rowH) && this.inView(mx, my);
                if (selected || rowHover) {
                    this.rect(m, rowX + 3.0f * this.s, ry + this.s, rowW - 6.0f * this.s, rowH - 2.0f * this.s, 2.0f * this.s,
                            selected ? withAlpha(ACCENT, Math.round(dropA * 0.28f)) : rgba(255, 255, 255, Math.round(dropA * 0.07f)));
                    Rectangle.flushPending();
                }
                drawClipped(m, optionFont, option, rowX + 9.0f * this.s, optionFont.getCenteredY(ry, rowH), rowW - 28.0f * this.s,
                        withAlpha(selected ? 0xFFFFFFFF : 0xFFC0C4CC, dropA));
                if (selected) {
                    float cs = 9.0f * this.s;
                    this.icon(m, "checkmark", rowX + rowW - 13.0f * this.s - cs / 2.0f, ry + rowH / 2.0f - cs / 2.0f, cs, rgba(255, 255, 255, dropA));
                }
                if (ep > 0.4f) {
                    this.hits.add(new Hit(rowX, ry, rowW, rowH, true, button -> {
                        if (button == 0) {
                            apply.accept(option);
                            this.openSelect = null;
                        }
                    }));
                }
            }
        }
    }

    /**
     * Раскладка чипов мультивыбора. Возвращает высоту строки; если передан chipOut,
     * складывает туда прямоугольники чипов (x, y, w) для отрисовки и кликов.
     */
    private float chipLayout(MultiSelectSetting multi, float originX, float originY, float width, List<float[]> chipOut) {
        FontRenderer chipFont = font(8.0f, true);
        float pad = 2.5f * this.s;
        float rowX = originX + pad;
        float rowW = Math.max(8.0f * this.s, width - pad * 2.0f);
        float chipH = 16.0f * this.s;
        float x = rowX;
        float y = originY + 14.0f * this.s;
        List<String> options = multi.getList();
        for (String option : options) {
            float cw = Math.min(rowW, Math.max(28.0f * this.s, 8.0f * this.s + 10.0f * this.s + 4.0f * this.s + chipFont.getStringWidth(option) + 8.0f * this.s * 0.55f));
            if (x > rowX && x + cw > rowX + rowW + 0.5f) {
                x = rowX;
                y += chipH + 3.0f * this.s;
            }
            if (chipOut != null) chipOut.add(new float[]{x, y, cw});
            x += cw + 3.0f * this.s;
        }
        float bottom = options.isEmpty() ? originY + 14.0f * this.s : y + chipH;
        return bottom - originY + 2.0f * this.s;
    }

    /** Мультивыбор: заголовок со счётчиком «n/N» и синие чипы с галочками. */
    private void renderMulti(MatrixStack m, MultiSelectSetting multi, float x, float y, float w, float mx, float my, float dt, int alpha) {
        float pad = 2.5f * this.s;
        float lx = x + pad;
        float rowW = Math.max(8.0f * this.s, w - pad * 2.0f);
        List<String> options = multi.getList();
        int count = 0;
        for (String option : options) {
            if (multi.isSelected(option)) count++;
        }
        String badge = count + "/" + options.size();
        FontRenderer badgeFont = font(7.5f, true);
        float badgeW = badgeFont.getStringWidth(badge) + 8.0f * this.s;
        float badgeH = 12.0f * this.s;
        float badgeX = lx + rowW - badgeW;
        float badgeY = y + (9.5f * this.s - badgeH) / 2.0f + 0.5f * this.s;
        FontRenderer name = font(9.5f, true);
        drawClipped(m, name, multi.getName(), lx, y, Math.max(8.0f * this.s, badgeX - lx - 6.0f * this.s), withAlpha(C_LABEL, alpha));
        this.outline(m, badgeX, badgeY, badgeW, badgeH, 4.0f * this.s, 0.8f, rgba(165, 165, 168, Math.round(alpha * 0.14f)),
                count > 0 ? withAlpha(ACCENT, Math.round(alpha * 0.45f)) : rgba(165, 165, 168, Math.round(alpha * 0.08f)));
        Rectangle.flushPending();
        badgeFont.drawString(m, badge, badgeX + (badgeW - badgeFont.getStringWidth(badge)) / 2.0f, badgeFont.getCenteredY(badgeY, badgeH),
                count > 0 ? rgba(255, 255, 255, alpha) : withAlpha(0xFF969AA6, alpha));

        List<float[]> chips = new ArrayList<>();
        this.chipLayout(multi, x, y, w, chips);
        FontRenderer chipFont = font(8.0f, true);
        float chipH = 16.0f * this.s;
        float box = 10.0f * this.s;
        for (int i = 0; i < chips.size() && i < options.size(); ++i) {
            String option = options.get(i);
            float cx = chips.get(i)[0];
            float cy = chips.get(i)[1];
            float cw = chips.get(i)[2];
            String key = multi.getName() + " " + option;
            boolean hovered = inside(mx, my, cx, cy, cw, chipH) && this.inView(mx, my);
            float sp = this.anim(this.toggleAnim, key, multi.isSelected(option) ? 1.0f : 0.0f, SPEED_TOGGLE, dt);
            float hp = this.anim(this.hoverAnim, key, hovered ? 1.0f : 0.0f, SPEED_HOVER, dt);
            this.outline(m, cx, cy, cw, chipH, 5.0f * this.s, 1.0f,
                    lerpColor(rgba(255, 255, 255, Math.round(alpha * (0.14f + 0.06f * hp))), withAlpha(ACCENT, Math.round(alpha * 0.7f)), sp),
                    lerpColor(rgba(255, 255, 255, Math.round(alpha * (0.06f + 0.04f * hp))), withAlpha(ACCENT, Math.round(alpha * (0.55f + 0.15f * hp))), sp));
            float bx = cx + 4.0f * this.s;
            float by = cy + (chipH - box) / 2.0f;
            this.outline(m, bx, by, box, box, 3.0f * this.s, 1.0f, rgba(255, 255, 255, Math.round(alpha * (0.2f + 0.15f * (1.0f - sp)))),
                    lerpColor(rgba(165, 165, 168, Math.round(alpha * 0.10f)), withAlpha(ACCENT, Math.round(alpha * 0.95f)), sp));
            Rectangle.flushPending();
            if (sp > 0.08f) {
                float cs = box * 0.7f;
                this.icon(m, "checkmark", bx + (box - cs) / 2.0f, by + (box - cs) / 2.0f, cs, rgba(14, 14, 15, Math.round(alpha * sp)));
            }
            float textX = bx + box + 4.0f * this.s;
            drawClipped(m, chipFont, option, textX, chipFont.getCenteredY(cy, chipH), Math.max(4.0f * this.s, cx + cw - textX - 4.0f * this.s),
                    lerpColor(withAlpha(0xFFC0C4CC, alpha), rgba(255, 255, 255, alpha), sp));
            this.hits.add(new Hit(cx, cy, cw, chipH, true, button -> {
                if (button != 0) return;
                List<String> selected = new ArrayList<>(multi.getSelected());
                if (!selected.remove(option)) {
                    selected.add(option);
                }
                multi.setSelected(selected);
            }));
        }
    }

    /** Цвет: плашка с превью и пипеткой слева, дальше круглые пресеты; по превью — пикер. */
    private void renderColor(MatrixStack m, ColorSetting color, float x, float y, float w, float mx, float my, float dt, int alpha) {
        float pad = 2.0f * this.s;
        float rowX = x + pad;
        float rowW = Math.max(8.0f * this.s, w - pad * 2.0f);
        FontRenderer name = font(8.5f, false);
        drawClipped(m, name, color.getName(), rowX, y, rowW, withAlpha(C_LABEL, alpha));
        float barY = y + (8.5f + 3.0f) * this.s;
        float barH = 13.0f * this.s;
        boolean hovered = inside(mx, my, rowX, barY, rowW, barH) && this.inView(mx, my);
        float hp = this.anim(this.hoverAnim, color, hovered ? 1.0f : 0.0f, SPEED_HOVER, dt);
        float radius = 5.0f * this.s;
        this.outline(m, rowX, barY, rowW, barH, radius, 0.8f, rgba(165, 165, 168, Math.round(alpha * (0.10f + 0.05f * hp))),
                lerpColor(rgba(14, 14, 15, Math.round(alpha * 0.92f)), rgba(22, 22, 23, Math.round(alpha * 0.95f)), hp));
        Rectangle.flushPending();

        int rgb = color.getColor() & 0xFFFFFF;
        float previewW = 28.0f * this.s;
        // Превью: левый край плашки залит текущим цветом, правый край срезан ножницами.
        this.clipped(m, rowX, barY, previewW, barH, () -> this.rect(m, rowX, barY, previewW + radius * 2.0f, barH, radius, withAlpha(rgb, alpha)));
        float brush = 10.0f * this.s;
        this.icon(m, "brush", rowX + (previewW - brush) / 2.0f, barY + (barH - brush) / 2.0f, brush, rgba(255, 255, 255, Math.round(alpha * 0.9f)));
        this.rect(m, rowX + previewW, barY + 2.5f * this.s, 1.0f, barH - 5.0f * this.s, 0.0f, rgba(165, 165, 168, Math.round(alpha * 0.10f)));
        Rectangle.flushPending();
        this.hits.add(new Hit(rowX, barY, previewW, barH, true, button -> {
            if (button == 0) this.openPicker = this.openPicker == color ? null : color;
        }));

        List<Integer> swatches = new ArrayList<>();
        swatches.add(ACCENT);
        for (int preset : SWATCHES) swatches.add(preset);
        int[] own = color.getPresets();
        if (own != null) {
            for (int preset : own) {
                if (swatches.stream().noneMatch(c -> (c & 0xFFFFFF) == (preset & 0xFFFFFF))) swatches.add(preset | 0xFF000000);
            }
        }
        float sx = rowX + previewW + 7.0f * this.s;
        float mid = barY + barH / 2.0f;
        float step = 12.0f * this.s;
        float r = 4.0f * this.s;
        float maxRight = rowX + rowW - 6.0f * this.s;
        for (int i = 0; i < swatches.size(); ++i) {
            float cx = sx + i * step;
            if (cx + r > maxRight) break;
            int swatch = swatches.get(i) | 0xFF000000;
            boolean selected = (swatch & 0xFFFFFF) == rgb;
            float rr = r + (selected ? this.s : 0.0f);
            this.rect(m, cx - rr, mid - rr, rr * 2.0f, rr * 2.0f, rr, withAlpha(swatch, alpha));
            Rectangle.flushPending();
            if (selected) {
                float cs = 7.0f * this.s;
                this.icon(m, "checkmark", cx - cs / 2.0f, mid - cs / 2.0f, cs, rgba(20, 22, 28, alpha));
            }
            this.hits.add(new Hit(cx - r - 2.0f, mid - r - 2.0f, r * 2.0f + 4.0f, r * 2.0f + 4.0f, true, button -> {
                if (button == 0) color.setColor(swatch & 0xFFFFFF | Math.round(color.getAlpha() * 255.0f) << 24);
            }));
        }

        float ep = this.dropAnim.getOrDefault(color, 0.0f);
        if (ep > 0.02f) {
            float size = PICKER_H * this.s * ep;
            float px = rowX;
            float py = barY + barH + 4.0f * this.s;
            int pa = Math.round(alpha * ep);
            int hue = Color.HSBtoRGB(color.getHue(), 1.0f, 1.0f);
            this.rectangle.render(ShapeProperties.create(m, px, py, size, size).round(4.0f * this.s)
                    .color(rgba(0, 0, 0, pa), withAlpha(hue, pa), rgba(0, 0, 0, pa), rgba(255, 255, 255, pa)).build());
            float hueX = px + size + 8.0f * this.s;
            float hueW = 8.0f * this.s;
            for (int i = 0; i < 6; ++i) {
                float seg = size / 6.0f;
                int from = withAlpha(Color.HSBtoRGB(i / 6.0f, 1.0f, 1.0f), pa);
                int to = withAlpha(Color.HSBtoRGB((i + 1) / 6.0f, 1.0f, 1.0f), pa);
                this.rectangle.render(ShapeProperties.create(m, hueX, py + i * seg, hueW, seg).color(from, from, to, to).build());
            }
            this.rect(m, px + color.getSaturation() * size - 2.5f, py + (1.0f - color.getBrightness()) * size - 2.5f, 5.0f, 5.0f, 2.5f, rgba(255, 255, 255, pa));
            this.rect(m, hueX - 1.5f, py + color.getHue() * size - 1.0f, hueW + 3.0f, 2.0f, 1.0f, rgba(255, 255, 255, pa));
            Rectangle.flushPending();
            if (ep > 0.9f) {
                this.hits.add(new Hit(px, py, size, size, true, button -> {
                    this.draggingSquare = color;
                    this.pickX = px;
                    this.pickY = py;
                    this.pickSize = size;
                    this.dragPicker(this.lastMouseX, this.lastMouseY);
                }));
                this.hits.add(new Hit(hueX, py, hueW, size, true, button -> {
                    this.draggingHue = color;
                    this.pickY = py;
                    this.pickSize = size;
                    color.setHue(clamp((this.lastMouseY - py) / size, 0.0f, 1.0f));
                }));
            }
        }
    }

    /** Бинд-настройка: подпись и «клавиша» справа; ЛКМ — ждать клавишу, ПКМ — сбросить. */
    private void renderBind(MatrixStack m, BindSetting bind, float x, float y, float w, int alpha) {
        float h = 16.0f * this.s;
        FontRenderer name = font(8.5f, false);
        boolean listening = this.bindingSetting == bind;
        String shown = listening ? "..." : bind.getKey() <= 0 ? "—" : BindCodes.getDisplayName(bind.getKey());
        FontRenderer keyFont = font(8.0f, true);
        float boxH = 14.0f * this.s;
        float boxW = Math.max(boxH, keyFont.getStringWidth(shown) + 10.0f * this.s);
        float boxX = x + w - ROW_PAD * this.s - boxW;
        float boxY = y + (h - boxH) / 2.0f;
        drawClipped(m, name, bind.getName(), this.labelX(x), name.getCenteredY(y, h), boxX - this.labelX(x) - 6.0f * this.s, withAlpha(C_LABEL, alpha));
        this.icon(m, "keyboard", x + ROW_PAD * this.s, y + (h - ROW_ICON * this.s) / 2.0f, ROW_ICON * this.s, withAlpha(C_ICON, alpha));
        this.outline(m, boxX, boxY, boxW, boxH, 3.5f * this.s, 1.0f, rgba(255, 255, 255, Math.round(alpha * (listening ? 0.55f : 0.22f))),
                listening ? withAlpha(ACCENT, Math.round(alpha * 0.88f)) : rgba(165, 165, 168, Math.round(alpha * 0.08f)));
        Rectangle.flushPending();
        keyFont.drawString(m, shown, boxX + (boxW - keyFont.getStringWidth(shown)) / 2.0f, keyFont.getCenteredY(boxY, boxH),
                listening ? rgba(255, 255, 255, alpha) : withAlpha(0xFFD0D2DA, alpha));
        this.hits.add(new Hit(x, y, w, h, true, button -> {
            if (button == 1) {
                bind.setKey(-1);
                this.bindingSetting = null;
            } else {
                this.bindingSetting = bind;
                this.bindingModule = null;
            }
        }));
    }

    private void renderText(MatrixStack m, TextSetting text, float x, float y, float w, int alpha) {
        float pad = 2.0f * this.s;
        float rowX = x + pad;
        float rowW = Math.max(8.0f * this.s, w - pad * 2.0f);
        FontRenderer name = font(8.5f, false);
        drawClipped(m, name, text.getName(), rowX, y, rowW, withAlpha(C_LABEL, alpha));
        float boxY = y + (8.5f + 3.0f) * this.s;
        float boxH = 15.0f * this.s;
        boolean editing = this.editingText == text;
        this.outline(m, rowX, boxY, rowW, boxH, 3.0f * this.s, 0.9f * this.s,
                editing ? withAlpha(ACCENT, Math.round(alpha * 0.55f)) : rgba(165, 165, 168, Math.round(alpha * 0.16f)), rgba(165, 165, 168, Math.round(alpha * 0.04f)));
        Rectangle.flushPending();
        String value = text.getText() == null ? "" : text.getText();
        FontRenderer input = font(8.0f, false);
        String shown = value.isEmpty() && !editing ? "..." : value;
        float maxW = rowW - 12.0f * this.s;
        while (!shown.isEmpty() && input.getStringWidth(shown) > maxW) {
            shown = shown.substring(1);
        }
        input.drawString(m, shown, rowX + 6.0f * this.s, input.getCenteredY(boxY, boxH), value.isEmpty() ? withAlpha(0xFF7A7E88, alpha) : withAlpha(0xFFE6E8EE, alpha));
        if (editing && System.currentTimeMillis() / 500L % 2L == 0L) {
            this.rect(m, rowX + 6.0f * this.s + input.getStringWidth(value.isEmpty() ? "" : shown) + 1.0f, boxY + 3.5f * this.s, 0.8f, boxH - 7.0f * this.s, 0.0f, rgba(232, 234, 241, alpha));
            Rectangle.flushPending();
        }
        this.hits.add(new Hit(rowX, boxY, rowW, boxH, true, button -> {
            this.editingText = text;
            this.searchFocused = false;
        }));
    }

    private void renderButton(MatrixStack m, ButtonSetting button, float x, float y, float w, float mx, float my, float dt, int alpha) {
        float h = 16.0f * this.s;
        float pad = 2.0f * this.s;
        boolean hovered = inside(mx, my, x + pad, y, w - pad * 2.0f, h) && this.inView(mx, my);
        float hp = this.anim(this.hoverAnim, button, hovered ? 1.0f : 0.0f, SPEED_HOVER, dt);
        this.outline(m, x + pad, y, w - pad * 2.0f, h, 3.5f * this.s, 0.9f, rgba(255, 255, 255, Math.round(alpha * (0.16f + 0.2f * hp))),
                lerpColor(rgba(165, 165, 168, Math.round(alpha * 0.06f)), withAlpha(ACCENT, Math.round(alpha * 0.45f)), hp));
        Rectangle.flushPending();
        String label = button.getButtonName() == null || button.getButtonName().isEmpty() ? button.getName() : button.getButtonName();
        FontRenderer font = font(8.5f, true);
        font.drawString(m, label, x + (w - font.getStringWidth(label)) / 2.0f, font.getCenteredY(y, h), withAlpha(0xFFE6E8EE, alpha));
        this.hits.add(new Hit(x + pad, y, w - pad * 2.0f, h, true, b -> {
            if (b == 0 && button.getRunnable() != null) button.getRunnable().run();
        }));
    }

    private void drawToggle(MatrixStack m, float x, float y, float w, float h, float progress, float a) {
        if (progress > 0.02f) {
            this.glow(m, x, y + 1.0f, w, h, h / 2.0f, 9.0f, withAlpha(ACCENT, Math.round(34 * progress * a)));
        }
        int fill = lerpColor(rgba(8, 8, 9, Math.round(255 * a)), withAlpha(ACCENT, Math.round(255 * a)), progress);
        int outline = lerpColor(rgba(255, 255, 255, Math.round(14 * a)), withAlpha(ACCENT, Math.round(255 * a)), progress);
        this.outline(m, x, y, w, h, h / 2.0f, 0.8f, outline, fill);
        float pad = 2.0f * this.s;
        float knob = h - pad * 2.0f;
        float travel = Math.max(0.0f, w - pad * 2.0f - knob);
        this.rect(m, x + pad + travel * progress, y + pad, knob, knob, knob / 2.0f,
                lerpColor(rgba(123, 134, 146, Math.round(255 * a)), rgba(14, 14, 15, Math.round(255 * a)), progress));
        Rectangle.flushPending();
    }

    // ── ввод ───────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        // Идёт назначение бинда: СКМ и боковые кнопки мыши становятся биндом.
        // ЛКМ и ПКМ оставляем интерфейсу — ими отменяют назначение и снимают бинд.
        if (button >= 2 && BindCodes.isMouse(button) && this.assignPendingBind(button)) {
            return true;
        }
        float mx = (float)mouseX / this.factor;
        float my = (float)mouseY / this.factor;
        this.lastMouseX = mx;
        this.lastMouseY = my;
        boolean wasSearch = this.searchFocused;
        boolean wasEditing = this.editingText != null;
        this.searchFocused = false;
        this.editingText = null;
        // Последние зарегистрированные зоны лежат поверх — идём с конца.
        for (int i = this.hits.size() - 1; i >= 0; --i) {
            Hit hit = this.hits.get(i);
            if (!inside(mx, my, hit.x, hit.y, hit.w, hit.h)) continue;
            if (hit.clip && !this.inView(mx, my)) continue;
            hit.action.accept(button);
            return true;
        }
        if (wasSearch || wasEditing) {
            return true;
        }
        this.openSelect = null;
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click wiosClick) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        this.draggingSlider = null;
        this.draggingSquare = null;
        this.draggingHue = null;
        this.draggingScrollbar = false;
        return super.mouseReleased(wiosClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click wiosClick, double deltaX, double deltaY) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        float mx = (float)mouseX / this.factor;
        float my = (float)mouseY / this.factor;
        this.lastMouseX = mx;
        this.lastMouseY = my;
        if (this.draggingSlider != null) {
            this.dragSlider(mx);
            return true;
        }
        if (this.draggingSquare != null) {
            this.dragPicker(mx, my);
            return true;
        }
        if (this.draggingHue != null) {
            this.draggingHue.setHue(clamp((my - this.pickY) / Math.max(1.0f, this.pickSize), 0.0f, 1.0f));
            return true;
        }
        if (this.draggingScrollbar) {
            this.dragScrollbar(my);
            return true;
        }
        return super.mouseDragged(wiosClick, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0.0 && this.assignPendingBind(vertical > 0.0 ? BindCodes.SCROLL_UP : BindCodes.SCROLL_DOWN)) {
            return true;
        }
        float current = this.scrollTarget.getOrDefault(this.scrollKey(), 0.0f);
        this.scrollTarget.put(this.scrollKey(), Math.max(0.0f, current - (float)vertical * 42.0f));
        return true;
    }

    /** Записывает код в модуль или BindSetting, который сейчас ждёт бинд. */
    private boolean assignPendingBind(int code) {
        if (this.bindingModule != null) {
            this.bindingModule.setKey(code);
            this.bindingModule = null;
            return true;
        }
        if (this.bindingSetting instanceof BindSetting bind) {
            bind.setKey(code);
            this.bindingSetting = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        if (this.bindingModule != null) {
            this.bindingModule.setKey(keyCode == 256 || keyCode == 261 ? -1 : keyCode);
            this.bindingModule = null;
            return true;
        }
        if (this.bindingSetting instanceof BindSetting bind) {
            bind.setKey(keyCode == 256 || keyCode == 261 ? -1 : keyCode);
            this.bindingSetting = null;
            return true;
        }
        if (keyCode == 70 && (modifiers & 2) != 0) {
            this.searchFocused = true;
            this.editingText = null;
            return true;
        }
        if (this.searchFocused) {
            if (keyCode == 259 && !this.searchText.isEmpty()) {
                this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
                return true;
            }
            if (keyCode == 256) {
                this.searchText = "";
                this.searchFocused = false;
                return true;
            }
            if (keyCode == 257) {
                this.searchFocused = false;
                return true;
            }
        }
        if (this.editingText instanceof TextSetting text) {
            String value = text.getText() == null ? "" : text.getText();
            if (keyCode == 259 && !value.isEmpty()) {
                text.setText(value.substring(0, value.length() - 1));
                return true;
            }
            if (keyCode == 256 || keyCode == 257) {
                this.editingText = null;
                return true;
            }
        }
        if (keyCode == 256) {
            this.closing = true;
            return true;
        }
        return super.keyPressed(wiosKey);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput wiosChar) {
        char symbol = (char) wiosChar.codepoint();
        int modifiers = wiosChar.modifiers();
        if (this.searchFocused) {
            this.searchText = this.searchText + symbol;
            this.scrollTarget.clear();
            this.scroll.clear();
            return true;
        }
        if (this.editingText instanceof TextSetting text) {
            String value = text.getText() == null ? "" : text.getText();
            if (value.length() < text.getMax()) {
                text.setText(value + symbol);
            }
            return true;
        }
        return super.charTyped(wiosChar);
    }

    // ── вспомогательное ────────────────────────────────────────────────────────

    private void selectCategory(ModuleCategory category) {
        if (category == this.active && this.searchText.isEmpty()) {
            return;
        }
        this.active = category;
        this.searchText = "";
        this.searchFocused = false;
        this.openSelect = null;
        this.openPicker = null;
        this.contentAnim = 0.0f;
    }

    private void dragSlider(float mouseX) {
        SliderSettings slider = this.draggingSlider;
        if (slider == null) return;
        float progress = clamp((mouseX - this.dragTrackX) / Math.max(1.0f, this.dragTrackW), 0.0f, 1.0f);
        float raw = slider.getMin() + progress * (slider.getMax() - slider.getMin());
        float step = slider.getStep();
        slider.setValue(step > 0.0f ? Math.round(raw / step) * step : raw);
    }

    private void dragPicker(float mouseX, float mouseY) {
        ColorSetting color = this.draggingSquare;
        if (color == null) return;
        color.setSaturation(clamp((mouseX - this.pickX) / Math.max(1.0f, this.pickSize), 0.0f, 1.0f));
        color.setBrightness(clamp(1.0f - (mouseY - this.pickY) / Math.max(1.0f, this.pickSize), 0.0f, 1.0f));
    }

    private void dragScrollbar(float mouseY) {
        float travel = Math.max(1.0f, this.sbTrackH - this.sbThumbH);
        float rel = clamp((mouseY - this.sbTrackY - this.sbGrab) / travel, 0.0f, 1.0f);
        float value = rel * this.sbMaxScroll;
        this.scrollTarget.put(this.scrollKey(), value);
        this.scroll.put(this.scrollKey(), value);
    }

    private boolean inView(float mx, float my) {
        return inside(mx, my, this.viewX, this.viewY, this.viewW, this.viewH);
    }

    /**
     * У части модулей в visibleName лежит описание («Летающие частицы в мире»),
     * а в шапке карточки нужно короткое имя: длинное заменяем внутренним, разбитым на слова.
     */
    private static String cardTitle(Module module) {
        String visible = module.getVisibleName();
        if (visible != null && visible.length() <= 20) {
            return visible;
        }
        return module.getName().replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private static List<Setting> visibleSettings(Module module) {
        List<Setting> result = new ArrayList<>();
        for (Setting setting : module.settings()) {
            if (setting == null || !setting.isVisible()) continue;
            result.add(setting);
        }
        return result;
    }

    /** Модули активной категории; при непустом поиске — по всем категориям сразу. */
    private List<Module> modules() {
        List<Module> result = new ArrayList<>();
        Wiosvisual client = Wiosvisual.getInstance();
        if (client == null || client.getModuleRepository() == null) {
            return result;
        }
        String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        for (Module module : client.getModuleRepository().modules()) {
            if (module.isHidden()) continue;
            if (this.showFavorites && !FAVORITES.contains(module.getName())) continue;
            if (query.isEmpty()) {
                if (!this.showFavorites && !this.showAll && module.getCategory() != this.active) continue;
            } else if (!module.getName().toLowerCase(Locale.ROOT).contains(query)
                    && !module.getVisibleName().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            result.add(module);
        }
        return result;
    }

    /** Категория попадает в сайдбар, только если в ней есть видимые модули. */
    private void refreshCategories() {
        Wiosvisual client = Wiosvisual.getInstance();
        if (client == null || client.getModuleRepository() == null) {
            return;
        }
        List<ModuleCategory> list = new ArrayList<>();
        for (ModuleCategory category : ModuleCategory.values()) {
            // «Конфигурации» — отдельная страница со своим UI (список сохранений), не список модулей,
            // поэтому показывается всегда, даже без ни одного видимого модуля в этой категории.
            if (category == ModuleCategory.CONFIGS) {
                list.add(category);
                continue;
            }
            for (Module module : client.getModuleRepository().modules()) {
                if (module.getCategory() != category || module.isHidden()) continue;
                list.add(category);
                break;
            }
        }
        if (!list.isEmpty()) {
            this.categories = list.toArray(new ModuleCategory[0]);
            if (!list.contains(this.active)) {
                this.active = this.categories[0];
            }
        }
    }

    private static String tabIcon(ModuleCategory category) {
        return switch (category) {
            case RENDER -> "tabs/render";
            case PLAYER -> "tabs/player";
            case MISC -> "tabs/misc";
            case CUSTOMIZATION, COSMETICS -> "tabs/themes";
            case CONFIGS -> "tabs/configs";
        };
    }

    private static String formatValue(float value, float step, boolean integer) {
        if (integer || step >= 1.0f) {
            return String.valueOf(Math.round(value));
        }
        String text = String.format(Locale.US, "%.2f", value);
        while (text.contains(".") && text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private static int pickColumn(float[] cols) {
        int best = 0;
        for (int i = 1; i < cols.length; ++i) {
            if (cols[i] < cols[best]) best = i;
        }
        return best;
    }

    private static float maxOf(float[] values) {
        float max = values[0];
        for (float value : values) max = Math.max(max, value);
        return max;
    }

    private float anim(Map<Object, Float> map, Object key, float target, float speed, float dt) {
        float next = approach(map.getOrDefault(key, target), target, speed, dt);
        map.put(key, next);
        return next;
    }

    /** Текст CsGui размером {@code px} дизайн-пикселей: у шрифтов Wios высота — половина размера. */
    private FontRenderer font(float px, boolean semi) {
        return Fonts.getSize(Math.max(2, Math.round(px * this.s * 2.0f)), semi ? Fonts.Type.SEMI : Fonts.Type.DEFAULT);
    }

    private static void drawClipped(MatrixStack m, FontRenderer font, String text, float x, float y, float maxW, int color) {
        if (text == null || text.isEmpty() || maxW <= 2.0f) return;
        if (font.getStringWidth(text) <= maxW) {
            font.drawString(m, text, x, y, color);
            return;
        }
        String cut = text;
        while (cut.length() > 1 && font.getStringWidth(cut + "…") > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        font.drawString(m, cut + "…", x, y, color);
    }

    /** Белая PNG-иконка из assets/wiosvisual/icons/csgui, подкрашенная цветом. */
    private void icon(MatrixStack m, String name, float x, float y, float size, int color) {
        if (size <= 0.0f || (color >>> 24) == 0) return;
        Rectangle.flushPending();
        Identifier id = Identifier.of("wiosvisual", "icons/csgui/" + name + ".png");
        // Одна дизайн-единица — два физических пикселя. Иконки 16×16 от 12 px и больше
        // рисуем ровно 16 или 32 px с пиксельной фильтрацией: каждый пиксель иконки ложится
        // в 1×1 или 2×2 пикселя экрана. Мельче — сжимаем со сглаживанием, иначе штрихи рвутся.
        float physical = size * 2.0f;
        boolean crisp = physical >= 12.0f;
        if (crisp) {
            float snapped = physical < 24.0f ? 16.0f : 32.0f;
            float center = physical / 2.0f;
            x = (Math.round((x * 2.0f + center - snapped / 2.0f))) / 2.0f;
            y = (Math.round((y * 2.0f + center - snapped / 2.0f))) / 2.0f;
            size = snapped / 2.0f;
        } else {
            x = Math.round(x * 2.0f) / 2.0f;
            y = Math.round(y * 2.0f) / 2.0f;
        }
        Render2D.drawTexturedQuad(m, id, x, x + size, y, y + size, 0.0f, 1.0f, 0.0f, 1.0f, color);
    }

    private static final java.util.Set<Identifier> SMOOTHED = new java.util.HashSet<>();

    private void clipped(MatrixStack m, float x, float y, float w, float h, Runnable draw) {
        ScissorAssist scissor = Wiosvisual.getInstance() == null ? null : Wiosvisual.getInstance().getScissorManager();
        Rectangle.flushPending();
        if (scissor != null) scissor.push(m.peek().getPositionMatrix(), x, y, w, h);
        draw.run();
        Rectangle.flushPending();
        if (scissor != null) scissor.pop();
    }

    private void rect(MatrixStack m, float x, float y, float w, float h, float round, int color) {
        if (w <= 0.0f || h <= 0.0f || (color >>> 24) == 0) return;
        this.rectangle.render(ShapeProperties.create(m, x, y, w, h).round(round).color(color).build());
    }

    private void outline(MatrixStack m, float x, float y, float w, float h, float round, float thickness, int outline, int fill) {
        if (w <= 0.0f || h <= 0.0f) return;
        this.rectangle.render(ShapeProperties.create(m, x, y, w, h).round(round).thickness(thickness).outlineColor(outline).color(fill).build());
    }

    private void glow(MatrixStack m, float x, float y, float w, float h, float round, float softness, int color) {
        if (w <= 0.0f || h <= 0.0f) return;
        this.rectangle.render(ShapeProperties.create(m, x, y, w, h).round(round).softness(softness).color(color).build());
    }

    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float approach(float current, float target, float speed, float dt) {
        return current + (target - current) * (1.0f - (float)Math.exp(-speed * dt));
    }

    private static float easeOut(float t) {
        float k = 1.0f - clamp(t, 0.0f, 1.0f);
        return 1.0f - k * k * k;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    private static int rgba(int r, int g, int b, int a) {
        return clampByte(a) << 24 | r << 16 | g << 8 | b;
    }

    private static int withAlpha(int color, int alpha) {
        return clampByte(alpha) << 24 | color & 0xFFFFFF;
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int lerpColor(int from, int to, float progress) {
        float k = clamp(progress, 0.0f, 1.0f);
        int a = (int)((from >>> 24) + ((to >>> 24) - (from >>> 24)) * k);
        int r = (int)((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * k);
        int g = (int)((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * k);
        int b = (int)((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** Кликабельная зона, собранная на последнем кадре; clip — только внутри области модулей. */
    private record Hit(float x, float y, float w, float h, boolean clip, IntConsumer action) {
    }
}
