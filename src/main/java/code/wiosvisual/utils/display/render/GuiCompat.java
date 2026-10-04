package code.wiosvisual.utils.display.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

/**
 * Мост между новым GUI-стеком 1.21.11 и рендером мода.
 *
 * <p>До 1.21.6 {@code DrawContext.getMatrices()} ({@code method_51448}) возвращал 3D-стек
 * {@code class_4587}, и весь GUI-рендер мода построен вокруг него ({@code ShapeProperties},
 * {@code Render2D}, шрифты берут {@code class_4587} и извлекают {@link Matrix4f}). В 1.21.6 контекст
 * перешёл на 2D-стек {@code Matrix3x2fStack}. Этот хелпер создаёт {@code class_4587},
 * инициализированный текущим 2D-преобразованием контекста (промоушен 3x2 → 4x4), чтобы мод рисовал
 * как раньше.
 *
 * <p>Важно: стек свой (один на контекст), не сам 2D-стек контекста. Трансформации ({@code push/translate/...}), применённые к нему,
 * влияют на последующую отрисовку мода (которая принимает матрицу явно), но не на собственные
 * вызовы {@code context.drawText(...)} и т.п. Для HUD, рисующего в абсолютных экранных координатах,
 * этого достаточно.
 */
public final class GuiCompat {

    private GuiCompat() {}

    /** Один стек на контекст: иначе push/translate/pop, вызванные по отдельности, теряются, а pop падает. */
    private static final java.util.Map<DrawContext, MatrixStack> STACKS = new java.util.WeakHashMap<>();

    public static MatrixStack stack(DrawContext context) {
        MatrixStack ms = STACKS.computeIfAbsent(context, c -> new MatrixStack());
        // Пока мод ничего не запушил — синхронизируемся с текущим 2D-преобразованием контекста.
        if (ms.isEmpty()) {
            Matrix3x2f m = context.getMatrices();
            Matrix4f m4 = ms.peek().getPositionMatrix();
            m4.identity();
            m4.m00(m.m00);
            m4.m01(m.m01);
            m4.m10(m.m10);
            m4.m11(m.m11);
            m4.m30(m.m20);
            m4.m31(m.m21);
            ms.peek().getNormalMatrix().identity();
        }
        return ms;
    }
}
