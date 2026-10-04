package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.render.WorldRenderEvent;
import code.wiosvisual.features.impl.render.cosmetics.CosmeticCatalog;
import code.wiosvisual.features.impl.render.cosmetics.CosmeticPets;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.Instance;
import java.util.EnumMap;
import java.util.Map;

/**
 * Косметика на своём игроке: накидка, крылья, рюкзак, питомец и шапка — по одному предмету
 * из каждой категории. Видна только тебе (от третьего лица и в инвентаре).
 */
public class Cosmetics
extends Module {
    private final Map<CosmeticCatalog.Category, SelectSetting> slots = new EnumMap<CosmeticCatalog.Category, SelectSetting>(CosmeticCatalog.Category.class);

    public static Cosmetics getInstance() {
        return Instance.get(Cosmetics.class);
    }

    public Cosmetics() {
        super("Cosmetics", "Cosmetics", ModuleCategory.COSMETICS);
        for (CosmeticCatalog.Category category : CosmeticCatalog.Category.values()) {
            this.slots.put(category, new SelectSetting(category.title, "Предмет в слоте «" + category.title + "»").value(CosmeticCatalog.options(category)));
        }
        this.setup(this.slots.values().toArray(new SelectSetting[0]));
    }

    /** Питомец рисуется в мире, а не на модели игрока: бегает следом и виден от первого лица. */
    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        CosmeticPets.render(this.selected(CosmeticCatalog.Category.PET), e);
    }

    /** Надетый предмет категории или null, если модуль выключен или выбрано «Нет». */
    public CosmeticCatalog.Entry selected(CosmeticCatalog.Category category) {
        if (!this.isState()) {
            return null;
        }
        SelectSetting setting = this.slots.get(category);
        return setting == null ? null : CosmeticCatalog.find(category, setting.getSelected());
    }
}
