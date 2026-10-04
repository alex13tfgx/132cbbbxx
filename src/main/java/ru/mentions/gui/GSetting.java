package ru.mentions.gui;

import java.util.function.Supplier;
import ru.mentions.utils.animation.Animation;
import ru.mentions.utils.animation.satoshi.EaseInOutQuad;

/** Общая часть настроек в ClickGui mentions: имя, видимость и анимации отрисовки. */
public abstract class GSetting {
    private final String name;
    private final Supplier<Boolean> visible;
    private final Animation runAnimation = new Animation();
    private final ru.mentions.utils.animation.satoshi.Animation animation1 = new EaseInOutQuad(300, 1);
    public ru.mentions.utils.animation.satoshi.Animation animation = new EaseInOutQuad(300, 1);
    public ru.mentions.utils.animation.satoshi.Animation animation2 = new EaseInOutQuad(300, 1);

    protected GSetting(String name, Supplier<Boolean> visible) {
        this.name = name;
        this.visible = visible;
    }

    public String getName() {
        return this.name;
    }

    public Supplier<Boolean> getVisible() {
        return this.visible;
    }

    public Animation getAnimation() {
        return this.runAnimation;
    }

    public ru.mentions.utils.animation.satoshi.Animation getAnimation1() {
        return this.animation1;
    }
}
