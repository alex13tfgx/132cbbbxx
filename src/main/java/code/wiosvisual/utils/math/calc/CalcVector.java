/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_243
 */
package code.wiosvisual.utils.math.calc;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class CalcVector {
    public static Vec3d lerpPosition(Entity entity) {
        float tickDelta = QuickImports.mc.getRenderTickCounter().getTickProgress(true);
        return new Vec3d(entity.lastX + (entity.getX() - entity.lastX) * (double)tickDelta, entity.lastY + (entity.getY() - entity.lastY) * (double)tickDelta, entity.lastZ + (entity.getZ() - entity.lastZ) * (double)tickDelta);
    }
}
