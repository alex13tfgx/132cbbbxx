/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_1799
 *  net.minecraft.class_243
 *  net.minecraft.class_332
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 */
package code.wiosvisual.utils.display.render;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.math.calc.Calculate;
import code.wiosvisual.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public final class UseIndicatorRenderer
implements QuickImports {
    private static final double[] SCREEN = new double[3];
    private static final float BACKGROUND_RADIUS = 12.0f;
    private static final float ITEM_SCALE = 0.7f;
    private static final int BACKGROUND_COLOR = -871362544;

    private UseIndicatorRenderer() {
    }

    public static void render(DrawContext context, LivingEntity entity, ItemStack stack, Camera camera) {
        if (context == null || stack == null || stack.isEmpty()) {
            return;
        }
        Vec3d position = Calculate.interpolate((Entity)entity).add(0.0, (double)UseIndicatorRenderer.getBodyOffset(entity), 0.0);
        if (!Projection.isInFrontOfCamera(position = UseIndicatorRenderer.offsetTowardCamera(entity, position, camera))) {
            return;
        }
        if (!Projection.worldSpaceToScreenSpaceInto(position.x, position.y, position.z, SCREEN)) {
            return;
        }
        float screenX = (float)SCREEN[0];
        float screenY = (float)SCREEN[1];
        float entityScale = entity.isBaby() ? 0.75f : 1.0f;
        float radius = 12.0f * entityScale;
        float itemScale = 0.7f * entityScale;
        MatrixStack matrices = GuiCompat.stack(context);
        matrices.push();
        matrices.translate(screenX, screenY, 0.0f);
        float diameter = radius * 2.0f;
        rectangle.render(ShapeProperties.create(matrices, -radius, -radius, diameter, diameter).round(radius).color(-871362544).build());
        float itemSize = 16.0f * itemScale;
        matrices.push();
        matrices.translate(-itemSize / 2.0f, -itemSize / 2.0f, 0.0f);
        matrices.scale(itemScale, itemScale, 1.0f);
        context.drawItem(stack, 0, 0);
        matrices.pop();
        matrices.pop();
    }

    private static Vec3d offsetTowardCamera(LivingEntity entity, Vec3d position, Camera camera) {
        if (UseIndicatorRenderer.mc.player == entity && UseIndicatorRenderer.mc.options.getPerspective().isFirstPerson()) {
            return position;
        }
        Vec3d toCamera = camera.pos.subtract(position);
        double lengthSq = toCamera.lengthSquared();
        if (lengthSq < 1.0E-6) {
            return position;
        }
        return position.add(toCamera.normalize().multiply(0.55));
    }

    private static float getBodyOffset(LivingEntity entity) {
        PlayerEntity player;
        float offset = entity.getHeight() * 0.55f;
        if (entity.isInSneakingPose()) {
            offset -= 0.18f;
        }
        if (entity.isSwimming()) {
            offset = entity.getHeight() * 0.35f;
        }
        if (entity instanceof PlayerEntity && (player = (PlayerEntity)entity).isGliding()) {
            offset = entity.getHeight() * 0.42f;
        }
        return offset;
    }
}
