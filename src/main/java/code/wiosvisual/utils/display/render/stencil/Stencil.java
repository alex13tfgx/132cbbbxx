/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_276
 *  org.lwjgl.opengl.EXTFramebufferObject
 *  org.lwjgl.opengl.GL11
 */
package code.wiosvisual.utils.display.render.stencil;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

public final class Stencil
implements QuickImports {
    public static void push() {
        // TODO(port 1.21.11): stencil через renderbuffer (class_276.field_1474/method_1235) удалён из API.
        GL11.glStencilMask((int)255);
        GL11.glClear((int)1024);
        GL11.glEnable((int)2960);
        GL11.glStencilFunc((int)519, (int)1, (int)1);
        GL11.glStencilOp((int)7681, (int)7681, (int)7681);
        GL11.glDisable((int)2929);
        GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
    }

    public static void read(int ref) {
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL11.glStencilFunc((int)514, (int)ref, (int)1);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
    }

    public static void pop() {
        GL11.glDisable((int)2960);
        GL11.glEnable((int)2929);
    }

    private Stencil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
