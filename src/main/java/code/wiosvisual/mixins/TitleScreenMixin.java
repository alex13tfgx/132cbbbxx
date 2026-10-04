/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_442
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.display.screens.mainmenu.MainMenu;
import code.wiosvisual.display.screens.mainmenu.NeverloseMenu;
import code.wiosvisual.utils.client.FirstLaunchManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MinecraftClient.class})
public class TitleScreenMixin {
    private static boolean WiosvisualRedirectScheduled = false;

    @Inject(method={"setScreen"}, at={@At(value="HEAD")}, cancellable=true)
    private void replaceTitleScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen) {
            MinecraftClient client = (MinecraftClient)(Object)this;
            FirstLaunchManager.applySavedPreferences();
            ci.cancel();
            if (client.currentScreen instanceof ru.mentions.screen.MainMenuScreen) {
                return;
            }
            if (WiosvisualRedirectScheduled) {
                return;
            }
            WiosvisualRedirectScheduled = true;
            client.execute(() -> {
                try {
                    client.setScreen((Screen)new ru.mentions.screen.MainMenuScreen());
                }
                finally {
                    WiosvisualRedirectScheduled = false;
                }
            });
        }
    }

}
