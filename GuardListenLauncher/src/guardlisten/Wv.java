package guardlisten;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Компоненты оформления WiosVisuals: заголовок окна, карточки версий, боковая панель, таблицы. */
final class Wv {
    static final Color WIN = new Color(0x000000);
    static final Color PANEL = new Color(14, 16, 19, 238);
    static final Color LINE = new Color(255, 255, 255, 150);
    static final Color TEXT = new Color(0xF3F5F7);
    static final Color MUTED = new Color(0x8B929B);
    static final Color DIM = new Color(0x5F666E);
    static final Color GREEN = new Color(0x3DDC84);
    static final Color RED = new Color(0xFF6B6B);
    static final Color BLUE = new Color(0xE8E8E8);
    static final Color BTN = new Color(0xE6E6E6);
    static final Color BTN_TEXT = new Color(0x000000);

    private Wv() { }

    // ---------- шрифты ----------
    private static final Map<String, Font> FONTS = new ConcurrentHashMap<>();
    private static java.util.Set<String> families;

    private static boolean has(String fam) {
        if (families == null)
            families = new java.util.HashSet<>(java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        return families.contains(fam);
    }

    private static Font fam(String fam, int fallbackStyle, float size, float tracking) {
        return FONTS.computeIfAbsent(fam + "|" + size + "|" + tracking, k -> {
            Font f = has(fam) ? new Font(fam, Font.PLAIN, 1).deriveFont(size) : Ui.font(fallbackStyle, size);
            return tracking == 0f ? f : f.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
        });
    }

    /** Самое жирное начертание (номера версий, тариф). */
    static Font heavy(float size) { return fam("Segoe UI Black", Font.BOLD, size, 0f); }

    static Font semi(float size) { return fam("Segoe UI Semibold", Font.BOLD, size, 0f); }

    static Font bold(float size) { return Ui.font(Font.BOLD, size); }

    static Font plain(float size) { return Ui.font(Font.PLAIN, size); }

    /** Жирный с разрядкой — для подписей капсом («MINECRAFT», «КЛИЕНТ»). */
    static Font caps(float size, float tracking) { return fam("Segoe UI Black", Font.BOLD, size, tracking); }

    static float baseline(Graphics2D g, float top, float h) {
        FontMetrics fm = g.getFontMetrics();
        return top + (h + fm.getAscent() - fm.getDescent()) / 2f;
    }

    static RoundRectangle2D rr(float x, float y, float w, float h, float r) { return new RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2); }

