package ru.mentions.gui;

import java.util.function.Supplier;
import ru.mentions.utils.animation.Animation;

public class ButtonSetting extends GSetting {
    public final Animation pressAnim = new Animation();
    private final Runnable action;

    public ButtonSetting(String name, Runnable action, Supplier<Boolean> visible) {
        super(name, visible);
        this.action = action;
    }

    public void press() {
        this.pressAnim.set(1);
        this.pressAnim.run(0, 0.3, ru.mentions.utils.animation.Easings.SINE_OUT);
        if (this.action != null) {
            this.action.run();
        }
    }
}
