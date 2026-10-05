package ru.mentions.screen;

import ru.mentions.Client;
import ru.mentions.gui.*;

import ru.mentions.utils.animation.Animation;
import ru.mentions.utils.animation.Easings;
import ru.mentions.utils.animation.satoshi.Direction;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;
import ru.mentions.utils.annotation.IMinecraft;
import ru.mentions.utils.colors.ColorFormatting;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.math.Keyboard;
import ru.mentions.utils.math.MathUtil;
import ru.mentions.utils.render.*;
import ru.mentions.utils.render.font.Font;
import ru.mentions.utils.render.font.Fonts;

import java.util.HashMap;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class Menu extends Screen implements IMinecraft {

    public static float S = 1.0F;

    private static Menu instance;

    /** Открывает ClickGui; экземпляр переиспользуется, чтобы сохранялись поиск и выбранный модуль. */
    public static void open() {
        if (instance == null) {
            instance = new Menu();
        }
        net.minecraft.client.MinecraftClient.getInstance().setScreen(instance);
    }

    public Menu() {
        super(Text.literal("MenuNight"));
    }

    boolean exit = false;

    public Animation glomalAnim = new Animation();

    private final ru.mentions.utils.animation.Animation tgHover = new ru.mentions.utils.animation.Animation();
    private float tgBtnX, tgBtnY, tgBtnR;


    private float scaleFix = 1F;

    double lastMouseX;
    double lastMouseY;

    public static ru.mentions.utils.animation.satoshi.Animation animation14 = new EaseInOutQuad(300, 1);
    public static ru.mentions.utils.animation.satoshi.Animation animCategoryReset = new EaseInOutQuad(300, 1);
    public static ru.mentions.utils.animation.satoshi.Animation animUserInfo = new EaseInOutQuad(300, 1);

    public static ru.mentions.utils.animation.satoshi.Animation animation1 = new EaseInOutQuad(300, 1);
    public static ru.mentions.utils.animation.satoshi.Animation animation2 = new EaseInOutQuad(300, 1);
    public static ru.mentions.utils.animation.satoshi.Animation animation3 = new EaseInOutQuad(300, 1);
    public static ru.mentions.utils.animation.satoshi.Animation animation4 = new EaseInOutQuad(300, 1);

    public Feature select = null;
    public Tab tab = Tab.VISUALS;

    /** Вкладки боковой панели. */
    public enum Tab {
        VISUALS("Visuals", "Визуал", "Q"),
        UTILITY("Utility", "Утилиты", "L"),
        HUD("HUD", "Интерфейс", "E"),
        CONFIGS("Configs", "Прочее", "N");

        final String title;
        final String section;
        final String icon;
        final ru.mentions.utils.animation.satoshi.Animation anim = new EaseInOutQuad(250, 1);
        final ru.mentions.utils.animation.satoshi.Animation hover = new EaseInOutQuad(200, 1);

        Tab(String title, String section, String icon) {
            this.title = title;
            this.section = section;
            this.icon = icon;
        }

        boolean accepts(Feature f) {
            GuiCategory category = f.getCategory();
            boolean hud = f.module() instanceof ru.mentions.module.impl.display.InterFace;
            return switch (this) {
                case VISUALS -> !hud && (category == GuiCategory.RENDER || category == GuiCategory.COSMETICS || category == GuiCategory.CUSTOMIZATION);
                case UTILITY -> category == GuiCategory.MISC || category == GuiCategory.PLAYER;
                case HUD -> hud;
                case CONFIGS -> false;
            };
        }
    }

    /** Прямоугольник, запомненный при отрисовке кадра: по нему обрабатываются клики. */
    private static final class Hit {
        final float x, y, w, h;
        final Object payload;

        Hit(float x, float y, float w, float h, Object payload) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.payload = payload;
        }

        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private final java.util.List<Hit> moduleHits = new java.util.ArrayList<>();
    private final java.util.List<Hit> tabHits = new java.util.ArrayList<>();
    private final java.util.List<Hit> configHits = new java.util.ArrayList<>();
    private float searchX, searchY, searchW, searchH;
    private float listX, listY, listW, listH;

    private float winX(int screenWidth) {
        // Окно по центру; панель настроек выезжает справа только у выбранного модуля.
        return screenWidth / 2F - 690 * S / 2F;
    }

    private static float sizeValue = 0.8F;
    private static final SliderSetting SIZE = new SliderSetting("Size", 0.6F, 1.5F, 0.05F, () -> sizeValue, v -> sizeValue = v, () -> true);

    private int catDir = 0;

    private float scrollTarget = 0;
    private float scrollAnim = 0;
    private float scrollVel = 0;
    private float maxScroll = 0;

    private float settingScrollTarget = 0;
    private float settingScrollAnim = 0;
    private float settingScrollVel = 0;
    private float settingMaxScroll = 0;

    private SliderSetting draggingSlider = null;
    private BindSetting activeBind = null;

    private Feature bindingModule = null;

    private StringSetting activeString = null;
    private String stringBuffer = "";

    private ColorSetting draggingColor = null;
    private int draggingColorBar = 0;

    public static boolean searchActive = false;
    private static String searchQuery = "";
    private long searchTypeTime = System.currentTimeMillis();

    private final ru.mentions.utils.animation.satoshi.Animation animSearchFocus = new EaseInOutQuad(300, 1, Direction.BACKWARDS);
    private final ru.mentions.utils.animation.satoshi.Animation animSearchText = new EaseInOutQuad(300, 1, Direction.BACKWARDS);
    private final ru.mentions.utils.animation.satoshi.Animation animSearchEmpty = new EaseInOutQuad(300, 1, Direction.BACKWARDS);

    private static final int SEARCH_LIMIT = 24;

    private final HashMap<String, ru.mentions.utils.animation.satoshi.Animation> chipAnims = new HashMap<>();

    private ru.mentions.utils.animation.satoshi.Animation chipAnim(String key) {
        return chipAnims.computeIfAbsent(key, k -> new EaseInOutQuad(300, 1));
    }

    private final GrayscalePipeline grayscalePipeline = new GrayscalePipeline();
    private final ClickGuiDotsPipeline dotsPipeline = new ClickGuiDotsPipeline();
    private final ScanLinesPipeline scanLinesPipeline = new ScanLinesPipeline();
    private final MenuRaysPipeline raysPipeline = new MenuRaysPipeline();
    private final HalftoneDotsPipeline halftonePipeline = new HalftoneDotsPipeline();

    private boolean effect(String name) {
        return false;
    }

    private static final class GuiParticle {
        final float x, y;
        final float maxRadius;
        final float lifeMs;
        final float drift;
        final long born = System.currentTimeMillis();

        GuiParticle(float x, float y) {
            this.x = x;
            this.y = y;
            this.maxRadius = 10F + (float) Math.random() * 7F;
            this.lifeMs = 1400F + (float) Math.random() * 700F;
            this.drift = 4F + (float) Math.random() * 6F;
        }

        float progress() {
            return (System.currentTimeMillis() - born) / lifeMs;
        }
    }

    private final java.util.List<GuiParticle> particles = new java.util.ArrayList<>();
    private long lastParticle;
    private long particleDelay = 90;

    private void spawnParticle(int screenWidth, int screenHeight) {
        if (System.currentTimeMillis() - lastParticle < particleDelay) return;
        lastParticle = System.currentTimeMillis();
        particleDelay = 70 + (long) (Math.random() * 90);
        if (particles.size() > 60) return;
        particles.add(new GuiParticle((float) (Math.random() * screenWidth), (float) (Math.random() * screenHeight)));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    private void renderParticles(float globalAnim) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            GuiParticle p = particles.get(i);
            float t = p.progress();
            if (t >= 1F) { particles.remove(i); continue; }
            float grow = 1F - (1F - t) * (1F - t) * (1F - t);
            float radius = p.maxRadius * grow * S;
            if (radius < 0.4F) continue;
            float fade = smoothstep(0F, 0.18F, t) * (1F - smoothstep(0.45F, 1F, t));
            float alpha = fade * globalAnim * 0.75F;
            if (alpha < 0.004F) continue;
            float thickness = 1.1F + 2.6F * (1F - grow);
            float py = p.y - p.drift * grow * S;
            RenderUtil.Render2D.outline(p.x - radius, py - radius, radius * 2, radius * 2, thickness, ColorUtil.replAlpha(ColorUtil.client(), alpha), radius);
        }
    }

    private void drawScanLines(int screenWidth, int screenHeight, float globalAnim) {
        if (globalAnim <= 0.01F) return;
        scanLinesPipeline.draw(screenWidth, screenHeight, globalAnim * 0.18F, ColorUtil.getColor(255), 4F * S, 1F, 50F, 3000F, 100F);
    }

    private final HashMap<String, Float> descHeights = new HashMap<>();

    private float descHeight(Font font, String desc) {
        return descHeights.computeIfAbsent(desc + "_" + S, d -> font.getWrappedHeight(desc, 140 * S, 6.5F * S));
    }

    private final HashMap<String, float[]> smoothVals = new HashMap<>();

    private float smooth(String key, float target) {
        float[] v = smoothVals.computeIfAbsent(key, k -> new float[]{target});
        v[0] += (target - v[0]) * (1F - (float) Math.exp(-dt * 16F));
        return v[0];
    }

    /** Длительность кадра в секундах; все сглаживания привязаны к ней, поэтому не зависят от FPS. */
    private float dt = 1F / 60F;
    private long lastFrameNs = System.nanoTime();

    private void tickFrame() {
        long now = System.nanoTime();
        dt = Math.max(0.001F, Math.min(0.05F, (now - lastFrameNs) / 1_000_000_000F));
        lastFrameNs = now;
    }

    private final java.util.IdentityHashMap<GSetting, ru.mentions.utils.animation.satoshi.Animation> visAnims = new java.util.IdentityHashMap<>();

    private float visAnim(Feature f, GSetting setting) {
        ru.mentions.utils.animation.satoshi.Animation a = visAnims.computeIfAbsent(setting, k -> new EaseInOutQuad(300, 1));
        a.setDirection(setting.getVisible().get() ? Direction.FORWARDS : Direction.BACKWARDS);
        return a.getOutput();
    }

    private float chipsHeight(Iterable<String> values, float width) {
        Font font = Fonts.sf_regular;
        float px = 0, py = 0;
        for (String val : values) {
            float tw = font.getWidth(val, 6 * S) + 8 * S;
            if (px + tw > width && px > 0) { px = 0; py += 12 * S; }
            px += tw + 3 * S;
        }
        return py + 10 * S;
    }

    private final java.util.IdentityHashMap<GSetting, float[]> chipsHeights = new java.util.IdentityHashMap<>();

    private float chipsHeight(GSetting key, Iterable<String> values, float width) {
        float[] cached = chipsHeights.get(key);
        if (cached != null && cached[0] == width) return cached[1];
        float height = chipsHeight(values, width);
        chipsHeights.put(key, new float[]{width, height});
        return height;
    }

    private float modeChipsHeight(ModeSetting s, float width) {
        return chipsHeight(s, s.values(), width);
    }

    private float multiChipsHeight(MultiBooleanSetting s, float width) {
        float[] cached = chipsHeights.get(s);
        if (cached != null && cached[0] == width) return cached[1];
        return chipsHeight(s, multiNames(s), width);
    }

    private String query() { return searchQuery.trim().toLowerCase(); }
    private boolean searching() { return !query().isEmpty(); }

    private boolean moduleVisible(Feature f) {
        String q = query();
        if (q.isEmpty()) return tab.accepts(f);
        return f.getName().toLowerCase().contains(q) || f.getBigName().toLowerCase().contains(q) || f.getDesc().toLowerCase().contains(q) || f.getCategory().getName().toLowerCase().contains(q);
    }

    private int searchResults() {
        int count = 0;
        for (Feature f : Features.all()) if (moduleVisible(f)) count++;
        return count;
    }

    private void searchChanged() {
        searchTypeTime = System.currentTimeMillis();
        scrollTarget = 0;
    }

    private void clearSearch() {
        if (searchQuery.isEmpty()) return;
        searchQuery = "";
        searchChanged();
    }

    private java.util.List<String> multiNames(MultiBooleanSetting s) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (BooleanSetting b : s.getValues()) names.add(b.getName());
        return names;
    }

    @Override
    protected void init() {
        exit = false;
        searchActive = false;
        searchTypeTime = System.currentTimeMillis();
        bindingModule = null;
        GuiSounds.open();
        glomalAnim.run(1, 0.5F, Easings.EXPO_OUT);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        closeCheck();
        glomalAnim.update();
        mouseX = (int) (mouseX / scaleFix);
        mouseY = (int) (mouseY / scaleFix);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        context.createNewRootLayer();
        Render2D.beginOverlay();
        renderOverlay(context, null);
    }

    public void renderOverlay(DrawContext context, RenderTickCounter tickCounter) {
        tickFrame();
        float targetScale = 2F;
        float currentScale = (float) mc.getWindow().getScaleFactor();
        scaleFix = targetScale / currentScale;

        int screenWidth  = (int) (mc.getWindow().getScaledWidth()  / scaleFix);
        int screenHeight = (int) (mc.getWindow().getScaledHeight() / scaleFix);

        float globalAnim = glomalAnim.get();

        ru.mentions.utils.render.FrameTextures.ensure();

        if (context != null) context.getMatrices().pushMatrix();


        if (effect("Серый фон")) grayscalePipeline.draw(globalAnim);
        ScreenBlur.capture(1);
        if (effect("Размывать фон")) {
            RenderUtil.Blur.blur(0, 0, screenWidth, screenHeight, globalAnim, 0, ColorUtil.getColor(0, 0));
        }
        if (effect("Затемнять фон")) {
            RenderUtil.Images.texture(Identifier.of("client","textures/frame/background.png"), 0, 0, screenWidth, screenHeight, ColorUtil.multAlpha(ColorUtil.client(), globalAnim));
        }

        // Затемнение фона убрано: мир за меню не темнеет.

        ScreenBlur.capture();

        SliderSetting sizeSlider = SIZE;
        if (draggingSlider != sizeSlider) S = sizeSlider.getValue();

        float w = 690 * S;
        float h = 325 * S;
        float x = winX(screenWidth);
        float y = screenHeight / 2F - h / 2 + (exit ? 60 * S - 60 * S * globalAnim : -60 * S + 60 * S * globalAnim);

        Font draw = Fonts.sf_regular;
        Font regular = Fonts.sf_regular;
        Font guiicon = Fonts.gui;

        float side = 150 * S;
        float header = 46 * S;
        float pad = 12 * S;
        float radius = 22 * S;
        int line = ColorUtil.getColor(255, 0.07F * globalAnim);

        moduleHits.clear();
        tabHits.clear();
        configHits.clear();

        ScreenBlur.capture();

        RenderUtil.Render2D.glow(x, y, w, h - 0.5F * S, ColorUtil.getColor(0, 0.28F * globalAnim), radius, 22, 1);
        RenderUtil.Blur.blur(x, y, w, h, globalAnim, radius, ColorUtil.multAlpha(ColorUtil.multDark(ColorUtil.background(), 0.6F), globalAnim));
        RenderUtil.Render2D.rect(x, y, w, h, ColorUtil.getColor(13, 14, 19, 0.82F * globalAnim), radius);
        RenderUtil.Render2D.outline(x, y, w, h, 0.75F * S, ColorUtil.getColor(255, 0.08F * globalAnim), radius);

        RenderUtil.Render2D.rect(x + side, y + 8 * S, 0.6F * S, h - 16 * S, line, 0);
        RenderUtil.Render2D.rect(x + 8 * S, y + header, w - 16 * S, 0.6F * S, line, 0);

        // логотип клиента над боковой панелью
        RenderUtil.Images.texture(code.wiosvisual.utils.display.render.BackgroundIcons.icon("wm_icon.png"), x + side / 2F - 14 * S, y + header / 2F - 14 * S, 28 * S, 28 * S,
                ColorUtil.replAlpha(ColorUtil.client(), globalAnim));

        // вкладки
        float ty = y + header + 10 * S;
        for (Tab t : Tab.values()) {
            if (t.section != null) {
                draw.draw(t.section, x + 12 * S, ty, 6F * S, ColorUtil.getColor(255, 0.32F * globalAnim));
                ty += 11 * S;
            }
            float ix = x + 8 * S;
            float iw = side - 16 * S;
            float ih = 22 * S;
            boolean hov = MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, ix, ty, iw, ih);
            t.anim.setDirection(tab == t ? Direction.FORWARDS : Direction.BACKWARDS);
            t.hover.setDirection(hov ? Direction.FORWARDS : Direction.BACKWARDS);
            float sel = t.anim.getOutput();
            float hv = t.hover.getOutput();

            RenderUtil.Render2D.rect(ix, ty, iw, ih, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * (sel * 0.10F + hv * 0.03F)), 11 * S);
            RenderUtil.Render2D.outline(ix, ty, iw, ih, 0.8F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sel * 0.85F), 11 * S);
            int textColor = ColorUtil.overCol(ColorUtil.getColor(255, globalAnim * (0.42F + 0.2F * hv)), ColorUtil.getColor(255, globalAnim), sel);
            int iconColor = ColorUtil.overCol(ColorUtil.getColor(255, globalAnim * (0.35F + 0.2F * hv)), ColorUtil.replAlpha(ColorUtil.client(), globalAnim), sel);
            Fonts.mentions_2.draw(t.icon, ix + 8 * S, ty + 7F * S, 7F * S, iconColor);
            draw.draw(t.title, ix + 22 * S, ty + 6.5F * S, 7.5F * S, textColor);
            tabHits.add(new Hit(ix, ty, iw, ih, t));
            ty += 24 * S;
        }

        // поиск в шапке
        float colX = x + side;
        float cw = w - side;
        {
            float ht = 22 * S;
            float xSearchBar = colX + pad;
            float ySearchBar = y + header / 2F - ht / 2F;
            float searchWidth = cw - 2 * pad;
            searchX = xSearchBar;
            searchY = ySearchBar;
            searchW = searchWidth;
            searchH = ht;

            boolean searchHover = MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xSearchBar, ySearchBar, searchWidth, ht);
            animation3.setDirection(searchHover || searchActive ? Direction.FORWARDS : Direction.BACKWARDS);
            animSearchFocus.setDirection(searchActive ? Direction.FORWARDS : Direction.BACKWARDS);
            animSearchText.setDirection(!searchQuery.isEmpty() ? Direction.FORWARDS : Direction.BACKWARDS);

            float hvs = animation3.getOutput();
            float focus = animSearchFocus.getOutput();
            float typed = animSearchText.getOutput();

            RenderUtil.Render2D.rect(xSearchBar, ySearchBar, searchWidth, ht, ColorUtil.getColor(255, globalAnim * (0.02F + 0.03F * Math.max(hvs, focus))), 11 * S);
            RenderUtil.Render2D.outline(xSearchBar, ySearchBar, searchWidth, ht, 0.7F * S, ColorUtil.overCol(ColorUtil.getColor(255, 0.05F * globalAnim), ColorUtil.replAlpha(ColorUtil.client(), globalAnim * 0.8F), focus), 7 * S);

            guiicon.draw("A", xSearchBar + 8 * S, ySearchBar + 8 * S, 6 * S, ColorUtil.overCol(ColorUtil.getColor(255, globalAnim * (0.3F + 0.4F * hvs)), ColorUtil.replAlpha(ColorUtil.client(), globalAnim), Math.max(hvs, focus)));

            float xSearchText = xSearchBar + 8 * S + 12 * S;
            float wSearchText = searchWidth - (xSearchText - xSearchBar) - 8 * S;

            float hidePlaceholder = Math.max(typed, focus);
            if (hidePlaceholder < 0.99F) {
                regular.draw("Search", xSearchText, ySearchBar + 7F * S + 4 * S * hidePlaceholder, 7.5F * S, ColorUtil.getColor(255, globalAnim * (1 - hidePlaceholder) * (0.35F + 0.4F * hvs)));
            }
            if (typed > 0.01F) {
                regular.drawFadingTextReverse(searchQuery, xSearchText, ySearchBar + 7F * S - 4 * S * (1 - typed), wSearchText, ColorUtil.getColor(255, globalAnim * typed * (0.6F + 0.4F * Math.max(hvs, focus))), 7.5F * S);
            }

            boolean justTyped = System.currentTimeMillis() - searchTypeTime < 500;
            float caretTarget = focus * (justTyped || (System.currentTimeMillis() / 500) % 2 == 0 ? 1F : 0F);
            float caret = smooth("search:caret", caretTarget);
            float caretX = smooth("search:caretx", xSearchText + Math.min(regular.getWidth(searchQuery, 7.5F * S), wSearchText) + 1.5F * S);
            RenderUtil.Render2D.rect(caretX, ySearchBar + 6F * S, 0.6F * S, 10F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * caret), 0.3F * S);
        }

        // список модулей
        float listTop = y + header + 1 * S;
        float listHeight = h - header - 9 * S;
        float listBottom = listTop + listHeight;
        listX = colX;
        listY = listTop;
        listW = cw;
        listH = listHeight;

        Scissor.enable(colX, listTop, cw, listHeight, 2);

        float f60 = dt * 60F;
        scrollVel += (scrollTarget - scrollAnim) * 0.09F * f60;
        scrollVel *= (float) Math.pow(0.84F, f60);
        scrollAnim += scrollVel * f60;
        if (Math.abs(scrollTarget - scrollAnim) < 0.05F && Math.abs(scrollVel) < 0.05F) { scrollAnim = scrollTarget; scrollVel = 0; }

        java.util.List<Feature> visM = new java.util.ArrayList<>();
        dropHiddenModules();
        for (Feature f : Features.all()) {
            // Модули, запрещённые на текущем сервере (ServerRestrictions), убраны мгновенно в dropHiddenModules().
            if (f.module().isHidden()) continue;
            f.getAnimation14().setDirection(moduleVisible(f) ? Direction.FORWARDS : Direction.BACKWARDS);
            if (f.getAnimation14().getOutput() > 0) visM.add(f);
        }

        float cardW = (cw - 3 * pad) / 2F;
        float cardH = 26 * S;
        float gap = 8 * S;
        float catAnim = animCategoryReset.getOutput();
        float yCur = listTop + pad - scrollAnim;
        int rowIdx = 0;
        int found = searchResults();

        if (tab == Tab.CONFIGS && !searching()) {
            draw.draw("ЛКМ — загрузить конфиг, ПКМ — сохранить текущие настройки в него", colX + pad + 2 * S, yCur, 6.5F * S, ColorUtil.getColor(255, 0.4F * globalAnim));
            yCur += 15 * S;
            String[] configs = code.wiosvisual.utils.config.ConfigManager.getAvailableConfigs();
            String activeConfig = code.wiosvisual.utils.config.ConfigManager.getActiveConfig();
            if (configs != null) {
                for (int i = 0; i < configs.length; i++) {
                    float stagger = Math.min((i / 2) * 0.09F, 0.7F);
                    float rowT = Math.max(0F, Math.min(1F, (catAnim - stagger) / Math.max(0.01F, 1F - stagger)));
                    float cardX = colX + pad + (i % 2) * (cardW + pad) + catDir * (1F - rowT) * 26F * S;
                    if (yCur + cardH >= listTop && yCur <= listBottom) {
                        drawConfigCard(configs[i], configs[i].equals(activeConfig), cardX, yCur, cardW, cardH, globalAnim * rowT, draw);
                    }
                    if (i % 2 == 1 || i == configs.length - 1) {
                        yCur += cardH + gap;
                    }
                }
            }
        } else {
            for (GuiCategory cat : GuiCategory.values()) {
                java.util.List<Feature> group = new java.util.ArrayList<>();
                for (Feature f : visM) {
                    if (f.getCategory() == cat) group.add(f);
                }
                if (group.isEmpty()) continue;
                if (tab != Tab.HUD) {
                    draw.draw(cat.getName(), colX + pad + 2 * S, yCur, 6.5F * S, ColorUtil.getColor(255, 0.4F * globalAnim));
                    yCur += 14 * S;
                }
                for (int mi = 0; mi < group.size(); mi += 2) {
                    Feature fa = group.get(mi);
                    Feature fb = (mi + 1 < group.size()) ? group.get(mi + 1) : null;
                    float rowAnim = Math.max(fa.getAnimation14().getOutput(), fb != null ? fb.getAnimation14().getOutput() : 0F);

                    float stagger = Math.min(rowIdx * 0.09F, 0.7F);
                    float rowT = Math.max(0F, Math.min(1F, (catAnim - stagger) / Math.max(0.01F, 1F - stagger)));
                    float rowX = colX + pad + catDir * (1F - rowT) * 26F * S;

                    if (yCur + cardH >= listTop && yCur <= listBottom) {
                        drawFeatureCard(fa, rowX, yCur, cardW, cardH, globalAnim, draw);
                        if (fb != null) drawFeatureCard(fb, rowX + cardW + pad, yCur, cardW, cardH, globalAnim, draw);
                    }
                    yCur += (cardH + gap) * rowAnim;
                    rowIdx++;
                }
                yCur += 6 * S;
            }
        }

        animSearchEmpty.setDirection(searching() && found == 0 ? Direction.FORWARDS : Direction.BACKWARDS);
        float emptyAnim = animSearchEmpty.getOutput();

        if (emptyAnim > 0.01F) {
            float xEmpty = colX + cw / 2F;
            float yEmpty = listTop + listHeight / 2F - 8 * S + 6 * S - 6 * S * emptyAnim;
            guiicon.drawCentered("A", xEmpty, yEmpty, 8 * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * emptyAnim * 0.4F));
            draw.drawCentered("Ничего не найдено", xEmpty, yEmpty + 14 * S, 7 * S, ColorUtil.getColor(255, globalAnim * emptyAnim * 0.5F));
        }

        maxScroll = Math.max(0, yCur + scrollAnim - listBottom + pad);
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;

        Scissor.reset();

        animation2.setDirection(select != null ? Direction.FORWARDS : Direction.BACKWARDS);
        float panelAnim = animation2.getOutput();
        if (panelAnim > 0.01F) {
        globalAnim *= panelAnim;
        float setW = 150 * S;
        float setH = 250 * S;
        float setX = settingsX(screenWidth, x, w) - (1F - panelAnim) * 24 * S;
        float setY = y;

        RenderUtil.Render2D.glow(setX, setY, setW, setH - 0.5F * S, ColorUtil.getColor(0, 0.28F * globalAnim), 22 * S, 22, 1);
        RenderUtil.Blur.blur(setX, setY, setW, setH, globalAnim, 22 * S, ColorUtil.multAlpha(ColorUtil.multDark(ColorUtil.background(), 0.6F), globalAnim));
        RenderUtil.Render2D.rect(setX, setY, setW, setH, ColorUtil.getColor(13, 14, 19, 0.82F * globalAnim), 22 * S);
        RenderUtil.Render2D.outline(setX, setY, setW, setH, 0.75F * S, ColorUtil.getColor(255, 0.08F * globalAnim), 22 * S);

        float xSetBase = setX + 6 * S;
        float ySetBase = setY + 6 * S;
        float wSetBase = setW - 12 * S;
        float hSetBase = setH - 12 * S;

        RenderUtil.Render2D.glow(xSetBase, ySetBase, wSetBase, hSetBase, ColorUtil.getColor(0, 0.04F * globalAnim), 7 * S, 8, 1);
        RenderUtil.Render2D.rect(xSetBase, ySetBase, wSetBase, hSetBase, ColorUtil.getColor(0, 0.15F * globalAnim), 12 * S);

        Scissor.enable(xSetBase, ySetBase, wSetBase, hSetBase, 2);

        float xSetting = xSetBase;
        float ySetting = ySetBase;
        float wSetting = wSetBase;
        float hSetting = hSetBase;

        settingScrollVel += (settingScrollTarget - settingScrollAnim) * 0.09F * f60;
        settingScrollVel *= (float) Math.pow(0.84F, f60);
        settingScrollAnim += settingScrollVel * f60;
        if (Math.abs(settingScrollTarget - settingScrollAnim) < 0.05F && Math.abs(settingScrollVel) < 0.05F) { settingScrollAnim = settingScrollTarget; settingScrollVel = 0; }

        float xST = xSetting + 10 * S;
        float yST = ySetting + 10 * S - settingScrollAnim;
        float wST = wSetting - 20 * S;
        float setTop = ySetting;
        float setBottom = ySetting + hSetting;

        for (Feature f : Features.all()) {
            f.animation3.setDirection(f == select ? Direction.FORWARDS : Direction.BACKWARDS);
            float fa = f.animation3.getOutput();

            if (fa > 0) {
                for (GSetting setting : f.getSettings()) {

                    if (setting instanceof SliderSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            float percent = MathHelper.clamp((s.getValue() - s.min) / (s.max - s.min), 0, 1);
                            s.getAnimation().update();
                            s.getAnimation().run(percent, 0.06F, Easings.LINEAR);

                            boolean onScreen = yST + 25 * S >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, 24 * S);
                            s.animation.setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = s.animation.getOutput();
                            float track = wST - 12 * S;

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, 25 * S, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, 25 * S, ColorUtil.getColor(40, 0.15F * globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, 25 * S, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * hover), 8 * S);
                                draw.draw(s.getName(), xST + 6 * S, yST + 5 * S, 6.5F * S, ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)));

                                String val = String.valueOf((Math.round(s.getValue() * 100.0) / 100.0));
                                draw.draw(val, xST + wST - 6 * S - draw.getWidth(val, 6 * S), yST + 5.25F * S, 6 * S, ColorUtil.replAlpha(ColorUtil.client(), (globalAnim * sa) * (0.6F + 0.4F * hover)));

                                RenderUtil.Render2D.rect(xST + 6 * S, yST + 16.5F * S, track, 3 * S, ColorUtil.getColor(0, (globalAnim * sa) * (0.15F)), 1.5F * S);
                                RenderUtil.Render2D.rect(xST + 6 * S, yST + 16.5F * S, track * s.getAnimation().get(), 3 * S, ColorUtil.replAlpha(ColorUtil.client(), (globalAnim * sa) * (0.5F + 0.5F * hover)), 1.5F * S);
                                RenderUtil.Render2D.rect(xST + 6 * S + track * s.getAnimation().get() - 3 * S, yST + 15 * S, 6 * S, 6 * S, ColorUtil.getColor((int) (200 + 55 * hover), (globalAnim * sa)), 6 * S);
                                RenderUtil.Render2D.rect(xST + 6 * S + track * s.getAnimation().get() - 2 * S, yST + 16 * S, 4 * S, 4 * S, ColorUtil.replAlpha(ColorUtil.client(), (globalAnim * sa) * (0.7F + 0.3F * hover)), 6 * S);
                            }
                            if (draggingSlider == s) {
                                float perc = MathUtil.clamp(((float) lastMouseX - (xST + 6 * S)) / track, 0, 1);
                                float newVal = MathUtil.clamp(MathUtil.round(s.min + (s.max - s.min) * perc, s.increment), s.min, s.max);
                                if (s.getValue() != newVal) { s.set(newVal); GuiSounds.sliderTick(perc); }
                            }
                            yST += 30 * S * fa * vis;
                        }
                    }

                    if (setting instanceof ModeSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            float hMode = 15 * S + modeChipsHeight(s, wST - 12 * S) + 4 * S;
                            boolean onScreen = yST + hMode >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, hMode);
                            s.animation.setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = s.animation.getOutput();

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, hMode, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, hMode, ColorUtil.getColor(40, 0.15F * globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, hMode, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * hover), 8 * S);

                                String current = s.getValue();
                                draw.drawFadingText(s.getName(), xST + 6 * S, yST + 4.5F * S, wST - draw.getWidth(current, 6 * S) - 16 * S, ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)), 6.5F * S);
                                draw.draw(current, xST + wST - 6 * S - draw.getWidth(current, 6 * S), yST + 4.75F * S, 6 * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * (0.6F + 0.4F * hover)));

                                float chipMaxW = wST - 12 * S;
                                float px = 0, py = 0;
                                for (String val : s.values()) {
                                    float tw = draw.getWidth(val, 6 * S) + 8 * S;
                                    if (px + tw > chipMaxW && px > 0) { px = 0; py += 12 * S; }
                                    float cx = xST + 6 * S + px;
                                    float cyy = yST + 15 * S + py;
                                    ru.mentions.utils.animation.satoshi.Animation chip = chipAnim(f.getName() + ":" + s.getName() + ":" + val);
                                    chip.setDirection(val.equals(current) ? Direction.FORWARDS : Direction.BACKWARDS);
                                    float sel = chip.getOutput();

                                    RenderUtil.Render2D.rect(cx, cyy, tw, 10 * S, ColorUtil.overCol(ColorUtil.getColor(0, 0.25F * globalAnim * sa), ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * (0.5F + 0.5F * hover)), sel), 3 * S);
                                    regular.drawCentered(val, cx + tw / 2, cyy + 1.5F * S, 6 * S, ColorUtil.getColor(255, globalAnim * sa * (0.35F + 0.65F * sel) * (0.7F + 0.3F * hover)));
                                    px += tw + 3 * S;
                                }
                            }
                            yST += (hMode + 5 * S) * fa * vis;
                        }
                    }

                    if (setting instanceof MultiBooleanSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            float hMulti = 15 * S + multiChipsHeight(s, wST - 12 * S) + 4 * S;
                            boolean onScreen = yST + hMulti >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, hMulti);
                            s.getAnimation1().setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = s.getAnimation1().getOutput();

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, hMulti, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, hMulti, ColorUtil.getColor(40, 0.15F * globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, hMulti, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * hover), 8 * S);

                                int total = s.getValues().size();
                                int selectedCount = 0;
                                for (BooleanSetting b : s.getValues()) if (b.getValue()) selectedCount++;
                                String counter = selectedCount + " / " + total;

                                draw.drawFadingText(s.getName(), xST + 6 * S, yST + 4.5F * S, wST - draw.getWidth(counter, 6 * S) - 16 * S, ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)), 6.5F * S);
                                draw.draw(counter, xST + wST - 6 * S - draw.getWidth(counter, 6 * S), yST + 4.75F * S, 6 * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * (0.6F + 0.4F * hover)));

                                float chipMaxW = wST - 12 * S;
                                float px = 0, py = 0;
                                for (BooleanSetting b : s.getValues()) {
                                    float tw = draw.getWidth(b.getName(), 6 * S) + 8 * S;
                                    if (px + tw > chipMaxW && px > 0) { px = 0; py += 12 * S; }
                                    float cx = xST + 6 * S + px;
                                    float cyy = yST + 15 * S + py;
                                    ru.mentions.utils.animation.satoshi.Animation chip = chipAnim(f.getName() + ":" + s.getName() + ":" + b.getName());
                                    chip.setDirection(b.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
                                    float sel = chip.getOutput();

                                    RenderUtil.Render2D.rect(cx, cyy, tw, 10 * S, ColorUtil.overCol(ColorUtil.getColor(0, 0.25F * globalAnim * sa), ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * (0.5F + 0.5F * hover)), sel), 3 * S);
                                    regular.drawCentered(b.getName(), cx + tw / 2, cyy + 1.5F * S, 6 * S, ColorUtil.getColor(255, globalAnim * sa * (0.35F + 0.65F * sel) * (0.7F + 0.3F * hover)));
                                    px += tw + 3 * S;
                                }
                            }
                            yST += (hMulti + 5 * S) * fa * vis;
                        }
                    }

                    if (setting instanceof BooleanSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            boolean onScreen = yST + 16 * S >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, 16 * S);
                            s.animation.setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = s.animation.getOutput();
                            s.animation2.setDirection(s.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
                            float sanimation2 = s.animation2.getOutput();

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, 16 * S, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, 16 * S, ColorUtil.getColor(40, 0.15F * globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, 16 * S, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * hover), 8 * S);

                                draw.draw(s.getName(), xST + 6 * S, yST + 4 * S, 6.5F * S, ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)));
                                RenderUtil.Render2D.rect(xST + wST - 6 * S - 12 * S, yST + 4.5F * S, 12 * S, 7 * S, ColorUtil.overCol(ColorUtil.getColor(0, 0.2F * globalAnim * sa), ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * (0.5F + 0.5F * hover)), sanimation2), 3.5F * S);
                                RenderUtil.Render2D.rect(xST + wST - 6 * S - 12 * S + 1.25F * S + 4.5F * S * sanimation2, yST + 5.75F * S, 4.5F * S, 4.5F * S, ColorUtil.getColor(255, globalAnim * sa * (0.2F + 0.4F * sanimation2 + 0.4F * hover)), 2.5F * S);
                            }
                            yST += 21 * S * fa * vis;
                        }
                    }

                    if (setting instanceof ButtonSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            boolean onScreen = yST + 16 * S >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, 16 * S);
                            ru.mentions.utils.animation.satoshi.Animation btn = chipAnim(f.getName() + ":" + s.getName() + ":btn");
                            btn.setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = btn.getOutput();

                            s.pressAnim.update();
                            float press = s.pressAnim.get();
                            float accent = Math.max(hover, press);

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, 16 * S, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, 16 * S, ColorUtil.overCol(ColorUtil.getColor(40, 0.15F * globalAnim * sa), ColorUtil.replAlpha(ColorUtil.client(), 0.25F * globalAnim * sa), accent), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, 16 * S, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * accent), 8 * S);
                                RenderUtil.Render2D.glow(xST, yST, wST, 16 * S, ColorUtil.replAlpha(ColorUtil.client(), 0.15F * globalAnim * sa * press), 5 * S, 8, 1);
                                draw.drawCentered(s.getName(), xST + wST / 2, yST + 4 * S, 6.5F * S, ColorUtil.overCol(ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)), ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa), press));
                            }
                            yST += 21 * S * fa * vis;
                        }
                    }

                    if (setting instanceof BindSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            boolean onScreen = yST + 16 * S >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, 16 * S);
                            s.animation.setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = s.animation.getOutput();

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, 16 * S, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, 16 * S, ColorUtil.getColor(40, 0.15F * globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, 16 * S, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * hover), 8 * S);

                                boolean binding = activeBind == s;
                                ru.mentions.utils.animation.satoshi.Animation bindAct = chipAnim(f.getName() + ":" + s.getName() + ":act");
                                bindAct.setDirection(binding ? Direction.FORWARDS : Direction.BACKWARDS);
                                float act = bindAct.getOutput();

                                String keyName = binding ? "..." : (s.get() == -1 ? "n/a" : Keyboard.keyName(s.get()));
                                float bw = smooth(f.getName() + ":" + s.getName() + ":bw", draw.getWidth(keyName, 6 * S) + 10 * S);

                                draw.drawFadingText(s.getName(), xST + 6 * S, yST + 4 * S, wST - bw - 16 * S, ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)), 6.5F * S);
                                RenderUtil.Render2D.rect(xST + wST - 6 * S - bw, yST + 3 * S, bw, 10 * S, ColorUtil.overCol(ColorUtil.getColor(0, 0.25F * globalAnim * sa), ColorUtil.replAlpha(ColorUtil.client(), 0.15F * globalAnim * sa), act), 3 * S);
                                RenderUtil.Render2D.outline(xST + wST - 6 * S - bw, yST + 3 * S, bw, 10 * S, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * act), 3 * S);
                                RenderUtil.Render2D.glow(xST + wST - 6 * S - bw, yST + 3 * S, bw, 10 * S, ColorUtil.replAlpha(ColorUtil.client(), 0.12F * globalAnim * sa * act), 3 * S, 6, 1);
                                regular.drawCentered(keyName, xST + wST - 6 * S - bw / 2, yST + 4.5F * S, 6 * S, ColorUtil.replAlpha(ColorUtil.overCol(ColorUtil.getColor(255), ColorUtil.client(), act), globalAnim * sa * (0.45F + 0.55F * Math.max(act, hover))));
                            }
                            yST += 21 * S * fa * vis;
                        }
                    }

                    if (setting instanceof StringSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            boolean onScreen = yST + 16 * S >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, 16 * S);
                            s.getAnimation1().setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = s.getAnimation1().getOutput();

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, 16 * S, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, 16 * S, ColorUtil.getColor(40, 0.15F * globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, 16 * S, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * hover), 8 * S);

                                boolean editing = activeString == s;
                                ru.mentions.utils.animation.satoshi.Animation editAct = chipAnim(f.getName() + ":" + s.getName() + ":act");
                                editAct.setDirection(editing ? Direction.FORWARDS : Direction.BACKWARDS);
                                float act = editAct.getOutput();

                                String shown = editing ? stringBuffer + ((System.currentTimeMillis() / 400) % 2 == 0 ? "_" : "") : s.getValue();
                                if (shown.isEmpty()) shown = "...";

                                float bw = smooth(f.getName() + ":" + s.getName() + ":bw", Math.min(draw.getWidth(shown, 6 * S) + 10 * S, wST * 0.55F));

                                draw.drawFadingText(s.getName(), xST + 6 * S, yST + 4 * S, wST - bw - 16 * S, ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)), 6.5F * S);
                                RenderUtil.Render2D.rect(xST + wST - 6 * S - bw, yST + 3 * S, bw, 10 * S, ColorUtil.overCol(ColorUtil.getColor(0, 0.25F * globalAnim * sa), ColorUtil.replAlpha(ColorUtil.client(), 0.15F * globalAnim * sa), act), 3 * S);
                                RenderUtil.Render2D.outline(xST + wST - 6 * S - bw, yST + 3 * S, bw, 10 * S, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * act), 3 * S);
                                RenderUtil.Render2D.glow(xST + wST - 6 * S - bw, yST + 3 * S, bw, 10 * S, ColorUtil.replAlpha(ColorUtil.client(), 0.12F * globalAnim * sa * act), 3 * S, 6, 1);
                                regular.drawFadingText(shown, xST + wST - 6 * S - bw + 5 * S, yST + 4.5F * S, bw - 8 * S, ColorUtil.getColor(255, globalAnim * sa * (0.45F + 0.55F * Math.max(act, hover))), 6 * S);
                            }
                            yST += 21 * S * fa * vis;
                        }
                    }

                    if (setting instanceof ColorSetting s) {
                        float vis = visAnim(f, s);
                        if (vis > 0.01F) {
                            float sa = fa * vis;
                            s.pickerAnim.setDirection(s.pickerOpen ? Direction.FORWARDS : Direction.BACKWARDS);
                            float open = s.pickerAnim.getOutput();
                            float hCol = 16 * S + open * 35 * S;

                            boolean onScreen = yST + hCol >= setTop && yST <= setBottom;
                            boolean hovS = onScreen && MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, xST, yST, wST, hCol);
                            s.getAnimation1().setDirection(hovS ? Direction.FORWARDS : Direction.BACKWARDS);
                            float hover = s.getAnimation1().getOutput();

                            float bx = xST + 6 * S;
                            float bwd = wST - 12 * S;
                            int col = s.getValue();
                            String kb = f.getName() + ":" + s.getName();

                            if (onScreen) {
                                RenderUtil.Render2D.glow(xST, yST, wST, hCol, ColorUtil.getColor(0, 0.06F * globalAnim * sa), 5 * S, 7, 1);
                                RenderUtil.Render2D.rect(xST, yST, wST, hCol, ColorUtil.getColor(40, 0.15F * globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(xST, yST, wST, hCol, 0.5F * S, ColorUtil.replAlpha(ColorUtil.client(), globalAnim * sa * hover), 8 * S);

                                draw.drawFadingText(s.getName(), xST + 6 * S, yST + 4 * S, wST - 34 * S, ColorUtil.getColor(255, (globalAnim * sa) * (0.5F + 0.5F * hover)), 6.5F * S);

                                int pr = (int) smooth(kb + ":pr", (col >> 16) & 0xFF);
                                int pg = (int) smooth(kb + ":pg", (col >> 8) & 0xFF);
                                int pb = (int) smooth(kb + ":pb", col & 0xFF);

                                float pcx = xST + wST - 6 * S - 9 * S;
                                float pcy = yST + 4 * S;

                                RenderUtil.Render2D.rect(pcx, pcy, 8 * S, 8 * S, ColorUtil.getColor(pr, pg, pb, globalAnim * sa), 8 * S);
                                RenderUtil.Render2D.outline(pcx - 0.75F * S, pcy - 0.75F * S, 9.5F * S, 9.5F * S, 0.25F * S, ColorUtil.getColor(pr, pg, pb, globalAnim * sa * (0.4F + 0.6F * hover)), 8 * S);
                                RenderUtil.Render2D.glow(pcx, pcy, 8 * S, 8 * S, ColorUtil.getColor(pr, pg, pb, 0.1F * globalAnim * sa), 8 * S, 7, 1);

                                if (open > 0.01F) {
                                    float[] hsb = java.awt.Color.RGBtoHSB((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, null);
                                    float shh = smooth(kb + ":h", hsb[0]);
                                    float shs = smooth(kb + ":s", hsb[1]);
                                    float shb = smooth(kb + ":b", hsb[2]);
                                    float[] smoothHsb = {shh, shs, shb};

                                    int segs = 16;
                                    float seg = bwd / segs;

                                    for (int bar = 0; bar < 3; bar++) {
                                        float by = yST + 18 * S + bar * 12 * S;

                                        for (int i = 0; i < segs; i++) {
                                            float t0 = (float) i / segs;
                                            float t1 = (float) (i + 1) / segs;
                                            int c0 = switch (bar) { case 0 -> java.awt.Color.HSBtoRGB(t0, 1F, 1F); case 1 -> java.awt.Color.HSBtoRGB(shh, t0, shb); default -> java.awt.Color.HSBtoRGB(shh, shs, t0); };
                                            int c1 = switch (bar) { case 0 -> java.awt.Color.HSBtoRGB(t1, 1F, 1F); case 1 -> java.awt.Color.HSBtoRGB(shh, t1, shb); default -> java.awt.Color.HSBtoRGB(shh, shs, t1); };
                                            int a0 = ColorUtil.replAlpha(c0, globalAnim * sa * open);
                                            int a1 = ColorUtil.replAlpha(c1, globalAnim * sa * open);

                                            RenderUtil.Render2D.gradientRect(bx + i * seg, by, seg + (i == segs - 1 ? 0 : 0.5F * S), 4 * S, new int[]{a0, a1, a1, a0}, i == 0 ? 2 * S : 0, i == segs - 1 ? 2 * S : 0, i == segs - 1 ? 2 * S : 0, i == 0 ? 2 * S : 0);
                                        }

                                        float kx = bx + bwd * smoothHsb[bar];
                                        int kc = switch (bar) { case 0 -> java.awt.Color.HSBtoRGB(shh, 1F, 1F); default -> java.awt.Color.HSBtoRGB(shh, shs, shb); };
                                        RenderUtil.Render2D.rect(kx - 3 * S, by - 1 * S, 6 * S, 6 * S, ColorUtil.getColor(255, globalAnim * sa * open), 6 * S);
                                        RenderUtil.Render2D.rect(kx - 2 * S, by, 4 * S, 4 * S, ColorUtil.replAlpha(kc, globalAnim * sa * open), 6 * S);
                                    }
                                }
                            }

                            if (open > 0.01F && draggingColor == s) {
                                float[] hsb = java.awt.Color.RGBtoHSB((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, null);
                                float t = MathUtil.clamp(((float) lastMouseX - bx) / bwd, 0, 1);
                                float[] nh = {hsb[0], hsb[1], hsb[2]};
                                nh[draggingColorBar] = t;
                                int rgb = java.awt.Color.HSBtoRGB(nh[0], nh[1], nh[2]);
                                int newCol = (col & 0xFF000000) | (rgb & 0x00FFFFFF);
                                if (newCol != col) { s.set(newCol); GuiSounds.colorTick(t); }
                            }
                            yST += (hCol + 5 * S) * fa * vis;
                        }
                    }
                }
            }
        }

        float contentHS = yST + settingScrollAnim - (ySetting + 10 * S);
        settingMaxScroll = Math.max(0, contentHS - (hSetting - 14 * S));
        if (settingScrollTarget > settingMaxScroll) settingScrollTarget = settingMaxScroll;

        Scissor.disable();
        }
        Client.get().render2D().flushAll();
        Render2D.endOverlay();
        if (context != null) context.getMatrices().popMatrix();
    }

    private float settingsX(int screenWidth, float guiX, float guiW) {
        float setW = 150 * S;
        float sx = guiX + guiW + 14 * S;
        if (sx + setW > screenWidth - 8 * S) sx = screenWidth - setW - 8 * S;
        return sx;
    }




    private void drawFeatureCard(Feature f, float x, float y, float cw, float ch, float g, Font draw) {
        float canim = f.getAnimation14().getOutput();
        float a = g * canim;
        f.getAnimation16().setDirection(f.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
        float enable = f.getAnimation16().getOutput();
        boolean hovered = MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, x, y, cw, ch);
        f.getAnimation12().setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
        float hover = f.getAnimation12().getOutput();
        f.animation1.setDirection(f == select ? Direction.FORWARDS : Direction.BACKWARDS);
        float selected = f.animation1.getOutput();

        RenderUtil.Render2D.rect(x, y, cw, ch, ColorUtil.getColor(255, a * (0.035F + 0.03F * hover)), 12 * S);
        RenderUtil.Render2D.outline(x, y, cw, ch, 0.7F * S, ColorUtil.overCol(ColorUtil.getColor(255, a * (0.07F + 0.06F * hover)), ColorUtil.replAlpha(ColorUtil.client(), a * 0.8F), selected), 12 * S);

        int arrowColor = ColorUtil.overCol(ColorUtil.getColor(255, a * (0.35F + 0.25F * hover)), ColorUtil.replAlpha(ColorUtil.client(), a), selected);
        draw.draw(">", x + 9 * S, y + ch / 2F - 4F * S, 7.5F * S, arrowColor);

        int nameColor = ColorUtil.overCol(ColorUtil.getColor(255, a * (0.42F + 0.15F * hover)), ColorUtil.getColor(255, a), enable);
        draw.draw(f.getBigName(), x + 21 * S, y + ch / 2F - 4F * S, 7.5F * S, nameColor);

        float sepX = x + cw - 38 * S;
        boolean bindingNow = bindingModule == f;
        if (bindingNow || f.getKey() != -1) {
            String keyName = bindingNow ? "..." : Keyboard.keyName(f.getKey()).replace("NONE", "n/a");
            draw.draw(keyName, sepX - 6 * S - draw.getWidth(keyName, 6F * S), y + ch / 2F - 3.2F * S, 6F * S,
                    ColorUtil.overCol(ColorUtil.getColor(255, a * 0.4F), ColorUtil.replAlpha(ColorUtil.client(), a), bindingNow ? 1F : enable * 0.6F));
        }
        RenderUtil.Render2D.rect(sepX, y + 5 * S, 0.6F * S, ch - 10 * S, ColorUtil.getColor(255, a * 0.08F), 0);

        float tw = 20 * S;
        float th = 10 * S;
        float tx = x + cw - 31 * S;
        float tyy = y + ch / 2F - th / 2F;
        RenderUtil.Render2D.rect(tx, tyy, tw, th, ColorUtil.overCol(ColorUtil.getColor(255, a * 0.10F), ColorUtil.replAlpha(ColorUtil.client(), a * (0.85F - 0.15F * hover)), enable), th / 2F);
        RenderUtil.Render2D.rect(tx + 1.5F * S + (tw - th) * enable, tyy + 1.5F * S, th - 3 * S, th - 3 * S, ColorUtil.getColor(255, a * (0.55F + 0.45F * enable)), (th - 3 * S) / 2F);

        if (canim > 0.5F) moduleHits.add(new Hit(x, y, cw, ch, f));
    }

    private void drawConfigCard(String name, boolean activeConfig, float x, float y, float cw, float ch, float a, Font draw) {
        boolean hovered = MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, x, y, cw, ch);
        float hover = smooth("cfg:" + name, hovered ? 1F : 0F);
        RenderUtil.Render2D.rect(x, y, cw, ch, ColorUtil.getColor(255, a * (0.035F + 0.03F * hover)), 8 * S);
        RenderUtil.Render2D.outline(x, y, cw, ch, 0.7F * S, activeConfig ? ColorUtil.replAlpha(ColorUtil.client(), a * 0.8F) : ColorUtil.getColor(255, a * (0.07F + 0.06F * hover)), 8 * S);
        draw.draw(name, x + 12 * S, y + ch / 2F - 4F * S, 7.5F * S, ColorUtil.getColor(255, a * (0.6F + 0.3F * hover)));
        if (activeConfig) {
            draw.draw("active", x + cw - 12 * S - draw.getWidth("active", 6F * S), y + ch / 2F - 3.2F * S, 6F * S, ColorUtil.replAlpha(ColorUtil.client(), a));
        }
        configHits.add(new Hit(x, y, cw, ch, name));
    }

    private void closeCheck() { if (exit && glomalAnim.isFinished()) { close(); exit = false; } }

    /**
     * Модуль, который сервер только что заблокировал, убирается мгновенно: закрываем его
     * настройки и ожидание бинда без анимации, чтобы ни кадра нельзя было с ним взаимодействовать.
     */
    private void dropHiddenModules() {
        if (select != null && select.module().isHidden()) {
            select = null;
            activeBind = null;
            animation2.finish(Direction.BACKWARDS);
        }
        if (bindingModule != null && bindingModule.module().isHidden()) {
            bindingModule = null;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float mouseX = (int) (click.x() / scaleFix);
        float mouseY = (int) (click.y() / scaleFix);

        dropHiddenModules();


        if (bindingModule != null) { bindingModule = null; GuiSounds.bindReset(); return true; }

        if (activeBind != null) {
            if (activeBind.allowMouse && click.button() != 0 && click.button() != 1) { activeBind.set(click.button()); GuiSounds.bindSet(); }
            else { GuiSounds.bindReset(); }
            activeBind = null; return true;
        }

        if (activeString != null) { activeString.set(stringBuffer); activeString = null; GuiSounds.editCommit(); }

        int screenWidth  = (int) (mc.getWindow().getScaledWidth()  / scaleFix);
        int screenHeight = (int) (mc.getWindow().getScaledHeight() / scaleFix);

        float w = 690 * S;
        float h = 325 * S;
        float x = winX(screenWidth);
        float y = screenHeight / 2F - h / 2;

        int button = click.button();
        Font draw = Fonts.sf_regular;

        if (MathUtil.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH)) {
            if (button == 1) { clearSearch(); searchActive = false; GuiSounds.searchClear(); }
            else if (button == 0) { if (!searchActive) GuiSounds.editStart(); searchActive = true; searchTypeTime = System.currentTimeMillis(); }
            return true;
        }

        searchActive = false;

        for (Hit hit : tabHits) {
            if (button == 0 && hit.contains(mouseX, mouseY)) {
                Tab clicked = (Tab) hit.payload;
                if (tab != clicked || searching()) {
                    clearSearch();
                    if (tab != clicked) {
                        catDir = Integer.compare(clicked.ordinal(), tab.ordinal());
                        tab = clicked;
                        animCategoryReset.reset();
                    }
                    scrollTarget = 0; scrollVel = 0; GuiSounds.category(clicked.ordinal(), Tab.values().length);
                }
                return true;
            }
        }

        if (MathUtil.isHovered(mouseX, mouseY, listX, listY, listW, listH)) {
            for (Hit hit : configHits) {
                if (hit.contains(mouseX, mouseY)) {
                    String name = (String) hit.payload;
                    if (button == 0) code.wiosvisual.utils.config.ConfigManager.loadConfig(name);
                    else if (button == 1) code.wiosvisual.utils.config.ConfigManager.saveConfig(name);
                    GuiSounds.button();
                    return true;
                }
            }
            for (Hit hit : moduleHits) {
                if (!hit.contains(mouseX, mouseY)) continue;
                Feature f = (Feature) hit.payload;
                if (f.module().isHidden()) continue;
                if (button == 2) { bindingModule = f; GuiSounds.bindStart(); return true; }
                if (button == 1 || (button == 0 && mouseX < hit.x + 22 * S)) {
                    select = (f == select) ? null : f; settingScrollTarget = 0; settingScrollAnim = 0; GuiSounds.expand(select == f);
                    return true;
                }
                if (button == 0) { f.setEnabled(!f.isEnabled()); return true; }
                return true;
            }
        }

        float setW = 150 * S;
        float setH = 250 * S;
        float setX = settingsX(screenWidth, x, w);
        float setY = y + (exit ? 60 * S - 60 * S * glomalAnim.get() : -60 * S + 60 * S * glomalAnim.get());
        float xSetting = setX + 6 * S;
        float ySetting = setY + 6 * S;
        float wSetting = setW - 12 * S;
        float hSetting = setH - 12 * S;

        if (MathUtil.isHovered(mouseX, mouseY, xSetting, ySetting, wSetting, hSetting) && select != null) {
            float xST = xSetting + 10 * S;
            float yST = ySetting + 10 * S - settingScrollAnim;
            float wST = wSetting - 20 * S;

            for (GSetting setting : select.getSettings()) {
                if (setting instanceof SliderSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        if (s.getVisible().get() && click.button() == 0 && MathUtil.isHovered(mouseX, mouseY, xST + 3 * S, yST + 11 * S, wST - 6 * S, 13 * S)) {
                            draggingSlider = s; GuiSounds.sliderGrab();
                        }
                        yST += 30 * S * vis;
                    }
                }
                if (setting instanceof ModeSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        float hMode = 15 * S + modeChipsHeight(s, wST - 12 * S) + 4 * S;
                        if (s.getVisible().get() && click.button() == 0) {
                            float chipMaxW = wST - 12 * S; float px = 0, py = 0; int chipIndex = 0;
                            for (String val : s.values()) {
                                float tw = draw.getWidth(val, 6 * S) + 8 * S;
                                if (px + tw > chipMaxW && px > 0) { px = 0; py += 12 * S; }
                                if (MathUtil.isHovered(mouseX, mouseY, xST + 6 * S + px, yST + 15 * S + py, tw, 10 * S)) {
                                    if (!val.equals(s.getValue())) GuiSounds.chip(chipIndex, s.values().size()); s.set(val);
                                }
                                px += tw + 3 * S; chipIndex++;
                            }
                        }
                        yST += (hMode + 5 * S) * vis;
                    }
                }
                if (setting instanceof MultiBooleanSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        float hMulti = 15 * S + multiChipsHeight(s, wST - 12 * S) + 4 * S;
                        if (s.getVisible().get() && click.button() == 0) {
                            float chipMaxW = wST - 12 * S; float px = 0, py = 0;
                            for (BooleanSetting b : s.getValues()) {
                                float tw = draw.getWidth(b.getName(), 6 * S) + 8 * S;
                                if (px + tw > chipMaxW && px > 0) { px = 0; py += 12 * S; }
                                if (MathUtil.isHovered(mouseX, mouseY, xST + 6 * S + px, yST + 15 * S + py, tw, 10 * S)) { b.set(!b.getValue()); GuiSounds.chipMulti(b.getValue()); }
                                px += tw + 3 * S;
                            }
                        }
                        yST += (hMulti + 5 * S) * vis;
                    }
                }
                if (setting instanceof BooleanSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        if (s.getVisible().get() && click.button() == 0 && MathUtil.isHovered(mouseX, mouseY, xST, yST, wST, 16 * S)) { s.set(!s.getValue()); GuiSounds.toggle(s.getValue()); }
                        yST += 21 * S * vis;
                    }
                }
                if (setting instanceof ButtonSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        if (s.getVisible().get() && click.button() == 0 && MathUtil.isHovered(mouseX, mouseY, xST, yST, wST, 16 * S)) { s.press(); GuiSounds.button(); }
                        yST += 21 * S * vis;
                    }
                }
                if (setting instanceof BindSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        if (s.getVisible().get() && click.button() == 0) {
                            String keyName = s.get() == -1 ? "n/a" : Keyboard.keyName(s.get());
                            float bw = draw.getWidth(keyName, 6 * S) + 10 * S;
                            if (MathUtil.isHovered(mouseX, mouseY, xST + wST - 6 * S - bw - 2 * S, yST + 1 * S, bw + 4 * S, 14 * S)) { activeBind = s; GuiSounds.bindStart(); }
                        }
                        yST += 21 * S * vis;
                    }
                }
                if (setting instanceof StringSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        if (s.getVisible().get() && click.button() == 0) {
                            String shown = s.getValue().isEmpty() ? "..." : s.getValue();
                            float bw = Math.min(draw.getWidth(shown, 6 * S) + 10 * S, wST * 0.55F);
                            if (MathUtil.isHovered(mouseX, mouseY, xST + wST - 6 * S - bw - 2 * S, yST + 1 * S, bw + 4 * S, 14 * S)) { activeString = s; stringBuffer = s.getValue(); GuiSounds.editStart(); }
                        }
                        yST += 21 * S * vis;
                    }
                }
                if (setting instanceof ColorSetting s) {
                    float vis = visAnim(select, s);
                    if (vis > 0.01F) {
                        float open = s.pickerAnim.getOutput();
                        float hCol = 16 * S + open * 35 * S;
                        if (s.getVisible().get() && click.button() == 0) {
                            if (MathUtil.isHovered(mouseX, mouseY, xST, yST, wST, 16 * S)) { s.pickerOpen = !s.pickerOpen; GuiSounds.picker(s.pickerOpen); }
                            else if (s.pickerOpen) {
                                for (int bar = 0; bar < 3; bar++) {
                                    float by = yST + 18 * S + bar * 12 * S;
                                    if (MathUtil.isHovered(mouseX, mouseY, xST + 6 * S, by - 3 * S, wST - 12 * S, 10 * S)) { draggingColor = s; draggingColorBar = bar; GuiSounds.sliderGrab(); }
                                }
                            }
                        }
                        yST += (hCol + 5 * S) * vis;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSlider != null || draggingColor != null) GuiSounds.sliderRelease();
        draggingSlider = null; draggingColor = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        dropHiddenModules();
        if (bindingModule != null) {
            boolean reset = key == 256 || key == 261;
            bindingModule.setKey(reset ? -1 : key); bindingModule = null;
            if (reset) GuiSounds.bindReset(); else GuiSounds.bindSet(); return true;
        }

        if (activeBind != null) {
            boolean reset = key == 256 || key == 261;
            activeBind.set(reset ? -1 : key); activeBind = null;
            if (reset) GuiSounds.bindReset(); else GuiSounds.bindSet(); return true;
        }

        if (searchActive) {
            if (key == 256) { if (!searchQuery.isEmpty()) clearSearch(); else searchActive = false; GuiSounds.searchClear(); }
            else if (key == 257 || key == 335) { searchActive = false; GuiSounds.editCommit(); }
            else if (key == 259 && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); searchChanged(); GuiSounds.erase(); }
            return true;
        }

        if (activeString != null) {
            if (key == 257 || key == 335) { activeString.set(stringBuffer); activeString = null; GuiSounds.editCommit(); }
            else if (key == 256) { activeString = null; GuiSounds.editCancel(); }
            else if (key == 259 && !stringBuffer.isEmpty()) { stringBuffer = stringBuffer.substring(0, stringBuffer.length() - 1); GuiSounds.erase(); }
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchActive) {
            if (input.isValidChar() && searchQuery.length() < SEARCH_LIMIT) { searchQuery += input.asString(); searchChanged(); GuiSounds.type(); }
            return true;
        }
        if (activeString != null) {
            if (input.isValidChar()) {
                String str = input.asString();
                if (!activeString.isOnlyNumber() || str.matches("[0-9.,-]+")) { stringBuffer += str; GuiSounds.type(); }
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean mouseScrolled(double mouseXRaw, double mouseYRaw, double horizontalAmount, double verticalAmount) {
        float mouseX = (float) (mouseXRaw / scaleFix);
        float mouseY = (float) (mouseYRaw / scaleFix);

        int screenWidth  = (int) (mc.getWindow().getScaledWidth()  / scaleFix);
        int screenHeight = (int) (mc.getWindow().getScaledHeight() / scaleFix);

        float w = 690 * S;
        float h = 325 * S;
        float x = winX(screenWidth);
        float y = screenHeight / 2F - h / 2;

        if (MathUtil.isHovered(mouseX, mouseY, listX, listY, listW, listH)) {
            float before = scrollTarget;
            scrollTarget = MathUtil.clamp((float) (scrollTarget - verticalAmount * 25 * S), 0, maxScroll);
            if (scrollTarget != before) GuiSounds.scroll(); return true;
        }

        float setW = 150 * S;
        float setH = 250 * S;
        float setX = settingsX(screenWidth, x, w);
        float setY = y + (exit ? 60 * S - 60 * S * glomalAnim.get() : -60 * S + 60 * S * glomalAnim.get());

        if (select != null && MathUtil.isHovered(mouseX, mouseY, setX + 6 * S, setY + 6 * S, setW - 12 * S, setH - 12 * S)) {
            float before = settingScrollTarget;
            settingScrollTarget = MathUtil.clamp((float) (settingScrollTarget - verticalAmount * 25 * S), 0, settingMaxScroll);
            if (settingScrollTarget != before) GuiSounds.scroll(); return true;
        }

        return super.mouseScrolled(mouseXRaw, mouseYRaw, horizontalAmount, verticalAmount);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() {
        if (!exit) { glomalAnim.run(0, 0.3F, Easings.QUART_IN); exit = true; GuiSounds.close(); } return false;
    }
}
