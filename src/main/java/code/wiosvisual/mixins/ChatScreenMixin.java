/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_408
 *  net.minecraft.class_437
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.display.hud.CustomHudOverlay;
import code.wiosvisual.display.hud.HudEditorOverlay;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ChatScreen.class})
public class ChatScreenMixin
extends Screen
implements QuickImports {
    protected ChatScreenMixin() {
        super((Text)Text.empty());
    }

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void onRenderAboveChat(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CustomHudOverlay.render(context, mc.getRenderTickCounter(), true);
        Hud hud = Hud.getInstance();
        if (hud != null && hud.isState()) {
            HudEditorOverlay.getInstance().render(context, mouseX, mouseY, delta);
        }
    }

    // 1.21.11: mouseClicked(Click, boolean doubled)
    @Inject(method={"mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseClicked(net.minecraft.client.gui.Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorOverlay.getInstance().mouseClicked(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
        }
    }

    public boolean mouseReleased(net.minecraft.client.gui.Click wiosClick) {
        double mouseX = wiosClick.x();
        double mouseY = wiosClick.y();
        int button = wiosClick.button();
        return HudEditorOverlay.getInstance().mouseReleased(mouseX, mouseY, button) || super.mouseReleased(wiosClick);
    }

    @Inject(method={"mouseScrolled"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorOverlay.getInstance().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

}
