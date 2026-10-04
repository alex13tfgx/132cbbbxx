package guardlisten;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/** Фоновые частицы лаунчера: обычные, весна, осень, зима. */
final class Particles {
    enum Kind {
        NORMAL("normal", "Обычные"), SPRING("spring", "Весна"), AUTUMN("autumn", "Осень"), WINTER("winter", "Зима"), OFF("off", "Выкл");
        final String id, title;
        Kind(String id, String title) { this.id = id; this.title = title; }
        static Kind of(String id) {
            for (Kind k : values()) if (k.id.equals(id)) return k;
            return NORMAL;
        }
    }

    private static final class P {
        float x, y, vx, vy, size, rot, vr, phase, sway, alpha;
        Color color;
    }

    private static final Color[] AUTUMN = {
            new Color(0xE0731F), new Color(0xC2411A), new Color(0xE8A91E), new Color(0x9C5A1C), new Color(0xB5301B)};
    private static final Color[] SPRING = {
            new Color(0xF8B6CB), new Color(0xFFD3E0), new Color(0xF48FB1), new Color(0xFFFFFF), new Color(0xFBC9D7)};

    private final Random rnd = new Random();
    private final List<P> list = new ArrayList<>();
    private Kind kind = Kind.NORMAL;
    private int w, h;
    private long last;

    Kind kind() { return kind; }

    void setKind(Kind k) {
        kind = k;
        list.clear();
        w = 0;
    }

    private void regen() {
        list.clear();
        int n;
        switch (kind) {
            case WINTER: n = 80; break;
            case AUTUMN: n = 34; break;
            case SPRING: n = 42; break;
            case NORMAL: n = 46; break;
            default: n = 0;
        }
        for (int i = 0; i < n; i++) {
            P p = new P();
            spawn(p, true);
            list.add(p);
        }
    }

    private void spawn(P p, boolean anywhere) {
        p.x = rnd.nextFloat() * w;
        p.y = anywhere ? rnd.nextFloat() * h : -20 - rnd.nextFloat() * 40;
        p.phase = rnd.nextFloat() * 6.28f;
        p.rot = rnd.nextFloat() * 360f;
        switch (kind) {
            case WINTER:
                p.size = 1.6f + rnd.nextFloat() * 3.4f;
                p.vy = 22 + p.size * 9 + rnd.nextFloat() * 18;
                p.vx = -6 + rnd.nextFloat() * 12;
                p.sway = 10 + rnd.nextFloat() * 16;
                p.alpha = 0.45f + rnd.nextFloat() * 0.5f;
                p.color = Color.WHITE;
                break;
            case AUTUMN:
                p.size = 6 + rnd.nextFloat() * 5;
                p.vy = 28 + rnd.nextFloat() * 36;
                p.vx = 4 + rnd.nextFloat() * 20;
                p.sway = 22 + rnd.nextFloat() * 24;
                p.vr = -90 + rnd.nextFloat() * 180;
                p.alpha = 0.75f;
                p.color = AUTUMN[rnd.nextInt(AUTUMN.length)];
                break;
            case SPRING:
                p.size = 4 + rnd.nextFloat() * 4;
                p.vy = 20 + rnd.nextFloat() * 26;
                p.vx = 8 + rnd.nextFloat() * 22;
                p.sway = 18 + rnd.nextFloat() * 22;
                p.vr = -120 + rnd.nextFloat() * 240;
                p.alpha = 0.7f;
                p.color = SPRING[rnd.nextInt(SPRING.length)];
                break;
            default: // NORMAL: мягкие огоньки, плывущие вверх
                p.size = 1.2f + rnd.nextFloat() * 2.2f;
                p.vy = -(6 + rnd.nextFloat() * 14);
                p.vx = -4 + rnd.nextFloat() * 8;
                p.sway = 6 + rnd.nextFloat() * 10;
                p.alpha = 0.25f + rnd.nextFloat() * 0.45f;
                p.color = Color.WHITE;
                if (!anywhere) p.y = h + 10 + rnd.nextFloat() * 30;
        }
    }

    boolean active() { return kind != Kind.OFF; }

    /** Двигает частицы по реальному прошедшему времени. */
    void step(float dt) {
        for (P p : list) {
            p.phase += dt * 1.6f;
            p.y += p.vy * dt;
            p.x += (p.vx + (float) Math.sin(p.phase) * p.sway) * dt;
            p.rot += p.vr * dt;
            boolean out = kind == Kind.NORMAL ? p.y < -20 : p.y > h + 20;
            if (out || p.x > w + 30 || p.x < -30) {
                spawn(p, false);
                if (p.x < 0 || p.x > w) p.x = rnd.nextFloat() * w;
            }
        }
    }

