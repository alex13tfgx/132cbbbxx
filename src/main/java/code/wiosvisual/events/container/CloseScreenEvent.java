/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_437
 */
package code.wiosvisual.events.container;

import code.wiosvisual.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.client.gui.screen.Screen;

public class CloseScreenEvent
extends EventCancellable {
    private Screen screen;

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CloseScreenEvent)) {
            return false;
        }
        CloseScreenEvent other = (CloseScreenEvent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Screen this$screen = this.getScreen();
        Screen other$screen = other.getScreen();
        return !(this$screen == null ? other$screen != null : !this$screen.equals(other$screen));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CloseScreenEvent;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Screen $screen = this.getScreen();
        result = result * 59 + ($screen == null ? 43 : $screen.hashCode());
        return result;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public String toString() {
        return "CloseScreenEvent(screen=" + String.valueOf(this.getScreen()) + ")";
    }

    public CloseScreenEvent(Screen screen) {
        this.screen = screen;
    }
}
