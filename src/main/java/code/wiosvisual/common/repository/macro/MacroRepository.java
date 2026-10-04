/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.repository.macro;

import code.wiosvisual.common.repository.macro.Macro;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import java.util.ArrayList;
import java.util.List;

public class MacroRepository
implements QuickImports,
QuickLogger {
    public List<Macro> macroList = new ArrayList<Macro>();

    public MacroRepository(EventManager eventManager) {
        EventManager.register(this);
    }

    public void addMacro(String name, String message, int key) {
        this.macroList.add(new Macro(name, message, key));
    }

    public boolean hasMacro(String text) {
        return this.macroList.stream().anyMatch(macro -> macro.name().equalsIgnoreCase(text));
    }

    public void deleteMacro(String text) {
        this.macroList.removeIf(macro -> macro.name().equalsIgnoreCase(text));
    }

    public void clearList() {
        this.macroList.clear();
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (MacroRepository.mc.player != null && e.action() == 0 && MacroRepository.mc.currentScreen == null) {
            this.macroList.stream().filter(macro -> macro.key() == e.key()).findFirst().ifPresent(macro -> MacroRepository.send(macro.message()));
        }
    }

    /**
     * sendChatMessage шлёт текст как обычное сообщение — «/near» так уходил в чат строкой.
     * Команды в 1.21 отправляются отдельным пакетом через sendChatCommand, без ведущего слеша.
     */
    private static void send(String message) {
        if (message == null) {
            return;
        }
        String text = message.trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.startsWith("/")) {
            String command = text.substring(1).trim();
            if (!command.isEmpty()) {
                MacroRepository.mc.player.networkHandler.sendChatCommand(command);
            }
            return;
        }
        MacroRepository.mc.player.networkHandler.sendChatMessage(text);
    }
}
