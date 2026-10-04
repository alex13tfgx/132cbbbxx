/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.draggable;

import code.wiosvisual.display.hud.ArmorBar;
import code.wiosvisual.display.hud.CoolDowns;
import code.wiosvisual.display.hud.CremKeys;
import code.wiosvisual.display.hud.CustomHotbar;
import code.wiosvisual.display.hud.DynamicIsland;
import code.wiosvisual.display.hud.InventoryHud;
import code.wiosvisual.display.hud.Keybinds;
import code.wiosvisual.display.hud.MusicInfo;
import code.wiosvisual.display.hud.Notifications;
import code.wiosvisual.display.hud.OnlineList;
import code.wiosvisual.display.hud.Potions;
import code.wiosvisual.display.hud.TargetHud;
import code.wiosvisual.display.hud.TotemBar;
import code.wiosvisual.utils.client.managers.api.draggable.AbstractDraggable;
import java.util.ArrayList;
import java.util.List;

public class DraggableRepository {
    private final List<AbstractDraggable> draggable = new ArrayList<AbstractDraggable>();

    public void setup() {
        this.register(new DynamicIsland(), new TargetHud(), Notifications.getInstance(), new CoolDowns(), new MusicInfo(), new Keybinds(), new Potions(), new ArmorBar(), new code.wiosvisual.display.hud.TargetArmor(), new TotemBar(), new CustomHotbar(), InventoryHud.getInstance(), new OnlineList(), new CremKeys());
    }

    public void register(AbstractDraggable ... module) {
        this.draggable.addAll(List.of(module));
    }

    public List<AbstractDraggable> draggable() {
        return this.draggable;
    }

    public <T extends AbstractDraggable> T get(String name) {
        return (T)((AbstractDraggable)this.draggable.stream().filter(module -> module.getName().equalsIgnoreCase(name)).map(module -> module).findFirst().orElse(null));
    }

    public <T extends AbstractDraggable> T get(Class<T> clazz) {
        return (T)((AbstractDraggable)this.draggable.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null));
    }
}
