/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_2960
 *  net.minecraft.class_4587
 *  net.minecraft.class_7833
 */
package code.wiosvisual.utils.display.render.shape.implement;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.shape.Shape;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class Image
implements Shape,
QuickImports {
    private String texture;

    @Override
    public void render(ShapeProperties shape) {
        MatrixStack matrix = shape.getMatrix();
        // Порт 1.21.11: drawEngine.quad(...,color) рисует сплошным цветом (POSITION_COLOR),
        // текстура здесь не сэмплится; ручная привязка через удалённый setShaderTexture убрана.
        float width = shape.getWidth();
        float x = shape.getX() + width;
        float y = shape.getY();
        matrix.push();
        matrix.translate(x, y, 0.0f);
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0f));
        matrix.translate(-x, -y, 0.0f);
        drawEngine.quad(matrix.peek().getPositionMatrix(), x, y, shape.getHeight(), width, shape.getColor().x);
        matrix.pop();
    }

    public Image setTexture(String texture) {
        this.texture = texture;
        return this;
    }
}
