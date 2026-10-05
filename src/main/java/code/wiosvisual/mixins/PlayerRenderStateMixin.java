package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.cosmetics.ICosmeticRenderState;
import code.wiosvisual.utils.display.render.state.ILivingStateEntity;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Хранит на render-state игрока флаг «это локальный игрок» для CosmeticFeatureRenderer. */
@Mixin(value = {LivingEntityRenderState.class})
public abstract class PlayerRenderStateMixin implements ICosmeticRenderState, ILivingStateEntity {
    @Unique
    private net.minecraft.entity.LivingEntity wiosvisual$entity;

    @Override
    public net.minecraft.entity.LivingEntity wiosvisual$getEntity() {
        return this.wiosvisual$entity;
    }

    @Override
    public void wiosvisual$setEntity(net.minecraft.entity.LivingEntity entity) {
        this.wiosvisual$entity = entity;
    }

    @Unique
    private boolean wiosvisual$localPlayer;

    @Override
    public boolean wiosvisual$isLocalPlayer() {
        return this.wiosvisual$localPlayer;
    }

    @Unique
    private boolean wiosvisual$customModel;

    @Override
    public boolean wiosvisual$isCustomModel() {
        return this.wiosvisual$customModel;
    }

    @Override
    public void wiosvisual$setCustomModel(boolean value) {
        this.wiosvisual$customModel = value;
    }

    @Override
    public void wiosvisual$setLocalPlayer(boolean value) {
        this.wiosvisual$localPlayer = value;
    }
}
