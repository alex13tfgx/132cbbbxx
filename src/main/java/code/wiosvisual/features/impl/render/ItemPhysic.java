package code.wiosvisual.features.impl.render;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.Instance;

public class ItemPhysic
extends Module {
    public final SelectSetting mode = new SelectSetting("\u0424\u0438\u0437\u0438\u043a\u0430", "\u0420\u0435\u0436\u0438\u043c \u0444\u0438\u0437\u0438\u043a\u0438 \u0434\u0440\u043e\u043f\u0430").value("\u041e\u0431\u044b\u0447\u043d\u0430\u044f", "\u0412\u0440\u0430\u0449\u0435\u043d\u0438\u0435").selected("\u041e\u0431\u044b\u0447\u043d\u0430\u044f");
    public final SliderSettings groundOffset = new SliderSettings("\u041e\u0442\u0441\u0442\u0443\u043f \u043e\u0442 \u0437\u0435\u043c\u043b\u0438", "\u041d\u0430 \u0441\u043a\u043e\u043b\u044c\u043a\u043e \u043f\u0440\u0435\u0434\u043c\u0435\u0442 \u043f\u043e\u0434\u043d\u044f\u0442 \u043d\u0430\u0434 \u0437\u0435\u043c\u043b\u0451\u0439").range(0.0f, 0.5f).setValue(0.0625f).step(0.0025f);
    public final SliderSettings spinSpeed = new SliderSettings("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0432\u0440\u0430\u0449\u0435\u043d\u0438\u044f", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0432\u0440\u0430\u0449\u0435\u043d\u0438\u044f \u0432 \u043f\u043e\u043b\u0451\u0442\u0435").range(0.0f, 60.0f).setValue(15.0f).step(0.5f);

    public ItemPhysic() {
        super("ItemPhysic", "Item Physic", ModuleCategory.RENDER);
        this.setup(this.mode, this.groundOffset, this.spinSpeed);
    }

    public static ItemPhysic getInstance() {
        return Instance.get(ItemPhysic.class);
    }

    public static boolean active() {
        ItemPhysic itemPhysic = ItemPhysic.getInstance();
        return itemPhysic != null && itemPhysic.isState();
    }

    public boolean isFlatOnGround() {
        return this.mode.isSelected("\u041e\u0431\u044b\u0447\u043d\u0430\u044f");
    }
}
