/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 */
package code.wiosvisual.features.impl.render.jumpCircle.data;

import code.wiosvisual.common.animation.Animation;
import net.minecraft.util.math.Vec3d;

public record JumpCircleLayer(Vec3d vec3d, long startTime, Animation animation) {
    public JumpCircleLayer(Vec3d vec3d, Animation animation) {
        this(vec3d, System.currentTimeMillis(), animation);
    }
}
