package code.wiosvisual.display.screens.mainmenu;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.mixins.IMinecraftClient;
import code.wiosvisual.utils.client.managers.file.impl.AccountFile;
import code.wiosvisual.utils.client.managers.file.impl.account.Account;
import code.wiosvisual.utils.client.managers.file.impl.account.AccountRepository;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.display.render.systemrender.builders.Builder;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadColorState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.SizeState;
import code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.authlib.minecraft.UserApiService;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Main menu ported from the neverlose client. The layout, particle backdrop and alt manager are
 * taken from there; accounts and session switching go through Wiosvisual's own repository so the
 * saved account list keeps working.
 */
public class NeverloseMenu
extends Screen
implements QuickImports {
    private static final Identifier WALLPAPER = Identifier.of((String)"wiosvisual", (String)"background/menubg_mono.png");
    private static final int WALLPAPER_WIDTH = 1920;
    private static final int WALLPAPER_HEIGHT = 1080;
    private static final int PARTICLE_COUNT = 50;
    private static final float PARTICLE_FADE_THRESHOLD = 0.8f;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float alpha;
        float lifetime;
        float maxLifetime;
        boolean isDead;

        Particle(float x, float y, Random rand) {
            this.x = x;
            this.y = y;
            this.vx = (rand.nextFloat() - 0.5f) * 0.2f;
            this.vy = (rand.nextFloat() - 0.5f) * 0.2f;
            this.size = 0.5f + rand.nextFloat();
            this.alpha = 0.2f + rand.nextFloat() * 0.3f;
            this.maxLifetime = 10.0f + rand.nextFloat() * 10.0f;
        }

        void update(float delta, int width, int height) {
            this.lifetime += delta / 60.0f;
            if (this.lifetime > this.maxLifetime * PARTICLE_FADE_THRESHOLD) {
                float fadeProgress = (this.lifetime - this.maxLifetime * PARTICLE_FADE_THRESHOLD) / (this.maxLifetime * 0.2f);
                this.alpha = Math.max(0.0f, this.alpha * (1.0f - fadeProgress));
            }
            if (this.lifetime >= this.maxLifetime) {
                this.isDead = true;
                return;
            }
            this.x += this.vx * delta;
            this.y += this.vy * delta;
            if (this.x < -10.0f || this.x > (float)width + 10.0f || this.y < -10.0f || this.y > (float)height + 10.0f) {
                this.isDead = true;
            }
        }
    }

    private static enum View {
        MAIN_MENU,
        ALT_SCREEN;
    }

    private final List<Particle> particles = new ArrayList<Particle>();
    private final Random rand = new Random();
    private AccountRepository accountRepository;
    private final FontRenderer titleFont = Fonts.getSize(34, Fonts.Type.SuisseIntlRegular);
    private final FontRenderer subtitleFont = Fonts.getSize(15, Fonts.Type.DEFAULT);
    private final FontRenderer buttonFont = Fonts.getSize(14, Fonts.Type.DEFAULT);
    private final FontRenderer panelFont = Fonts.getSize(13, Fonts.Type.SEMI);
    private final FontRenderer smallFont = Fonts.getSize(12, Fonts.Type.DEFAULT);

    private View currentView = View.MAIN_MENU;
    private final float[] buttonHoverProgress = new float[6];
    private int lastWidth;
    private int lastHeight;
    private String nicknameText = "";
    private boolean nicknameFieldFocused;
    private float scrollOffset;

    private static NeverloseMenu instance;

    /** Built on first use so the client (fonts, account repository) is already up. */
    public static NeverloseMenu getInstance() {
        if (instance == null) {
            instance = new NeverloseMenu();
        }
        return instance;
    }

    public NeverloseMenu() {
        super(Text.literal((String)"Main Menu"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }


    private AccountRepository accounts() {
        if (this.accountRepository == null) {
            this.accountRepository = Wiosvisual.getInstance().getAccountRepository();
        }
        return this.accountRepository;
    }

    private void initParticles(int width, int height) {
        while (this.particles.size() < PARTICLE_COUNT) {
            this.particles.add(new Particle(this.rand.nextFloat() * (float)width, this.rand.nextFloat() * (float)height, this.rand));
        }
    }

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = GuiCompat.stack(context);
        int w = this.width;
        int h = this.height;
        if (this.lastWidth != w || this.lastHeight != h) {
            this.particles.clear();
            this.lastWidth = w;
            this.lastHeight = h;
        }
        float deltaTime = Math.min(delta, 0.05f);
        for (Particle p : this.particles) {
            p.update(deltaTime * 60.0f, w, h);
        }
        this.particles.removeIf(p -> p.isDead);
        this.initParticles(w, h);

        this.drawBackground(matrix, w, h);
        this.updateButtonAnimations(deltaTime, mouseX, mouseY, w, h);
        if (this.currentView == View.MAIN_MENU) {
            this.renderMainMenu(context, matrix, w, h, mouseX, mouseY);
        } else {
            this.renderAltScreen(context, matrix, w, h, mouseX, mouseY);
        }
    }

    void drawBackground(MatrixStack matrix, int w, int h) {
        rectangle.render(ShapeProperties.create(matrix, 0.0, 0.0, w, h).color(0xFF000000).build());
        Rectangle.flushPending();
        this.drawWallpaper(matrix, w, h);
        // Лёгкое затемнение поверх чёрно-белых размытых обоев.
        rectangle.render(ShapeProperties.create(matrix, 0.0, 0.0, w, h).color(new Color(0, 0, 0, 140).getRGB()).build());
        Rectangle.flushPending();
    }

    /**
     * Обои рисуются немедленным рендером (как в {@link MainMenu}), а не через {@code class_332}:
     * контекст сбрасывает свой буфер в конце кадра и накрыл бы собой всё меню.
     * Кадрирование делается по UV — картинка заполняет экран без растяжения.
     */
    private void drawWallpaper(MatrixStack matrix, int w, int h) {
        float imageAspect = (float)WALLPAPER_WIDTH / (float)WALLPAPER_HEIGHT;
        float screenAspect = (float)w / (float)h;
        float u0 = 0.0f;
        float v0 = 0.0f;
        float u1 = 1.0f;
        float v1 = 1.0f;
        if (screenAspect > imageAspect) {
            float visible = imageAspect / screenAspect;
            v0 = (1.0f - visible) / 2.0f;
            v1 = v0 + visible;
        } else {
            float visible = screenAspect / imageAspect;
            u0 = (1.0f - visible) / 2.0f;
            u1 = u0 + visible;
        }
        // 1.21.11: BuiltTexture не умеет привязывать текстуру по GL-id — рисуем через слой с Sampler0.
        org.joml.Matrix4f m = matrix.peek().getPositionMatrix();
        net.minecraft.client.render.BufferBuilder b = net.minecraft.client.render.Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(m, 0.0f, 0.0f, 0.0f).texture(u0, v0).color(0xFFFFFFFF);
        b.vertex(m, 0.0f, (float)h, 0.0f).texture(u0, v1).color(0xFFFFFFFF);
        b.vertex(m, (float)w, (float)h, 0.0f).texture(u1, v1).color(0xFFFFFFFF);
        b.vertex(m, (float)w, 0.0f, 0.0f).texture(u1, v0).color(0xFFFFFFFF);
        code.wiosvisual.utils.display.render.pipeline.WiosRender.draw(code.wiosvisual.utils.display.render.pipeline.WiosRender.posTexColor(WALLPAPER, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, false, false), b);
    }

    /* ------------------------------------------------------------------ *
     *  Главная страница: заголовок с бегущей тенью, приветствие, сетка
     *  2x2 из кнопок и слайдер «сдвиньте, чтобы выйти».
     * ------------------------------------------------------------------ */

    private static final float BTN_W = 166.0f;
    private static final float BTN_H = 20.0f;
    private static final float BTN_GAP = 5.0f;
    private static final float SLIDER_W = 166.0f;
    private static final float SLIDER_H = 20.0f;
    /** Воздух между навигацией и парой «Моды / Выход». */
    private static final float GROUP_GAP = 17.0f;
    /** Центр колонки кнопок и центр логотипа — доли ширины экрана. */
    private static final float COLUMN_CENTER = 0.777f;
    private static final float LOGO_CENTER_X = 0.25f;
    private static final int LOGO_FONT_SIZE = 190;
    /** Акцент: единственный цвет на почти чёрном экране. */
    private static final int ACCENT_LIGHT = 0xFFB4BBEE;
    private static final int ACCENT = 0xFF7E87CF;
    /** Визуальный ряд кнопки idx: порядок на экране отличается от порядка действий. */
    private static final int[] SLOT = new int[]{1, 0, 3, 2};
    private static final long SWEEP_MS = 3200L;

    private static boolean english;
    private float langHover;
    private static final String[] SOCIAL_URLS = new String[]{
            "https://t.me/wioscloud",
            "https://www.youtube.com/@jskov1337",
            "https://discord.gg/uQ4TcvcZZa"};
    private static final Identifier[] SOCIAL_ICONS = new Identifier[]{BackgroundIcons.SOCIAL_TELEGRAM, BackgroundIcons.SOCIAL_YOUTUBE, BackgroundIcons.SOCIAL_DISCORD};
    /** Фирменные цвета: Telegram, YouTube, Discord. */
    private static final int[] SOCIAL_COLORS = new int[]{0x27A7E7, 0xFF0033, 0x5865F2};
    private static final float SOCIAL_SIZE = 20.0f;
    private static final float SOCIAL_GAP = 6.0f;
    private final float[] socialHover = new float[3];
    /** Подсветка под курсором: плавно переезжает от кнопки к кнопке. */
    private float hlX = -1.0f;
    private float hlY;
    private float hlW;
    private float hlAlpha;

    static String tr(String ru, String en) {
        return english ? en : ru;
    }

    private float columnCenterX(int w) {
        return (float)w * COLUMN_CENTER;
    }

    private float columnX(int w) {
        return this.columnCenterX(w) - BTN_W / 2.0f;
    }

    private float titleY(int h) {
        return (float)h * 0.22f;
    }

    private float gridY(int h) {
        return this.titleY(h) + 46.0f;
    }

    /** Верх строки {@code row}; с ряда 4 добавляется разрыв перед «Модами». */
    private float rowY(int h, int row) {
        return this.gridY(h) + (float)row * (BTN_H + BTN_GAP) + (row >= 4 ? GROUP_GAP : 0.0f);
    }

    private float sliderY(int h) {
        return this.rowY(h, 4);
    }

    /** Левый верхний угол кнопки {@code idx}: 0/1 — верхний ряд, 2/3 — нижний. */
    private float[] buttonPos(int idx, int w, int h) {
        return new float[]{this.columnX(w), this.rowY(h, SLOT[idx])};
    }

    private float[] langPos(int w) {
        return new float[]{(float)w - 30.0f, 10.0f, 20.0f, 14.0f};
    }

    private void renderMainMenu(DrawContext context, MatrixStack matrix, int w, int h, int mouseX, int mouseY) {
        float centerX = this.columnCenterX(w);
        float titleY = this.titleY(h);
        this.drawLogo(matrix, w, h);

        // Заголовок: светлые буквы, по которым медленно проходит тёмная волна.
        float sweep = (float)(System.currentTimeMillis() % SWEEP_MS) / (float)SWEEP_MS;
        this.drawSweepText(matrix, this.titleFont, "WiosVisuals", centerX, titleY, ACCENT_LIGHT, ACCENT, sweep, true);
        String nick = mc.getSession() == null ? "Player" : mc.getSession().getUsername();
        String welcome = "Welcome, ";
        float welcomeW = this.subtitleFont.getStringWidth(welcome) + this.subtitleFont.getStringWidth(nick);
        float welcomeX = centerX - welcomeW / 2.0f;
        this.subtitleFont.drawString(matrix, welcome, welcomeX, titleY + 26.0f, 0xFFF0F0F0);
        this.drawSweepText(matrix, this.subtitleFont, nick, welcomeX + this.subtitleFont.getStringWidth(welcome), titleY + 26.0f, 0xFFD8D8D8, 0xFF707070, sweep, false);

        if (this.hlAlpha > 0.01f) {
            float hx = NeverloseMenu.snap(this.hlX);
            float hy = NeverloseMenu.snap(this.hlY);
            rectangle.render(ShapeProperties.create(matrix, hx - 1.5f, hy - 1.5f, this.hlW + 3.0f, BTN_H + 3.0f).round(BTN_H / 2.0f + 1.5f).softness(0.8f)
                    .color(new Color(255, 255, 255, (int)(95.0f * this.hlAlpha)).getRGB()).build());
            Rectangle.flushPending();
        }
        String[] labels = new String[]{tr("Сетевая игра", "Multiplayer"), tr("Одиночная игра", "Singleplayer"), tr("Настройки", "Settings"), tr("Аккаунты", "Accounts")};
        Identifier[] icons = new Identifier[]{BackgroundIcons.MULTIPLAYER, BackgroundIcons.SOLO, BackgroundIcons.SETTINGS, BackgroundIcons.ACCOUNT};
        for (int i = 0; i < 4; ++i) {
            float[] pos = this.buttonPos(i, w, h);
            this.drawMenuButton(context, matrix, pos[0], pos[1], labels[i], icons[i], this.buttonHoverProgress[i]);
        }
        this.drawExitButton(context, matrix, w, h);
        this.drawSocials(context, matrix, w, h);

        float[] lang = this.langPos(w);
        rectangle.render(ShapeProperties.create(matrix, lang[0], lang[1], lang[2], lang[3]).round(4.0f)
                .color(NeverloseMenu.interpolateColor(new Color(30, 30, 30, 170).getRGB(), new Color(55, 55, 55, 210).getRGB(), this.langHover)).build());
        Rectangle.flushPending();
        this.smallFont.drawCenteredString(matrix, english ? "EN" : "RU", lang[0] + lang[2] / 2.0f, lang[1] + 4.0f, 0xFFD0D0D0);
    }

    /**
     * Текст с бегущей волной цвета: {@code light} — основной цвет букв, {@code dark} — цвет,
     * в который буквы уходят, когда через них проходит волна.
     */
    private void drawSweepText(MatrixStack matrix, FontRenderer font, String text, float x, float y, int light, int dark, float phase, boolean centered) {
        float total = font.getStringWidth(text);
        float cursor = centered ? x - total / 2.0f : x;
        float start = cursor;
        for (int i = 0; i < text.length(); ++i) {
            String glyph = String.valueOf(text.charAt(i));
            float glyphW = font.getStringWidth(glyph);
            float centre = total <= 0.0f ? 0.0f : (cursor - start + glyphW / 2.0f) / total;
            // Волна зациклена и шире самой строки: между проходами буквы успевают посветлеть.
            float delta = centre * 0.6f - (phase * 1.6f - 0.5f);
            float wave = (float)Math.exp(-(delta * delta) / 0.02f);
            font.drawString(matrix, glyph, cursor, y, NeverloseMenu.interpolateColor(light, dark, wave));
            cursor += glyphW;
        }
    }

    /** Привязка к целому физическому пикселю: иначе края кнопок и текст размазываются. */
    static float snap(float value) {
        double scale = mc.getWindow().getScaleFactor();
        return (float)(Math.round(value * scale) / scale);
    }

    private void drawMenuButton(DrawContext context, MatrixStack matrix, float x, float y, String label, Identifier icon, float hover) {
        this.drawMenuButton(context, matrix, x, y, BTN_W, label, icon, hover, false);
    }

    private void drawMenuButton(DrawContext context, MatrixStack matrix, float rawX, float rawY, float width, String label, Identifier icon, float hover, boolean centered) {
        float x = NeverloseMenu.snap(rawX);
        float y = NeverloseMenu.snap(rawY);
        int top = NeverloseMenu.interpolateColor(new Color(33, 33, 33, 238).getRGB(), new Color(50, 50, 50, 248).getRGB(), hover);
        int bottom = NeverloseMenu.interpolateColor(new Color(27, 27, 27, 238).getRGB(), new Color(43, 43, 43, 248).getRGB(), hover);
        // Рамка — отдельная подложка на пиксель больше: шейдерная обводка на скруглениях шла точками.
        rectangle.render(ShapeProperties.create(matrix, x - 1.0f, y - 1.0f, width + 2.0f, BTN_H + 2.0f).round(BTN_H / 2.0f + 1.0f).softness(0.5f)
                .color(new Color(255, 255, 255, 22).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, width, BTN_H).round(BTN_H / 2.0f).softness(0.5f).color(top, top, bottom, bottom).build());
        Rectangle.flushPending();
        int text = NeverloseMenu.interpolateColor(new Color(222, 222, 222).getRGB(), -1, hover);
        float textY = NeverloseMenu.snap(y + BTN_H / 2.0f - 3.5f);
        float iconColorMix = hover;
        int iconColor = NeverloseMenu.interpolateColor(new Color(170, 170, 170, 230).getRGB(), new Color(245, 245, 245, 255).getRGB(), iconColorMix);
        // Иконка слева, подпись сразу за ней — одинаково у всех строк колонки.
        float iconX = NeverloseMenu.snap(x + 12.0f);
        BackgroundIcons.draw(context, icon, iconX, NeverloseMenu.snap(y + BTN_H / 2.0f - 4.0f), 8.0f, iconColor);
        this.buttonFont.drawString(matrix, label, iconX + 15.0f, textY, text);
    }

    /** Большая «W» слева — единственное цветное пятно на почти чёрном фоне. */
    private void drawLogo(MatrixStack matrix, int w, int h) {
        FontRenderer font = Fonts.getSize(LOGO_FONT_SIZE, Fonts.Type.BOLD);
        float glyphW = font.getStringWidth("W");
        float glyphH = font.getStringHeight("W");
        font.drawGradientString(matrix, "W", NeverloseMenu.snap((float)w * LOGO_CENTER_X - glyphW / 2.0f), NeverloseMenu.snap((float)h * 0.5f - glyphH / 2.0f), ACCENT_LIGHT, ACCENT);
    }

    /** Последние две строки колонки: «Моды», затем «Выход». */
    private float[] modsPos(int w, int h) {
        return new float[]{this.columnX(w), this.rowY(h, 4), BTN_W, SLIDER_H};
    }

    private float[] exitPos(int w, int h) {
        return new float[]{this.columnX(w), this.rowY(h, 5), BTN_W, SLIDER_H};
    }

    /** x, y, w, h кнопки: 0..3 — сетка, 4 — выход, 5 — моды. */
    private float[] hitBox(int idx, int w, int h) {
        if (idx < 4) {
            float[] pos = this.buttonPos(idx, w, h);
            return new float[]{pos[0], pos[1], BTN_W, BTN_H};
        }
        return idx == 4 ? this.exitPos(w, h) : this.modsPos(w, h);
    }

    /** Левый верхний угол кнопки соцсети {@code idx}: ряд по центру под кнопкой выхода. */
    private float[] socialPos(int idx, int w, int h) {
        float row = SOCIAL_SIZE * 3.0f + SOCIAL_GAP * 2.0f;
        float x = this.columnCenterX(w) - row / 2.0f + (float)idx * (SOCIAL_SIZE + SOCIAL_GAP);
        return new float[]{x, this.rowY(h, 5) + SLIDER_H + 12.0f};
    }

    private void drawSocials(DrawContext context, MatrixStack matrix, int w, int h) {
        for (int i = 0; i < SOCIAL_URLS.length; ++i) {
            float[] pos = this.socialPos(i, w, h);
            float x = NeverloseMenu.snap(pos[0]);
            float y = NeverloseMenu.snap(pos[1] - this.socialHover[i] * 1.5f);
            float hover = this.socialHover[i];
            int brand = 0xFF000000 | SOCIAL_COLORS[i];
            // В покое — тёмная кнопка с цветной иконкой и рамкой в цвет бренда; при наведении заливается цветом.
            int top = NeverloseMenu.interpolateColor(new Color(34, 34, 36, 235).getRGB(), brand, hover);
            int bottom = NeverloseMenu.interpolateColor(new Color(24, 24, 26, 235).getRGB(), NeverloseMenu.interpolateColor(brand, 0xFF000000, 0.25f), hover);
            int border = NeverloseMenu.interpolateColor((110 << 24) | SOCIAL_COLORS[i], (230 << 24) | SOCIAL_COLORS[i], hover);
            rectangle.render(ShapeProperties.create(matrix, x - 1.0f, y - 1.0f, SOCIAL_SIZE + 2.0f, SOCIAL_SIZE + 2.0f).round(6.0f).softness(0.5f).color(border).build());
            rectangle.render(ShapeProperties.create(matrix, x, y, SOCIAL_SIZE, SOCIAL_SIZE).round(5.0f).softness(0.5f).color(top, top, bottom, bottom).build());
            Rectangle.flushPending();
            int iconColor = NeverloseMenu.interpolateColor(brand, -1, hover);
            BackgroundIcons.draw(context, SOCIAL_ICONS[i], NeverloseMenu.snap(x + 5.0f), NeverloseMenu.snap(y + 5.0f), 10.0f, iconColor);
        }
    }

    private void drawExitButton(DrawContext context, MatrixStack matrix, int w, int h) {
        float[] pos = this.exitPos(w, h);
        this.drawMenuButton(context, matrix, pos[0], pos[1], pos[2], tr("Выход", "Exit"), BackgroundIcons.EXIT, this.buttonHoverProgress[4], false);
        float[] mods = this.modsPos(w, h);
        this.drawMenuButton(context, matrix, mods[0], mods[1], mods[2], tr("Моды", "Mods"), BackgroundIcons.SETTINGS, this.buttonHoverProgress[5], false);
    }

    private void updateButtonAnimations(float dT, float mX, float mY, int sw, int sh) {
        float lS = 1.0f - (float)Math.pow(0.001f, dT);
        boolean main = this.currentView == View.MAIN_MENU;
        int target = -1;
        for (int i = 0; i < 6; ++i) {
            float[] box = this.hitBox(i, sw, sh);
            boolean hovered = main && NeverloseMenu.isMouseOver(mX, mY, box[0], box[1], box[2], box[3]);
            if (hovered) {
                target = i;
            }
            this.buttonHoverProgress[i] = MathHelper.lerp((float)lS, (float)this.buttonHoverProgress[i], (float)(hovered ? 1.0f : 0.0f));
        }
        float move = 1.0f - (float)Math.pow(0.00002f, dT);
        if (target >= 0) {
            float[] pos = this.hitBox(target, sw, sh);
            float bw = pos[2];
            if (this.hlX < 0.0f || this.hlAlpha < 0.02f) {
                // Появляется сразу на месте, а не выезжает из угла.
                this.hlX = pos[0];
                this.hlY = pos[1];
                this.hlW = bw;
            } else {
                this.hlX += (pos[0] - this.hlX) * move;
                this.hlY += (pos[1] - this.hlY) * move;
                this.hlW += (bw - this.hlW) * move;
            }
        }
        this.hlAlpha += ((target >= 0 ? 1.0f : 0.0f) - this.hlAlpha) * move;
        for (int i = 0; i < this.socialHover.length; ++i) {
            float[] pos = this.socialPos(i, sw, sh);
            boolean hovered = main && NeverloseMenu.isMouseOver(mX, mY, pos[0], pos[1], SOCIAL_SIZE, SOCIAL_SIZE);
            this.socialHover[i] = MathHelper.lerp((float)lS, (float)this.socialHover[i], (float)(hovered ? 1.0f : 0.0f));
        }
        float[] lang = this.langPos(sw);
        boolean langHovered = main && NeverloseMenu.isMouseOver(mX, mY, lang[0], lang[1], lang[2], lang[3]);
        this.langHover = MathHelper.lerp((float)lS, (float)this.langHover, (float)(langHovered ? 1.0f : 0.0f));
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        float mX = (float)mouseX;
        float mY = (float)mouseY;
        if (this.currentView == View.MAIN_MENU && button == 0) {
            for (int i = 0; i < 4; ++i) {
                float[] pos = this.buttonPos(i, this.width, this.height);
                if (!NeverloseMenu.isMouseOver(mX, mY, pos[0], pos[1], BTN_W, BTN_H)) continue;
                this.handleMainMenuButtonClick(i);
                return true;
            }
            float[] lang = this.langPos(this.width);
            if (NeverloseMenu.isMouseOver(mX, mY, lang[0], lang[1], lang[2], lang[3])) {
                english = !english;
                return true;
            }
            for (int i = 0; i < SOCIAL_URLS.length; ++i) {
                float[] pos = this.socialPos(i, this.width, this.height);
                if (!NeverloseMenu.isMouseOver(mX, mY, pos[0], pos[1], SOCIAL_SIZE, SOCIAL_SIZE)) continue;
                try {
                    net.minecraft.util.Util.getOperatingSystem().open(new java.net.URI(SOCIAL_URLS[i]));
                }
                catch (Exception ignored) {
                    // Нет браузера или ссылка не открылась — меню не должно падать.
                }
                return true;
            }
            float[] exit = this.exitPos(this.width, this.height);
            if (NeverloseMenu.isMouseOver(mX, mY, exit[0], exit[1], exit[2], exit[3])) {
                this.client.scheduleStop();
                return true;
            }
            float[] mods = this.modsPos(this.width, this.height);
            if (NeverloseMenu.isMouseOver(mX, mY, mods[0], mods[1], mods[2], mods[3])) {
                this.client.setScreen(new ModsScreen(this));
                return true;
            }
        } else if (this.currentView == View.ALT_SCREEN) {
            return this.handleAltScreenClick(mX, mY, button);
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    private void handleMainMenuButtonClick(int idx) {
        switch (idx) {
            case 0: {
                this.client.setScreen(this.client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
                break;
            }
            case 1: {
                this.client.setScreen(new SelectWorldScreen(this));
                break;
            }
            case 2: {
                this.client.setScreen(new net.minecraft.client.gui.screen.option.OptionsScreen(this, this.client.options));
                break;
            }
            case 3: {
                this.currentView = View.ALT_SCREEN;
                break;
            }
        }
    }

    private void renderAltScreen(DrawContext context, MatrixStack matrix, int sw, int sh, float mX, float mY) {
        float tw = 405.0f;
        float th = 165.0f;
        float sx = (float)sw / 2.0f - tw / 2.0f;
        float sy = (float)sh / 2.0f - th / 2.0f;

        this.drawPanel(matrix, sx, sy, 100.0f, 100.0f, "Account Panel");
        rectangle.render(ShapeProperties.create(matrix, sx + 5.0f, sy + 38.0f, 73.0, 14.0).round(3.0f).thickness(1.0f).outlineColor(this.nicknameFieldFocused ? new Color(255, 255, 255, 120).getRGB() : new Color(255, 255, 255, 25).getRGB()).color(new Color(0, 0, 0, 215).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, sx + 80.0f, sy + 38.0f, 14.0, 14.0).round(3.0f).color(new Color(24, 24, 24, 230).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, sx + 5.0f, sy + 58.0f, 89.0, 16.0).round(3.0f).color(new Color(16, 16, 16, 230).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, sx + 5.0f, sy + 79.0f, 89.0, 16.0).round(3.0f).color(new Color(16, 16, 16, 230).getRGB()).build());
        Rectangle.flushPending();
        String nickLabel = this.nicknameText.isEmpty() ? "Enter nick..." : this.nicknameText;
        this.smallFont.drawString(matrix, nickLabel, sx + 9.0f, sy + 42.0f, this.nicknameText.isEmpty() ? new Color(115, 115, 115).getRGB() : -1);
        this.smallFont.drawCenteredString(matrix, "+", sx + 87.0f, sy + 42.0f, -1);
        this.smallFont.drawString(matrix, "Random", sx + 9.0f, sy + 63.0f, new Color(205, 205, 205).getRGB());
        this.smallFont.drawString(matrix, "Delete", sx + 9.0f, sy + 84.0f, new Color(205, 205, 205).getRGB());

        this.drawPanel(matrix, sx, sy + 105.0f, 100.0f, 58.0f, "Active Session");
        Account active = this.getActiveAccount();
        if (active != null) {
            this.drawAccountFace(context, active, sx + 8.0f, sy + 128.0f, 18.0f);
            this.smallFont.drawString(matrix, active.name, sx + 31.0f, sy + 130.0f, -1);
            this.smallFont.drawString(matrix, active.date == null ? "-" : active.date, sx + 31.0f, sy + 139.0f, new Color(125, 125, 125).getRGB());
        } else {
            this.smallFont.drawString(matrix, "No active session", sx + 8.0f, sy + 132.0f, new Color(125, 125, 125).getRGB());
        }

        this.drawPanel(matrix, sx + 105.0f, sy, 300.0f, 165.0f, "Accounts List");
        List<Account> accounts = this.accounts().accountList;
        float cardW = 143.0f;
        float cardH = 40.0f;
        float listX = sx + 110.0f;
        float listY = sy + 30.0f - this.scrollOffset;
        for (int i = 0; i < accounts.size(); ++i) {
            Account account = accounts.get(i);
            float cx = listX + (float)(i % 2) * (cardW + 4.0f);
            float cy = listY + (float)(i / 2) * (cardH + 4.0f);
            if (cy + cardH < sy + 26.0f || cy > sy + 165.0f) continue;
            boolean hovered = NeverloseMenu.isMouseOver(mX, mY, cx, cy, cardW, cardH);
            rectangle.render(ShapeProperties.create(matrix, cx, cy, cardW, cardH).round(4.0f).color(new Color(hovered ? 26 : 14, hovered ? 26 : 14, hovered ? 26 : 14, 230).getRGB()).build());
            Rectangle.flushPending();
            this.drawAccountFace(context, account, cx + 5.0f, cy + 6.0f, 28.0f);
            this.smallFont.drawString(matrix, account.name, cx + 39.0f, cy + 11.0f, -1);
            this.smallFont.drawString(matrix, account.date == null ? "-" : account.date, cx + 39.0f, cy + 22.0f, new Color(125, 125, 125).getRGB());
            rectangle.render(ShapeProperties.create(matrix, cx + cardW - 20.0f, cy + cardH / 2.0f - 7.0f, 14.0, 14.0).round(3.0f).color(new Color(30, 30, 30, 230).getRGB()).build());
            Rectangle.flushPending();
            this.smallFont.drawCenteredString(matrix, "x", cx + cardW - 13.0f, cy + cardH / 2.0f - 4.0f, new Color(190, 100, 100).getRGB());
        }
    }

    private void drawPanel(MatrixStack matrix, float x, float y, float w, float h, String title) {
        rectangle.render(ShapeProperties.create(matrix, x, y, w, h).round(5.0f).softness(2.0f).color(new Color(0, 0, 0, 225).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, x + 5.0f, y + 8.0f, 2.0, 9.0).round(1.0f).color(new Color(235, 235, 235, 235).getRGB()).build());
        Rectangle.flushPending();
        this.panelFont.drawString(matrix, title, x + 11.0f, y + 8.0f, -1);
    }

    private void drawAccountFace(DrawContext context, Account account, float x, float y, float size) {
        try {
            GameProfile profile = new GameProfile(UUID.fromString(account.uuid), account.name);
            Identifier skinTexture = net.minecraft.util.Identifier.of("textures/entity/player/wide/steve.png") /* TODO port: skin API */;
            if (skinTexture == null) {
                skinTexture = Identifier.of((String)"minecraft", (String)"textures/entity/steve.png");
            }
            Render2D.drawTexture(context, skinTexture, x, y, size, 4.0f, 8, 8, 64, ColorAssist.getRect(1.0f), -1);
        }
        catch (Throwable ignored) {
            // A malformed uuid must not take the whole menu down.
        }
    }

    private boolean handleAltScreenClick(float mX, float mY, int button) {
        if (button != 0) {
            return false;
        }
        float tw = 405.0f;
        float th = 165.0f;
        float sx = (float)this.width / 2.0f - tw / 2.0f;
        float sy = (float)this.height / 2.0f - th / 2.0f;
        if (NeverloseMenu.isMouseOver(mX, mY, sx + 5.0f, sy + 38.0f, 73.0f, 14.0f)) {
            this.nicknameFieldFocused = true;
            return true;
        }
        this.nicknameFieldFocused = false;
        if (NeverloseMenu.isMouseOver(mX, mY, sx + 80.0f, sy + 38.0f, 14.0f, 14.0f) && !this.nicknameText.isEmpty()) {
            this.addAccount(this.nicknameText);
            this.nicknameText = "";
            return true;
        }
        if (NeverloseMenu.isMouseOver(mX, mY, sx + 5.0f, sy + 58.0f, 89.0f, 16.0f)) {
            this.addAccount(this.generateRandomNickname());
            return true;
        }
        if (NeverloseMenu.isMouseOver(mX, mY, sx + 5.0f, sy + 79.0f, 89.0f, 16.0f)) {
            this.accounts().accountList.clear();
            this.saveAccounts();
            this.scrollOffset = 0.0f;
            return true;
        }
        List<Account> accounts = this.accounts().accountList;
        float cardW = 143.0f;
        float cardH = 40.0f;
        float listX = sx + 110.0f;
        float listY = sy + 30.0f - this.scrollOffset;
        for (int i = 0; i < accounts.size(); ++i) {
            float cx = listX + (float)(i % 2) * (cardW + 4.0f);
            float cy = listY + (float)(i / 2) * (cardH + 4.0f);
            if (NeverloseMenu.isMouseOver(mX, mY, cx + cardW - 20.0f, cy + cardH / 2.0f - 7.0f, 14.0f, 14.0f)) {
                accounts.remove(i);
                this.saveAccounts();
                return true;
            }
            if (!NeverloseMenu.isMouseOver(mX, mY, cx, cy, cardW, cardH)) continue;
            this.selectAccount(accounts.get(i));
            return true;
        }
        return false;
    }

    /** .jar, брошенные прямо на главное меню, открывают экран модов и сразу устанавливаются. */
    @Override
    public void onFilesDropped(List<Path> paths) {
        ModsScreen screen = new ModsScreen(this);
        this.client.setScreen(screen);
        screen.installFiles(paths);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.currentView == View.ALT_SCREEN) {
            this.currentView = View.MAIN_MENU;
            this.nicknameFieldFocused = false;
            return true;
        }
        if (this.nicknameFieldFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!this.nicknameText.isEmpty()) {
                this.nicknameText = this.nicknameText.substring(0, this.nicknameText.length() - 1);
            }
            return true;
        }
        return super.keyPressed(wiosKey);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput wiosChar) {
        char chr = (char) wiosChar.codepoint();
        int modifiers = wiosChar.modifiers();
        if (this.nicknameFieldFocused && this.nicknameText.length() < 16 && chr >= ' ') {
            this.nicknameText = this.nicknameText + chr;
            return true;
        }
        return super.charTyped(wiosChar);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.currentView == View.ALT_SCREEN) {
            this.scrollOffset = MathHelper.clamp((float)((float)((double)this.scrollOffset - verticalAmount * 15.0)), (float)0.0f, (float)Math.max(0.0f, (float)((this.accounts().accountList.size() + 1) / 2) * 44.0f - 130.0f));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private Account getActiveAccount() {
        for (Account account : this.accounts().accountList) {
            if (!account.active) continue;
            return account;
        }
        return null;
    }

    private void addAccount(String name) {
        String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
        Account account = new Account(name, false, false, null, offlineUuid, "0");
        account.date = LocalDateTime.now().format(DATE_FORMAT);
        this.accounts().accountList.add(account);
        this.selectAccount(account);
    }

    private void selectAccount(Account account) {
        for (Account other : this.accounts().accountList) {
            other.active = other == account;
        }
        this.setSession(account);
        this.saveAccounts();
    }

    private void setSession(Account account) {
        try {
            Session newSession = new Session(account.name, UUID.fromString(account.uuid), "0", Optional.empty(), Optional.empty());
            IMinecraftClient mca = (IMinecraftClient)((Object)MinecraftClient.getInstance());
            mca.setSessionT(newSession);
            /* TODO port 1.21.11: GameProfile.getProperties() недоступен */
            UserApiService apiService = UserApiService.OFFLINE;
            mca.setUserApiService(apiService);
            mca.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
            mca.setProfileKeys(ProfileKeys.create((UserApiService)apiService, (Session)newSession, (Path)MinecraftClient.getInstance().runDirectory.toPath()));
            mca.setAbuseReportContextT(AbuseReportContext.create((ReporterEnvironment)ReporterEnvironment.ofIntegratedServer(), (UserApiService)apiService));
        }
        catch (Throwable ignored) {
            // Session swapping is best effort; a failure must not break the menu.
        }
    }

    private void saveAccounts() {
        try {
            Wiosvisual.getInstance().getFileController().saveFile(AccountFile.class);
        }
        catch (Exception ignored) {
            // empty catch block
        }
    }

    private String generateRandomNickname() {
        StringBuilder sb = new StringBuilder();
        char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u'};
        char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};
        for (int i = 0; i < 8; ++i) {
            sb.append(i % 2 == 0 ? consonants[this.rand.nextInt(consonants.length)] : vowels[this.rand.nextInt(vowels.length)]);
        }
        return sb.substring(0, 1).toUpperCase() + sb.substring(1) + (this.rand.nextInt(90) + 10);
    }

    static int interpolateColor(int c1, int c2, float f) {
        int a = (int)MathHelper.lerp((float)f, (float)(c1 >> 24 & 0xFF), (float)(c2 >> 24 & 0xFF));
        int r = (int)MathHelper.lerp((float)f, (float)(c1 >> 16 & 0xFF), (float)(c2 >> 16 & 0xFF));
        int g = (int)MathHelper.lerp((float)f, (float)(c1 >> 8 & 0xFF), (float)(c2 >> 8 & 0xFF));
        int b = (int)MathHelper.lerp((float)f, (float)(c1 & 0xFF), (float)(c2 & 0xFF));
        return a << 24 | r << 16 | g << 8 | b;
    }

    static boolean isMouseOver(float mX, float mY, float x, float y, float w, float h) {
        return mX >= x && mX <= x + w && mY >= y && mY <= y + h;
    }
}
