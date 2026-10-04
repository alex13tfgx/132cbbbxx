package guardlisten;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Каталог модов (Modrinth): поиск, установка и удаление прямо из лаунчера. */
final class ModsPanel extends Ui.Screen {
    private static final String API = "https://api.modrinth.com/v2";
    private static final String FABRIC_API_ID = "P7dR8mSH";
    private static final int ROW_H = 76, GAP = 8;
    private static final Color META = new Color(0x666666);

    private static final class Mod {
        String id, title, desc, author, iconUrl;
        long downloads;
        String file;       // установленный файл (если известен)
        boolean system;    // системный мод: нельзя удалить
    }

    private final Path root;
    private final Supplier<Client> client;
    private final int cx, cw;

    private final Ui.Field search;
    private final JLabel status;
    private final JPanel list = new JPanel(null);
    private final JScrollPane scroll;
    private final Ui.Link tabCatalog, tabInstalled;
    private boolean installedTab;
    private final javax.swing.Timer debounce;
    private int token;
    private final Set<String> busy = ConcurrentHashMap.newKeySet();
    private final Map<String, BufferedImage> icons = new ConcurrentHashMap<>();
    private final Set<String> iconRequested = ConcurrentHashMap.newKeySet();
    private final ExecutorService iconPool = Executors.newFixedThreadPool(10, r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    private List<Mod> shown = new ArrayList<>();
    private double scrollTarget, scrollPos;
    private boolean scrollActive;
    private final Anim.Ticker scrollTicker = this::scrollStep;

    ModsPanel(Path root, Supplier<Client> client, int x, int w, int h, int cx, int cw) {
        super();
        this.root = root; this.client = client; this.cx = cx; this.cw = cw;
        setOpaque(false);
        setBounds(x, 0, w, h);

        JLabel title = Ui.label("Модификаторы", Ui.font(Font.BOLD, 22f), Ui.TEXT, SwingConstants.LEFT);
        title.setBounds(cx, 28, 220, 40);
        add(title);

        tabCatalog = new Ui.Link("Каталог", () -> switchTab(false));
        tabInstalled = new Ui.Link("Установленные", () -> switchTab(true));
        tabCatalog.setFont(Ui.font(Font.BOLD, 15f));
        tabInstalled.setFont(Ui.font(Font.BOLD, 15f));
        tabCatalog.setBounds(cx + 250, 38, 80, 22);
        tabInstalled.setBounds(cx + 340, 38, 130, 22);
        add(tabCatalog);
        add(tabInstalled);

        search = new Ui.Field("Поиск модификаторов на Modrinth...", false);
        search.setBounds(cx, 84, cw, 44);
        add(search);
        debounce = new javax.swing.Timer(420, e -> reload());
        debounce.setRepeats(false);
        search.input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });

        status = Ui.label("", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.CENTER);
        status.setBounds(cx, 220, cw, 24);
        add(status);

