/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 */
package code.wiosvisual.common.proxy;

import code.wiosvisual.common.proxy.GuiProxy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class ProxyScreens {
    private ProxyScreens() {
    }

    public static void open(Screen parent) {
        try {
            MinecraftClient.getInstance().setScreen((Screen)new GuiProxy(parent));
        }
        catch (Throwable error) {
            System.err.println("Failed to open proxy screen");
            error.printStackTrace();
        }
    }
}
