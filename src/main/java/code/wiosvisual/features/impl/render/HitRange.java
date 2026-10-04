/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10142
 *  net.minecraft.class_10156
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_243
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4587
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.math.calc.CalcVector;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

public class HitRange
extends Module {
    private static final float CIRCLE_RADIUS = 3.0f;
    /** 3.2 блока в квадрате — дистанция, с которой цель считается «в радиусе». */
    private static final double CIRCLE_TARGET_REACH_SQ = 10.24;
    private static final int CIRCLE_SEGMENTS = 60;
    private final BooleanSetting ownCircle = new BooleanSetting("Свой круг", "Рисовать круг вокруг себя").setValue(true);
    private final BooleanSetting otherCircles = new BooleanSetting("Чужие круги", "Рисовать круги вокруг игроков в броне").setValue(true);
    private final BooleanSetting circleFill = new BooleanSetting("Заливка", "Заливать круг полупрозрачным цветом").setValue(true);
    private final SliderSettings fillOpacity = new SliderSettings("Прозрачность заливки", "Непрозрачность заливки круга").setValue(0.25f).range(0.0f, 1.0f).step(0.05f).visible(() -> this.circleFill.isValue());
    private final SliderSettings lineWidth = new SliderSettings("Толщина линии", "Толщина контура круга").setValue(2.0f).range(0.5f, 5.0f).step(0.5f);
    private final SliderSettings floorHeight = new SliderSettings("Высота над полом", "Насколько круг приподнят над землёй").setValue(0.02f).range(0.0f, 0.5f).step(0.01f);
    private final ColorSetting inReachColor = new ColorSetting("Цель в радиусе", "Цвет, когда рядом есть противник в броне").setColor(new Color(0, 255, 60).getRGB());
    private final ColorSetting noTargetColor = new ColorSetting("Нет цели", "Цвет, когда противника рядом нет").setColor(new Color(255, 40, 40).getRGB());

    public HitRange() {
        super("HitRange", "Hit Range", ModuleCategory.RENDER);
        this.setup(this.ownCircle, this.otherCircles, this.circleFill, this.fillOpacity, this.lineWidth, this.floorHeight, this.inReachColor, this.noTargetColor);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (HitRange.mc.player == null || HitRange.mc.world == null) {
            return;
        }
        this.renderCircles(e);
    }

    /**
     * «Круг» из Pulse: круги досягаемости под собой и под игроками в броне. Зелёный —
     * рядом есть противник в броне ближе 3.2 блока, красный — нет. Невидимых и игроков
     * без брони пропускаем: по голым обычно не бьют, а невидимку круг бы выдал.
     */
    private void renderCircles(WorldRenderEvent e) {
        boolean targetInReach = false;
        for (PlayerEntity p : HitRange.mc.world.getPlayers()) {
            if (HitRange.isCircleTarget(p) && p.squaredDistanceTo((Entity)HitRange.mc.player) <= CIRCLE_TARGET_REACH_SQ) {
                targetInReach = true;
                break;
            }
        }
        Color base = new Color(targetInReach ? this.inReachColor.getColor() : this.noTargetColor.getColor(), true);
        float alphaFill = this.circleFill.isValue() ? Math.max(0.0f, Math.min(1.0f, this.fillOpacity.getValue())) : 0.0f;
        int fillArgb = Math.round(alphaFill * 255.0f) << 24 | base.getRed() << 16 | base.getGreen() << 8 | base.getBlue();
        int lineArgb = 240 << 24 | base.getRed() << 16 | base.getGreen() << 8 | base.getBlue();

        Matrix4f matrix = e.getStack().peek().getPositionMatrix();
        double yOffset = this.floorHeight.getValue();

        if (alphaFill > 0.01f) {
            boolean any = false;
            BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            for (PlayerEntity p : HitRange.mc.world.getPlayers()) {
                if (!this.shouldDrawCircle(p)) continue;
                Vec3d pos = CalcVector.lerpPosition((Entity)p);
                HitRange.appendCircleFill(fill, matrix, pos.x, pos.y + yOffset, pos.z, fillArgb);
                any = true;
            }
            // Пустой буфер отправлять нельзя: method_60800 на нём бросает исключение.
            if (any) {
                WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.TRIANGLES, true, false), fill);
            }
        }

        boolean anyLine = false;
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (PlayerEntity p : HitRange.mc.world.getPlayers()) {
            if (!this.shouldDrawCircle(p)) continue;
            Vec3d pos = CalcVector.lerpPosition((Entity)p);
            HitRange.appendCircleOutline(lines, matrix, pos.x, pos.y + yOffset, pos.z, lineArgb);
            anyLine = true;
        }
        if (anyLine) {
            WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.DEBUG_LINES, true, false), lines);
        }
    }

    private boolean shouldDrawCircle(PlayerEntity p) {
        if (p == HitRange.mc.player) {
            return this.ownCircle.isValue();
        }
        return this.otherCircles.isValue() && HitRange.isCircleTarget(p);
    }

    private static boolean isCircleTarget(PlayerEntity p) {
        if (p == null || p == HitRange.mc.player || p.isInvisible()) {
            return false;
        }
        return !p.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                || !p.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                || !p.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                || !p.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private static void appendCircleFill(BufferBuilder buffer, Matrix4f matrix, double cx, double y, double cz, int color) {
        float fy = (float)y;
        for (int i = 0; i < CIRCLE_SEGMENTS; ++i) {
            double a1 = Math.PI * 2 * (double)i / (double)CIRCLE_SEGMENTS;
            double a2 = Math.PI * 2 * (double)(i + 1) / (double)CIRCLE_SEGMENTS;
            buffer.vertex(matrix, (float)cx, fy, (float)cz).color(color);
            buffer.vertex(matrix, (float)(cx - Math.sin(a2) * CIRCLE_RADIUS), fy, (float)(cz + Math.cos(a2) * CIRCLE_RADIUS)).color(color);
            buffer.vertex(matrix, (float)(cx - Math.sin(a1) * CIRCLE_RADIUS), fy, (float)(cz + Math.cos(a1) * CIRCLE_RADIUS)).color(color);
        }
    }

    private static void appendCircleOutline(BufferBuilder buffer, Matrix4f matrix, double cx, double y, double cz, int color) {
        float fy = (float)y;
        for (int i = 0; i < CIRCLE_SEGMENTS; ++i) {
            double a1 = Math.PI * 2 * (double)i / (double)CIRCLE_SEGMENTS;
            double a2 = Math.PI * 2 * (double)(i + 1) / (double)CIRCLE_SEGMENTS;
            buffer.vertex(matrix, (float)(cx - Math.sin(a1) * CIRCLE_RADIUS), fy, (float)(cz + Math.cos(a1) * CIRCLE_RADIUS)).color(color);
            buffer.vertex(matrix, (float)(cx - Math.sin(a2) * CIRCLE_RADIUS), fy, (float)(cz + Math.cos(a2) * CIRCLE_RADIUS)).color(color);
        }
    }
}
