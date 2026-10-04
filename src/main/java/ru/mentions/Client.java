package ru.mentions;

import ru.mentions.utils.render.Render2D;

/** Точка доступа к общим сервисам отрисовки mentions внутри Wios. */
public final class Client {
    private static final Client INSTANCE = new Client();

    private Render2D render2D;

    private Client() {}

    public static Client get() {
        return INSTANCE;
    }

    public synchronized Render2D render2D() {
        if (this.render2D == null) {
            this.render2D = new Render2D();
        }
        return this.render2D;
    }
}
