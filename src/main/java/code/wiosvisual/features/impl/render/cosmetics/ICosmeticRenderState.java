package code.wiosvisual.features.impl.render.cosmetics;

/**
 * Помечает class_10042/class_10055 (PlayerEntityRenderState) флагом «это локальный игрок».
 * Косметика — чисто клиентская фича без сети, поэтому рисуется только на своей модели
 * (см. PlayerCosmeticsMixin#wiosvisual$markLocalPlayer), иначе она бы отображалась
 * на всех игроках, которых видит клиент.
 */
public interface ICosmeticRenderState {
    boolean wiosvisual$isLocalPlayer();

    void wiosvisual$setLocalPlayer(boolean value);

    boolean wiosvisual$isCustomModel();

    void wiosvisual$setCustomModel(boolean value);
}
