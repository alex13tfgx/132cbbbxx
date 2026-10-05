package guardlisten;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/** Кастомные компоненты в тёмном стиле лаунчера. */
final class Ui {
    static final Color BG = new Color(0x050505);
    static final Color BORDER = new Color(255, 255, 255, 90);
    static final Color FIELD = new Color(0x000000);
    static final Color FIELD_BORDER = new Color(0x6A6A6A);
    static final Color TEXT = new Color(0xF5F5F5);
    static final Color MUTED = new Color(0x8C8C8C);
    static final Color A1 = new Color(0xB4B4B4);
    static final Color A2 = new Color(0xFFFFFF);
    static final Color ORANGE = new Color(0xCFCFCF);
    static final Color ERR = new Color(0xE0E0E0);

    static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    /** Плавно приближает значение v к цели (0..1) и перерисовывает компонент. */
    static final class Hover implements Anim.Ticker {
        float v, target;
        float speed = 16f;
        private final JComponent c;

        Hover(JComponent c) { this.c = c; }

        void set(boolean on) {
            target = on ? 1f : 0f;
            if (v != target) Anim.add(this);
        }

        @Override public boolean tick(float dt) {
            v = Anim.approach(v, target, speed, dt);
            c.repaint();
            return v != target;
        }
    }

