/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_638$class_5271
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.Ambience;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientWorld.Properties.class})
public class ClientWorldPropertiesMixin
implements QuickImports {
    @Shadow
    private long timeOfDay;

    @Inject(method={"setTimeOfDay"}, at={@At(value="HEAD")}, cancellable=true)
    public void setTimeOfDayHook(long timeOfDay, CallbackInfo ci) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.state) {
            this.timeOfDay = ambience.getCustomTime();
            ci.cancel();
            return;
        }
    }
}
