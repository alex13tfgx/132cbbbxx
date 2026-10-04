/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  it.unimi.dsi.fastutil.chars.Char2IntArrayMap
 *  net.minecraft.class_3532
 *  net.minecraft.class_9848
 *  org.joml.Vector4i
 *  org.lwjgl.opengl.GL11
 */
package code.wiosvisual.utils.display.color;

import code.wiosvisual.utils.math.calc.Calculate;
import code.wiosvisual.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;

public final class ColorAssist {
    public static final int green = new Color(64, 255, 64).getRGB();
    public static final int yellow = new Color(255, 255, 64).getRGB();
    public static final int orange = new Color(255, 128, 32).getRGB();
    public static final int red = new Color(255, 64, 64).getRGB();
    private static final long CACHE_EXPIRATION_TIME = 60000L;
    private static final ConcurrentHashMap<ColorKey, CacheEntry> colorCache = new ConcurrentHashMap();
    private static final ScheduledExecutorService cacheCleaner = Executors.newScheduledThreadPool(1);
    private static final DelayQueue<CacheEntry> cleanupQueue = new DelayQueue();
    public static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9a-f-or]");
    public static Char2IntArrayMap colorCodes = new Char2IntArrayMap(){
        {
            this.put('0', 0);
            this.put('1', 170);
            this.put('2', 43520);
            this.put('3', 43690);
            this.put('4', 0xAA0000);
            this.put('5', 0xAA00AA);
            this.put('6', 0xFFAA00);
            this.put('7', 0xAAAAAA);
            this.put('8', 0x555555);
            this.put('9', 0x5555FF);
            this.put('A', 0x55FF55);
            this.put('B', 0x55FFFF);
            this.put('C', 0xFF5555);
            this.put('D', 0xFF55FF);
            this.put('E', 0xFFFF55);
            this.put('F', 0xFFFFFF);
        }
    };
    public static final int RED;
    public static final int GREEN;
    public static final int BLUE;
    public static final int YELLOW;
    public static final int WHITE;
    public static final int BLACK;
    public static final int HALF_BLACK;
    public static final int LIGHT_RED;

    public static int colorForRectsCustom$() {
        return ThemeManager.primaryColor.getColor();
    }

    public static int colorForRectsBlack$() {
        return new Color(0, 0, 0, 255).getRGB();
    }

    public static int colorForTextWhite$() {
        return new Color(255, 255, 255, 255).getRGB();
    }

    public static int colorForTextCustom$() {
        return ThemeManager.primaryColor.getColor();
    }

    public static int clampSaturation(int color, float min, float max) {
        float[] hsb = Color.RGBtoHSB(ColorAssist.getRed(color), ColorAssist.getGreen(color), ColorAssist.getBlue(color), new float[3]);
        return Color.HSBtoRGB(hsb[0], Math.clamp(hsb[1], min, max), hsb[2]);
    }

    public static int red(int c) {
        return c >> 16 & 0xFF;
    }

    public static int green(int c) {
        return c >> 8 & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int alpha(int c) {
        return c >> 24 & 0xFF;
    }

    public static float redf(int c) {
        return (float)ColorAssist.red(c) / 255.0f;
    }

    public static float greenf(int c) {
        return (float)ColorAssist.green(c) / 255.0f;
    }

    public static float bluef(int c) {
        return (float)ColorAssist.blue(c) / 255.0f;
    }

    public static float alphaf(int c) {
        return (float)ColorAssist.alpha(c) / 255.0f;
    }

    public static int[] getRGBA(int c) {
        return new int[]{ColorAssist.red(c), ColorAssist.green(c), ColorAssist.blue(c), ColorAssist.alpha(c)};
    }

    public static int[] getRGB(int c) {
        return new int[]{ColorAssist.red(c), ColorAssist.green(c), ColorAssist.blue(c)};
    }

    public static float[] getRGBAf(int c) {
        return new float[]{ColorAssist.redf(c), ColorAssist.greenf(c), ColorAssist.bluef(c), ColorAssist.alphaf(c)};
    }

    public static float[] getRGBf(int c) {
        return new float[]{ColorAssist.redf(c), ColorAssist.greenf(c), ColorAssist.bluef(c)};
    }

    public static int multSaturation(int color, float saturation) {
        float[] hsb = Color.RGBtoHSB(ColorAssist.getRed(color), ColorAssist.getGreen(color), ColorAssist.getBlue(color), new float[3]);
        return Color.HSBtoRGB(hsb[0], Calculate.clamp(hsb[1] + saturation, 0.0f, 1.0f), hsb[2]);
    }

    public static int getColor(float red, float green, float blue, float alpha) {
        return ColorAssist.getColor(Math.round(red * 255.0f), Math.round(green * 255.0f), Math.round(blue * 255.0f), Math.round(alpha * 255.0f));
    }

    public static int getColor(int red, int green, int blue, float alpha) {
        return ColorAssist.getColor(red, green, blue, Math.round(alpha * 255.0f));
    }

    public static int getColor(float red, float green, float blue) {
        return ColorAssist.getColor(red, green, blue, 1.0f);
    }

    public static int getColor(int brightness, int alpha) {
        return ColorAssist.getColor(brightness, brightness, brightness, alpha);
    }

    public static int getColor(int brightness, float alpha) {
        return ColorAssist.getColor(brightness, Math.round(alpha * 255.0f));
    }

    public static int getColor(int brightness) {
        return ColorAssist.getColor(brightness, brightness, brightness);
    }

    public static int replAlpha(int color, int alpha) {
        return ColorAssist.getColor(ColorAssist.red(color), ColorAssist.green(color), ColorAssist.blue(color), alpha);
    }

    public static int replAlpha(int color, float alpha) {
        return ColorAssist.getColor(ColorAssist.red(color), ColorAssist.green(color), ColorAssist.blue(color), alpha);
    }

    public static int multAlpha(int color, float percent01) {
        return ColorAssist.getColor(ColorAssist.red(color), ColorAssist.green(color), ColorAssist.blue(color), Math.round((float)ColorAssist.alpha(color) * percent01));
    }

    public static int applyOpacity(int hex, int percent) {
        return ColorAssist.applyOpacity(hex, 2.55f * (float)Math.min(percent, 100));
    }

    public static int applyOpacity(int hex, float opacity) {
        return ColorHelper.getArgb((int)((int)((float)ColorHelper.getAlpha((int)hex) * (opacity / 255.0f))), (int)ColorHelper.getRed((int)hex), (int)ColorHelper.getGreen((int)hex), (int)ColorHelper.getBlue((int)hex));
    }

    public static int lerp(float value, int from, int to) {
        return ColorHelper.lerp((float)value, (int)from, (int)to);
    }

    public static int pixelColor(int x, int y) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);
        GL11.glReadPixels((int)x, (int)y, (int)1, (int)1, (int)6408, (int)5121, (ByteBuffer)byteBuffer);
        return ColorHelper.getArgb((int)ColorAssist.getRed(byteBuffer.get()), (int)ColorAssist.getGreen(byteBuffer.get()), (int)ColorAssist.getBlue(byteBuffer.get()));
    }

    public static int calculateHuyDegrees(int divisor, int offset) {
        long currentTime = System.currentTimeMillis();
        long calculatedValue = (currentTime / (long)divisor + (long)offset) % 360L;
        return (int)calculatedValue;
    }

    public static int reAlphaInt(int color, int alpha) {
        return Math.clamp((long)alpha, 0, 255) << 24 | color & 0xFFFFFF;
    }

    public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
        float hueStep = 90.0f;
        float basaHuy = ColorAssist.calculateHuyDegrees(speed, index);
        float huy = (basaHuy + (float)index * hueStep) % 360.0f;
        saturation = Math.clamp(saturation, 0.0f, 1.0f);
        brightness = Math.clamp(brightness, 0.0f, 1.0f);
        int rgb = Color.HSBtoRGB(huy /= 360.0f, saturation, brightness);
        int Ialpha = Math.max(0, Math.min(255, (int)(alpha * 255.0f)));
        return ColorAssist.reAlphaInt(rgb, Ialpha);
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static void setAlphaColor(int color, float alpha) {
        float red = (float)(color >> 16 & 0xFF) / 255.0f;
        float green = (float)(color >> 8 & 0xFF) / 255.0f;
        float blue = (float)(color & 0xFF) / 255.0f;
    }

    public static void setColor(int color) {
        ColorAssist.setAlphaColor(color, (float)(color >> 24 & 0xFF) / 255.0f);
    }

    public static int toColor(String hexColor) {
        int argb = Integer.parseInt(hexColor.substring(1), 16);
        return ColorAssist.setAlpha(argb, 255);
    }

    public static int setAlpha(int color, int alpha) {
        return color & 0xFFFFFF | alpha << 24;
    }

    public static int gradient(int start, int end, int index, int speed) {
        int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int color = ColorAssist.interpolate(start, end, Math.clamp((float)angle / 180.0f - 1.0f, 0.0f, 1.0f));
        float[] hs = ColorAssist.rgba(color);
        float[] hsb = Color.RGBtoHSB((int)(hs[0] * 255.0f), (int)(hs[1] * 255.0f), (int)(hs[2] * 255.0f), null);
        hsb[1] = hsb[1] * 1.5f;
        hsb[1] = Math.min(hsb[1], 1.0f);
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    public static int gradient(int speed, int index, int ... colors) {
        int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int)((float)angle / 360.0f * (float)colors.length);
        if (colorIndex == colors.length) {
            --colorIndex;
        }
        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return ColorAssist.interpolate(color1, color2, (float)angle / 360.0f * (float)colors.length - (float)colorIndex);
    }

    public static Double interpolateD(double oldValue, double newValue, double interpolationValue) {
        return oldValue + (newValue - oldValue) * interpolationValue;
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return ColorAssist.interpolateD(oldValue, newValue, (float)interpolationValue).intValue();
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

    public static int multColor(int colorStart, int colorEnd, float progress) {
        return ColorAssist.getColor(Math.round((float)ColorAssist.red(colorStart) * (ColorAssist.redf(colorEnd) * progress)), Math.round((float)ColorAssist.green(colorStart) * (ColorAssist.greenf(colorEnd) * progress)), Math.round((float)ColorAssist.blue(colorStart) * (ColorAssist.bluef(colorEnd) * progress)), Math.round((float)ColorAssist.alpha(colorStart) * (ColorAssist.alphaf(colorEnd) * progress)));
    }

    public static int multRed(int colorStart, int colorEnd, float progress) {
        return ColorAssist.getColor(Math.round((float)ColorAssist.red(colorStart) * (ColorAssist.redf(colorEnd) * progress)), Math.round((float)ColorAssist.green(colorStart) * (ColorAssist.greenf(colorEnd) * progress)), Math.round((float)ColorAssist.blue(colorStart) * (ColorAssist.bluef(colorEnd) * progress)), Math.round((float)ColorAssist.alpha(colorStart) * (ColorAssist.alphaf(colorEnd) * progress)));
    }

    public static int multDark(int color, float percent01) {
        return ColorAssist.getColor(Math.round((float)ColorAssist.red(color) * percent01), Math.round((float)ColorAssist.green(color) * percent01), Math.round((float)ColorAssist.blue(color) * percent01), ColorAssist.alpha(color));
    }

    public static int multBright(int color, float percent01) {
        return ColorAssist.getColor(Math.min(255, Math.round((float)ColorAssist.red(color) / percent01)), Math.min(255, Math.round((float)ColorAssist.green(color) / percent01)), Math.min(255, Math.round((float)ColorAssist.blue(color) / percent01)), ColorAssist.alpha(color));
    }

    public static int overCol(int color1, int color2, float percent01) {
        float percent = MathHelper.clamp((float)percent01, (float)0.0f, (float)1.0f);
        return ColorAssist.getColor(MathHelper.lerp((float)percent, (int)ColorAssist.red(color1), (int)ColorAssist.red(color2)), MathHelper.lerp((float)percent, (int)ColorAssist.green(color1), (int)ColorAssist.green(color2)), MathHelper.lerp((float)percent, (int)ColorAssist.blue(color1), (int)ColorAssist.blue(color2)), MathHelper.lerp((float)percent, (int)ColorAssist.alpha(color1), (int)ColorAssist.alpha(color2)));
    }

    public static Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
        return new Vector4i(ColorAssist.multRedAndAlpha(color.x, red, alpha), ColorAssist.multRedAndAlpha(color.y, red, alpha), ColorAssist.multRedAndAlpha(color.w, red, alpha), ColorAssist.multRedAndAlpha(color.z, red, alpha));
    }

    public static int multRedAndAlpha(int color, float red, float alpha) {
        return ColorAssist.getColor(ColorAssist.red(color), Math.min(255, Math.round((float)ColorAssist.green(color) / red)), Math.min(255, Math.round((float)ColorAssist.blue(color) / red)), Math.round((float)ColorAssist.alpha(color) * alpha));
    }

    public static int multRed(int color, float percent01) {
        return ColorAssist.getColor(ColorAssist.red(color), Math.min(255, Math.round((float)ColorAssist.green(color) / percent01)), Math.min(255, Math.round((float)ColorAssist.blue(color) / percent01)), ColorAssist.alpha(color));
    }

    public static int multGreen(int color, float percent01) {
        return ColorAssist.getColor(Math.min(255, Math.round((float)ColorAssist.green(color) / percent01)), ColorAssist.green(color), Math.min(255, Math.round((float)ColorAssist.blue(color) / percent01)), ColorAssist.alpha(color));
    }

    public static int rgba(int red, int green, int blue, int alpha) {
        return ColorAssist.getColor(red, green, blue, alpha);
    }

    public static int[] genGradientForText(int color1, int color2, int length) {
        int[] gradient = new int[length];
        for (int i = 0; i < length; ++i) {
            float pc = (float)i / (float)(length - 1);
            gradient[i] = ColorAssist.overCol(color1, color2, pc);
        }
        return gradient;
    }

    public static float[] rgba(int color) {
        return new float[]{(float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, (float)(color >> 24 & 0xFF) / 255.0f};
    }

    public static int interpolate(int start, int end, float value) {
        float[] startColor = ColorAssist.rgba(start);
        float[] endColor = ColorAssist.rgba(end);
        return ColorAssist.rgba((int)Calculate.interpolate(startColor[0] * 255.0f, endColor[0] * 255.0f, value), (int)Calculate.interpolate(startColor[1] * 255.0f, endColor[1] * 255.0f, value), (int)Calculate.interpolate(startColor[2] * 255.0f, endColor[2] * 255.0f, value), (int)Calculate.interpolate(startColor[3] * 255.0f, endColor[3] * 255.0f, value));
    }

    public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
        float hue = (float)angle / 360.0f;
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return ColorAssist.getColor(ColorAssist.red(color), ColorAssist.green(color), ColorAssist.blue(color), Math.round(opacity * 255.0f));
    }

    public static int fade(int speed, int index, int first, int second) {
        int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
        angle = angle >= 180 ? 360 - angle : angle;
        return ColorAssist.overCol(first, second, (float)angle / 180.0f);
    }

    public static int rgbaFloat(float r, float g, float b, float a) {
        return (int)(MathHelper.clamp((float)a, (float)0.0f, (float)1.0f) * 255.0f) << 24 | (int)(MathHelper.clamp((float)r, (float)0.0f, (float)1.0f) * 255.0f) << 16 | (int)(MathHelper.clamp((float)g, (float)0.0f, (float)1.0f) * 255.0f) << 8 | (int)(MathHelper.clamp((float)b, (float)0.0f, (float)1.0f) * 255.0f);
    }

    public static int fade(int index) {
        Color clientColor = new Color(ColorAssist.getClientColor());
        return ColorAssist.fade(8, index, ColorAssist.getClientColor(), -1);
    }

    public static int getRainbowColor(int color1, int color2) {
        long time = System.currentTimeMillis();
        float t = (float)(Math.sin((double)time / 500.0 * 0.001) + 1.0) / 2.0f;
        return ColorAssist.interpolateColor(color1, color2, t);
    }

    public static int getDynamicRainbowColor(int color1, int color2) {
        long time = System.currentTimeMillis();
        float speed = 0.5f;
        float t = (float)(Math.sin((float)time / speed) + 1.0) / 2.0f;
        t = t * t * (3.0f - 2.0f * t);
        return ColorAssist.interpolateColor(color1, color2, t);
    }

    public static int getTrueRainbowColor() {
        long time = System.currentTimeMillis();
        float speed = 0.5f;
        float hue = (float)time / speed % 360.0f;
        return Color.HSBtoRGB(hue / 360.0f, 0.8f, 1.0f);
    }

    public static int getGradientRainbowColor(int color1, int color2) {
        long time = System.currentTimeMillis();
        float speed = 0.5f;
        float t = (float)(Math.sin((float)time / speed) + 1.0) / 2.0f;
        float rainbowMod = (float)(Math.sin((float)time / speed * 2.0f) * (double)0.1f + (double)0.9f);
        t *= rainbowMod;
        t = MathHelper.clamp((float)t, (float)0.0f, (float)1.0f);
        return ColorAssist.interpolateColor(color1, color2, t);
    }

    public static int getSmoothRainbowColor(int color1, int color2) {
        long time = System.currentTimeMillis();
        float speed = 0.5f;
        float t = (float)(Math.sin((float)time / speed) + 1.0) / 2.0f;
        t = t * t * (3.0f - 2.0f * t);
        return ColorAssist.interpolateColor(color1, color2, t);
    }

    public static int getSimpleRainbowColor(int color1, int color2) {
        long time = System.currentTimeMillis();
        float speed = 500.0f;
        float t = (float)(Math.sin((float)time / (speed * 0.1f)) + 1.0) / 2.0f;
        return ColorAssist.interpolateColor(color1, color2, t);
    }

    public static int getTrueRainbowColor(int color1, int color2) {
        return ColorAssist.fade(5, 0, color1, color2);
    }

    public static int getFastRainbowColor(int color1, int color2) {
        long time = System.currentTimeMillis();
        float hue = (float)time / 10.0f % 360.0f;
        int color = Color.HSBtoRGB(hue / 360.0f, 0.8f, 1.0f);
        return color;
    }

    public static int getDebugRainbowColor(int color1, int color2) {
        long time = System.currentTimeMillis();
        float speed = 0.5f;
        float t = (float)(Math.sin((float)time / (speed * 100.0f)) + 1.0) / 2.0f;
        if (time % 1000L < 50L) {
            System.out.println("Rainbow t: " + t + ", speed: " + speed + ", time: " + time);
        }
        return ColorAssist.interpolateColor(color1, color2, t);
    }

    public static int interpolateColor(int colorStart, int colorEnd, float t) {
        float startAlpha = (float)(colorStart >> 24 & 0xFF) / 255.0f;
        float startRed = (float)(colorStart >> 16 & 0xFF) / 255.0f;
        float startGreen = (float)(colorStart >> 8 & 0xFF) / 255.0f;
        float startBlue = (float)(colorStart & 0xFF) / 255.0f;
        float endAlpha = (float)(colorEnd >> 24 & 0xFF) / 255.0f;
        float endRed = (float)(colorEnd >> 16 & 0xFF) / 255.0f;
        float endGreen = (float)(colorEnd >> 8 & 0xFF) / 255.0f;
        float endBlue = (float)(colorEnd & 0xFF) / 255.0f;
        float alpha = startAlpha + t * (endAlpha - startAlpha);
        float red = startRed + t * (endRed - startRed);
        float green = startGreen + t * (endGreen - startGreen);
        float blue = startBlue + t * (endBlue - startBlue);
        return (int)(alpha * 255.0f) << 24 | (int)(red * 255.0f) << 16 | (int)(green * 255.0f) << 8 | (int)(blue * 255.0f);
    }

    public static Vector4i roundClientColor(float alpha) {
        return new Vector4i(ColorAssist.multAlpha(ColorAssist.fade(270), alpha), ColorAssist.multAlpha(ColorAssist.fade(0), alpha), ColorAssist.multAlpha(ColorAssist.fade(180), alpha), ColorAssist.multAlpha(ColorAssist.fade(90), alpha));
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        ColorKey key = new ColorKey(red, green, blue, alpha);
        CacheEntry cacheEntry = colorCache.computeIfAbsent(key, k -> {
            CacheEntry newEntry = new CacheEntry((ColorKey)k, ColorAssist.computeColor(red, green, blue, alpha), 60000L);
            cleanupQueue.offer(newEntry);
            return newEntry;
        });
        return cacheEntry.getColor();
    }

    public static int getColor(int red, int green, int blue) {
        return ColorAssist.getColor(red, green, blue, 255);
    }

    private static int computeColor(int red, int green, int blue, int alpha) {
        return MathHelper.clamp((int)alpha, (int)0, (int)255) << 24 | MathHelper.clamp((int)red, (int)0, (int)255) << 16 | MathHelper.clamp((int)green, (int)0, (int)255) << 8 | MathHelper.clamp((int)blue, (int)0, (int)255);
    }

    private static String generateKey(int red, int green, int blue, int alpha) {
        return red + "," + green + "," + blue + "," + alpha;
    }

    public static String formatting(int color) {
        return "\u23cf" + color + "\u23cf";
    }

    public static String removeFormatting(String text) {
        return text == null || text.isEmpty() ? null : FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }

    public static int getMainGuiColor() {
        return new Color(0, 0, 0, 255).getRGB();
    }

    public static int getGuiRectColor(float alpha) {
        return ColorAssist.multAlpha(new Color(0).getRGB(), alpha);
    }

    public static int getGuiRectColor2(float alpha) {
        return ColorAssist.multAlpha(new Color(0xA0A0C).getRGB(), alpha);
    }

    public static int getRect(float alpha) {
        return ColorAssist.multAlpha(new Color(0, 0, 0, 228).getRGB(), alpha);
    }

    public static int getRectDarker(float alpha) {
        return ColorAssist.multAlpha(new Color(0).getRGB(), alpha);
    }

    public static int getText(float alpha) {
        return ColorAssist.multAlpha(ColorAssist.getText(), alpha);
    }

    public static int getText() {
        return new Color(255, 255, 255, 255).getRGB();
    }

    public static int getClientColor() {
        return ThemeManager.primaryColor.getColor();
    }

    public static int getClientColor2() {
        return ThemeManager.accentShade(ColorAssist.getClientColor());
    }

    public static int getClientColor2(float alpha) {
        return ColorAssist.multAlpha(ColorAssist.getClientColor2(), alpha);
    }

    public static int getClientColor(float alpha) {
        return ColorAssist.multAlpha(ColorAssist.getClientColor(), alpha);
    }

    public static int getHudColor() {
        return ThemeManager.primaryColor.getColor();
    }

    public static int getHudColor(float alpha) {
        return ColorAssist.multAlpha(ColorAssist.getHudColor(), alpha);
    }

    public static int getHudColor2() {
        return ThemeManager.accentShade(ColorAssist.getHudColor());
    }

    public static int getHudColor2(float alpha) {
        return ColorAssist.multAlpha(ColorAssist.getHudColor2(), alpha);
    }

    public static int getFriendColor() {
        return new Color(0x55FF55).getRGB();
    }

    public static int getOutline(float alpha, float bright) {
        return ColorAssist.multBright(ColorAssist.multAlpha(ColorAssist.getOutline(), alpha), bright);
    }

    public static int getOutline(float alpha) {
        return ColorAssist.multAlpha(ColorAssist.getOutline(), alpha);
    }

    public static int getOutline() {
        return new Color(3618630).getRGB();
    }

    private ColorAssist() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        cacheCleaner.scheduleWithFixedDelay(() -> {
            CacheEntry entry = (CacheEntry)cleanupQueue.poll();
            while (entry != null) {
                if (entry.isExpired()) {
                    colorCache.remove(entry.getKey());
                }
                entry = (CacheEntry)cleanupQueue.poll();
            }
        }, 0L, 1L, TimeUnit.SECONDS);
        RED = ColorAssist.getColor(255, 0, 0);
        GREEN = ColorAssist.getColor(0, 255, 0);
        BLUE = ColorAssist.getColor(0, 0, 255);
        YELLOW = ColorAssist.getColor(255, 255, 0);
        WHITE = ColorAssist.getColor(255);
        BLACK = ColorAssist.getColor(0);
        HALF_BLACK = ColorAssist.getColor(0, 0.5f);
        LIGHT_RED = ColorAssist.getColor(255, 85, 85);
    }

    private static class ColorKey {
        final int red;
        final int green;
        final int blue;
        final int alpha;

        public int getRed() {
            return this.red;
        }

        public int getGreen() {
            return this.green;
        }

        public int getBlue() {
            return this.blue;
        }

        public int getAlpha() {
            return this.alpha;
        }

        public ColorKey(int red, int green, int blue, int alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ColorKey)) {
                return false;
            }
            ColorKey other = (ColorKey)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getRed() != other.getRed()) {
                return false;
            }
            if (this.getGreen() != other.getGreen()) {
                return false;
            }
            if (this.getBlue() != other.getBlue()) {
                return false;
            }
            return this.getAlpha() == other.getAlpha();
        }

        protected boolean canEqual(Object other) {
            return other instanceof ColorKey;
        }

        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getRed();
            result = result * 59 + this.getGreen();
            result = result * 59 + this.getBlue();
            result = result * 59 + this.getAlpha();
            return result;
        }
    }

    private static class CacheEntry
    implements Delayed {
        private final ColorKey key;
        private final int color;
        private final long expirationTime;

        CacheEntry(ColorKey key, int color, long ttl) {
            this.key = key;
            this.color = color;
            this.expirationTime = System.currentTimeMillis() + ttl;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = this.expirationTime - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof CacheEntry) {
                return Long.compare(this.expirationTime, ((CacheEntry)other).expirationTime);
            }
            return 0;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > this.expirationTime;
        }

        public ColorKey getKey() {
            return this.key;
        }

        public int getColor() {
            return this.color;
        }

        public long getExpirationTime() {
            return this.expirationTime;
        }
    }

    public static class IntColor {
        public static float[] rgb(int color) {
            return new float[]{(float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, (float)(color >> 24 & 0xFF) / 255.0f};
        }

        public static int rgba(int r, int g, int b, int a) {
            return a << 24 | r << 16 | g << 8 | b;
        }

        public static int rgb(int r, int g, int b) {
            return 0xFF000000 | r << 16 | g << 8 | b;
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
    }
}