    static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))); }

    /** Клик + наведение для своих компонентов. */
    static void clickable(JComponent c, Ui.Hover hv, Runnable r) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hv.set(true); }
            public void mouseExited(MouseEvent e) { hv.set(false); }
            public void mouseReleased(MouseEvent e) { if (c.contains(e.getPoint()) && c.isEnabled() && r != null) r.run(); }
        });
    }

    // ---------- иконка ----------
    /**
     * Делает из светлой иконки клиента тёмный вариант «стекло в ночи»: фон уходит в глубокий синий,
     * светлые детали логотипа остаются сияющими. mono — серый вариант для недоступных версий.
     */
    static BufferedImage deepIcon(BufferedImage src, int size, int radius, boolean mono) {
        int n = src.getWidth(), m = src.getHeight();
        BufferedImage blur = src;
        while (blur.getWidth() > 16) { // сильное размытие = «локальная яркость фона»
            BufferedImage h = new BufferedImage(Math.max(1, blur.getWidth() / 2), Math.max(1, blur.getHeight() / 2), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = h.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(blur, 0, 0, h.getWidth(), h.getHeight(), null);
            g.dispose();
            blur = h;
        }
        BufferedImage big = new BufferedImage(n, m, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = big.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        bg.drawImage(blur, 0, 0, n, m, null);
        bg.dispose();
        Color c0 = mono ? new Color(0x14181D) : new Color(0x061A33);
        Color c1 = mono ? new Color(0x5A636D) : new Color(0x2C7BC4);
        Color c2 = mono ? new Color(0xE6EAEE) : new Color(0xF0F8FF);
        BufferedImage out = new BufferedImage(n, m, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < m; y++) {
            for (int x = 0; x < n; x++) {
                int p = src.getRGB(x, y), q = big.getRGB(x, y);
                float l = lum(p), lb = lum(q);
                float d = Math.max(0f, l - lb + 0.015f) * 4.2f;
                // мягкое свечение вокруг центра, как подсветка за стеклом
                float dx = (x - n * 0.5f) / n, dy = (y - m * 0.52f) / m;
                float glow = Math.max(0f, 0.32f - (dx * dx + dy * dy) * 1.6f);
                float t = Math.min(1f, d + glow);
                Color c = t < 0.5f ? Ui.lerp(c0, c1, t * 2f) : Ui.lerp(c1, c2, (t - 0.5f) * 2f);
                out.setRGB(x, y, (0xFF << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue());
            }
        }
        return Ui.roundIcon(out, size, radius);
    }

    private static float lum(int p) {
        return (0.2126f * ((p >> 16) & 255) + 0.7152f * ((p >> 8) & 255) + 0.0722f * (p & 255)) / 255f;
    }

    // ---------- простые иконки-рисунки ----------
    enum Glyph { DOT, DIAMOND, GRID, GEAR, EXIT, DOWNLOAD, PLAY, CHECK, CROSS, LOCK, ARROW, INFO, FOLDER, PLUS }

    static void glyph(Graphics2D g, Glyph k, float cx, float cy, float s, Color c) {
        g.setColor(c);
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(Math.max(1.6f, s * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float h = s / 2f;
        switch (k) {
            case DOT:
                g.fill(new Ellipse2D.Float(cx - h, cy - h, s, s));
                break;
            case DIAMOND: {
                Path2D p = new Path2D.Float();
                float r = h * 0.95f;
                p.moveTo(cx, cy - r); p.lineTo(cx + r, cy); p.lineTo(cx, cy + r); p.lineTo(cx - r, cy); p.closePath();
                g.fill(p);
                break;
            }
            case GRID: {
                float cell = s / 3f, gap = Math.max(1.2f, s * 0.09f);
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        g.fill(new Rectangle2D.Float(cx - h + i * cell + gap / 2, cy - h + j * cell + gap / 2, cell - gap, cell - gap));
                break;
            }
            case GEAR: {
                Area a = new Area(new Ellipse2D.Float(cx - h * 0.72f, cy - h * 0.72f, h * 1.44f, h * 1.44f));
                for (int i = 0; i < 8; i++) {
                    Shape tooth = new Rectangle2D.Float(cx - h * 0.17f, cy - h, h * 0.34f, h * 0.5f);
                    a.add(new Area(AffineTransform.getRotateInstance(Math.PI / 4 * i, cx, cy).createTransformedShape(tooth)));
                }
                a.subtract(new Area(new Ellipse2D.Float(cx - h * 0.32f, cy - h * 0.32f, h * 0.64f, h * 0.64f)));
                g.fill(a);
                break;
            }
            case EXIT: {
                Path2D p = new Path2D.Float();
                p.moveTo(cx - h, cy + h * 0.55f);
                p.curveTo(cx - h, cy - h * 0.35f, cx - h * 0.2f, cy - h * 0.35f, cx + h * 0.9f, cy - h * 0.35f);
                g.draw(p);
                g.draw(new Line2D.Float(cx + h * 0.9f, cy - h * 0.35f, cx + h * 0.35f, cy - h * 0.9f));
                g.draw(new Line2D.Float(cx + h * 0.9f, cy - h * 0.35f, cx + h * 0.35f, cy + h * 0.2f));
                break;
            }
            case DOWNLOAD:
                g.draw(new Line2D.Float(cx, cy - h, cx, cy + h));
                g.draw(new Line2D.Float(cx, cy + h, cx - h * 0.6f, cy + h * 0.4f));
                g.draw(new Line2D.Float(cx, cy + h, cx + h * 0.6f, cy + h * 0.4f));
                break;
            case PLAY: {
                Path2D p = new Path2D.Float();
                p.moveTo(cx - h * 0.6f, cy - h); p.lineTo(cx + h * 0.9f, cy); p.lineTo(cx - h * 0.6f, cy + h); p.closePath();
                g.fill(p);
                break;
            }
            case CHECK: {
                Path2D p = new Path2D.Float();
                p.moveTo(cx - h, cy + h * 0.05f); p.lineTo(cx - h * 0.25f, cy + h * 0.8f); p.lineTo(cx + h, cy - h * 0.85f);
                g.setStroke(new BasicStroke(Math.max(2.2f, s * 0.15f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(p);
                break;
            }
            case CROSS:
                g.draw(new Line2D.Float(cx - h * 0.8f, cy - h * 0.8f, cx + h * 0.8f, cy + h * 0.8f));
                g.draw(new Line2D.Float(cx - h * 0.8f, cy + h * 0.8f, cx + h * 0.8f, cy - h * 0.8f));
                break;
            case LOCK:
                g.fill(rr(cx - h * 0.8f, cy - h * 0.1f, h * 1.6f, h * 1.1f, h * 0.22f));
                g.draw(new Arc2D.Float(cx - h * 0.5f, cy - h * 0.95f, h, h * 1.4f, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Float(cx - h * 0.5f, cy - h * 0.25f, cx - h * 0.5f, cy - h * 0.1f));
                g.draw(new Line2D.Float(cx + h * 0.5f, cy - h * 0.25f, cx + h * 0.5f, cy - h * 0.1f));
                break;
            case ARROW:
                g.draw(new Line2D.Float(cx - h, cy, cx + h, cy));
                g.draw(new Line2D.Float(cx + h, cy, cx + h * 0.35f, cy - h * 0.65f));
                g.draw(new Line2D.Float(cx + h, cy, cx + h * 0.35f, cy + h * 0.65f));
                break;
            case INFO:
                g.fill(new Ellipse2D.Float(cx - h, cy - h, s, s));
                g.setColor(new Color(0x0B0F14));
                g.setStroke(new BasicStroke(Math.max(1.6f, s * 0.13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(cx, cy - h * 0.05f, cx, cy + h * 0.5f));
                g.fill(new Ellipse2D.Float(cx - s * 0.08f, cy - h * 0.52f, s * 0.16f, s * 0.16f));
                break;
            case FOLDER: {
                Path2D p = new Path2D.Float();
                p.moveTo(cx - h, cy - h * 0.7f); p.lineTo(cx - h * 0.2f, cy - h * 0.7f); p.lineTo(cx, cy - h * 0.4f);
                p.lineTo(cx + h, cy - h * 0.4f); p.lineTo(cx + h, cy + h * 0.75f); p.lineTo(cx - h, cy + h * 0.75f); p.closePath();
                g.draw(p);
                break;
            }
            case PLUS:
                g.draw(new Line2D.Float(cx - h * 0.8f, cy, cx + h * 0.8f, cy));
                g.draw(new Line2D.Float(cx, cy - h * 0.8f, cx, cy + h * 0.8f));
                break;
        }
        g.setStroke(old);
    }

    // ---------- контейнеры ----------
    /** Скруглённая полупрозрачная панель с тонкой рамкой. */
    static class Panel extends JPanel {
        final float radius;

        Panel(float radius) {
            super(null);
            this.radius = radius;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(0, 0, 0, 245));
            g.fill(rr(0, 0, w, h, radius));
            g.setColor(LINE);
            g.draw(rr(0.5f, 0.5f, w - 1, h - 1, radius));
            g.dispose();
        }
    }

    /** Заголовок окна: иконка, название, свернуть/закрыть. */
    static final class TitleBar extends JPanel {
        final Ui.IconBtn min, close;
        private final Image logo;
        private final String title;

        TitleBar(Image logo, String title, Runnable onMin, Runnable onClose) {
            super(null);
            this.logo = logo; this.title = title;
            setOpaque(false);
            min = new Ui.IconBtn(Ui.Icon.MIN, onMin);
            close = new Ui.IconBtn(Ui.Icon.CLOSE, onClose);
            add(min);
            add(close);
        }

        @Override public void doLayout() {
            int w = getWidth(), h = getHeight();
            close.setBounds(w - 46, (h - 32) / 2, 32, 32);
            min.setBounds(w - 84, (h - 32) / 2, 32, 32);
        }

        @Override public void setBounds(int x, int y, int w, int h) { super.setBounds(x, y, w, h); doLayout(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int h = getHeight();
            if (logo != null) g.drawImage(logo, 18, (h - 28) / 2, 28, 28, null);
            g.setFont(bold(15f));
            g.setColor(TEXT);
            g.drawString(title, 56, baseline(g, 0, h));
            g.setColor(new Color(255, 255, 255, 18));
            g.fillRect(0, h - 1, getWidth(), 1);
            g.dispose();
        }
    }

    // ---------- экран выбора версии ----------
    static final class VersionCard extends JComponent {
        final Client client;
        private final Image icon;
        private final Ui.Hover hv = new Ui.Hover(this);

        VersionCard(Client client, Image icon, Runnable r) {
            this.client = client; this.icon = icon;
            hv.speed = 11f;
            if (client.available) clickable(this, hv, r);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            boolean av = client.available;
            float v = hv.v;
            int pad = 8;
            float w = getWidth() - pad * 2, h = getHeight() - pad * 2;
            g.translate(pad, pad - 4 * v);
            // мягкая тень/свечение снаружи при наведении
            for (int i = 6; i > 0; i--) {
                g.setColor(new Color(255, 255, 255, (int) (5 * v)));
                g.fill(rr(-i, -i + 2, w + 2 * i, h + 2 * i, 24 + i));
            }
            Shape card = rr(0, 0, w, h, 24);
            g.setColor(Color.BLACK);
            g.fill(card);
            Graphics2D gl = (Graphics2D) g.create();
            gl.clip(card);
            float gx = w * 0.48f, gy = h * 0.02f, gr = w * 0.62f;
            int ga = av ? (int) (90 + 50 * v) : 48;
            gl.setPaint(new RadialGradientPaint(gx, gy, gr, new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, av ? ga / 4 : ga / 6), new Color(255, 255, 255, 0)}));
            gl.fill(new Rectangle2D.Float(0, 0, w, h));
            gl.dispose();
            g.setColor(av ? Ui.lerp(new Color(255, 255, 255, 150), Color.WHITE, v) : new Color(255, 255, 255, 70));
            g.draw(rr(0.5f, 0.5f, w - 1, h - 1, 24));

            int ix = 30, iy = 30, is = 92;
            if (av) {
                g.setPaint(new RadialGradientPaint(ix + is / 2f, iy + is / 2f, is * 0.85f, new float[]{0f, 1f},
                        new Color[]{new Color(255, 255, 255, (int) (22 + 20 * v)), new Color(255, 255, 255, 0)}));
                g.fill(new Ellipse2D.Float(ix - is * 0.35f, iy - is * 0.35f, is * 1.7f, is * 1.7f));
            }
            if (icon != null) g.drawImage(icon, ix, iy, is, is, null);
            g.setColor(new Color(255, 255, 255, av ? 40 : 22));
            g.draw(rr(ix + 0.5f, iy + 0.5f, is - 1, is - 1, 20));

            g.setFont(caps(13f, 0.14f));
            g.setColor(av ? BLUE : new Color(0x7A7A7A));
            g.drawString("MINECRAFT", ix, iy + is + 42);
            g.setFont(heavy(42f));
            g.setColor(av ? TEXT : new Color(0xB4BAC2));
            g.drawString(client.mc, ix - 2, iy + is + 88);
            g.setFont(plain(15f));
            g.setColor(av ? new Color(0xA3AAB3) : MUTED);
            g.drawString(client.subtitle, ix, iy + is + 118);

            float by = h - 36;
            g.setFont(semi(15f));
            if (av) {
                g.setColor(TEXT);
                g.drawString("Выбрать версию", ix, by + 5);
                glyph(g, Glyph.ARROW, w - 42 + 4 * v, by, 18, BLUE);
            } else {
                g.setColor(MUTED);
                g.drawString("Пока недоступна", ix, by + 5);
                glyph(g, Glyph.LOCK, w - 42, by, 18, new Color(0x9A9A9A));
                // плашка «Скоро»
                float pw = 86, ph = 32, px = w - pw - 26, py = 26;
                g.setColor(new Color(255, 255, 255, 16));
                g.fill(rr(px, py, pw, ph, ph / 2));
                g.setColor(new Color(255, 255, 255, 26));
                g.draw(rr(px + 0.5f, py + 0.5f, pw - 1, ph - 1, ph / 2));
                glyph(g, Glyph.LOCK, px + 22, py + ph / 2, 11, MUTED);
                g.setFont(semi(13f));
                g.setColor(new Color(0xB4BAC2));
                g.drawString("Скоро", px + 34, baseline(g, py, ph));
            }
            g.dispose();
        }
    }

    /** Плашка-подсказка «i  текст». */
    static final class InfoPill extends JComponent {
        private final String text;

        InfoPill(String text) { this.text = text; }

        int prefWidth() { return getFontMetrics(plain(13.5f)).stringWidth(text) + 66; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(Color.BLACK);
            g.fill(rr(0, 0, w, h, h / 2f));
            g.setColor(new Color(255, 255, 255, 120));
            g.draw(rr(0.5f, 0.5f, w - 1, h - 1, h / 2f));
            glyph(g, Glyph.INFO, 26, h / 2f, 16, BLUE);
            g.setFont(plain(13.5f));
            g.setColor(new Color(0xB4BAC2));
            g.drawString(text, 46, baseline(g, 0, h));
            g.dispose();
        }
    }

    // ---------- боковая панель ----------
    /** Профиль сверху боковой панели (клик — открыть аккаунт). */
    static final class Profile extends JPanel {
        final Ui.Avatar avatar = new Ui.Avatar();
        String name = "", sub = "";
        private final Ui.Hover hv = new Ui.Hover(this);

        Profile(Runnable r) {
            super(null);
            setOpaque(false);
            add(avatar);
            clickable(this, hv, r);
        }

        @Override public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);
            avatar.setBounds(12, (h - 52) / 2, 52, 52);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            if (hv.v > 0.01f) {
                g.setColor(new Color(255, 255, 255, (int) (10 * hv.v)));
                g.fill(rr(0, 0, w, h, 14));
            }
            g.setFont(bold(16f));
            g.setColor(TEXT);
            g.drawString(name, 78, h / 2f - 3);
            g.setFont(plain(13f));
            g.setColor(MUTED);
            g.drawString(sub, 78, h / 2f + 17);
            g.setColor(new Color(0x9AA0A6));
            g.fill(new Ellipse2D.Float(w - 22, h / 2f - 3, 6, 6));
            g.dispose();
        }
    }

    /** Карточка подписки. */
    static final class SubCard extends JComponent {
        String plan = "LIFETIME", state = "Активна";
        float fill = 1f;

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(Color.BLACK);
            g.fill(rr(0, 0, w, h, 16));
            g.setColor(new Color(255, 255, 255, 140));
            g.draw(rr(0.5f, 0.5f, w - 1, h - 1, 16));
            g.setFont(plain(12.5f));
            g.setColor(new Color(0xB4BAC2));
            g.drawString("Подписка", 16, 30);
            g.setFont(bold(12.5f));
            g.setColor(GREEN);
            g.drawString(state, w - 16 - g.getFontMetrics().stringWidth(state), 30);
            g.setFont(heavy(20f));
            g.setColor(TEXT);
            g.drawString(plan, 16, 64);
            g.setColor(new Color(255, 255, 255, 30));
            g.fill(rr(16, 78, w - 32, 4, 2));
            g.setColor(Color.WHITE);
            g.fill(rr(16, 78, (w - 32) * fill, 4, 2));
            g.setFont(plain(11.5f));
            g.setColor(new Color(0xA3AAB3));
            g.drawString("Доступ к клиенту", 16, 106);
            String ext = "Продлить";
            g.drawString(ext, w - 16 - g.getFontMetrics().stringWidth(ext), 106);
            g.dispose();
        }
    }

    /** Пункт навигации с иконкой. */
    static final class Nav extends JComponent {
        final String text;
        final Glyph glyph;
        private final Ui.Hover hv = new Ui.Hover(this), sel = new Ui.Hover(this);

        Nav(String text, Glyph glyph, Runnable r) {
            this.text = text; this.glyph = glyph;
            clickable(this, hv, r);
        }

        void setSelected(boolean b) { sel.set(b); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            float s = sel.v, v = hv.v * (1f - s);
            if (v > 0.01f) {
                g.setColor(new Color(255, 255, 255, (int) (9 * v)));
                g.fill(rr(0, 0, w, h, 12));
            }
            if (s > 0.01f) {
                g.setColor(new Color(255, 255, 255, (int) (14 * s)));
                g.fill(rr(0, 0, w, h, 12));
                g.setColor(new Color(255, 255, 255, (int) (170 * s)));
                g.draw(rr(0.5f, 0.5f, w - 1, h - 1, 12));
            }
            Color ic = Ui.lerp(Ui.lerp(new Color(0x9A9A9A), TEXT, hv.v), Color.WHITE, s);
            glyph(g, glyph, 25, h / 2f, glyph == Glyph.DOT ? 15 : 14, ic);
            g.setFont(semi(14.5f));
            g.setColor(Ui.lerp(Ui.lerp(new Color(0xC9CED4), TEXT, hv.v), Color.WHITE, s));
            g.drawString(text, 48, baseline(g, 0, h));
            g.dispose();
        }
    }

    /** Кнопка с рамкой и иконкой («Выйти», «Открыть папку»). */
    static final class OutlineBtn extends JComponent {
        private final String text;
        private final Glyph glyph;
        private final Ui.Hover hv = new Ui.Hover(this);

        OutlineBtn(String text, Glyph glyph, Runnable r) {
            this.text = text; this.glyph = glyph;
            clickable(this, hv, r);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(255, 255, 255, (int) (10 * hv.v)));
            g.fill(rr(0, 0, w, h, 14));
            g.setColor(new Color(255, 255, 255, (int) (110 + 110 * hv.v)));
            g.draw(rr(0.5f, 0.5f, w - 1, h - 1, 14));
            g.setFont(semi(14f));
            int tw = g.getFontMetrics().stringWidth(text);
            float x = (w - tw - 28) / 2f;
            Color c = Ui.lerp(new Color(0xC9CED4), TEXT, hv.v);
            glyph(g, glyph, x + 9, h / 2f, 15, c);
            g.setColor(c);
            g.drawString(text, x + 28, baseline(g, 0, h));
            g.dispose();
        }
    }

    // ---------- главная ----------
    /** Таблица «подпись — значение» в две колонки. */
    static final class InfoGrid extends JComponent {
        final String[] labels, values;
        final Color[] colors;
        private final Runnable[] actions;
        private int hover = -1;

        InfoGrid(String... labels) {
            this.labels = labels;
            values = new String[labels.length];
            colors = new Color[labels.length];
            actions = new Runnable[labels.length];
            java.util.Arrays.fill(values, "—");
            MouseAdapter ma = new MouseAdapter() {
                public void mouseMoved(MouseEvent e) { setHover(cellAt(e.getX(), e.getY())); }
                public void mouseExited(MouseEvent e) { setHover(-1); }
                public void mouseReleased(MouseEvent e) {
                    int i = cellAt(e.getX(), e.getY());
                    if (i >= 0 && actions[i] != null) actions[i].run();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        /** Клик по ячейке (например, копирование UUID). */
        void onClick(int i, Runnable r) { actions[i] = r; }

        private int cellAt(int x, int y) {
            int rows = (labels.length + 1) / 2;
            int i = Math.min(rows - 1, y * rows / Math.max(1, getHeight())) * 2 + (x * 2 >= getWidth() ? 1 : 0);
            return i < labels.length ? i : -1;
        }

        private void setHover(int i) {
            if (i >= 0 && actions[i] == null) i = -1;
            if (i == hover) return;
            hover = i;
            setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        void set(int i, String v, Color c) { values[i] = v; colors[i] = c; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            int rows = (labels.length + 1) / 2;
            float ch = (float) h / rows, cw = w / 2f;
            g.setColor(Color.BLACK);
            g.fill(rr(0, 0, w, h, 14));
            g.setColor(new Color(255, 255, 255, 130));
            g.draw(rr(0.5f, 0.5f, w - 1, h - 1, 14));
            g.setColor(new Color(255, 255, 255, 45));
            for (int r = 1; r < rows; r++) g.fill(new Rectangle2D.Float(1, r * ch, w - 2, 1));
            g.fill(new Rectangle2D.Float(cw, 1, 1, h - 2));
            for (int i = 0; i < labels.length; i++) {
                float x = (i % 2) * cw + 18, y = (i / 2) * ch;
                g.setFont(plain(13f));
                g.setColor(new Color(0xA3AAB3));
                g.drawString(labels[i], x, y + ch * 0.40f);
                if (hover == i) {
                    String hint = "нажмите, чтобы скопировать";
                    g.setFont(plain(11.5f));
                    g.setColor(new Color(0x7A7A7A));
                    g.drawString(hint, (i % 2) * cw + cw - 18 - g.getFontMetrics().stringWidth(hint), y + ch * 0.40f);
                }
                g.setFont(bold(15f));
                g.setColor(colors[i] != null ? colors[i] : TEXT);
                g.drawString(values[i], x, y + ch * 0.40f + 24);
            }
            g.dispose();
        }
    }

    /** Светлая кнопка запуска со свечением (внутренний отступ 12 px под свечение). */
    static final class LightButton extends JComponent {
        static final int GLOW = 12;
        private String text;
        private Glyph glyph;
        private boolean press;
        private final Ui.Hover hv = new Ui.Hover(this);
        private final Runnable action;

        LightButton(String text, Glyph glyph, Runnable action) {
            this.text = text; this.glyph = glyph; this.action = action;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); press = false; repaint(); }
                public void mousePressed(MouseEvent e) { press = true; repaint(); }
                public void mouseReleased(MouseEvent e) {
                    boolean fire = press && isEnabled() && contains(e.getPoint());
                    press = false;
                    repaint();
                    if (fire && action != null) action.run();
                }
            });
        }

        void set(String text, Glyph glyph) { this.text = text; this.glyph = glyph; repaint(); }

        @Override public void setEnabled(boolean b) { super.setEnabled(b); setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            boolean en = isEnabled();
            float x = GLOW, y = GLOW + (press ? 1 : 0), w = getWidth() - GLOW * 2, h = getHeight() - GLOW * 2;
            if (en) {
                for (int i = GLOW; i > 0; i -= 2) {
                    g.setColor(new Color(255, 255, 255, (int) (3 + 5 * hv.v)));
                    g.fill(rr(x - i, y - i + 3, w + 2 * i, h + 2 * i, 18 + i));
                }
            }
            Color top = en ? Ui.lerp(new Color(0xF4F4F4), Color.WHITE, hv.v) : new Color(0x2A2A2A);
            Color bot = en ? Ui.lerp(BTN, new Color(0xF4F4F4), hv.v) : new Color(0x202020);
            g.setPaint(new GradientPaint(0, y, top, 0, y + h, bot));
            g.fill(rr(x, y, w, h, 18));
            g.setFont(semi(17f));
            int tw = g.getFontMetrics().stringWidth(text);
            float gx = x + (w - tw - 30) / 2f;
            Color c = en ? BTN_TEXT : new Color(0x7B828A);
            glyph(g, glyph, gx + 8, y + h / 2f, glyph == Glyph.PLAY ? 13 : 15, c);
            g.setColor(c);
            g.drawString(text, gx + 30, baseline(g, y, h));
            g.dispose();
        }
    }

    /** Нижняя панель главной: статус готовности, прогресс и кнопка запуска. */
    static final class ActionBar extends JPanel {
        static final int OK = 1, ERR = 2, BUSY = 3;
        final LightButton button;
        final Ui.Bar bar = new Ui.Bar();
        private String text = "";
        private int kind;

        ActionBar(LightButton button) {
            super(null);
            this.button = button;
            setOpaque(false);
            add(button);
            add(bar);
            bar.setVisible(false);
        }

        void status(String t, int k) { text = t; kind = k; repaint(); }

        void progress(boolean on) { bar.setVisible(on); revalidate(); doLayout(); repaint(); }

        @Override public void setBounds(int x, int y, int w, int h) { super.setBounds(x, y, w, h); doLayout(); }

        @Override public void doLayout() {
            int w = getWidth(), h = getHeight();
            int bw = 232 + LightButton.GLOW * 2, bh = 58 + LightButton.GLOW * 2;
            button.setBounds(w - 26 - bw + LightButton.GLOW, (h - bh) / 2, bw, bh);
            bar.setBounds(30, h / 2 + 14, Math.max(40, w - 30 - bw - 40), 6);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(Color.BLACK);
            g.fill(rr(0, 0, w, h, 18));
            g.setColor(new Color(255, 255, 255, 140));
            g.draw(rr(0.5f, 0.5f, w - 1, h - 1, 18));
            float ty = bar.isVisible() ? h / 2f - 8 : h / 2f;
            float tx = 30;
            if (kind == OK) { glyph(g, Glyph.CHECK, 38, ty, 16, GREEN); tx = 60; }
            else if (kind == ERR) { glyph(g, Glyph.CROSS, 38, ty, 14, RED); tx = 60; }
            g.setFont(semi(16f));
            g.setColor(kind == ERR ? new Color(0xF2B8B8) : kind == BUSY ? new Color(0xC9CED4) : new Color(0xDDE2E7));
            String t = text;
            int max = w - (int) tx - 232 - LightButton.GLOW - 60;
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(t) > max) {
                while (t.length() > 1 && fm.stringWidth(t + "…") > max) t = t.substring(0, t.length() - 1);
                t += "…";
            }
            g.drawString(t, tx, ty + (fm.getAscent() - fm.getDescent()) / 2f);
            g.dispose();
        }
    }
}
