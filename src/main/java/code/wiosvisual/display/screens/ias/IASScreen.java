/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.UserApiService
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_320
 *  net.minecraft.class_320$class_321
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_5520
 *  net.minecraft.class_7569
 *  net.minecraft.class_7574
 *  net.minecraft.class_7853
 */
package code.wiosvisual.display.screens.ias;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.mixins.IMinecraftClient;
import code.wiosvisual.utils.client.managers.file.impl.account.Account;
import code.wiosvisual.utils.client.managers.file.impl.account.AccountRepository;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.scissor.ScissorAssist;
import code.wiosvisual.utils.theme.ThemeManager;
import com.mojang.authlib.minecraft.UserApiService;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class IASScreen
extends Screen
implements QuickImports {
    public static IASScreen INSTANCE = new IASScreen();
    private AccountRepository accountRepository = Wiosvisual.getInstance().getAccountRepository();
    private float scroll = 0.0f;
    private float smoothedScroll = 0.0f;
    private int selectedIndex = -1;
    private static final int ITEM_HEIGHT = 25;
    private static final int PADDING = 10;
    private static final int SCROLLBAR_WIDTH = 5;
    private boolean typing = false;
    private String typedText = "";
    private int cursorPos = 0;
    private int selStart = -1;
    private int selEnd = -1;
    private float textXOffset = 0.0f;
    private boolean editingAccount = false;
    private int editingIndex = -1;
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 16;
    private long lastClickTime = 0L;
    private int lastSelectedIndex = -1;

    public IASScreen() {
        super(Text.of((String)"Account Switcher"));
    }

    protected void init() {
        super.init();
        if (this.accountRepository == null) {
            this.accountRepository = Wiosvisual.getInstance().getAccountRepository();
        }
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"Add Account"), button -> {
            if (!this.typing) {
                this.typing = true;
                this.editingAccount = false;
                this.editingIndex = -1;
                this.typedText = "";
                this.cursorPos = 0;
                this.clearSelection();
            } else {
                this.addNewAccount();
            }
        }).dimensions(this.width / 2 - 200, this.height - 30, 95, 20).build());
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"Edit"), button -> {
            if (this.selectedIndex >= 0 && this.selectedIndex < this.accountRepository.accountList.size()) {
                this.startEditing(this.selectedIndex);
            }
        }).dimensions(this.width / 2 - 100, this.height - 30, 95, 20).build());
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"Delete"), button -> {
            if (this.selectedIndex >= 0 && this.selectedIndex < this.accountRepository.accountList.size()) {
                this.accountRepository.accountList.remove(this.selectedIndex);
                this.selectedIndex = -1;
                this.saveAccounts();
            }
        }).dimensions(this.width / 2, this.height - 30, 95, 20).build());
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"Close"), button -> this.close()).dimensions(this.width / 2 + 100, this.height - 30, 95, 20).build());
    }

    private void addNewAccount() {
        if (this.typedText == null || this.typedText.trim().isEmpty()) {
            this.typing = true;
            this.editingAccount = false;
            this.editingIndex = -1;
            return;
        }
        String username = this.typedText.trim();
        if (username.length() >= 3 && username.length() <= 16 && !this.accountRepository.accountList.stream().anyMatch(account -> account.name.equals(username))) {
            String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
            this.accountRepository.accountList.add(new Account(username, false, false, null, offlineUuid, "0"));
            this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
            this.typedText = "";
            this.cursorPos = 0;
            this.typing = false;
            this.clearSelection();
            this.saveAccounts();
        }
    }

    private void startEditing(int index) {
        this.editingAccount = true;
        this.editingIndex = index;
        Account account = this.accountRepository.accountList.get(index);
        this.typedText = account.name;
        this.cursorPos = this.typedText.length();
        this.selStart = -1;
        this.selEnd = -1;
    }

    private void finishEditing() {
        if (this.editingAccount && this.editingIndex >= 0 && this.editingIndex < this.accountRepository.accountList.size()) {
            if (this.typedText.length() >= 3 && this.typedText.length() <= 16) {
                String newName = this.typedText.trim();
                Account account = this.accountRepository.accountList.get(this.editingIndex);
                if (!newName.equals(account.name) && !this.accountRepository.accountList.stream().anyMatch(a -> a.name.equals(newName) && a != account)) {
                    account.name = newName;
                    account.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newName).getBytes(StandardCharsets.UTF_8)).toString();
                    this.saveAccounts();
                }
            }
            this.editingAccount = false;
            this.editingIndex = -1;
            this.typedText = "";
            this.cursorPos = 0;
            this.clearSelection();
        }
    }

    private void clearSelection() {
        this.selStart = -1;
        this.selEnd = -1;
    }

    private boolean hasSelection() {
        return this.selStart >= 0 && this.selEnd >= 0 && this.selStart != this.selEnd;
    }

    private void saveAccounts() {
        try {
            Wiosvisual.getInstance().getFileController().saveFiles();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 1.21.11: ванильный фон (панорама, blur, затемнение) рисуется отложенно поверх нашего немедленного рендера — отключаем. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.accountRepository == null) {
            this.accountRepository = Wiosvisual.getInstance().getAccountRepository();
        }
        if (this.accountRepository == null || this.accountRepository.accountList == null) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }
        int x = this.width / 2 - 200;
        int y = this.height / 2 - 150;
        int width = 400;
        int height = 300;
        MatrixStack matrix = GuiCompat.stack(context);
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(8.0f).softness(2.0f).outlineColor(new Color(33, 33, 33, 255).getRGB()).color(ThemeManager.BackgroundGui.getColor()).build());
        Fonts.getSize(18, Fonts.Type.SuisseIntlMedium).drawString(matrix, "Account Switcher", x + 10, y + 10, ThemeManager.textColor.getColor());
        float textFieldX = x + 10;
        float textFieldY = y + height - 50;
        float textFieldWidth = width - 20;
        float textFieldHeight = 20.0f;
        rectangle.render(ShapeProperties.create(matrix, textFieldX, textFieldY, textFieldWidth, textFieldHeight).round(5.0f).thickness(2.0f).outlineColor(new Color(100, 100, 100, 95).getRGB()).color(ThemeManager.BackgroundSettings.getColor()).build());
        Fonts.Type fontType = Fonts.Type.DEFAULT;
        FontRenderer font = Fonts.getSize(16, fontType);
        long currentTime = System.currentTimeMillis();
        boolean blink = currentTime % 1000L < 500L;
        context.enableScissor((int)(textFieldX + 3.0f), (int)textFieldY, (int)(textFieldX + textFieldWidth - 3.0f), (int)(textFieldY + textFieldHeight) + 5);
        if (this.typing && this.hasSelection()) {
            int start = Math.min(this.selStart, this.selEnd);
            int end = Math.max(this.selStart, this.selEnd);
            float selXStart = textFieldX + 5.0f - this.textXOffset + font.getStringWidth(this.typedText.substring(0, start));
            float selWidth = font.getStringWidth(this.typedText.substring(start, end));
            rectangle.render(ShapeProperties.create(matrix, selXStart, textFieldY + 13.5f, selWidth, textFieldHeight - 10.0f).color(-11172376).build());
        }
        if (!this.typedText.isEmpty() || this.typing) {
            font.drawString(matrix, this.typedText, textFieldX + 5.0f - this.textXOffset, textFieldY + 16.0f, ThemeManager.textColor.getColor());
        } else {
            String placeholder = this.editingAccount ? "Editing account..." : "Type nickname to add account";
            font.drawString(matrix, placeholder, textFieldX + 5.0f, textFieldY + 16.0f, new Color(150, 150, 150, 255).getRGB());
        }
        if (this.typing && blink && !this.hasSelection()) {
            float cursorX = textFieldX + 5.0f - this.textXOffset + font.getStringWidth(this.typedText.substring(0, this.cursorPos));
            rectangle.render(ShapeProperties.create(matrix, cursorX + 1.0f, textFieldY + 15.0f, 0.5, textFieldHeight - 13.0f).color(ThemeManager.textColor.getColor()).build());
        }
        context.disableScissor();
        if (this.accountRepository.accountList.isEmpty()) {
            Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawString(matrix, "No accounts found. Type nickname above to add.", x + 10, y + 60, new Color(150, 150, 150, 255).getRGB());
            super.render(context, mouseX, mouseY, delta);
            return;
        }
        float listX = x + 10;
        float listY = y + 35;
        float listWidth = width - 20 - 5 - 5;
        float listHeight = height - 100;
        ScissorAssist scissorManager = Wiosvisual.getInstance().getScissorManager();
        scissorManager.push(matrix.peek().getPositionMatrix(), listX, listY, listWidth, listHeight);
        this.smoothedScroll = MathHelper.lerp((float)0.1f, (float)this.smoothedScroll, (float)this.scroll);
        int startIndex = Math.max(0, (int)(this.smoothedScroll / 25.0f));
        int endIndex = Math.min(this.accountRepository.accountList.size(), startIndex + (int)(listHeight / 25.0f) + 1);
        for (int i = startIndex; i < endIndex && i < this.accountRepository.accountList.size(); ++i) {
            int bgColor;
            Account account = this.accountRepository.accountList.get(i);
            float itemY = listY + (float)(i * 25) - this.smoothedScroll;
            if (itemY + 25.0f < listY || itemY > listY + listHeight) continue;
            boolean isHovered = (float)mouseX >= listX && (float)mouseX <= listX + listWidth && (float)mouseY >= itemY && (float)mouseY <= itemY + 25.0f;
            boolean isSelected = i == this.selectedIndex;
            boolean isEditing = this.editingAccount && this.editingIndex == i;
            int n = bgColor = isHovered || isSelected ? new Color(ThemeManager.BackgroundSettings.getColor()).darker().getRGB() : ThemeManager.BackgroundSettings.getColor();
            if (isEditing) {
                bgColor = new Color(100, 150, 255, 100).getRGB();
            }
            rectangle.render(ShapeProperties.create(matrix, listX, itemY, listWidth, 25.0).round(4.0f).color(bgColor).build());
            if (account.starred) {
                Fonts.getSize(20, Fonts.Type.ICONS).drawString(matrix, "S", listX + listWidth - 25.0f, itemY + 12.0f - 8.0f, new Color(255, 255, 0, 255).getRGB());
            }
            Object displayName = account.name;
            if (account.starred) {
                displayName = "\u2605 " + (String)displayName;
            }
            Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawString(matrix, (String)displayName, listX + 5.0f, itemY + 12.0f - 5.0f, ThemeManager.textColor.getColor());
        }
        scissorManager.pop();
        int visibleItems = (int)(listHeight / 25.0f);
        if (this.accountRepository.accountList.size() > visibleItems) {
            float scrollbarX = x + width - 10 - 5;
            float scrollbarHeight = listHeight;
            float scrollbarThumbHeight = scrollbarHeight * ((float)visibleItems / (float)this.accountRepository.accountList.size());
            float scrollbarThumbY = listY + (scrollbarHeight - scrollbarThumbHeight) * (this.smoothedScroll / (float)Math.max(1, (this.accountRepository.accountList.size() - visibleItems) * 25));
            rectangle.render(ShapeProperties.create(matrix, scrollbarX, listY, 5.0, scrollbarHeight).round(2.0f).color(new Color(50, 50, 50, 255).getRGB()).build());
            rectangle.render(ShapeProperties.create(matrix, scrollbarX, scrollbarThumbY, 5.0, scrollbarThumbHeight).round(2.0f).color(new Color(100, 100, 100, 255).getRGB()).build());
        }
        super.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click wiosClick, boolean wiosDbl) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        if (this.accountRepository == null || this.accountRepository.accountList == null) {
            return super.mouseClicked(wiosClick, wiosDbl);
        }
        float listX = this.width / 2 - 200 + 10;
        float listY = this.height / 2 - 150 + 35;
        float listWidth = 370.0f;
        float listHeight = 200.0f;
        int visibleItems = (int)(listHeight / 25.0f);
        int startIndex = Math.max(0, (int)(this.smoothedScroll / 25.0f));
        float textFieldX = this.width / 2 - 200 + 10;
        float textFieldY = this.height / 2 - 150 + 300 - 50;
        float textFieldWidth = 380.0f;
        float textFieldHeight = 20.0f;
        if (button == 0 && mouseX >= (double)textFieldX && mouseX <= (double)(textFieldX + textFieldWidth) && mouseY >= (double)textFieldY && mouseY <= (double)(textFieldY + textFieldHeight)) {
            this.typing = true;
            this.selStart = this.cursorPos = this.getCursorIndexAt((float)mouseX);
            this.selEnd = this.cursorPos;
            return true;
        }
        if (button == 0) {
            this.typing = false;
            if (!this.editingAccount) {
                this.clearSelection();
            }
        }
        for (int i = startIndex; i < Math.min(this.accountRepository.accountList.size(), startIndex + visibleItems + 1); ++i) {
            float itemY = listY + (float)(i * 25) - this.smoothedScroll;
            float starX = listX + listWidth - 25.0f;
            float starY = itemY + 12.0f - 8.0f;
            float starSize = 20.0f;
            if (button == 0 && mouseX >= (double)starX && mouseX <= (double)(starX + starSize) && mouseY >= (double)starY && mouseY <= (double)(starY + starSize)) {
                Account account = this.accountRepository.accountList.get(i);
                account.starred = !account.starred;
                this.accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                this.saveAccounts();
                return true;
            }
            if (!(mouseX >= (double)listX) || !(mouseX <= (double)(listX + listWidth)) || !(mouseY >= (double)itemY) || !(mouseY <= (double)(itemY + 25.0f))) continue;
            this.selectedIndex = i;
            if (button == 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - this.lastClickTime < 300L && this.lastSelectedIndex == i) {
                    this.switchAccount(this.accountRepository.accountList.get(i));
                }
                this.lastClickTime = currentTime;
                this.lastSelectedIndex = i;
            } else if (button == 1) {
                this.accountRepository.accountList.remove(i);
                this.selectedIndex = -1;
                this.saveAccounts();
            }
            return true;
        }
        return super.mouseClicked(wiosClick, wiosDbl);
    }

    private int getCursorIndexAt(float mouseX) {
        float textFieldX = this.width / 2 - 200 + 10;
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float textStartX = textFieldX + 5.0f - this.textXOffset;
        float relativeX = mouseX - textStartX;
        if (relativeX < 0.0f) {
            return 0;
        }
        if (relativeX > font.getStringWidth(this.typedText)) {
            return this.typedText.length();
        }
        for (int i = 0; i <= this.typedText.length(); ++i) {
            float charX = font.getStringWidth(this.typedText.substring(0, i));
            if (!(relativeX <= charX + font.getStringWidth(this.typedText.substring(i, Math.min(i + 1, this.typedText.length()))) / 2.0f)) continue;
            return i;
        }
        return this.typedText.length();
    }

    private void switchAccount(Account account) {
        for (Account other : this.accountRepository.accountList) {
            other.active = other == account;
        }
        this.saveAccounts();
        IASScreen.applyAccountSession(account);
        this.close();
    }

    public static void restoreActiveAccount() {
        try {
            Wiosvisual instance = Wiosvisual.getInstance();
            if (instance == null) {
                return;
            }
            AccountRepository repository = instance.getAccountRepository();
            if (repository == null || repository.accountList == null) {
                return;
            }
            for (Account account : repository.accountList) {
                if (account == null || !account.active || account.uuid == null || account.name == null) continue;
                IASScreen.applyAccountSession(account);
                break;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void applyAccountSession(Account account) {
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

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.accountRepository == null) {
            return false;
        }
        float listY = this.height / 2 - 150 + 35;
        float listHeight = 200.0f;
        int maxScroll = Math.max(0, this.accountRepository.accountList.size() * 25 - (int)listHeight);
        this.scroll = MathHelper.clamp((float)(this.scroll - (float)verticalAmount * 10.0f), (float)0.0f, (float)maxScroll);
        return true;
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput wiosKey) {
        int keyCode = wiosKey.key();
        int scanCode = wiosKey.scancode();
        int modifiers = wiosKey.modifiers();
        if (keyCode == 256) {
            if (this.editingAccount) {
                this.finishEditing();
                return true;
            }
            this.close();
            return true;
        }
        if (this.typing) {
            if (keyCode == 257 || keyCode == 335) {
                if (this.editingAccount) {
                    this.finishEditing();
                } else {
                    this.addNewAccount();
                }
                return true;
            }
            if (keyCode == 259) {
                if (this.hasSelection()) {
                    int start = Math.min(this.selStart, this.selEnd);
                    int end = Math.max(this.selStart, this.selEnd);
                    this.typedText = this.typedText.substring(0, start) + this.typedText.substring(end);
                    this.cursorPos = start;
                    this.clearSelection();
                } else if (this.cursorPos > 0) {
                    this.typedText = this.typedText.substring(0, this.cursorPos - 1) + this.typedText.substring(this.cursorPos);
                    --this.cursorPos;
                }
                return true;
            }
            if (keyCode == 261) {
                if (this.hasSelection()) {
                    int start = Math.min(this.selStart, this.selEnd);
                    int end = Math.max(this.selStart, this.selEnd);
                    this.typedText = this.typedText.substring(0, start) + this.typedText.substring(end);
                    this.cursorPos = start;
                    this.clearSelection();
                } else if (this.cursorPos < this.typedText.length()) {
                    this.typedText = this.typedText.substring(0, this.cursorPos) + this.typedText.substring(this.cursorPos + 1);
                }
                return true;
            }
            if (keyCode == 263) {
                if (this.cursorPos > 0) {
                    --this.cursorPos;
                }
                if ((modifiers & 1) == 0) {
                    this.clearSelection();
                }
                return true;
            }
            if (keyCode == 262) {
                if (this.cursorPos < this.typedText.length()) {
                    ++this.cursorPos;
                }
                if ((modifiers & 1) == 0) {
                    this.clearSelection();
                }
                return true;
            }
        }
        return super.keyPressed(wiosKey);
    }

    public boolean charTyped(net.minecraft.client.input.CharInput wiosChar) {
        char chr = (char) wiosChar.codepoint();
        int modifiers = wiosChar.modifiers();
        if (this.typing && this.typedText.length() < 16) {
            if (this.hasSelection()) {
                int start = Math.min(this.selStart, this.selEnd);
                int end = Math.max(this.selStart, this.selEnd);
                this.typedText = this.typedText.substring(0, start) + chr + this.typedText.substring(end);
                this.cursorPos = start + 1;
                this.clearSelection();
            } else {
                this.typedText = this.typedText.substring(0, this.cursorPos) + chr + this.typedText.substring(this.cursorPos);
                ++this.cursorPos;
            }
            return true;
        }
        return super.charTyped(wiosChar);
    }
}
