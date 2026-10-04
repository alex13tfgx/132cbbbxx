/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1293
 *  net.minecraft.class_1294
 */
package code.wiosvisual.features.impl.render;

import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright
extends Module {
    public final SelectSetting modeSetting = new SelectSetting("\u0420\u0435\u0436\u0438\u043c", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0440\u0435\u0436\u0438\u043c \u0440\u0430\u0431\u043e\u0442\u044b").value("\u041d\u043e\u0447\u043d\u043e\u0435 \u0437\u0440\u0435\u043d\u0438\u0435", "\u0413\u0430\u043c\u043c\u0430").selected("\u0413\u0430\u043c\u043c\u0430");
    private double previousGamma = 1.0;

    public static Fullbright getInstance() {
        return Instance.get(Fullbright.class);
    }

    public Fullbright() {
        super("Fullbright", "Full Bright", ModuleCategory.RENDER);
        this.setup(this.modeSetting);
    }

    @Override
    public void activate() {
        super.activate();
        if (mc != null && Fullbright.mc.options != null) {
            this.previousGamma = (Double)Fullbright.mc.options.getGamma().getValue();
        }
    }

    @Override
    public void deactivate() {
        if (mc != null && Fullbright.mc.options != null) {
            Fullbright.mc.options.getGamma().setValue(this.previousGamma);
        }
        if (Fullbright.mc.player != null && this.modeSetting.isSelected("\u041d\u043e\u0447\u043d\u043e\u0435 \u0437\u0440\u0435\u043d\u0438\u0435")) {
            Fullbright.mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (Fullbright.mc.player == null || Fullbright.mc.options == null) {
            return;
        }
        if (this.modeSetting.isSelected("\u0413\u0430\u043c\u043c\u0430")) {
            Fullbright.mc.options.getGamma().setValue(10.0);
        } else if (this.modeSetting.isSelected("\u041d\u043e\u0447\u043d\u043e\u0435 \u0437\u0440\u0435\u043d\u0438\u0435") && !Fullbright.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            Fullbright.mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        }
    }
}
