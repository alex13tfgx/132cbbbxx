package ru.mentions.module.api.settings.impl;

import code.wiosvisual.features.module.setting.Setting;
import org.joml.Vector2f;
import ru.mentions.module.api.Module;

/** Позиция перетаскиваемого элемента интерфейса. В ClickGui Wios не отображается. */
public class DragSetting extends Setting {
    public final Vector2f targetPosition = new Vector2f();
    public final Vector2f position = new Vector2f();
    public final Vector2f size = new Vector2f();
    public boolean active = true;
    public boolean structure;
    public boolean lockX;

    public DragSetting(Module parent, String name, Vector2f value) {
        super(name, "");
        this.targetPosition.set(value);
        this.position.set(value);
        this.setVisible(() -> false);
        parent.setup(this);
    }

    public Vector2f getValue() {
        return this.targetPosition;
    }
}
