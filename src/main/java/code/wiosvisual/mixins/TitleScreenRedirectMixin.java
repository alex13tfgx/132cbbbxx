package code.wiosvisual.mixins;

import code.wiosvisual.display.screens.mainmenu.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safety net for {@link TitleScreenMixin}: that one only catches {@code setScreen(TitleScreen)}, so any
 * path that builds the vanilla title screen another way (leaving a world, for one) slipped through.
 * This fires while the vanilla screen lays out its buttons and swaps in the custom menu instead.
 */
@Mixin(value={TitleScreen.class})
public class TitleScreenRedirectMixin {
    @Inject(method={"addNormalWidgets"}, at={@At(value="HEAD")})
    private void wiosvisual$redirectToMainMenu(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen instanceof ru.mentions.screen.MainMenuScreen) {
            return;
        }
        client.execute(() -> {
            if (!(client.currentScreen instanceof ru.mentions.screen.MainMenuScreen)) {
                client.setScreen((Screen)new ru.mentions.screen.MainMenuScreen());
            }
        });
    }
}
