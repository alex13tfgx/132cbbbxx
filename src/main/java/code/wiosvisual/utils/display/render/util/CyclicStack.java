/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.display.render.util;

import java.util.Stack;

public class CyclicStack<T>
extends Stack<T> {
    int index = 0;

    public T next() {
        this.index = (this.index + 1) % this.size();
        return (T)this.get(this.index);
    }
}
