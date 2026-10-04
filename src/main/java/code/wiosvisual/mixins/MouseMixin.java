/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.v2.WrapWithCondition
 *  net.minecraft.class_310
 *  net.minecraft.class_312
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.keyboard.HotBarScrollEvent;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.keyboard.MouseRotationEvent;
import code.wiosvisual.events.render.FovEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Mouse.class})
public class MouseMixin {
    @Final
    @Shadow
    private MinecraftClient client;
    @Shadow
    public double cursorDeltaX;
    @Shadow
    public double cursorDeltaY;

    // 1.21.11: onMouseButton(long window, MouseInput input, int action)
    @Inject(method={"onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V"}, at={@At(value="HEAD")})
    public void onMouseButtonHook(long window, net.minecraft.client.input.MouseInput input, int action, CallbackInfo ci) {
        int button = input.button();
        if (button != -1 && window == this.client.getWindow().getHandle()) {
            SafeEvents.dispatch(() -> new KeyEvent(this.client.currentScreen, InputUtil.Type.MOUSE, button, action));
        }
    }

    @Inject(method={"onMouseScroll"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;")}, cancellable=true)
    public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        HotBarScrollEvent event = SafeEvents.dispatch(() -> new HotBarScrollEvent(horizontal, vertical));
        if (event != null && event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"updateMouse"}, at={@At(value="HEAD")})
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        FovEvent event = SafeEvents.dispatch(FovEvent::new);
        if (event != null && event.isCancelled()) {
            double slowdown = (double)event.getFov() / (double)((Integer)this.client.options.getFov().getValue()).intValue();
            this.cursorDeltaX *= slowdown;
            this.cursorDeltaY *= slowdown;
        }
    }

    @WrapWithCondition(method={"updateMouse"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")}, require=1, allow=1)
    private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
        MouseRotationEvent event = SafeEvents.dispatch(() -> new MouseRotationEvent((float)cursorDeltaX, (float)cursorDeltaY));
        if (event == null) {
            instance.changeLookDirection(cursorDeltaX, cursorDeltaY);
            return false;
        }
        if (event.isCancelled()) {
            return false;
        }
        instance.changeLookDirection((double)event.getCursorDeltaX(), (double)event.getCursorDeltaY());
        return false;
    }
}
