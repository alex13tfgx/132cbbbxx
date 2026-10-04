/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 */
package code.wiosvisual.utils.display.render.shader;

import net.minecraft.util.Identifier;

public class SkyShaderUtil {
    public static Identifier getShader(String name) {
        return Identifier.of((String)"wiosvisual", (String)("core/" + name));
    }
}
