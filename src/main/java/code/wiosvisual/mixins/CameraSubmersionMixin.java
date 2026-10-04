/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4184
 *  net.minecraft.class_5636
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.NoRender;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Camera.class})
public class CameraSubmersionMixin {
    @Inject(method={"getSubmersionType"}, at={@At(value="RETURN")}, cancellable=true)
    private void onGetSubmersionType(CallbackInfoReturnable<CameraSubmersionType> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender == null || !noRender.isState()) {
            return;
        }
        CameraSubmersionType type = (CameraSubmersionType)cir.getReturnValue();
        if (type == null) {
            return;
        }
        if (type == CameraSubmersionType.WATER && noRender.shouldHideWaterOverlay()) {
            cir.setReturnValue(CameraSubmersionType.NONE);
            return;
        }
        if (type == CameraSubmersionType.LAVA && noRender.shouldHideLavaOverlay()) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
