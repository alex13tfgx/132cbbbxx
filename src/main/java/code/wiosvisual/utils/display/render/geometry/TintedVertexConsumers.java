package code.wiosvisual.utils.display.render.geometry;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

/**
 * Обёртка над провайдером вершин, которая красит всю геометрию в один цвет и гасит тени.
 * Нужна, чтобы отрисовать силуэт предмета — копии модели со смещением дают контур.
 */
public final class TintedVertexConsumers
implements VertexConsumerProvider {
    private final VertexConsumerProvider delegate;
    private final int color;

    public TintedVertexConsumers(VertexConsumerProvider delegate, int color) {
        this.delegate = delegate;
        this.color = color;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return new Tinted(this.delegate.getBuffer(layer), this.color);
    }

    /** Пропускает координаты и UV как есть, но подменяет цвет и выставляет полную яркость. */
    private static final class Tinted
    implements VertexConsumer {
        private static final int FULL_LIGHT = 0xF000F0;
        private final VertexConsumer delegate;
        private final int color;

        private Tinted(VertexConsumer delegate, int color) {
            this.delegate = delegate;
            this.color = color;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.delegate.color(this.color);
            return this;
        }

        @Override
        public VertexConsumer color(int argb) {
            this.delegate.color(this.color);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            this.delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            this.delegate.overlay(240, 240);
            return this;
        }

        @Override
        public VertexConsumer light(int light) {
            this.delegate.light(FULL_LIGHT);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            this.delegate.lineWidth(width);
            return this;
        }
    }
}
