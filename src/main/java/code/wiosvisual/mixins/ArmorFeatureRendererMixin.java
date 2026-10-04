/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_10034
 *  net.minecraft.class_10055
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_572
 *  net.minecraft.class_970
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.HitColor;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ArmorFeatureRenderer.class})
public abstract class ArmorFeatureRendererMixin<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>>
implements QuickImports {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private void Wiosvisual$hideArmorForCustomModels(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, S state, float limbAngle, float limbDistance, CallbackInfo ci) {
        HitColor.endArmorTint();
        if (ArmorFeatureRendererMixin.mc.world == null) {
            return;
        }
        LivingEntity living = null;
        if (state instanceof PlayerEntityRenderState) {
            PlayerEntityRenderState playerState = (PlayerEntityRenderState)state;
            Entity entity = ArmorFeatureRendererMixin.mc.world.getEntityById(playerState.id);
            if (!(entity instanceof PlayerEntity)) {
                return;
            }
            PlayerEntity player = (PlayerEntity)entity;
            living = player;
        } else {
            // У не-игроков в состоянии рендера нет id, поэтому берём существо, которое
            // EntityRenderDispatcher рисует прямо сейчас — иначе броня мобов не красилась вовсе.
            Entity current = HitColor.getRenderedEntity();
            if (!(current instanceof LivingEntity)) {
                return;
            }
            living = (LivingEntity)current;
        }
        HitColor.beginArmorTint(living, state.hurt);
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void Wiosvisual$clearHitColorArmorTint(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, S state, float limbAngle, float limbDistance, CallbackInfo ci) {
        HitColor.endArmorTint();
    }
}
