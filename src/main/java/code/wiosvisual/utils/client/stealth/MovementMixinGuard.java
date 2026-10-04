/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.stealth;

import code.wiosvisual.features.module.Module;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.features.aura.warp.TurnsConnection;
import code.wiosvisual.utils.features.aura.warp.TurnsConstructor;

public final class MovementMixinGuard {
    public static boolean shouldUseEntityMovementPhysics() {
        return false;
    }

    public static boolean shouldHandleVelocityStrafe() {
        TurnsConstructor rotationPlan = TurnsConnection.INSTANCE.getCurrentRotationPlan();
        return rotationPlan != null && rotationPlan.isMoveCorrection();
    }

    public static boolean isHitBoxActive() {
        return false;
    }

    public static boolean shouldHandleMoveEvent() {
        return false;
    }

    public static boolean shouldHandleMotionEvent() {
        return false;
    }

    public static boolean shouldHandlePostMotionEvent() {
        return false;
    }

    public static boolean shouldHandleNoSlowRedirect() {
        return false;
    }

    private static boolean isActive(Class<? extends Module> clazz) {
        Module module = Instance.get(clazz);
        return module != null && module.isState();
    }

    private MovementMixinGuard() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
