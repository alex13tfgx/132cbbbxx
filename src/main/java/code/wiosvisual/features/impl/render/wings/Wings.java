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
 *  net.minecraft.class_1304
 *  net.minecraft.class_1657
 *  net.minecraft.class_1802
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_3532
 *  net.minecraft.class_4587
 *  net.minecraft.class_7833
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.lwjgl.opengl.GL11
 */
package code.wiosvisual.features.impl.render.wings;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ColorSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class Wings
extends Module {
    private static final WingPoint[] SHAPE = new WingPoint[]{new WingPoint(0.08f, 0.1f, 0.88f), new WingPoint(0.28f, 0.34f, 0.78f), new WingPoint(0.56f, 0.82f, 0.62f), new WingPoint(0.86f, 0.3f, 0.52f), new WingPoint(1.14f, 0.46f, 0.4f), new WingPoint(1.24f, 0.04f, 0.3f), new WingPoint(1.02f, -0.18f, 0.28f), new WingPoint(1.18f, -0.64f, 0.22f), new WingPoint(0.86f, -0.46f, 0.2f), new WingPoint(0.8f, -0.98f, 0.14f), new WingPoint(0.54f, -0.74f, 0.16f), new WingPoint(0.3f, -1.16f, 0.12f), new WingPoint(0.1f, -0.54f, 0.18f)};
    private final BooleanSetting self = new BooleanSetting("\u041d\u0430 \u0441\u0435\u0431\u044f", "\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0442\u044c \u043a\u0440\u044b\u043b\u044c\u044f \u043d\u0430 \u0441\u0435\u0431\u0435").setValue(true);
    private final BooleanSetting friends = new BooleanSetting("\u041d\u0430 \u0434\u0440\u0443\u0437\u0435\u0439", "\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0442\u044c \u043a\u0440\u044b\u043b\u044c\u044f \u043d\u0430 \u0434\u0440\u0443\u0437\u044c\u044f\u0445").setValue(false);
    private final BooleanSetting players = new BooleanSetting("\u041d\u0430 \u0438\u0433\u0440\u043e\u043a\u043e\u0432", "\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0442\u044c \u043a\u0440\u044b\u043b\u044c\u044f \u043d\u0430 \u0434\u0440\u0443\u0433\u0438\u0445 \u0438\u0433\u0440\u043e\u043a\u0430\u0445").setValue(false);
    private final SliderSettings size = new SliderSettings("\u0420\u0430\u0437\u043c\u0435\u0440", "\u0420\u0430\u0437\u043c\u0435\u0440 \u043a\u0440\u044b\u043b\u044c\u0435\u0432").setValue(1.0f).range(0.75f, 1.35f).step(0.05f);
    private final BooleanSetting useThemeColor = new BooleanSetting("\u0426\u0432\u0435\u0442 \u0442\u0435\u043c\u044b", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u0446\u0432\u0435\u0442 \u0442\u0435\u043c\u044b \u043a\u043b\u0438\u0435\u043d\u0442\u0430").setValue(true);
    private final ColorSetting color = new ColorSetting("\u0426\u0432\u0435\u0442", "\u0426\u0432\u0435\u0442 \u043a\u0440\u044b\u043b\u044c\u0435\u0432").value(new Color(150, 130, 255).getRGB()).presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB()).visible(() -> !this.useThemeColor.isValue());
    private float selfBodyYaw;
    private boolean selfBodyYawInitialized;
    private float glideProgress;

    public Wings() {
        super("Wings", "Wings", ModuleCategory.RENDER);
        this.setup(this.self, this.friends, this.players, this.size, this.useThemeColor, this.color);
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (Wings.mc.player == null || Wings.mc.world == null || Wings.mc.gameRenderer == null) {
            return;
        }
        float tickDelta = event.getPartialTicks();
        if (this.self.isValue() && !Wings.mc.options.getPerspective().isFirstPerson() && Wings.mc.player.isAlive() && !this.hasElytra((PlayerEntity)Wings.mc.player)) {
            try {
                this.renderWings((PlayerEntity)Wings.mc.player, tickDelta);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (this.players.isValue()) {
            for (Entity entity : Wings.mc.world.getEntities()) {
                PlayerEntity player;
                if (!(entity instanceof PlayerEntity) || (player = (PlayerEntity)entity) == Wings.mc.player || !player.isAlive() || player.isInvisible() || this.hasElytra(player)) continue;
                try {
                    this.renderWings(player, tickDelta);
                }
                catch (Exception exception) {}
            }
        } else if (this.friends.isValue()) {
            for (Entity entity : Wings.mc.world.getEntities()) {
                PlayerEntity player;
                if (!(entity instanceof PlayerEntity) || (player = (PlayerEntity)entity) == Wings.mc.player || !player.isAlive() || player.isInvisible() || this.hasElytra(player) || !FriendUtils.isFriend((Entity)player)) continue;
                try {
                    this.renderWings(player, tickDelta);
                }
                catch (Exception exception) {}
            }
        }
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private void renderWings(PlayerEntity player, float tickDelta) {
        double x = MathHelper.lerp((double)tickDelta, (double)player.lastX, (double)player.getX());
        double y = MathHelper.lerp((double)tickDelta, (double)player.lastY, (double)player.getY());
        double z = MathHelper.lerp((double)tickDelta, (double)player.lastZ, (double)player.getZ());
        float bodyYaw = this.resolveBodyYaw(player, tickDelta);
        float move = MathHelper.clamp((float)player.limbAnimator.getAnimationProgress(tickDelta), (float)0.0f, (float)1.0f);
        WingPose pose = this.resolvePose(player, tickDelta);
        if (pose == null) {
            return;
        }
        float flap = (float)Math.sin(((float)player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float open = (8.0f + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float wingScale = this.size.getValue() * pose.scaleMultiplier;
        int baseColor = this.resolveBaseColor();
        int glowColor = this.resolveGlowColor(baseColor);
        int coreColor = this.resolveCoreColor(baseColor);
        int outlineColor = baseColor;
        MatrixStack stack = new MatrixStack();
        stack.multiplyPositionMatrix(Render3D.lastWorldSpaceMatrix.getPositionMatrix());
        stack.push();
        stack.translate(x, y, z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        if (pose.preTranslateY != 0.0f || pose.preTranslateZ != 0.0f) {
            stack.translate(0.0f, pose.preTranslateY, pose.preTranslateZ);
        }
        if (pose.pitchRotation != 0.0f) {
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        }
        if (pose.rollRotation != 0.0f) {
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
        }
        stack.translate(0.0f, pose.anchorY, pose.anchorZ);
        stack.scale(wingScale, wingScale, wingScale);
        this.renderWingSide(stack, -1.0f, open, baseColor, glowColor, coreColor, outlineColor, pose);
        this.renderWingSide(stack, 1.0f, open, baseColor, glowColor, coreColor, outlineColor, pose);
        stack.pop();
    }

    private void renderWingSide(MatrixStack stack, float side, float open, int baseColor, int glowColor, int coreColor, int outlineColor, WingPose pose) {
        stack.push();
        stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));
        this.drawWingLayer(stack, side, 1.22f, Wings.setAlpha(glowColor, 48), Wings.setAlpha(glowColor, 0));
        this.drawWingLayer(stack, side, 0.84f, Wings.setAlpha(coreColor, 57), Wings.setAlpha(coreColor, 0));
        this.drawWingLayer(stack, side, 1.0f, Wings.setAlpha(baseColor, 220), Wings.setAlpha(baseColor, 10));
        this.drawWingOutline(stack, side, 1.0f, Wings.setAlpha(outlineColor, 136));
        this.drawWingRibs(stack, side, 0.96f, Wings.setAlpha(glowColor, 44));
        stack.pop();
    }

    private void drawWingLayer(MatrixStack stack, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SHAPE.length; ++i) {
            WingPoint cur = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, rootColor);
            this.vertex(buffer, matrix, side * cur.x * scale, cur.y * scale, 0.0f, this.applyPointAlpha(edgeColor, cur.alphaMul));
            this.vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0.0f, this.applyPointAlpha(edgeColor, next.alphaMul));
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.TRIANGLES, true, false), buffer);
    }

    private void drawWingOutline(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        GL11.glEnable((int)2848);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (WingPoint point : SHAPE) {
            this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f, color);
        }
        this.vertex(buffer, matrix, side * Wings.SHAPE[0].x * scale, Wings.SHAPE[0].y * scale, 0.0f, color);
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.DEBUG_LINE_STRIP, true, false), buffer);
        GL11.glDisable((int)2848);
    }

    private void drawWingRibs(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        int[] ribIndices = new int[]{2, 4, 7, 9, 11};
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int idx : ribIndices) {
            WingPoint point = SHAPE[idx];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, Wings.setAlpha(color, Math.max(8, (int)((float)Wings.alpha(color) * 0.75f))));
            this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f, this.applyPointAlpha(color, point.alphaMul));
        }
        WiosRender.draw(WiosRender.posColor(VertexFormat.DrawMode.DEBUG_LINES, true, false), buffer);
    }

    private int applyPointAlpha(int color, float multiplier) {
        return Wings.setAlpha(color, Math.max(0, Math.min(255, (int)((float)Wings.alpha(color) * multiplier))));
    }

    private static int setAlpha(int color, int a) {
        return MathHelper.clamp((int)a, (int)0, (int)255) << 24 | color & 0xFFFFFF;
    }

    private static int alpha(int color) {
        return color >> 24 & 0xFF;
    }

    private static int red(int color) {
        return color >> 16 & 0xFF;
    }

    private static int green(int color) {
        return color >> 8 & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    private static int getColor(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z).color(color);
    }

    private int resolveBaseColor() {
        return this.useThemeColor.isValue() ? ColorAssist.getClientColor() : this.color.getColor();
    }

    private int resolveGlowColor(int base) {
        return ColorAssist.interpolateColor(base, Wings.getColor(255, 255, 255, 255), 0.28f);
    }

    private int resolveCoreColor(int base) {
        return ColorAssist.interpolateColor(base, Wings.getColor(255, 255, 255, 255), 0.55f);
    }

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerpAngleDegrees((float)tickDelta, (float)player.lastBodyYaw, (float)player.bodyYaw);
        if (player != Wings.mc.player) {
            return target;
        }
        if (!this.selfBodyYawInitialized || player.age < 2) {
            this.selfBodyYaw = target;
            this.selfBodyYawInitialized = true;
            return this.selfBodyYaw;
        }
        this.selfBodyYaw = Wings.approachDegrees(this.selfBodyYaw, target, 14.0f);
        return this.selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees((float)(target - current));
        delta = MathHelper.clamp((float)delta, (float)(-maxDelta), (float)maxDelta);
        return current + delta;
    }

    private WingPose resolvePose(PlayerEntity player, float tickDelta) {
        float pitch = MathHelper.lerp((float)tickDelta, (float)player.lastPitch, (float)player.getPitch());
        if (player == Wings.mc.player) {
            this.glideProgress = MathHelper.clamp((float)(this.glideProgress + (player.isGliding() ? 0.12f : -0.2f)), (float)0.0f, (float)1.0f);
        }
        if (player.isGliding()) {
            float flightProgress = player == Wings.mc.player ? this.glideProgress : 1.0f;
            float pitchRotation = flightProgress * (-90.0f - pitch);
            return new WingPose(0.34f, 0.46f, 0.0f, 0.0f, pitchRotation, 0.0f, 0.76f, 0.92f, 0.1f, 0.58f, 0.05f, 0.06f, -5.0f, -2.0f, 0.13f);
        }
        if (player.isTouchingWater()) {
            return null;
        }
        if (player.isSneaking()) {
            return new WingPose(0.0f, 0.0f, 0.96f, 0.1f, 18.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.02f, -11.0f, -4.0f, 0.12f);
        }
        return new WingPose(0.0f, 0.0f, 1.38f, 0.1f, 0.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.02f, -11.0f, -4.0f, 0.12f);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.selfBodyYawInitialized = false;
        this.glideProgress = 0.0f;
    }

    private static final class WingPose {
        final float preTranslateY;
        final float preTranslateZ;
        final float anchorY;
        final float anchorZ;
        final float pitchRotation;
        final float rollRotation;
        final float openMultiplier;
        final float scaleMultiplier;
        final float motionSpreadBoost;
        final float flapAmplitude;
        final float sideOffset;
        final float sideYOffset;
        final float sideZOffset;
        final float sideRoll;
        final float sidePitch;
        final float flapSpeed;

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ, float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier, float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
            this(preTranslateY, preTranslateZ, anchorY, anchorZ, pitchRotation, rollRotation, openMultiplier, scaleMultiplier, motionSpreadBoost, flapAmplitude, sideOffset, 0.0f, sideZOffset, sideRoll, sidePitch, flapSpeed);
        }

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ, float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier, float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideYOffset, float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
            this.preTranslateY = preTranslateY;
            this.preTranslateZ = preTranslateZ;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.pitchRotation = pitchRotation;
            this.rollRotation = rollRotation;
            this.openMultiplier = openMultiplier;
            this.scaleMultiplier = scaleMultiplier;
            this.motionSpreadBoost = motionSpreadBoost;
            this.flapAmplitude = flapAmplitude;
            this.sideOffset = sideOffset;
            this.sideYOffset = sideYOffset;
            this.sideZOffset = sideZOffset;
            this.sideRoll = sideRoll;
            this.sidePitch = sidePitch;
            this.flapSpeed = flapSpeed;
        }
    }

    private static final class WingPoint {
        final float x;
        final float y;
        final float alphaMul;

        WingPoint(float x, float y, float alphaMul) {
            this.x = x;
            this.y = y;
            this.alphaMul = alphaMul;
        }
    }
}
