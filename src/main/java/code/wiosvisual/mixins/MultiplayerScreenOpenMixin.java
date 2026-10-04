/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_364
 *  net.minecraft.class_4068
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_500
 *  net.minecraft.class_6379
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.common.proxy.Config;
import code.wiosvisual.common.proxy.Proxy;
import code.wiosvisual.common.proxy.ProxyScreens;
import code.wiosvisual.common.proxy.ProxyServer;
import code.wiosvisual.mixins.IScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MultiplayerScreen.class})
public class MultiplayerScreenOpenMixin {
    @Inject(method={"init()V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;updateButtonActivationStates()V")})
    public void multiplayerGuiOpen(CallbackInfo ci) {
        try {
            String playerName = MinecraftClient.getInstance().getSession().getUsername();
            if (!playerName.equals(Config.lastPlayerName)) {
                Config.lastPlayerName = playerName;
                ProxyServer.proxy = Config.accounts.containsKey(playerName) ? Config.accounts.get(playerName) : (Config.accounts.containsKey("") ? Config.accounts.get("") : new Proxy());
            }
            MultiplayerScreen ms = (MultiplayerScreen)(Object)this;
            ProxyServer.proxyMenuButton = ButtonWidget.builder((Text)Text.literal((String)"Proxy"), buttonWidget -> ProxyScreens.open((Screen)ms)).dimensions(5, 5, 120, 20).build();
            IScreen si = (IScreen)ms;
            si.getDrawables().add((Drawable)ProxyServer.proxyMenuButton);
            si.getSelectables().add((Selectable)ProxyServer.proxyMenuButton);
            si.getChildren().add((Element)ProxyServer.proxyMenuButton);
        }
        catch (Throwable error) {
            System.err.println("Failed to initialize proxy menu on multiplayer screen");
            error.printStackTrace();
        }
    }
}
