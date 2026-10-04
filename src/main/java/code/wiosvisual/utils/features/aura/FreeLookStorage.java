/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3532
 */
package code.wiosvisual.utils.features.aura;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.events.keyboard.MouseRotationEvent;
import code.wiosvisual.events.render.CameraEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.features.aura.warp.Turns;
import net.minecraft.util.math.MathHelper;

public class FreeLookStorage
implements QuickImports {
    private static boolean active;
    private static float freeYaw;
    private static float freePitch;

    public FreeLookStorage() {
        Wiosvisual.getInstance().getEventManager();
        EventManager.register(this);
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        if (!value) {
            active = false;
            return;
        }
        FreeLookStorage.activateForSilentRotation();
    }

    public static void activateForSilentRotation() {
        if (mc == null || FreeLookStorage.mc.player == null) {
            return;
        }
        FreeLookStorage.captureCameraAngles();
        active = true;
    }

    private static void captureCameraAngles() {
        if (FreeLookStorage.mc.gameRenderer != null) {
            freeYaw = FreeLookStorage.mc.gameRenderer.getCamera().getYaw();
            freePitch = FreeLookStorage.mc.gameRenderer.getCamera().getPitch();
        } else {
            freeYaw = FreeLookStorage.mc.player.getYaw();
            freePitch = FreeLookStorage.mc.player.getPitch();
        }
    }

    @EventHandler
    public void onLook(MouseRotationEvent event) {
        if (active) {
            this.rotateTowards(event.getCursorDeltaX(), event.getCursorDeltaY());
            event.cancel();
        }
    }

    @EventHandler
    public void onRotation(CameraEvent event) {
        if (active) {
            event.setAngle(new Turns(freeYaw, freePitch));
            event.cancel();
        } else if (event.getAngle() != null) {
            freeYaw = event.getAngle().getYaw();
            freePitch = event.getAngle().getPitch();
        }
    }

    private void rotateTowards(double targetYaw, double targetPitch) {
        freePitch = MathHelper.clamp((float)((float)((double)freePitch + targetPitch * 0.15)), (float)-90.0f, (float)90.0f);
        freeYaw = (float)((double)freeYaw + targetYaw * 0.15);
    }

    public static void blendToward(float targetYaw, float targetPitch, float factor) {
        if (!active) {
            return;
        }
        float blend = MathHelper.clamp((float)factor, (float)0.0f, (float)1.0f);
        float yawDelta = MathHelper.wrapDegrees((float)(targetYaw - freeYaw));
        float pitchDelta = targetPitch - freePitch;
        freeYaw = MathHelper.wrapDegrees((float)(freeYaw + yawDelta * blend));
        freePitch = MathHelper.clamp((float)(freePitch + pitchDelta * blend), (float)-90.0f, (float)90.0f);
    }

    public static float getFreeYaw() {
        return freeYaw;
    }

    public static float getFreePitch() {
        return freePitch;
    }

    public static void setFreeYaw(float freeYaw) {
        FreeLookStorage.freeYaw = freeYaw;
    }

    public static void setFreePitch(float freePitch) {
        FreeLookStorage.freePitch = freePitch;
    }
}
