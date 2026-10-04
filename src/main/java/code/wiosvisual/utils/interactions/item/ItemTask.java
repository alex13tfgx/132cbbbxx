/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1309
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1839
 */
package code.wiosvisual.utils.interactions.item;

import code.wiosvisual.display.hud.CoolDowns;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;

public final class ItemTask
implements QuickImports {
    public static int maxUseTick(Item item) {
        return ItemTask.maxUseTick(item.getDefaultStack());
    }

    public static int maxUseTick(ItemStack stack) {
        return switch (stack.getUseAction()) {
            case UseAction.EAT, UseAction.DRINK -> 32;
            case UseAction.CROSSBOW, UseAction.SPEAR -> 10;
            case UseAction.BOW -> 20;
            case UseAction.BLOCK -> 0;
            default -> stack.getMaxUseTime((LivingEntity)ItemTask.mc.player);
        };
    }

    public static float getCooldownProgress(Item item) {
        CoolDowns coolDowns = CoolDowns.getInstance();
        if (coolDowns != null) {
            return coolDowns.list.stream().filter(c -> c.item().equals(item)).findFirst().map(coolDown -> {
                if (ItemTask.mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
                    return Float.valueOf((float)((double)(-coolDown.time().elapsedTime()) / 1000.0));
                }
                return Float.valueOf(0.0f);
            }).orElse(Float.valueOf(0.0f)).floatValue();
        }
        return 0.0f;
    }

    private ItemTask() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
