/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.features.impl.misc.creeperFarm;

import code.wiosvisual.events.player.RotationUpdateEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.impl.misc.creeperFarm.CreeperFarmController;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.utils.client.managers.event.EventHandler;

public class CreeperFarmModule
extends Module {
    private final CreeperFarmController controller = new CreeperFarmController();

    public CreeperFarmModule() {
        super("Creeper Farm", "Creeper Farm", ModuleCategory.MISC);
    }

    @EventHandler
    public void tickEvent(TickEvent tickEvent) {
        this.controller.onTick();
    }

    @EventHandler
    public void rotationUpdateEvent(RotationUpdateEvent rotationUpdateEvent) {
        this.controller.onRotationUpdate();
    }

    public CreeperFarmController getController() {
        return this.controller;
    }
}
