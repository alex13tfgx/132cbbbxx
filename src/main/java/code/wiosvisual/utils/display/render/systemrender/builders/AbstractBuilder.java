/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.display.render.systemrender.builders;

public abstract class AbstractBuilder<T> {
    public AbstractBuilder() {
        this.reset();
    }

    public final T build() {
        T instance = this._build();
        this.reset();
        return instance;
    }

    protected abstract void reset();

    protected abstract T _build();
}
