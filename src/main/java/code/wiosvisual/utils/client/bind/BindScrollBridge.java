/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3675$class_307
 */
package code.wiosvisual.utils.client.bind;

import code.wiosvisual.events.keyboard.HotBarScrollEvent;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.util.InputUtil;

public class BindScrollBridge
implements QuickImports {
    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent event) {
        if (BindScrollBridge.mc.currentScreen != null || event.getVertical() == 0.0) {
            return;
        }
        int code = event.getVertical() > 0.0 ? 998 : 999;
        SafeEvents.dispatch(() -> new KeyEvent(null, InputUtil.Type.KEYSYM, code, 1));
    }
}
