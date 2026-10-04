/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_437
 */
package code.wiosvisual.events.container;

import code.wiosvisual.utils.client.managers.event.events.Event;
import net.minecraft.client.gui.screen.Screen;

public class SetScreenEvent
implements Event {
    public Screen screen;

    public Screen getScreen() {
        return this.screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public SetScreenEvent(Screen screen) {
        this.screen = screen;
    }
}
