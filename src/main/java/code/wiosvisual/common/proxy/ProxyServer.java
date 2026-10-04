/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ModInitializer
 *  net.minecraft.class_4185
 */
package code.wiosvisual.common.proxy;

import code.wiosvisual.common.proxy.Config;
import code.wiosvisual.common.proxy.Proxy;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.widget.ButtonWidget;

public class ProxyServer
implements ModInitializer {
    public static boolean proxyEnabled = false;
    public static Proxy proxy = new Proxy();
    public static Proxy lastUsedProxy = new Proxy();
    public static ButtonWidget proxyMenuButton;

    public static String getLastUsedProxyIp() {
        return ProxyServer.lastUsedProxy.ipPort.isEmpty() ? "none" : lastUsedProxy.getIp();
    }

    public void onInitialize() {
        Config.loadConfig();
    }
}