        list.setOpaque(false);
        scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.setBounds(cx, 144, cw + 6, h - 144 - 18);
        scroll.setWheelScrollingEnabled(false);
        scroll.getVerticalScrollBar().setUI(new ThinBar());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.addMouseWheelListener(e -> smoothScroll(e.getPreciseWheelRotation()));
        add(scroll);
        setComponentZOrder(status, 0);
        styleTabs();
    }

    // ---------- плавный скролл ----------
    private void smoothScroll(double rot) {
        JScrollBar bar = scroll.getVerticalScrollBar();
        if (!scrollActive) { scrollPos = bar.getValue(); scrollTarget = scrollPos; }
        double max = bar.getMaximum() - bar.getVisibleAmount();
        scrollTarget = Math.max(0, Math.min(max, scrollTarget + rot * 120));
        scrollActive = true;
        Anim.add(scrollTicker);
    }

    private boolean scrollStep(float dt) {
        JScrollBar bar = scroll.getVerticalScrollBar();
        scrollPos += (scrollTarget - scrollPos) * (1 - Math.exp(-13 * dt));
        if (Math.abs(scrollTarget - scrollPos) < 0.5) scrollPos = scrollTarget;
        bar.setValue((int) Math.round(scrollPos));
        scrollActive = scrollPos != scrollTarget;
        return scrollActive;
    }

    private static final class ThinBar extends BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private static JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            g.setColor(new Color(255, 255, 255, isThumbRollover() ? 90 : 50));
            g.fill(new RoundRectangle2D.Float(r.x + 1, r.y, r.width - 2, r.height, 6, 6));
            g.dispose();
        }
    }

    // ---------- вкладки ----------
    private void styleTabs() {
        tabCatalog.setForeground(installedTab ? Ui.MUTED : Color.WHITE);
        tabInstalled.setForeground(installedTab ? Color.WHITE : Ui.MUTED);
    }

    private void switchTab(boolean installed) {
        if (installedTab == installed) return;
        installedTab = installed;
        search.input.setText("");
        styleTabs();
        reload();
    }

    boolean scrolling() { return scrollActive; }

    private boolean loaded;

    void onShow() { if (installedTab || !loaded) reload(); }

    // ---------- хранилище установленных ----------
    private Path modsDir() { return root.resolve("instances").resolve(client.get().id).resolve("mods"); }

    private Path storeFile() { return root.resolve("instances").resolve(client.get().id).resolve("launcher-mods.json"); }

    private Map<String, Object> storeCache;
    private Path storeCachePath;

    /** Список установленных модов; читается с диска один раз, дальше из памяти. */
    private synchronized Map<String, Object> store() {
        Path f = storeFile();
        if (storeCache == null || !f.equals(storeCachePath)) {
            Map<String, Object> m = new LinkedHashMap<>();
            try {
                if (Files.exists(f)) m = Json.obj(Json.parse(Files.readString(f)));
            } catch (Exception ignored) { }
            storeCache = m;
            storeCachePath = f;
        }
        return new LinkedHashMap<>(storeCache);
    }

    private synchronized void saveStore(Map<String, Object> m) {
        storeCache = new LinkedHashMap<>(m);
        storeCachePath = storeFile();
        try {
            Files.createDirectories(storeFile().getParent());
            Files.writeString(storeFile(), Json.write(m));
        } catch (Exception ignored) { }
    }

    private boolean isInstalled(String id) {
        Map<String, Object> rec = Json.obj(store().get(id));
        String f = Json.str(rec, "file");
        return f != null && Files.exists(modsDir().resolve(f));
    }

    // ---------- загрузка списка ----------
    private void reload() {
        debounce.stop();
        loaded = true;
        int my = ++token;
        String q = search.input.getText().trim();
        if (installedTab) {
            List<Mod> mods = installedMods();
            if (!q.isEmpty()) mods.removeIf(m -> !m.title.toLowerCase().contains(q.toLowerCase()));
            fill(mods, "Ничего не установлено");
            return;
        }
        status.setText("Загрузка...");
        fill(new ArrayList<>(), null);
        Client c = client.get();
        new Thread(() -> {
            try {
                String facets = "[[\"project_type:mod\"],[\"categories:fabric\"],[\"versions:" + c.mc + "\"]]";
                String url = API + "/search?limit=40&index=" + (q.isEmpty() ? "downloads" : "relevance")
                        + (q.isEmpty() ? "" : "&query=" + URLEncoder.encode(q, StandardCharsets.UTF_8))
                        + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8);
                Map<String, Object> res = Json.obj(Json.parse(Installer.get(url)));
                List<Mod> mods = new ArrayList<>();
                for (Object o : Json.list(res.get("hits"))) {
                    Map<String, Object> h = Json.obj(o);
                    Mod m = new Mod();
                    m.id = Json.str(h, "project_id");
                    m.title = Json.str(h, "title");
                    m.desc = Json.str(h, "description");
                    m.author = Json.str(h, "author");
                    m.iconUrl = Json.str(h, "icon_url");
                    m.downloads = Json.num(h, "downloads");
                    if (m.id != null && m.title != null) mods.add(m);
                }
                SwingUtilities.invokeLater(() -> { if (my == token) fill(mods, "Ничего не найдено"); });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> { if (my == token) { fill(new ArrayList<>(), null); status.setText("Нет соединения с Modrinth"); } });
            }
        }, "mods-search").start();
    }

    private List<Mod> installedMods() {
        List<Mod> out = new ArrayList<>();
        Map<String, Object> st = store();
        Set<String> known = new HashSet<>();
        for (Map.Entry<String, Object> e : st.entrySet()) {
            Map<String, Object> rec = Json.obj(e.getValue());
            String f = Json.str(rec, "file");
            if (f == null || !Files.exists(modsDir().resolve(f))) continue;
            Mod m = new Mod();
            m.id = e.getKey();
            m.title = Json.str(rec, "title") != null ? Json.str(rec, "title") : f;
            m.desc = f;
            m.iconUrl = Json.str(rec, "icon");
            m.file = f;
            known.add(f);
            out.add(m);
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modsDir(), "*.jar")) {
            for (Path p : ds) {
                String f = p.getFileName().toString();
                if (known.contains(f)) continue;
                Mod m = new Mod();
                m.id = "file:" + f;
                m.title = f.replaceAll("\\.jar$", "");
                m.desc = f;
                m.file = f;
                m.system = f.equals("wiosvisual.jar") || f.startsWith("fabric-api");
                if (m.system) m.desc = f.equals("wiosvisual.jar") ? "Клиент WiosVisuals" : "Обязательная библиотека Fabric";
                out.add(m);
            }
        } catch (Exception ignored) { }
        out.sort(Comparator.comparing((Mod m) -> !m.system).thenComparing(m -> m.title.toLowerCase()));
        return out;
    }

    private void fill(List<Mod> mods, String emptyText) {
        shown = mods;
        list.removeAll();
        int y = 0;
        int idx = 0;
        for (Mod m : mods) {
            prefetch(m);
            Row r = new Row(m);
            if (idx < 14) r.fadeIn(idx * 28);
            idx++;
            r.setBounds(0, y, cw, ROW_H);
            list.add(r);
            y += ROW_H + GAP;
        }
        list.setPreferredSize(new Dimension(cw, Math.max(0, y)));
        list.setSize(cw, Math.max(0, y));
        status.setText(mods.isEmpty() && emptyText != null ? emptyText : (mods.isEmpty() ? status.getText() : ""));
        if (!mods.isEmpty()) status.setText("");
        scrollActive = false;
        list.revalidate();
        list.repaint();
        scroll.revalidate();
        SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new Point(0, 0)));
        javax.swing.Timer reset = new javax.swing.Timer(60, e -> scroll.getViewport().setViewPosition(new Point(0, 0)));
        reset.setRepeats(false);
        reset.start();
    }

    // ---------- действия ----------
    private void install(Mod m, Row row) {
        if (!busy.add(m.id)) return;
        row.refresh();
        Client c = client.get();
        new Thread(() -> {
            String err = null;
            try {
                installProject(m.id, m.title, m.iconUrl, c, 0);
            } catch (Exception e) {
                err = e.getMessage() != null ? e.getMessage() : e.toString();
            }
            String fe = err;
            SwingUtilities.invokeLater(() -> {
                busy.remove(m.id);
                row.refresh();
                if (fe != null) row.error(fe);
            });
        }, "mod-install").start();
    }

    private void installProject(String projectId, String title, String icon, Client c, int depth) throws Exception {
        String url = API + "/project/" + projectId + "/version?loaders=" + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8)
                + "&game_versions=" + URLEncoder.encode("[\"" + c.mc + "\"]", StandardCharsets.UTF_8);
        List<Object> versions = Json.list(Json.parse(Installer.get(url)));
        if (versions.isEmpty()) throw new IllegalStateException("Нет версии для " + c.mc);
        Map<String, Object> v = Json.obj(versions.get(0));
        if (depth < 2) {
            for (Object d : Json.list(v.get("dependencies"))) {
                Map<String, Object> dm = Json.obj(d);
                String pid = Json.str(dm, "project_id");
                if (pid == null || FABRIC_API_ID.equals(pid) || !"required".equals(Json.str(dm, "dependency_type"))) continue;
                if (isInstalled(pid)) continue;
                String dtitle = pid;
                String dicon = null;
                try {
                    Map<String, Object> pm = Json.obj(Json.parse(Installer.get(API + "/project/" + pid)));
                    if (Json.str(pm, "title") != null) dtitle = Json.str(pm, "title");
                    dicon = Json.str(pm, "icon_url");
                } catch (Exception ignored) { }
                installProject(pid, dtitle, dicon, c, depth + 1);
            }
        }
        List<Object> files = Json.list(v.get("files"));
        Map<String, Object> file = null;
        for (Object f : files) if (Boolean.TRUE.equals(Json.obj(f).get("primary"))) file = Json.obj(f);
        if (file == null && !files.isEmpty()) file = Json.obj(files.get(0));
        if (file == null) throw new IllegalStateException("Файл мода не найден");
        String name = Json.str(file, "filename");
        Files.createDirectories(modsDir());
        Path dst = modsDir().resolve(name);
        Path tmp = modsDir().resolve(name + ".part");
        Files.write(tmp, Installer.getBytes(Json.str(file, "url")));
        Files.move(tmp, dst, StandardCopyOption.REPLACE_EXISTING);
        Map<String, Object> st = store();
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("title", title);
        rec.put("file", name);
        if (icon != null) rec.put("icon", icon);
        st.put(projectId, rec);
        saveStore(st);
    }

    private void removeMod(Mod m, Row row) {
        try {
            Map<String, Object> st = store();
            String f = m.file;
            if (f == null) f = Json.str(Json.obj(st.get(m.id)), "file");
            if (f != null) Files.deleteIfExists(modsDir().resolve(f));
            st.remove(m.id);
            saveStore(st);
        } catch (Exception e) {
            row.error("Не удалось удалить: " + e.getMessage());
            return;
        }
        if (installedTab) {
            reload();
        } else row.refresh();
    }

    // ---------- иконки ----------
    private BufferedImage icon(Mod m) {
        if (m.iconUrl == null) return null;
        BufferedImage img = icons.get(m.iconUrl);
        if (img == null) prefetch(m);
        return img;
    }

    /** Загружает иконку в фоне (диск-кэш -> сеть), декодирует (в т.ч. webp) и перерисовывает список. */
    private void prefetch(Mod m) {
        if (m.iconUrl == null || icons.containsKey(m.iconUrl) || !iconRequested.add(m.iconUrl)) return;
        String url = m.iconUrl;
        iconPool.submit(() -> {
            try {
                Path cache = root.resolve("cache").resolve("icons").resolve(Integer.toHexString(url.hashCode()) + ".png");
                BufferedImage bi = null;
                if (Files.exists(cache)) bi = javax.imageio.ImageIO.read(cache.toFile());
                if (bi == null) {
                    bi = javax.imageio.ImageIO.read(new ByteArrayInputStream(Installer.getBytes(url)));
                    if (bi != null) {
                        Files.createDirectories(cache.getParent());
                        javax.imageio.ImageIO.write(bi, "png", cache.toFile());
                    }
                }
                if (bi != null) icons.put(url, Ui.roundIcon(bi, 48, 12));
            } catch (Throwable ignored) { }
            SwingUtilities.invokeLater(list::repaint);
        });
    }

    /** Обрезает строку под ширину с «...»; двоичный поиск — ~7 замеров вместо сотни. */
    private static String shorten(String s, FontMetrics fm, int w) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= w) return s;
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (fm.stringWidth(s.substring(0, mid) + "...") <= w) lo = mid; else hi = mid - 1;
        }
        return s.substring(0, lo).trim() + "...";
    }

    private static String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    // ---------- строка мода ----------
    private final class Row extends JComponent {
        final Mod m;
        final Ui.GradButton btn;
        private final Ui.Hover hv = new Ui.Hover(this);
        private String err;
        private float appear = 1f;
        private String cTitle, cDesc, cMeta, cErr;
        private int cW = -1;

        void fadeIn(int delayMs) {
            appear = 0f;
            float[] t = {-delayMs / 1000f};
            Anim.add(dt -> {
                t[0] += dt;
                if (t[0] <= 0) return true;
                float x = Math.min(1f, t[0] / 0.32f);
                appear = Anim.easeOut(x);
                repaint();
                return x < 1f;
            });
        }

        @Override public void paint(Graphics g0) {
            if (appear >= 1f) { super.paint(g0); return; }
            Graphics2D g = (Graphics2D) g0.create();
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.001f, appear) * Ui.Screen.alphaOf(g0)));
            g.translate(0, Math.round((1f - appear) * 14));
            super.paint(g);
            g.dispose();
        }

        Row(Mod m) {
            this.m = m;
            btn = new Ui.GradButton("", false, this::click).ghost();
            add(btn);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
                public void mouseExited(MouseEvent e) { if (!contains(e.getPoint())) hv.set(false); }
            });
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv.set(true); }
            });
            refresh();
        }

        private boolean installed() { return m.file != null || isInstalled(m.id); }

        private void click() {
            err = null;
            if (m.system) return;
            if (busy.contains(m.id)) return;
            if (installed()) removeMod(m, this); else install(m, this);
        }

        void error(String e) { err = e; repaint(); }

        void refresh() {
            boolean b = busy.contains(m.id);
            if (m.system) { btn.setText("Системный"); btn.setEnabled(false); }
            else if (b) { btn.setText("Установка..."); btn.setEnabled(false); }
            else if (installed()) { btn.setText("Удалить"); btn.setEnabled(true); }
            else { btn.setText("Скачать"); btn.setEnabled(true); }
            btn.repaint();
            repaint();
        }

        @Override public void doLayout() { btn.setBounds(getWidth() - 124, (getHeight() - 36) / 2, 108, 36); }

        @Override public void setBounds(int x, int y, int w, int h) { super.setBounds(x, y, w, h); doLayout(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(Ui.lerp(Ui.FIELD, new Color(0x161616), hv.v));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            g.setColor(Ui.lerp(Ui.FIELD_BORDER, new Color(0x4A4A4A), hv.v));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 14, 14));

            BufferedImage ic = icon(m);
            if (ic != null) {
                g.drawImage(ic, 14, 14, 48, 48, null);
            } else {
                g.setClip(new RoundRectangle2D.Float(14, 14, 48, 48, 12, 12));
                g.setColor(new Color(0x1E1E1E));
                g.fillRect(14, 14, 48, 48);
                g.setColor(Ui.MUTED);
                g.setFont(Ui.font(Font.BOLD, 20f));
                String l = m.title.isEmpty() ? "?" : m.title.substring(0, 1).toUpperCase();
                FontMetrics lf = g.getFontMetrics();
                g.drawString(l, 14 + (48 - lf.stringWidth(l)) / 2, 14 + (48 + lf.getAscent() - lf.getDescent()) / 2);
            }
            g.setClip(null);

            int tx = 76, maxW = w - tx - 140;
            Font fTitle = Ui.font(Font.BOLD, 15f), fDesc = Ui.font(Font.PLAIN, 13f), fMeta = Ui.font(Font.PLAIN, 12f);
            if (maxW != cW || !java.util.Objects.equals(err, cErr)) {
                // строки обрезаются один раз и кэшируются до смены ширины/текста
                cW = maxW;
                cErr = err;
                cTitle = shorten(m.title, g.getFontMetrics(fTitle), maxW);
                cDesc = shorten(err != null ? err : m.desc, g.getFontMetrics(fDesc), maxW);
                cMeta = m.downloads > 0 || m.author != null
                        ? shorten((m.author != null ? m.author : "") + (m.downloads > 0 ? "  \u00b7  " + fmt(m.downloads) + " загрузок" : ""), g.getFontMetrics(fMeta), maxW)
                        : null;
            }
            g.setFont(fTitle);
            g.setColor(Ui.TEXT);
            g.drawString(cTitle, tx, 30);
            g.setFont(fDesc);
            g.setColor(err != null ? Ui.ERR : Ui.MUTED);
            g.drawString(cDesc, tx, 50);
            if (cMeta != null) {
                g.setColor(META);
                g.setFont(fMeta);
                g.drawString(cMeta, tx, 66);
            }
            g.dispose();
        }
    }
}
