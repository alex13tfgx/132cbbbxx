package guardlisten;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/** Ресурспаки клиента: список из папки resourcepacks, добавление и удаление. */
final class PacksPanel extends Ui.Screen {
    private static final int ROW_H = 64, GAP = 8;

    private final Path root;
    private final Supplier<Client> client;
    private final int cx, cw;
    private final JPanel list = new JPanel(null);
    private final JScrollPane scroll;
    private final JLabel empty;

    PacksPanel(Path root, Supplier<Client> client, int w, int h, int cx, int cw) {
        this.root = root; this.client = client; this.cx = cx; this.cw = cw;
        setBounds(0, 0, w, h);
        JLabel t = Ui.label("Ресурспаки", Ui.font(Font.BOLD, 26f), Wv.TEXT, SwingConstants.LEFT);
        t.setBounds(cx, 26, 300, 40);
        JLabel sub = Ui.label("Наборы ресурсов, которые увидит клиент", Ui.font(Font.PLAIN, 14f), Wv.MUTED, SwingConstants.LEFT);
        sub.setBounds(cx, 64, 400, 20);
        Wv.OutlineBtn add = new Wv.OutlineBtn("Добавить", Wv.Glyph.PLUS, this::addPack);
        add.setBounds(cx + cw - 316, 34, 140, 42);
        Wv.OutlineBtn open = new Wv.OutlineBtn("Открыть папку", Wv.Glyph.FOLDER, this::openFolder);
        open.setBounds(cx + cw - 166, 34, 166, 42);
        empty = Ui.label("Ресурспаки не установлены. Нажмите «Добавить» и выберите .zip", Ui.font(Font.PLAIN, 14f), Wv.MUTED, SwingConstants.CENTER);
        empty.setBounds(cx, 220, cw, 24);
        list.setOpaque(false);
        scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getVerticalScrollBar().setUI(new ModsPanel.ThinBar());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setBounds(cx, 104, cw + 8, h - 104 - 24);
        for (Component c : new Component[]{t, sub, add, open, empty, scroll}) add(c);
    }

    private Path dir() { return root.resolve("instances").resolve(client.get().id).resolve("resourcepacks"); }

    void onShow() { reload(); }

    void reload() {
        list.removeAll();
        List<Path> packs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir())) {
            for (Path p : ds) if (Files.isDirectory(p) || p.getFileName().toString().toLowerCase().endsWith(".zip")) packs.add(p);
        } catch (Exception ignored) { }
        packs.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));
        for (int i = 0; i < packs.size(); i++) {
            Row r = new Row(packs.get(i));
            r.setBounds(0, i * (ROW_H + GAP), cw, ROW_H);
            list.add(r);
        }
        list.setPreferredSize(new Dimension(cw, Math.max(0, packs.size() * (ROW_H + GAP) - GAP)));
        empty.setVisible(packs.isEmpty());
        list.revalidate();
        scroll.revalidate();
        scroll.doLayout();
        scroll.getViewport().doLayout();
        repaint();
    }

    private void openFolder() {
        try {
            Files.createDirectories(dir());
            Desktop.getDesktop().open(dir().toFile());
        } catch (Exception ignored) { }
    }

    private void addPack() {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Выберите ресурспак (.zip)", FileDialog.LOAD);
        fd.setMultipleMode(true);
        fd.setFilenameFilter((d, n) -> n.toLowerCase().endsWith(".zip"));
        fd.setVisible(true);
        File[] files = fd.getFiles();
        if (files == null || files.length == 0) return;
        try {
            Files.createDirectories(dir());
            for (File f : files) Files.copy(f.toPath(), dir().resolve(f.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Не удалось добавить: " + e.getMessage());
        }
        reload();
    }

    private void removePack(Path p) {
        String n = p.getFileName().toString();
        if (JOptionPane.showConfirmDialog(this, "Удалить ресурспак «" + n + "»?", "Удаление",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            if (Files.isDirectory(p)) {
                try (var s = Files.walk(p)) {
                    s.sorted(Comparator.reverseOrder()).forEach(q -> { try { Files.delete(q); } catch (Exception ignored) { } });
                }
            } else Files.deleteIfExists(p);
        } catch (Exception ignored) { }
        reload();
    }

    private static String size(Path p) {
        try {
            if (Files.isDirectory(p)) return "Папка";
            long b = Files.size(p);
            return b >= 1048576 ? String.format("%.1f МБ", b / 1048576.0) : Math.max(1, b / 1024) + " КБ";
        } catch (Exception e) { return ""; }
    }

    private final class Row extends JPanel {
        private final Path path;
        private final String name, meta;

        Row(Path path) {
            super(null);
            this.path = path;
            setOpaque(false);
            String n = path.getFileName().toString();
            String base = n.toLowerCase().endsWith(".zip") ? n.substring(0, n.length() - 4) : n;
            String clean = base.replaceAll("§.", "").trim(); // коды цвета Minecraft
            name = clean.isEmpty() ? base : clean;
            meta = (Files.isDirectory(path) ? "Папка" : "ZIP · " + PacksPanel.size(path));
            Ui.IconBtn del = new Ui.IconBtn(Ui.Icon.CLOSE, () -> removePack(this.path));
            del.setToolTipText("Удалить");
            add(del);
        }

        @Override public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);
            getComponent(0).setBounds(w - 50, (h - 32) / 2, 32, 32);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.aa(g);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(255, 255, 255, 6));
            g.fill(Wv.rr(0, 0, w, h, 14));
            g.setColor(new Color(255, 255, 255, 22));
            g.draw(Wv.rr(0.5f, 0.5f, w - 1, h - 1, 14));
            g.setColor(new Color(255, 255, 255, 14));
            g.fill(Wv.rr(14, 12, 40, 40, 10));
            Wv.glyph(g, Wv.Glyph.GRID, 34, 32, 16, new Color(0xB4BAC2));
            g.setFont(Wv.bold(15f));
            g.setColor(Wv.TEXT);
            String n = name;
            FontMetrics fm = g.getFontMetrics();
            while (n.length() > 1 && fm.stringWidth(n) > w - 140) n = n.substring(0, n.length() - 1);
            if (!n.equals(name)) n += "…";
            g.drawString(n, 68, 29);
            g.setFont(Wv.plain(12.5f));
            g.setColor(Wv.MUTED);
            g.drawString(meta, 68, 48);
            g.dispose();
        }
    }
}
