package code.wiosvisual.utils.display.render.esp;

import code.wiosvisual.utils.display.render.pipeline.WiosRender;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;

/**
 * Свечение по силуэту модели через трафарет: сначала сама модель пишется в stencil,
 * затем она же рисуется чуть увеличенной там, где трафарета нет — наружу выступает
 * только ободок.
 *
 * <p>В отличие от ванильного Glowing обводка проходит проверку глубины ({@code GL_LEQUAL}),
 * поэтому за стеной её не видно. Модели ставятся в очередь во время отрисовки сущности,
 * а рисуются позже: буфер сущностей у Minecraft отложенный, и рисовать обводку сразу
 * означало бы положить её под ещё не выгруженные модели.
 */
public final class OutlineRenderer {
    /** GL_LEQUAL — обводку перекрывает геометрия мира. */
    private static final int DEPTH_LEQUAL = 515;
    private static final int STENCIL_TEST = 2960;
    private static final int STENCIL_BUFFER_BIT = 1024;
    private static final int ALWAYS = 519;
    private static final int NOTEQUAL = 517;
    private static final int KEEP = 7680;
    private static final int REPLACE = 7681;

    private static final List<Entry> QUEUE = new ArrayList<Entry>();

    private OutlineRenderer() {
    }

    /** Копия матрицы обязательна: стек продолжит меняться до момента отрисовки. */
    public static void enqueue(EntityModel<?> model, MatrixStack matrices, int color, float thickness) {
        MatrixStack copy = new MatrixStack();
        copy.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
        copy.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());
        QUEUE.add(new Entry(model, copy, color, thickness));
    }

    public static boolean hasEntries() {
        return !QUEUE.isEmpty();
    }

    public static void clear() {
        QUEUE.clear();
    }

    public static void renderAll() {
        if (QUEUE.isEmpty()) {
            return;
        }
        OutlineRenderer.setupShader();
        for (Entry entry : QUEUE) {
            MatrixStack stack = new MatrixStack();
            stack.peek().getPositionMatrix().set(entry.matrices.peek().getPositionMatrix());
            stack.peek().getNormalMatrix().set(entry.matrices.peek().getNormalMatrix());

            // Проход 1: силуэт модели в трафарет, без записи цвета и глубины.
            GL11.glEnable((int)STENCIL_TEST);
            GL11.glStencilMask((int)255);
            GL11.glClear((int)STENCIL_BUFFER_BIT);
            GL11.glStencilFunc((int)ALWAYS, (int)1, (int)255);
            GL11.glStencilOp((int)KEEP, (int)KEEP, (int)REPLACE);
            OutlineRenderer.drawModel(entry.model, stack, -1);

            // Проход 2: увеличенная копия только вне трафарета — это и есть ободок.
            GL11.glStencilFunc((int)NOTEQUAL, (int)1, (int)255);
            GL11.glStencilOp((int)KEEP, (int)KEEP, (int)KEEP);
            // Ключевое отличие от ванильного свечения: обводка сравнивается с глубиной мира.
            stack.push();
            stack.scale(entry.thickness, entry.thickness, entry.thickness);
            OutlineRenderer.drawModel(entry.model, stack, entry.color);
            stack.pop();

            GL11.glStencilMask((int)255);
            GL11.glStencilFunc((int)ALWAYS, (int)0, (int)255);
            GL11.glDisable((int)STENCIL_TEST);
        }
        QUEUE.clear();
    }

    private static void setupShader() {
        // TODO(port 1.21.11): entity-outline framebuffer API (method_3316/method_23209) удалён.
    }

    private static void drawModel(EntityModel<?> model, MatrixStack matrices, int color) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        model.render(matrices, buffer, 0xF000F0, OverlayTexture.DEFAULT_UV, color);
        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            // TODO(port 1.21.11): контур ESP использовал entity-формат field_1580 + outline-фреймбуфер;
            // требует переписи на entity RenderLayer. Временно не рисуем.
            built.close();
        }
    }

    private record Entry(EntityModel<?> model, MatrixStack matrices, int color, float thickness) {
    }
}
