/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2379
 *  net.minecraft.class_2940
 *  net.minecraft.class_2941
 *  net.minecraft.class_2943
 *  net.minecraft.class_2945
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.util.math.EulerAngle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DataTracker.class})
public abstract class DataTrackerMixin {
    @Inject(method={"set(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private <T> void onSet(TrackedData<T> data, T value, CallbackInfo ci) {
        TrackedDataHandler serializer = data.dataType();
        if (serializer == TrackedDataHandlerRegistry.BYTE && !(value instanceof Byte)) {
            ci.cancel();
        }
        if (serializer == TrackedDataHandlerRegistry.ROTATION && !(value instanceof EulerAngle)) {
            ci.cancel();
        }
    }
}
