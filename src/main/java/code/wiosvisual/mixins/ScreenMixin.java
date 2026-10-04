/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_2558
 *  net.minecraft.class_2583
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 *  net.minecraft.class_500
 *  net.minecraft.class_526
 *  net.minecraft.class_751
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.display.screens.EmotionWheelScreen;
import code.wiosvisual.display.screens.clickgui.WiosGuiScreen;
import code.wiosvisual.events.chat.ChatEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.render.StartupBackgroundRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Screen.class})
public class ScreenMixin {
    private static final CubeMapRenderer CUSTOM_PANORAMA_RENDERER = new CubeMapRenderer(Identifier.of((String)"minecraft", (String)"panorama/panorama"));

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void wiosvisual$restoreGuiRenderState(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void wiosvisual$restoreGuiRenderStateTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    }

    @Inject(at={@At(value="INVOKE", target="Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", ordinal=1)}, method={"handleTextClick"}, cancellable=true, require=0)
    public void handleCustomClickEvent(Style style, CallbackInfoReturnable<Boolean> cir) {
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return;
        }
        SafeEvents.dispatch(() -> new ChatEvent(String.valueOf(clickEvent)));
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Inject(method={"renderBackground"}, at={@At(value="HEAD")}, cancellable=true)
    private void disableBackgroundBlurAndDimming(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object)this instanceof WiosGuiScreen || (Object)this instanceof EmotionWheelScreen) {
            ci.cancel();
            return;
        }
        if (MinecraftClient.getInstance().world != null) {
            ci.cancel();
            return;
        }
        if ((Object)this instanceof MultiplayerScreen || (Object)this instanceof SelectWorldScreen) {
            // Списки серверов и миров получают те же обои, что и остальной клиент, вместо сетки из ваниллы.
            StartupBackgroundRenderer.drawBackground(context, 0.0f, 0.0f, (float)context.getScaledWindowWidth(), (float)context.getScaledWindowHeight(), 1.0f);
            ci.cancel();
        }
    }
}
