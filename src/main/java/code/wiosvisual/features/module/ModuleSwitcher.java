/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  org.lwjgl.glfw.GLFW
 */
package code.wiosvisual.features.module;

import code.wiosvisual.common.logger.implement.ConsoleLogger;
import code.wiosvisual.display.screens.clickgui.WiosGuiScreen;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.exception.ModuleException;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import java.util.List;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class ModuleSwitcher
implements QuickLogger,
QuickImports {
    private final List<Module> modules;

    public ModuleSwitcher(List<Module> modules, EventManager eventManager) {
        this.modules = modules;
        EventManager.register(this);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.key() == 344 && event.action() == 1 && ModuleSwitcher.mc.currentScreen == null && ModuleSwitcher.mc.player != null) {
            this.openClickGui();
            return;
        }
        for (Module module : this.modules) {
            boolean isCombo;
            if (module.getName().equals("ClickGui")) continue;
            int keyCode = module.getKey();
            int secondKey = module.getSecondKey();
            boolean bl = isCombo = secondKey != -1 && secondKey != -1;
            if (isCombo) {
                if (event.key() != secondKey || ModuleSwitcher.mc.currentScreen != null || !this.isKeyPressed(keyCode)) continue;
                try {
                    if (module.getName().equals("ClickGui") && event.action() == 0) {
                        this.openClickGui();
                        return;
                    }
                    this.handleModuleState(module, event.action());
                }
                catch (Exception e) {
                    this.handleException(module.getName(), e);
                }
                continue;
            }
            if (event.key() != keyCode || ModuleSwitcher.mc.currentScreen != null) continue;
            try {
                if (module.getName().equals("ClickGui") && event.action() == 0) {
                    this.openClickGui();
                    return;
                }
                this.handleModuleState(module, event.action());
            }
            catch (Exception e) {
                this.handleException(module.getName(), e);
            }
        }
    }

    private void openClickGui() {
        ru.mentions.screen.Menu.open();
    }

    private boolean isKeyPressed(int keyCode) {
        if (keyCode == -1 || keyCode == -1) {
            return false;
        }
        return GLFW.glfwGetKey((long)mc.getWindow().getHandle(), (int)keyCode) == 1;
    }

    public void handleModuleState(Module module, int action) {
        if (module.getName().equals("ClickGui")) {
            return;
        }
        if (module.getType() == 1 && action == 1) {
            module.switchState();
        }
    }

    public void handleException(String moduleName, Exception e) {
        ConsoleLogger consoleLogger = new ConsoleLogger();
        if (e instanceof ModuleException) {
            this.logDirect("[" + moduleName + "] " + String.valueOf(Formatting.RED) + e.getMessage());
        } else {
            consoleLogger.log("Error in module " + moduleName + ": " + e.getMessage());
        }
    }
}
