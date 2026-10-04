/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  net.minecraft.class_10185
 *  net.minecraft.class_3532
 *  net.minecraft.class_743
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.player.InputEvent;
import code.wiosvisual.mixins.InputMixin;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.features.aura.warp.Turns;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.features.aura.warp.TurnsConstructor;
import code.wiosvisual.utils.interactions.inv.InventoryFlowManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={KeyboardInput.class})
public class KeyboardInputMixin
extends InputMixin {
    @ModifyExpressionValue(method={"tick"}, at={@At(value="NEW", target="(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;")})
    private PlayerInput tickHook(PlayerInput original) {
        InputEvent event = SafeEvents.dispatch(() -> new InputEvent(original));
        if (event != null) {
            InventoryFlowManager.input(event);
        }
        return this.transformInput(event != null ? event.getInput() : original);
    }

    @Unique
    private PlayerInput transformInput(PlayerInput input) {
        TurnsConnection rotationController = TurnsConnection.INSTANCE;
        Turns angle = rotationController.getCurrentAngle();
        TurnsConstructor configurable = rotationController.getCurrentRotationPlan();
        if (KeyboardInputMixin.mc.player == null || angle == null || configurable == null || !configurable.isMoveCorrection() || !configurable.isFreeCorrection()) {
            return input;
        }
        float deltaYaw = KeyboardInputMixin.mc.player.getYaw() - angle.getYaw();
        float z = KeyboardInput.getMovementMultiplier((boolean)input.forward(), (boolean)input.backward());
        float x = KeyboardInput.getMovementMultiplier((boolean)input.left(), (boolean)input.right());
        float newX = x * MathHelper.cos((float)(deltaYaw * ((float)Math.PI / 180))) - z * MathHelper.sin((float)(deltaYaw * ((float)Math.PI / 180)));
        float newZ = z * MathHelper.cos((float)(deltaYaw * ((float)Math.PI / 180))) + x * MathHelper.sin((float)(deltaYaw * ((float)Math.PI / 180)));
        int movementSideways = Math.round(newX);
        int movementForward = Math.round(newZ);
        return new PlayerInput((float)movementForward > 0.0f, (float)movementForward < 0.0f, (float)movementSideways > 0.0f, (float)movementSideways < 0.0f, input.jump(), input.sneak(), input.sprint());
    }
}
