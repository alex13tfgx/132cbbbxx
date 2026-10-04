/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1657
 *  net.minecraft.class_239
 *  net.minecraft.class_3966
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.math.time.TimerUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TapeMouse
extends Module
implements QuickImports {
    private final SelectSetting buttonSetting = new SelectSetting("\u041a\u043d\u043e\u043f\u043a\u0430", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043a\u043d\u043e\u043f\u043a\u0443 \u043c\u044b\u0448\u0438").value("\u041b\u0435\u0432\u0430\u044f \u043a\u043d\u043e\u043f\u043a\u0430", "\u041f\u0440\u0430\u0432\u0430\u044f \u043a\u043d\u043e\u043f\u043a\u0430").selected("\u041b\u0435\u0432\u0430\u044f \u043a\u043d\u043e\u043f\u043a\u0430");
    private final SliderSettings delaySetting = new SliderSettings("\u0417\u0430\u0434\u0435\u0440\u0436\u043a\u0430 (\u043c\u0441)", "\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b \u043c\u0435\u0436\u0434\u0443 \u043a\u043b\u0438\u043a\u0430\u043c\u0438").range(100, 30000).step(50.0f).setValue(450.0f);
    private final BooleanSetting waitCooldown = new BooleanSetting("Ждать перезарядку", "Бить только полностью заряженным ударом").setValue(true);
    private final TimerUtil timer = new TimerUtil();

    public TapeMouse() {
        super("TapeMouse", "Tape Mouse", ModuleCategory.MISC);
        this.setup(this.buttonSetting, this.delaySetting, this.waitCooldown);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!this.state) {
            return;
        }
        if (TapeMouse.mc.player == null || TapeMouse.mc.world == null || TapeMouse.mc.currentScreen != null || TapeMouse.mc.interactionManager == null) {
            return;
        }
        long delayMs = (long)this.delaySetting.getValue();
        if (!this.timer.hasTimeElapsed(delayMs)) {
            return;
        }
        if (this.buttonSetting.isSelected("\u041b\u0435\u0432\u0430\u044f \u043a\u043d\u043e\u043f\u043a\u0430")) {
            HitResult hitResult = TapeMouse.mc.crosshairTarget;
            if (hitResult instanceof EntityHitResult) {
                EntityHitResult entityHit = (EntityHitResult)hitResult;
                if (this.waitCooldown.isValue() && TapeMouse.mc.player.getAttackCooldownProgress(0.0f) < 1.0f) {
                    return;
                }
                TapeMouse.mc.interactionManager.attackEntity((PlayerEntity)TapeMouse.mc.player, entityHit.getEntity());
                TapeMouse.mc.player.resetTicksSince();
                TapeMouse.mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            TapeMouse.mc.interactionManager.interactItem((PlayerEntity)TapeMouse.mc.player, Hand.MAIN_HAND);
            TapeMouse.mc.player.swingHand(Hand.MAIN_HAND);
        }
        this.timer.resetCounter();
    }
}
