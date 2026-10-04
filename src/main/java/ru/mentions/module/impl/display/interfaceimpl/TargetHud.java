package ru.mentions.module.impl.display.interfaceimpl;

import org.joml.Matrix3x2fStack;
import ru.mentions.Client;
import code.wiosvisual.events.render.DrawEvent;
import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.impl.display.InterFace;
import ru.mentions.theme.ThemeColor;
import ru.mentions.utils.animation.Animation;
import ru.mentions.utils.animation.Easings;
import ru.mentions.utils.animation.satoshi.Direction;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;
import ru.mentions.utils.colors.ColorFormatting;
import ru.mentions.utils.colors.ColorUtil;
import ru.mentions.utils.math.ServerUtil;
import ru.mentions.utils.render.Draw;
import ru.mentions.utils.render.ItemRender;
import ru.mentions.utils.render.RenderUtil;
import ru.mentions.utils.render.font.Fonts;
import ru.mentions.utils.taskript.StopWatch;
import com.mojang.blaze3d.textures.GpuTexture;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class TargetHud implements element {

    private static float S = 1.0F;

    private static float ITEM_SIZE = 8F * S;
    private static float ARMOR_PAD = 2.5F * S;
    private static float ARMOR_X = 10F * S;
    private static float ARMOR_Y = 12F * S;

    private static float W_BASE = 120F * S;
    private static float H_BASE = 32F * S;
    private static float RADIUS = 12F * S;
    private static float ANIM_OFFSET = 8F * S;

    private static float HEAD_CONTAINER_W = 30F * S;
    private static float HEAD_OFFSET_X = 3F * S;
    private static float HEAD_OFFSET_Y = 3F * S;
    private static float CONTENT_OFFSET_X = 34F * S;

    private static float BAR_OFFSET_X = 5F * S;
    private static float BAR_OFFSET_Y = 9F * S;
    private static float BAR_MARGIN_R = 10F * S;
    private static float BAR_HEIGHT = 5F * S;

    private static float NAME_Y = 4.5F * S;
    private static float HP_Y = 13.5F * S;
    private static float NAME_SIZE = 7F * S;
    private static float HP_SIZE = 6F * S;

    private static float GLOW_RADIUS = 12F * S;
    private static float BLUR_RADIUS = 20F * S;
    private static float FACE_SIZE = 24F * S;

    private final ru.mentions.utils.animation.satoshi.Animation openAnimation = new EaseInOutQuad(200, 1);
    private final Animation settingsAnimation = new Animation();
    private final Animation animHP = new Animation();
    private final Animation animHpText = new Animation();
    private final StopWatch time = new StopWatch();
    private LivingEntity target;
    private boolean inWorld;
    private boolean settingsOpen;
    private float lastX, lastY, lastW, lastH;
    private float popupX, popupY, popupW, popupH;
    private float hpColorX, hpColorY, hpColorW, hpColorH;
    private float view1X, view1Y, view1W, view1H;
    private float view2X, view2Y, view2W, view2H;

    /** Текущая цель плашки или null, если плашка скрыта. */
    public LivingEntity currentTarget() {
        return inWorld && target != null && !target.isRemoved() ? target : null;
    }

    @Override
    public void onRender(DragSetting drag, InterFace interFace) {
    }

    public void onRender(DragSetting drag, InterFace interFace, DrawEvent eventDisplay) {
        S = InterFace.getInstance().sizeHud.getValue();
        ITEM_SIZE = 8F * S;
        ARMOR_PAD = 2.5F * S;
        ARMOR_X = 10F * S;
        ARMOR_Y = 12F * S;
        W_BASE = 120F * S;
        H_BASE = 32F * S;
        RADIUS = 12F * S;
        ANIM_OFFSET = 8F * S;
        HEAD_CONTAINER_W = 30F * S;
        HEAD_OFFSET_X = 3F * S;
        HEAD_OFFSET_Y = 3F * S;
        CONTENT_OFFSET_X = 34F * S;
        BAR_OFFSET_X = 5F * S;
        BAR_OFFSET_Y = 9F * S;
        BAR_MARGIN_R = 10F * S;
        BAR_HEIGHT = 3F * S;
        NAME_Y = 4.5F * S;
        HP_Y = 13.5F * S;
        NAME_SIZE = 7F * S;
        HP_SIZE = 6F * S;
        GLOW_RADIUS = 12F * S;
        BLUR_RADIUS = 20F * S;
        FACE_SIZE = 24F * S;

        if (mc.player == null || mc.world == null) return;

        LivingEntity auraTarget = null;

        if( mc.targetedEntity != null) {
            target = mc.targetedEntity.getEntity();
            time.reset();
        }

        if (mc.currentScreen instanceof ChatScreen) {
            target = mc.player;
            time.reset();
        }

        inWorld = target != null && mc.world != null && !target.isRemoved();
        boolean out = !inWorld || time.finished(400);
        openAnimation.setDirection(out ? Direction.BACKWARDS : Direction.FORWARDS);

        if (openAnimation.getOutput() <= 0.0 || target == null) {
            drag.active = false;
            return;
        }

        drag.active = true;
        drag.size.set(W_BASE, H_BASE);

        float x = drag.position.x;
        float y = drag.position.y;
        float w = drag.size.x;
        float h = drag.size.y;
        float alpha = openAnimation.getOutput();
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;

        float addALL = ANIM_OFFSET - ANIM_OFFSET * alpha;

        HudPanel.fill(x + addALL, y, W_BASE, H_BASE, alpha, RADIUS,
                ColorUtil.replAlpha(ru.mentions.theme.ThemeColor.getHudBackgroundColor(), InterFace.getInstance().alphaHUD.getValue() * alpha));
        Draw.outline(x + addALL, y, W_BASE, H_BASE, 0.75F, ThemeColor.getOutlineColor(alpha * 0.08F), RADIUS);

        RenderUtil.Render2D.rect(x + addALL + HEAD_OFFSET_X, y + HEAD_OFFSET_Y, FACE_SIZE, FACE_SIZE,
                ColorUtil.getColor(255, 0.06F * alpha), 12F * S);

        if (target instanceof PlayerEntity) {
            drawFace(target, eventDisplay.getPartialTicks(), x + addALL + HEAD_OFFSET_X, y + HEAD_OFFSET_Y, alpha);
        } else {
            drawMobIcon(eventDisplay, target, x + addALL + HEAD_OFFSET_X + FACE_SIZE / 2F,
                    y + HEAD_OFFSET_Y + FACE_SIZE / 2F, FACE_SIZE, alpha);
        }

        RenderUtil.Render2D.rect(x + addALL + 31F * S, y + 5F * S, 0.5F, H_BASE - 10F * S,
                ThemeColor.getSeparatorColor(alpha), 0.25F);

        animHP.update();
        float barW = W_BASE - CONTENT_OFFSET_X - BAR_MARGIN_R;
        animHP.run(Math.round(getHealth(target) / target.getMaxHealth() * barW), 0.5F, Easings.BACK_OUT);

        animHpText.update();
        animHpText.run(getHealth(target), 0.15F, Easings.LINEAR);

        float barX = x + addALL + CONTENT_OFFSET_X;
        float barY = y + H_BASE - BAR_OFFSET_Y;

        int hpColor = ColorUtil.getClientColor(1);

        float displayHp = animHpText.get();
        String hpText = ServerUtil.isCopyTime()
                ? String.format("%.0f", displayHp)
                : (target.isInvisible() ? "null" : String.format("%.0f", displayHp));
        String name = target.getName().getString().replace(mc.player.getName().getString(),
                mc.player.getName().getString());

        float barRadius = BAR_HEIGHT / 2F;

        RenderUtil.Render2D.rect(barX, barY, barW, BAR_HEIGHT,
                ColorUtil.multAlpha(ColorUtil.getColor(255,0.1F), alpha), barRadius);

        RenderUtil.Render2D.gradientRect(
                barX,
                barY,
                Math.min(barW, animHP.get()),
                BAR_HEIGHT,
                new int[]{
                        ColorUtil.multDark(ColorUtil.replAlpha(hpColor, alpha), 0.5F),
                        ColorUtil.replAlpha(hpColor, alpha),
                        ColorUtil.replAlpha(hpColor, alpha),
                        ColorUtil.multDark(ColorUtil.replAlpha(hpColor, alpha), 0.5F)
                },
                barRadius
        );

        Fonts.sf_regular.drawFadingText(name, x + addALL + CONTENT_OFFSET_X, y + NAME_Y,
                W_BASE - CONTENT_OFFSET_X - 5F * S,
                ColorUtil.getColor(255, alpha), NAME_SIZE);

        Fonts.sf_regular.draw("Здоровья: " + ColorFormatting.getColor(ColorUtil.replAlpha(ColorUtil.client(), alpha)) + hpText,
                x + addALL + CONTENT_OFFSET_X,
                y + HP_Y, HP_SIZE, ColorUtil.getColor(255, alpha));

        // Броня над плашкой убрана: её показывает отдельный элемент Hud «Броня».
    }

    private void renderArmor(DrawEvent eventDisplay, LivingEntity entity, float armorX, float armorY, float alpha) {
        List<ItemStack> armorItems = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.FEET,
                EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
        }) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (!stack.isEmpty()) armorItems.add(stack);
        }

        if (armorItems.isEmpty()) return;

        float bgX = armorX - ITEM_SIZE / 2 - ARMOR_PAD;
        float bgY = armorY - ITEM_SIZE / 2 - ARMOR_PAD + 0.5F * S;
        float bgW = armorItems.size() * ITEM_SIZE + ARMOR_PAD * 2;
        float bgH = ITEM_SIZE + ARMOR_PAD * 2;

        // Draw.glow(bgX, bgY, bgW, bgH - 0.5F * S, ColorUtil.getColor(0, 0.1F * alpha), 4 * S, GLOW_RADIUS, 1);
        HudPanel.fill(bgX, bgY, bgW, bgH, alpha, 4 * S,
                ColorUtil.replAlpha(ru.mentions.theme.ThemeColor.getHudBackgroundColor(), InterFace.getInstance().alphaHUD.getValue() * alpha), BLUR_RADIUS, 1, 1, 4);

        for (ItemStack stack : armorItems) {
            Matrix3x2fStack matrix = eventDisplay.getDrawContext().getMatrices();
            matrix.pushMatrix();
            matrix.translate(armorX, armorY);
            matrix.scale(0.5F * S, 0.5F * S);
            ItemRender.drawItemWithContext(eventDisplay.getDrawContext(), stack, -8, -8, alpha, alpha);
            matrix.popMatrix();
            armorX += ITEM_SIZE;
        }
    }

    private void drawMobIcon(DrawEvent eventDisplay, LivingEntity entity, float centerX, float centerY,
            float size, float alpha) {
        try {
            SpawnEggItem egg = SpawnEggItem.forEntity(entity.getType());
            if (egg == null) {
                drawFace(entity, eventDisplay.getPartialTicks(), centerX - size / 2F, centerY - size / 2F, alpha);
                return;
            }
            ItemStack stack = new ItemStack(egg);
            Matrix3x2fStack matrix = eventDisplay.getDrawContext().getMatrices();
            matrix.pushMatrix();
            matrix.translate(centerX, centerY);
            float s = size / 16F;
            matrix.scale(s, s);
            ItemRender.drawItemWithContext(eventDisplay.getDrawContext(), stack, -8, -8, alpha, alpha);
            matrix.popMatrix();
        } catch (Exception ignored) {
        }
    }

    private int getHealthColor(LivingEntity entity) {
        float pct = MathHelper.clamp(getHealth(entity) / Math.max(1F, entity.getMaxHealth()), 0F, 1F);
        int red = ColorUtil.getColor(255, 75, 75, 255);
        int yellow = ColorUtil.getColor(255, 205, 85, 255);
        int green = ColorUtil.getColor(95, 220, 115, 255);
        return pct < 0.5F
                ? ColorUtil.overCol(red, yellow, pct * 2F)
                : ColorUtil.overCol(yellow, green, (pct - 0.5F) * 2F);
    }

    private float getHealth(LivingEntity entity) {
        float hp = entity.getHealth() + entity.getAbsorptionAmount();
        if (entity instanceof PlayerEntity player && mc.world != null) {
            ScoreboardObjective scoreBoard = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (scoreBoard != null) {
                MutableText text = ReadableScoreboardScore.getFormattedScore(
                        mc.world.getScoreboard().getScore(player, scoreBoard),
                        scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY)
                );
                try {
                    hp = Float.parseFloat(ColorUtil.removeFormatting(text.getString()));
                } catch (Exception ignored) {
                }
            }
        }
        return MathHelper.clamp(hp, 0, entity.getMaxHealth() + entity.getAbsorptionAmount());
    }

    private void drawFace(LivingEntity lastTarget, float lastTickDelta, float x, float y, float alpha) {
        try {
            EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(lastTarget);
            if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) return;

            LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer =
                    (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;

            LivingEntityRenderState state = renderer.getAndUpdateRenderState(lastTarget, lastTickDelta);
            Identifier textureLocation = renderer.getTexture(state);

            float hurtPercent = lastTarget.hurtTime > 0 ? lastTarget.hurtTime / 10.0f : 0.0f;
            int r = 255;
            int g = (int) (255 * (1.0f - hurtPercent));
            int b = (int) (255 * (1.0f - hurtPercent));
            int color = new Color(r, g, b, (int) (255 * alpha)).getRGB();

            float texW = 64f;
            float texH = 64f;
            try {
                AbstractTexture texture = mc.getTextureManager().getTexture(textureLocation);
                if (texture != null) {
                    GpuTexture gpu = texture.getGlTexture();
                    if (gpu != null) {
                        texW = gpu.getWidth(0);
                        texH = gpu.getHeight(0);
                    }
                }
            } catch (Exception ignored) {
            }
            if (texW <= 0f) texW = 64f;
            if (texH <= 0f) texH = 64f;

            RenderUtil.Images.texture(textureLocation, x, y, FACE_SIZE, FACE_SIZE,
                    8f / texW, 8f / texH, 16f / texW, 16f / texH, color, 0, 12);
        } catch (Exception ignored) {
        }
    }

    private boolean inRect(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }
}
