package guardlisten;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** GuardListen Launcher — запускает клиенты (Wios и др.) на Fabric. */
public final class Main {
    static final int AUTH_W = 500, AUTH_H = 560, APP_W = 1060, APP_H = 680, SIDE = 250, M = 44, CW = AUTH_W - 2 * M;
    static final int CX = 48, CWD = APP_W - SIDE - 2 * CX;
    private int W = AUTH_W, H = AUTH_H;

    private final Path appDir = detectAppDir();
    private final Path root = Path.of(System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")), "GuardListen");
    private final Accounts accounts = new Accounts(root);
    private final List<Client> clients = Client.load(appDir);
    private final Map<String, Object> settings = new LinkedHashMap<>();

    private JFrame frame;
    private LayeredRoot rootPanel;
    private Ui.Screen authScreen, appScreen, homeScreen, clientsScreen, settingsScreen, accountScreen;
    private Ui.NavItem[] nav;
    private JPanel clientList;
    private ModsPanel modsScreen;
    private Ui.Screen transFrom, transTo;
    private Anim.Ticker transTicker;
    private JLabel sideName, sideRole, accName, accRole, accLogin;
    private Ui.Avatar sideAvatar, accAvatar;
    private Ui.AvatarChoice[] choices;
    private BufferedImage customAvatar;
    private Ui.IconBtn closeBtn;
    private int clientIdx;
    private boolean registerMode, busy;

    // auth
    private Ui.Field loginField, passField, pass2Field;
    private JLabel authTitle, authSub, authError;
    private Ui.GradButton authBtn;
    private Ui.Link switchLink, forgotLink;
    // main
    private JTextArea desc;
    private JLabel clientTitle, status, buildLabel;
    private volatile long shownBuild = -1;
    private Ui.GradButton launchBtn;
    private Ui.Bar bar;
    // settings
    private Ui.Field nickField;
    private JLabel ramLabel;

    public static void main(String[] args) throws Exception {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        Main m = new Main();
        if (args.length >= 2 && args[0].equals("--shot")) {
            SwingUtilities.invokeAndWait(() -> m.shots(Path.of(args[1])));
            System.exit(0);
        }
        if (args.length >= 1 && args[0].equals("--prepare")) {
            Client c = m.clients.get(0);
            Installer.Plan p = new Installer(m.root).prepare(c, "Player", 4, c.resolveJar(m.appDir, m.root),
                    (t, f) -> System.out.printf("%3d%% %s%n", (int) (f * 100), t));
            System.out.println(String.join(" ", p.command));
            System.exit(0);
        }
        if (args.length >= 1 && args[0].equals("--run")) {
            Client c = m.clients.get(0);
            Installer.Plan p = new Installer(m.root).prepare(c, "Player", 4, c.resolveJar(m.appDir, m.root), (t, f) -> { });
            System.exit(new ProcessBuilder(p.command).directory(p.gameDir.toFile()).inheritIO().start().waitFor());
        }
        if (args.length >= 1 && args[0].equals("--rpc-test")) {
            Discord d = new Discord();
            d.set("В лаунчере \u2022 Ник: test", "Проверка статуса");
            d.start();
            Thread.sleep(6000);
            System.out.println("Discord ответ: " + d.lastResponse.replaceAll("\\s+", " "));
            d.setEnabled(false);
            Thread.sleep(2500);
            System.exit(0);
        }
        if (args.length >= 2 && args[0].equals("--live")) {
            SwingUtilities.invokeAndWait(m::start);
            SwingUtilities.invokeAndWait(() -> m.frame.setAlwaysOnTop(true));
            Files.createDirectories(Path.of(args[1]));
            Robot r = new Robot();
            for (String scr : new String[]{"home", "clients", "mods", "settings", "account"}) {
                SwingUtilities.invokeAndWait(() -> m.show(scr));
                Thread.sleep(scr.equals("mods") ? 5000 : 900);
                Rectangle[] b = new Rectangle[1];
                SwingUtilities.invokeAndWait(() -> b[0] = m.frame.getBounds());
                javax.imageio.ImageIO.write(r.createScreenCapture(b[0]), "png", Path.of(args[1], "live_" + scr + ".png").toFile());
            }
            System.exit(0);
        }
        SwingUtilities.invokeLater(m::start);
    }

    private static Path detectAppDir() {
        try {
            Path p = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return (Files.isRegularFile(p) ? p.getParent() : p.getParent()).toAbsolutePath();
        } catch (Exception e) { return Path.of("").toAbsolutePath(); }
    }

    // ---------- настройки ----------
    private int ram() { Object o = settings.get("ram"); return o instanceof Number ? ((Number) o).intValue() : 4; }

    private void loadSettings() {
        try {
            Path f = root.resolve("settings.json");
            if (Files.exists(f)) settings.putAll(Json.obj(Json.parse(Files.readString(f))));
        } catch (Exception ignored) { }
    }

    private void saveSettings() {
        try {
            Files.createDirectories(root);
            Files.writeString(root.resolve("settings.json"), Json.write(settings));
        } catch (Exception ignored) { }
    }

    private String nick() {
        if (msa != null && msa.configured() && msa.linked()) return msa.name();
        String n = Json.str(settings, "nick");
        if (n != null && !n.isBlank()) return n;
        Accounts.User u = accounts.session();
        return u == null ? "Player" : sanitize(u.login);
    }

    private static String sanitize(String s) {
        int at = s.indexOf('@');
        if (at > 0) s = s.substring(0, at);
        s = s.replaceAll("[^A-Za-z0-9_]", "_");
        if (s.length() > 16) s = s.substring(0, 16);
        return s.length() < 3 ? "Player" : s;
    }

    // ---------- UI ----------
    private final Particles particles = new Particles();
    private final Discord discord = new Discord();
    private MicrosoftAuth msa;
    private JLabel msStatus, msCode;
    private Ui.GradButton msBtn;
    private Ui.Link msOpen;
    private volatile boolean msCancel;
    private boolean msBusy;
    private String msErr;
    private MicrosoftAuth.DeviceCode msDc;
    private Ui.Chip[] rpcChips, fpsChips;
    private String screenName = "home";
    private Ui.Chip[] partChips;
    private BufferedImage logo, logo36, logo32, logo40, bgSrc;
    private final Map<Integer, BufferedImage> bgCache = new HashMap<>();

    /** Фон по принципу "cover", заранее отмасштабированный под размер окна (с учётом масштаба экрана). */
    private BufferedImage background(int w, int h) {
        if (bgSrc == null) return null;
        return bgCache.computeIfAbsent(w * 10000 + h, k -> {
            double dpi = Ui.dpi();
            int pw = (int) Math.ceil(w * dpi), ph = (int) Math.ceil(h * dpi);
            double sc = Math.max((double) pw / bgSrc.getWidth(), (double) ph / bgSrc.getHeight());
            int iw = (int) Math.ceil(bgSrc.getWidth() * sc), ih = (int) Math.ceil(bgSrc.getHeight() * sc);
            BufferedImage src = bgSrc;
            while (src.getWidth() / 2 >= iw) { // ступенчатое уменьшение: быстро и без «лесенки»
                BufferedImage half = new BufferedImage(src.getWidth() / 2, src.getHeight() / 2, BufferedImage.TYPE_INT_RGB);
                Graphics2D hg = half.createGraphics();
                hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                hg.drawImage(src, 0, 0, half.getWidth(), half.getHeight(), null);
                hg.dispose();
                src = half;
            }
            BufferedImage out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, (pw - iw) / 2, (ph - ih) / 2, iw, ih, null);
            g.setColor(new Color(0, 0, 0, 40));
            g.fillRect(0, 0, pw, ph);
            g.dispose();
            return out;
        });
    }