    /** Масштаб экрана (1, 1.25, 1.5 ...) для чётких иконок на HiDPI. */
    static double dpi() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().getDefaultTransform().getScaleX();
        } catch (Throwable t) { return 1.0; }
    }

    /** Качественно уменьшает картинку (ступенчато) и скругляет углы с антиалиасингом. */
    static java.awt.image.BufferedImage roundIcon(Image srcImg, int size, int radius) {
        int px = (int) Math.round(size * dpi());
        int rpx = (int) Math.round(radius * dpi());
        java.awt.image.BufferedImage cur = new java.awt.image.BufferedImage(srcImg.getWidth(null), srcImg.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g0 = cur.createGraphics();
        g0.drawImage(srcImg, 0, 0, null);
        g0.dispose();
        while (cur.getWidth() / 2 >= px && cur.getHeight() / 2 >= px) {
            java.awt.image.BufferedImage h = new java.awt.image.BufferedImage(cur.getWidth() / 2, cur.getHeight() / 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = h.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(cur, 0, 0, h.getWidth(), h.getHeight(), null);
            g.dispose();
            cur = h;
        }
        java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(px, px, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gs = scaled.createGraphics();
        gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gs.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gs.drawImage(cur, 0, 0, px, px, null);
        gs.dispose();
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(px, px, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D go = out.createGraphics();
        aa(go);
        go.setPaint(new TexturePaint(scaled, new Rectangle(0, 0, px, px)));
        go.fill(new RoundRectangle2D.Float(0, 0, px, px, rpx * 2f, rpx * 2f));
        go.dispose();
        return out;
    }

    private static final java.util.Map<Integer, Font> FONTS = new java.util.concurrent.ConcurrentHashMap<>();

    static Font font(int style, float size) {
        return FONTS.computeIfAbsent(style * 100000 + Math.round(size * 100), k -> new Font("Segoe UI", style, 1).deriveFont(style, size));
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static JLabel label(String text, Font f, Color c, int align) {
        JLabel l = new JLabel(text, align);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    /** Основная кнопка с градиентом и свечением. */
    static final class GradButton extends JComponent {
        private String text;
        private final boolean play;
        private boolean press;
        private Runnable action;
        private final Hover hv = new Hover(this);
        boolean ghost;

        GradButton ghost() { ghost = true; return this; }

        GradButton(String text, boolean play, Runnable action) {
            this.text = text; this.play = play; this.action = action;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); press = false; repaint(); }
                public void mousePressed(MouseEvent e) { press = true; repaint(); }
                public void mouseReleased(MouseEvent e) {
                    boolean fire = press && isEnabled() && contains(e.getPoint());
                    press = false; repaint();
                    if (fire && GradButton.this.action != null) GradButton.this.action.run();
                }
            });
        }

        void setText(String t) { text = t; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight() - (ghost ? 0 : 8), y = 0;
            if (ghost) {
                g.setColor(lerp(new Color(0x101010), new Color(0x242424), hv.v));
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                g.setColor(isEnabled() ? lerp(new Color(0x3A3A3A), new Color(0xB0B0B0), hv.v) : new Color(0x2A2A2A));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
                g.setFont(font(Font.BOLD, 13f));
                FontMetrics gf = g.getFontMetrics();
                g.setColor(isEnabled() ? TEXT : new Color(0x707070));
                g.drawString(text, (w - gf.stringWidth(text)) / 2, (h + gf.getAscent() - gf.getDescent()) / 2);
                g.dispose();
                return;
            }
            // свечение
            for (int i = 7; i > 0; i--) {
                g.setColor(new Color(255, 255, 255, isEnabled() ? 3 : 1));
                g.fill(new RoundRectangle2D.Float(2 - i / 2f, 4 - i / 2f + i / 2f, w - 4 + i, h + i, 12 + i, 12 + i));
            }
            Color c1 = A1, c2 = A2;
            if (!isEnabled()) { c1 = new Color(0x2A2A2A); c2 = new Color(0x333333); }
            else { c1 = lerp(new Color(0xF4F4F4), Color.WHITE, hv.v); c2 = lerp(new Color(0xE6E6E6), new Color(0xF4F4F4), hv.v); }
            if (press) y = 1;
            if (isEnabled() && hv.v > 0) {
                g.setColor(new Color(255, 255, 255, (int) (14 * hv.v)));
                g.fill(new RoundRectangle2D.Float(-3 * hv.v, 3 - 3 * hv.v, w + 6 * hv.v, h + 6 * hv.v, 16, 16));
            }
            g.setPaint(new GradientPaint(0, y, c1, 0, y + h, c2));
            g.fill(new RoundRectangle2D.Float(0, y, w, h, 16, 16));
            g.setFont(font(Font.BOLD, 15f));
            FontMetrics fm = g.getFontMetrics();
            String t = text.toUpperCase().equals(text) ? text : text;
            int tw = fm.stringWidth(t);
            int iw = play ? 22 : 0;
            int x = (w - tw - iw) / 2;
            g.setColor(isEnabled() ? Color.BLACK : new Color(0x808080));
            if (play) {
                int cy = y + h / 2;
                Path2D tri = new Path2D.Float();
                tri.moveTo(x, cy - 6); tri.lineTo(x + 9, cy); tri.lineTo(x, cy + 6); tri.closePath();
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(tri);
            }
            g.drawString(t, x + iw, y + (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Маленькая иконка-кнопка. */
    enum Icon { CLOSE, MIN, LINK, EYE, LEFT, RIGHT, BACK }

    static final class IconBtn extends JComponent {
        private final Icon icon;
        private final Hover hv = new Hover(this);
        boolean on;
        private final Runnable action;

        IconBtn(Icon icon, Runnable action) {
            this.icon = icon; this.action = action;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint()) && IconBtn.this.action != null) IconBtn.this.action.run(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(lerp(MUTED, TEXT, on ? 1f : hv.v));
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            switch (icon) {
                case CLOSE:
                    g.draw(new Line2D.Float(cx - 6, cy - 6, cx + 6, cy + 6));
                    g.draw(new Line2D.Float(cx - 6, cy + 6, cx + 6, cy - 6));
                    break;
                case MIN:
                    g.draw(new Line2D.Float(cx - 6, cy, cx + 6, cy));
                    break;
                case LINK:
                    g.draw(new RoundRectangle2D.Float(cx - 7, cy - 5, 11, 11, 3, 3));
                    g.draw(new Line2D.Float(cx - 1, cy + 1, cx + 7, cy - 7));
                    g.draw(new Line2D.Float(cx + 2, cy - 7, cx + 7, cy - 7));
                    g.draw(new Line2D.Float(cx + 7, cy - 7, cx + 7, cy - 2));
                    break;
                case EYE:
                    g.draw(new Arc2D.Float(cx - 9, cy - 6, 18, 12, 0, 180, Arc2D.OPEN));
                    g.draw(new Arc2D.Float(cx - 9, cy - 6, 18, 12, 180, 180, Arc2D.OPEN));
                    g.fill(new Ellipse2D.Float(cx - 2.5f, cy - 2.5f, 5, 5));
                    if (!on) g.draw(new Line2D.Float(cx - 8, cy + 8, cx + 8, cy - 8));
                    break;
                case LEFT:
                    g.draw(new Line2D.Float(cx + 3, cy - 7, cx - 3, cy));
                    g.draw(new Line2D.Float(cx - 3, cy, cx + 3, cy + 7));
                    break;
                case RIGHT:
                    g.draw(new Line2D.Float(cx - 3, cy - 7, cx + 3, cy));
                    g.draw(new Line2D.Float(cx + 3, cy, cx - 3, cy + 7));
                    break;
                case BACK:
                    g.draw(new Line2D.Float(cx - 7, cy, cx + 7, cy));
                    g.draw(new Line2D.Float(cx - 7, cy, cx - 1, cy - 6));
                    g.draw(new Line2D.Float(cx - 7, cy, cx - 1, cy + 6));
                    break;
            }
            g.dispose();
        }
    }

    private static void hint(Graphics g0, JTextField f, String ph) {
        if (!f.getText().isEmpty()) return;
        Graphics2D g = (Graphics2D) g0.create();
        aa(g);
        g.setColor(MUTED);
        g.setFont(f.getFont());
        FontMetrics fm = g.getFontMetrics();
        g.drawString(ph, f.getInsets().left, (f.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }

    /** Поле ввода с плейсхолдером и (для паролей) кнопкой "глаз". */
    static final class Field extends JPanel {
        final JTextField input;
        private final IconBtn eye;

        Field(String placeholder, boolean password) {
            super(null);
            setOpaque(false);
            input = password
                    ? new JPasswordField() { @Override protected void paintComponent(Graphics g) { super.paintComponent(g); hint(g, this, placeholder); } }
                    : new JTextField() { @Override protected void paintComponent(Graphics g) { super.paintComponent(g); hint(g, this, placeholder); } };
            input.setOpaque(false);
            input.setBorder(new EmptyBorder(0, 0, 0, 0));
            input.setFont(font(Font.PLAIN, 15f));
            input.setForeground(TEXT);
            input.setCaretColor(TEXT);
            input.setSelectionColor(new Color(0x555555));
            input.setSelectedTextColor(Color.WHITE);
            add(input);
            if (password) {
                JPasswordField pf = (JPasswordField) input;
                pf.setEchoChar('•');
                eye = new IconBtn(Icon.EYE, null);
                eye.addMouseListener(new MouseAdapter() {
                    public void mouseReleased(MouseEvent e) {
                        eye.on = !eye.on;
                        pf.setEchoChar(eye.on ? (char) 0 : '•');
                        eye.repaint();
                    }
                });
                add(eye);
            } else eye = null;
        }

        String text() { return input instanceof JPasswordField ? new String(((JPasswordField) input).getPassword()) : input.getText(); }

        @Override public void setBounds(int x, int y, int w, int h) { super.setBounds(x, y, w, h); doLayout(); }

        @Override public void doLayout() {
            int w = getWidth(), h = getHeight();
            input.setBounds(16, 0, w - 32 - (eye != null ? 30 : 0), h);
            if (eye != null) eye.setBounds(w - 46, h / 2 - 14, 28, 28);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            boolean focus = input.hasFocus();
            g.setColor(FIELD);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            g.setColor(focus ? new Color(0x8A8A8A) : FIELD_BORDER);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
            g.dispose();
        }
    }

    /** Ссылка-текст. */
    static final class Link extends JLabel {
        private final Hover hv = new Hover(this);

        @Override protected void paintComponent(Graphics g) {
            setForeground(lerp(MUTED, TEXT, hv.v));
            super.paintComponent(g);
        }

        Link(String text, Runnable r) {
            super(text);
            setFont(font(Font.PLAIN, 14f));
            setForeground(MUTED);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) r.run(); }
            });
        }
    }

    /** Круглый аватар с буквой. */
    static class Avatar extends JComponent {
        /** Картинка по умолчанию (логотип), если аватар не выбран. */
        static Image fallback;
        String letter = "?";
        String art;                       // id нарисованной аватарки
        java.awt.image.BufferedImage custom; // своё фото (квадрат)
        private java.awt.image.BufferedImage cache;
        private int cacheSize;

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int s = Math.min(getWidth(), getHeight());
            if (custom != null) {
                if (cache == null || cacheSize != s) { cache = roundIcon(custom, s, s / 2); cacheSize = s; }
                g.drawImage(cache, 0, 0, s, s, null);
            } else if (art != null) {
                Avatars.draw(g, art, s);
            } else if (fallback != null) {
                if (cache == null || cacheSize != s) { cache = roundIcon(fallback, s, s / 2); cacheSize = s; }
                g.drawImage(cache, 0, 0, s, s, null);
            } else {
                g.setPaint(new RadialGradientPaint(s * 0.4f, s * 0.35f, s * 0.75f, new float[]{0f, 1f},
                        new Color[]{new Color(0x5A5A5A), new Color(0x151515)}));
                g.fill(new Ellipse2D.Float(0, 0, s, s));
                g.setColor(Color.WHITE);
                g.setFont(font(Font.BOLD, s * 0.42f));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(letter, (s - fm.stringWidth(letter)) / 2f, (s + fm.getAscent() - fm.getDescent()) / 2f);
            }
            g.setColor(new Color(255, 255, 255, 30));
            g.draw(new Ellipse2D.Float(0.5f, 0.5f, s - 1, s - 1));
            g.dispose();
        }

        void setCustom(java.awt.image.BufferedImage img) { custom = img; cache = null; repaint(); }
    }

    /** Кнопка выбора аватарки в сетке. */
    static final class AvatarChoice extends JComponent {
        final String id;
        boolean selected;
        private final Hover hv = new Hover(this);

        AvatarChoice(String id, Runnable r) {
            this.id = id;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) r.run(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int s = Math.min(getWidth(), getHeight());
            int pad = 6;
            int base = s - 2 * pad;
            float sc = 1f + 0.06f * hv.v;
            Graphics2D gi = (Graphics2D) g.create();
            gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gi.translate(s / 2f, s / 2f);
            gi.scale(sc, sc);
            gi.translate(-base / 2f, -base / 2f);
            Avatars.draw(gi, id, base);
            gi.dispose();
            g.setStroke(new BasicStroke(2.2f));
            g.setColor(selected ? Color.WHITE : new Color(255, 255, 255, (int) (70 * hv.v)));
            g.draw(new Ellipse2D.Float(1.5f, 1.5f, s - 3, s - 3));
            g.dispose();
        }
    }

    /** Тонкая полоса прогресса. */
    static final class Bar extends JComponent implements Anim.Ticker {
        double value, shown;

        void setValue(double v) { value = v; Anim.add(this); }

        @Override public boolean tick(float dt) {
            shown += (value - shown) * (1 - Math.exp(-9 * dt));
            if (Math.abs(value - shown) < 0.001) shown = value;
            repaint();
            return shown != value;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int h = getHeight();
            g.setColor(new Color(0x222222));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), h, h, h));
            g.setPaint(new GradientPaint(0, 0, A1, getWidth(), 0, A2));
            if (shown <= 0) { g.dispose(); return; }
            g.fill(new RoundRectangle2D.Float(0, 0, (float) (getWidth() * shown), h, h, h));
            g.dispose();
        }
    }

    /** Пункт бокового меню. */
    static final class NavItem extends JComponent {
        final String text;
        boolean selected;
        private final Hover hv = new Hover(this), sel = new Hover(this);

        void setSelected(boolean b) { selected = b; sel.set(b); }

        NavItem(String text, Runnable r) {
            this.text = text;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) r.run(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            Color bg = lerp(lerp(new Color(255, 255, 255, 0), new Color(255, 255, 255, 12), hv.v), new Color(255, 255, 255, 24), sel.v);
            g.setColor(bg);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            if (sel.v > 0.01f) {
                float bh = (getHeight() - 18) * sel.v;
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Float(0, (getHeight() - bh) / 2f, 3, bh, 3, 3));
            }
            g.setColor(lerp(lerp(MUTED, TEXT, hv.v), Color.WHITE, sel.v));
            g.setFont(font(selected ? Font.BOLD : Font.PLAIN, 15f));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 18, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Карточка клиента в списке. */
    static final class Card extends JComponent {
        final String title, sub;
        final Image icon;
        boolean selected;
        private final Hover hv = new Hover(this);

        Card(String title, String sub, Image icon, Runnable r) {
            this.title = title; this.sub = sub; this.icon = icon;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) r.run(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(lerp(FIELD, new Color(0x171717), hv.v));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            g.setColor(selected ? Color.WHITE : FIELD_BORDER);
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));
            if (icon != null) g.drawImage(icon, 16, 15, 40, 40, null);
            g.setColor(TEXT);
            g.setFont(font(Font.BOLD, 16f));
            g.drawString(title, 72, 31);
            g.setColor(MUTED);
            g.setFont(font(Font.PLAIN, 13f));
            g.drawString(sub, 72, 51);
            if (selected) {
                g.setColor(Color.WHITE);
                g.setFont(font(Font.BOLD, 12f));
                String t = "ВЫБРАН";
                g.drawString(t, getWidth() - 20 - g.getFontMetrics().stringWidth(t), 40);
            }
            g.dispose();
        }
    }

    /** Переключатель-чип (выбор одного из вариантов). */
    static final class Chip extends JComponent {
        final String text;
        private final Hover hv = new Hover(this), sel = new Hover(this);

        Chip(String text, Runnable r) {
            this.text = text;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { hv.set(false); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) r.run(); }
            });
        }

        void setSelected(boolean b) { sel.set(b); }

        void snap(boolean b) { sel.v = sel.target = b ? 1f : 0f; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(lerp(lerp(new Color(16, 16, 16, 200), new Color(36, 36, 36, 220), hv.v), Color.WHITE, sel.v));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
            g.setColor(lerp(lerp(new Color(0x3A3A3A), new Color(0x909090), hv.v), Color.WHITE, sel.v));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12));
            g.setFont(font(Font.BOLD, 14f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(lerp(TEXT, new Color(0x0A0A0A), sel.v));
            g.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Экран (раздел) с плавным появлением: прозрачность + сдвиг, без снимков экрана. */
    static class Screen extends JPanel {
        float alpha = 1f, dy;

        Screen() {
            super(null);
            setOpaque(false);
        }

        static float alphaOf(Graphics g) {
            Composite c = ((Graphics2D) g).getComposite();
            return c instanceof AlphaComposite ? ((AlphaComposite) c).getAlpha() : 1f;
        }

        @Override public void paint(Graphics g0) {
            if (alpha <= 0.002f) return;
            if (alpha >= 0.998f && dy == 0f) { super.paint(g0); return; }
            Graphics2D g = (Graphics2D) g0.create();
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alpha * alphaOf(g0))));
            g.translate(0, Math.round(dy));
            super.paint(g);
            g.dispose();
        }
    }
}
