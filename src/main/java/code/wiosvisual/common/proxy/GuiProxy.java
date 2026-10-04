/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_4286
 *  net.minecraft.class_4286$class_8929
 *  net.minecraft.class_437
 *  net.minecraft.class_442
 *  net.minecraft.class_500
 *  net.minecraft.class_5348
 */
package code.wiosvisual.common.proxy;

import code.wiosvisual.common.proxy.Config;
import code.wiosvisual.common.proxy.Proxy;
import code.wiosvisual.common.proxy.ProxyServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GuiProxy
extends Screen {
    private boolean isSocks4 = false;
    private TextFieldWidget ipPort;
    private TextFieldWidget username;
    private TextFieldWidget password;
    private CheckboxWidget enabledCheck;
    private Screen parentScreen;
    private String msg = "";
    private int[] positionY;
    private int positionX;

    public GuiProxy(Screen parentScreen) {
        super((Text)Text.translatable((String)"PROXY"));
        this.parentScreen = parentScreen;
    }

    private static boolean isValidIpPort(String ipP) {
        if (ipP == null || ipP.isEmpty()) {
            return false;
        }
        String[] split = ipP.split(":");
        if (split.length > 1) {
            if (!GuiProxy.isNumeric(split[1])) {
                return false;
            }
            try {
                int port = Integer.parseInt(split[1]);
                return port >= 0 && port <= 65535;
            }
            catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); ++i) {
            if (Character.isDigit(value.charAt(i))) continue;
            return false;
        }
        return true;
    }

    private boolean checkProxy() {
        if (!GuiProxy.isValidIpPort(this.ipPort.getText())) {
            this.msg = String.valueOf(Formatting.RED) + Text.translatable((String)"\u041e\u0428\u0418\u0411\u041a\u0410 \u0412 \u041d\u0410\u041f\u0418\u0421\u0410\u041d\u0418\u0418 \u0410\u0419\u041f\u0418 \u0410\u0414\u0420\u0415\u0421\u0410 \u0418\u041b\u0418 \u041f\u041e\u0420\u0422\u0410").getString();
            this.ipPort.setFocused(true);
            return false;
        }
        return true;
    }

    private void centerButtons(int amount, int buttonLength, int gap) {
        this.positionX = this.width / 2 - buttonLength / 2;
        this.positionY = new int[amount];
        int center = (this.height + amount * gap) / 2;
        int buttonStarts = center - amount * gap;
        for (int i = 0; i != amount; ++i) {
            this.positionY[i] = buttonStarts + gap * i;
        }
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        super.keyPressed(wiosKey);
        this.msg = "";
        return true;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);
        if (this.enabledCheck.isChecked() && !GuiProxy.isValidIpPort(this.ipPort.getText())) {
        }
        context.drawTextWithShadow(this.textRenderer, Text.translatable((String)"\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0430\u0439\u043f\u0438 \u0430\u0434\u0440\u0435\u0441 \u0438 \u043f\u043e\u0440\u0442. \u041f\u0440\u0438\u043c\u0435\u0440 \u043d\u0438\u0436\u0435").getString(), this.width / 2 - 106, this.positionY[2] - 30, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable((String)"\u0410\u0439\u043f\u0438:\u041f\u043e\u0440\u0442").getString(), this.width / 2 - 24, this.positionY[2] - 14, 0xA0A0A0);
        this.ipPort.render(context, mouseX, mouseY, partialTicks);
        context.drawTextWithShadow(this.textRenderer, Text.translatable((String)"\u041d\u0438\u043a\u043d\u0435\u0439\u043c").getString(), this.width / 2 - 20, this.positionY[4] - 14, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable((String)"\u041f\u0430\u0440\u043e\u043b\u044c").getString(), this.width / 2 - 20, this.positionY[5] + 3, 0xA0A0A0);
        this.username.render(context, mouseX, mouseY, partialTicks);
        this.password.render(context, mouseX, mouseY, partialTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.msg, this.width / 2, this.positionY[6] + 5, 0xA0A0A0);
    }

    public void init() {
        int buttonLength = 160;
        this.centerButtons(10, buttonLength, 26);
        this.isSocks4 = ProxyServer.proxy.type == Proxy.ProxyType.SOCKS4;
        this.ipPort = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[2], buttonLength, 20, (Text)Text.literal((String)""));
        this.ipPort.setText(ProxyServer.proxy.ipPort);
        this.ipPort.setMaxLength(1024);
        this.ipPort.setFocused(true);
        this.addSelectableChild(this.ipPort);
        this.username = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[4], buttonLength, 20, (Text)Text.literal((String)""));
        this.username.setMaxLength(255);
        this.username.setText(ProxyServer.proxy.username);
        this.addSelectableChild(this.username);
        this.password = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[5] + 15, buttonLength, 20, (Text)Text.literal((String)""));
        this.password.setMaxLength(255);
        this.password.setText(ProxyServer.proxy.password);
        this.addSelectableChild(this.password);
        int posXButtons = this.width / 2 - buttonLength / 2 * 3 / 2;
        ButtonWidget apply = ButtonWidget.builder((Text)Text.translatable((String)"\u041f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c"), button -> {
            if (this.checkProxy()) {
                ProxyServer.proxy = new Proxy(this.isSocks4, this.ipPort.getText(), this.username.getText(), this.password.getText());
                ProxyServer.proxyEnabled = this.enabledCheck.isChecked();
                Config.setDefaultProxy(ProxyServer.proxy);
                Config.saveConfig();
                MinecraftClient.getInstance().setScreen((Screen)new MultiplayerScreen((Screen)new TitleScreen()));
            }
        }).dimensions(posXButtons + (buttonLength / 2 - 65) * 2, this.positionY[8], buttonLength / 2 + 3, 20).build();
        this.addDrawableChild(apply);
        CheckboxWidget.Builder checkboxBuilder = CheckboxWidget.builder((Text)Text.translatable((String)"\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u043f\u0440\u043e\u043a\u0441\u0438"), (TextRenderer)this.textRenderer);
        checkboxBuilder.pos(this.width / 2 - (15 + this.textRenderer.getWidth((StringVisitable)Text.translatable((String)"\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u043f\u0440\u043e\u043a\u0441\u0438"))) / 2, this.positionY[7]);
        if (ProxyServer.proxyEnabled) {
            checkboxBuilder.checked(ProxyServer.proxyEnabled);
        }
        this.enabledCheck = checkboxBuilder.build();
        this.addDrawableChild(this.enabledCheck);
        ButtonWidget cancel = ButtonWidget.builder((Text)Text.translatable((String)"\u041e\u0442\u043c\u0435\u043d\u0438\u0442\u044c"), button -> MinecraftClient.getInstance().setScreen(this.parentScreen)).dimensions(posXButtons + (buttonLength / 2 - 16) * 2, this.positionY[8], buttonLength / 2 - 3, 20).build();
        this.addDrawableChild(cancel);
    }

    public void close() {
        this.msg = "";
    }
}
