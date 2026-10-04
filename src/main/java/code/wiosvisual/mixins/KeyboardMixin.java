/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_309
 *  net.minecraft.class_310
 *  net.minecraft.class_3675$class_307
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.display.screens.clickgui.WiosGuiScreen;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Keyboard.class})
public class KeyboardMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    // 1.21.11: onKey(long window, int action, KeyInput input)
    @Inject(method={"onKey(JILnet/minecraft/client/input/KeyInput;)V"}, at={@At(value="HEAD")})
    private void onKey(long window, int action, net.minecraft.client.input.KeyInput input, CallbackInfo ci) {
        int key = input.key();
        if (key != -1 && window == this.client.getWindow().getHandle()) {
            if (key == 344 && action == 1 && this.client.currentScreen == null && this.client.player != null) {
                ru.mentions.screen.Menu.open();
            }
            SafeEvents.dispatch(() -> new KeyEvent(this.client.currentScreen, InputUtil.Type.KEYSYM, key, action));
        }
    }
}
