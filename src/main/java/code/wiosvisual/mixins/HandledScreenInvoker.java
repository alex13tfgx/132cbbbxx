package code.wiosvisual.mixins;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Даёт доступ к HandledScreen#onMouseClick. Прямой вызов ClientPlayerInteractionManager#clickSlot
 * не работает в креативе (CreativeInventoryScreen обрабатывает слоты сам) и ломается в локальном
 * мире из-за syncId; ванильный onMouseClick корректен во всех режимах.
 */
@Mixin(value={HandledScreen.class})
public interface HandledScreenInvoker {
    @Invoker(value="onMouseClick")
    public void wiosvisual$onMouseClick(Slot var1, int var2, int var3, SlotActionType var4);
}
