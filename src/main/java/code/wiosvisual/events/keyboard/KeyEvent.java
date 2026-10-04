/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 */
package code.wiosvisual.events.keyboard;

import code.wiosvisual.utils.client.bind.BindCodes;
import code.wiosvisual.utils.client.managers.event.events.Event;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;

public record KeyEvent(Screen screen, InputUtil.Type type, int key, int action) implements Event,
QuickImports
{
    public boolean isKeyDown(int key) {
        return this.matchesBind(key, KeyEvent.mc.currentScreen == null);
    }

    public boolean isKeyDown(int key, boolean screen) {
        return this.matchesBind(key, screen);
    }

    public boolean matchesBind(int bindKey, boolean allowScreen) {
        if (BindCodes.isScroll(bindKey)) {
            // Прокрутку в KeyEvent присылает BindScrollBridge — только вне экранов.
            return this.action == 1 && allowScreen && this.key == bindKey;
        }
        if (this.action != 1 || !allowScreen) {
            return false;
        }
        if (BindCodes.isMouse(bindKey)) {
            return this.type == InputUtil.Type.MOUSE && this.key == bindKey;
        }
        return this.type == InputUtil.Type.KEYSYM && this.key == bindKey;
    }

    public boolean isKeyReleased(int key) {
        return this.isKeyReleased(key, KeyEvent.mc.currentScreen == null);
    }

    public boolean isKeyReleased(int key, boolean screen) {
        return this.key == key && this.action == 0 && screen;
    }
}
