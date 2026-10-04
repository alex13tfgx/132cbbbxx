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
import code.wiosvisual.utils.math.projection.Projection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Имена стрел арбалета над самим снарядом: сервер даёт стрелам собственные названия
 * («Стрела обледенения» и прочие), но ванилла показывает их только по customNameVisible.
 */
public class ArrowNames
extends Module {
    private final BooleanSetting onlyMine = new BooleanSetting("Только свои", "Показывать имена только у своих стрел").setValue(true);
    private final BooleanSetting colored = new BooleanSetting("Цвета сервера", "Сохранять цвета из названия стрелы").setValue(true);
    private final BooleanSetting shadow = new BooleanSetting("Тень", "Рисовать тень под текстом").setValue(true);
    private final SliderSettings offset = new SliderSettings("Высота", "Насколько поднять подпись над стрелой").range(0.0f, 1.0f).step(0.05f).setValue(0.35f);
    private final SliderSettings distance = new SliderSettings("Дальность", "Максимальная дальность отрисовки в блоках").range(8.0f, 128.0f).step(1.0f).setValue(64.0f);

    public static ArrowNames getInstance() {
        return Instance.get(ArrowNames.class);
    }

    public ArrowNames() {
        super("ArrowNames", "Arrow Names", ModuleCategory.RENDER);
        this.setup(this.onlyMine, this.colored, this.shadow, this.offset, this.distance);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (ArrowNames.mc.world == null || ArrowNames.mc.player == null) {
            return;
        }
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = GuiCompat.stack(context);
        FontRenderer font = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);
        float tickDelta = tickCounter.getTickProgress(false);
        double maxDistance = this.distance.getValue();
        for (Entity entity : ArrowNames.mc.world.getEntities()) {
            if (!(entity instanceof PersistentProjectileEntity)) continue;
            PersistentProjectileEntity arrow = (PersistentProjectileEntity)entity;
            Text name = this.resolveName(arrow);
            if (name == null) continue;
            if (ArrowNames.mc.player.squaredDistanceTo((Entity)arrow) > maxDistance * maxDistance) continue;
            Vec3d pos = arrow.getLerpedPos(tickDelta).add(0.0, (double)this.offset.getValue(), 0.0);
            if (!Projection.isInFrontOfCamera(pos)) continue;
            Vec3d screen = Projection.worldSpaceToScreenSpace(pos);
            if (screen == null) continue;
            String plain = name.getString();
            if (this.shadow.isValue()) {
                font.drawCenteredString(matrix, plain, screen.x + 0.75, screen.y + 0.75, ColorAssist.replAlpha(0, 170));
            }
            if (this.colored.isValue()) {
                // findStyle разворачивает Text в строку с §-кодами, так что цвета сервера сохраняются.
                StringBuilder styled = new StringBuilder();
                font.findStyle(styled, name);
                font.drawCenteredString(matrix, styled.toString(), screen.x, screen.y, -1);
            } else {
                font.drawCenteredString(matrix, plain, screen.x, screen.y, -1);
            }
        }
    }

    /** Имя берётся с самого снаряда; безымянные ванильные стрелы пропускаются. */
    private Text resolveName(PersistentProjectileEntity arrow) {
        if (this.onlyMine.isValue() && arrow.getOwner() != ArrowNames.mc.player) {
            return null;
        }
        Text custom = arrow.getCustomName();
        if (custom == null || custom.getString().isEmpty()) {
            return null;
        }
        return custom;
    }
}
