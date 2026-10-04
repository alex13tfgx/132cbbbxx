/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1735
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_465
 *  net.minecraft.class_490
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.container.HandledScreenEvent;
import code.wiosvisual.mixins.IScreen;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={HandledScreen.class})
public abstract class HandledScreenMixin {
    @Shadow
    public int backgroundWidth;
    @Shadow
    public int backgroundHeight;
    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method={"render"}, at={@At(value="RETURN")})
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SafeEvents.dispatch(() -> new HandledScreenEvent(context, this.focusedSlot, this.backgroundWidth, this.backgroundHeight, mouseX, mouseY, delta));
    }

    @Inject(method={"mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseClicked(net.minecraft.client.gui.Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof InventoryScreen) {
            for (Element element : ((IScreen)((Object)this)).getChildren()) {
                // TODO(port 1.21.11): делегирование клика виджету требует class_11909(Click); спец-обработка ClearButtonWidget отключена.
                if (true) continue;
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }
}
