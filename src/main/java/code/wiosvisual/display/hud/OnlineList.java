package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.animation.implement.EaseOut;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.client.net.BackendConfig;
import code.wiosvisual.utils.client.net.ClientRole;
import code.wiosvisual.utils.client.net.OnlineUser;
import code.wiosvisual.utils.client.net.PresenceService;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/**
 * HUD-панель «кто сейчас в клиенте»: ник и роль каждого игрока, данные приходят
 * с бэкенда через {@link PresenceService}. Без настроенного адреса панель скрыта.
 */
public class OnlineList extends AbstractDraggable {
    private static final int MAX_ROWS = 10;
    private static final float ROW_HEIGHT = 11.0f;

    private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
    private final FontRenderer fontName = Fonts.getSize(14, Fonts.Type.DEFAULT);
    private final FontRenderer fontRole = Fonts.getSize(13, Fonts.Type.DEFAULT);
    private final List<OnlineUser> snapshot = new ArrayList<>();

    public OnlineList() {
        super("Online", 300, 150, 110, 23, true);
        this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
    }

    @Override
    public boolean visible() {
        Hud.getInstance();
        if (!Hud.interfaceSettings.isSelected("Online") || !Hud.getInstance().state) {
            return false;
        }
        if (!BackendConfig.isConfigured()) {
            return false;
        }
        return !this.snapshot.isEmpty() || PlayerInteractionHelper.isChat(OnlineList.mc.currentScreen);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            FontRenderer iconFont = Fonts.getSize(15, Fonts.Type.WIOSVISUAL_ICONS);
            float pX = (float) (this.getX() + this.getWidth()) - iconFont.getStringWidth("p") - 8.0f;
            float pY = (float) this.getY() + 9.5f;
            if (mouseX >= (double) pX && mouseX <= (double) (pX + iconFont.getStringWidth("p"))
                    && mouseY >= (double) pY && mouseY <= (double) (pY + iconFont.getStringHeight("p"))) {
                Hud.getInstance();
                ArrayList<String> selected = new ArrayList<>(Hud.interfaceSettings.getSelected());
                selected.remove("Online");
                Hud.getInstance();
                Hud.interfaceSettings.setSelected(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        this.snapshot.clear();
        List<OnlineUser> users = PresenceService.getInstance().getOnlineUsers();
        for (int i = 0; i < users.size() && i < MAX_ROWS; ++i) {
            this.snapshot.add(users.get(i));
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = GuiCompat.stack(context);
        List<OnlineUser> rows = new ArrayList<>(this.snapshot);
        boolean placeholder = rows.isEmpty();
        int total = PresenceService.getInstance().getOnlineCount();
        String title = "Online " + total;

        float posX = this.getX();
        float posY = this.getY();
        float width = HudListPanel.headerMinWidth(this.fontHeader, title);
        if (placeholder) {
            width = Math.max(width, HudListPanel.rowWidth(this.fontName, "Player", 0.0f, this.fontRole, "User"));
        }
        for (OnlineUser user : rows) {
            width = Math.max(width, HudListPanel.rowWidth(this.fontName, user.nameForList(), 0.0f, this.fontRole, user.role().label()));
        }

        float contentHeight = ROW_HEIGHT * (placeholder ? 1 : rows.size());
        float panelHeight = HudListPanel.panelHeight(contentHeight);

        Hud.getInstance();
        rectangle.render(ShapeProperties.create(matrix, posX, posY, width, panelHeight)
                .round(5.0f).softness(2.0f).color(HudTheme.panelTop(242), HudTheme.panelTop(242), HudTheme.panelBottom(242), HudTheme.panelBottom(242)).build());

        float headerTextY = HudListPanel.headerTextY(this.fontHeader, posY, title);
        this.fontHeader.drawString(matrix, title, posX + 7.0f, headerTextY, ThemeManager.textColor.getColor());

        float rowY = HudListPanel.contentStartY(posY);
        if (placeholder) {
            this.drawRow(matrix, posX, rowY, width, "Player", ClientRole.USER);
            rowY += ROW_HEIGHT;
        } else {
            for (OnlineUser user : rows) {
                this.drawRow(matrix, posX, rowY, width, user.nameForList(), user.role());
                rowY += ROW_HEIGHT;
            }
        }

        this.setWidth((int) width);
        this.setHeight((int) panelHeight);
    }

    private void drawRow(MatrixStack matrix, float posX, float rowY, float width, String name, ClientRole role) {
        float nameY = HudListPanel.rowTextY(this.fontName, rowY, name);
        this.fontName.drawString(matrix, name, Math.round(posX + 7.0f), Math.round(nameY), ThemeManager.textColor.getColor());

        String label = role.label();
        float labelWidth = this.fontRole.getStringWidth(label);
        float labelX = posX + width - 6.5f - labelWidth;
        this.fontRole.drawString(matrix, label, Math.round(labelX),
                Math.round(HudListPanel.rowTextY(this.fontRole, rowY, label)), role.color());
    }
}
