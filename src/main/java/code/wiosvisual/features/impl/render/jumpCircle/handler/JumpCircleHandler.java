/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_4534
 *  com.mojang.blaze3d.platform.GlStateManager$class_4535
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10142
 *  net.minecraft.class_10156
 *  net.minecraft.class_1044
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_5944
 *  net.minecraft.class_7833
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 */
package code.wiosvisual.features.impl.render.jumpCircle.handler;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import code.wiosvisual.features.impl.render.jumpCircle.data.JumpCircleLayer;
import code.wiosvisual.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import code.wiosvisual.main.client.Api;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public record JumpCircleHandler(JumpCircleModule module) implements Api
{
    public static Identifier IDENTIFIER = Identifier.of((String)"textures/circle.png");

    public void provideRender(MatrixStack matrixStack, float delta) {
        BufferBuilder bufferBuilder = this.createBuilder();
        this.setupGlConst();
        this.setupTexture();
        for (JumpCircleLayer layer : this.module.getStorage().getJumpCircleLayers()) {
            this.setupVertices(bufferBuilder, layer, matrixStack);
        }
        this.closeBuffer(bufferBuilder);
        this.returnGlConst();
    }

    private void setupVertices(BufferBuilder builder, JumpCircleLayer layer, MatrixStack matrixStack) {
        float animValue = layer.animation().getOutput().floatValue();
        float sinTime = (float)((double)System.nanoTime() / 1.0E9);
        int selectedColor = JumpCircleModule.colorMode.get().equals("\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439") ? ThemeManager.primaryColor.getColor() : JumpCircleModule.color.getColor();
        Camera camera = JumpCircleHandler.mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            return;
        }
        int firstColor = ColorAssist.multAlpha(selectedColor, animValue);
        int secondColor = ColorAssist.multAlpha(ColorAssist.multSaturation(selectedColor, -0.3f), animValue);
        matrixStack.push();
        matrixStack.translate(layer.vec3d());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(360.0f * sinTime));
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        float size = this.module.getSize().getValue() / (this.module.getSize().getMax() - this.module.getSize().getMin()) * animValue;
        builder.vertex(matrix4f, -size, -size, 0.0f).texture(0.0f, 0.0f).color(firstColor);
        builder.vertex(matrix4f, -size, size, 0.0f).texture(0.0f, 1.0f).color(firstColor);
        builder.vertex(matrix4f, size, size, 0.0f).texture(1.0f, 1.0f).color(secondColor);
        builder.vertex(matrix4f, size, -size, 0.0f).texture(1.0f, 0.0f).color(secondColor);
        builder.vertex(matrix4f, -size, -size, 0.0f).texture(0.0f, 0.0f).color(firstColor);
        builder.vertex(matrix4f, -size, size, 0.0f).texture(0.0f, 1.0f).color(firstColor);
        builder.vertex(matrix4f, size, size, 0.0f).texture(1.0f, 1.0f).color(secondColor);
        builder.vertex(matrix4f, size, -size, 0.0f).texture(1.0f, 0.0f).color(secondColor);
        matrixStack.pop();
    }

    private BufferBuilder createBuilder() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    private void closeBuffer(BufferBuilder bufferBuilder) {
        BuiltBuffer builtBuffer = bufferBuilder.endNullable();
        if (builtBuffer != null && builtBuffer.getDrawParameters().vertexCount() > 0) {
            WiosRender.draw(WiosRender.posTexColor(IDENTIFIER, VertexFormat.DrawMode.QUADS, false, false), builtBuffer);
        }
    }

    private void setupGlConst() {
    }

    private void returnGlConst() {
    }


    private void setupTexture() {
        AbstractTexture texture = mc.getTextureManager().getTexture(IDENTIFIER);
    }
}
