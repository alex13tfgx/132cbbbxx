/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.minecraft.UserApiService
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_320
 *  net.minecraft.class_320$class_321
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_5520
 *  net.minecraft.class_7569
 *  net.minecraft.class_7574
 *  net.minecraft.class_7853
 *  org.joml.Matrix4f
 *  org.lwjgl.glfw.GLFW
 */
package code.wiosvisual.display.screens.mainmenu;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.animation.Easy.Direction;
import code.wiosvisual.common.animation.Easy.EaseBackIn;
import code.wiosvisual.mixins.IMinecraftClient;
import code.wiosvisual.utils.client.managers.file.impl.AccountFile;
import code.wiosvisual.utils.client.managers.file.impl.account.Account;
import code.wiosvisual.utils.client.managers.file.impl.account.AccountRepository;
import code.wiosvisual.utils.display.atlasfont.msdf.MsdfFont;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.HudShine;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.display.render.StartupBackgroundRenderer;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.systemrender.builders.Builder;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadColorState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.QuadRadiusState;
import code.wiosvisual.utils.display.render.systemrender.builders.states.SizeState;
import code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltText;
import code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture;
import code.wiosvisual.utils.display.scissor.ScissorAssist;
import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class AccountsScreen
extends Screen
implements QuickImports {
    static final float PANEL_WIDTH = 260.0f;
    static final float PANEL_HEIGHT = 340.0f;
    static final float ACCOUNT_SPACING = 30.0f;
    static final int MIN_LENGTH = 3;
    static final int MAX_LENGTH = 16;
    private static final Supplier<MsdfFont> ICONS_FONT_MAIN = Suppliers.memoize(() -> MsdfFont.builder().atlas("iconmain").data("iconmain").build());
    private final Screen parent;
    private final AccountRepository accountRepository = Wiosvisual.getInstance().getAccountRepository();
    private final Random rand = new Random();
    private final EaseBackIn openAnimation = new EaseBackIn(400, 1.0, 1.15f);
    private String currentAccount = "None";
    private boolean typing;
    private String typedText = "";
    private int cursorPos;
    private int selStart = -1;
    private int selEnd = -1;
    private long lastClick;
    private float textXOffset;
    private boolean dragging;
    private float scroll;
    private float smoothedScroll;
    private float parallaxX;
    private float parallaxY;
    private final Color bgColor = new Color(16, 16, 18, 245);
    private final Color fillColor = new Color(30, 30, 33, 255);
    private final Color outlineColor = new Color(70, 70, 76, 200);
    private final Color textColor = new Color(232, 238, 250, 255);

    public AccountsScreen(Screen parent) {
        super(Text.of((String)"Accounts"));
        this.parent = parent;
        this.openAnimation.setDirection(Direction.FORWARDS);
        this.openAnimation.reset();
    }

    static float panelX(int screenWidth) {
        return (float)screenWidth / 2.0f - 130.0f;
    }

    static float panelY(int screenHeight) {
        return (float)screenHeight / 2.0f - 170.0f;
    }

    protected void init() {
        super.init();
        if (mc.getSession() != null) {
            this.currentAccount = mc.getSession().getUsername();
        }
    }

    public void tick() {
        super.tick();
        for (Account account : this.accountRepository.accountList) {
            float target = account.starred ? 1.0f : 0.0f;
            account.starAnim += (target - account.starAnim) * 0.2f;
        }
    }

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundLayer(context, mouseX, mouseY);
        float anim = MathHelper.clamp((float)((float)this.openAnimation.getOutput()), (float)0.0f, (float)1.0f);
        float panelX = AccountsScreen.panelX(this.width);
        float panelY = AccountsScreen.panelY(this.height);
        MatrixStack matrices = GuiCompat.stack(context);
        matrices.push();
        this.renderPanel(context, panelX, panelY);
        matrices.pop();
    }

    private void renderBackgroundLayer(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = GuiCompat.stack(context);
        float margin = 30.0f;
        float maxParallax = 16.0f;
        float targetParallaxX = MathHelper.clamp((float)(-((float)mouseX / (float)this.width - 0.5f) * 2.0f * maxParallax), (float)(-maxParallax), (float)maxParallax);
        float targetParallaxY = MathHelper.clamp((float)(-((float)mouseY / (float)this.height - 0.5f) * 2.0f * maxParallax), (float)(-maxParallax), (float)maxParallax);
        this.parallaxX += (targetParallaxX - this.parallaxX) * 0.08f;
        this.parallaxY += (targetParallaxY - this.parallaxY) * 0.08f;
        this.parallaxX = MathHelper.clamp((float)this.parallaxX, (float)(-margin), (float)margin);
        this.parallaxY = MathHelper.clamp((float)this.parallaxY, (float)(-margin), (float)margin);
        int strips = 32;
        float sh = (float)this.height / strips + 1.0f;
        for (int i = 0; i < strips; i++) {
            float t = (float)i / (strips - 1);
            int c = (int)(10 + 20 * (1.0f - Math.abs(t - 0.45f) * 1.6f > 0 ? 1.0f - Math.abs(t - 0.45f) * 1.6f : 0.0f));
            rectangle.render(ShapeProperties.create(matrices, 0.0, (double)(i * (float)this.height / strips), this.width, sh).color(new Color(c, c, c + 2, 255).getRGB()).build());
        }
    }

    private void drawFullscreenBackgroundTexture(MatrixStack matrices, Identifier id, float x, float y, float w, float h, float alpha) {
        int glId = code.wiosvisual.utils.display.render.systemrender.renderers.impl.BuiltTexture.id(mc.getTextureManager().getTexture(id));
        ((BuiltTexture)Builder.texture().size(new SizeState(w, h)).radius(QuadRadiusState.NO_ROUND).color(new QuadColorState(0xFF16161C)).smoothness(1.0f).texture(0.0f, 0.0f, 1.0f, 1.0f, glId).build()).render(matrices.peek().getPositionMatrix(), x, y, 0.0f);
    }

    private void renderPanel(DrawContext context, float panelX, float panelY) {
        MatrixStack matrices = GuiCompat.stack(context);
        rectangle.render(ShapeProperties.create(matrices, panelX + 130.0f - 30.0f, panelY - 18.0f, 60.0, 16.0).color(this.bgColor.getRGB()).round(3.0f).build());
        Fonts.getSize(18, Fonts.Type.DEFAULT).drawCenteredString(matrices, "Accounts", panelX + 130.0f, panelY - 14.0f, this.textColor.getRGB());
        rectangle.render(ShapeProperties.create(matrices, panelX, panelY, 260.0, 340.0).color(this.bgColor.getRGB()).round(8.0f).build());
        float listY = panelY + 10.0f;
        float listHeight = 248.0f;
        float inputY = panelY + 340.0f - 58.0f;
        float inputX = panelX + 10.0f;
        float inputW = 240.0f;
        float textFieldW = inputW - 34.0f;
        this.solidOutlinedRect(matrices, inputX, inputY, textFieldW, 26.0f, 6.0f);
        this.solidOutlinedRect(matrices, inputX + textFieldW + 6.0f, inputY + 3.0f, 22.0f, 22.0f, 5.0f);
        ((BuiltText)Builder.text().font(ICONS_FONT_MAIN.get()).text("D").size(16.0f).color(this.textColor.getRGB()).build()).render(matrices.peek().getPositionMatrix(), inputX + textFieldW + 10.0f, inputY + 5.0f);
        this.renderTextField(context, inputX + 4.0f, inputY + 5.0f, textFieldW - 8.0f, 22.0f);
        this.solidOutlinedRect(matrices, panelX + 10.0f, panelY + 340.0f - 24.0f, 240.0f, 18.0f, 5.0f);
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(matrices, "Back", panelX + 130.0f, panelY + 340.0f - 19.0f, this.textColor.getRGB());
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        ScissorAssist scissor = Wiosvisual.getInstance().getScissorManager();
        scissor.push(positionMatrix, panelX + 8.0f, listY, 244.0f, listHeight);
        float maxScrollClamp = Math.max(0.0f, (float)this.accountRepository.accountList.size() * 30.0f - listHeight);
        this.scroll = MathHelper.clamp((float)this.scroll, (float)0.0f, (float)maxScrollClamp);
        this.smoothedScroll = MathHelper.lerp((float)0.1f, (float)this.smoothedScroll, (float)this.scroll);
        for (int i = 0; i < this.accountRepository.accountList.size(); ++i) {
            Account account = this.accountRepository.accountList.get(i);
            float accY = panelY + 14.0f + (float)i * 30.0f - this.smoothedScroll;
            if (accY + 26.0f < listY || accY > listY + listHeight) continue;
            this.solidOutlinedRect(matrices, panelX + 10.0f, accY, 240.0f, 26.0f, 6.0f);
            this.solidOutlinedRect(matrices, panelX + 14.0f, accY + 3.0f, 20.0f, 20.0f, 10.0f);
            Color starColor = this.interpolateColor(this.textColor, new Color(255, 255, 0, this.textColor.getAlpha()), account.starAnim);
            Fonts.getSize(28, Fonts.Type.ICONS).drawString(matrices, "S", panelX + 260.0f - 36.0f, accY + 8.0f, starColor.getRGB());
            this.drawAccountFace(context, account, panelX + 15.0f, accY + 4.5f);
            Fonts.getSize(16, Fonts.Type.SEMI).drawString(matrices, account.name, panelX + 40.0f, accY + 10.0f, this.textColor.getRGB());
        }
        scissor.pop();
        if (this.accountRepository.accountList.size() > 7) {
            this.renderScrollbar(matrices, panelX, listY, listHeight);
        }
        String currentText = "Current account - " + this.currentAccount;
        FontRenderer font = Fonts.getSize(16, Fonts.Type.SEMI);
        float currentWidth = font.getStringWidth(currentText) + 24.0f;
        float currentX = panelX + 130.0f - currentWidth / 2.0f;
        rectangle.render(ShapeProperties.create(matrices, currentX, panelY + 340.0f + 8.0f, currentWidth, 16.0).color(this.bgColor.getRGB()).round(3.0f).build());
        font.drawCenteredString(matrices, currentText, panelX + 130.0f, panelY + 340.0f + 12.0f, this.textColor.getRGB());
    }

    private void solidOutlinedRect(MatrixStack matrices, float x, float y, float w, float h, float round) {
        rectangle.render(ShapeProperties.create(matrices, x, y, w, h).thickness(2.0f).round(round).outlineColor(this.outlineColor.getRGB()).color(this.fillColor.getRGB()).build());
        Rectangle.flushPending();
        
    }

    private void renderTextField(DrawContext context, float textFieldX, float textFieldY, float textFieldWidth, float textFieldHeight) {
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        boolean blink = System.currentTimeMillis() % 1000L < 500L;
        context.enableScissor((int)textFieldX, (int)textFieldY, (int)(textFieldX + textFieldWidth), (int)(textFieldY + textFieldHeight));
        if (this.typing && this.hasSelection()) {
            int start = Math.min(this.selStart, this.selEnd);
            int end = Math.max(this.selStart, this.selEnd);
            float selXStart = textFieldX + 2.0f - this.textXOffset + font.getStringWidth(this.typedText.substring(0, start));
            float selWidth = font.getStringWidth(this.typedText.substring(start, end));
            rectangle.render(ShapeProperties.create(GuiCompat.stack(context), selXStart, textFieldY + 1.0f, selWidth, textFieldHeight - 2.0f).color(-11172376).build());
        }
        if (!this.typedText.isEmpty() || this.typing) {
            font.drawString(GuiCompat.stack(context), this.typedText, textFieldX + 2.0f - this.textXOffset, textFieldY + 6.0f, this.textColor.getRGB());
        } else {
            font.drawString(GuiCompat.stack(context), "Typing nickname", textFieldX + 2.0f, textFieldY + 6.0f, this.textColor.getRGB());
        }
        if (this.typing && blink && !this.hasSelection()) {
            float cursorX = textFieldX + 2.0f - this.textXOffset + font.getStringWidth(this.typedText.substring(0, this.cursorPos));
            rectangle.render(ShapeProperties.create(GuiCompat.stack(context), cursorX, textFieldY + 3.0f, 0.5, textFieldHeight - 6.0f).color(this.textColor.getRGB()).build());
        }
        context.disableScissor();
    }

    private void renderScrollbar(MatrixStack matrices, float panelX, float listY, float listHeight) {
        float contentHeight = (float)this.accountRepository.accountList.size() * 30.0f;
        float maxScroll = Math.max(0.0f, contentHeight - listHeight);
        this.scroll = MathHelper.clamp((float)this.scroll, (float)0.0f, (float)maxScroll);
        float scrollbarWidth = 3.0f;
        float scrollbarX = panelX + 260.0f - scrollbarWidth - 12.0f;
        float scrollbarY = listY + 1.0f;
        float scrollbarHeight = listHeight - 2.0f;
        rectangle.render(ShapeProperties.create(matrices, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight).round(1.0f).color(new Color(10, 10, 12, 140).getRGB()).build());
        float handleHeight = Math.max(24.0f, listHeight * (listHeight / (contentHeight + listHeight)));
        float scrollRatio = maxScroll > 0.0f ? this.smoothedScroll / maxScroll : 0.0f;
        float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;
        rectangle.render(ShapeProperties.create(matrices, scrollbarX, handleY, scrollbarWidth, handleHeight).round(1.0f).color(new Color(130, 130, 138, 190).getRGB()).build());
    }

    private void drawAccountFace(DrawContext context, Account account, float x, float y) {
        GameProfile profile = new GameProfile(UUID.fromString(account.uuid), account.name);
        Identifier skinTexture = net.minecraft.util.Identifier.of("textures/entity/player/wide/steve.png") /* TODO port: skin API */;
        if (skinTexture == null) {
            skinTexture = Identifier.of((String)"minecraft", (String)"textures/entity/steve.png");
        }
        Render2D.drawTexture(context, skinTexture, x, y, 18.0f, 8.0f, 8, 8, 64, ColorAssist.getRect(1.0f), -1);
    }

    private Color interpolateColor(Color start, Color end, float t) {
        int r = (int)((float)start.getRed() + (float)(end.getRed() - start.getRed()) * t);
        int g = (int)((float)start.getGreen() + (float)(end.getGreen() - start.getGreen()) * t);
        int b = (int)((float)start.getBlue() + (float)(end.getBlue() - start.getBlue()) * t);
        int a = (int)((float)start.getAlpha() + (float)(end.getAlpha() - start.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    private boolean isHover(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= (double)x && mouseX <= (double)(x + w) && mouseY >= (double)y && mouseY <= (double)(y + h);
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.openAnimation.getOutput() < (double)0.95f) {
            return true;
        }
        float panelX = AccountsScreen.panelX(this.width);
        float panelY = AccountsScreen.panelY(this.height);
        float inputY = panelY + 340.0f - 58.0f;
        float inputX = panelX + 10.0f;
        float inputW = 240.0f;
        float textFieldW = inputW - 34.0f;
        if (button == 0 && this.isHover(mouseX, mouseY, panelX + 10.0f, panelY + 340.0f - 24.0f, 240.0f, 18.0f)) {
            mc.setScreen(this.parent);
            return true;
        }
        if (button == 0 && this.isHover(mouseX, mouseY, inputX + 4.0f, inputY + 5.0f, textFieldW - 8.0f, 22.0f)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastClick < 250L) {
                this.selStart = 0;
                this.selEnd = this.typedText.length();
            } else {
                this.typing = true;
                this.selStart = this.cursorPos = this.getCursorIndexAt(mouseX);
                this.selEnd = this.cursorPos;
                this.lastClick = currentTime;
            }
            this.dragging = true;
            return true;
        }
        this.typing = false;
        this.clearSelection();
        if (button == 0 && this.isHover(mouseX, mouseY, inputX + textFieldW + 6.0f, inputY + 3.0f, 22.0f, 22.0f)) {
            this.addRandomAccount();
            return true;
        }
        float listY = panelY + 10.0f;
        float listHeight = 248.0f;
        for (int i = 0; i < this.accountRepository.accountList.size(); ++i) {
            float accY = panelY + 14.0f + (float)i * 30.0f - this.smoothedScroll;
            if (accY + 26.0f < listY || accY > listY + listHeight) continue;
            float starX = panelX + 260.0f - 36.0f;
            if (button == 0 && this.isHover(mouseX, mouseY, starX, accY + 8.0f, 18.0f, 18.0f)) {
                Account account = this.accountRepository.accountList.get(i);
                account.starred = !account.starred;
                this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                this.saveAccounts();
                return true;
            }
            if (!this.isHover(mouseX, mouseY, panelX + 10.0f, accY, 240.0f, 26.0f)) continue;
            if (button == 0) {
                this.selectAccount(this.accountRepository.accountList.get(i));
            } else if (button == 1) {
                this.accountRepository.accountList.remove(i);
                this.saveAccounts();
            }
            return true;
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        float panelX = AccountsScreen.panelX(this.width);
        float panelY = AccountsScreen.panelY(this.height);
        float listY = panelY + 10.0f;
        float listHeight = 248.0f;
        float contentHeight = (float)this.accountRepository.accountList.size() * 30.0f;
        float maxScroll = Math.max(0.0f, contentHeight - listHeight);
        if (maxScroll > 0.0f && this.isHover(mouseX, mouseY, panelX + 8.0f, listY, 244.0f, listHeight)) {
            this.scroll = (float)((double)this.scroll - vertical * 24.0);
            this.scroll = MathHelper.clamp((float)this.scroll, (float)0.0f, (float)maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    public boolean mouseDragged(net.minecraft.client.gui.Click wiosClick, double deltaX, double deltaY) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.dragging && button == 0) {
            this.selEnd = this.cursorPos = this.getCursorIndexAt(mouseX);
            return true;
        }
        return super.mouseDragged(wiosClick, deltaX, deltaY);
    }

    public boolean mouseReleased(net.minecraft.client.gui.Click wiosClick) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        this.dragging = false;
        return super.mouseReleased(wiosClick);
    }

    public boolean charTyped(net.minecraft.client.input.CharInput wiosChar) {
        char chr = (char) wiosChar.codepoint();
        int modifiers = wiosChar.modifiers();
        if (this.typing && this.typedText.length() < 16) {
            this.deleteSelectedText();
            this.typedText = this.typedText.substring(0, this.cursorPos) + chr + this.typedText.substring(this.cursorPos);
            ++this.cursorPos;
            this.clearSelection();
            this.updateTextXOffset();
            return true;
        }
        return super.charTyped(wiosChar);
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        if (this.typing) {
            if ((net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow(), 341) || net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow(), 345))) {
                switch (keyCode) {
                    case 65: {
                        this.selStart = 0;
                        this.selEnd = this.typedText.length();
                        return true;
                    }
                    case 67: {
                        if (this.hasSelection()) {
                            GLFW.glfwSetClipboardString((long)mc.getWindow().getHandle(), (CharSequence)this.getSelectedText());
                        }
                        return true;
                    }
                    case 86: {
                        String clipboard = GLFW.glfwGetClipboardString((long)mc.getWindow().getHandle());
                        if (clipboard != null) {
                            this.deleteSelectedText();
                            this.typedText = this.typedText.substring(0, this.cursorPos) + clipboard + this.typedText.substring(this.cursorPos);
                            this.cursorPos += clipboard.length();
                            this.clearSelection();
                            this.updateTextXOffset();
                        }
                        return true;
                    }
                }
            }
            switch (keyCode) {
                case 259: {
                    if (this.hasSelection()) {
                        this.deleteSelectedText();
                    } else if (this.cursorPos > 0) {
                        this.typedText = this.typedText.substring(0, this.cursorPos - 1) + this.typedText.substring(this.cursorPos);
                        --this.cursorPos;
                    }
                    this.updateTextXOffset();
                    return true;
                }
                case 263: {
                    if (this.cursorPos > 0) {
                        --this.cursorPos;
                    }
                    this.updateSelectionAfterMove();
                    this.updateTextXOffset();
                    return true;
                }
                case 262: {
                    if (this.cursorPos < this.typedText.length()) {
                        ++this.cursorPos;
                    }
                    this.updateSelectionAfterMove();
                    this.updateTextXOffset();
                    return true;
                }
                case 257: {
                    if (this.typedText.length() >= 3 && this.typedText.length() <= 16) {
                        this.addAccount(this.typedText);
                    }
                    return true;
                }
            }
        }
        if (keyCode == 256) {
            mc.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(wiosKey);
    }

    private void addAccount(String name) {
        String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
        this.accountRepository.accountList.add(new Account(name, false, false, null, offlineUuid, "0"));
        this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
        this.saveAccounts();
        this.typedText = "";
        this.cursorPos = 0;
        this.typing = false;
        this.clearSelection();
    }

    private void addRandomAccount() {
        String finalUsername = null;
        StringBuilder username = new StringBuilder();
        char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u'};
        char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};
        int attempts = 0;
        do {
            username.setLength(0);
            int length = 6 + this.rand.nextInt(5);
            boolean startWithVowel = this.rand.nextBoolean();
            for (int i = 0; i < length; ++i) {
                if (i % 2 == 0) {
                    username.append(startWithVowel ? vowels[this.rand.nextInt(vowels.length)] : consonants[this.rand.nextInt(consonants.length)]);
                    continue;
                }
                username.append(startWithVowel ? consonants[this.rand.nextInt(consonants.length)] : vowels[this.rand.nextInt(vowels.length)]);
            }
            if (this.rand.nextInt(100) < 30) {
                username.append(this.rand.nextInt(100));
            }
            String tempUsername = username.substring(0, 1).toUpperCase() + username.substring(1);
            ++attempts;
            if (!this.accountRepository.accountList.stream().noneMatch(account -> account.name.equals(tempUsername))) continue;
            finalUsername = tempUsername;
            break;
        } while (attempts < 10);
        if (finalUsername == null) {
            finalUsername = username.substring(0, 1).toUpperCase() + username.substring(1) + System.currentTimeMillis() % 1000L;
        }
        this.addAccount(finalUsername);
    }

    private void selectAccount(Account account) {
        for (Account other : this.accountRepository.accountList) {
            other.active = other == account;
        }
        this.currentAccount = account.name;
        this.setSession(account);
        this.saveAccounts();
    }

    private void saveAccounts() {
        try {
            Wiosvisual.getInstance().getFileController().saveFile(AccountFile.class);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void setSession(Account account) {
        Session newSession = new Session(account.name, UUID.fromString(account.uuid), "0", Optional.empty(), Optional.empty());
        IMinecraftClient mca = (IMinecraftClient)MinecraftClient.getInstance();
        mca.setSessionT(newSession);
        /* TODO(port 1.21.11): GameProfile.getProperties() недоступен — очистка кэша скина пропущена. */
        UserApiService apiService = UserApiService.OFFLINE;
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
        mca.setProfileKeys(ProfileKeys.create((UserApiService)apiService, (Session)newSession, (Path)MinecraftClient.getInstance().runDirectory.toPath()));
        mca.setAbuseReportContextT(AbuseReportContext.create((ReporterEnvironment)ReporterEnvironment.ofIntegratedServer(), (UserApiService)apiService));
    }

    private boolean hasSelection() {
        return this.selStart != this.selEnd;
    }

    private String getSelectedText() {
        int start = Math.min(this.selStart, this.selEnd);
        int end = Math.max(this.selStart, this.selEnd);
        return this.typedText.substring(start, end);
    }

    private void deleteSelectedText() {
        if (this.hasSelection()) {
            int start = Math.min(this.selStart, this.selEnd);
            int end = Math.max(this.selStart, this.selEnd);
            this.typedText = this.typedText.substring(0, start) + this.typedText.substring(end);
            this.cursorPos = start;
            this.clearSelection();
        }
    }

    private void clearSelection() {
        this.selStart = this.cursorPos;
        this.selEnd = this.cursorPos;
    }

    private void updateSelectionAfterMove() {
        if ((net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow(), 340) || net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow(), 344))) {
            this.selEnd = this.cursorPos;
        } else {
            this.clearSelection();
        }
    }

    private int getCursorIndexAt(double mouseX) {
        int pos;
        float textFieldX = AccountsScreen.panelX(this.width) + 14.0f;
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float relX = (float)mouseX - textFieldX - 2.0f + this.textXOffset;
        for (pos = 0; pos < this.typedText.length() && !(font.getStringWidth(this.typedText.substring(0, pos + 1)) > relX); ++pos) {
        }
        return pos;
    }

    private void updateTextXOffset() {
        float textFieldWidth = 190.0f;
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float cursorX = font.getStringWidth(this.typedText.substring(0, this.cursorPos));
        if (cursorX < this.textXOffset) {
            this.textXOffset = cursorX;
        } else if (cursorX > this.textXOffset + textFieldWidth - 10.0f) {
            this.textXOffset = cursorX - (textFieldWidth - 10.0f);
        }
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    public boolean shouldPause() {
        return false;
    }
}
