/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_429
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_500
 *  net.minecraft.class_526
 *  org.joml.Vector4f
 */
package code.wiosvisual.display.screens.mainmenu;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.display.screens.mainmenu.AccountsScreen;
import code.wiosvisual.utils.client.text.TextAnimation;
import code.wiosvisual.utils.display.atlasfont.msdf.MsdfFont;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.render.HudShine;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.display.render.StartupBackgroundRenderer;
import code.wiosvisual.Wiosvisual;
import code.wiosvisual.utils.display.scissor.ScissorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.systemrender.builders.Builder;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadColorState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.SizeState;
import code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture;
import code.wiosvisual.utils.math.calc.Calculate;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.net.URI;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

public class MainMenu
extends Screen
implements QuickImports {
    public static MainMenu INSTANCE = new MainMenu();
    public int x;
    public int y;
    public int width;
    public int height;
    private static final Supplier<MsdfFont> ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons").data("icons").build());
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final long AUTH_TRANSITION_DURATION_MS = 700L;
    private final TextAnimation textAnimation = new TextAnimation();
    public boolean isDarkMode = true;
    public String themeIcon = "I";
    private long transitionStartTime = 0L;
    private final float transitionDuration = 0.35f;
    private boolean isTransitioning = false;
    private final Color lightBgColor = new Color(255, 255, 255, 255);
    private final Color lightButtonColor = new Color(128, 128, 128, 55);
    private final Color lightOutlineColor = new Color(86, 86, 86, 95);
    private final Color lightGradientColor = new Color(255, 255, 255, 95);
    private final Color lightTextColor = new Color(128, 128, 128, 255);
    private final Color darkBgColor = new Color(30, 30, 30, 255);
    private final Color darkButtonColor = new Color(50, 50, 50, 55);
    private final Color darkOutlineColor = new Color(100, 100, 100, 95);
    private final Color darkGradientColor = new Color(80, 80, 80, 95);
    private final Color darkTextColor = new Color(200, 200, 200, 255);
    private long authTransitionStartedAt = -1L;
    private static final Identifier SINGLE_IMG = BackgroundIcons.SINGLE;
    private static final Identifier MULTI_IMG = BackgroundIcons.MULTI;
    private static final Identifier SOLO_ICON = BackgroundIcons.SOLO;
    private static final Identifier MULTI_ICON = BackgroundIcons.MULTIPLAYER;
    private static final Identifier SETTINGS_ICON = BackgroundIcons.SETTINGS;
    private static final Identifier ACCOUNT_ICON = BackgroundIcons.ACCOUNT;
    private static final Identifier EXIT_ICON_ID = BackgroundIcons.EXIT;
    private final Identifier[] SOCIAL_ICONS = new Identifier[]{BackgroundIcons.DISCORD};
    private final String[] SOCIAL_URLS = new String[]{"https://discord.gg/TTVr5ztFXD"};
    private final float[] socialHover = new float[this.SOCIAL_ICONS.length];
    private float parallaxX = 0.0f;
    private float parallaxY = 0.0f;
    private float hoverSingle = 0.0f;
    private float hoverMulti = 0.0f;
    private float hoverSettings = 0.0f;
    private float hoverAccounts = 0.0f;
    private float hoverExit = 0.0f;
    private boolean exitSliding = false;
    private float exitSlideProgress = 0.0f;
    private double exitDragStartX = 0.0;
    private float exitStartProgress = 0.0f;
    private final Color cardButtonColor = new Color(9, 26, 68, 245);
    private final Color cardGradientColor = new Color(19, 50, 116, 245);
    private final Color cardOutlineColor = new Color(150, 185, 245, 130);
    private final Color cardOutlineHover = new Color(205, 227, 255, 220);
    private final Color cardTextColor = new Color(232, 238, 250, 255);

    public MainMenu() {
        super(Text.of((String)"MainMenu"));
    }

    public void startAuthTransition() {
        this.authTransitionStartedAt = System.currentTimeMillis();
    }

    public void tick() {
        super.tick();
        this.textAnimation.updateText();
    }

    private Color interpolateColor(Color start, Color end, float t) {
        int r = (int)((float)start.getRed() + (float)(end.getRed() - start.getRed()) * t);
        int g = (int)((float)start.getGreen() + (float)(end.getGreen() - start.getGreen()) * t);
        int b = (int)((float)start.getBlue() + (float)(end.getBlue() - start.getBlue()) * t);
        int a = (int)((float)start.getAlpha() + (float)(end.getAlpha() - start.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = GuiCompat.stack(context);
        this.x = window.getScaledWidth();
        this.y = window.getScaledHeight();
        this.width = window.getScaledWidth();
        this.height = window.getScaledHeight();
        float centerX = (float)this.width / 2.0f;
        float centerY = (float)this.height / 2.0f;
        Color bgColor = new Color(8, 22, 58, 238);
        Color buttonColor = this.cardButtonColor;
        Color gradientColor = this.cardGradientColor;
        Color outlineColor = this.cardOutlineColor;
        Color textColor = this.cardTextColor;
        this.renderBackgroundLayer(context, mouseX, mouseY, 1.0f);
        matrices.push();
        Calculate.scale(matrices, centerX, centerY, 1.0f, () -> {
            this.renderMainMenuContent(context, mouseX, mouseY, centerX, centerY, bgColor, buttonColor, gradientColor, outlineColor, textColor);
        });
        matrices.pop();
        this.renderAuthTransitionOverlay(context);
    }

    private void renderMainMenuContent(DrawContext context, int mouseX, int mouseY, float centerX, float centerY, Color bgColor, Color buttonColor, Color gradientColor, Color outlineColor, Color textColor) {
        MatrixStack matrices = GuiCompat.stack(context);
        float buttonWidth = 175.0f;
        float buttonHeight = 20.0f;
        float imageHeight = 46.0f;
        float colGap = 6.0f;
        float rowGap = 9.0f;
        float round = 5.0f;
        float cardHeight = buttonHeight + imageHeight;
        float gridHeight = cardHeight + rowGap + buttonHeight;
        float startY = centerY - gridHeight / 2.0f + 14.0f;
        float leftX = centerX - colGap / 2.0f - buttonWidth;
        float rightX = centerX + colGap / 2.0f;
        float y2 = startY + cardHeight + rowGap;
        float titleY = startY - 66.0f;
        this.drawShimmerTitle(matrices, "WiosVisuals", centerX, titleY);
        FontRenderer badgeFont = Fonts.getSize(14, Fonts.Type.DEFAULT);
        badgeFont.drawCenteredString(matrices, "FreeVersion", centerX, titleY + Fonts.getSize(34, Fonts.Type.BOLD).getStringHeight("WiosVisuals") + 2.0f, new Color(150, 185, 245, 220).getRGB());
        this.hoverSingle += ((this.isHover(mouseX, mouseY, leftX, startY, buttonWidth, cardHeight) ? 1.0f : 0.0f) - this.hoverSingle) * 0.2f;
        this.hoverMulti += ((this.isHover(mouseX, mouseY, rightX, startY, buttonWidth, cardHeight) ? 1.0f : 0.0f) - this.hoverMulti) * 0.2f;
        this.hoverSettings += ((this.isHover(mouseX, mouseY, leftX, y2, buttonWidth, buttonHeight) ? 1.0f : 0.0f) - this.hoverSettings) * 0.2f;
        this.hoverAccounts += ((this.isHover(mouseX, mouseY, rightX, y2, buttonWidth, buttonHeight) ? 1.0f : 0.0f) - this.hoverAccounts) * 0.2f;
        this.drawCard(context, leftX, startY, buttonWidth, buttonHeight, imageHeight, cardHeight, round, "SinglePlayer", SOLO_ICON, SINGLE_IMG, this.hoverSingle, buttonColor, gradientColor, outlineColor, textColor);
        this.drawCard(context, rightX, startY, buttonWidth, buttonHeight, imageHeight, cardHeight, round, "MultiPlayer", MULTI_ICON, MULTI_IMG, this.hoverMulti, buttonColor, gradientColor, outlineColor, textColor);
        this.drawSimpleButton(context, leftX, y2, buttonWidth, buttonHeight, round, "Settings", SETTINGS_ICON, this.hoverSettings, buttonColor, gradientColor, outlineColor, textColor);
        this.drawSimpleButton(context, rightX, y2, buttonWidth, buttonHeight, round, "Accounts", ACCOUNT_ICON, this.hoverAccounts, buttonColor, gradientColor, outlineColor, textColor);
        this.renderSlideExit(context, mouseX, mouseY, round, buttonColor, gradientColor, outlineColor, textColor);
        this.renderSocials(context, mouseX, mouseY);
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(matrices, "\u00a9 2026 WiosVisuals. All rights reserved.", centerX, this.height - 12, textColor.getRGB());
    }

    /**
     * Wordmark with a highlight sweeping through the letters: the text is drawn once in its base
     * tone, then again in white inside a narrow scissored band that travels across it.
     */
    private void drawShimmerTitle(MatrixStack matrices, String text, float centerX, float y) {
        FontRenderer font = Fonts.getSize(34, Fonts.Type.BOLD);
        font.drawCenteredString(matrices, text, centerX, y, new Color(186, 205, 238, 255).getRGB());
        float textWidth = font.getStringWidth(text);
        float textHeight = font.getStringHeight(text);
        float left = centerX - textWidth / 2.0f;
        float cycle = 7300.0f;
        float progress = (float)(System.currentTimeMillis() % (long)cycle) / cycle;
        float band = textWidth * 0.3f;
        float bandX = left - band + progress * (textWidth + band * 2.0f);
        ScissorAssist scissor = Wiosvisual.getInstance().getScissorManager();
        scissor.push(matrices.peek().getPositionMatrix(), bandX, y - 3.0f, band, textHeight + 6.0f);
        font.drawCenteredString(matrices, text, centerX, y, new Color(255, 255, 255, 255).getRGB());
        scissor.pop();
    }

    private void renderBackgroundLayer(DrawContext context, int mouseX, int mouseY, float menuReveal) {
        MatrixStack matrices = GuiCompat.stack(context);
        float margin = 30.0f;
        float maxParallax = 16.0f;
        float targetParallaxX = MathHelper.clamp((float)(-((float)mouseX / (float)this.width - 0.5f) * 2.0f * maxParallax), (float)(-maxParallax), (float)maxParallax);
        float targetParallaxY = MathHelper.clamp((float)(-((float)mouseY / (float)this.height - 0.5f) * 2.0f * maxParallax), (float)(-maxParallax), (float)maxParallax);
        this.parallaxX += (targetParallaxX - this.parallaxX) * 0.08f;
        this.parallaxY += (targetParallaxY - this.parallaxY) * 0.08f;
        this.parallaxX = MathHelper.clamp((float)this.parallaxX, (float)(-margin), (float)margin);
        this.parallaxY = MathHelper.clamp((float)this.parallaxY, (float)(-margin), (float)margin);
        float normalAlpha = MathHelper.clamp((float)menuReveal, (float)0.0f, (float)1.0f);
        if (normalAlpha <= 0.01f) {
            StartupBackgroundRenderer.drawStartupBackdrop(context, this.width, this.height);
            return;
        }
        this.drawFullscreenBackgroundTexture(matrices, StartupBackgroundRenderer.BACKGROUND, -margin + this.parallaxX, -margin + this.parallaxY, (float)this.width + margin * 2.0f, (float)this.height + margin * 2.0f, normalAlpha);
        rectangle.render(ShapeProperties.create(matrices, -1.0, -1.0, this.width + 2, this.height + 2).color(new Color(0, 0, 0, (int)(70.0f * normalAlpha)).getRGB()).build());
    }

    private void drawFullscreenBackgroundTexture(MatrixStack matrices, Identifier id, float x, float y, float w, float h, float alpha) {
        int glId = code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture.id(mc.getTextureManager().getTexture(id));
        ((BuiltTexture)Builder.texture().size(new SizeState(w, h)).radius(QuadRadiusState.NO_ROUND).color(QuadColorState.WHITE).smoothness(1.0f).texture(0.0f, 0.0f, 1.0f, 1.0f, glId).build()).render(matrices.peek().getPositionMatrix(), x, y, 0.0f);
    }

    private void renderAuthTransitionOverlay(DrawContext context) {
        if (this.authTransitionStartedAt < 0L) {
            return;
        }
        float progress = (float)(System.currentTimeMillis() - this.authTransitionStartedAt) / 700.0f;
        if (progress >= 1.0f) {
            this.authTransitionStartedAt = -1L;
            return;
        }
        float eased = 1.0f - (float)Math.pow(1.0f - MathHelper.clamp((float)progress, (float)0.0f, (float)1.0f), 3.0);
        int alpha = (int)(255.0f * (1.0f - eased));
        rectangle.render(ShapeProperties.create(GuiCompat.stack(context), 0.0, 0.0, this.width, this.height).color(new Color(0, 0, 0, alpha).getRGB()).build());
    }

    private boolean isHover(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= (double)x && mouseX <= (double)(x + w) && mouseY >= (double)y && mouseY <= (double)(y + h);
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    public boolean shouldPause() {
        return false;
    }

    private void drawRoundedTexture(MatrixStack matrices, Identifier id, float x, float y, float w, float h, QuadRadiusState radius) {
        int glId = code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture.id(mc.getTextureManager().getTexture(id));
        ((BuiltTexture)Builder.texture().size(new SizeState(w, h)).radius(radius).color(QuadColorState.WHITE).smoothness(1.0f).texture(0.0f, 0.0f, 1.0f, 1.0f, glId).build()).render(matrices.peek().getPositionMatrix(), x, y, 0.0f);
    }

    private void drawButtonLabel(MatrixStack matrices, float bx, float by, float bw, float bh, String label, Identifier icon, float iconSize, int textColor) {
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float textW = font.getStringWidth(label);
        float gap = 4.0f;
        float groupW = textW + gap + iconSize;
        float groupX = bx + (bw - groupW) / 2.0f;
        float textY = font.getCenteredY(by, bh, label);
        font.drawString(matrices, label, groupX, textY, textColor);
        float iconX = groupX + textW + gap;
        float iconY = by + (bh - iconSize) / 2.0f;
        this.drawRoundedTexture(matrices, icon, iconX, iconY, iconSize, iconSize, QuadRadiusState.NO_ROUND);
    }

    private void drawCard(DrawContext context, float cardX, float cardY, float buttonWidth, float buttonHeight, float imageHeight, float cardHeight, float round, String label, Identifier icon, Identifier cardImage, float hover, Color buttonColor, Color gradientColor, Color outlineColor, Color textColor) {
        MatrixStack matrices = GuiCompat.stack(context);
        float h = MathHelper.clamp((float)hover, (float)0.0f, (float)1.0f);
        float scale = 1.0f + 0.13f * h;
        float cx = cardX + buttonWidth / 2.0f;
        float cy = cardY + cardHeight / 2.0f;
        Calculate.scale(matrices, cx, cy, scale, () -> {
            this.drawRoundedTexture(matrices, cardImage, cardX, cardY + buttonHeight, buttonWidth, imageHeight, new QuadRadiusState(0.0f, round, round, 0.0f));
            rectangle.render(ShapeProperties.create(matrices, cardX, cardY, buttonWidth, buttonHeight).round(new Vector4f(round, 0.0f, round, 0.0f)).color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
            this.drawButtonLabel(matrices, cardX, cardY, buttonWidth, buttonHeight, label, icon, 11.0f, textColor.getRGB());
            Color outline = this.interpolateColor(outlineColor, this.cardOutlineHover, h);
            rectangle.render(ShapeProperties.create(matrices, cardX, cardY, buttonWidth, cardHeight).thickness(2.0f).round(round).outlineColor(outline.getRGB()).color(new Color(0, 0, 0, 0).getRGB()).build());
            Rectangle.flushPending();
            HudShine.render(matrices, cardX, cardY, buttonWidth, cardHeight, round, 1.0f, 0.45f, 0.42f);
        });
    }

    private void drawSimpleButton(DrawContext context, float bx, float by, float buttonWidth, float buttonHeight, float round, String label, Identifier icon, float hover, Color buttonColor, Color gradientColor, Color outlineColor, Color textColor) {
        MatrixStack matrices = GuiCompat.stack(context);
        float h = MathHelper.clamp((float)hover, (float)0.0f, (float)1.0f);
        float scale = 1.0f + 0.13f * h;
        float cx = bx + buttonWidth / 2.0f;
        float cy = by + buttonHeight / 2.0f;
        Calculate.scale(matrices, cx, cy, scale, () -> {
            Color outline = this.interpolateColor(outlineColor, this.cardOutlineHover, h);
            rectangle.render(ShapeProperties.create(matrices, bx, by, buttonWidth, buttonHeight).thickness(2.0f).round(round).outlineColor(outline.getRGB()).color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
            Rectangle.flushPending();
            HudShine.render(matrices, bx, by, buttonWidth, buttonHeight, round, 1.0f, 0.45f, 0.42f);
            this.drawButtonLabel(matrices, bx, by, buttonWidth, buttonHeight, label, icon, 11.0f, textColor.getRGB());
        });
    }

    private void renderSlideExit(DrawContext context, int mouseX, int mouseY, float round, Color buttonColor, Color gradientColor, Color outlineColor, Color textColor) {
        boolean hoveredBtn;
        MatrixStack matrices = GuiCompat.stack(context);
        float exW = 64.0f;
        float exH = 22.0f;
        float exX = 10.0f;
        float exY = (float)this.height - exH - 10.0f;
        float pad = 3.0f;
        float circleD = exH - pad * 2.0f;
        float maxTravel = exW - circleD - pad * 2.0f;
        if (!this.exitSliding && this.exitSlideProgress > 0.0f && this.exitSlideProgress < 0.985f) {
            this.exitSlideProgress += (0.0f - this.exitSlideProgress) * 0.25f;
            if (this.exitSlideProgress < 0.01f) {
                this.exitSlideProgress = 0.0f;
            }
        }
        this.hoverExit += (((hoveredBtn = this.isHover(mouseX, mouseY, exX, exY, exW, exH)) || this.exitSliding ? 1.0f : 0.0f) - this.hoverExit) * 0.2f;
        float h = MathHelper.clamp((float)this.hoverExit, (float)0.0f, (float)1.0f);
        float scale = 1.0f + 0.13f * h;
        float circleX = exX + pad + this.exitSlideProgress * maxTravel;
        float circleY = exY + pad;
        Calculate.scale(matrices, exX + exW / 2.0f, exY + exH / 2.0f, scale, () -> {
            Color outline = this.interpolateColor(outlineColor, this.cardOutlineHover, h);
            rectangle.render(ShapeProperties.create(matrices, exX, exY, exW, exH).thickness(2.0f).round(round).outlineColor(outline.getRGB()).color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
            Rectangle.flushPending();
            HudShine.render(matrices, exX, exY, exW, exH, round, 1.0f, 0.45f, 0.42f);
            if (this.exitSlideProgress > 0.001f) {
                int a = (int)(120.0f + this.exitSlideProgress * 120.0f);
                rectangle.render(ShapeProperties.create(matrices, exX + pad, circleY, circleX - exX - pad + circleD, circleD).round(circleD / 2.0f).color(new Color(210, 60, 60, a).getRGB()).build());
            }
            FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
            String label = "Exit";
            float iconSize = 9.0f;
            float gap = 4.0f;
            float textW = font.getStringWidth(label);
            float groupW = textW + gap + iconSize;
            float groupX = exX + (exW - groupW) / 2.0f;
            float textY = font.getCenteredY(exY, exH, label);
            font.drawString(matrices, label, groupX, textY, textColor.getRGB());
            float iconY = exY + (exH - iconSize) / 2.0f;
            this.drawRoundedTexture(matrices, EXIT_ICON_ID, groupX + textW + gap, iconY, iconSize, iconSize, QuadRadiusState.NO_ROUND);
            float circleVis = Math.max(this.hoverExit, this.exitSliding ? 1.0f : 0.0f);
            if (circleVis > 0.01f) {
                Color base = this.exitSlideProgress > 0.001f ? new Color(220, 70, 70) : new Color(205, 205, 205);
                int a = (int)(235.0f * MathHelper.clamp((float)circleVis, (float)0.0f, (float)1.0f));
                rectangle.render(ShapeProperties.create(matrices, circleX, circleY, circleD, circleD).round(circleD / 2.0f).color(new Color(base.getRed(), base.getGreen(), base.getBlue(), a).getRGB()).build());
            }
        });
    }

    private float socialIconSize() {
        return 26.0f;
    }

    private float socialGap() {
        return 12.0f;
    }

    private float socialX() {
        return 14.0f;
    }

    private float socialY(int i) {
        float iconSize = this.socialIconSize();
        float gap = this.socialGap();
        float step = iconSize + gap;
        float blockH = (float)this.SOCIAL_ICONS.length * iconSize + (float)(this.SOCIAL_ICONS.length - 1) * gap;
        float top = (float)this.height / 2.0f - blockH / 2.0f;
        return top + (float)i * step;
    }

    private void renderSocials(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = GuiCompat.stack(context);
        float iconSize = this.socialIconSize();
        float sx = this.socialX();
        for (int i = 0; i < this.SOCIAL_ICONS.length; ++i) {
            float sy = this.socialY(i);
            boolean hovered = this.isHover(mouseX, mouseY, sx, sy, iconSize, iconSize);
            int n = i;
            this.socialHover[n] = this.socialHover[n] + ((hovered ? 1.0f : 0.0f) - this.socialHover[i]) * 0.2f;
            float h = MathHelper.clamp((float)this.socialHover[i], (float)0.0f, (float)1.0f);
            float draw = iconSize + 4.0f * h;
            float off = (draw - iconSize) / 2.0f;
            float round = draw * 0.28f;
            this.drawRoundedTexture(matrices, this.SOCIAL_ICONS[i], sx - off, sy - off, draw, draw, new QuadRadiusState(round));
            if (!(h > 0.01f)) continue;
            rectangle.render(ShapeProperties.create(matrices, sx - off, sy - off, draw, draw).thickness(1.5f).round(round).outlineColor(new Color(255, 255, 255, (int)(200.0f * h)).getRGB()).color(new Color(0, 0, 0, 0).getRGB()).build());
        }
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        float centerX = (float)this.width / 2.0f;
        float centerY = (float)this.height / 2.0f;
        float buttonWidth = 175.0f;
        float buttonHeight = 20.0f;
        float imageHeight = 46.0f;
        float colGap = 6.0f;
        float rowGap = 9.0f;
        float cardHeight = buttonHeight + imageHeight;
        float gridHeight = cardHeight + rowGap + buttonHeight;
        float startY = centerY - gridHeight / 2.0f + 14.0f;
        float leftX = centerX - colGap / 2.0f - buttonWidth;
        float rightX = centerX + colGap / 2.0f;
        float y2 = startY + cardHeight + rowGap;
        float exW = 64.0f;
        float exH = 22.0f;
        float pad = 3.0f;
        float exX = 10.0f;
        float exY = (float)this.height - exH - 10.0f;
        float circleD = exH - pad * 2.0f;
        float maxTravel = exW - circleD - pad * 2.0f;
        float circleX = exX + pad + this.exitSlideProgress * maxTravel;
        float circleY = exY + pad;
        if (button == 0 && this.isHover(mouseX, mouseY, circleX, circleY, circleD, circleD)) {
            this.exitSliding = true;
            this.exitDragStartX = mouseX;
            this.exitStartProgress = this.exitSlideProgress;
            return true;
        }
        if (button == 0) {
            float socialSize = this.socialIconSize();
            float socialPosX = this.socialX();
            for (int i = 0; i < this.SOCIAL_ICONS.length; ++i) {
                if (!this.isHover(mouseX, mouseY, socialPosX, this.socialY(i), socialSize, socialSize)) continue;
                try {
                    Util.getOperatingSystem().open(new URI(this.SOCIAL_URLS[i]));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        if (button == 0) {
            if (this.isHover(mouseX, mouseY, leftX, startY, buttonWidth, cardHeight)) {
                mc.setScreen((Screen)new SelectWorldScreen((Screen)this));
                return true;
            }
            if (this.isHover(mouseX, mouseY, rightX, startY, buttonWidth, cardHeight)) {
                mc.setScreen((Screen)new MultiplayerScreen((Screen)this));
                return true;
            }
            if (this.isHover(mouseX, mouseY, leftX, y2, buttonWidth, buttonHeight)) {
                mc.setScreen((Screen)new OptionsScreen((Screen)this, MainMenu.mc.options));
                return true;
            }
            if (this.isHover(mouseX, mouseY, rightX, y2, buttonWidth, buttonHeight)) {
                mc.setScreen((Screen)new AccountsScreen(this));
                return true;
            }
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    public boolean mouseDragged(net.minecraft.client.gui.Click wiosClick, double deltaX, double deltaY) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.exitSliding && button == 0) {
            float exW = 64.0f;
            float exH = 22.0f;
            float pad = 3.0f;
            float circleD = exH - pad * 2.0f;
            float maxTravel = exW - circleD - pad * 2.0f;
            float delta = (float)(mouseX - this.exitDragStartX);
            this.exitSlideProgress = MathHelper.clamp((float)(this.exitStartProgress + delta / maxTravel), (float)0.0f, (float)1.0f);
            if (this.exitSlideProgress >= 0.985f) {
                this.exitSliding = false;
                mc.stop();
            }
            return true;
        }
        return super.mouseDragged(wiosClick, deltaX, deltaY);
    }

    public boolean mouseReleased(net.minecraft.client.gui.Click wiosClick) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.exitSliding && button == 0) {
            this.exitSliding = false;
            if (this.exitSlideProgress >= 0.985f) {
                mc.stop();
            }
            return true;
        }
        return super.mouseReleased(wiosClick);
    }
}
