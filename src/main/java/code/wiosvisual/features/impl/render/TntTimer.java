package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import code.wiosvisual.utils.math.projection.Projection;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Таймер до взрыва ТНТ: над каждым тикающим блоком висит плашка с иконкой
 * и остатком времени. Чем дальше динамит, тем мельче подпись.
 */
public class TntTimer
extends Module {
    private final BooleanSetting icon = new BooleanSetting("Иконка", "Показывать иконку ТНТ рядом со временем").setValue(true);
    private final BooleanSetting background = new BooleanSetting("Подложка", "Тёмная плашка под текстом").setValue(true);
    private final SliderSettings distance = new SliderSettings("Дальность", "Максимальная дальность отрисовки в блоках").range(8.0f, 128.0f).step(1.0f).setValue(64.0f);

    public static TntTimer getInstance() {
        return Instance.get(TntTimer.class);
    }

    public TntTimer() {
        super("TntTimer", "TNT Timer", ModuleCategory.RENDER);
        this.setup(this.icon, this.background, this.distance);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (TntTimer.mc.world == null || TntTimer.mc.player == null) {
            return;
        }
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = GuiCompat.stack(context);
        FontRenderer font = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);
        float tickDelta = tickCounter.getTickProgress(false);
        double maxDistance = this.distance.getValue();
        for (Entity entity : TntTimer.mc.world.getEntities()) {
            if (!(entity instanceof TntEntity)) continue;
            TntEntity tnt = (TntEntity)entity;
            Vec3d pos = tnt.getLerpedPos(tickDelta).add(0.0, 0.5, 0.0);
            double dist = new net.minecraft.util.math.Vec3d(TntTimer.mc.player.getX(), TntTimer.mc.player.getY(), TntTimer.mc.player.getZ()).distanceTo(pos);
            if (dist > maxDistance) continue;
            if (!Projection.isInFrontOfCamera(pos)) continue;
            Vec3d screen = Projection.worldSpaceToScreenSpace(pos);
            if (screen == null) continue;
            this.renderLabel(context, matrix, font, tnt, screen, (float)dist);
        }
    }

    private void renderLabel(DrawContext context, MatrixStack matrix, FontRenderer font, TntEntity tnt, Vec3d screen, float dist) {
        String text = String.format(Locale.US, "%.1f", Float.valueOf((float)tnt.getFuse() / 20.0f));
        // Дальний динамит подписывается мельче, чтобы не забивать экран.
        float scale = MathHelper.clamp((float)(1.0f - dist / 20.0f), (float)0.5f, (float)1.0f);
        float textW = font.getStringWidth(text);
        float iconW = this.icon.isValue() ? 12.0f : 0.0f;
        float contentW = iconW + textW;
        matrix.push();
        matrix.translate((float)screen.x, (float)screen.y, 0.0f);
        matrix.scale(scale, scale, 1.0f);
        float x = -contentW / 2.0f;
        if (this.background.isValue()) {
            rectangle.render(ShapeProperties.create(matrix, x - 4.0f, -3.0f, contentW + 8.0f, 15.0f).round(4.0f).color(ColorAssist.replAlpha(0, 130)).build());
            Rectangle.flushPending();
        }
        if (this.icon.isValue()) {
            Render2D.defaultDrawStack(context, Items.TNT.getDefaultStack(), x, -1.0f, false, false, 0.62f);
        }
        font.drawString(matrix, text, x + iconW, 0.0f, ColorAssist.multAlpha(-1, 1.0f));
        matrix.pop();
    }
}
