/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_9779
 */
package code.wiosvisual.utils.client.timer;

import code.wiosvisual.utils.client.timer.IRenderTickCounterDynamic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public class TimerManager {
    private static float timerSpeed = 1.0f;

    public static void setTimer(float speed) {
        timerSpeed = Math.max(0.1f, Math.min(speed, 10.0f));
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        RenderTickCounter tickCounter = mc.getRenderTickCounter();
        if (tickCounter instanceof IRenderTickCounterDynamic) {
            ((IRenderTickCounterDynamic)tickCounter).setSpeedMultiplier(speed);
        }
    }

    public static void reset() {
        TimerManager.setTimer(1.0f);
    }

    public static float getTimerSpeed() {
        return timerSpeed;
    }
}
