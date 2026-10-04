package code.wiosvisual.utils.interactions.inv;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * Поиск предмета в инвентаре игрока. Хотбар — индексы 0–8, основной инвентарь — 9–35.
 * У окна инвентаря своя нумерация слотов: хотбар там идёт номерами 36–44, поэтому
 * для кликов по окну индекс хотбара нужно пересчитать.
 */
public final class InventorySearch implements QuickImports {
    private InventorySearch() {
    }

    /**
     * @param includeMain искать и в основном инвентаре, а не только в хотбаре
     * @param rawIndex    вернуть индекс инвентаря как есть (для выбора слота хотбара);
     *                    иначе — номер слота в окне инвентаря (для кликов по окну)
     */
    public static Optional<Integer> find(Predicate<ItemStack> predicate, boolean includeMain, boolean rawIndex) {
        if (mc.player == null) {
            return Optional.empty();
        }
        PlayerInventory inventory = mc.player.getInventory();
        int end = includeMain ? 36 : 9;
        for (int i = 0; i < end; ++i) {
            if (!predicate.test(inventory.getStack(i))) {
                continue;
            }
            if (!includeMain || rawIndex) {
                return Optional.of(i);
            }
            return Optional.of(i < 9 ? i + 36 : i);
        }
        return Optional.empty();
    }
}
