package code.wiosvisual.utils.client.window;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

/**
 * Ставит логотип клиента иконкой окна игры (панель задач и заголовок) вместо ванильной.
 * Ванилла выставляет свою иконку в конструкторе MinecraftClient, поэтому вызывать это
 * нужно уже после него — иначе наш значок будет перезаписан.
 */
public final class WindowIcon {
    private static final String LOGO_PATH = "/assets/wiosvisual/background/logo.png";
    /** Windows берёт из набора ближайший размер, поэтому отдаём несколько. */
    private static final int[] SIZES = new int[]{16, 24, 32, 48, 64, 128, 256};

    private WindowIcon() {
    }

    public static void apply() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        if (handle == 0L) {
            return;
        }
        BufferedImage source = WindowIcon.readLogo();
        if (source == null) {
            return;
        }
        List<ByteBuffer> allocated = new ArrayList<ByteBuffer>(SIZES.length);
        GLFWImage.Buffer icons = null;
        try {
            icons = GLFWImage.malloc(SIZES.length);
            for (int i = 0; i < SIZES.length; ++i) {
                int size = SIZES[i];
                ByteBuffer pixels = WindowIcon.toRgba(source, size);
                allocated.add(pixels);
                icons.position(i).width(size).height(size).pixels(pixels);
            }
            icons.position(0);
            GLFW.glfwSetWindowIcon((long)handle, (GLFWImage.Buffer)icons);
        }
        catch (Throwable error) {
            // Иконка — косметика: если драйвер или платформа её не приняли, игра должна жить дальше.
        }
        finally {
            if (icons != null) {
                icons.free();
            }
            for (ByteBuffer buffer : allocated) {
                MemoryUtil.memFree((ByteBuffer)buffer);
            }
        }
    }

    private static BufferedImage readLogo() {
        try (InputStream stream = WindowIcon.class.getResourceAsStream(LOGO_PATH);){
            BufferedImage bufferedImage = stream == null ? null : ImageIO.read(stream);
            return bufferedImage;
        }
        catch (Throwable error) {
            return null;
        }
    }

    /** Масштабирует логотип до нужной стороны и раскладывает его в RGBA, как ждёт GLFW. */
    private static ByteBuffer toRgba(BufferedImage source, int size) {
        BufferedImage scaled = new BufferedImage(size, size, 2);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, size, size, null);
        graphics.dispose();
        ByteBuffer buffer = MemoryUtil.memAlloc((int)(size * size * 4));
        for (int y = 0; y < size; ++y) {
            for (int x = 0; x < size; ++x) {
                int argb = scaled.getRGB(x, y);
                buffer.put((byte)(argb >> 16 & 0xFF));
                buffer.put((byte)(argb >> 8 & 0xFF));
                buffer.put((byte)(argb & 0xFF));
                buffer.put((byte)(argb >> 24 & 0xFF));
            }
        }
        buffer.flip();
        return buffer;
    }
}
