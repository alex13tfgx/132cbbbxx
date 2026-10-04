package ru.mentions.module.api.preview;

import ru.mentions.module.api.Module;
import ru.mentions.module.api.settings.impl.ButtonSetting;

/** Заглушка: настройки предпоказа в Wios не показываются и никогда не создаются. */
public final class PreviewSettings {
    private final float distance;
    private final float height;
    private final float interval;

    private PreviewSettings(float distance, float height, float interval) {
        this.distance = distance;
        this.height = height;
        this.interval = interval;
    }

    public static PreviewSettings of(Module parent, float distance, float height, float interval) {
        return new PreviewSettings(distance, height, interval);
    }

    public static PreviewSettings withoutInterval(Module parent, float distance, float height) {
        return new PreviewSettings(distance, height, 0.0f);
    }

    public static PreviewSettings none(Module parent) {
        return new PreviewSettings(0.0f, 0.0f, 0.0f);
    }

    public boolean repeats() {
        return false;
    }

    public long intervalMs() {
        return (long)(this.interval * 1000.0f);
    }

    public float distance(float fallback) {
        return this.distance;
    }

    public float height(float fallback) {
        return this.height;
    }

    public static ButtonSetting button(Module parent) {
        return null;
    }
}
