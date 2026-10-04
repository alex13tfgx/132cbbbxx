package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.cosmetics.CosmeticFeatureRenderer;
import code.wiosvisual.features.impl.render.cosmetics.ICosmeticRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Добавляет слой косметики рендереру игрока. */
@Mixin(value={LivingEntityRenderer.class})
public abstract class PlayerCosmeticsMixin {
    @Shadow
    protected abstract boolean addFeature(FeatureRenderer var1);

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method={"<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Lnet/minecraft/client/render/entity/model/EntityModel;F)V"}, at={@At(value="RETURN")})
    private void Wiosvisual$addCosmeticsFeature(EntityRendererFactory.Context context, EntityModel model, float shadowRadius, CallbackInfo ci) {
        if ((Object)this instanceof PlayerEntityRenderer) {
            this.addFeature(new CosmeticFeatureRenderer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>)(Object)this));
            this.addFeature(new code.wiosvisual.features.impl.render.custommodels.CustomModelFeatureRenderer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>)(Object)this));
        }
    }

    /**
     * Косметика чисто клиентская (без сети) и должна рисоваться только на своей модели —
     * иначе она отображалась бы на всех игроках. method_62355 — переопределение
     * updateRenderState в PlayerEntityRenderer, здесь ещё жив настоящий entity. Пишется
     * в intermediary: refmap устарел.
     */
    @Inject(method={"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"}, at={@At(value="TAIL")})
    private void wiosvisual$markLocalPlayer(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof ICosmeticRenderState access) {
            access.wiosvisual$setLocalPlayer(entity == MinecraftClient.getInstance().player);
            boolean custom = code.wiosvisual.features.impl.render.CustomModels.shouldReplace(entity);
            access.wiosvisual$setCustomModel(custom);
            if (custom) {
                // Ванильное тело скрывается полностью (не «призраком»): его заменяет модель Blockbench.
                state.invisible = true;
                state.invisibleToPlayer = true;
            }
        }
    }
}
