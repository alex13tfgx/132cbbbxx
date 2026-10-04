/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1076
 *  net.minecraft.class_1077
 *  net.minecraft.class_1664
 *  net.minecraft.class_2561
 *  net.minecraft.class_304
 *  net.minecraft.class_315
 *  net.minecraft.class_332
 *  net.minecraft.class_3419
 *  net.minecraft.class_3675
 *  net.minecraft.class_4063
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_5365
 */
package code.wiosvisual.display.screens.settings;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

public class WindowsSettingsScreen
extends Screen
implements QuickImports {
    private static final Color C_DIM = new Color(6, 6, 8, 215);
    private static final Color C_WINDOW = new Color(22, 22, 26, 255);
    private static final Color C_HEADER = new Color(30, 30, 35, 255);
    private static final Color C_SIDEBAR = new Color(26, 26, 31, 255);
    private static final Color C_CONTENT = new Color(19, 19, 23, 255);
    private static final Color C_CARD = new Color(33, 33, 39, 255);
    private static final Color C_CARD_HOVER = new Color(44, 44, 52, 255);
    private static final Color C_NAV_SEL = new Color(46, 46, 54, 255);
    private static final Color C_ACCENT = new Color(141, 99, 246, 255);
    private static final Color C_ACCENT_DIM = new Color(141, 99, 246, 60);
    private static final Color C_TEXT = new Color(237, 237, 242, 255);
    private static final Color C_SUBTEXT = new Color(150, 150, 162, 255);
    private static final Color C_OUTLINE = new Color(64, 64, 74, 150);
    private static final Color C_TRACK = new Color(64, 64, 74, 255);
    private static final Color C_SEARCH = new Color(38, 38, 44, 255);
    private static final NavItem[] NAV = new NavItem[]{new NavItem("home", "\u0413\u043b\u0430\u0432\u043d\u0430\u044f"), new NavItem("display", "\u0414\u0438\u0441\u043f\u043b\u0435\u0439"), new NavItem("graphics", "\u0413\u0440\u0430\u0444\u0438\u043a\u0430"), new NavItem("sound", "\u0417\u0432\u0443\u043a"), new NavItem("controls", "\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435"), new NavItem("keys", "\u041a\u043b\u0430\u0432\u0438\u0448\u0438"), new NavItem("chat", "\u0427\u0430\u0442"), new NavItem("appearance", "\u0412\u043d\u0435\u0448\u043d\u0438\u0439 \u0432\u0438\u0434"), new NavItem("language", "\u042f\u0437\u044b\u043a"), new NavItem("accessibility", "\u0421\u043f\u0435\u0446. \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438")};
    private final Screen parent;
    private int selected = 0;
    private final float[] navHover = new float[NAV.length];
    private float winX;
    private float winY;
    private float winW;
    private float winH;
    private float headerH;
    private float sidebarX;
    private float sidebarW;
    private float contentX;
    private float contentY;
    private float contentW;
    private float contentH;
    private final List<Slider> sliders = new ArrayList<Slider>();
    private final List<Toggle> toggles = new ArrayList<Toggle>();
    private final List<Cycle> cycles = new ArrayList<Cycle>();
    private final List<ClickRow> rows = new ArrayList<ClickRow>();
    private final HashMap<String, Float> toggleAnim = new HashMap();
    private Slider draggingSlider = null;
    private KeyBinding listeningBind = null;
    private float scroll = 0.0f;
    private float scrollTarget = 0.0f;
    private float contentVirtualHeight = 0.0f;
    private static float uiScale = 3.0f;
    private float lastScale = 3.0f;
    private float lastGuiScale = 1.0f;
    private float vw;
    private float vh;
    private String search = "";
    private boolean searchFocused = false;
    private float searchX;
    private float searchY;
    private float searchW;
    private float searchH;
    private float zPlusX;
    private float zMinusX;
    private float zBtnY;
    private float zBtnSize;

    public WindowsSettingsScreen(Screen parent) {
        super((Text)Text.literal((String)"\u041f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u044b"));
        this.parent = parent;
    }

    public void close() {
        mc.setScreen(this.parent);
    }

    public boolean shouldPause() {
        return true;
    }

    public void tick() {
        super.tick();
    }

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float margin;
        float S;
        MatrixStack matrices = GuiCompat.stack(context);
        float guiScale = (float)window.getScaleFactor();
        float realW = window.getWidth();
        float realH = window.getHeight();
        float maxS = Math.min(5.0f, Math.min(realW / 460.0f, realH / 300.0f));
        this.lastScale = S = WindowsSettingsScreen.clamp(uiScale, 1.5f, Math.max(1.5f, maxS));
        this.lastGuiScale = guiScale;
        this.vw = realW / S;
        this.vh = realH / S;
        int vmx = (int)((float)mouseX * guiScale / S);
        int vmy = (int)((float)mouseY * guiScale / S);
        matrices.push();
        matrices.scale(S / guiScale, S / guiScale, 1.0f);
        rectangle.render(ShapeProperties.create(matrices, 0.0, 0.0, this.vw, this.vh).color(C_DIM.getRGB()).build());
        this.winX = margin = (float)Math.max(10, Math.round(Math.min(this.vw, this.vh) * 0.05f));
        this.winY = margin;
        this.winW = this.vw - margin * 2.0f;
        this.winH = this.vh - margin * 2.0f;
        this.headerH = 30.0f;
        this.sidebarX = this.winX;
        this.sidebarW = Math.min(176.0f, Math.max(140.0f, this.winW * 0.26f));
        this.contentX = this.winX + this.sidebarW;
        this.contentY = this.winY + this.headerH;
        this.contentW = this.winW - this.sidebarW;
        this.contentH = this.winH - this.headerH;
        rectangle.render(ShapeProperties.create(matrices, this.winX, this.winY, this.winW, this.winH).round(10.0f).color(C_WINDOW.getRGB()).build());
        rectangle.render(ShapeProperties.create(matrices, this.contentX, this.contentY, this.contentW, this.contentH).round(0.0f, 0.0f, 10.0f, 0.0f).color(C_CONTENT.getRGB()).build());
        rectangle.render(ShapeProperties.create(matrices, this.sidebarX, this.contentY, this.sidebarW, this.contentH).round(0.0f, 0.0f, 0.0f, 10.0f).color(C_SIDEBAR.getRGB()).build());
        this.sliders.clear();
        this.toggles.clear();
        this.cycles.clear();
        this.rows.clear();
        this.renderHeader(context, vmx, vmy);
        this.renderSidebar(context, vmx, vmy, delta);
        this.scroll += (this.scrollTarget - this.scroll) * 0.25f;
        this.renderContent(context, vmx, vmy, delta);
        this.renderZoomControls(context, vmx, vmy);
        matrices.pop();
    }

    private void renderZoomControls(DrawContext context, int mouseX, int mouseY) {
        MatrixStack m = GuiCompat.stack(context);
        this.zBtnSize = 18.0f;
        this.zBtnY = this.winY + this.winH - this.zBtnSize - 8.0f;
        this.zPlusX = this.winX + this.winW - this.zBtnSize - 10.0f;
        this.zMinusX = this.zPlusX - this.zBtnSize - 5.0f;
        float labelW = 40.0f;
        float bgX = this.zMinusX - labelW - 6.0f;
        rectangle.render(ShapeProperties.create(m, bgX - 6.0f, this.zBtnY - 5.0f, this.zPlusX + this.zBtnSize - (bgX - 6.0f) + 6.0f, this.zBtnSize + 10.0f).round(8.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
        int pct = Math.round(uiScale / 3.0f * 100.0f);
        Fonts.getSize(12, Fonts.Type.SEMI).drawCenteredString(m, pct + "%", bgX + labelW / 2.0f, this.zBtnY + this.zBtnSize / 2.0f - 2.5f, C_TEXT.getRGB());
        boolean hMinus = (float)mouseX >= this.zMinusX && (float)mouseX <= this.zMinusX + this.zBtnSize && (float)mouseY >= this.zBtnY && (float)mouseY <= this.zBtnY + this.zBtnSize;
        boolean hPlus = (float)mouseX >= this.zPlusX && (float)mouseX <= this.zPlusX + this.zBtnSize && (float)mouseY >= this.zBtnY && (float)mouseY <= this.zBtnY + this.zBtnSize;
        rectangle.render(ShapeProperties.create(m, this.zMinusX, this.zBtnY, this.zBtnSize, this.zBtnSize).round(5.0f).color((hMinus ? C_CARD_HOVER : C_TRACK).getRGB()).build());
        rectangle.render(ShapeProperties.create(m, this.zPlusX, this.zBtnY, this.zBtnSize, this.zBtnSize).round(5.0f).color((hPlus ? C_ACCENT : C_TRACK).getRGB()).build());
        Fonts.getSize(16, Fonts.Type.BOLD).drawCenteredString(m, "-", this.zMinusX + this.zBtnSize / 2.0f, this.zBtnY + this.zBtnSize / 2.0f - 4.5f, C_TEXT.getRGB());
        Fonts.getSize(15, Fonts.Type.BOLD).drawCenteredString(m, "+", this.zPlusX + this.zBtnSize / 2.0f, this.zBtnY + this.zBtnSize / 2.0f - 4.0f, C_TEXT.getRGB());
    }

    private void renderHeader(DrawContext context, int mouseX, int mouseY) {
        boolean caret;
        MatrixStack m = GuiCompat.stack(context);
        FontRenderer title = Fonts.getSize(18, Fonts.Type.BOLD);
        title.drawString(m, "\u041f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u044b", this.winX + 14.0f, this.winY + this.headerH / 2.0f - 1.0f, C_TEXT.getRGB());
        this.searchW = Math.min(360.0f, this.winW * 0.42f);
        this.searchH = 17.0f;
        this.searchX = this.winX + this.winW / 2.0f - this.searchW / 2.0f;
        this.searchY = this.winY + this.headerH / 2.0f - this.searchH / 2.0f;
        rectangle.render(ShapeProperties.create(m, this.searchX, this.searchY, this.searchW, this.searchH).round(5.0f).thickness(1.0f).outlineColor((this.searchFocused ? C_ACCENT : C_OUTLINE).getRGB()).color(C_SEARCH.getRGB()).build());
        FontRenderer sf = Fonts.getSize(13, Fonts.Type.DEFAULT);
        boolean bl = caret = this.searchFocused && System.currentTimeMillis() / 500L % 2L == 0L;
        if (this.search.isEmpty() && !this.searchFocused) {
            sf.drawString(m, "\u041d\u0430\u0439\u0442\u0438 \u043f\u0430\u0440\u0430\u043c\u0435\u0442\u0440", this.searchX + 8.0f, this.searchY + this.searchH / 2.0f, C_SUBTEXT.getRGB());
        } else {
            sf.drawString(m, this.search + (caret ? "|" : ""), this.searchX + 8.0f, this.searchY + this.searchH / 2.0f, C_TEXT.getRGB());
        }
        float dotX = this.winX + this.winW - 18.0f;
        float dotY = this.winY + this.headerH / 2.0f;
        boolean closeHover = this.dist(mouseX, mouseY, dotX, dotY) < 9.0f;
        rectangle.render(ShapeProperties.create(m, dotX - 7.0f, dotY - 7.0f, 14.0, 14.0).round(4.0f).color((closeHover ? C_CARD_HOVER : C_HEADER).getRGB()).build());
        Fonts.getSize(15, Fonts.Type.BOLD).drawCenteredString(m, "x", dotX, dotY - 4.0f, C_SUBTEXT.getRGB());
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack m = GuiCompat.stack(context);
        float pad = 10.0f;
        float x = this.sidebarX + pad;
        float w = this.sidebarW - pad * 2.0f;
        float cardY = this.contentY + pad;
        float cardH = 40.0f;
        rectangle.render(ShapeProperties.create(m, x, cardY, w, cardH).round(7.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
        String user = this.safeUsername();
        float avSize = 26.0f;
        float avX = x + 7.0f;
        float avY = cardY + (cardH - avSize) / 2.0f;
        rectangle.render(ShapeProperties.create(m, avX, avY, avSize, avSize).round(13.0f).color(C_ACCENT.getRGB()).build());
        Fonts.getSize(18, Fonts.Type.BOLD).drawCenteredString(m, user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase(), avX + avSize / 2.0f, avY + avSize / 2.0f - 5.0f, Color.WHITE.getRGB());
        Fonts.getSize(14, Fonts.Type.SEMI).drawString(m, this.trim(user, Fonts.getSize(14, Fonts.Type.SEMI), w - avSize - 18.0f), avX + avSize + 6.0f, cardY + 11.0f, C_TEXT.getRGB());
        Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u043f\u0440\u043e\u0444\u0438\u043b\u044c", avX + avSize + 6.0f, cardY + 24.0f, C_SUBTEXT.getRGB());
        float navY = cardY + cardH + 8.0f;
        float itemH = 24.0f;
        float gap = 3.0f;
        for (int i = 0; i < NAV.length; ++i) {
            float iy = navY + (float)i * (itemH + gap);
            boolean hover = (float)mouseX >= x && (float)mouseX <= x + w && (float)mouseY >= iy && (float)mouseY <= iy + itemH;
            int n = i;
            this.navHover[n] = this.navHover[n] + ((hover || i == this.selected ? 1.0f : 0.0f) - this.navHover[i]) * 0.25f;
            float h = this.navHover[i];
            if (i == this.selected) {
                rectangle.render(ShapeProperties.create(m, x, iy, w, itemH).round(6.0f).color(C_NAV_SEL.getRGB()).build());
                rectangle.render(ShapeProperties.create(m, x + 2.0f, iy + 5.0f, 3.0, itemH - 10.0f).round(1.5f).color(C_ACCENT.getRGB()).build());
            } else if (h > 0.01f) {
                int a = (int)(28.0f * h);
                rectangle.render(ShapeProperties.create(m, x, iy, w, itemH).round(6.0f).color(new Color(255, 255, 255, a).getRGB()).build());
            }
            Color icol = i == this.selected ? C_ACCENT : new Color(110, 110, 122, 255);
            rectangle.render(ShapeProperties.create(m, x + 11.0f, iy + itemH / 2.0f - 5.0f, 10.0, 10.0).round(3.0f).thickness(1.5f).outlineColor(icol.getRGB()).color(new Color(0, 0, 0, 0).getRGB()).build());
            int tcol = i == this.selected ? C_TEXT.getRGB() : C_SUBTEXT.getRGB();
            Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, NAV[i].title(), x + 28.0f, iy + itemH / 2.0f - 1.0f, tcol);
        }
    }

    private void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        float used;
        MatrixStack m = GuiCompat.stack(context);
        float pad = 16.0f;
        float cx = this.contentX + pad;
        float cw = this.contentW - pad * 2.0f;
        int sx1 = (int)this.contentX;
        int sy1 = (int)this.contentY;
        int sx2 = (int)(this.contentX + this.contentW);
        int sy2 = (int)(this.contentY + this.contentH);
        float k = this.lastScale / this.lastGuiScale;
        context.enableScissor(Math.round((float)sx1 * k), Math.round((float)sy1 * k), Math.round((float)sx2 * k), Math.round((float)sy2 * k));
        float y = this.contentY + 14.0f - this.scroll;
        boolean searching = !this.search.isBlank();
        String id = NAV[this.selected].id();
        Fonts.getSize(26, Fonts.Type.BOLD).drawString(m, searching ? "\u0420\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u044b \u043f\u043e\u0438\u0441\u043a\u0430" : NAV[this.selected].title(), cx, y, C_TEXT.getRGB());
        float startY = y += 30.0f;
        if (searching) {
            y = this.renderDisplay(context, cx, cw, y, mouseX, mouseY);
            y = this.renderGraphics(context, cx, cw, y, mouseX, mouseY);
            y = this.renderSound(context, cx, cw, y, mouseX, mouseY);
            y = this.renderControls(context, cx, cw, y, mouseX, mouseY);
            y = this.renderChat(context, cx, cw, y, mouseX, mouseY);
            y = this.renderAppearance(context, cx, cw, y, mouseX, mouseY);
            y = this.renderAccessibility(context, cx, cw, y, mouseX, mouseY);
            if (this.sliders.isEmpty() && this.toggles.isEmpty() && this.cycles.isEmpty()) {
                Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e \u043f\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u0443 \"" + this.search + "\"", cx, y, C_SUBTEXT.getRGB());
                y += 20.0f;
            }
        } else {
            switch (id) {
                case "home": {
                    y = this.renderHome(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "display": {
                    y = this.renderDisplay(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "graphics": {
                    y = this.renderGraphics(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "sound": {
                    y = this.renderSound(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "controls": {
                    y = this.renderControls(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "keys": {
                    y = this.renderKeys(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "chat": {
                    y = this.renderChat(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "appearance": {
                    y = this.renderAppearance(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "language": {
                    y = this.renderLanguage(context, cx, cw, y, mouseX, mouseY);
                    break;
                }
                case "accessibility": {
                    y = this.renderAccessibility(context, cx, cw, y, mouseX, mouseY);
                }
            }
        }
        context.disableScissor();
        this.contentVirtualHeight = used = y + this.scroll - startY + 40.0f;
        float maxScroll = Math.max(0.0f, used - (this.contentH - (startY - this.contentY)));
        this.scrollTarget = WindowsSettingsScreen.clamp(this.scrollTarget, 0.0f, maxScroll);
    }

    private float renderHome(DrawContext context, float cx, float cw, float y, int mouseX, int mouseY) {
        MatrixStack m = GuiCompat.stack(context);
        String user = this.safeUsername();
        float cardH = 78.0f;
        rectangle.render(ShapeProperties.create(m, cx, y, cw, cardH).round(9.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
        float avSize = 54.0f;
        float avX = cx + 12.0f;
        float avY = y + (cardH - avSize) / 2.0f;
        rectangle.render(ShapeProperties.create(m, avX, avY, avSize, avSize).round(10.0f).color(C_ACCENT.getRGB()).build());
        Fonts.getSize(34, Fonts.Type.BOLD).drawCenteredString(m, user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase(), avX + avSize / 2.0f, avY + avSize / 2.0f - 9.0f, Color.WHITE.getRGB());
        Fonts.getSize(20, Fonts.Type.BOLD).drawString(m, user, avX + avSize + 14.0f, y + 16.0f, C_TEXT.getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(m, "WiosVisuals Client dev", avX + avSize + 14.0f, y + 34.0f, C_SUBTEXT.getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(m, "Minecraft " + mc.getGameVersion(), avX + avSize + 14.0f, y + 48.0f, C_SUBTEXT.getRGB());
        float statX = cx + cw - 190.0f;
        this.drawStatus(m, statX, y + 14.0f, "Baritone", "\u0410\u043a\u0442\u0438\u0432\u0435\u043d");
        this.drawStatus(m, statX + 100.0f, y + 14.0f, "Fabric", "API \u0430\u043a\u0442\u0438\u0432\u0435\u043d");
        this.drawStatus(m, statX, y + 44.0f, "\u0421\u0435\u0441\u0441\u0438\u044f", this.trim(user, Fonts.getSize(11, Fonts.Type.DEFAULT), 80.0f));
        this.drawStatus(m, statX + 100.0f, y + 44.0f, "Java", System.getProperty("java.version", "?"));
        float colGap = 14.0f;
        float colW = (cw - colGap) / 2.0f;
        Fonts.getSize(16, Fonts.Type.SEMI).drawString(m, "\u0420\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0443\u0435\u043c\u044b\u0435 \u043f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u044b", cx, y += cardH + 18.0f, C_TEXT.getRGB());
        Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "\u0427\u0430\u0441\u0442\u043e \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u043c\u044b\u0435 \u0440\u0430\u0437\u0434\u0435\u043b\u044b", cx, y + 14.0f, C_SUBTEXT.getRGB());
        Fonts.getSize(16, Fonts.Type.SEMI).drawString(m, "\u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f", cx + colW + colGap, y, C_TEXT.getRGB());
        Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "\u0421\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430", cx + colW + colGap, y + 14.0f, C_SUBTEXT.getRGB());
        float rowY = y + 24.0f;
        String[][] recs = new String[][]{{"display", "\u0414\u0438\u0441\u043f\u043b\u0435\u0439", "\u0420\u0430\u0437\u0440\u0435\u0448\u0435\u043d\u0438\u0435, FOV, GUI"}, {"sound", "\u0417\u0432\u0443\u043a", "\u0413\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c \u0438 \u043a\u0430\u043d\u0430\u043b\u044b"}, {"controls", "\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435", "\u0427\u0443\u0432\u0441\u0442\u0432\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u043c\u044b\u0448\u0438"}, {"graphics", "\u0413\u0440\u0430\u0444\u0438\u043a\u0430", "\u0414\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c, FPS, \u0433\u0440\u0430\u0444\u0438\u043a\u0430"}, {"language", "\u042f\u0437\u044b\u043a", "\u042f\u0437\u044b\u043a \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430"}};
        float rh = 32.0f;
        float ry = rowY;
        for (String[] r : recs) {
            boolean hover = (float)mouseX >= cx && (float)mouseX <= cx + colW && (float)mouseY >= ry && (float)mouseY <= ry + rh;
            rectangle.render(ShapeProperties.create(m, cx, ry, colW, rh).round(7.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color((hover ? C_CARD_HOVER : C_CARD).getRGB()).build());
            rectangle.render(ShapeProperties.create(m, cx + 9.0f, ry + rh / 2.0f - 8.0f, 16.0, 16.0).round(4.0f).color(C_ACCENT_DIM.getRGB()).build());
            rectangle.render(ShapeProperties.create(m, cx + 14.0f, ry + rh / 2.0f - 3.0f, 6.0, 6.0).round(2.0f).color(C_ACCENT.getRGB()).build());
            Fonts.getSize(13, Fonts.Type.SEMI).drawString(m, r[1], cx + 32.0f, ry + 9.0f, C_TEXT.getRGB());
            Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(m, r[2], cx + 32.0f, ry + 21.0f, C_SUBTEXT.getRGB());
            String target = r[0];
            this.rows.add(new ClickRow(cx, ry, colW, rh, () -> this.navigateTo(target)));
            ry += rh + 6.0f;
        }
        float infoX = cx + colW + colGap;
        float infoH = ry - rowY - 6.0f;
        rectangle.render(ShapeProperties.create(m, infoX, rowY, colW, infoH).round(7.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
        Fonts.getSize(15, Fonts.Type.SEMI).drawString(m, "Wiosvisual Client", infoX + 12.0f, rowY + 12.0f, C_TEXT.getRGB());
        String[] info = new String[]{"\u0412\u0435\u0440\u0441\u0438\u044f: dev", "Minecraft: " + mc.getGameVersion(), "Java: " + System.getProperty("java.version", "?"), "\u041e\u0421: " + System.getProperty("os.name", "?")};
        float iy = rowY + 28.0f;
        for (String s : info) {
            Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, s, infoX + 12.0f, iy, C_SUBTEXT.getRGB());
            iy += 14.0f;
        }
        return Math.max(ry, rowY + infoH) + 10.0f;
    }

    private void drawStatus(MatrixStack m, float x, float y, String title, String value) {
        Fonts.getSize(12, Fonts.Type.SEMI).drawString(m, title, x, y, C_TEXT.getRGB());
        Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(m, value, x, y + 12.0f, C_SUBTEXT.getRGB());
    }

    private float renderDisplay(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u041f\u043e\u043b\u0435 \u0437\u0440\u0435\u043d\u0438\u044f (FOV)", 30.0, 110.0, true, "\u00b0", () -> ((Integer)this.opt().getFov().getValue()).intValue(), v -> this.opt().getFov().setValue((int)v.doubleValue()));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u041c\u0430\u0441\u0448\u0442\u0430\u0431 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430", 0.0, 4.0, true, "", () -> ((Integer)this.opt().getGuiScale().getValue()).intValue(), v -> this.opt().getGuiScale().setValue((int)v.doubleValue()));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u042f\u0440\u043a\u043e\u0441\u0442\u044c", 0.0, 1.0, false, "%", () -> (Double)this.opt().getGamma().getValue(), v -> this.opt().getGamma().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "disp_bob", "\u041f\u043e\u043a\u0430\u0447\u0438\u0432\u0430\u043d\u0438\u0435 \u043a\u0430\u043c\u0435\u0440\u044b", () -> (Boolean)this.opt().getBobView().getValue(), v -> this.opt().getBobView().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "disp_full", "\u041f\u043e\u043b\u043d\u043e\u044d\u043a\u0440\u0430\u043d\u043d\u044b\u0439 \u0440\u0435\u0436\u0438\u043c", () -> window.isFullscreen(), v -> window.toggleFullscreen());
        return y;
    }

    private float renderGraphics(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u0414\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u043f\u0440\u043e\u0440\u0438\u0441\u043e\u0432\u043a\u0438", 2.0, 32.0, true, " \u0447.", () -> ((Integer)this.opt().getViewDistance().getValue()).intValue(), v -> this.opt().getViewDistance().setValue((int)v.doubleValue()));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u041b\u0438\u043c\u0438\u0442 \u043a\u0430\u0434\u0440\u043e\u0432", 10.0, 260.0, true, " FPS", () -> ((Integer)this.opt().getMaxFps().getValue()).intValue(), v -> this.opt().getMaxFps().setValue((int)v.doubleValue()));
        y = this.rowCycle(ctx, cx, cw, y, mx, my, "\u0413\u0440\u0430\u0444\u0438\u043a\u0430", () -> this.graphicsName(net.minecraft.client.option.GraphicsMode.FANCY), () -> {
            GraphicsMode[] vals = GraphicsMode.values();
            GraphicsMode cur = net.minecraft.client.option.GraphicsMode.FANCY;
            this.opt().applyGraphicsMode(vals[(cur.ordinal() + 1) % vals.length]);
        });
        y = this.rowCycle(ctx, cx, cw, y, mx, my, "\u041e\u0431\u043b\u0430\u043a\u0430", () -> this.cloudName((CloudRenderMode)this.opt().getCloudRenderMode().getValue()), () -> {
            CloudRenderMode[] vals = CloudRenderMode.values();
            CloudRenderMode cur = (CloudRenderMode)this.opt().getCloudRenderMode().getValue();
            this.opt().getCloudRenderMode().setValue(vals[(cur.ordinal() + 1) % vals.length]);
        });
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "gfx_vsync", "\u0412\u0435\u0440\u0442\u0438\u043a\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0430\u0446\u0438\u044f", () -> (Boolean)this.opt().getEnableVsync().getValue(), v -> {
            this.opt().getEnableVsync().setValue(v);
            window.setVsync((boolean)v);
        });
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "gfx_shadows", "\u0422\u0435\u043d\u0438 \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0435\u0439", () -> (Boolean)this.opt().getEntityShadows().getValue(), v -> this.opt().getEntityShadows().setValue(v));
        return y;
    }

    private float renderSound(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        Object[][] cats;
        for (Object[] c : cats = new Object[][]{{SoundCategory.MASTER, "\u041e\u0431\u0449\u0430\u044f \u0433\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c"}, {SoundCategory.MUSIC, "\u041c\u0443\u0437\u044b\u043a\u0430"}, {SoundCategory.RECORDS, "\u041f\u0440\u043e\u0438\u0433\u0440\u044b\u0432\u0430\u0442\u0435\u043b\u044c"}, {SoundCategory.BLOCKS, "\u0411\u043b\u043e\u043a\u0438"}, {SoundCategory.HOSTILE, "\u0412\u0440\u0430\u0436\u0434\u0435\u0431\u043d\u044b\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0430"}, {SoundCategory.NEUTRAL, "\u0414\u0440\u0443\u0436\u0435\u043b\u044e\u0431\u043d\u044b\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0430"}, {SoundCategory.PLAYERS, "\u0418\u0433\u0440\u043e\u043a\u0438"}, {SoundCategory.AMBIENT, "\u041e\u043a\u0440\u0443\u0436\u0435\u043d\u0438\u0435"}, {SoundCategory.VOICE, "\u0413\u043e\u043b\u043e\u0441/\u0440\u0435\u0447\u044c"}}) {
            SoundCategory cat = (SoundCategory)c[0];
            String label = (String)c[1];
            y = this.rowSlider(ctx, cx, cw, y, mx, my, label, 0.0, 1.0, false, "%", () -> (Double)this.opt().getSoundVolumeOption(cat).getValue(), v -> this.opt().getSoundVolumeOption(cat).setValue(v));
        }
        return y;
    }

    private float renderControls(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u0427\u0443\u0432\u0441\u0442\u0432\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u043c\u044b\u0448\u0438", 0.0, 1.0, false, "%2x", () -> (Double)this.opt().getMouseSensitivity().getValue(), v -> this.opt().getMouseSensitivity().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "ctl_raw", "\u041d\u0435\u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u043d\u043d\u044b\u0439 \u0432\u0432\u043e\u0434 \u043c\u044b\u0448\u0438", () -> (Boolean)this.opt().getRawMouseInput().getValue(), v -> this.opt().getRawMouseInput().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "ctl_inv", "\u0418\u043d\u0432\u0435\u0440\u0441\u0438\u044f \u043c\u044b\u0448\u0438", () -> (Boolean)this.opt().getInvertMouseY().getValue(), v -> this.opt().getInvertMouseY().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "ctl_jump", "\u0410\u0432\u0442\u043e\u043f\u0440\u044b\u0436\u043e\u043a", () -> (Boolean)this.opt().getAutoJump().getValue(), v -> this.opt().getAutoJump().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "ctl_sneak", "\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0430\u0435\u043c\u043e\u0435 \u043f\u0440\u0438\u0441\u0435\u0434\u0430\u043d\u0438\u0435", () -> (Boolean)this.opt().getSneakToggled().getValue(), v -> this.opt().getSneakToggled().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "ctl_sprint", "\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0430\u0435\u043c\u044b\u0439 \u0431\u0435\u0433", () -> (Boolean)this.opt().getSprintToggled().getValue(), v -> this.opt().getSprintToggled().setValue(v));
        return y;
    }

    private float renderChat(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0447\u0430\u0442\u0430", 0.0, 1.0, false, "%", () -> (Double)this.opt().getChatOpacity().getValue(), v -> this.opt().getChatOpacity().setValue(v));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u041c\u0430\u0441\u0448\u0442\u0430\u0431 \u0447\u0430\u0442\u0430", 0.0, 1.0, false, "%", () -> (Double)this.opt().getChatScale().getValue(), v -> this.opt().getChatScale().setValue(v));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0444\u043e\u043d\u0430 \u0442\u0435\u043a\u0441\u0442\u0430", 0.0, 1.0, false, "%", () -> (Double)this.opt().getTextBackgroundOpacity().getValue(), v -> this.opt().getTextBackgroundOpacity().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "chat_subs", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b", () -> (Boolean)this.opt().getShowSubtitles().getValue(), v -> this.opt().getShowSubtitles().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "chat_colors", "\u0426\u0432\u0435\u0442\u0430 \u0432 \u0447\u0430\u0442\u0435", () -> (Boolean)this.opt().getChatColors().getValue(), v -> this.opt().getChatColors().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "chat_links", "\u0421\u0441\u044b\u043b\u043a\u0438 \u0432 \u0447\u0430\u0442\u0435", () -> (Boolean)this.opt().getChatLinks().getValue(), v -> this.opt().getChatLinks().setValue(v));
        return y;
    }

    private float renderAppearance(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        for (PlayerModelPart part : PlayerModelPart.values()) {
            String label = part.getOptionName().getString();
            y = this.rowToggle(ctx, cx, cw, y, mx, my, "app_" + part.name(), label, () -> this.opt().isPlayerModelPartEnabled(part), v -> this.opt().setPlayerModelPart(part, v.booleanValue()));
        }
        return y;
    }

    private float renderAccessibility(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "acc_contrast", "\u0412\u044b\u0441\u043e\u043a\u0430\u044f \u043a\u043e\u043d\u0442\u0440\u0430\u0441\u0442\u043d\u043e\u0441\u0442\u044c", () -> (Boolean)this.opt().getHighContrast().getValue(), v -> this.opt().getHighContrast().setValue(v));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u0421\u0438\u043b\u0430 \u0442\u0440\u044f\u0441\u043a\u0438 \u043e\u0442 \u0443\u0440\u043e\u043d\u0430", 0.0, 1.0, false, "%", () -> (Double)this.opt().getDamageTiltStrength().getValue(), v -> this.opt().getDamageTiltStrength().setValue(v));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u0418\u0441\u043a\u0430\u0436\u0435\u043d\u0438\u0435 \u044d\u0444\u0444\u0435\u043a\u0442\u043e\u0432", 0.0, 1.0, false, "%", () -> (Double)this.opt().getDistortionEffectScale().getValue(), v -> this.opt().getDistortionEffectScale().setValue(v));
        y = this.rowSlider(ctx, cx, cw, y, mx, my, "\u042d\u0444\u0444\u0435\u043a\u0442 FOV", 0.0, 1.0, false, "%", () -> (Double)this.opt().getFovEffectScale().getValue(), v -> this.opt().getFovEffectScale().setValue(v));
        y = this.rowToggle(ctx, cx, cw, y, mx, my, "acc_mono", "\u041c\u043e\u043d\u043e\u0445\u0440\u043e\u043c\u043d\u044b\u0439 \u043b\u043e\u0433\u043e\u0442\u0438\u043f", () -> (Boolean)this.opt().getMonochromeLogo().getValue(), v -> this.opt().getMonochromeLogo().setValue(v));
        return y;
    }

    private float renderLanguage(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        MatrixStack m = GuiCompat.stack(ctx);
        LanguageManager lm = mc.getLanguageManager();
        String current = lm.getLanguage();
        SortedMap all = lm.getAllLanguages();
        float rh = 26.0f;
        for (Map.Entry e : (Iterable<Map.Entry>)all.entrySet()) {
            String code = (String)e.getKey();
            LanguageDefinition def = (LanguageDefinition)e.getValue();
            boolean sel = code.equals(current);
            boolean hover = (float)mx >= cx && (float)mx <= cx + cw && (float)my >= y && (float)my <= y + rh;
            rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(6.0f).thickness(1.0f).outlineColor((sel ? C_ACCENT : C_OUTLINE).getRGB()).color((sel ? C_NAV_SEL : (hover ? C_CARD_HOVER : C_CARD)).getRGB()).build());
            Fonts.getSize(13, Fonts.Type.SEMI).drawString(m, def.getDisplayText().getString(), cx + 12.0f, y + rh / 2.0f - 1.0f, C_TEXT.getRGB());
            Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(m, code, cx + cw - Fonts.getSize(10, Fonts.Type.DEFAULT).getStringWidth(code) - 12.0f, y + rh / 2.0f, C_SUBTEXT.getRGB());
            String target = code;
            this.rows.add(new ClickRow(cx, y, cw, rh, () -> this.applyLanguage(target)));
            y += rh + 5.0f;
        }
        return y;
    }

    private float renderKeys(DrawContext ctx, float cx, float cw, float y, int mx, int my) {
        MatrixStack m = GuiCompat.stack(ctx);
        Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(m, "\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043d\u0430 \u043a\u043b\u0430\u0432\u0438\u0448\u0443 \u0441\u043f\u0440\u0430\u0432\u0430 \u0438 \u0437\u0430\u0434\u0430\u0439\u0442\u0435 \u043d\u043e\u0432\u0443\u044e. ESC \u2014 \u0441\u0431\u0440\u043e\u0441\u0438\u0442\u044c.", cx, y, C_SUBTEXT.getRGB());
        y += 18.0f;
        float rh = 26.0f;
        for (KeyBinding kb : this.opt().allKeys) {
            boolean hover;
            boolean listening = this.listeningBind == kb;
            rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(6.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
            Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(m, Text.translatable((String)kb.getId()).getString(), cx + 12.0f, y + rh / 2.0f - 1.0f, C_TEXT.getRGB());
            float bw = 86.0f;
            float bh = 17.0f;
            float bx = cx + cw - bw - 10.0f;
            float by = y + (rh - bh) / 2.0f;
            boolean bl = hover = (float)mx >= bx && (float)mx <= bx + bw && (float)my >= by && (float)my <= by + bh;
            int bc = listening ? C_ACCENT.getRGB() : (kb.isDefault() ? C_TRACK.getRGB() : new Color(80, 70, 110, 255).getRGB());
            rectangle.render(ShapeProperties.create(m, bx, by, bw, bh).round(5.0f).color((hover && !listening ? C_CARD_HOVER : new Color(bc, true)).getRGB()).build());
            String txt = listening ? "> ... <" : kb.getBoundKeyLocalizedText().getString();
            Fonts.getSize(11, Fonts.Type.SEMI).drawCenteredString(m, txt, bx + bw / 2.0f, by + bh / 2.0f - 2.5f, C_TEXT.getRGB());
            KeyBinding target = kb;
            this.rows.add(new ClickRow(bx, by, bw, bh, () -> {
                this.listeningBind = this.listeningBind == target ? null : target;
            }));
            y += rh + 5.0f;
        }
        return y;
    }

    private float rowSlider(DrawContext ctx, float cx, float cw, float y, int mx, int my, String label, double min, double max, boolean integer, String unit, DoubleSupplier get, Consumer<Double> set) {
        Object valueText;
        if (!this.matchesSearch(label)) {
            return y;
        }
        MatrixStack m = GuiCompat.stack(ctx);
        float rh = 34.0f;
        rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(7.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
        Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, label, cx + 12.0f, y + rh / 2.0f - 1.0f, C_TEXT.getRGB());
        double value = WindowsSettingsScreen.clampD(get.getAsDouble(), min, max);
        float trackW = Math.min(150.0f, cw * 0.38f);
        float trackX = cx + cw - trackW - 16.0f;
        float trackY = y + rh / 2.0f;
        float t = (float)((value - min) / (max - min));
        rectangle.render(ShapeProperties.create(m, trackX, trackY - 1.5f, trackW, 3.0).round(1.5f).color(C_TRACK.getRGB()).build());
        rectangle.render(ShapeProperties.create(m, trackX, trackY - 1.5f, trackW * t, 3.0).round(1.5f).color(C_ACCENT.getRGB()).build());
        float knobX = trackX + trackW * t;
        rectangle.render(ShapeProperties.create(m, knobX - 4.5f, trackY - 4.5f, 9.0, 9.0).round(4.5f).color(Color.WHITE.getRGB()).build());
        if (integer) {
            int iv = (int)Math.round(value);
            valueText = " FPS".equals(unit) && iv >= 260 ? "\u0411\u0435\u0437 \u043b\u0438\u043c\u0438\u0442\u0430" : ("".equals(unit) && label.startsWith("\u041c\u0430\u0441\u0448\u0442\u0430\u0431") && iv == 0 ? "\u0410\u0432\u0442\u043e" : iv + unit);
        } else {
            double mul = "%2x".equals(unit) ? 200.0 : 100.0;
            valueText = Math.round(value * mul) + "%";
        }
        FontRenderer vf = Fonts.getSize(11, Fonts.Type.SEMI);
        vf.drawString(m, (String)valueText, trackX - vf.getStringWidth((String)valueText) - 8.0f, trackY - 1.0f, C_SUBTEXT.getRGB());
        this.sliders.add(new Slider(trackX, y, trackW, rh, min, max, integer, set));
        return y + rh + 6.0f;
    }

    private float rowToggle(DrawContext ctx, float cx, float cw, float y, int mx, int my, String id, String label, BooleanSupplier get, Consumer<Boolean> set) {
        if (!this.matchesSearch(label)) {
            return y;
        }
        MatrixStack m = GuiCompat.stack(ctx);
        float rh = 30.0f;
        boolean hover = (float)mx >= cx && (float)mx <= cx + cw && (float)my >= y && (float)my <= y + rh;
        rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(7.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color((hover ? C_CARD_HOVER : C_CARD).getRGB()).build());
        Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, label, cx + 12.0f, y + rh / 2.0f - 1.0f, C_TEXT.getRGB());
        boolean on = get.getAsBoolean();
        float pillW = 30.0f;
        float pillH = 15.0f;
        float pillX = cx + cw - pillW - 14.0f;
        float pillY = y + (rh - pillH) / 2.0f;
        float a = this.toggleAnim.getOrDefault(id, Float.valueOf(on ? 1.0f : 0.0f)).floatValue();
        a += ((on ? 1.0f : 0.0f) - a) * 0.3f;
        this.toggleAnim.put(id, Float.valueOf(a));
        Color track = WindowsSettingsScreen.lerpColor(C_TRACK, C_ACCENT, a);
        rectangle.render(ShapeProperties.create(m, pillX, pillY, pillW, pillH).round(pillH / 2.0f).color(track.getRGB()).build());
        float knobD = pillH - 4.0f;
        float knobX = pillX + 2.0f + a * (pillW - knobD - 4.0f);
        rectangle.render(ShapeProperties.create(m, knobX, pillY + 2.0f, knobD, knobD).round(knobD / 2.0f).color(Color.WHITE.getRGB()).build());
        this.toggles.add(new Toggle(cx, y, cw, rh, get, set));
        return y + rh + 6.0f;
    }

    private float rowCycle(DrawContext ctx, float cx, float cw, float y, int mx, int my, String label, Supplier<String> valueText, Runnable next) {
        if (!this.matchesSearch(label)) {
            return y;
        }
        MatrixStack m = GuiCompat.stack(ctx);
        float rh = 30.0f;
        rectangle.render(ShapeProperties.create(m, cx, y, cw, rh).round(7.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color(C_CARD.getRGB()).build());
        Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(m, label, cx + 12.0f, y + rh / 2.0f - 1.0f, C_TEXT.getRGB());
        String val = valueText.get();
        FontRenderer bf = Fonts.getSize(11, Fonts.Type.SEMI);
        float bw = Math.max(70.0f, bf.getStringWidth(val) + 22.0f);
        float bh = 17.0f;
        float bx = cx + cw - bw - 14.0f;
        float by = y + (rh - bh) / 2.0f;
        boolean hover = (float)mx >= bx && (float)mx <= bx + bw && (float)my >= by && (float)my <= by + bh;
        rectangle.render(ShapeProperties.create(m, bx, by, bw, bh).round(5.0f).thickness(1.0f).outlineColor(C_OUTLINE.getRGB()).color((hover ? C_CARD_HOVER : C_TRACK).getRGB()).build());
        bf.drawCenteredString(m, val, bx + bw / 2.0f, by + bh / 2.0f - 2.5f, C_TEXT.getRGB());
        this.cycles.add(new Cycle(bx, by, bw, bh, next));
        return y + rh + 6.0f;
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        double vx = mouseX * (double)this.lastGuiScale / (double)this.lastScale;
        double vy = mouseY * (double)this.lastGuiScale / (double)this.lastScale;
        if (button == 0) {
            if (WindowsSettingsScreen.inside(vx, vy, this.zMinusX, this.zBtnY, this.zBtnSize, this.zBtnSize)) {
                this.changeScale(-0.3f);
                return true;
            }
            if (WindowsSettingsScreen.inside(vx, vy, this.zPlusX, this.zBtnY, this.zBtnSize, this.zBtnSize)) {
                this.changeScale(0.3f);
                return true;
            }
            if (WindowsSettingsScreen.inside(vx, vy, this.searchX, this.searchY, this.searchW, this.searchH)) {
                this.searchFocused = true;
                return true;
            }
            this.searchFocused = false;
            float dotX = this.winX + this.winW - 18.0f;
            float dotY = this.winY + this.headerH / 2.0f;
            if (this.dist((int)vx, (int)vy, dotX, dotY) < 9.0f) {
                this.close();
                return true;
            }
            float pad = 10.0f;
            float x = this.sidebarX + pad;
            float w = this.sidebarW - pad * 2.0f;
            float navY = this.contentY + pad + 40.0f + 8.0f;
            float itemH = 24.0f;
            float gap = 3.0f;
            for (int i = 0; i < NAV.length; ++i) {
                float iy = navY + (float)i * (itemH + gap);
                if (!(vx >= (double)x) || !(vx <= (double)(x + w)) || !(vy >= (double)iy) || !(vy <= (double)(iy + itemH))) continue;
                this.selected = i;
                this.search = "";
                this.searchFocused = false;
                this.scroll = 0.0f;
                this.scrollTarget = 0.0f;
                return true;
            }
            for (ClickRow r : this.rows) {
                if (!r.contains(vx, vy)) continue;
                r.action.run();
                return true;
            }
            for (Toggle tg : this.toggles) {
                if (!tg.contains(vx, vy)) continue;
                tg.set.accept(!tg.get.getAsBoolean());
                this.saveOptions();
                return true;
            }
            for (Cycle c : this.cycles) {
                if (!c.contains(vx, vy)) continue;
                c.next.run();
                this.saveOptions();
                return true;
            }
            for (Slider s : this.sliders) {
                if (!s.contains(vx, vy)) continue;
                this.draggingSlider = s;
                this.applySlider(s, vx);
                return true;
            }
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    public boolean mouseDragged(net.minecraft.client.gui.Click wiosClick, double dx, double dy) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.draggingSlider != null) {
            this.applySlider(this.draggingSlider, mouseX * (double)this.lastGuiScale / (double)this.lastScale);
            return true;
        }
        return super.mouseDragged(wiosClick, dx, dy);
    }

    public boolean mouseReleased(net.minecraft.client.gui.Click wiosClick) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.draggingSlider != null) {
            this.draggingSlider = null;
            this.saveOptions();
            return true;
        }
        return super.mouseReleased(wiosClick);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double vx = mouseX * (double)this.lastGuiScale / (double)this.lastScale;
        if (vx >= (double)this.contentX && vx <= (double)(this.contentX + this.contentW)) {
            this.scrollTarget -= (float)vertical * 24.0f;
        }
        return true;
    }

    public boolean charTyped(net.minecraft.client.input.CharInput wiosChar) {
        char chr = (char) wiosChar.codepoint();
        int modifiers = wiosChar.modifiers();
        if (this.searchFocused && chr >= ' ' && this.search.length() < 48) {
            this.search = this.search + chr;
            this.scroll = 0.0f;
            this.scrollTarget = 0.0f;
            return true;
        }
        return super.charTyped(wiosChar);
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        if (this.listeningBind != null) {
            if (keyCode == 256) {
                this.listeningBind.setBoundKey(InputUtil.UNKNOWN_KEY);
            } else {
                this.listeningBind.setBoundKey(InputUtil.fromKeyCode(new net.minecraft.client.input.KeyInput(keyCode, scanCode, 0)));
            }
            KeyBinding.updateKeysByCode();
            this.listeningBind = null;
            this.saveOptions();
            return true;
        }
        if (this.searchFocused) {
            if (keyCode == 256) {
                if (!this.search.isEmpty()) {
                    this.search = "";
                } else {
                    this.searchFocused = false;
                }
                return true;
            }
            if (keyCode == 259) {
                if (!this.search.isEmpty()) {
                    this.search = this.search.substring(0, this.search.length() - 1);
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                this.searchFocused = false;
                return true;
            }
            return true;
        }
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(wiosKey);
    }

    private void changeScale(float delta) {
        uiScale = WindowsSettingsScreen.clamp(uiScale + delta, 1.5f, 4.8f);
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
    }

    private boolean matchesSearch(String label) {
        if (this.search == null || this.search.isBlank()) {
            return true;
        }
        return label.toLowerCase().contains(this.search.toLowerCase());
    }

    private void applySlider(Slider s, double mouseX) {
        double t = WindowsSettingsScreen.clampD((mouseX - (double)s.x) / (double)s.w, 0.0, 1.0);
        double val = s.min + t * (s.max - s.min);
        if (s.integer) {
            val = Math.round(val);
        }
        s.set.accept(val);
    }

    private GameOptions opt() {
        return WindowsSettingsScreen.mc.options;
    }

    private void saveOptions() {
        try {
            this.opt().write();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void navigateTo(String id) {
        for (int i = 0; i < NAV.length; ++i) {
            if (!NAV[i].id().equals(id)) continue;
            this.selected = i;
            this.scroll = 0.0f;
            this.scrollTarget = 0.0f;
            return;
        }
    }

    private void applyLanguage(String code) {
        LanguageManager lm = mc.getLanguageManager();
        if (code.equals(lm.getLanguage())) {
            return;
        }
        lm.setLanguage(code);
        this.opt().language = code;
        mc.reloadResources();
        this.saveOptions();
    }

    private String graphicsName(GraphicsMode mode) {
        return switch (mode) {
            default -> throw new MatchException(null, null);
            case GraphicsMode.FAST -> "\u0411\u044b\u0441\u0442\u0440\u0430\u044f";
            case GraphicsMode.FANCY -> "\u0414\u0435\u0442\u0430\u043b\u044c\u043d\u0430\u044f";
            case GraphicsMode.FABULOUS -> "\u041f\u0440\u0435\u0432\u043e\u0441\u0445\u043e\u0434\u043d\u0430\u044f";
        };
    }

    private String cloudName(CloudRenderMode mode) {
        return switch (mode) {
            default -> throw new MatchException(null, null);
            case CloudRenderMode.OFF -> "\u0412\u044b\u043a\u043b";
            case CloudRenderMode.FAST -> "\u0411\u044b\u0441\u0442\u0440\u044b\u0435";
            case CloudRenderMode.FANCY -> "\u0414\u0435\u0442\u0430\u043b\u044c\u043d\u044b\u0435";
        };
    }

    private String safeUsername() {
        try {
            String u = mc.getSession().getUsername();
            return u == null ? "Player" : u;
        }
        catch (Exception e) {
            return "Player";
        }
    }

    private String trim(String s, FontRenderer font, float maxWidth) {
        if (s == null) {
            return "";
        }
        if (font.getStringWidth(s) <= maxWidth) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (font.getStringWidth(sb.toString() + c + "..") > maxWidth) break;
            sb.append(c);
        }
        return String.valueOf(sb) + "..";
    }

    private float dist(int mx, int my, float x, float y) {
        return (float)Math.hypot((float)mx - x, (float)my - y);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    private static double clampD(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = t < 0.0f ? 0.0f : Math.min(t, 1.0f);
        return new Color((int)((float)a.getRed() + (float)(b.getRed() - a.getRed()) * t), (int)((float)a.getGreen() + (float)(b.getGreen() - a.getGreen()) * t), (int)((float)a.getBlue() + (float)(b.getBlue() - a.getBlue()) * t), (int)((float)a.getAlpha() + (float)(b.getAlpha() - a.getAlpha()) * t));
    }

    private record NavItem(String id, String title) {
    }

    private static final class Slider {
        final float x;
        final float y;
        final float w;
        final float h;
        final double min;
        final double max;
        final boolean integer;
        final Consumer<Double> set;

        Slider(float x, float y, float w, float h, double min, double max, boolean integer, Consumer<Double> set) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.set = set;
        }

        boolean contains(double mx, double my) {
            return mx >= (double)this.x && mx <= (double)(this.x + this.w) && my >= (double)this.y && my <= (double)(this.y + this.h);
        }
    }

    private static final class ClickRow {
        final float x;
        final float y;
        final float w;
        final float h;
        final Runnable action;

        ClickRow(float x, float y, float w, float h, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean contains(double mx, double my) {
            return mx >= (double)this.x && mx <= (double)(this.x + this.w) && my >= (double)this.y && my <= (double)(this.y + this.h);
        }
    }

    private static final class Toggle {
        final float x;
        final float y;
        final float w;
        final float h;
        final BooleanSupplier get;
        final Consumer<Boolean> set;

        Toggle(float x, float y, float w, float h, BooleanSupplier get, Consumer<Boolean> set) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.get = get;
            this.set = set;
        }

        boolean contains(double mx, double my) {
            return mx >= (double)this.x && mx <= (double)(this.x + this.w) && my >= (double)this.y && my <= (double)(this.y + this.h);
        }
    }

    private static final class Cycle {
        final float x;
        final float y;
        final float w;
        final float h;
        final Runnable next;

        Cycle(float x, float y, float w, float h, Runnable next) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.next = next;
        }

        boolean contains(double mx, double my) {
            return mx >= (double)this.x && mx <= (double)(this.x + this.w) && my >= (double)this.y && my <= (double)(this.y + this.h);
        }
    }
}
