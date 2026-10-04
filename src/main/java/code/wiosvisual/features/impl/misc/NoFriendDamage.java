package code.wiosvisual.features.impl.misc;

import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.types.Priority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Не даёт ударить игрока из списка друзей: атака отменяется до отправки на сервер,
 * поэтому это работает и для обычных ударов, и для атак из других модулей.
 * Стрелы, зелья и взрывы летят не через атаку — от них клиент друга не защитит.
 */
public class NoFriendDamage
extends Module {
    public NoFriendDamage() {
        super("NoFriendDamage", "No Friend Damage", ModuleCategory.MISC);
    }

    /**
     * Самый высокий приоритет: визуальные модули (звук удара, хитмаркер, частицы)
     * проверяют отмену и не должны реагировать на удар, которого не было.
     */
    @EventHandler(value=Priority.HIGHEST)
    public void onAttack(AttackEvent e) {
        Entity target = e.getEntity();
        if (target instanceof PlayerEntity && target != mc.player && FriendUtils.isFriend(target)) {
            e.cancel();
        }
    }
}