    void resetClock() { last = 0; }

    void paint(Graphics2D g0, int width, int height) {
        if (kind == Kind.OFF) return;
        if (width != w || height != h || list.isEmpty()) { w = width; h = height; regen(); }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform base = g.getTransform();
        Composite baseComp = g.getComposite();
        float baseA = baseComp instanceof AlphaComposite ? ((AlphaComposite) baseComp).getAlpha() : 1f;
        for (P p : list) {
            float a = p.alpha;
            if (kind == Kind.NORMAL) a *= 0.6f + 0.4f * (float) Math.sin(p.phase * 1.3f);
            a = Math.max(0f, Math.min(1f, a)) * baseA;
            if (a <= 0.01f) continue;
            g.setComposite(AlphaComposite.SrcOver.derive(a));
            switch (kind) {
                case WINTER:
                case NORMAL: {
                    // мягкая точка с ореолом: картинка в 3 раза больше ядра
                    float r = p.size * 1.6f;
                    g.drawImage(DOT, Math.round(p.x - r), Math.round(p.y - r), Math.round(r * 2), Math.round(r * 2), null);
                    break;
                }
                case AUTUMN: {
                    g.translate(p.x, p.y);
                    g.rotate(Math.toRadians(p.rot));
                    float sz = p.size;
                    g.drawImage(sprite(LEAF, p.color), Math.round(-sz), Math.round(-sz), Math.round(sz * 2), Math.round(sz * 2), null);
                    g.setTransform(base);
                    break;
                }
                default: { // SPRING: лепестки
                    g.translate(p.x, p.y);
                    g.rotate(Math.toRadians(p.rot));
                    float squash = 0.55f + 0.45f * (float) Math.abs(Math.sin(Math.toRadians(p.rot)));
                    g.scale(1, squash);
                    float sz = p.size;
                    g.drawImage(sprite(PETAL, p.color), Math.round(-sz), Math.round(-sz), Math.round(sz * 2), Math.round(sz * 2), null);
                    g.setTransform(base);
                }
            }
        }
        g.dispose();
    }

    // ---------- спрайты (рисуются один раз, дальше только копируются) ----------
    private static final int SPR = 48;
    private static final int LEAF = 1, PETAL = 2;
    private static final java.awt.image.BufferedImage DOT = makeDot();
    private static final Map<Integer, java.awt.image.BufferedImage> SPRITES = new HashMap<>();

    private static java.awt.image.BufferedImage makeDot() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(SPR, SPR, java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = img.createGraphics();
        Ui.aa(g);
        float c = SPR / 2f;
        g.setPaint(new RadialGradientPaint(c, c, c, new float[]{0f, 0.28f, 0.4f, 1f},
                new Color[]{new Color(255, 255, 255, 255), new Color(255, 255, 255, 235), new Color(255, 255, 255, 70), new Color(255, 255, 255, 0)}));
        g.fillRect(0, 0, SPR, SPR);
        g.dispose();
        return img;
    }

    private static java.awt.image.BufferedImage sprite(int shape, Color color) {
        return SPRITES.computeIfAbsent(shape * 0x1000000 + (color.getRGB() & 0xFFFFFF), k -> {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(SPR, SPR, java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = img.createGraphics();
            Ui.aa(g);
            float s = SPR / 2f - 1;
            g.translate(SPR / 2f, SPR / 2f);
            Path2D path = new Path2D.Float();
            if (shape == LEAF) {
                path.moveTo(0, -s);
                path.curveTo(s * 0.9f, -s * 0.5f, s * 0.8f, s * 0.5f, 0, s);
                path.curveTo(-s * 0.8f, s * 0.5f, -s * 0.9f, -s * 0.5f, 0, -s);
            } else {
                path.moveTo(0, -s);
                path.curveTo(s * 0.9f, -s * 0.6f, s * 0.7f, s * 0.7f, 0, s * 0.8f);
                path.curveTo(-s * 0.7f, s * 0.7f, -s * 0.9f, -s * 0.6f, 0, -s);
            }
            g.setColor(color);
            g.fill(path);
            if (shape == LEAF) {
                g.setColor(new Color(0, 0, 0, 70));
                g.setStroke(new BasicStroke(SPR / 28f));
                g.draw(new Line2D.Float(0, -s, 0, s));
            }
            g.dispose();
            return img;
        });
    }
}
