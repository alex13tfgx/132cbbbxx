/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_243
 */
package code.wiosvisual.utils.features.aura.warp;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.rotations.constructor.RotateConstructor;
import code.wiosvisual.utils.features.aura.utils.MathAngle;
import code.wiosvisual.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class TurnsConstructor
implements QuickImports {
    private final Turns angle;
    private final Vec3d vec3d;
    private final Entity entity;
    private final RotateConstructor angleSmooth;
    private final int ticksUntilReset;
    private final float resetThreshold;
    public final boolean moveCorrection;
    public final boolean freeCorrection;
    public final boolean changeLook;

    public Turns nextRotation(Turns fromAngle, boolean isResetting) {
        if (isResetting) {
            return this.angleSmooth.limitAngleChange(fromAngle, MathAngle.fromVec2f(TurnsConstructor.mc.player.getRotationClient()));
        }
        return this.angleSmooth.limitAngleChange(fromAngle, this.angle, this.vec3d, this.entity);
    }

    public Turns getAngle() {
        return this.angle;
    }

    public Vec3d getVec3d() {
        return this.vec3d;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public RotateConstructor getAngleSmooth() {
        return this.angleSmooth;
    }

    public int getTicksUntilReset() {
        return this.ticksUntilReset;
    }

    public float getResetThreshold() {
        return this.resetThreshold;
    }

    public boolean isMoveCorrection() {
        return this.moveCorrection;
    }

    public boolean isChangeLook() {
        return this.changeLook;
    }

    public TurnsConstructor(Turns angle, Vec3d vec3d, Entity entity, RotateConstructor angleSmooth, int ticksUntilReset, float resetThreshold, boolean moveCorrection, boolean freeCorrection) {
        this.changeLook = false;
        this.angle = angle;
        this.vec3d = vec3d;
        this.entity = entity;
        this.angleSmooth = angleSmooth;
        this.ticksUntilReset = ticksUntilReset;
        this.resetThreshold = resetThreshold;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = freeCorrection;
    }

    public boolean isFreeCorrection() {
        return this.freeCorrection;
    }
}
