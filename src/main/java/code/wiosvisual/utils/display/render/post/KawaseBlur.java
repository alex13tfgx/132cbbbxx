package code.wiosvisual.utils.display.render.post;

import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * TODO(port 1.21.11): полноэкранный kawase blur на удалённом API (framebuffer bind + ShaderProgram +
 * сырые GL-id). Требует переписывания на новый GPU-API. Временно no-op (blur фона отключён).
 */
public final class KawaseBlur
implements QuickImports {
    public static final KawaseBlur INSTANCE = new KawaseBlur();
    public final List<SimpleFramebuffer> fbos = new ArrayList<SimpleFramebuffer>();

    public void recreate() {
    }

    public void render(MatrixStack matrixStack, int steps, float offset) {
    }

    public int getBlurTexture() {
        return 0;
    }
}
