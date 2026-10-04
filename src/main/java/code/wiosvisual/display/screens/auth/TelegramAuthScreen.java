/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_156
 *  net.minecraft.class_1921
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package code.wiosvisual.display.screens.auth;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.display.screens.mainmenu.MainMenu;
import code.wiosvisual.utils.client.auth.TelegramAuthManager;
import code.wiosvisual.utils.client.auth.TelegramAuthService;
import code.wiosvisual.utils.client.logs.Logger;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import java.awt.Color;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TelegramAuthScreen
extends Screen
implements QuickImports {
    private static final long POLL_INTERVAL_MS = 2500L;
    private static final long SESSION_RELOAD_DELAY_MS = 900L;
    private static final Identifier BACKGROUND_IMAGE = Identifier.of((String)"minecraft", (String)"textures/gui/background.png");
    private static String draftLogin = "";
    private static String draftPassword = "";
    private String authCode = "";
    private String authUrl = "";
    private String statusText = "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043b\u043e\u0433\u0438\u043d \u0438 \u043f\u0430\u0440\u043e\u043b\u044c";
    private boolean loading;
    private boolean awaitingApproval;
    private long nextPollAt;
    private TextFieldWidget loginField;
    private TextFieldWidget passwordField;

    public TelegramAuthScreen() {
        super(Text.of((String)"Telegram Auth"));
    }

    protected void init() {
        super.init();
        TelegramAuthManager.ensureInitialized();
        if (draftLogin.isBlank()) {
            draftLogin = TelegramAuthManager.getSavedLogin();
        }
        if (draftPassword.isBlank()) {
            draftPassword = TelegramAuthManager.getSavedPassword();
        }
        this.initTextFields();
        this.debug("Screen initialized");
    }

    private void initTextFields() {
        int fieldWidth = 236;
        int fieldHeight = 18;
        int x = this.width / 2 - fieldWidth / 2;
        int y = this.height / 2 - 24;
        this.loginField = new TextFieldWidget(this.textRenderer, x, y, fieldWidth, fieldHeight, (Text)Text.literal((String)"\u041b\u043e\u0433\u0438\u043d"));
        this.loginField.setPlaceholder((Text)Text.literal((String)"\u041b\u043e\u0433\u0438\u043d"));
        this.loginField.setMaxLength(32);
        this.loginField.setText(draftLogin);
        this.loginField.setDrawsBackground(false);
        this.loginField.setEditableColor(0xF0F0F0);
        this.loginField.setUneditableColor(0x909090);
        this.loginField.setChangedListener(value -> {
            draftLogin = value == null ? "" : value;
        });
        this.addSelectableChild(this.loginField);
        this.passwordField = new TextFieldWidget(this.textRenderer, x, y + 30, fieldWidth, fieldHeight, (Text)Text.literal((String)"\u041f\u0430\u0440\u043e\u043b\u044c"));
        this.passwordField.setPlaceholder((Text)Text.literal((String)"\u041f\u0430\u0440\u043e\u043b\u044c"));
        this.passwordField.setMaxLength(64);
        this.passwordField.setText(draftPassword);
        this.passwordField.setDrawsBackground(false);
        this.passwordField.setEditableColor(0xF0F0F0);
        this.passwordField.setUneditableColor(0x909090);
        this.passwordField.setChangedListener(value -> {
            draftPassword = value == null ? "" : value;
        });
        this.addSelectableChild(this.passwordField);
        this.setInitialFocus((Element)this.loginField);
    }

    public void tick() {
        super.tick();
        if (!this.awaitingApproval || this.authCode.isBlank() || this.loading) {
            return;
        }
        if (System.currentTimeMillis() >= this.nextPollAt) {
            this.pollStatus();
        }
    }

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, BACKGROUND_IMAGE, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        float panelWidth = 276.0f;
        float panelHeight = 246.0f;
        float panelX = (float)this.width / 2.0f - panelWidth / 2.0f;
        float panelY = (float)this.height / 2.0f - panelHeight / 2.0f;
        int panelColor = new Color(21, 24, 29, 196).getRGB();
        int panelStroke = new Color(63, 68, 78, 220).getRGB();
        int titleColor = new Color(237, 239, 244, 255).getRGB();
        int subtitleColor = new Color(139, 146, 161, 255).getRGB();
        int actionColor = new Color(67, 150, 255, 255).getRGB();
        rectangle.render(ShapeProperties.create(GuiCompat.stack(context), panelX, panelY, panelWidth, panelHeight).round(10.0f).thickness(1.0f).outlineColor(panelStroke).color(panelColor).build());
        FontRenderer titleFont = Fonts.getSize(16, Fonts.Type.DEFAULT);
        FontRenderer subtitleFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
        titleFont.drawString(GuiCompat.stack(context), "TelegramAuth", panelX + 14.0f, panelY + 17.0f, titleColor);
        subtitleFont.drawString(GuiCompat.stack(context), "\u0412\u0445\u043e\u0434 \u0432 Wiosvisual \u0447\u0435\u0440\u0435\u0437 Telegram", panelX + 14.0f, panelY + 40.0f, subtitleColor);
        float fieldWidth = panelWidth - 28.0f;
        float fieldHeight = 22.0f;
        float fieldX = panelX + 14.0f;
        float loginFieldY = panelY + 66.0f;
        float passwordFieldY = loginFieldY + 30.0f;
        if (this.loginField != null && this.passwordField != null) {
            this.loginField.setX((int)fieldX);
            this.loginField.setY((int)loginFieldY);
            this.loginField.setWidth((int)fieldWidth);
            this.loginField.setHeight((int)fieldHeight);
            this.passwordField.setX((int)fieldX);
            this.passwordField.setY((int)passwordFieldY);
            this.passwordField.setWidth((int)fieldWidth);
            this.passwordField.setHeight((int)fieldHeight);
            this.drawInputField(context, this.loginField, "\u041b\u043e\u0433\u0438\u043d", fieldX, loginFieldY, fieldWidth, fieldHeight, false);
            this.drawInputField(context, this.passwordField, "\u041f\u0430\u0440\u043e\u043b\u044c", fieldX, passwordFieldY, fieldWidth, fieldHeight, true);
        }
        float buttonWidth = fieldWidth;
        float buttonHeight = 22.0f;
        float loginButtonY = panelY + 128.0f;
        float tgButtonY = loginButtonY + 28.0f;
        this.drawMainButton(context, fieldX, loginButtonY, buttonWidth, buttonHeight, this.loading ? "\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430..." : "\u0412\u043e\u0439\u0442\u0438", !this.loading, mouseX, mouseY, new Color(61, 137, 240, 255).getRGB(), titleColor);
        this.drawSecondaryButton(context, fieldX, tgButtonY, buttonWidth, buttonHeight, "\u0412\u043e\u0439\u0442\u0438 \u0447\u0435\u0440\u0435\u0437 Telegram", !this.loading, mouseX, mouseY, new Color(32, 36, 45, 255).getRGB(), new Color(59, 66, 81, 255).getRGB(), titleColor);
        if (this.statusText != null && !this.statusText.isBlank()) {
            FontRenderer statusFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
            String lowered = this.statusText.toLowerCase(Locale.ROOT);
            boolean isError = lowered.contains("\u043e\u0448\u0438\u0431") || lowered.contains("denied") || lowered.contains("invalid") || lowered.contains("mismatch") || lowered.contains("failed");
            int statusColor = isError ? new Color(255, 114, 114, 255).getRGB() : subtitleColor;
            statusFont.drawCenteredString(GuiCompat.stack(context), this.trimToWidth(this.statusText, 52), panelX + panelWidth / 2.0f, panelY + 189.0f, statusColor);
        }
        FontRenderer bottomFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
        String noAccountText = "\u041d\u0435\u0442 \u0430\u043a\u043a\u0430\u0443\u043d\u0442\u0430? ";
        String registerText = "\u0420\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u044f \u0447\u0435\u0440\u0435\u0437 Telegram";
        float noAccountWidth = bottomFont.getStringWidth(noAccountText);
        float totalWidth = noAccountWidth + bottomFont.getStringWidth(registerText);
        float startX = panelX + (panelWidth - totalWidth) / 2.0f;
        float bottomY = panelY + panelHeight - 18.0f;
        bottomFont.drawString(GuiCompat.stack(context), noAccountText, startX, bottomY, subtitleColor);
        bottomFont.drawString(GuiCompat.stack(context), registerText, startX + noAccountWidth, bottomY, actionColor);
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (button != 0) {
            return super.mouseClicked(wiosClick, wiosDbl);
        }
        float panelWidth = 276.0f;
        float panelHeight = 246.0f;
        float panelX = (float)this.width / 2.0f - panelWidth / 2.0f;
        float panelY = (float)this.height / 2.0f - panelHeight / 2.0f;
        float fieldWidth = panelWidth - 28.0f;
        float fieldHeight = 22.0f;
        float fieldX = panelX + 14.0f;
        float loginFieldY = panelY + 66.0f;
        float passwordFieldY = loginFieldY + 30.0f;
        if (this.isHovered(mouseX, mouseY, fieldX, loginFieldY, fieldWidth, fieldHeight)) {
            this.setFocused((Element)this.loginField);
            this.loginField.setFocused(true);
            this.passwordField.setFocused(false);
            return true;
        }
        if (this.isHovered(mouseX, mouseY, fieldX, passwordFieldY, fieldWidth, fieldHeight)) {
            this.setFocused((Element)this.passwordField);
            this.passwordField.setFocused(true);
            this.loginField.setFocused(false);
            return true;
        }
        if (this.loginField != null && this.loginField.mouseClicked(wiosClick, wiosDbl)) {
            return true;
        }
        if (this.passwordField != null && this.passwordField.mouseClicked(wiosClick, wiosDbl)) {
            return true;
        }
        float buttonWidth = fieldWidth;
        float buttonHeight = 22.0f;
        float loginButtonY = panelY + 128.0f;
        float tgButtonY = loginButtonY + 28.0f;
        if (!this.loading && this.isHovered(mouseX, mouseY, fieldX, loginButtonY, buttonWidth, buttonHeight)) {
            this.loginWithCredentials();
            return true;
        }
        if (!this.loading && this.isHovered(mouseX, mouseY, fieldX, tgButtonY, buttonWidth, buttonHeight)) {
            this.beginAuthorization("login");
            return true;
        }
        FontRenderer bottomFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
        String noAccountText = "\u041d\u0435\u0442 \u0430\u043a\u043a\u0430\u0443\u043d\u0442\u0430? ";
        String registerText = "\u0420\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u044f \u0447\u0435\u0440\u0435\u0437 Telegram";
        float noAccountWidth = bottomFont.getStringWidth(noAccountText);
        float totalWidth = noAccountWidth + bottomFont.getStringWidth(registerText);
        float startX = panelX + (panelWidth - totalWidth) / 2.0f;
        float registerX = startX + noAccountWidth;
        float registerWidth = bottomFont.getStringWidth(registerText);
        float registerY = panelY + panelHeight - 19.0f;
        if (!this.loading && this.isHovered(mouseX, mouseY, registerX, registerY - 1.0f, registerWidth, 11.0)) {
            this.beginAuthorization("register");
            return true;
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void beginAuthorization(String mode) {
        this.loading = true;
        this.statusText = "\u0421\u043e\u0437\u0434\u0430\u0435\u043c \u0441\u0435\u0441\u0441\u0438\u044e...";
        CompletableFuture.supplyAsync(() -> {
            try {
                return TelegramAuthService.createSession(mode);
            }
            catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).orTimeout(12L, TimeUnit.SECONDS).whenComplete((response, throwable) -> mc.execute(() -> {
            this.loading = false;
            if (throwable != null || response == null) {
                this.debugError("Failed to create session", throwable instanceof Throwable ? throwable : new RuntimeException("Unknown auth error"));
                this.statusText = "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0441\u0435\u0441\u0441\u0438\u044e";
                this.awaitingApproval = false;
                this.authCode = "";
                this.authUrl = "";
                return;
            }
            this.authCode = response.code();
            this.authUrl = response.authUrl();
            this.awaitingApproval = true;
            this.statusText = "\u041e\u0436\u0438\u0434\u0430\u0435\u043c \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0432 Telegram...";
            this.nextPollAt = System.currentTimeMillis() + 900L;
        }));
    }

    private void pollStatus() {
        this.loading = true;
        CompletableFuture.supplyAsync(() -> {
            try {
                return TelegramAuthService.fetchStatus(this.authCode);
            }
            catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).orTimeout(10L, TimeUnit.SECONDS).whenComplete((response, throwable) -> mc.execute(() -> {
            this.loading = false;
            this.nextPollAt = System.currentTimeMillis() + 2500L;
            if (throwable != null || response == null) {
                this.debugError("Poll failed", throwable instanceof Throwable ? throwable : new RuntimeException("Unknown poll error"));
                this.statusText = "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435 \u043e\u0442\u0432\u0435\u0447\u0430\u0435\u0442";
                return;
            }
            String status = response.status() == null ? "" : response.status().toUpperCase(Locale.ROOT);
            String reason = response.reason() == null ? "" : response.reason().trim().toUpperCase(Locale.ROOT);
            String message = response.message() == null ? "" : response.message().trim();
            switch (status) {
                case "APPROVED": {
                    this.awaitingApproval = false;
                    TelegramAuthManager.authorize(new TelegramAuthManager.AuthSession(true, response.telegramId(), response.telegramId(), response.username(), response.displayName(), response.role(), response.subscriptionLabel(), TelegramAuthManager.getAuthToken()));
                    this.openMainMenuWithTransition();
                    break;
                }
                case "WAIT_CREDENTIALS": {
                    this.statusText = message.isBlank() ? "\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0434\u0438\u0442\u0435 \u0432\u0445\u043e\u0434 \u0438 \u043e\u0442\u043f\u0440\u0430\u0432\u044c\u0442\u0435 \u043b\u043e\u0433\u0438\u043d/\u043f\u0430\u0440\u043e\u043b\u044c \u0432 \u0431\u043e\u0442\u0435" : message;
                    break;
                }
                case "REGISTERED": {
                    this.awaitingApproval = false;
                    this.statusText = message.isBlank() ? "\u0410\u043a\u043a\u0430\u0443\u043d\u0442 \u0441\u043e\u0437\u0434\u0430\u043d. \u0422\u0435\u043f\u0435\u0440\u044c \u0432\u043e\u0439\u0434\u0438\u0442\u0435 \u043f\u043e \u043b\u043e\u0433\u0438\u043d\u0443 \u0438 \u043f\u0430\u0440\u043e\u043b\u044e" : message;
                    break;
                }
                case "DENIED": {
                    this.awaitingApproval = false;
                    if (message.isBlank() && "HWID_MISMATCH".equals(reason)) {
                        this.statusText = "HWID \u043d\u0435 \u0441\u043e\u0432\u043f\u0430\u0434\u0430\u0435\u0442. \u0417\u0430\u043f\u0440\u043e\u0441\u0438\u0442\u0435 \u0441\u0431\u0440\u043e\u0441 HWID \u0443 \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440\u0430 \u0432 \u0431\u043e\u0442\u0435";
                        break;
                    }
                    this.statusText = message.isBlank() ? "\u0414\u043e\u0441\u0442\u0443\u043f \u043e\u0442\u043a\u043b\u043e\u043d\u0435\u043d" : message;
                    break;
                }
                case "EXPIRED": {
                    this.awaitingApproval = false;
                    this.statusText = message.isBlank() ? "\u0421\u0435\u0441\u0441\u0438\u044f \u0438\u0441\u0442\u0435\u043a\u043b\u0430" : message;
                    break;
                }
                default: {
                    this.statusText = message.isBlank() ? "\u041e\u0436\u0438\u0434\u0430\u0435\u043c \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0432 Telegram..." : message;
                }
            }
        }));
    }

    private void loginWithCredentials() {
        String password;
        String login = this.loginField == null ? "" : this.loginField.getText().trim();
        String string = password = this.passwordField == null ? "" : this.passwordField.getText();
        if (login.isBlank() || password.isBlank()) {
            this.statusText = "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043b\u043e\u0433\u0438\u043d \u0438 \u043f\u0430\u0440\u043e\u043b\u044c";
            return;
        }
        draftLogin = login;
        draftPassword = password;
        this.loading = true;
        this.statusText = "\u041f\u0440\u043e\u0432\u0435\u0440\u044f\u0435\u043c \u0434\u0430\u043d\u043d\u044b\u0435...";
        CompletableFuture.supplyAsync(() -> {
            try {
                return TelegramAuthService.loginWithCredentials(login, password);
            }
            catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).orTimeout(12L, TimeUnit.SECONDS).whenComplete((session, throwable) -> mc.execute(() -> {
            this.loading = false;
            if (throwable != null || session == null) {
                this.statusText = this.extractErrorMessage((Throwable)throwable, "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u043b\u043e\u0433\u0438\u043d \u0438\u043b\u0438 \u043f\u0430\u0440\u043e\u043b\u044c");
                return;
            }
            TelegramAuthManager.setSavedCredentials(login, password);
            TelegramAuthManager.authorize(session);
            this.openMainMenuWithTransition();
        }));
    }

    private void openMainMenuWithTransition() {
        MainMenu.INSTANCE.startAuthTransition();
        mc.setScreen((Screen)MainMenu.INSTANCE);
    }

    public boolean charTyped(net.minecraft.client.input.CharInput wiosChar) {
        char chr = (char) wiosChar.codepoint();
        int modifiers = wiosChar.modifiers();
        if (this.loginField != null && this.loginField.charTyped(wiosChar)) {
            return true;
        }
        if (this.passwordField != null && this.passwordField.charTyped(wiosChar)) {
            return true;
        }
        return super.charTyped(wiosChar);
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        if (this.loginField != null && this.loginField.keyPressed(wiosKey)) {
            return true;
        }
        if (this.passwordField != null && this.passwordField.keyPressed(wiosKey)) {
            return true;
        }
        if (!(this.loading || keyCode != 257 && keyCode != 335)) {
            this.loginWithCredentials();
            return true;
        }
        return super.keyPressed(wiosKey);
    }

    private void drawInputField(DrawContext context, TextFieldWidget field, String placeholder, float x, float y, float width, float height, boolean passwordMode) {
        int drawColor;
        Object draw;
        String value;
        boolean focused = this.getFocused() == field;
        boolean showCaret = focused && System.currentTimeMillis() / 500L % 2L == 0L;
        int fill = new Color(28, 31, 38, 255).getRGB();
        int stroke = focused ? new Color(86, 154, 248, 255).getRGB() : new Color(52, 58, 71, 255).getRGB();
        int textColor = new Color(232, 235, 241, 255).getRGB();
        int placeholderColor = new Color(112, 120, 135, 255).getRGB();
        rectangle.render(ShapeProperties.create(GuiCompat.stack(context), x, y, width, height).round(6.0f).thickness(1.0f).outlineColor(stroke).color(fill).build());
        String string = value = field.getText() == null ? "" : field.getText();
        if (value.isBlank() && !focused) {
            draw = placeholder;
            drawColor = placeholderColor;
        } else if (passwordMode) {
            draw = "\u2022".repeat(Math.max(0, value.length()));
            drawColor = textColor;
        } else {
            draw = value;
            drawColor = textColor;
        }
        if (showCaret) {
            draw = (String)draw + "|";
        }
        FontRenderer inputFont = Fonts.getSize(13, Fonts.Type.DEFAULT);
        inputFont.drawString(GuiCompat.stack(context), this.trimToWidth((String)draw, 32), x + 9.0f, this.centeredTextY(inputFont, (String)draw, y, height), drawColor);
    }

    private void drawMainButton(DrawContext context, float x, float y, float width, float height, String label, boolean enabled, int mouseX, int mouseY, int fill, int textColor) {
        boolean hovered;
        boolean bl = hovered = enabled && this.isHovered(mouseX, mouseY, x, y, width, height);
        int buttonColor = enabled ? (hovered ? this.brighten(fill, 14) : fill) : new Color(78, 83, 93, 255).getRGB();
        rectangle.render(ShapeProperties.create(GuiCompat.stack(context), x, y, width, height).round(6.0f).color(buttonColor).build());
        FontRenderer font = Fonts.getSize(11, Fonts.Type.DEFAULT);
        font.drawCenteredString(GuiCompat.stack(context), label, x + width / 2.0f, this.centeredTextY(font, label, y, height), enabled ? textColor : new Color(166, 170, 177, 255).getRGB());
    }

    private void drawSecondaryButton(DrawContext context, float x, float y, float width, float height, String label, boolean enabled, int mouseX, int mouseY, int fill, int stroke, int textColor) {
        boolean hovered = enabled && this.isHovered(mouseX, mouseY, x, y, width, height);
        int buttonColor = hovered ? this.brighten(fill, 8) : fill;
        rectangle.render(ShapeProperties.create(GuiCompat.stack(context), x, y, width, height).round(6.0f).thickness(1.0f).outlineColor(stroke).color(buttonColor).build());
        FontRenderer font = Fonts.getSize(11, Fonts.Type.DEFAULT);
        font.drawCenteredString(GuiCompat.stack(context), label, x + width / 2.0f, this.centeredTextY(font, label, y, height), textColor);
    }

    private float centeredTextY(FontRenderer font, String text, float boxY, float boxHeight) {
        return font.getCenteredY(boxY, boxHeight, text);
    }

    private int brighten(int rgb, int delta) {
        Color color = new Color(rgb, true);
        return new Color(Math.min(255, color.getRed() + delta), Math.min(255, color.getGreen() + delta), Math.min(255, color.getBlue() + delta), color.getAlpha()).getRGB();
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void debug(String message) {
        String line = "[TelegramAuth] " + message;
        Logger.info(line);
        System.out.println(line);
    }

    private void debugError(String message, Throwable throwable) {
        String line = "[TelegramAuth] " + message;
        Logger.error(line, throwable);
        System.err.println(line);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    private String extractErrorMessage(Throwable throwable, String fallback) {
        String best = "";
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null || message.isBlank()) continue;
            best = message;
        }
        if (best.isBlank()) {
            return fallback;
        }
        return this.trimToWidth(best.replace("java.lang.RuntimeException:", "").replace("java.io.IOException:", "").trim(), 52);
    }

    private String trimToWidth(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, limit - 3)) + "...";
    }
}
