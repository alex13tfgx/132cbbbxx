/*
 * Автокоманды: список команд отправляется при входе в мир и/или по таймеру.
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.events.render.WorldLoadEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.util.ArrayList;
import java.util.List;

public class AutoCommand
extends Module {
    /** Команды перечисляются через «;», ведущий слеш необязателен. */
    private final TextSetting commands = new TextSetting("Команды", "Список через ; — например /hub;/spawn").setText("").setMax(256);
    private final BooleanSetting onJoin = new BooleanSetting("При входе", "Отправлять команды после захода в мир").setValue(true);
    private final SliderSettings joinDelay = new SliderSettings("Задержка входа", "Сколько секунд ждать после захода").setValue(3.0f).range(0, 30).step(1.0f).visible(() -> this.onJoin.isValue());
    private final BooleanSetting repeat = new BooleanSetting("Повторять", "Отправлять список ещё и по таймеру").setValue(false);
    private final SliderSettings interval = new SliderSettings("Интервал", "Период повтора в секундах").setValue(60.0f).range(5, 600).step(5.0f).visible(() -> this.repeat.isValue());
    /** Пауза между командами внутри одной пачки, чтобы сервер не резал их как флуд. */
    private final SliderSettings spacing = new SliderSettings("Пауза между командами", "Задержка внутри пачки, в тиках").setValue(10.0f).range(1, 60).step(1.0f);

    private final List<String> queue = new ArrayList<String>();
    private int joinCountdown = -1;
    private int repeatCountdown = -1;
    private int sendCountdown;

    public AutoCommand() {
        super("AutoCommand", "Auto Command", ModuleCategory.MISC);
        this.setup(this.commands, this.onJoin, this.joinDelay, this.repeat, this.interval, this.spacing);
    }

    @Override
    public void activate() {
        this.reset();
        this.repeatCountdown = this.repeat.isValue() ? this.seconds(this.interval.getValue()) : -1;
    }

    @Override
    public void deactivate() {
        this.reset();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        this.reset();
        if (this.onJoin.isValue()) {
            this.joinCountdown = this.seconds(this.joinDelay.getValue());
        }
        if (this.repeat.isValue()) {
            this.repeatCountdown = this.seconds(this.interval.getValue());
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.player.networkHandler == null) {
            return;
        }
        if (this.joinCountdown > 0 && --this.joinCountdown == 0) {
            this.enqueueAll();
        }
        if (this.repeat.isValue()) {
            if (this.repeatCountdown < 0) {
                this.repeatCountdown = this.seconds(this.interval.getValue());
            }
            if (this.repeatCountdown > 0 && --this.repeatCountdown == 0) {
                this.enqueueAll();
                this.repeatCountdown = this.seconds(this.interval.getValue());
            }
        } else {
            this.repeatCountdown = -1;
        }
        if (this.queue.isEmpty()) {
            return;
        }
        if (this.sendCountdown > 0) {
            --this.sendCountdown;
            return;
        }
        this.send(this.queue.remove(0));
        this.sendCountdown = (int)this.spacing.getValue();
    }

    private void enqueueAll() {
        this.queue.clear();
        for (String entry : this.commands.getText().split(";")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            this.queue.add(trimmed);
        }
        this.sendCountdown = 0;
    }

    private void send(String entry) {
        if (entry.startsWith("/")) {
            String command = entry.substring(1).trim();
            if (command.isEmpty()) {
                return;
            }
            mc.player.networkHandler.sendChatCommand(command);
        } else {
            mc.player.networkHandler.sendChatMessage(entry);
        }
    }

    /** Секунды настройки в тики; ноль тоже должен сработать, поэтому минимум — один тик. */
    private int seconds(float value) {
        return Math.max(1, Math.round(value * 20.0f));
    }

    private void reset() {
        this.queue.clear();
        this.joinCountdown = -1;
        this.repeatCountdown = -1;
        this.sendCountdown = 0;
    }
}