    /** Нижние слои окна: заливка, фоновая картинка, частицы. */
    private void paintBackdrop(Graphics2D g, int w, int h) {
        g.setColor(Ui.BG);
        g.fillRect(0, 0, w, h);
        BufferedImage bg = background(W, H);
        if (bg != null) {
            Graphics2D bgG = (Graphics2D) g.create();
            bgG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            bgG.drawImage(bg, 0, 0, W, H, null);
            bgG.dispose();
        }
        particles.paint(g, W, H);
    }

    /**
     * Настройка из launcher.json: сначала файл рядом с лаунчером (только непустые значения),
     * затем вшитый в лаунчер res/launcher.json.
     */
    private String launcherConfig(String key) {
        try {
            Path f = appDir.resolve("launcher.json");
            if (Files.exists(f)) {
                String v = Json.str(Json.obj(Json.parse(Files.readString(f))), key);
                if (v != null && !v.isBlank()) return v;
            }
        } catch (Exception ignored) { }
        try (java.io.InputStream in = Main.class.getResourceAsStream("/launcher.json")) {
            if (in != null) return Json.str(Json.obj(Json.parse(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))), key);
        } catch (Exception ignored) { }
        return null;
    }

    private void build() {
        loadSettings();
        msa = new MicrosoftAuth(launcherConfig("msClientId"), root);
        if (System.getProperty("gl.particles") != null) settings.put("particles", System.getProperty("gl.particles"));
        particles.setKind(Particles.Kind.of(Json.str(settings, "particles")));
        javax.imageio.ImageIO.scanForPlugins();
        try { logo = javax.imageio.ImageIO.read(Main.class.getResource("/icon.png")); } catch (Exception ignored) { }
        if (logo != null) { logo36 = Ui.roundIcon(logo, 36, 10); logo32 = Ui.roundIcon(logo, 32, 8); logo40 = Ui.roundIcon(logo, 40, 10); }
        try { bgSrc = javax.imageio.ImageIO.read(Main.class.getResource("/background.png")); } catch (Exception ignored) { }
        rootPanel = new LayeredRoot(this::paintBackdrop);
        // фон для обоих размеров окна готовится заранее в фоне, а не в момент отрисовки
        Thread bgThread = new Thread(() -> { background(AUTH_W, AUTH_H); background(APP_W, APP_H); }, "gl-bg");
        bgThread.setDaemon(true);
        bgThread.start();
        rootPanel.setOpaque(true);

        authScreen = screen(0, 0, AUTH_W, AUTH_H);
        appScreen = screen(0, 0, APP_W, APP_H);
        homeScreen = screen(SIDE, 0, APP_W - SIDE, APP_H);
        clientsScreen = screen(SIDE, 0, APP_W - SIDE, APP_H);
        settingsScreen = screen(SIDE, 0, APP_W - SIDE, APP_H);
        accountScreen = screen(SIDE, 0, APP_W - SIDE, APP_H);
        buildAuth();
        buildSidebar();
        buildHome();
        buildClients();
        buildSettings();
        buildAccount();
        modsScreen = new ModsPanel(root, () -> clients.get(clientIdx), SIDE, APP_W - SIDE, APP_H, CX, CWD);
        modsScreen.setVisible(false);
        for (JPanel p : List.of(homeScreen, clientsScreen, modsScreen, settingsScreen, accountScreen)) appScreen.add(p);
        rootPanel.add(authScreen);
        rootPanel.add(appScreen);

        closeBtn = new Ui.IconBtn(Ui.Icon.CLOSE, () -> System.exit(0));
        rootPanel.add(closeBtn);
        rootPanel.setComponentZOrder(closeBtn, 0);
        applySize(false);
    }

    private Ui.Screen screen(int x, int y, int w, int h) {
        Ui.Screen p = new Ui.Screen();
        p.setOpaque(false);
        p.setBounds(x, y, w, h);
        p.setVisible(false);
        return p;
    }

    private void applySize(boolean app) {
        int nw = app ? APP_W : AUTH_W, nh = app ? APP_H : AUTH_H;
        if (nw == W && nh == H && rootPanel.getWidth() == W) return;
        W = nw;
        H = nh;
        rootPanel.setSize(W, H);
        closeBtn.setBounds(W - 52, 18, 30, 30);
        if (frame != null) {
            Rectangle b = frame.getBounds();
            int cx = b.x + b.width / 2, cy = b.y + b.height / 2;
            frame.setBounds(cx - W / 2, cy - H / 2, W, H);
            if (System.getProperty("gl.noshape") == null) frame.setShape(new RoundRectangle2D.Float(0, 0, W, H, 22, 22));
            frame.validate();
        }
    }

    // ----- авторизация -----
    private void buildAuth() {
        authTitle = Ui.label("Авторизация", Ui.font(Font.BOLD, 24f), Ui.TEXT, SwingConstants.LEFT);
        authTitle.setBounds(M, 92, CW, 34);
        authSub = Ui.label("Войдите, чтобы попасть в свой профиль", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        authSub.setBounds(M, 130, CW, 22);
        loginField = new Ui.Field("Введите ваш логин или почту", false);
        passField = new Ui.Field("Введите пароль", true);
        pass2Field = new Ui.Field("Повторите введённый пароль", true);
        authError = Ui.label("", Ui.font(Font.PLAIN, 13f), Ui.ERR, SwingConstants.LEFT);
        authError.setBounds(M, 340, CW, 20);
        authBtn = new Ui.GradButton("Войти", false, this::submitAuth);
        authBtn.setBounds(M, 380, CW, 52);
        switchLink = new Ui.Link("Нет аккаунта? Регистрация", () -> { registerMode = !registerMode; layoutAuth(); });
        switchLink.setBounds(M, 452, CW, 22);
        forgotLink = new Ui.Link("Забыли пароль", () -> JOptionPane.showMessageDialog(frame,
                "Аккаунты хранятся локально.\nДля сброса удалите файл:\n" + root.resolve("accounts.json"),
                "Сброс пароля", JOptionPane.INFORMATION_MESSAGE));
        forgotLink.setBounds(M, 478, CW, 22);
        for (Ui.Field f : List.of(loginField, passField, pass2Field))
            f.input.addActionListener(e -> submitAuth());
        for (Component c : new Component[]{authTitle, authSub, loginField, passField, pass2Field, authError, authBtn, switchLink, forgotLink})
            authScreen.add(c);
        layoutAuth();
    }

    private void layoutAuth() {
        authError.setText("");
        if (registerMode) {
            authTitle.setText("Регистрация");
            authSub.setText("Создайте локальный аккаунт лаунчера");
            loginField.setBounds(M, 172, CW, 48);
            passField.setBounds(M, 236, CW, 48);
            pass2Field.setBounds(M, 300, CW, 48);
            pass2Field.setVisible(true);
            authError.setBounds(M, 352, CW, 20);
            authBtn.setText("Создать аккаунт");
            switchLink.setText("Уже есть аккаунт? Войти");
        } else {
            authTitle.setText("Авторизация");
            authSub.setText("Войдите, чтобы попасть в свой профиль");
            loginField.setBounds(M, 180, CW, 48);
            passField.setBounds(M, 264, CW, 48);
            pass2Field.setVisible(false);
            authError.setBounds(M, 330, CW, 20);
            authBtn.setText("Войти");
            switchLink.setText("Нет аккаунта? Регистрация");
        }
        forgotLink.setVisible(!registerMode);
        authScreen.repaint();
    }

    private void submitAuth() {
        String err = registerMode
                ? accounts.register(loginField.text(), passField.text(), pass2Field.text())
                : accounts.login(loginField.text(), passField.text());
        if (err != null) { authError.setText(err); return; }
        passField.input.setText("");
        pass2Field.input.setText("");
        registerMode = false;
        layoutAuth();
        show("home");
    }

    // ----- боковое меню -----
    private static final String[][] NAV = {{"home", "Главная"}, {"clients", "Клиенты"}, {"mods", "Модификаторы"}, {"settings", "Настройки"}, {"account", "Аккаунт"}};

    private void buildSidebar() {
        JLabel brand = Ui.label("GuardListen", Ui.font(Font.BOLD, 17f), Ui.TEXT, SwingConstants.LEFT);
        brand.setBounds(68, 28, 130, 40);
        JPanel side = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 90));
                g.fillRect(0, 0, SIDE, getHeight());
                g.setColor(new Color(255, 255, 255, 28));
                g.fillRect(SIDE - 1, 0, 1, getHeight());
            }
        };
        side.setOpaque(false);
        side.setBounds(0, 0, SIDE, APP_H);
        appScreen.add(side);
        Ui.Avatar logoIcon = new Ui.Avatar() {
            @Override protected void paintComponent(Graphics g0) {
                if (logo36 != null) g0.drawImage(logo36, 0, 0, 36, 36, null);
            }
        };
        logoIcon.setBounds(20, 30, 36, 36);
        side.add(logoIcon);
        side.add(brand);

        nav = new Ui.NavItem[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            String id = NAV[i][0];
            nav[i] = new Ui.NavItem(NAV[i][1], () -> show(id));
            nav[i].setBounds(14, 100 + i * 46, SIDE - 28, 40);
            side.add(nav[i]);
        }

        sideAvatar = new Ui.Avatar();
        sideAvatar.setBounds(18, APP_H - 70, 44, 44);
        sideName = Ui.label("", Ui.font(Font.BOLD, 14f), Ui.TEXT, SwingConstants.LEFT);
        sideName.setBounds(72, APP_H - 68, 130, 22);
        sideRole = Ui.label("", Ui.font(Font.BOLD, 12f), Ui.ORANGE, SwingConstants.LEFT);
        sideRole.setBounds(72, APP_H - 46, 130, 18);
        side.add(sideAvatar);
        side.add(sideName);
        side.add(sideRole);
    }

    // ----- главная -----
    private void buildHome() {
        clientTitle = Ui.label("", Ui.font(Font.BOLD, 22f), Ui.TEXT, SwingConstants.LEFT);
        clientTitle.setBounds(CX, 28, CWD - 40, 40);
        if (logo != null) {
            clientTitle.setIcon(new ImageIcon(logo32) {
                @Override public int getIconWidth() { return 32; }
                @Override public int getIconHeight() { return 32; }
                @Override public void paintIcon(Component c, Graphics g, int x, int y) { g.drawImage(logo32, x, y, 32, 32, null); }
            });
            clientTitle.setIconTextGap(12);
        }
        desc = new JTextArea();
        desc.setEditable(false);
        desc.setFocusable(false);
        desc.setOpaque(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(Ui.font(Font.PLAIN, 16f));
        desc.setForeground(Ui.MUTED);
        desc.setBounds(CX, 112, CWD - 16, 200);
        buildLabel = Ui.label("", Ui.font(Font.PLAIN, 13f), Ui.MUTED, SwingConstants.LEFT);
        buildLabel.setBounds(CX + 44, 72, CWD - 44, 20);
        homeScreen.add(buildLabel);
        status = Ui.label("", Ui.font(Font.PLAIN, 13f), Ui.MUTED, SwingConstants.LEFT);
        status.setBounds(CX, APP_H - 152, CWD, 20);
        bar = new Ui.Bar();
        bar.setBounds(CX, APP_H - 126, CWD, 14);
        bar.setVisible(false);
        launchBtn = new Ui.GradButton("ЗАПУСТИТЬ", true, this::launch);
        launchBtn.setBounds(CX, APP_H - 100, CWD, 52);
        for (Component c : new Component[]{clientTitle, desc, status, bar, launchBtn}) homeScreen.add(c);
        refreshClient();
    }

    /** Время последней сборки, с которой уже запускали игру. */
    private long launchedBuild() { Object o = settings.get("lastBuild"); return o instanceof Number ? ((Number) o).longValue() : 0; }

    /** Обновляет строку «Сборка от …» по самому свежему jar клиента. */
    private void refreshBuild() {
        Path jar = clients.get(clientIdx).resolveJar(appDir, root);
        if (jar == null) {
            buildLabel.setForeground(Ui.ERR);
            buildLabel.setText("Сборка клиента не найдена");
            shownBuild = 0;
            return;
        }
        try {
            long t = Files.getLastModifiedTime(jar).toMillis();
            long mb = Math.round(Files.size(jar) / 1048576.0);
            String when = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(t));
            boolean fresh = launchedBuild() != 0 && t > launchedBuild();
            buildLabel.setForeground(fresh ? Color.WHITE : Ui.MUTED);
            buildLabel.setText((fresh ? "\u25CF Новая сборка от " : "Сборка от ") + when + "  \u00b7  " + mb + " МБ");
            shownBuild = t;
        } catch (Exception ignored) { }
    }

    /** Следит за папками сборок: новый билд сразу отображается в открытом лаунчере. */
    private void startBuildWatcher() {
        Thread w = new Thread(() -> {
            while (true) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
                try {
                    Path jar = clients.get(clientIdx).resolveJar(appDir, root);
                    long t = jar == null ? 0 : Files.getLastModifiedTime(jar).toMillis();
                    if (t != shownBuild) SwingUtilities.invokeLater(this::refreshBuild);
                } catch (Exception ignored) { }
            }
        }, "gl-build-watch");
        w.setDaemon(true);
        w.start();
    }

    private void refreshClient() {
        Client c = clients.get(clientIdx);
        clientTitle.setText(c.name + " " + c.mc + " Premium");
        if (buildLabel != null) refreshBuild();
        desc.setText(c.description);
    }

    // ----- клиенты -----
    private void buildClients() {
        JLabel t = Ui.label("Клиенты", Ui.font(Font.BOLD, 22f), Ui.TEXT, SwingConstants.LEFT);
        t.setBounds(CX, 28, CWD, 40);
        JLabel sub = Ui.label("Выберите клиент для запуска", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        sub.setBounds(CX, 68, CWD, 22);
        clientList = new JPanel(null);
        clientList.setOpaque(false);
        clientList.setBounds(CX, 110, CWD, APP_H - 140);
        clientsScreen.add(t);
        clientsScreen.add(sub);
        clientsScreen.add(clientList);
        refreshClients();
    }

    private void refreshClients() {
        clientList.removeAll();
        for (int i = 0; i < clients.size(); i++) {
            int idx = i;
            Client c = clients.get(i);
            Ui.Card card = new Ui.Card(c.name + " " + c.mc + " Premium", "Fabric " + c.loader, logo40, () -> {
                if (busy) return;
                clientIdx = idx;
                refreshClient();
                refreshClients();
                show("home");
            });
            card.selected = i == clientIdx;
            card.setBounds(0, i * 82, CWD, 70);
            clientList.add(card);
        }
        clientList.repaint();
    }

    // ----- настройки -----
    private void buildSettings() {
        JLabel t = Ui.label("Настройки", Ui.font(Font.BOLD, 22f), Ui.TEXT, SwingConstants.LEFT);
        t.setBounds(CX, 28, CWD, 40);
        JLabel nl = Ui.label("Игровой ник", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        nl.setBounds(CX, 96, CWD, 20);
        nickField = new Ui.Field("Ник в игре", false);
        nickField.setBounds(CX, 122, CWD, 48);

        JLabel rl = Ui.label("Оперативная память", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        rl.setBounds(CX, 194, CWD, 20);
        Ui.IconBtn minus = new Ui.IconBtn(Ui.Icon.LEFT, () -> { settings.put("ram", Math.max(2, ram() - 1)); refreshRam(); });
        Ui.IconBtn plus = new Ui.IconBtn(Ui.Icon.RIGHT, () -> { settings.put("ram", Math.min(16, ram() + 1)); refreshRam(); });
        minus.setBounds(CX, 220, 30, 30);
        plus.setBounds(CX + CWD - 30, 220, 30, 30);
        ramLabel = Ui.label("", Ui.font(Font.BOLD, 16f), Ui.TEXT, SwingConstants.CENTER);
        ramLabel.setBounds(CX + 36, 220, CWD - 72, 30);

        JLabel pl = Ui.label("Частицы на фоне", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        pl.setBounds(CX, 272, CWD, 20);
        settingsScreen.add(pl);
        Particles.Kind[] kinds = Particles.Kind.values();
        partChips = new Ui.Chip[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            Particles.Kind k = kinds[i];
            partChips[i] = new Ui.Chip(k.title, () -> setParticles(k));
            partChips[i].setBounds(CX + i * 132, 298, 120, 40);
            settingsScreen.add(partChips[i]);
            partChips[i].snap(k == particles.kind());
        }
        JLabel dl = Ui.label("Статус в Discord", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        dl.setBounds(CX, 356, CWD, 20);
        settingsScreen.add(dl);
        boolean rpcOn = !"off".equals(Json.str(settings, "discord"));
        rpcChips = new Ui.Chip[]{
                new Ui.Chip("Вкл", () -> setRpc(true)),
                new Ui.Chip("Выкл", () -> setRpc(false))};
        for (int i = 0; i < 2; i++) {
            rpcChips[i].setBounds(CX + i * 132, 382, 120, 40);
            rpcChips[i].snap(i == 0 ? rpcOn : !rpcOn);
            settingsScreen.add(rpcChips[i]);
        }
        JLabel fl = Ui.label("FPS в игре", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        fl.setBounds(CX, 440, CWD, 20);
        settingsScreen.add(fl);
        boolean unl = unlimitedFps();
        fpsChips = new Ui.Chip[]{
                new Ui.Chip("Без лимита", () -> setFps(true)),
                new Ui.Chip("Как в игре", () -> setFps(false))};
        for (int i = 0; i < 2; i++) {
            fpsChips[i].setBounds(CX + i * 152, 466, 140, 40);
            fpsChips[i].snap(i == 0 ? unl : !unl);
            settingsScreen.add(fpsChips[i]);
        }
        Ui.GradButton save = new Ui.GradButton("Сохранить", false, () -> {
            String n = nickField.text().trim();
            if (n.matches("[A-Za-z0-9_]{3,16}")) settings.put("nick", n);
            else if (n.isEmpty()) settings.remove("nick");
            else { JOptionPane.showMessageDialog(frame, "Ник: 3–16 символов, латиница, цифры и _"); return; }
            saveSettings();
            refreshUser();
            show("home");
        });
        save.setBounds(CX, 540, CWD, 52);

        Ui.Link folder = new Ui.Link("Открыть папку игры", () -> {
            try {
                Path d = root.resolve("instances").resolve(clients.get(clientIdx).id);
                Files.createDirectories(d);
                Desktop.getDesktop().open(d.toFile());
            } catch (Exception ignored) { }
        });
        folder.setBounds(CX, 612, CWD, 22);
        for (Component c : new Component[]{t, nl, nickField, rl, minus, plus, ramLabel, save, folder}) settingsScreen.add(c);
        refreshRam();
    }

    private boolean unlimitedFps() { return !"game".equals(Json.str(settings, "fps")); }

    private void setFps(boolean unlimited) {
        settings.put("fps", unlimited ? "unlimited" : "game");
        saveSettings();
        fpsChips[0].setSelected(unlimited);
        fpsChips[1].setSelected(!unlimited);
    }

    /**
     * «Без лимита»: выключает вертикальную синхронизацию (она держит FPS на частоте монитора)
     * и ставит ограничитель кадров в «Неограниченно» (260 в options.txt).
     */
    private static void applyUnlimitedFps(Path gameDir) {
        Path f = gameDir.resolve("options.txt");
        Map<String, String> want = new LinkedHashMap<>();
        want.put("enableVsync", "false");
        want.put("maxFps", "260");
        try {
            List<String> lines = Files.exists(f) ? new ArrayList<>(Files.readAllLines(f)) : new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String key = lines.get(i).split(":", 2)[0];
                if (want.containsKey(key)) lines.set(i, key + ":" + want.remove(key));
            }
            for (Map.Entry<String, String> e : want.entrySet()) lines.add(e.getKey() + ":" + e.getValue());
            Files.write(f, lines);
        } catch (Exception ignored) { }
    }

    private void setRpc(boolean on) {
        settings.put("discord", on ? "on" : "off");
        saveSettings();
        discord.setEnabled(on);
        rpcChips[0].setSelected(on);
        rpcChips[1].setSelected(!on);
    }

    /** Текст статуса Discord для текущего раздела. */
    private void presence() {
        String n = screenName;
        if (n.equals("auth")) { discord.set("В лаунчере GuardListen", "Входит в аккаунт"); return; }
        String st;
        switch (n) {
            case "clients": st = "Выбирает клиент"; break;
            case "mods": st = "Подбирает модификаторы"; break;
            case "settings": st = "В настройках"; break;
            case "account": st = "В профиле"; break;
            default: st = "Готовится к игре";
        }
        discord.set("В лаунчере \u2022 Ник: " + nick(), st);
    }

    private void setParticles(Particles.Kind k) {
        particles.setKind(k);
        startParticles();
        settings.put("particles", k.id);
        saveSettings();
        for (int i = 0; i < partChips.length; i++) partChips[i].setSelected(Particles.Kind.values()[i] == k);
        rootPanel.repaint();
    }

    private void refreshRam() { ramLabel.setText(ram() + " ГБ"); }

    // ----- аккаунт -----
    private void buildAccount() {
        JLabel t = Ui.label("Аккаунт", Ui.font(Font.BOLD, 22f), Ui.TEXT, SwingConstants.LEFT);
        t.setBounds(CX, 28, CWD, 40);
        accAvatar = new Ui.Avatar();
        accAvatar.setBounds(CX, 100, 84, 84);
        accName = Ui.label("", Ui.font(Font.BOLD, 20f), Ui.TEXT, SwingConstants.LEFT);
        accName.setBounds(CX + 104, 110, CWD - 104, 28);
        accRole = Ui.label("", Ui.font(Font.BOLD, 14f), Ui.ORANGE, SwingConstants.LEFT);
        accRole.setBounds(CX + 104, 142, CWD - 104, 22);
        accLogin = Ui.label("", Ui.font(Font.PLAIN, 13f), Ui.MUTED, SwingConstants.LEFT);
        accLogin.setBounds(CX + 104, 164, CWD - 104, 20);
        JLabel al = Ui.label("Аватар", Ui.font(Font.BOLD, 15f), Ui.TEXT, SwingConstants.LEFT);
        al.setBounds(CX, 224, CWD, 22);
        accountScreen.add(al);
        choices = new Ui.AvatarChoice[Avatars.IDS.length];
        for (int i = 0; i < choices.length; i++) {
            String id = Avatars.IDS[i];
            choices[i] = new Ui.AvatarChoice(id, () -> chooseAvatar(id));
            choices[i].setBounds(CX + i * 82 - 6, 252, 76, 76);
            accountScreen.add(choices[i]);
        }
        Ui.GradButton upload = new Ui.GradButton("Загрузить своё фото", false, this::uploadAvatar).ghost();
        upload.setBounds(CX, 346, 220, 40);
        Ui.GradButton reset = new Ui.GradButton("Сбросить", false, () -> chooseAvatar(null)).ghost();
        reset.setBounds(CX + 232, 346, 120, 40);
        Ui.GradButton out = new Ui.GradButton("Выйти из аккаунта", false, () -> { accounts.logout(); show("auth"); });
        out.setBounds(CX, 596, CWD, 52);
        JLabel ml = Ui.label("Лицензия Minecraft", Ui.font(Font.BOLD, 15f), Ui.TEXT, SwingConstants.LEFT);
        ml.setBounds(CX, 412, CWD, 22);
        msStatus = Ui.label("", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        msStatus.setBounds(CX, 438, CWD, 20);
        msCode = Ui.label("", Ui.font(Font.BOLD, 28f), Color.WHITE, SwingConstants.LEFT);
        msCode.setBounds(CX, 462, CWD, 38);
        msBtn = new Ui.GradButton("Войти через Microsoft", false, this::msClick).ghost();
        msBtn.setBounds(CX, 508, 280, 44);
        msOpen = new Ui.Link("Открыть страницу входа", () -> openUrl(msDc != null ? msDc.url : "https://www.microsoft.com/link"));
        msOpen.setBounds(CX + 300, 519, 260, 22);
        for (Component c : new Component[]{ml, msStatus, msCode, msBtn, msOpen}) accountScreen.add(c);
        refreshMs();
        for (Component c : new Component[]{t, accAvatar, accName, accRole, accLogin, upload, reset, out}) accountScreen.add(c);
        loadCustomAvatar();
    }

    // ---------- вход через Microsoft ----------
    private void refreshMs() {
        msOpen.setVisible(msDc != null);
        msCode.setText(msDc != null ? msDc.userCode : "");
        if (!msa.configured()) {
            msStatus.setForeground(Ui.MUTED);
            msStatus.setText("Вход через Microsoft не настроен — игра запускается без проверки лицензии");
            msBtn.setVisible(false);
            return;
        }
        msBtn.setVisible(true);
        if (msDc != null) {
            msStatus.setForeground(Ui.TEXT);
            msStatus.setText("Введите этот код на странице Microsoft (он уже скопирован):");
            msBtn.setText("Отмена");
        } else if (msBusy) {
            msStatus.setForeground(Ui.MUTED);
            msStatus.setText("Подключение к Microsoft...");
            msBtn.setText("Подождите...");
        } else if (msa.linked()) {
            msStatus.setForeground(Ui.TEXT);
            msStatus.setText("\u2714 Лицензия подтверждена: " + msa.name());
            msBtn.setText("Выйти из Microsoft");
        } else {
            msStatus.setForeground(msErr != null ? Ui.ERR : Ui.MUTED);
            msStatus.setText(msErr != null ? msErr : "Для игры нужен аккаунт Microsoft с купленным Minecraft: Java Edition");
            msBtn.setText("Войти через Microsoft");
        }
        accountScreen.repaint();
    }

    private void msClick() {
        if (msDc != null) { msCancel = true; msDc = null; refreshMs(); return; }
        if (msBusy) return;
        if (msa.linked()) {
            msa.logout();
            msErr = null;
            refreshMs();
            refreshUser();
            presence();
            return;
        }
        msBusy = true;
        msCancel = false;
        msErr = null;
        refreshMs();
        new Thread(() -> {
            try {
                MicrosoftAuth.DeviceCode dc = msa.requestCode();
                SwingUtilities.invokeLater(() -> {
                    msBusy = false;
                    msDc = dc;
                    try {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(dc.userCode), null);
                    } catch (Exception ignored) { }
                    openUrl(dc.url);
                    refreshMs();
                });
                msa.waitForLogin(dc, () -> msCancel);
                SwingUtilities.invokeLater(() -> {
                    msDc = null;
                    refreshMs();
                    refreshUser();
                    presence();
                });
            } catch (Exception e) {
                String m = e instanceof MicrosoftAuth.AuthException ? e.getMessage() : "Нет соединения с Microsoft";
                SwingUtilities.invokeLater(() -> {
                    msBusy = false;
                    msDc = null;
                    if (!msCancel) msErr = m;
                    refreshMs();
                });
            }
        }, "gl-msa").start();
    }

    private static void openUrl(String url) {
        try { Desktop.getDesktop().browse(java.net.URI.create(url)); } catch (Exception ignored) { }
    }

    private void applyAvatar(Ui.Avatar a) {
        String id = Json.str(settings, "avatar");
        if ("custom".equals(id) && customAvatar != null) { a.art = null; a.setCustom(customAvatar); }
        else { a.setCustom(null); a.art = id; }
        a.repaint();
    }

    private void loadCustomAvatar() {
        try {
            Path f = root.resolve("avatar.png");
            if (Files.exists(f)) customAvatar = javax.imageio.ImageIO.read(f.toFile());
        } catch (Exception ignored) { }
    }

    private void chooseAvatar(String id) {
        if (id == null) settings.remove("avatar"); else settings.put("avatar", id);
        saveSettings();
        refreshUser();
    }

    private void uploadAvatar() {
        FileDialog fd = new FileDialog(frame, "Выберите изображение", FileDialog.LOAD);
        fd.setFilenameFilter((d, n) -> n.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|bmp|webp)$"));
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage src = javax.imageio.ImageIO.read(new File(fd.getDirectory(), fd.getFile()));
            if (src == null) { JOptionPane.showMessageDialog(frame, "Не удалось прочитать изображение"); return; }
            int side = Math.min(src.getWidth(), src.getHeight());
            BufferedImage sq = src.getSubimage((src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side);
            int out = Math.min(side, 512);
            BufferedImage img = new BufferedImage(out, out, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(sq, 0, 0, out, out, null);
            g.dispose();
            Files.createDirectories(root);
            javax.imageio.ImageIO.write(img, "png", root.resolve("avatar.png").toFile());
            customAvatar = img;
            chooseAvatar("custom");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Ошибка: " + e.getMessage());
        }
    }

    private void refreshUser() {
        Accounts.User u = accounts.session();
        if (u == null) return;
        String n = nick();
        String letter = n.substring(0, 1).toUpperCase();
        sideName.setText(n);
        sideRole.setText(u.role);
        sideAvatar.letter = letter;
        accName.setText(n);
        accRole.setText(u.role);
        accLogin.setText("Логин: " + u.login);
        accAvatar.letter = letter;
        applyAvatar(sideAvatar);
        applyAvatar(accAvatar);
        if (choices != null) for (Ui.AvatarChoice c : choices) c.selected = c.id.equals(Json.str(settings, "avatar"));
        appScreen.repaint();
    }

    private Ui.Screen contentOf(String name) {
        switch (name) {
            case "clients": return clientsScreen;
            case "mods": return modsScreen;
            case "settings": return settingsScreen;
            case "account": return accountScreen;
            default: return homeScreen;
        }
    }

    private Ui.Screen visibleContent() {
        for (Ui.Screen s : new Ui.Screen[]{homeScreen, clientsScreen, modsScreen, settingsScreen, accountScreen})
            if (s.isVisible()) return s;
        return null;
    }

    /** Мгновенно завершает текущий переход между разделами (если он идёт). */
    private void finishTransition() {
        if (transFrom != null) { transFrom.setVisible(false); transFrom.alpha = 1f; transFrom.dy = 0f; }
        if (transTo != null) { transTo.alpha = 1f; transTo.dy = 0f; }
        transFrom = transTo = null;
        transTicker = null;
    }

    private void show(String name) {
        if (frame == null || !frame.isVisible()) { doShow(name); return; }
        boolean app = !name.equals("auth");
        boolean sameMode = app && appScreen.isVisible();
        finishTransition();
        Ui.Screen from = sameMode ? visibleContent() : null;
        Ui.Screen to = sameMode ? contentOf(name) : (app ? appScreen : authScreen);
        if (from == to) return;
        doShow(name);
        if (from != null) from.setVisible(true); // старый раздел плавно гаснет, новый проявляется
        animate(from, to, sameMode ? 14f : 0f, sameMode ? 0.28f : 0.36f);
    }

    private void animate(Ui.Screen from, Ui.Screen to, float shift, float dur) {
        transFrom = from;
        transTo = to;
        to.alpha = 0f;
        to.dy = shift;
        float[] t = {0f};
        Anim.Ticker tk = new Anim.Ticker() {
            @Override public boolean tick(float dt) {
                if (transTicker != this) return false;
                t[0] += dt;
                float x = Math.min(1f, t[0] / dur);
                float e = Anim.easeOut(x);
                to.alpha = e;
                to.dy = (1f - e) * shift;
                if (from != null) from.alpha = 1f - Anim.easeOut(Math.min(1f, x * 2.4f));
                Container par = to.getParent();
                if (par != null && from != null) par.repaint(to.getX(), to.getY(), to.getWidth(), to.getHeight());
                else rootPanel.repaint();
                if (x >= 1f) { finishTransition(); return false; }
                return true;
            }
        };
        transTicker = tk;
        Anim.add(tk);
    }

    private void doShow(String name) {
        boolean app = !name.equals("auth");
        applySize(app);
        authScreen.setVisible(!app);
        appScreen.setVisible(app);
        homeScreen.setVisible(name.equals("home"));
        clientsScreen.setVisible(name.equals("clients"));
        modsScreen.setVisible(name.equals("mods"));
        settingsScreen.setVisible(name.equals("settings"));
        accountScreen.setVisible(name.equals("account"));
        for (int i = 0; i < NAV.length; i++) nav[i].setSelected(NAV[i][0].equals(name));
        if (app) refreshUser();
        if (name.equals("clients")) refreshClients();
        if (name.equals("mods")) modsScreen.onShow();
        if (name.equals("settings")) nickField.input.setText(Json.str(settings, "nick") == null ? "" : Json.str(settings, "nick"));
        screenName = name;
        if (!busy) presence();
        rootPanel.revalidate();
        rootPanel.repaint();
        if (frame != null) frame.repaint();
    }

    private void start() {
        build();
        frame = new JFrame("GuardListen");
        try { frame.setIconImage(javax.imageio.ImageIO.read(Main.class.getResource("/icon.png"))); } catch (Exception ignored) { }
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(rootPanel);
        frame.setSize(AUTH_W, AUTH_H);
        frame.setLocationRelativeTo(null);
        if (System.getProperty("gl.noshape") == null) frame.setShape(new RoundRectangle2D.Float(0, 0, AUTH_W, AUTH_H, 22, 22));
        MouseAdapter drag = new MouseAdapter() {
            Point o;
            public void mousePressed(MouseEvent e) { o = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point p = frame.getLocation();
                frame.setLocation(p.x + e.getX() - o.x, p.y + e.getY() - o.y);
            }
        };
        rootPanel.addMouseListener(drag);
        rootPanel.addMouseMotionListener(drag);
        doShow(accounts.session() != null ? "home" : "auth");
        warmUp();
        discord.setEnabled(!"off".equals(Json.str(settings, "discord")));
        presence();
        discord.start();
        startBuildWatcher();
        rootPanel.winAlpha = 0f;
        frame.setVisible(true);
        float[] t = {0f};
        Anim.add(dt -> {
            t[0] += dt;
            rootPanel.winAlpha = Anim.easeOut(Math.min(1f, t[0] / 0.4f));
            rootPanel.repaintBackdrop();
            return rootPanel.winAlpha < 1f;
        });
        startParticles();
    }

    /**
     * Один раз рисует все разделы в невидимую картинку: Java «прогревает» код отрисовки,
     * шрифты и кэши иконок заранее — первое открытие раздела больше не подвисает.
     */
    private void warmUp() {
        BufferedImage img = new BufferedImage(APP_W, APP_H, BufferedImage.TYPE_INT_ARGB);
        Ui.Screen shown = visibleContent();
        boolean appWas = appScreen.isVisible();
        for (Ui.Screen sc : new Ui.Screen[]{homeScreen, clientsScreen, settingsScreen, accountScreen, modsScreen}) {
            for (Ui.Screen o : new Ui.Screen[]{homeScreen, clientsScreen, modsScreen, settingsScreen, accountScreen}) o.setVisible(o == sc);
            for (int i = 0; i < 3; i++) {
                Graphics2D g = img.createGraphics();
                appScreen.paint(g);
                g.dispose();
            }
        }
        for (Ui.Screen o : new Ui.Screen[]{homeScreen, clientsScreen, modsScreen, settingsScreen, accountScreen}) o.setVisible(o == shown);
        appScreen.setVisible(appWas);
    }

    private final Anim.Ticker particleTicker = dt -> {
        if (!particles.active() || frame == null || !frame.isVisible()) return false;
        particles.step(dt);
        rootPanel.repaintBackdrop();
        return true;
    };

    /** Запускает анимацию частиц (если включены) и выставляет лимит fps под нагрузку. */
    private void startParticles() {
        Anim.cap = particles.active() ? 90 : 144;
        if (particles.active()) Anim.add(particleTicker);
    }

    // ---------- запуск ----------
    private void progress(String text, double f) {
        SwingUtilities.invokeLater(() -> {
            status.setForeground(Ui.MUTED);
            status.setText(text);
            bar.setValue(f);
            discord.set("В лаунчере \u2022 Ник: " + nick(), "Загружает игру: " + (int) Math.round(f * 100) + "%");
        });
    }

    private void launch() {
        if (busy) return;
        busy = true;
        launchBtn.setEnabled(false);
        launchBtn.setText("ПОДГОТОВКА...");
        bar.setVisible(true);
        Client c = clients.get(clientIdx);
        String nick = nick();
        int ram = ram();
        new Thread(() -> {
            try {
                Path jar = c.resolveJar(appDir, root);
                if (jar != null) {
                    long bt = Files.getLastModifiedTime(jar).toMillis();
                    SwingUtilities.invokeLater(() -> { settings.put("lastBuild", bt); saveSettings(); refreshBuild(); });
                }
                if (jar == null) throw new IllegalStateException("Не найден jar клиента. Положите его в clients/" + c.id + ".jar рядом с лаунчером");
                MicrosoftAuth.Session ses = msa.configured() ? msa.session() : null; // без лицензии дальше не пускает
                Installer.Plan plan = new Installer(root).prepare(c, nick, ram, jar, ses, this::progress);
                if (unlimitedFps()) applyUnlimitedFps(plan.gameDir);
                Files.createDirectories(root.resolve("logs"));
                Path log = root.resolve("logs").resolve(c.id + "-latest.log");
                Process p = new ProcessBuilder(plan.command).directory(plan.gameDir.toFile())
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                discord.setPaused(true);
                SwingUtilities.invokeLater(() -> { launchBtn.setText("ИГРА ЗАПУЩЕНА"); frame.setVisible(false); });
                int code = p.waitFor();
                SwingUtilities.invokeLater(() -> {
                    frame.setVisible(true);
                    startParticles();
                    discord.setPaused(false);
                    if (code != 0) {
                        status.setForeground(Ui.ERR);
                        status.setText("Игра завершилась с кодом " + code + ". Лог: " + log);
                    } else status.setText("");
                });
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    status.setForeground(Ui.ERR);
                    String m = String.valueOf(t.getMessage());
                    status.setText(m.length() > 70 ? m.substring(0, 70) + "..." : m);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    busy = false;
                    discord.setPaused(false);
                    presence();
                    launchBtn.setEnabled(true);
                    launchBtn.setText("ЗАПУСТИТЬ");
                    bar.setVisible(false);
                });
            }
        }, "launch").start();
    }

    // ---------- скриншоты экранов (для проверки вёрстки) ----------
    private void shots(Path dir) {
        try {
            Files.createDirectories(dir);
            build();
            for (String s : new String[]{"auth", "home", "clients", "settings", "account"}) {
                show(s);
                if (!s.equals("auth")) {
                    sideName.setText("user"); sideRole.setText("Administrator"); sideAvatar.letter = "U";
                    accName.setText("user"); accRole.setText("Administrator"); accLogin.setText("Логин: user"); accAvatar.letter = "U";
                }
                BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setColor(new Color(0x303030));
                g.fillRect(0, 0, W, H);
                rootPanel.printAll(g);
                g.dispose();
                javax.imageio.ImageIO.write(img, "png", dir.resolve(s + ".png").toFile());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
