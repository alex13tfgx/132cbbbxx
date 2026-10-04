/*
 * Короткое приседание в момент удара (shift-tab). Порт одноимённой фичи из Pulse.
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.client.util.InputUtil;

public class ShiftTab
extends Module {
    private final SliderSettings sneakTicks = new SliderSettings("Тиков приседа", "Сколько тиков держать шифт после удара").setValue(4.0f).range(1, 10).step(1.0f);

    private int ticksLeft;
    private boolean restorePending;
    private boolean previousSneakPressed;

    public ShiftTab() {
        super("ShiftTab", "Shift Tab", ModuleCategory.MISC);
        this.setup(this.sneakTicks);
    }

    @Override
    public void deactivate() {
        this.ticksLeft = 0;
        this.restorePending = false;
        if (mc.options == null) {
            return;
        }
        mc.options.sneakKey.setPressed(this.isSneakKeyHeld());
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        // Удар отменён (например, NoFriendDamage) — реагировать не на что.
        if (e.isCancelled()) {
            return;
        }
        this.startSneakBurst();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) {
            return;
        }
        if (this.ticksLeft > 0) {
            mc.options.sneakKey.setPressed(true);
            mc.player.setSneaking(true);
            --this.ticksLeft;
        } else if (this.restorePending) {
            mc.options.sneakKey.setPressed(this.previousSneakPressed);
            mc.player.setSneaking(this.previousSneakPressed);
            this.restorePending = false;
        }
    }

    private void startSneakBurst() {
        if (mc.player == null || mc.options == null) {
            return;
        }
        this.previousSneakPressed = this.isSneakKeyHeld();
        this.ticksLeft = (int)this.sneakTicks.getValue();
        this.restorePending = true;
        mc.options.sneakKey.setPressed(true);
        mc.player.setSneaking(true);
    }

    /** Зажат ли шифт физически - чтобы вернуть состояние после приседания. */
    private boolean isSneakKeyHeld() {
        if (mc.getWindow() == null || mc.options == null) {
            return false;
        }
        return InputUtil.isKeyPressed(mc.getWindow(), mc.options.sneakKey.getDefaultKey().getCode());
    }
}
