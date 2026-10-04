/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_10149
 *  net.minecraft.class_10156
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_5944
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 *  org.lwjgl.opengl.GL11
 */
package code.wiosvisual.utils.display.render.shape.implement;

import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.render.draw.DrawEngine;
import code.wiosvisual.utils.display.render.draw.DrawEngineImpl;
import code.wiosvisual.utils.display.render.shape.Shape;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.display.render.pipeline.RoundRenderer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.joml.Vector4i;

public class Rectangle
implements Shape,
QuickImports {
    private static final DrawEngine BATCH_DRAW_ENGINE = new DrawEngineImpl();
    private static final ThreadLocal<Boolean> BATCHING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ArrayList<BatchEntry> BATCH = new ArrayList(64);
    private static float[] rectX = new float[64];
    private static float[] rectY = new float[64];
    private static float[] rectW = new float[64];
    private static float[] rectH = new float[64];
    private static float[] rectR = new float[64];
    private static float[] rectA = new float[64];
    private static int curCount;
    private static int lastCount;
    /**
     * Обратная матрица того состояния стека, в котором элемент начал рисоваться.
     * Нужна, чтобы записанные прямоугольники хранились в системе координат самого элемента:
     * панели вроде Potions внутри себя дополнительно масштабируют матрицу, и без учёта
     * этого масштаба HudShine («переливание») считался по неверным размерам и не покрывал панель.
     */
    /** Переиспользуемые векторы разбора матрицы: значения сразу раскладываются в float,
     * хранить их между вызовами не нужно, а рисование идёт только из рендер-потока. */
    private static final Vector3f SCRATCH_POS = new Vector3f();
    private static final Vector3f SCRATCH_SIZE = new Vector3f();
    private static final Matrix4f PANEL_BASE_INV = new Matrix4f();
    private static final Matrix4f PANEL_LOCAL = new Matrix4f();
    private static final Vector4f PANEL_CORNER = new Vector4f();
    private static boolean panelBaseSet;

    public static void beginBatch() {
        BATCHING.set(Boolean.TRUE);
        BATCH.clear();
        curCount = 0;
    }

    public static void setPanelBase(Matrix4fc base) {
        if (base == null) {
            Rectangle.clearPanelBase();
            return;
        }
        PANEL_BASE_INV.set(base).invert();
        panelBaseSet = true;
    }

    public static void clearPanelBase() {
        panelBaseSet = false;
    }

    private static void recordRect(ShapeProperties shape) {
        if (curCount >= rectX.length) {
            int n = rectX.length * 2;
            rectX = Arrays.copyOf(rectX, n);
            rectY = Arrays.copyOf(rectY, n);
            rectW = Arrays.copyOf(rectW, n);
            rectH = Arrays.copyOf(rectH, n);
            rectR = Arrays.copyOf(rectR, n);
            rectA = Arrays.copyOf(rectA, n);
        }
        float x = shape.getX();
        float y = shape.getY();
        float w = shape.getWidth();
        float h = shape.getHeight();
        float round = shape.getRound().x;
        float scale = Rectangle.localTransform(shape);
        if (scale > 0.0f) {
            float x2 = PANEL_CORNER.x;
            float y2 = PANEL_CORNER.y;
            PANEL_CORNER.set(x + w, y + h, 0.0f, 1.0f).mul((Matrix4fc)PANEL_LOCAL);
            x = Math.min(x2, PANEL_CORNER.x);
            y = Math.min(y2, PANEL_CORNER.y);
            w = Math.abs(PANEL_CORNER.x - x2);
            h = Math.abs(PANEL_CORNER.y - y2);
            round *= scale;
        }
        Rectangle.rectX[Rectangle.curCount] = x;
        Rectangle.rectY[Rectangle.curCount] = y;
        Rectangle.rectW[Rectangle.curCount] = w;
        Rectangle.rectH[Rectangle.curCount] = h;
        Rectangle.rectR[Rectangle.curCount] = round;
        Vector4i colors = shape.getColor();
        float minAlpha = Math.min(Math.min(ColorAssist.alphaf(colors.x), ColorAssist.alphaf(colors.y)), Math.min(ColorAssist.alphaf(colors.z), ColorAssist.alphaf(colors.w)));
        Rectangle.rectA[Rectangle.curCount] = minAlpha;
        ++curCount;
    }

    /**
     * Считает преобразование фигуры относительно базовой матрицы элемента и кладёт в
     * PANEL_CORNER левый верхний угол в координатах элемента. Возвращает 0, если внутреннего
     * преобразования нет (тогда координаты фигуры и так корректны).
     */
    private static float localTransform(ShapeProperties shape) {
        if (!panelBaseSet || shape.getMatrix() == null) {
            return 0.0f;
        }
        PANEL_LOCAL.set(PANEL_BASE_INV).mul(shape.getMatrix().peek().getPositionMatrix());
        float sx = PANEL_LOCAL.m00();
        float sy = PANEL_LOCAL.m11();
        if (Math.abs(sx - 1.0f) < 1.0E-4f && Math.abs(sy - 1.0f) < 1.0E-4f && Math.abs(PANEL_LOCAL.m30()) < 1.0E-4f && Math.abs(PANEL_LOCAL.m31()) < 1.0E-4f) {
            return 0.0f;
        }
        PANEL_CORNER.set(shape.getX(), shape.getY(), 0.0f, 1.0f).mul((Matrix4fc)PANEL_LOCAL);
        return (Math.abs(sx) + Math.abs(sy)) * 0.5f;
    }

    public static int panelCount() {
        return lastCount;
    }

    public static float panelX(int i) {
        return rectX[i];
    }

    public static float panelY(int i) {
        return rectY[i];
    }

    public static float panelWidth(int i) {
        return rectW[i];
    }

    public static float panelHeight(int i) {
        return rectH[i];
    }

    public static float panelRound(int i) {
        return rectR[i];
    }

    public static float panelAlpha(int i) {
        return rectA[i];
    }

    public static void flushPending() {
        if (BATCH.isEmpty()) {
            return;
        }
        // Состояние (blend/depth/cull) и шейдер теперь запечены в RoundRenderer.PIPELINE,
        // поэтому ручное управление RenderSystem больше не нужно.
        Tessellator tessellator = Tessellator.getInstance();
        int count = BATCH.size();
        for (int i = 0; i < count; ++i) {
            Rectangle.drawEntry(BATCH.get(i), tessellator);
        }
        BATCH.clear();
    }

    public static void endBatch() {
        Rectangle.flushPending();
        BATCHING.set(Boolean.FALSE);
        lastCount = curCount;
    }

    public static void flushBatch() {
        Rectangle.endBatch();
    }

    public static boolean isBatching() {
        return BATCHING.get();
    }

    @Override
    public void render(ShapeProperties shape) {
        if (BATCHING.get().booleanValue()) {
            // Геометрию записываем всегда: по ней HudShine считает границы панели.
            Rectangle.recordRect(shape);
            if (Rectangle.invisible(shape)) {
                return;
            }
            BATCH.add(BatchEntry.from(shape));
            return;
        }
        if (Rectangle.invisible(shape)) {
            return;
        }
        this.renderImmediate(shape);
    }

    /**
     * Прямоугольник, который всё равно ничего не даст на экране: нулевой размер
     * либо полная прозрачность всех четырёх углов и обводки. Каждый такой вызов — целый
     * draw call с пятью юниформами, поэтому отсекаем его до сборки записи.
     */
    private static boolean invisible(ShapeProperties shape) {
        if (shape.getWidth() <= 0.0f || shape.getHeight() <= 0.0f) {
            return true;
        }
        Vector4i color = shape.getColor();
        return ColorAssist.alpha(color.x) == 0
                && ColorAssist.alpha(color.y) == 0
                && ColorAssist.alpha(color.z) == 0
                && ColorAssist.alpha(color.w) == 0
                && ColorAssist.alpha(shape.getOutlineColor()) == 0;
    }

    private void renderImmediate(ShapeProperties shape) {
        Rectangle.drawEntry(BatchEntry.from(shape), Tessellator.getInstance());
    }

    private static void drawEntry(BatchEntry entry, Tessellator tessellator) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        float softness = entry.softness;
        BATCH_DRAW_ENGINE.quad(entry.matrix, buffer, entry.x - softness / 2.0f, entry.y - softness / 2.0f, entry.width + softness, entry.height + softness);
        RoundRenderer.draw(buffer,
                entry.scaledWidth, entry.scaledHeight,
                entry.locationX, entry.locationY,
                entry.round.x, entry.round.y, entry.round.z, entry.round.w,
                entry.thickness, softness,
                entry.color1, entry.color2, entry.color3, entry.color4, entry.outlineColor,
                1.0f);
    }

    private record BatchEntry(Matrix4f matrix, float x, float y, float width, float height, float softness, float thickness, float scaledWidth, float scaledHeight, float locationX, float locationY, Vector4f round, int color1, int color2, int color3, int color4, int outlineColor) {
        private static BatchEntry from(ShapeProperties shape) {
            net.minecraft.client.util.Window window = MinecraftClient.getInstance().getWindow();
            float scale = (float)window.getScaleFactor();
            Matrix4f matrix4f = new Matrix4f((Matrix4fc)shape.getMatrix().peek().getPositionMatrix());
            Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0f, SCRATCH_POS).mul(scale);
            Vector3f size = matrix4f.getScale(SCRATCH_SIZE).mul(scale);
            Vector4f round = new Vector4f((Vector4fc)shape.getRound()).mul(size.y);
            float scaledWidth = shape.getWidth() * size.x;
            float scaledHeight = shape.getHeight() * size.y;
            return new BatchEntry(matrix4f, shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight(), shape.getSoftness(), shape.getThickness(), scaledWidth, scaledHeight, pos.x, (float)window.getHeight() - scaledHeight - pos.y, round, shape.getColor().x, shape.getColor().y, shape.getColor().z, shape.getColor().w, shape.getOutlineColor());
        }
    }
}
