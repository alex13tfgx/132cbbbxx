/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4587
 */
package code.wiosvisual.utils.display.render.effect;

import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;

public class LiquidGlassEffect {
    private static final Rectangle rectangle = new Rectangle();

    public static void render(MatrixStack matrix, float x, float y, float width, float height, float radius, float alpha) {
        if (alpha <= 0.0f) {
            return;
        }
        float baseAlpha = alpha * 0.08f;
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(radius).color(new Color(255, 255, 255, (int)(baseAlpha * 255.0f)).getRGB()).build());
        float topGlowHeight = height * 0.3f;
        float topAlpha = alpha * 0.15f;
        rectangle.render(ShapeProperties.create(matrix, x, y, width, topGlowHeight).round(radius).color(new Color(255, 255, 255, (int)(topAlpha * 255.0f)).getRGB(), new Color(255, 255, 255, (int)(topAlpha * 255.0f)).getRGB(), new Color(255, 255, 255, 0).getRGB(), new Color(255, 255, 255, 0).getRGB()).build());
        float edgeWidth = Math.min(width * 0.05f, 2.0f);
        float edgeAlpha = alpha * 0.12f;
        rectangle.render(ShapeProperties.create(matrix, x, y, edgeWidth, height).round(radius).color(new Color(255, 255, 255, (int)(edgeAlpha * 255.0f)).getRGB(), new Color(255, 255, 255, 0).getRGB(), new Color(255, 255, 255, 0).getRGB(), new Color(255, 255, 255, (int)(edgeAlpha * 255.0f)).getRGB()).build());
    }

    public static void render(MatrixStack matrix, float x, float y, float width, float height, float radius, float alpha, float intensity) {
        if (alpha <= 0.0f || intensity <= 0.0f) {
            return;
        }
        float scaledAlpha = alpha * intensity;
        float baseAlpha = scaledAlpha * 0.08f;
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(radius).color(new Color(255, 255, 255, (int)(baseAlpha * 255.0f)).getRGB()).build());
        float topGlowHeight = height * 0.3f;
        float topAlpha = scaledAlpha * 0.15f;
        rectangle.render(ShapeProperties.create(matrix, x, y, width, topGlowHeight).round(radius).color(new Color(255, 255, 255, (int)(topAlpha * 255.0f)).getRGB(), new Color(255, 255, 255, (int)(topAlpha * 255.0f)).getRGB(), new Color(255, 255, 255, 0).getRGB(), new Color(255, 255, 255, 0).getRGB()).build());
        float edgeWidth = Math.min(width * 0.05f, 2.0f);
        float edgeAlpha = scaledAlpha * 0.12f;
        rectangle.render(ShapeProperties.create(matrix, x, y, edgeWidth, height).round(radius).color(new Color(255, 255, 255, (int)(edgeAlpha * 255.0f)).getRGB(), new Color(255, 255, 255, 0).getRGB(), new Color(255, 255, 255, 0).getRGB(), new Color(255, 255, 255, (int)(edgeAlpha * 255.0f)).getRGB()).build());
    }
}
