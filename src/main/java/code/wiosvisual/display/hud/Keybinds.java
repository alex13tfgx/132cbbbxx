/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_4587
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.shape.implement.Rectangle;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.display.hud.HudListPanel;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.ModuleRepository;
import code.wiosvisual.utils.client.chat.StringHelper;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.BackgroundIcons;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class Keybinds
extends AbstractDraggable {
    private final List<Module> keysList = new ArrayList<Module>();
    private long lastKeyChange = 0L;
    private String currentRandomKey = "NONE";
    private static final String[] EXAMPLE_KEYS = new String[]{"A", "B", "C", "D", "E"};
    private static final Random RANDOM = new Random();
    private final FontRenderer fontMedium = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
    private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
    private final FontRenderer fontBind = Fonts.getSize(13, Fonts.Type.DEFAULT);
    private final FontRenderer fontModule = Fonts.getSize(14, Fonts.Type.DEFAULT);
    private final FontRenderer categoryIcon = Fonts.getSize(16, Fonts.Type.WIOSVISUAL_ICONS);
    private static final float KEYBIND_ROW_ICON_SIZE = 9.0f;

    public Keybinds() {
        super("Keybinds", 300, 40, 80, 23, true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    @Override
    public boolean visible() {
        Hud.getInstance();
        if (!Hud.interfaceSettings.isSelected("Keybinds") || !Hud.getInstance().state) {
            return false;
        }
        return !this.keysList.isEmpty() || PlayerInteractionHelper.isChat(Keybinds.mc.currentScreen);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            FontRenderer iconFont = Fonts.getSize(15, Fonts.Type.WIOSVISUAL_ICONS);
            float pX = (float)(this.getX() + this.getWidth()) - iconFont.getStringWidth("p") - 8.0f;
            float pY = (float)this.getY() + 9.5f;
            float pWidth = iconFont.getStringWidth("p");
            float pHeight = iconFont.getStringHeight("p");
            if (mouseX >= (double)pX && mouseX <= (double)(pX + pWidth) && mouseY >= (double)pY && mouseY <= (double)(pY + pHeight)) {
                Hud.getInstance();
                ArrayList<String> selected = new ArrayList<String>(Hud.interfaceSettings.getSelected());
                selected.remove("Keybinds");
                Hud.getInstance();
                Hud.interfaceSettings.setSelected(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        long currentTime;
        Wiosvisual wiosvisual = Wiosvisual.getInstance();
        if (wiosvisual == null) {
            this.keysList.clear();
            return;
        }
        ModuleRepository moduleRepository = wiosvisual.getModuleRepository();
        this.keysList.clear();
        if (moduleRepository != null) {
            for (Module module : moduleRepository.modules()) {
                if (module.getAnimation().getOutput().floatValue() == 0.0f || module.getKey() == -1 || "ClickGui".equals(module.getName()) || module.isHidden()) continue;
                this.keysList.add(module);
            }
        }
        if (this.keysList.isEmpty() && PlayerInteractionHelper.isChat(Keybinds.mc.currentScreen) && (currentTime = System.currentTimeMillis()) - this.lastKeyChange >= 1000L) {
            this.currentRandomKey = EXAMPLE_KEYS[RANDOM.nextInt(EXAMPLE_KEYS.length)];
            this.lastKeyChange = currentTime;
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        this.drawMode1(context);
    }

    private final DeltaHud.ListPanel deltaPanel = new DeltaHud.ListPanel();
    private final FontRenderer deltaHeaderFont = Fonts.getSize(14, Fonts.Type.DEFAULT);

    private void drawMode1(DrawContext context) {
        MatrixStack matrix = GuiCompat.stack(context);
        List<DeltaHud.Row> rows = new ArrayList<DeltaHud.Row>();
        if (this.keysList.isEmpty() && PlayerInteractionHelper.isChat(Keybinds.mc.currentScreen)) {
            rows.add(new DeltaHud.Row(1.0f, "Module", this.currentRandomKey, 0xFFFFFFFF, this.categoryGlyph("b")));
        } else {
            for (Module module : this.keysList) {
                float animValue = module.getAnimation().getOutput().floatValue();
                if (!(animValue > 0.0f)) continue;
                rows.add(new DeltaHud.Row(animValue, module.getName(), StringHelper.getBindName(module.getKey()), 0xFFFFFFFF,
                        this.categoryGlyph(Keybinds.getCategoryIcon(module.getCategory()))));
            }
        }
        float[] size = this.deltaPanel.draw(context, matrix, rectangle, this.getX(), this.getY(), BackgroundIcons.KEYBINDS,
                HudTheme.hotkeysIconBright(255), "Hot Keys", this.deltaHeaderFont, this.fontModule, rows);
        this.setWidth((int)Math.ceil(size[0]));
        this.setHeight((int)Math.ceil(size[1]));
    }

    private DeltaHud.IconDrawer categoryGlyph(String glyph) {
        return (ctx, m, x, y, size, a) -> {
            Rectangle.flushPending();
            this.categoryIcon.drawString(m, glyph, x, this.categoryIcon.getCenteredY(y, size), DeltaHud.withAlpha(HudTheme.hotkeysIcon(255), a));
        };
    }

    private void drawKeybindCategoryIcon(DrawContext context, MatrixStack matrix, ModuleCategory category, float x, float rowY, int color1, int color2, int alpha) {
        String icon = Keybinds.getCategoryIcon(category);
        float iconY = HudListPanel.alignedRowTextY(this.fontModule, this.categoryIcon, rowY);
        this.drawIconGlow(matrix, icon, x, iconY, ColorAssist.replAlpha(color1, alpha));
        this.categoryIcon.drawRainbowString(matrix, icon, x, iconY, color1, color2);
    }

    /**
     * Ореол под иконкой строки: тот же глиф несколько раз рисуется со сдвигом
     * и низкой альфой, поэтому значок светится цветом темы.
     */
    private void drawIconGlow(MatrixStack matrix, String icon, float x, float y, int color) {
        int glow = ColorAssist.replAlpha(color, Math.round((float)ColorAssist.alpha(color) * 0.17f));
        float[][] offsets = new float[][]{{-0.75f, 0.0f}, {0.75f, 0.0f}, {0.0f, -0.75f}, {0.0f, 0.75f}};
        for (float[] offset : offsets) {
            this.categoryIcon.drawString(matrix, icon, x + offset[0], y + offset[1], glow);
        }
    }

    private static String getCategoryIcon(ModuleCategory category) {
        if (category == ModuleCategory.RENDER) {
            return "e";
        }
        if (category == ModuleCategory.PLAYER) {
            return "c";
        }
        if (category == ModuleCategory.MISC) {
            return "f";
        }
        return category.getReadableName().substring(0, 1);
    }

    private void drawMode2(DrawContext context) {
        MatrixStack matrix = GuiCompat.stack(context);
        FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.BOLD);
        FontRenderer moduleFont = Fonts.getSize(13, Fonts.Type.BOLD);
        FontRenderer keyFont = Fonts.getSize(13, Fonts.Type.BOLD);
        float x = this.getX();
        float y = this.getY();
        float panelWidth = 95.0f;
        float titleHeight = 24.0f;
        float playerHeight = 16.0f;
        float spacing = -3.5f;
        float titleSpacing = -1.5f;
        float statusPanelWidth = 16.0f;
        float mainPanelWidth = panelWidth - statusPanelWidth - 11.0f;
        boolean showExamples = this.keysList.isEmpty() && PlayerInteractionHelper.isChat(Keybinds.mc.currentScreen);
        int moduleCount = showExamples ? 3 : this.keysList.size();
        float contentHeight = (float)moduleCount * (playerHeight + spacing) - (moduleCount > 0 ? spacing : 0.0f);
        float totalHeight = titleHeight + titleSpacing + 7.0f + contentHeight;
        this.setWidth((int)panelWidth);
        this.setHeight((int)totalHeight);
        rectangle.render(ShapeProperties.create(matrix, x, y, panelWidth, totalHeight).round(7.0f).color(ThemeManager.BackgroundGui.getColor()).build());
        rectangle.render(ShapeProperties.create(matrix, x + 4.5f, y + 4.5f, panelWidth - 9.0f, titleHeight - 9.0f).round(3.5f).color(ThemeManager.BackgroundSettings.getColor()).build());
        titleFont.drawString(matrix, "Binds", x + 10.0f, y + 10.0f, ThemeManager.textColor.getColor());
        Fonts.getSize(19, Fonts.Type.ICONS).drawString(matrix, "D", x + panelWidth - 19.0f, y + 9.5f, ThemeManager.textColor.getColor());
        float currentY = y + titleHeight + titleSpacing;
        rectangle.render(ShapeProperties.create(matrix, x + 4.5f, currentY, mainPanelWidth, contentHeight).round(3.5f).color(ThemeManager.BackgroundSettings.getColor()).build());
        float statusPanelX = x + 4.5f + mainPanelWidth + 2.5f;
        rectangle.render(ShapeProperties.create(matrix, statusPanelX, currentY, statusPanelWidth, contentHeight).round(3.5f).color(ThemeManager.BackgroundSettings.getColor()).build());
        if (showExamples) {
            String[] exampleModules = new String[]{"Flight", "Speed", "ClickGui"};
            String[] exampleBinds = new String[]{"R", "V", "F"};
            for (int i = 0; i < 3; ++i) {
                this.drawModuleBind(matrix, currentY, x, statusPanelX, statusPanelWidth, playerHeight, moduleFont, keyFont, exampleModules[i], exampleBinds[i]);
                currentY += playerHeight + spacing;
            }
        } else {
            for (Module module : this.keysList) {
                String moduleName = module.getVisibleName();
                String keyName = StringHelper.getBindName(module.getKey());
                this.drawModuleBind(matrix, currentY, x, statusPanelX, statusPanelWidth, playerHeight, moduleFont, keyFont, moduleName, keyName);
                currentY += playerHeight + spacing;
            }
        }
    }

    private void drawModuleBind(MatrixStack matrix, float currentY, float x, float statusPanelX, float statusPanelWidth, float playerHeight, FontRenderer moduleFont, FontRenderer keyFont, String moduleName, String keyName) {
        float barWidth = 1.3f;
        float barHeight = 7.0f;
        float barX = x + 4.5f + 2.8f;
        float barY = currentY + 5.5f;
        rectangle.render(ShapeProperties.create(matrix, barX, barY, barWidth, barHeight).round(0.9f).color(HudTheme.hudGray(255), HudTheme.hudGraySecondary(255), HudTheme.hudGray(255), HudTheme.hudGraySecondary(255)).build());
        float nameX = barX + barWidth + 3.5f;
        float nameY = currentY + playerHeight - 9.0f;
        moduleFont.drawString(matrix, moduleName, nameX, nameY, ThemeManager.textColor.getColor());
        float keyTextWidth = keyFont.getStringWidth(keyName);
        float maxWidth = statusPanelWidth - 2.0f;
        if (keyTextWidth > maxWidth) {
            float scale = maxWidth / keyTextWidth;
            float keyX = statusPanelX + 1.0f;
            float keyY = currentY + playerHeight - 9.0f;
            matrix.push();
            matrix.translate(keyX, keyY, 0.0f);
            matrix.scale(scale, scale, 1.0f);
            keyFont.drawString(matrix, keyName, 0.0, 0.0, HudTheme.hudGray(255));
            matrix.pop();
        } else {
            float keyX = statusPanelX + (statusPanelWidth - keyTextWidth) / 2.0f;
            float keyY = currentY + playerHeight - 9.0f;
            keyFont.drawString(matrix, keyName, keyX, keyY, HudTheme.hudGray(255));
        }
    }
}
