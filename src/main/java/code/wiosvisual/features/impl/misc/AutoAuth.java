package code.wiosvisual.features.impl.misc;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.TextSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.packet.network.Network;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

/**
 * Автоматический ввод пароля на серверах с авторизацией: ловит системное
 * сообщение с просьбой войти или зарегистрироваться и отправляет команду.
 * Порт AutoAuth из DELTA.
 */
public class AutoAuth
extends Module {
    private final TextSetting loginPassword = new TextSetting("Пароль авторизации", "Пароль для /login");
    private final TextSetting registerPassword = new TextSetting("Пароль регистрации", "Пароль для /reg");

    /** Команда, ожидающая отправки в следующем тике (без ведущего слэша). */
    private String pendingCommand;

    public static AutoAuth getInstance() {
        return Instance.get(AutoAuth.class);
    }

    public AutoAuth() {
        super("AutoAuth", "Auto Auth", ModuleCategory.MISC);
        this.setup(this.loginPassword, this.registerPassword);
    }

    @Override
    public void deactivate() {
        this.pendingCommand = null;
        super.deactivate();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (!(packet instanceof GameMessageS2CPacket)) {
            return;
        }
        String message = ((GameMessageS2CPacket)packet).content().getString();
        if (message == null) {
            return;
        }
        if (AutoAuth.mentions(message, "Зарегистрируйтесь", "/reg", "/register") && !this.registerPassword.getText().isEmpty()) {
            this.pendingCommand = "reg " + this.registerPassword.getText();
            return;
        }
        if (AutoAuth.mentions(message, "Авторизуйтесь", "Войдите в игру", "/login") && !this.loginPassword.getText().isEmpty()) {
            this.pendingCommand = "login " + this.loginPassword.getText();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (this.pendingCommand == null || AutoAuth.mc.player == null) {
            return;
        }
        // Во время боя чат не трогаем — как и в оригинале.
        if (Network.isPvp()) {
            return;
        }
        AutoAuth.mc.player.networkHandler.sendChatCommand(this.pendingCommand);
        this.pendingCommand = null;
    }

    private static boolean mentions(String message, String ... needles) {
        for (String needle : needles) {
            if (!message.contains(needle)) continue;
            return true;
        }
        return false;
    }
}
