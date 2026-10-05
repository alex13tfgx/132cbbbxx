package guardlisten;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Корневая панель окна со слоистой отрисовкой.
 *
 * Слои: фон + частицы (меняются каждый кадр) и интерфейс (меняется редко).
 * Интерфейс рисуется в кэш-картинку только в тех областях, которые реально попросили
 * перерисовку (наведение, ввод, переходы). Кадр частиц — это фон + спрайты + готовый кэш,
 * без повторного сглаживания всех кнопок и карточек.
 */
final class LayeredRoot extends JPanel {
    interface Backdrop { void paint(Graphics2D g, int w, int h); }

    private final Backdrop backdrop;
    /** Прозрачность всего окна (плавное появление при запуске). */
    float winAlpha = 1f;
    private final boolean cacheEnabled = !Boolean.getBoolean("gl.nocache");
    private BufferedImage cache;
    private double cacheScale;
    private Rectangle dirty;
    private final Object lock = new Object();
    private boolean backdropOnly;

    LayeredRoot(Backdrop backdrop) {
        super(null);
        this.backdrop = backdrop;
        setOpaque(true);
        RepaintManager.setCurrentManager(new Manager());
    }

    /** Перерисовать только фон/частицы — интерфейс берётся из кэша. */
    void repaintBackdrop() {
        backdropOnly = true;
        try { repaint(); } finally { backdropOnly = false; }
    }

    private void markDirty(Rectangle r) {
        synchronized (lock) { dirty = dirty == null ? new Rectangle(r) : dirty.union(r); }
    }

    /** Все запросы перерисовки потомков сводятся к корню и помечают область кэша грязной. */
    private final class Manager extends RepaintManager {
        @Override public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
            if (w <= 0 || h <= 0) return;
            LayeredRoot root = LayeredRoot.this;
            if (c == root) {
                if (!backdropOnly) markDirty(new Rectangle(x, y, Math.min(w, getWidth()), Math.min(h, getHeight())));
                super.addDirtyRegion(c, x, y, w, h);
                return;
            }
            if (SwingUtilities.isDescendingFrom(c, root)) {
                Rectangle r = new Rectangle(x, y, Math.min(w, c.getWidth() + 4), Math.min(h, c.getHeight() + 4));
                r = SwingUtilities.convertRectangle(c, r, root).intersection(new Rectangle(0, 0, root.getWidth(), root.getHeight()));
                if (r.isEmpty()) return;
                markDirty(r);
                super.addDirtyRegion(root, r.x, r.y, r.width, r.height);
                return;
            }
            super.addDirtyRegion(c, x, y, w, h);
        }
    }

    private double scale() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        return gc != null ? gc.getDefaultTransform().getScaleX() : 1.0;
    }

    /** Перерисовывает в кэше только грязную область интерфейса. */
    private void updateCache() {
        int w = getWidth(), h = getHeight();
        double sc = scale();
        int cw = (int) Math.ceil(w * sc), ch = (int) Math.ceil(h * sc);
        Rectangle d;
        synchronized (lock) { d = dirty; dirty = null; }
        if (cache == null || cache.getWidth() != cw || cache.getHeight() != ch || cacheScale != sc) {
            cache = new BufferedImage(Math.max(1, cw), Math.max(1, ch), BufferedImage.TYPE_INT_ARGB_PRE);
            cacheScale = sc;
            d = new Rectangle(0, 0, w, h);
        }
        if (d == null) return;
        d.grow(2, 2);
        d = d.intersection(new Rectangle(0, 0, w, h));
        if (d.isEmpty()) return;
        Graphics2D cg = cache.createGraphics();
        cg.scale(sc, sc);
        cg.setComposite(AlphaComposite.Clear);
        cg.fill(d);
        cg.setComposite(AlphaComposite.SrcOver);
        cg.clip(d);
        super.paintChildren(cg);
        cg.dispose();
    }

    @Override public void paint(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth(), h = getHeight();
        if (winAlpha < 1f) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, winAlpha)));
        }
        backdrop.paint(g, w, h);
        if (cacheEnabled) {
            updateCache();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(cache, 0, 0, w, h, null);
        } else {
            super.paintChildren(g);
        }
        Ui.aa(g);
        g.setColor(Ui.BORDER);
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 18, 18));
        g.dispose();
    }
}
