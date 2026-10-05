package code.wiosvisual.utils.display.render.state;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;

/**
 * Сущность, из которой собран render-state. В 1.21.11 рендер двухфазный: сначала собираются
 * состояния, потом из них рисуются модели, и в момент отрисовки «текущей сущности» уже нет.
 * State создаётся заново каждый кадр, поэтому ссылка не протухает.
 */
public interface ILivingStateEntity {
    LivingEntity wiosvisual$getEntity();

    void wiosvisual$setEntity(LivingEntity entity);

    static LivingEntity of(Object state) {
        return state instanceof LivingEntityRenderState && state instanceof ILivingStateEntity link ? link.wiosvisual$getEntity() : null;
    }
}
