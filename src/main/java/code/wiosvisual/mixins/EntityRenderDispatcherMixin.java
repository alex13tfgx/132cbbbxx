/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1531
 *  net.minecraft.class_1657
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_4587
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_5498
 *  net.minecraft.class_897
 *  net.minecraft.class_898
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.features.impl.render.HitColor;
import code.wiosvisual.features.impl.render.NoRender;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.geometry.Render3D;
import code.wiosvisual.utils.math.calc.Calculate;
import code.wiosvisual.utils.math.projection.Projection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EntityRenderManager.class})
public class EntityRenderDispatcherMixin
implements QuickImports {
    @ModifyExpressionValue(method={"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"}, at={@At(value="FIELD", target="Lnet/minecraft/client/render/entity/state/EntityRenderState;invisible:Z")})
    private boolean renderHitboxHook(boolean original, @Local(ordinal=0, argsOnly=true) Entity entity) {
        NoRender noRender;
        PlayerEntity player;
        if (entity instanceof ArmorStandEntity) {
            return true;
        }
        if (entity instanceof PlayerEntity && (player = (PlayerEntity)entity) != EntityRenderDispatcherMixin.mc.player && (noRender = NoRender.getInstance()) != null && noRender.shouldHideFriends() && FriendUtils.isFriend((Entity)player)) {
            return true;
        }
        return original;
    }

    @Inject(method={"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void renderFriendsHook(Entity entity, double x, double y, double z, float yaw, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? extends Entity, ?> renderer, CallbackInfo ci) {
        NoRender noRender;
        PlayerEntity player;
        HitColor.setRenderedEntity(entity);
        if (entity instanceof PlayerEntity && (player = (PlayerEntity)entity) != EntityRenderDispatcherMixin.mc.player && (noRender = NoRender.getInstance()) != null && noRender.shouldHideFriends() && FriendUtils.isFriend((Entity)player)) {
            ci.cancel();
        }
    }

    @Inject(method={"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"}, at={@At(value="RETURN")})
    private void Wiosvisual$clearRenderedEntity(Entity entity, double x, double y, double z, float yaw, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<? extends Entity, ?> renderer, CallbackInfo ci) {
        HitColor.setRenderedEntity(null);
    }

    @Inject(method={"renderHitbox"}, at={@At(value="HEAD")}, cancellable=true)
    private static void renderHitboxHook(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, float red, float green, float blue, CallbackInfo ci) {
        if (!Wiosvisual.getInstance().getBoxESPRepository().entities.containsKey(entity.getType())) {
            EntityRenderDispatcherMixin.renderBox(entity);
        }
        ci.cancel();
    }

    @Unique
    private static void renderBox(Entity entity) {
        if (entity != EntityRenderDispatcherMixin.mc.player && entity.isInvisible()) {
            return;
        }
        if (entity != EntityRenderDispatcherMixin.mc.player || !EntityRenderDispatcherMixin.mc.options.getPerspective().equals((Object)Perspective.FIRST_PERSON)) {
            int color = FriendUtils.isFriend(entity) ? ColorAssist.getFriendColor() : ColorAssist.getClientColor();
            Vec3d offset = Calculate.interpolate(entity).subtract(new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getZ()));
            Box box = entity.getBoundingBox().offset(offset);
            if (Projection.canSee(box)) {
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity)entity;
                    float width = entity.getWidth();
                    Vec3d eyeMin = entity.getEyePos().add(offset).add((double)(-width / 2.0f), 0.0, (double)(-width / 2.0f));
                    Render3D.drawBox(box, ColorAssist.multRed(color, 1 + living.hurtTime), 2.0f, true, true, true);
                    Render3D.drawLine(eyeMin, eyeMin.add((double)width, 0.0, 0.0), ColorAssist.RED, 2.0f, true);
                    Render3D.drawLine(eyeMin.add((double)width, 0.0, 0.0), eyeMin.add((double)width, 0.0, (double)width), ColorAssist.RED, 2.0f, true);
                    Render3D.drawLine(eyeMin, eyeMin.add(0.0, 0.0, (double)width), ColorAssist.RED, 2.0f, true);
                    Render3D.drawLine(eyeMin.add(0.0, 0.0, (double)width), eyeMin.add((double)width, 0.0, (double)width), ColorAssist.RED, 2.0f, true);
                } else {
                    Render3D.drawBox(box, color, 2.0f, true, true, true);
                }
            }
        }
    }
}
