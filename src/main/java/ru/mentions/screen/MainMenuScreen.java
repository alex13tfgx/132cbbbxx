package ru.mentions.screen;

import ru.mentions.utils.animation.Animation;
import ru.mentions.utils.animation.Easings;
import ru.mentions.utils.annotation.IMinecraft;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.math.MathUtil;
import ru.mentions.utils.render.*;
import ru.mentions.utils.render.font.Font;
import ru.mentions.utils.render.font.Fonts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen extends Screen implements IMinecraft {

    public MainMenuScreen() {
        super(Text.literal("MainMenuScreen"));
    }

    public Animation alpha = new Animation();
    private final List<MenuButton> buttons = new ArrayList<>();
    private MenuButton multiplayerButton;
    private MenuButton singleplayerButton;
    private MenuButton settingsButton;
    private MenuButton accountsButton;
    private MenuButton modsButton;
    private MenuButton modificationsButton;
    private MenuButton exitButton;

    boolean exit = false;
    private float scaleFix = 1F;
    double lastMouseX;
    double lastMouseY;
    private final MainMenuShaderRenderer menuShader = new MainMenuShaderRenderer();
    private boolean exitSlideDragging = false;
    private float exitSlideProgress = 0F;

    private float langBtnX, langBtnY, langBtnW, langBtnH;
    private final Animation langHover = new Animation();

    private float tgBtnX, tgBtnY, tgBtnR;
    private final Animation tgHover = new Animation();

    private static final Identifier[] MENU_BGS = new Identifier[]{
            Identifier.of("client", "textures/frame/menu.png"),
            Identifier.of("client", "textures/frame/menu2.png"),
            Identifier.of("client", "textures/frame/menu3.png"),
            Identifier.of("client", "textures/frame/menu4.png"),
    };
    private static final float[] MENU_BG_W = {1925F, 1920F, 1920F, 1920F};
    private static final float[] MENU_BG_H = {1080F, 1080F, 1080F, 1080F};

    private static int menuBgSaved = 0;

    public static void setBgSaved(int i) {
        menuBgSaved = ((i % MENU_BGS.length) + MENU_BGS.length) % MENU_BGS.length;
    }

    /** Фон главного меню: используется и заставкой загрузки, чтобы они совпадали. */
    public static net.minecraft.util.Identifier backgroundTexture() {
        return MENU_BGS[menuBgSaved];
    }

    /** Чёрно-белые копии обоев (1280 px, уже затемнены) для экрана загрузки. */
    private static final Identifier[] LOADER_BGS = new Identifier[]{
            Identifier.of("client", "textures/frame/menu_bw.png"),
            Identifier.of("client", "textures/frame/menu2_bw.png"),
            Identifier.of("client", "textures/frame/menu3_bw.png"),
            Identifier.of("client", "textures/frame/menu4_bw.png"),
    };
    private static final float[] LOADER_BG_H = {718F, 720F, 720F, 720F};

    public static net.minecraft.util.Identifier loaderTexture() {
        return LOADER_BGS[menuBgSaved];
    }

    public static float loaderWidth() {
        return 1280F;
    }

    public static float loaderHeight() {
        return LOADER_BG_H[menuBgSaved];
    }

    public static float backgroundWidth() {
        return MENU_BG_W[menuBgSaved];
    }

    public static float backgroundHeight() {
        return MENU_BG_H[menuBgSaved];
    }

    public static int getBgSaved() {
        return menuBgSaved;
    }

    private static float menuBgPos = 0F;
    private static float menuBgTarget = 0F;
    private static float menuBgLastPos = 0F;
    private static boolean menuBgInited = false;
    private static final Animation menuBgAnim = new Animation();
    private final Animation edgeArrowL = new Animation();
    private final Animation edgeArrowR = new Animation();

    @Override
    protected void init() {
        exit = false;
        alpha.set(0);
        alpha.run(1, 0.5F, Easings.BACK_OUT);


        buildButtons();
    }

    private void openModList() {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            mc.setScreen(new com.terraformersmc.modmenu.gui.ModsScreen(this));
        } else {
            code.wiosvisual.display.screens.clickgui.WiosGuiScreen.getInstance().openGui(this);
        }
    }

    private void buildButtons() {
        buttons.clear();

        singleplayerButton = new MenuButton("Одиночная игра", "Singleplayer",
                "Играть в оффлайн-миры", "Play the offline worlds",
                () -> mc.setScreen(new SelectWorldScreen(this)));
        multiplayerButton = new MenuButton("Сетевая игра", "Multiplayer",
                "Подключиться к мультиплеер-серверам", "Join and play on multiplayer servers",
                () -> mc.setScreen(new MultiplayerScreen(this)));

        buttons.add(multiplayerButton);
        buttons.add(singleplayerButton);

        // Список установленных модов и их настроек — ModMenu (вшит в jar клиента).
        // Модули Wios открываются оттуда же кнопкой настроек у Wios, а в игре — биндом ClickGUI.
        modificationsButton = new MenuButton("Модификации", "Modifications",
                "Установленные моды и их настройки", "Installed mods and their settings",
                this::openModList);
        buttons.add(modificationsButton);
        modsButton = null;

        accountsButton = new MenuButton("Аккаунты", "Accounts",
                "Переключение и управление аккаунтами", "Switch and manage your accounts",
                () -> mc.setScreen(new code.wiosvisual.display.screens.mainmenu.AccountsScreen(this)));
        settingsButton = new MenuButton("Настройки", "Settings",
                "Настройки игры и видео", "Adjust game and video options",
                () -> mc.setScreen(new OptionsScreen(this, mc.options)));
        exitButton = new MenuButton("Выход", "Exit",
                "Закрыть игру и выйти на рабочий стол", "Close the game and return to desktop",
                true, () -> mc.scheduleStop());

        buttons.add(settingsButton);
        buttons.add(accountsButton);
        buttons.add(exitButton);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        closeCheck();
        mouseX = (int) (mouseX / scaleFix);
        mouseY = (int) (mouseY / scaleFix);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        alpha.update();

        renderOverlay(context);
    }

    public void renderOverlay(DrawContext context) {
        float targetScale = 2F;
        float currentScale = (float) mc.getWindow().getScaleFactor();
        scaleFix = targetScale / currentScale;

        int screenWidth  = (int) (mc.getWindow().getScaledWidth()  / scaleFix);
        int screenHeight = (int) (mc.getWindow().getScaledHeight() / scaleFix);

        if (context != null) context.getMatrices().pushMatrix();
        Render2D.beginOverlay();
        ru.mentions.utils.render.FrameTextures.ensure();

        float alphaVal = this.alpha.get();

        renderMenuBg(screenWidth, screenHeight, alphaVal, (float) lastMouseX, (float) lastMouseY);

        // Одно размытие за кадр: раньше экран захватывался и размывался дважды.
        ScreenBlur.capture(3);

        RenderUtil.Blur.blur(0,0,screenWidth,screenHeight,alphaVal,ColorUtil.getColor(0,alphaVal * 0.2F));

        // Плавное появление: логотип выезжает первым, название следом; логотип едва заметно парит.
        float logoEase = 1F - (float) Math.pow(1F - Math.min(1F, alphaVal), 3);
        float titleT = Math.max(0F, Math.min(1F, (alphaVal - 0.35F) / 0.65F));
        float titleEase = 1F - (float) Math.pow(1F - titleT, 3);
        float bob = (float) Math.sin(System.currentTimeMillis() / 900.0) * 1.6F * logoEase;
        float logoSize = 34F;
        float logoY = screenHeight * 0.29F + 24 - 24 * logoEase + bob;
        // оптическое центрирование: «масса» знака (кольцо) левее центра картинки на ~7.4% (центроид x≈109 из 256)
        float logoX = screenWidth / 2F - logoSize / 2F + logoSize * 0.074F;
        RenderUtil.Images.texture(code.wiosvisual.utils.display.render.BackgroundIcons.icon("wm_icon.png"), logoX, logoY, logoSize, logoSize,
                ColorUtil.replAlpha(ColorUtil.client(), logoEase));

        Fonts.sf_regular.drawCentered("WiosVisuals", screenWidth / 2,
                screenHeight * 0.375F + 18 - 18 * titleEase, 13, ColorUtil.getColor(255, titleEase));

        for (MenuButton b : buttons) b.update(lastMouseX, lastMouseY);
        drawMenuButtons(screenWidth, screenHeight, alphaVal);
        drawMenuTelegramButton(screenWidth, screenHeight, alphaVal);
        drawEdgeArrows(screenWidth, screenHeight, alphaVal);

        Render2D.endOverlay();
        if (context != null) context.getMatrices().popMatrix();
    }

    public static void renderMenuBg(int screenWidth, int screenHeight, float alphaVal, float mouseX, float mouseY) {
        float parallaxStrength = 0.04F;
        float offsetX = (screenWidth / 2F - mouseX) * parallaxStrength;
        float offsetY = (screenHeight / 2F - mouseY) * parallaxStrength;

        menuBgAnim.update();
        if (!menuBgInited) {
            menuBgTarget = menuBgSaved;
            menuBgPos = menuBgSaved;
            menuBgAnim.set(menuBgSaved);
            menuBgInited = true;
        }
        float pos = menuBgAnim.get();
        float dir = Math.signum(pos - menuBgLastPos);
        menuBgLastPos = pos;

        int len = MENU_BGS.length;
        float norm = ((pos % len) + len) % len;
        int floorI = (int) Math.floor(norm);
        float frac = norm - floorI;
        int idx0 = floorI % len;
        int idx1 = (idx0 + 1) % len;

        float slide = screenWidth * 0.06F;
        float t;
        int fIdx, tIdx;
        if (dir >= 0F || frac < 0.001F || frac > 0.999F) {
            t = frac;
            fIdx = idx0;
            tIdx = idx1;
        } else {
            t = 1F - frac;
            fIdx = idx1;
            tIdx = idx0;
        }

        if (t < 0.001F) {
            float[] r = bgRect(fIdx, screenWidth, screenHeight, offsetX, offsetY, 0F);
            RenderUtil.Images.texture(MENU_BGS[fIdx], r[0], r[1], r[2], r[3], ColorUtil.getColor(255, alphaVal));
        } else {
            float a0 = (1F - t) * alphaVal;
            float a1 = t * alphaVal;
            float[] r0 = bgRect(fIdx, screenWidth, screenHeight, offsetX, offsetY, -dir * t * slide);
            float[] r1 = bgRect(tIdx, screenWidth, screenHeight, offsetX, offsetY, -dir * (t - 1F) * slide);
            RenderUtil.Images.texture(MENU_BGS[fIdx], r0[0], r0[1], r0[2], r0[3], ColorUtil.getColor(255, a0));
            RenderUtil.Images.texture(MENU_BGS[tIdx], r1[0], r1[1], r1[2], r1[3], ColorUtil.getColor(255, a1));
        }
    }

    private static float[] bgRect(int index, int screenW, int screenH, float offX, float offY, float slideOff) {
        float bgScale = 1.18F;
        float iw = MENU_BG_W[index];
        float ih = MENU_BG_H[index];
        float s = Math.max((float) screenW / iw, (float) screenH / ih) * bgScale;
        float w = iw * s;
        float h = ih * s;
        float x = (screenW - w) / 2F + offX + slideOff;
        float y = (screenH - h) / 2F + offY;
        return new float[]{x, y, w, h};
    }

    private void drawEdgeArrows(int screenW, int screenH, float alphaVal) {
        boolean leftHot = (float) lastMouseX < 36F || MathUtil.isHovered((float) lastMouseX, (float) lastMouseY,
                6F, screenH / 2F - 14F, 28F, 28F);
        boolean rightHot = (float) lastMouseX > screenW - 36F || MathUtil.isHovered((float) lastMouseX, (float) lastMouseY,
                screenW - 34F, screenH / 2F - 14F, 28F, 28F);

        edgeArrowL.update();
        edgeArrowR.update();
        edgeArrowL.run(leftHot ? 1 : 0, 0.25F, Easings.QUAD_OUT);
        edgeArrowR.run(rightHot ? 1 : 0, 0.25F, Easings.QUAD_OUT);

        float l = edgeArrowL.get();
        float r = edgeArrowR.get();

        if (l > 0.01F) {
            drawArrow(6F, screenH / 2F - 14F, l, alphaVal, false);
        }
        if (r > 0.01F) {
            drawArrow(screenW - 34F, screenH / 2F - 14F, r, alphaVal, true);
        }
    }

    private void drawArrow(float x, float y, float hp, float alphaVal, boolean right) {
        Font draw = Fonts.sf_regular;
        float size = 7F + 6F * hp;
        float cx = x + 14F;
        float cy = y + 14F;
        float a = alphaVal * hp;
        String g = right ? ">" : "<";
        draw.drawCentered(g, cx + 1.5F, cy + 1.5F, size, ColorUtil.getColor(0, a * 0.4F));
        draw.drawCentered(g, cx, cy, size, ColorUtil.getColor(255, a));
    }

    private void startBgSwitch(int dir) {
        int cur = Math.round(menuBgAnim.get());
        menuBgSaved = ((cur + dir) % MENU_BGS.length + MENU_BGS.length) % MENU_BGS.length;
        menuBgTarget += dir;
        menuBgAnim.run(menuBgTarget, 0.55F, Easings.CUBIC_OUT);
    }

    private void drawMenuButtons(int screenWidth, int screenHeight, float alphaVal) {
        updateExitSlideProgress();

        float gap = 4;
        float panelWidth = Math.min(220, screenWidth - 36F);
        float cardWidth = (panelWidth - gap) / 2F;
        float cardHeight = 25;
        float totalCardsWidth = cardWidth * 2F + gap;

        float smallGap = 4;
        int smallCount = modsButton == null ? 2 : 3;
        float smallWidth = (totalCardsWidth - smallGap * (smallCount - 1)) / smallCount;
        float smallHeight = 25;

        float modHeight = 25;

        float exitWidth = Math.min(150F, totalCardsWidth * 0.65F);
        float exitHeight = 25;

        float totalBlockHeight = cardHeight + gap + modHeight + gap + smallHeight + gap + exitHeight;

        float startX = screenWidth / 2F - totalCardsWidth / 2F;
        float startY = screenHeight / 2F - totalBlockHeight / 2F;
        float appearY = (1F - alphaVal) * 34F;

        float currentY = startY + appearY;

        multiplayerButton.drawHero(startX, currentY, cardWidth, cardHeight, alphaVal, 0);
        singleplayerButton.drawHero(startX + cardWidth + gap, currentY, cardWidth, cardHeight, alphaVal, 1);

        currentY += cardHeight + gap;

        modificationsButton.drawCompact(startX, currentY, totalCardsWidth, modHeight, alphaVal, "p");

        currentY += modHeight + gap;

        float smallX = startX;
        settingsButton.drawCompact(smallX, currentY, smallWidth, smallHeight, alphaVal, "l");
        smallX += smallWidth + smallGap;
        accountsButton.drawCompact(smallX, currentY, smallWidth, smallHeight, alphaVal, "m");
        if (modsButton != null) {
            smallX += smallWidth + smallGap;
            modsButton.drawCompact(smallX, currentY, smallWidth, smallHeight, alphaVal, "n");
        }

        currentY += smallHeight + gap;

        float exitX = screenWidth / 2F - exitWidth / 2F;
        exitButton.drawExit(exitX, currentY, exitWidth, exitHeight, alphaVal, exitSlideProgress, exitSlideDragging);
    }

    private void updateExitSlideProgress() {
        if (!exitSlideDragging || exitButton == null) return;

        float trackX = exitButton.getLastX() + 5F;
        float trackW = Math.max(1F, exitButton.getLastW() - 34F);
        exitSlideProgress = Math.max(0F, Math.min(1F, ((float) lastMouseX - trackX) / trackW));
    }

    private void drawMenuTelegramButton(int screenWidth, int screenHeight, float alphaVal) {
        float radius = 13;
        float workRadius = radius * (0.8F + 0.2F * scaleFix);
        float bx = screenWidth - workRadius - 13;
        float by = screenHeight - workRadius - 13F;

        tgBtnX = bx; tgBtnY = by; tgBtnR = workRadius;

        boolean hov = ru.mentions.utils.math.MathUtil.isHovered((float) lastMouseX, (float) lastMouseY,
                bx - workRadius, by - workRadius, workRadius * 2F, workRadius * 2F);
        tgHover.update();
        tgHover.run(hov ? 1 : 0, 0.18F, Easings.QUAD_OUT);
        float hp = tgHover.get();
        float tFlash = (1F - alphaVal) * 1F;

        ru.mentions.utils.render.RenderUtil.Blur.blur(bx - workRadius, by - workRadius,
                workRadius * 2F, workRadius * 2F, workRadius, 10F,
                ColorUtil.getColor(20, alphaVal * (0.25F + 0.12F * hp)));

        float iconSize = workRadius * 2F - 6F;
        ru.mentions.utils.render.RenderUtil.Images.texture(Identifier.of("client", "textures/tg.png"),
                bx - iconSize / 2F, by - iconSize / 2F, iconSize, iconSize,
                ColorUtil.replAlpha(ColorUtil.getColor(255), alphaVal * (0.6F + 0.4F * hp)));
    }

    private void drawLanguageButton(int screenWidth, float alphaVal) {
        float w = 25, h = 15;
        float bx = screenWidth - w - 12, by = 12;
        langBtnX = bx; langBtnY = by; langBtnW = w; langBtnH = h;

        boolean hov = ru.mentions.utils.math.MathUtil.isHovered((float) lastMouseX, (float) lastMouseY, bx, by, w, h);
        langHover.update();
        langHover.run(hov ? 1 : 0, 0.18F, Easings.QUAD_OUT);
        float hp = langHover.get();

        int accent = ColorUtil.getColor(110, 255, 133);
        RenderUtil.Blur.blur(bx, by, w, h, 1, 5, ColorUtil.getColor(20, alphaVal * (0.3F + 0.1F * hp)));

        int textColor = ColorUtil.replAlpha(
                ColorUtil.interpolateColor(ColorUtil.getColor(170, 170, 170, 1F), accent, hp), alphaVal);
        Fonts.sf_regular.drawCentered(ru.mentions.lang.Lang.tag(), bx + w / 2F, by + h / 2F - 3.8F, 6, textColor);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float mouseX = (float) (click.x() / scaleFix);
        float mouseY = (float) (click.y() / scaleFix);
        int button = click.button();

        if (button == 0) {
            if (ru.mentions.utils.math.MathUtil.isHovered(mouseX, mouseY, langBtnX, langBtnY, langBtnW, langBtnH)) {
                ru.mentions.lang.Lang.toggle();
                return true;
            }
            if (ru.mentions.utils.math.MathUtil.isHovered(mouseX, mouseY,
                    tgBtnX - tgBtnR, tgBtnY - tgBtnR, tgBtnR * 2F, tgBtnR * 2F)) {
                Util.getOperatingSystem().open("https://t.me/wioscloud");
                return true;
            }
            for (MenuButton btn : buttons) {
                if (btn.contains(mouseX, mouseY)) {
                    if (btn == exitButton) {
                        exitSlideDragging = true;
                        updateExitSlideProgress();
                        return true;
                    }
                    btn.onClick();
                    return true;
                }
            }

            if (mouseX < 36F) { startBgSwitch(-1); return true; }
            if (mouseX > (float) mc.getWindow().getScaledWidth() / scaleFix - 36F) { startBgSwitch(1); return true; }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (exitSlideDragging) {
            lastMouseX = click.x() / scaleFix;
            lastMouseY = click.y() / scaleFix;
            updateExitSlideProgress();
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (exitSlideDragging) {
            lastMouseX = click.x() / scaleFix;
            lastMouseY = click.y() / scaleFix;
            updateExitSlideProgress();
            exitSlideDragging = false;

            if (exitSlideProgress >= 0.90F) {
                exitSlideProgress = 1F;
                exitButton.onClick();
            } else {
                exitSlideProgress = 0F;
            }
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean shouldCloseOnEsc() {
        if (!exit) {
            exit = false;
        }
        return false;
    }

    private void closeCheck() {
        if (exit && alpha.isFinished()) {
            close();
            alpha.run(0, 0.5F, Easings.SINE_OUT);
            exit = false;
        }
    }

    private static class MenuButton {
        private final String textRu, textEn;
        private final String descRu, descEn;
        private final Runnable action;
        private final boolean danger;
        private final Animation hoverAnim = new Animation();
        private boolean hovered;

        private float lastX, lastY, lastW, lastH;

        public MenuButton(String textRu, String textEn, String descRu, String descEn, Runnable action) {
            this(textRu, textEn, descRu, descEn, false, action);
        }

        public MenuButton(String textRu, String textEn, String descRu, String descEn, boolean danger, Runnable action) {
            this.textRu = textRu;
            this.textEn = textEn;
            this.descRu = descRu;
            this.descEn = descEn;
            this.danger = danger;
            this.action = action;
            this.hoverAnim.set(0);
        }

        private String text() { return textRu; }
        private String description() { return ru.mentions.lang.Lang.pick(descRu, descEn); }

        public float getWidth() {
            Font font = Fonts.sf_regular;
            return Math.max(font.getWidth(text(), 8), font.getWidth(description(), 6)) + 34;
        }

        public boolean contains(float x, float y) {
            return MathUtil.isHovered(x, y, lastX, lastY, lastW, lastH);
        }

        public float getHoverValue() {
            return hoverAnim.get();
        }

        public void update(double mouseX, double mouseY) {
            hovered = MathUtil.isHovered((float) mouseX, (float) mouseY, lastX, lastY, lastW, lastH);
            hoverAnim.update();
            hoverAnim.run(hovered ? 1 : 0, 0.18F, Easings.QUAD_OUT);
        }

        private void rememberBounds(float x, float y, float width, float height) {
            lastX = x;
            lastY = y;
            lastW = width;
            lastH = height;
        }

        public float getLastX() {
            return lastX;
        }

        public float getLastW() {
            return lastW;
        }

        public void drawHero(float x, float y, float width, float height, float globalAlpha, int variant) {
            rememberBounds(x, y, width, height);

            float hp = hoverAnim.get();
            float radius = 9F;
            float headerHeight = height;
            int accent = ColorUtil.overCol(ColorUtil.getColor(125, 125, 125), ColorUtil.client(), hp);

            RenderUtil.Blur.blur(x, y, width, headerHeight, 1, 6,
                    ColorUtil.getColor(12, globalAlpha * (0.3F + hp * 0.1F)));

            int textColor = ColorUtil.replAlpha(
                    ColorUtil.interpolateColor(ColorUtil.getColor(255, 1F), ColorUtil.getColor(255, 1F), hp),
                    globalAlpha * (0.3F + hp * 0.7F));

            Fonts.sf_regular.draw(text(), x + 12F, y + 7.8F, 7, textColor);
            Fonts.icon.drawCentered(variant == 0 ? "j" : "k", x + width - 15, y + 9.25F, 6,
                    ColorUtil.replAlpha(accent, globalAlpha * (0.3F + hp * 0.7F)));
        }

        public void drawCompact(float x, float y, float width, float height, float globalAlpha, String icon) {
            rememberBounds(x, y, width, height);

            float hp = hoverAnim.get();
            float radius = 9F;
            int accent = ColorUtil.overCol(ColorUtil.getColor(255), ColorUtil.client(), hp);
            int fill = ColorUtil.getColor(12, globalAlpha * (0.3F + hp * 0.1F));

            RenderUtil.Blur.blur(x, y, width, height, 1, radius, fill);

            int textColor = ColorUtil.replAlpha(
                    ColorUtil.getColor(255),
                    globalAlpha * (0.3F + hp * 0.7F));

            Fonts.sf_regular.draw(text(), x + 12F, y + 7.8F, 7, textColor);
            Fonts.icon.drawCentered(icon, x + width - 15, y + 9.25F, 6,
                    ColorUtil.replAlpha(accent, globalAlpha * (0.2F + hp * 0.8F)));
        }

        public void drawExit(float x, float y, float width, float height, float globalAlpha, float slideProgress, boolean dragging) {
            rememberBounds(x, y, width, height);

            float hp = hoverAnim.get();
            float radius = 9F;
            int accent = ColorUtil.getColor(255, dragging ? 145 : 255, dragging ? 145 : 255);
            int fill = ColorUtil.getColor(20, globalAlpha * (0.35F + hp * 0.10F));
            float progress = Math.max(0F, Math.min(1F, slideProgress));
            float knobSize = height - 8F;
            float knobX = x + 5F + (width - knobSize - 10F) * progress;

            RenderUtil.Blur.blur(x, y, width, height, 1, radius, fill);

            RenderUtil.Render2D.rect(knobX, y + 4F, knobSize, knobSize,
                    ColorUtil.getColor(255, 255, 255, globalAlpha * (0.02F + hp * 0.03F + (dragging ? 0.02F : 0F))), 4);
            Fonts.icon.drawCentered("i", knobX + knobSize / 2F, y + height / 2F - 3, 7,
                    ColorUtil.replAlpha(accent, globalAlpha * (0.2F + hp * 0.2F + progress * 0.2f)));
            Fonts.sf_regular.drawCentered(ru.mentions.lang.Lang.pick("Сдвиньте, чтобы выйти", "Сдвиньте, чтобы выйти"), x + width / 2F + 8F,
                    y + height / 2F - 4.4F, 7,
                    ColorUtil.getColor(255, globalAlpha * (0.1F + hp * 0.2F - progress * 0.16F)));
        }

        public void draw(float x, float y, float width, float height, float globalAlpha) {
            float hp = hoverAnim.get();
            float radius = 9;
            int accent = ColorUtil.getColor(110, 255, 133);

            rememberBounds(x, y, width, height);

            int fill = danger ? ColorUtil.overCol(ColorUtil.getColor(12,
                    globalAlpha * (0.3F - 0.2F * hp)), ColorUtil.getColor(
                    45, 0, 0, globalAlpha * (0.3F)), hp) :
                    ColorUtil.getColor(12, globalAlpha * (0.3F - 0.2F * hp));
            RenderUtil.Blur.blur(x, y, width, height, 1, radius, fill);

            Font font = Fonts.sf_regular;

            int titleTarget = danger ? ColorUtil.getColor(255, 125, 125) : accent;
            int titleColor = ColorUtil.replAlpha(
                    ColorUtil.interpolateColor(ColorUtil.getColor(160, 160, 160, 1F), titleTarget, hp), globalAlpha);

            String text = text();
            float titleX = x + width / 2F - font.getWidth(text, 8) / 2F;
            font.draw(text, titleX, y + 12, 8, titleColor);
        }

        public boolean isHovered() {
            return hovered;
        }

        public void onClick() {
            if (action != null) {
                action.run();
            }
        }
    }
}
