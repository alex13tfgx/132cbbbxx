package code.wiosvisual.utils.display.render.shader;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

/**
 * TODO(port 1.21.11): полноэкранный эффект размытия руки на удалённом API (framebuffer bind +
 * ShaderProgram + сырые GL-id). Требует переписывания на новый GPU-API. Временно no-op:
 * конструктор сохранён для совместимости с вызывающими, отрисовка отключена.
 */
public final class HandBlurRenderer {
    public HandBlurRenderer(Framebuffer sourceFbo, SimpleFramebuffer outputFbo, int depthAttachment, float blurRadius, float threshold, float[] tintColor, float[] tintColor2, int mode, float time, float shaderScale) {
    }

    public void renderPostProcess() {
    }
}
