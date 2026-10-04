/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_638
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package code.wiosvisual.mixins;

import code.wiosvisual.features.impl.render.Ambience;
import code.wiosvisual.events.player.EntitySpawnEvent;
import code.wiosvisual.events.render.WorldLoadEvent;
import code.wiosvisual.utils.client.managers.event.SafeEvents;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientWorld.class})
public class ClientWorldMixin
implements QuickImports {
    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    public void initHook(CallbackInfo info) {
        SafeEvents.dispatch(() -> new WorldLoadEvent());
    }

    @Inject(method={"addEntity"}, at={@At(value="HEAD")}, cancellable=true)
    public void addEntityHook(Entity entity, CallbackInfo ci) {
        if (PlayerInteractionHelper.nullCheck()) {
            return;
        }
        EntitySpawnEvent event = SafeEvents.dispatch(() -> new EntitySpawnEvent(entity));
        if (event != null && event.isCancelled()) {
            ci.cancel();
        }
    }

    @Shadow
    @Final
    private ClientWorld.Properties clientWorldProperties;

    /**
     * Ambience drives the world clock itself: vanilla time ticking is cancelled and the animated
     * value from the module is written into the client world properties instead.
     */
    @Inject(method={"tickTime"}, at={@At(value="HEAD")}, cancellable=true)
    private void wiosvisual$ambienceTickTime(CallbackInfo ci) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.state) {
            this.clientWorldProperties.setTimeOfDay(ambience.getCustomTime());
            ci.cancel();
        }
    }
}
