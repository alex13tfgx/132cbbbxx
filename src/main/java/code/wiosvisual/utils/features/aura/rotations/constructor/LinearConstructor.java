/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 */
package code.wiosvisual.utils.features.aura.rotations.constructor;

import code.wiosvisual.utils.features.aura.rotations.constructor.RotateConstructor;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LinearConstructor
extends RotateConstructor {
    public LinearConstructor() {
        super("Linear");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawStep = MathHelper.clamp((float)delta.getYaw(), (float)-180.0f, (float)180.0f);
        float pitchStep = MathHelper.clamp((float)delta.getPitch(), (float)-90.0f, (float)90.0f);
        return new Turns(currentAngle.getYaw() + yawStep, currentAngle.getPitch() + pitchStep);
    }
}
