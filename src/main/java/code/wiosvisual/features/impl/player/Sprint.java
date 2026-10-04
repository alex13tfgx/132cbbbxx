/*
 * Автоспринт: клиент сам держит бег, пока игрок двигается.
 */
package code.wiosvisual.features.impl.player;

import code.wiosvisual.events.player.SprintEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.input.MoveUtil;

public class Sprint
extends Module {
    /** Ванильный режим бежит только вперёд, мультинаправленный — в любую сторону. */
    private final SelectSetting mode = new SelectSetting("Режим", "Куда разрешено спринтить").value("Ванильный", "Мультинаправленный").selected("Ванильный");
    private final BooleanSetting keepInAir = new BooleanSetting("В прыжке", "Не сбрасывать спринт, пока игрок в воздухе").setValue(true);
    private final BooleanSetting whileSneaking = new BooleanSetting("В приседе", "Спринтить даже с зажатым шифтом").setValue(false);

    public Sprint() {
        super("Sprint", "Sprint", ModuleCategory.PLAYER);
        this.setup(this.mode, this.keepInAir, this.whileSneaking);
    }

    @EventHandler
    public void onSprint(SprintEvent e) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!this.canSprintNow()) {
            return;
        }
        e.setSprinting(true);
    }

    private boolean canSprintNow() {
        if (!MoveUtil.isMoving()) {
            return false;
        }
        if (!this.whileSneaking.isValue() && mc.player.isSneaking()) {
            return false;
        }
        if (!this.keepInAir.isValue() && !mc.player.isOnGround()) {
            return false;
        }
        // В ванильном режиме бег разрешён только при движении вперёд.
        if (this.mode.isSelected("Ванильный") && mc.player.input.getMovementInput().y <= 0.0f) {
            return false;
        }
        // Голод и слепота — ванильные ограничения, их не обходим.
        if (mc.player.getHungerManager().getFoodLevel() <= 6 && !mc.player.getAbilities().creativeMode) {
            return false;
        }
        return !mc.player.isUsingItem() || this.mode.isSelected("Мультинаправленный");
    }
}
