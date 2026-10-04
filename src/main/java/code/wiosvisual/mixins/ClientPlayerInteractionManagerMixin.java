/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1268
 *  net.minecraft.class_1269
 *  net.minecraft.class_1269$class_9860
 *  net.minecraft.class_1269$class_9861
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1713
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_636
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.events.block.BlockBreakingEvent;
import code.wiosvisual.events.block.BreakBlockEvent;
import code.wiosvisual.events.item.ClickSlotEvent;
import code.wiosvisual.events.item.UsingItemEvent;
import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.utils.client.FakePlayerManager;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.interactions.item.ItemToolkit;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
    @Inject(method={"attackEntity"}, at={@At(value="HEAD")}, cancellable=true)
    public void attackEntityHook(PlayerEntity player, Entity target, CallbackInfo info) {
        if (FakePlayerManager.handleAttack(target)) {
            player.swingHand(Hand.MAIN_HAND);
            info.cancel();
            return;
        }
        AttackEvent event = SafeEvents.dispatch(() -> new AttackEvent(target));
        if (event != null && event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method={"interactItem"}, at={@At(value="RETURN")})
    public void interactItemHook(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult.Success success;
        Object object = cir.getReturnValue();
        if (object instanceof ActionResult.Success && !(success = (ActionResult.Success)object).swingSource().equals((Object)ActionResult.SwingSource.CLIENT)) {
            SafeEvents.dispatch(() -> new UsingItemEvent((byte)0));
        }
    }

    @Inject(method={"stopUsingItem"}, at={@At(value="HEAD")})
    public void stopUsingItemHook(CallbackInfo ci) {
        SafeEvents.dispatch(() -> new UsingItemEvent((byte)2));
        if (ItemToolkit.INSTANCE.isUseItem()) {
            ItemToolkit.INSTANCE.setUseItem(false);
        }
    }

    @Inject(method={"interactItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void gameModeHook(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        UsingItemEvent event = SafeEvents.dispatch(() -> new UsingItemEvent((byte)-1, hand));
        if (event != null && event.isCancelled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method={"clickSlot"}, at={@At(value="HEAD")}, cancellable=true)
    public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
        ClickSlotEvent event = SafeEvents.dispatch(() -> new ClickSlotEvent(syncId, slotId, button, actionType));
        if (event != null && event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method={"updateBlockBreakingProgress"}, at={@At(value="HEAD")})
    private void injectBlockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        SafeEvents.dispatch(() -> new BlockBreakingEvent(pos, direction));
    }

    @Inject(method={"breakBlock"}, at={@At(value="RETURN")})
    private void injectBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        SafeEvents.dispatch(() -> new BreakBlockEvent(pos));
    }
}
