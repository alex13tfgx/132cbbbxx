/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_7833
 */
package code.wiosvisual.display.hud;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.RotationAxis;

public final class HudDraggableRenderer {
    private HudDraggableRenderer() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }
        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) {
            return;
        }
        List<AbstractDraggable> draggables = Wiosvisual.getInstance().getDraggableRepository().draggable();
        int count = draggables.size();
        for (int i = 0; i < count; ++i) {
            AbstractDraggable draggable = draggables.get(i);
            if (draggable.canDraw(hud, draggable)) {
                draggable.startAnimation();
            } else {
                draggable.stopAnimation();
            }
            float alpha = draggable.getScaleAnimation().getOutput().floatValue();
            if (draggable.isCloseAnimationFinished()) continue;
            draggable.updateEditorMotion();
            float offsetX = draggable.getRenderX() - (float)draggable.getX();
            float offsetY = draggable.getRenderY() - (float)draggable.getY();
            float sway = draggable.getSwayAngle();
            // Сглаженное следование и покачивание затухают экспонентой и не доходят до нуля:
            // элемент навсегда оставался сдвинут на доли пикселя, и текст мылился.
            double guiScale = client.getWindow().getScaleFactor();
            offsetX = Math.abs(offsetX) < 0.05f ? 0.0f : (float)(Math.round(offsetX * guiScale) / guiScale);
            offsetY = Math.abs(offsetY) < 0.05f ? 0.0f : (float)(Math.round(offsetY * guiScale) / guiScale);
            if (Math.abs(sway) < 0.05f) {
                sway = 0.0f;
            }
            float centerX = (float)draggable.getX() + (float)draggable.getWidth() / 2.0f;
            float centerY = (float)draggable.getY() + (float)draggable.getHeight() / 2.0f;
            try {
                float scale = draggable.getScaleFactor();
                GuiCompat.stack(context).push();
                GuiCompat.stack(context).translate(offsetX, offsetY, 0.0f);
                // Поворот и масштаб — только когда они реально есть: иначе матрица остаётся целочисленной.
                if (sway != 0.0f || Math.abs(scale - 1.0f) > 0.001f) {
                    GuiCompat.stack(context).translate(centerX, centerY, 0.0f);
                    if (sway != 0.0f) {
                        GuiCompat.stack(context).multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sway));
                    }
                    GuiCompat.stack(context).scale(scale, scale, 1.0f);
                    GuiCompat.stack(context).translate(-centerX, -centerY, 0.0f);
                }
                Rectangle.beginBatch();
                // Запоминаем матрицу элемента: панели, которые внутри себя ещё раз масштабируются
                // (например Potions), иначе отдавали HudShine размеры без учёта этого масштаба.
                Rectangle.setPanelBase(GuiCompat.stack(context).peek().getPositionMatrix());
                try {
                    draggable.drawDraggable(context);
                }
                finally {
                    Rectangle.clearPanelBase();
                    Rectangle.endBatch();
                }
                draggable.clampToScreen();
                GuiCompat.stack(context).pop();
                continue;
            }
            finally {
            }
        }
    }
}
