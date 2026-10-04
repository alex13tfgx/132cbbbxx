package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.features.module.setting.implement.SliderSettings;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.chat.ChatMessage;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

/**
 * Инвест в клан командой {@code /clan invest}: по бинду или автоматически при достижении порога.
 */
public class AutoInvest
extends Module {
    private final SelectSetting mode = new SelectSetting("Режим", "Как запускать инвест").value("Бинд", "Авто").selected("Бинд");
    private final BindSetting investKey = new BindSetting("Клавиша инвеста", "Клавиша, по которой инвестируется часть баланса").visible(() -> this.mode.isSelected("Бинд"));
    private final SliderSettings bindPercent = new SliderSettings("% от баланса (Бинд)", "Какую часть баланса вложить по бинду").range(1, 100).setInteger(true).setValue(50.0f).visible(() -> this.mode.isSelected("Бинд"));
    private final TextSetting triggerAmount = new TextSetting("Триггер монет", "Инвестировать, когда баланс не меньше этого значения").setText("10000000").visible(() -> this.mode.isSelected("Авто"));
    private final SliderSettings autoPercent = new SliderSettings("% от баланса (Авто)", "Какую часть баланса вкладывать автоматически").range(1, 100).setInteger(true).setValue(50.0f).visible(() -> this.mode.isSelected("Авто"));
    private int cooldownTicks;

    public AutoInvest() {
        super("AutoInvest", "Auto Invest", ModuleCategory.MISC);
        this.setup(this.mode, this.investKey, this.bindPercent, this.triggerAmount, this.autoPercent);
    }

    @Override
    public void deactivate() {
        this.cooldownTicks = 0;
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (AutoInvest.mc.player == null || !this.mode.isSelected("Бинд")) {
            return;
        }
        int key = this.investKey.getKey();
        if (key == -1 || !e.isKeyDown(key)) {
            return;
        }
        int balance = this.getBalance();
        if (balance <= 0) {
            this.info("§cНе удалось определить баланс!");
            return;
        }
        int amount = (int)((float)balance * (this.bindPercent.getValue() / 100.0f));
        if (amount <= 0) {
            this.info("§cСумма инвеста = 0!");
            return;
        }
        this.invest(amount);
        this.info("§aИнвестировано §f" + amount + " §aмонет (" + this.bindPercent.getInt() + "% от " + balance + ")");
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (AutoInvest.mc.player == null || AutoInvest.mc.world == null || !this.mode.isSelected("Авто")) {
            return;
        }
        if (this.cooldownTicks > 0) {
            --this.cooldownTicks;
            return;
        }
        int balance = this.getBalance();
        if (balance <= 0) {
            return;
        }
        int trigger;
        try {
            String text = this.triggerAmount.getText() == null ? "" : this.triggerAmount.getText().trim();
            trigger = text.isEmpty() ? 0 : Integer.parseInt(text);
        }
        catch (NumberFormatException ex) {
            return;
        }
        if (balance < trigger) {
            return;
        }
        int amount = (int)((float)balance * (this.autoPercent.getValue() / 100.0f));
        if (amount <= 0) {
            return;
        }
        this.invest(amount);
        this.info("§aАвто-инвест: §f" + amount + " §aмонет (" + this.autoPercent.getInt() + "% от " + balance + ")");
        this.cooldownTicks = 100;
    }

    private void invest(int amount) {
        AutoInvest.mc.player.networkHandler.sendChatCommand("clan invest " + amount);
    }

    private int getBalance() {
        if (AutoInvest.mc.world == null || AutoInvest.mc.player == null) {
            return -1;
        }
        Scoreboard scoreboard = AutoInvest.mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return -1;
        }
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            String line = Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()).getString();
            int balance = this.parseMoneyLine(line);
            if (balance > 0) {
                return balance;
            }
        }
        return -1;
    }

    private int parseMoneyLine(String line) {
        if (line == null) {
            return -1;
        }
        String text = line.replaceAll("§.", "").toLowerCase();
        if (!text.contains("монет") && !text.contains("баланс")) {
            return -1;
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits);
        }
        catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void info(String text) {
        ChatMessage.brandmessage(text);
    }
}
