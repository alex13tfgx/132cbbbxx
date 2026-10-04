package guardlisten;

import java.awt.*;
import java.awt.geom.*;

/** Набор нарисованных аватарок-зверей (векторные, чёткие при любом размере). */
final class Avatars {
    /** Аватарки-картинки (res/avatars/pN.png). */
    static final String[] IDS = {"p1", "p2", "p3", "p4", "p5"};

    private static final java.util.Map<String, java.awt.image.BufferedImage> PHOTOS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, java.awt.image.BufferedImage> ROUND = new java.util.concurrent.ConcurrentHashMap<>();

    static java.awt.image.BufferedImage photo(String id) {
        if (id == null || !id.matches("p[0-9]+")) return null;
        return PHOTOS.computeIfAbsent(id, k -> {
            try { return javax.imageio.ImageIO.read(Avatars.class.getResource("/avatars/" + k + ".png")); }
            catch (Exception e) { return new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB); }
        });
    }

    private Avatars() { }

    private static Color c(int rgb) { return new Color(rgb); }

    private static void e(Graphics2D g, int rgb, double cx, double cy, double w, double h) {
        g.setColor(c(rgb));
        g.fill(new Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h));
    }

    private static void poly(Graphics2D g, int rgb, double... p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.closePath();
        g.setColor(c(rgb));
        g.fill(path);
    }

    private static void line(Graphics2D g, int rgb, double w, double x1, double y1, double x2, double y2) {
        g.setColor(c(rgb));
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void eye(Graphics2D g, double cx, double cy, double r) {
        e(g, 0x15151A, cx, cy, r * 2, r * 2.2);
        e(g, 0xFFFFFF, cx - r * 0.3, cy - r * 0.4, r * 0.7, r * 0.7);
    }

    static int bgOf(String id) {
        switch (id) {
            case "cat": return 0x2B3445;
            case "dog": return 0x2B3F36;
            case "fox": return 0x44302B;
            case "panda": return 0x2B3F3F;
            case "frog": return 0x24402B;
            case "bear": return 0x40362B;
            case "rabbit": return 0x3A3045;
            default: return 0x32334A;
        }
    }

    /** Рисует аватарку в квадрате size x size в точке (0,0) внутри круга. */
    static void draw(Graphics2D g0, String id, int size) {
        java.awt.image.BufferedImage ph = photo(id);
        if (ph != null) {
            java.awt.image.BufferedImage r = ROUND.computeIfAbsent(id + "@" + size, k -> Ui.roundIcon(ph, size, size / 2));
            g0.drawImage(r, 0, 0, size, size, null);
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        Ui.aa(g);
        g.setClip(new Ellipse2D.Double(0, 0, size, size));
        g.scale(size / 100.0, size / 100.0);
        g.setColor(c(bgOf(id)));
        g.fillRect(0, 0, 100, 100);
        switch (id) {
            case "cat":
                poly(g, 0xE8A04C, 22, 40, 27, 12, 46, 28);
                poly(g, 0xE8A04C, 78, 40, 73, 12, 54, 28);
                poly(g, 0xF4B8B0, 28, 35, 30, 20, 40, 29);
                poly(g, 0xF4B8B0, 72, 35, 70, 20, 60, 29);
                e(g, 0xE8A04C, 50, 58, 64, 54);
                e(g, 0xF6D9A8, 50, 68, 30, 20);
                line(g, 0xC47A2C, 2.4, 50, 32, 50, 40);
                line(g, 0xC47A2C, 2.4, 41, 33, 43, 40);
                line(g, 0xC47A2C, 2.4, 59, 33, 57, 40);
                eye(g, 37, 54, 4.6);
                eye(g, 63, 54, 4.6);
                poly(g, 0xE88A8A, 46.5, 62, 53.5, 62, 50, 66);
                line(g, 0x6A3A1A, 1.6, 50, 66, 50, 69);
                line(g, 0x6A3A1A, 1.6, 50, 69, 45, 72);
                line(g, 0x6A3A1A, 1.6, 50, 69, 55, 72);
                line(g, 0xFFFFFF, 1.2, 30, 64, 14, 61);
                line(g, 0xFFFFFF, 1.2, 30, 68, 14, 70);
                line(g, 0xFFFFFF, 1.2, 70, 64, 86, 61);
                line(g, 0xFFFFFF, 1.2, 70, 68, 86, 70);
                break;
            case "dog":
                e(g, 0x6B4423, 20, 52, 22, 44);
                e(g, 0x6B4423, 80, 52, 22, 44);
                e(g, 0xD2A56B, 50, 56, 60, 58);
                e(g, 0xF2D9B0, 50, 68, 36, 28);
                e(g, 0x6B4423, 38, 44, 14, 12);
                e(g, 0x15151A, 50, 61, 15, 10);
                e(g, 0xFFFFFF, 47, 59, 4, 3);
                eye(g, 38, 49, 4.2);
                eye(g, 62, 49, 4.2);
                line(g, 0x4A2E16, 1.8, 50, 66, 50, 72);
                line(g, 0x4A2E16, 1.8, 50, 72, 43, 74);
                line(g, 0x4A2E16, 1.8, 50, 72, 57, 74);
                e(g, 0xE88A8A, 50, 79, 10, 12);
                break;
            case "fox":
                poly(g, 0xE5732B, 18, 46, 22, 10, 46, 30);
                poly(g, 0xE5732B, 82, 46, 78, 10, 54, 30);
                poly(g, 0x3B2418, 24, 38, 25, 20, 38, 31);
                poly(g, 0x3B2418, 76, 38, 75, 20, 62, 31);
                e(g, 0xE5732B, 50, 56, 66, 52);
                poly(g, 0xFFF3E6, 50, 90, 16, 58, 34, 60, 50, 66, 66, 60, 84, 58);
                e(g, 0x15151A, 50, 79, 8, 6);
                poly(g, 0x15151A, 33, 52, 44, 53, 42, 57);
                poly(g, 0x15151A, 67, 52, 56, 53, 58, 57);
                break;
            case "panda":
                e(g, 0x15151A, 24, 28, 20, 20);
                e(g, 0x15151A, 76, 28, 20, 20);
                e(g, 0xF4F4F4, 50, 58, 68, 60);
                e(g, 0x15151A, 35, 52, 17, 22);
                e(g, 0x15151A, 65, 52, 17, 22);
                e(g, 0xFFFFFF, 35, 52, 6, 7);
                e(g, 0xFFFFFF, 65, 52, 6, 7);
                e(g, 0x15151A, 35, 53, 3, 4);
                e(g, 0x15151A, 65, 53, 3, 4);
                e(g, 0x15151A, 50, 65, 11, 8);
                line(g, 0x15151A, 1.8, 50, 69, 50, 73);
                line(g, 0x15151A, 1.8, 50, 73, 44, 75);
                line(g, 0x15151A, 1.8, 50, 73, 56, 75);
                break;
            case "frog":
                e(g, 0x6BBF59, 50, 62, 78, 54);
                e(g, 0x6BBF59, 30, 38, 26, 26);
                e(g, 0x6BBF59, 70, 38, 26, 26);
                e(g, 0xFFFFFF, 30, 38, 17, 17);
                e(g, 0xFFFFFF, 70, 38, 17, 17);
                e(g, 0x15151A, 31, 39, 9, 10);
                e(g, 0x15151A, 69, 39, 9, 10);
                e(g, 0xFFFFFF, 29, 36, 3, 3);
                e(g, 0xFFFFFF, 67, 36, 3, 3);
                e(g, 0xA6E08F, 50, 72, 56, 22);
                g.setColor(c(0x1F3D1A));
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(30, 56, 40, 24, 200, 140, Arc2D.OPEN));
                e(g, 0x1F3D1A, 45, 58, 2.5, 2.5);
                e(g, 0x1F3D1A, 55, 58, 2.5, 2.5);
                e(g, 0xF08080, 24, 64, 9, 6);
                e(g, 0xF08080, 76, 64, 9, 6);
                break;
            case "bear":
                e(g, 0x8B5E3C, 24, 30, 22, 22);
                e(g, 0x8B5E3C, 76, 30, 22, 22);
                e(g, 0xD2A27C, 24, 30, 11, 11);
                e(g, 0xD2A27C, 76, 30, 11, 11);
                e(g, 0x8B5E3C, 50, 58, 68, 62);
                e(g, 0xD9B48C, 50, 68, 32, 26);
                e(g, 0x15151A, 50, 62, 12, 8);
                eye(g, 36, 50, 4.2);
                eye(g, 64, 50, 4.2);
                line(g, 0x3B2418, 1.8, 50, 66, 50, 71);
                line(g, 0x3B2418, 1.8, 50, 71, 45, 74);
                line(g, 0x3B2418, 1.8, 50, 71, 55, 74);
                break;
            case "rabbit":
                e(g, 0xF4F4F4, 36, 26, 17, 50);
                e(g, 0xF4F4F4, 64, 26, 17, 50);
                e(g, 0xF4B8C8, 36, 28, 8, 34);
                e(g, 0xF4B8C8, 64, 28, 8, 34);
                e(g, 0xF4F4F4, 50, 66, 62, 54);
                eye(g, 38, 62, 4.2);
                eye(g, 62, 62, 4.2);
                e(g, 0xF08FA8, 50, 70, 8, 6);
                line(g, 0x9A6A7A, 1.6, 50, 73, 50, 77);
                line(g, 0x9A6A7A, 1.6, 50, 77, 45, 79);
                line(g, 0x9A6A7A, 1.6, 50, 77, 55, 79);
                e(g, 0xF9C7D2, 29, 72, 9, 6);
                e(g, 0xF9C7D2, 71, 72, 9, 6);
                break;
            default: // owl
                poly(g, 0x9C6B3C, 20, 38, 24, 12, 42, 28);
                poly(g, 0x9C6B3C, 80, 38, 76, 12, 58, 28);
                e(g, 0x9C6B3C, 50, 58, 70, 66);
                e(g, 0xF2E4C9, 36, 52, 28, 28);
                e(g, 0xF2E4C9, 64, 52, 28, 28);
                e(g, 0x15151A, 36, 52, 14, 14);
                e(g, 0x15151A, 64, 52, 14, 14);
                e(g, 0xFFFFFF, 33, 49, 5, 5);
                e(g, 0xFFFFFF, 61, 49, 5, 5);
                poly(g, 0xF0A030, 45, 62, 55, 62, 50, 74);
                for (int i = 0; i < 3; i++) {
                    g.setColor(c(0x7A5028));
                    g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Arc2D.Double(38 + i * 8, 78, 8, 8, 200, 140, Arc2D.OPEN));
                }
                break;
        }
        g.dispose();
    }
}
