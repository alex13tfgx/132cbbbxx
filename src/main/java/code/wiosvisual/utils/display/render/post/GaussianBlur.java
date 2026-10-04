package code.wiosvisual.utils.display.render.post;

import net.minecraft.client.gl.Framebuffer;

/**
 * TODO(port 1.21.11): полноэкранный gaussian blur на удалённом API (framebuffer bind class_276.method_1235
 * + ShaderProgram + сырые GL-id). Требует переписывания на новый GPU-API. Временно no-op.
 */
public final class GaussianBlur {
    public static void applyBlurEffect(Framebuffer read, Framebuffer write, int countOfPasses, float scaleMultiplier) {
    }
}
