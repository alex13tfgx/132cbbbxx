/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_9779$class_9781
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 */
package code.wiosvisual.mixins;

import code.wiosvisual.utils.client.timer.IRenderTickCounterDynamic;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={RenderTickCounter.Dynamic.class})
public class RenderTickCounterDynamicMixin
implements IRenderTickCounterDynamic {
    @Shadow
    @Final
    @Mutable
    private float tickTime;
    // без инициализатора: в обфусцированной сборке он не применяется, поэтому 0 трактуем как 1.0
    @Unique
    private float speedMultiplier;

    @Override
    public void setSpeedMultiplier(float speed) {
        this.speedMultiplier = Math.max(0.1f, Math.min(speed, 10.0f));
        this.tickTime = 50.0f / this.speedMultiplier;
    }

    @Override
    public float getSpeedMultiplier() {
        return this.speedMultiplier <= 0.0f ? 1.0f : this.speedMultiplier;
    }
}
