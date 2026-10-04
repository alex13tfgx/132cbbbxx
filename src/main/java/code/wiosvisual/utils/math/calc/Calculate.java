/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1297
 *  net.minecraft.class_243
 *  net.minecraft.class_3532
 *  net.minecraft.class_4587
 *  net.minecraft.class_9848
 *  org.joml.Vector3d
 */
package code.wiosvisual.utils.math.calc;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public final class Calculate
implements QuickImports {
    public static double PI2 = Math.PI * 2;

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static float clamp(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }

    public static double computeGcd() {
        return Math.pow((Double)Calculate.mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickProgress(true);
    }

    public static int getRandom(int min, int max) {
        return (int)Calculate.getRandom((float)min, (float)max + 1.0f);
    }

    public static float getRandom(float min, float max) {
        return (float)Calculate.getRandom((double)min, (double)max);
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            double d = min;
            min = max;
            max = d;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static void scale(MatrixStack stack, float x, float y, float scale, Runnable data) {
        if (scale != 1.0f) {
            float scale2 = 0.5f + scale / 2.0f;
            stack.push();
            stack.translate(x, y, 0.0f);
            stack.scale(scale2, scale2, 1.0f);
            stack.translate(-x, -y, 0.0f);
            Calculate.setAlpha(scale, data);
            stack.pop();
        } else {
            data.run();
        }
    }

    public static void scale(MatrixStack stack, float x, float y, float scaleX, float scaleY, Runnable data) {
        float sumScale = scaleX * scaleY;
        if (sumScale != 1.0f) {
            stack.push();
            stack.translate(x, y, 0.0f);
            stack.scale(scaleX, scaleY, 1.0f);
            stack.translate(-x, -y, 0.0f);
            Calculate.setAlpha(sumScale, data);
            stack.pop();
        } else {
            data.run();
        }
    }

    public static float textScrolling(float textWidth) {
        int speed = (int)(textWidth * 75.0f);
        return (float)MathHelper.clamp((double)((double)(System.currentTimeMillis() % (long)speed) * Math.PI / (double)speed), (double)0.0, (double)1.0) * textWidth;
    }

    public static void setAlpha(float alpha, Runnable data) {
        Calculate.setColor(1.0f, 1.0f, 1.0f, alpha, data);
    }

    public static void setColor(float red, float green, float blue, float alpha, Runnable data) {
        data.run();
    }

    public static double round(double num, double increment) {
        double rounded = (double)Math.round(num / increment) * increment;
        return (double)Math.round(rounded * 100.0) / 100.0;
    }

    public static int floorNearestMulN(int x, int n) {
        return n * (int)Math.floor((double)x / (double)n);
    }

    public static int getRed(int hex) {
        return hex >> 16 & 0xFF;
    }

    public static int getGreen(int hex) {
        return hex >> 8 & 0xFF;
    }

    public static int getBlue(int hex) {
        return hex & 0xFF;
    }

    public static int getAlpha(int hex) {
        return hex >> 24 & 0xFF;
    }

    public static int applyOpacity(int color, float opacity) {
        return ColorHelper.getArgb((int)((int)((float)Calculate.getAlpha(color) * opacity / 255.0f)), (int)Calculate.getRed(color), (int)Calculate.getGreen(color), (int)Calculate.getBlue(color));
    }

    public static Vec3d cosSin(int i, int size, double width) {
        int index = Math.min(i, size);
        float cos = (float)(Math.cos((double)index * PI2 / (double)size) * width);
        float sin = (float)(-Math.sin((double)index * PI2 / (double)size) * width);
        return new Vec3d((double)cos, 0.0, (double)sin);
    }

    public static double absSinAnimation(double input) {
        return Math.abs(1.0 + Math.sin(input)) / 2.0;
    }

    public static Vector3d interpolate(Vector3d prevPos, Vector3d pos) {
        return new Vector3d(Calculate.interpolate(prevPos.x, pos.x), Calculate.interpolate(prevPos.y, pos.y), Calculate.interpolate(prevPos.z, pos.z));
    }

    public static float interpolate(double oldValue, double newValue, double interpolationValue) {
        return (float)(oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolate(float prev, float to, float value) {
        return prev + (to - prev) * value;
    }

    public static Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
        return new Vec3d(Calculate.interpolate(prevPos.x, pos.x), Calculate.interpolate(prevPos.y, pos.y), Calculate.interpolate(prevPos.z, pos.z));
    }

    public static Vec3d interpolate(Entity entity) {
        if (entity == null) {
            return Vec3d.ZERO;
        }
        return new Vec3d(Calculate.interpolate(entity.lastX, entity.getX()), Calculate.interpolate(entity.lastY, entity.getY()), Calculate.interpolate(entity.lastZ, entity.getZ()));
    }

    public static float interpolate(float prev, float orig) {
        return MathHelper.lerp((float)tickCounter.getTickProgress(false), (float)prev, (float)orig);
    }

    public static double interpolate(double prev, double orig) {
        return MathHelper.lerp((double)tickCounter.getTickProgress(false), (double)prev, (double)orig);
    }

    public static float interpolateSmooth(double smooth, float prev, float orig) {
        return (float)MathHelper.lerp((double)((double)tickCounter.getFixedDeltaTicks() / smooth), (double)prev, (double)orig);
    }

    private Calculate() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
