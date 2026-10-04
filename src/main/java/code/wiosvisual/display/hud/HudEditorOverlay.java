/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_4587
 *  org.joml.Vector4d
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.display.hud.TargetHud;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.client.managers.file.impl.ElementsFile;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.math.projection.Projection;
import code.wiosvisual.utils.theme.ThemeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4d;

public class HudEditorOverlay
implements QuickImports {
    private static final HudEditorOverlay INSTANCE = new HudEditorOverlay();
    private static final float MENU_HEADER_H = 18.0f;
    private static final float MENU_ROW_H = 16.0f;
    private static final float CAPSULE_H = 13.0f;
    private static final float SCALE_BUTTON_SIZE = 22.0f;
    private static final float SCALE_BUTTON_MARGIN = 6.0f;
    private static final float SCALE_BUTTONS_AREA_H = 28.0f;
    private static final long MENU_ANIM_MS = 240L;
    private boolean menuOpen;
    private boolean menuClosing;
    private long menuAnimStart;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;
    private MenuMode menuMode;
    private String menuTitle = "";
    private AbstractDraggable targetDraggable;
    private final List<MenuEntry> menuEntries = new ArrayList<MenuEntry>();

    public static HudEditorOverlay getInstance() {
        return INSTANCE;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) {
            this.closeMenu();
            return;
        }
        List<AbstractDraggable> visible = this.getVisibleDraggables(hud);
        for (AbstractDraggable draggable : visible) {
            draggable.render(context, mouseX, mouseY, delta);
        }
        if (this.menuOpen) {
            float progress = this.getMenuAnimProgress();
            if (this.menuClosing && progress <= 0.01f) {
                this.closeMenu();
                return;
            }
            this.renderMenu(GuiCompat.stack(context), mouseX, mouseY, progress);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) {
            this.closeMenu();
            return false;
        }
        if (this.menuOpen && !this.menuClosing) {
            if (button == 0) {
                if (this.menuMode == MenuMode.ACTIONS && this.targetDraggable != null && this.handleScaleButtonClick(mouseX, mouseY)) {
                    return true;
                }
                if (this.isInsideMenu(mouseX, mouseY)) {
                    int index = this.getMenuIndex(mouseY);
                    if (index >= 0 && index < this.menuEntries.size()) {
                        MenuEntry entry = this.menuEntries.get(index);
                        entry.action().run();
                        if (entry.keepOpen() && this.targetDraggable != null) {
                            // Переключатели не закрывают меню: обновляем подписи с галочками на месте.
                            this.rebuildActionsEntries(this.targetDraggable);
                            this.menuWidth = Math.max(this.menuWidth, this.measureMenuWidth());
                        } else {
                            this.beginClose();
                        }
                        this.saveHudState();
                        return true;
                    }
                    return true;
                }
            }
            if (!this.isInsideMenu(mouseX, mouseY)) {
                this.beginClose();
            } else {
                return true;
            }
        }
        AbstractDraggable hovered = this.getHoveredDraggable(hud, mouseX, mouseY);
        if (button == 1) {
            if (hovered != null) {
                this.openActionsMenu((float)mouseX, (float)mouseY, hovered);
                return true;
            }
            PlayerEntity hoveredPlayer = this.getHoveredPlayer(mouseX, mouseY);
            if (hoveredPlayer != null) {
                this.openFriendMenu((float)mouseX, (float)mouseY, hoveredPlayer);
                return true;
            }
            List<AbstractDraggable> disabled = this.getDisabledDraggables(hud);
            if (!disabled.isEmpty()) {
                this.openAddMenu((float)mouseX, (float)mouseY, disabled, hud);
                return true;
            }
            return false;
        }
        if (button == 0 && hovered != null) {
            return hovered.beginDrag(mouseX, mouseY);
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Hud hud = Hud.getInstance();
        if (hud == null) {
            return false;
        }
        boolean handled = false;
        for (AbstractDraggable draggable : Wiosvisual.getInstance().getDraggableRepository().draggable()) {
            handled |= draggable.mouseReleased(mouseX, mouseY, button);
        }
        return handled;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    private void renderMenu(MatrixStack matrix, int mouseX, int mouseY, float progress) {
        float t = HudEditorOverlay.easeOutCubic(MathHelper.clamp((float)progress, (float)0.0f, (float)1.0f));
        float scale = 0.72f + 0.28f * t;
        float alpha = t;
        float slideY = (1.0f - t) * 12.0f;
        float animY = this.menuY - slideY;
        float pivotX = this.menuX + this.menuWidth / 2.0f;
        float pivotY = animY + this.menuHeight / 2.0f;
        matrix.push();
        matrix.translate(pivotX, pivotY, 0.0f);
        matrix.scale(scale, scale, 1.0f);
        matrix.translate(-pivotX, -pivotY, 0.0f);
        this.drawMenu(matrix, mouseX, mouseY, alpha, animY, t);
        matrix.pop();
    }

    private void drawMenu(MatrixStack matrix, int mouseX, int mouseY, float alpha, float animY, float progress) {
        FontRenderer titleFont = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
        FontRenderer itemFont = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
        int baseBg = ThemeManager.BackgroundGui.getColor() | 0xFF000000;
        int fill = ColorAssist.multAlpha(baseBg, 0.97f * alpha);
        int border = ColorAssist.multAlpha(ThemeManager.textColor.getColor(), 0.16f * alpha);
        int accent = HudTheme.accent(255);
        int textCol = ThemeManager.textColor.getColor();
        rectangle.render(ShapeProperties.create(matrix, this.menuX, animY, this.menuWidth, this.menuHeight).round(10.0f).color(fill).build());
        rectangle.render(ShapeProperties.create(matrix, this.menuX, animY, this.menuWidth, this.menuHeight).round(10.0f).thickness(1.0f).outlineColor(border).color(0).build());
        float currentY = animY;
        if (this.menuMode == MenuMode.ACTIONS && this.targetDraggable != null) {
            this.drawScaleButtons(matrix, mouseX, mouseY, alpha, currentY, progress, accent, textCol, baseBg);
            currentY += 28.0f;
        }
        float centerX = this.menuX + this.menuWidth / 2.0f;
        float titleAreaH = 12.0f;
        titleFont.drawCenteredString(matrix, this.menuTitle, centerX, HudEditorOverlay.centeredTextY(titleFont, this.menuTitle, currentY, titleAreaH), ColorAssist.multAlpha(textCol, alpha));
        float lineW = Math.min(this.menuWidth - 18.0f, titleFont.getStringWidth(this.menuTitle) + 16.0f);
        rectangle.render(ShapeProperties.create(matrix, centerX - lineW / 2.0f, currentY + 18.0f - 5.0f, lineW, 1.4f).round(1.0f).color(ColorAssist.multAlpha(accent, 0.85f * alpha)).build());
        currentY += 18.0f;
        for (MenuEntry entry : this.menuEntries) {
            float capX = this.menuX + 6.0f;
            float capW = this.menuWidth - 12.0f;
            float capY = currentY + 1.5f;
            boolean hovered = !this.menuClosing && progress > 0.85f && (float)mouseX >= capX && (float)mouseX <= capX + capW && (float)mouseY >= capY && (float)mouseY <= capY + 13.0f;
            int capFill = hovered ? ColorAssist.multAlpha(accent, 0.22f * alpha) : ColorAssist.multAlpha(HudEditorOverlay.blend(baseBg, -1, 0.07f), 0.9f * alpha);
            rectangle.render(ShapeProperties.create(matrix, capX, capY, capW, 13.0).round(6.5f).color(capFill).build());
            rectangle.render(ShapeProperties.create(matrix, capX, capY, capW, 13.0).round(6.5f).thickness(0.8f).outlineColor(ColorAssist.multAlpha(hovered ? accent : textCol, (hovered ? 0.45f : 0.1f) * alpha)).color(0).build());
            int labelColor = ColorAssist.multAlpha(textCol, (hovered ? 1.0f : 0.86f) * alpha);
            itemFont.drawCenteredString(matrix, entry.label(), centerX, HudEditorOverlay.centeredTextY(itemFont, entry.label(), capY, 13.0f), labelColor);
            currentY += 16.0f;
        }
    }

    private void drawScaleButtons(MatrixStack matrix, int mouseX, int mouseY, float alpha, float currentY, float progress, int accent, int textCol, int baseBg) {
        FontRenderer buttonFont = Fonts.getSize(20, Fonts.Type.SuisseIntlSemiBold);
        float centerX = this.menuX + this.menuWidth / 2.0f;
        float buttonY = currentY + 3.0f;
        float leftButtonX = centerX - 22.0f - 3.0f;
        boolean leftHovered = !this.menuClosing && progress > 0.85f && (float)mouseX >= leftButtonX && (float)mouseX <= leftButtonX + 22.0f && (float)mouseY >= buttonY && (float)mouseY <= buttonY + 22.0f;
        this.drawScaleButton(matrix, leftButtonX, buttonY, "-", leftHovered, alpha, accent, textCol, baseBg, buttonFont);
        float rightButtonX = centerX + 3.0f;
        boolean rightHovered = !this.menuClosing && progress > 0.85f && (float)mouseX >= rightButtonX && (float)mouseX <= rightButtonX + 22.0f && (float)mouseY >= buttonY && (float)mouseY <= buttonY + 22.0f;
        this.drawScaleButton(matrix, rightButtonX, buttonY, "+", rightHovered, alpha, accent, textCol, baseBg, buttonFont);
    }

    private void drawScaleButton(MatrixStack matrix, float x, float y, String symbol, boolean hovered, float alpha, int accent, int textCol, int baseBg, FontRenderer font) {
        int buttonFill = hovered ? ColorAssist.multAlpha(accent, 0.25f * alpha) : ColorAssist.multAlpha(HudEditorOverlay.blend(baseBg, -1, 0.1f), 0.9f * alpha);
        rectangle.render(ShapeProperties.create(matrix, x, y, 22.0, 22.0).round(11.0f).color(buttonFill).build());
        rectangle.render(ShapeProperties.create(matrix, x, y, 22.0, 22.0).round(11.0f).thickness(1.0f).outlineColor(ColorAssist.multAlpha(hovered ? accent : textCol, (hovered ? 0.55f : 0.18f) * alpha)).color(0).build());
        int symbolColor = ColorAssist.multAlpha(textCol, (hovered ? 1.0f : 0.8f) * alpha);
        font.drawCenteredBoth(matrix, symbol, x + 11.0f, y, 22.0f, symbolColor);
    }

    private boolean handleScaleButtonClick(double mouseX, double mouseY) {
        float centerX = this.menuX + this.menuWidth / 2.0f;
        float buttonY = this.menuY + 3.0f;
        float leftButtonX = centerX - 22.0f - 3.0f;
        if (mouseX >= (double)leftButtonX && mouseX <= (double)(leftButtonX + 22.0f) && mouseY >= (double)buttonY && mouseY <= (double)(buttonY + 22.0f)) {
            this.targetDraggable.decreaseScale();
            this.saveHudState();
            return true;
        }
        float rightButtonX = centerX + 3.0f;
        if (mouseX >= (double)rightButtonX && mouseX <= (double)(rightButtonX + 22.0f) && mouseY >= (double)buttonY && mouseY <= (double)(buttonY + 22.0f)) {
            this.targetDraggable.increaseScale();
            this.saveHudState();
            return true;
        }
        return false;
    }

    private float getMenuAnimProgress() {
        float raw = (float)(System.currentTimeMillis() - this.menuAnimStart) / 240.0f;
        if (this.menuClosing) {
            return 1.0f - MathHelper.clamp((float)raw, (float)0.0f, (float)1.0f);
        }
        return MathHelper.clamp((float)raw, (float)0.0f, (float)1.0f);
    }

    private static float centeredTextY(FontRenderer font, String text, float boxY, float boxHeight) {
        return font.getCenteredY(boxY, boxHeight, text);
    }

    private void openAddMenu(float x, float y, List<AbstractDraggable> disabled, Hud hud) {
        this.menuEntries.clear();
        this.targetDraggable = null;
        this.menuMode = MenuMode.ADD;
        this.menuTitle = "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u044d\u043b\u0435\u043c\u0435\u043d\u0442";
        for (AbstractDraggable draggable : disabled) {
            String name = draggable.getName();
            this.menuEntries.add(new MenuEntry(this.russianName(name), () -> hud.setHudElementEnabled(name, true)));
        }
        this.openMenu(x, y);
    }

    private PlayerEntity getHoveredPlayer(double mouseX, double mouseY) {
        if (HudEditorOverlay.mc.world == null || HudEditorOverlay.mc.player == null) {
            return null;
        }
        PlayerEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (PlayerEntity player : HudEditorOverlay.mc.world.getPlayers()) {
            double distance;
            Vector4d vec;
            if (player == HudEditorOverlay.mc.player || player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_") || !Projection.isWithinRenderDistance((Entity)player) || !Projection.canSee(player.getBoundingBox()) || Projection.cantSee(vec = Projection.getVector4D((Entity)player)) || mouseX < vec.x || mouseX > vec.z || mouseY < vec.y || mouseY > vec.w || !((distance = (double)HudEditorOverlay.mc.player.distanceTo((Entity)player)) < bestDistance)) continue;
            bestDistance = distance;
            best = player;
        }
        return best;
    }

    private void openFriendMenu(float x, float y, PlayerEntity player) {
        String name;
        this.menuEntries.clear();
        this.targetDraggable = null;
        this.menuMode = MenuMode.ADD;
        this.menuTitle = name = player.getGameProfile().name();
        this.menuEntries.add(new MenuEntry(FriendUtils.isFriend(name) ? "\u0423\u0431\u0440\u0430\u0442\u044c \u0438\u0437 \u0434\u0440\u0443\u0437\u0435\u0439" : "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432 \u0434\u0440\u0443\u0437\u044c\u044f", () -> {
            if (FriendUtils.isFriend(name)) {
                FriendUtils.removeFriend(name);
            } else {
                FriendUtils.addFriend(name);
            }
        }));
        this.openMenu(x, y);
    }

    private void openActionsMenu(float x, float y, AbstractDraggable draggable) {
        this.targetDraggable = draggable;
        this.menuMode = MenuMode.ACTIONS;
        this.menuTitle = this.russianName(draggable.getName());
        this.rebuildActionsEntries(draggable);
        this.openMenu(x, y);
    }

    private void rebuildActionsEntries(AbstractDraggable draggable) {
        this.menuEntries.clear();
        Hud hud = Hud.getInstance();
        if (hud != null) {
            this.menuEntries.add(new MenuEntry("\u0423\u0434\u0430\u043b\u0438\u0442\u044c", () -> hud.setHudElementEnabled(draggable.getName(), false)));
        }
        this.menuEntries.add(new MenuEntry("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043c\u0430\u0441\u0448\u0442\u0430\u0431", () -> draggable.setScaleFactor(1.0f)));
        this.menuEntries.add(new MenuEntry("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043f\u043e\u0437\u0438\u0446\u0438\u044e", () -> {
            AbstractDraggable fresh = this.createFresh(draggable);
            if (fresh != null) {
                draggable.setX(fresh.getX());
                draggable.setY(fresh.getY());
                draggable.clampToScreen();
            }
        }));
        if (draggable instanceof TargetHud) {
            TargetHud targetHud = (TargetHud)draggable;
            this.addTargetHudMenuEntries(targetHud);
        }
        if (draggable instanceof DynamicIsland) {
            this.addWatermarkMenuEntries();
        }
    }

    /** Части ватермарки: галочка — показывается, клик — убрать или вернуть. */
    private void addWatermarkMenuEntries() {
        for (String part : Hud.watermarkElements.getList()) {
            boolean enabled = Hud.watermarkElements.isSelected(part);
            this.menuEntries.add(new MenuEntry((enabled ? "✓ " : "") + part, () -> {
                ArrayList<String> selected = new ArrayList<String>(Hud.watermarkElements.getSelected());
                if (selected.contains(part)) {
                    selected.remove(part);
                } else {
                    selected.add(part);
                }
                Hud.watermarkElements.setSelected(selected);
            }, true));
        }
    }

    private void addTargetHudMenuEntries(TargetHud targetHud) {
        this.menuEntries.add(new MenuEntry((targetHud.isShowOnHover() ? "✓ " : "") + "Отображать при наведении", () -> {
            targetHud.setShowOnHover(!targetHud.isShowOnHover());
            try {
                Wiosvisual.getInstance().getFileController().saveFile(ElementsFile.class);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }


    private String toggleLabel(String name, boolean enabled) {
        return enabled ? "\u0423\u0431\u0440\u0430\u0442\u044c " + name : "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c " + name;
    }

    private float measureMenuWidth() {
        FontRenderer titleFont = Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold);
        FontRenderer itemFont = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
        float contentW = titleFont.getStringWidth(this.menuTitle);
        for (MenuEntry entry : this.menuEntries) {
            contentW = Math.max(contentW, itemFont.getStringWidth(entry.label()));
        }
        return MathHelper.clamp((float)(contentW + 20.0f), (float)84.0f, (float)220.0f);
    }

    private void openMenu(float x, float y) {
        this.menuWidth = this.measureMenuWidth();
        float totalHeight = 6.0f;
        if (this.menuMode == MenuMode.ACTIONS) {
            totalHeight += 28.0f;
        }
        totalHeight += 18.0f;
        this.menuHeight = totalHeight += 16.0f * (float)this.menuEntries.size();
        this.menuX = MathHelper.clamp((float)x, (float)4.0f, (float)((float)window.getScaledWidth() - this.menuWidth - 4.0f));
        this.menuY = MathHelper.clamp((float)y, (float)4.0f, (float)((float)window.getScaledHeight() - this.menuHeight - 4.0f));
        this.menuOpen = true;
        this.menuClosing = false;
        this.menuAnimStart = System.currentTimeMillis();
    }

    private String russianName(String name) {
        if (name == null) {
            return "";
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "watermark" -> "\u0412\u0430\u0442\u0435\u0440\u043c\u0430\u0440\u043a\u0430";
            case "target hud" -> "\u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f \u043e \u0446\u0435\u043b\u0438";
            case "cooldowns" -> "\u041f\u0435\u0440\u0435\u0437\u0430\u0440\u044f\u0434\u043a\u0438";
            case "keybinds" -> "\u041a\u043b\u0430\u0432\u0438\u0448\u0438";
            case "potions" -> "\u0417\u0435\u043b\u044c\u044f";
            case "info" -> "\u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f";
            case "armorbar" -> "\u041f\u0430\u043d\u0435\u043b\u044c \u0431\u0440\u043e\u043d\u0438";
            case "totembar" -> "\u041f\u0430\u043d\u0435\u043b\u044c \u0442\u043e\u0442\u0435\u043c\u043e\u0432";
            case "cremkeys" -> "Кейстроки";
            case "customhotbar" -> "\u041a\u0430\u0441\u0442\u043e\u043c\u043d\u044b\u0439 \u0445\u043e\u0442\u0431\u0430\u0440";
            default -> name;
        };
    }

    private boolean isInsideMenu(double mouseX, double mouseY) {
        return mouseX >= (double)this.menuX && mouseX <= (double)(this.menuX + this.menuWidth) && mouseY >= (double)this.menuY && mouseY <= (double)(this.menuY + this.menuHeight);
    }

    private int getMenuIndex(double mouseY) {
        float localY;
        float startY = this.menuY;
        if (this.menuMode == MenuMode.ACTIONS) {
            startY += 28.0f;
        }
        if ((localY = (float)mouseY - (startY += 18.0f)) < 0.0f) {
            return -1;
        }
        return (int)(localY / 16.0f);
    }

    private void beginClose() {
        if (this.menuOpen && !this.menuClosing) {
            this.menuClosing = true;
            this.menuAnimStart = System.currentTimeMillis();
        }
    }

    private void closeMenu() {
        this.menuOpen = false;
        this.menuClosing = false;
        this.menuAnimStart = 0L;
        this.targetDraggable = null;
        this.menuEntries.clear();
    }

    private List<AbstractDraggable> getVisibleDraggables(Hud hud) {
        ArrayList<AbstractDraggable> visible = new ArrayList<AbstractDraggable>();
        for (AbstractDraggable draggable : Wiosvisual.getInstance().getDraggableRepository().draggable()) {
            if (!this.isSupported(draggable) || !draggable.canDraw(hud, draggable)) continue;
            visible.add(draggable);
        }
        return visible;
    }

    private List<AbstractDraggable> getDisabledDraggables(Hud hud) {
        ArrayList<AbstractDraggable> disabled = new ArrayList<AbstractDraggable>();
        for (AbstractDraggable draggable : Wiosvisual.getInstance().getDraggableRepository().draggable()) {
            if (!this.isSupported(draggable) || hud.isHudElementEnabled(draggable.getName())) continue;
            disabled.add(draggable);
        }
        return disabled;
    }

    private AbstractDraggable getHoveredDraggable(Hud hud, double mouseX, double mouseY) {
        List<AbstractDraggable> reversed = this.getVisibleDraggables(hud);
        Collections.reverse(reversed);
        for (AbstractDraggable draggable : reversed) {
            if (!draggable.isHovered(mouseX, mouseY)) continue;
            return draggable;
        }
        return null;
    }

    private boolean isSupported(AbstractDraggable draggable) {
        return draggable != null && !"Notifications".equalsIgnoreCase(draggable.getName());
    }

    private AbstractDraggable createFresh(AbstractDraggable draggable) {
        try {
            return (AbstractDraggable)draggable.getClass().getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private void saveHudState() {
        try {
            Wiosvisual.getInstance().getFileController().saveFiles();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static float easeOutCubic(float value) {
        float t = MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private static int blend(int from, int to, float t) {
        float p = MathHelper.clamp((float)t, (float)0.0f, (float)1.0f);
        int fr = from >> 16 & 0xFF;
        int fg = from >> 8 & 0xFF;
        int fb = from & 0xFF;
        int fa = from >>> 24 & 0xFF;
        int tr = to >> 16 & 0xFF;
        int tg = to >> 8 & 0xFF;
        int tb = to & 0xFF;
        int ta = to >>> 24 & 0xFF;
        int r = (int)((float)fr + (float)(tr - fr) * p);
        int g = (int)((float)fg + (float)(tg - fg) * p);
        int b = (int)((float)fb + (float)(tb - fb) * p);
        int a = (int)((float)fa + (float)(ta - fa) * p);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static enum MenuMode {
        ADD,
        ACTIONS;

    }

    private record MenuEntry(String label, Runnable action, boolean keepOpen) {
        MenuEntry(String label, Runnable action) {
            this(label, action, false);
        }
    }
}
