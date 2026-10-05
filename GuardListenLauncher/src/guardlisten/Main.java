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
    static final int AUTH_W = 500, AUTH_H = 560, APP_W = 1180, APP_H = 740, TB = 52, M = 44, CW = AUTH_W - 2 * M;
    /** Боковая панель и панель содержимого (под заголовком окна). */
    static final int GAP = 20, BODY_Y = TB + 18, BODY_H = APP_H - BODY_Y - GAP, SIDE_X = GAP, SIDE_W = 262;
    static final int CONT_X = SIDE_X + SIDE_W + 18, CONT_W = APP_W - CONT_X - GAP;
    static final int CX = 32, CWD = CONT_W - 2 * CX;
    static final String LAUNCHER_VERSION = "1.0 Beta";
    private int W = AUTH_W, H = AUTH_H;

    private final Path appDir = detectAppDir();
    private final Path root = Path.of(System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")), "GuardListen");
    private final Accounts accounts = new Accounts(root);
    private final List<Client> clients = Client.load(appDir);
    private final Map<String, Object> settings = new LinkedHashMap<>();

    private JFrame frame;
    private LayeredRoot rootPanel;
    private Ui.Screen authScreen, versionScreen, appScreen, homeScreen, settingsScreen, accountScreen;
    private Wv.Nav[] nav;
    private Wv.Panel contentPanel;
    private ModsPanel modsScreen;
    private PacksPanel packsScreen;
    private Wv.TitleBar titleBar;
    private Wv.Profile profile;
    private Wv.SubCard subCard;
    private BufferedImage deepIcon, monoIcon;
    private Ui.Screen transFrom, transTo;
    private Anim.Ticker transTicker;
    private JLabel accName, accRole, accLogin;
    private Ui.Avatar accAvatar;
    private Ui.AvatarChoice[] choices;
    private BufferedImage customAvatar;
    private int clientIdx;
    private boolean registerMode, busy;

    // auth
    private Ui.Field loginField, passField, pass2Field;
    private JLabel authTitle, authSub, authError;
    private Ui.GradButton authBtn;
    private Ui.Link switchLink, forgotLink;
    // main
    private Wv.InfoGrid infoGrid, clientGrid;
    private JLabel buildLabel;
    private volatile long shownBuild = -1;
    private Wv.ActionBar actionBar;
    private Wv.LightButton launchBtn;
    private String lastError;
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
            for (String scr : new String[]{"versions", "home", "mods", "packs", "settings", "account"}) {
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
    private BufferedImage logo, logo28;

    /** Нижние слои окна: чёрная заливка и частицы. */
    private void paintBackdrop(Graphics2D g, int w, int h) {
        g.setColor(Wv.WIN);
        g.fillRect(0, 0, w, h);
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
        if (logo != null) {
            deepIcon = Wv.deepIcon(logo, 92, 20, true);
            monoIcon = Wv.deepIcon(logo, 92, 20, true);
            logo28 = Ui.roundIcon(deepIcon, 28, 8);
            Ui.Avatar.fallback = deepIcon;
        }
        rootPanel = new LayeredRoot(this::paintBackdrop);
        rootPanel.setOpaque(true);
        clientIdx = initialClient();

        authScreen = screen(0, 0, AUTH_W, AUTH_H);
        versionScreen = screen(0, TB, APP_W, APP_H - TB);
        appScreen = screen(0, 0, APP_W, APP_H);
        contentPanel = new Wv.Panel(20);
        contentPanel.setBounds(CONT_X, BODY_Y, CONT_W, BODY_H);
        homeScreen = screen(0, 0, CONT_W, BODY_H);
        settingsScreen = screen(0, 0, CONT_W, BODY_H);
        accountScreen = screen(0, 0, CONT_W, BODY_H);
        buildAuth();
        buildVersions();
        buildSidebar();
        buildHome();
        buildSettings();
        buildAccount();
        modsScreen = new ModsPanel(root, () -> clients.get(clientIdx), 0, CONT_W, BODY_H, CX, CWD);
        modsScreen.setVisible(false);
        packsScreen = new PacksPanel(root, () -> clients.get(clientIdx), CONT_W, BODY_H, CX, CWD);
        packsScreen.setVisible(false);
        for (JPanel p : List.of(homeScreen, modsScreen, packsScreen, settingsScreen, accountScreen)) contentPanel.add(p);
        appScreen.add(contentPanel);
        rootPanel.add(authScreen);
        rootPanel.add(versionScreen);
        rootPanel.add(appScreen);

        titleBar = new Wv.TitleBar(logo28, "WiosVisuals", () -> frame.setState(Frame.ICONIFIED), () -> System.exit(0));
        rootPanel.add(titleBar);
        rootPanel.setComponentZOrder(titleBar, 0);
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
        titleBar.setBounds(0, 0, W, TB);
        if (frame != null) {
            Rectangle b = frame.getBounds();
            int cx = b.x + b.width / 2, cy = b.y + b.height / 2;
            frame.setBounds(cx - W / 2, cy - H / 2, W, H);
            if (System.getProperty("gl.noshape") == null) frame.setShape(new RoundRectangle2D.Float(0, 0, W, H, 18, 18));
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
        show("versions");
    }

    // ----- выбор версии -----
    /** Сохранённый выбор клиента, если он доступен, иначе первый доступный. */
    private int initialClient() {
        String id = Json.str(settings, "client");
        int first = 0;
        for (int i = clients.size() - 1; i >= 0; i--) {
            if (!clients.get(i).available) continue;
            first = i;
            if (clients.get(i).id.equals(id)) return i;
        }
        return first;
    }

    private void buildVersions() {
        int w = APP_W;
        JLabel t = Ui.label("Выберите версию", Wv.heavy(38f), Wv.TEXT, SwingConstants.CENTER);
        t.setBounds(0, 104, w, 52);
        JLabel sub = Ui.label("Выберите сборку клиента, с которой хотите продолжить.", Wv.plain(15.5f), new Color(0xA3AAB3), SwingConstants.CENTER);
        sub.setBounds(0, 160, w, 24);
        versionScreen.add(t);
        versionScreen.add(sub);
        int n = clients.size(), gap = 8; // у карточек свой отступ 8 px под свечение
        int cw = Math.min(486, (w - 120 - gap * (n - 1)) / n), ch = 316;
        int x0 = (w - (cw * n + gap * (n - 1))) / 2;
        for (int i = 0; i < n; i++) {
            int idx = i;
            Client c = clients.get(i);
            Wv.VersionCard card = new Wv.VersionCard(c, c.available ? deepIcon : monoIcon, () -> {
                if (busy) return;
                clientIdx = idx;
                settings.put("client", clients.get(idx).id);
                saveSettings();
                show("home");
            });
            card.setBounds(x0 + i * (cw + gap), 214, cw, ch);
            versionScreen.add(card);
        }
        Wv.InfoPill pill = new Wv.InfoPill("Для запуска выберите доступную версию клиента");
        int pw = pill.prefWidth();
        pill.setBounds((w - pw) / 2, 566, pw, 40);
        versionScreen.add(pill);
    }

    // ----- боковое меню -----
    private static final String[][] NAV = {{"home", "Главная"}, {"mods", "Моды"}, {"packs", "Ресурспаки"}, {"settings", "Настройки"}};
    private static final Wv.Glyph[] NAV_GLYPH = {Wv.Glyph.DOT, Wv.Glyph.DIAMOND, Wv.Glyph.GRID, Wv.Glyph.GEAR};

    private void buildSidebar() {
        Wv.Panel side = new Wv.Panel(20);
        side.setBounds(SIDE_X, BODY_Y, SIDE_W, BODY_H);
        appScreen.add(side);
        int iw = SIDE_W - 24;
        profile = new Wv.Profile(() -> show("account"));
        profile.setBounds(12, 12, iw, 76);
        side.add(profile);
        JComponent div1 = divider();
        div1.setBounds(24, 98, SIDE_W - 48, 1);
        side.add(div1);
        subCard = new Wv.SubCard();
        subCard.setBounds(14, 114, SIDE_W - 28, 124);
        side.add(subCard);

        nav = new Wv.Nav[NAV.length];
        int y = 256;
        for (int i = 0; i < NAV.length; i++) {
            String id = NAV[i][0];
            if (id.equals("settings")) { // настройки отделены чертой
                JComponent d = divider();
                d.setBounds(24, y + 8, SIDE_W - 48, 1);
                side.add(d);
                y += 22;
            }
            nav[i] = new Wv.Nav(NAV[i][1], NAV_GLYPH[i], () -> show(id));
            nav[i].setBounds(12, y, iw, 50);
            side.add(nav[i]);
            y += 58;
        }
        Wv.OutlineBtn out = new Wv.OutlineBtn("Выйти", Wv.Glyph.EXIT, () -> { accounts.logout(); show("auth"); });
        out.setBounds(14, BODY_H - 14 - 50, SIDE_W - 28, 50);
        side.add(out);
    }

    private static JComponent divider() {
        return new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 22));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
    }

    // ----- главная -----
    private void buildHome() {
        JLabel t = Ui.label("Информация", Ui.font(Font.BOLD, 26f), Wv.TEXT, SwingConstants.LEFT);
        t.setBounds(CX, 26, CWD, 38);
        JLabel sub = Ui.label("Ваш аккаунт и состояние клиента", Wv.semi(14f), new Color(0xA3AAB3), SwingConstants.LEFT);
        sub.setBounds(CX, 62, CWD, 20);
        infoGrid = new Wv.InfoGrid("UUID", "Тип доступа", "Версия лаунчера", "Статус");
        infoGrid.onClick(0, this::copyUuid);
        infoGrid.setBounds(CX, 100, CWD, 136);
        JLabel cl = Ui.label("КЛИЕНТ", Wv.caps(15f, 0.06f), Wv.TEXT, SwingConstants.LEFT);
        cl.setBounds(CX, 258, CWD, 22);
        clientGrid = new Wv.InfoGrid("Версия Minecraft", "Загрузчик", "Защита", "Выделено памяти");
        clientGrid.setBounds(CX, 290, CWD, 136);
        buildLabel = Ui.label("", Ui.font(Font.PLAIN, 13f), Wv.MUTED, SwingConstants.LEFT);
        buildLabel.setBounds(CX + 4, 436, CWD, 20);
        launchBtn = new Wv.LightButton("Установить", Wv.Glyph.DOWNLOAD, this::launch);
        actionBar = new Wv.ActionBar(launchBtn);
        actionBar.setBounds(CX, BODY_H - 28 - 112, CWD, 112);
        for (Component c : new Component[]{t, sub, infoGrid, cl, clientGrid, buildLabel, actionBar}) homeScreen.add(c);
        refreshClient();
    }

    /** Время последней сборки, с которой уже запускали игру. */
    private long launchedBuild() { Object o = settings.get("lastBuild"); return o instanceof Number ? ((Number) o).longValue() : 0; }

    /** Обновляет строку «Сборка от …» по самому свежему jar клиента. */
    private void refreshBuild() {
        Path jar = clients.get(clientIdx).resolveJar(appDir, root);
        if (jar == null) {
            buildLabel.setForeground(Wv.RED);
            buildLabel.setText("Сборка клиента не найдена");
            shownBuild = 0;
            refreshReady();
            return;
        }
        try {
            long t = Files.getLastModifiedTime(jar).toMillis();
            long mb = Math.round(Files.size(jar) / 1048576.0);
            String when = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(t));
            boolean fresh = launchedBuild() != 0 && t > launchedBuild();
            buildLabel.setForeground(fresh ? Wv.BLUE : Wv.MUTED);
            buildLabel.setText((fresh ? "● Новая сборка от " : "Сборка от ") + when + "  ·  " + mb + " МБ");
            shownBuild = t;
        } catch (Exception ignored) { }
        refreshReady();
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

    /** Статус «Готов к запуску» и текст кнопки (Установить / Играть), пока игра не запускается. */
    private void refreshReady() {
        if (busy || actionBar == null) return;
        boolean inst = clients.get(clientIdx).installed(root);
        launchBtn.set(inst ? "Играть" : "Установить", inst ? Wv.Glyph.PLAY : Wv.Glyph.DOWNLOAD);
        if (lastError != null) actionBar.status(lastError, Wv.ActionBar.ERR);
        else if (shownBuild == 0) actionBar.status("Сборка клиента не найдена", Wv.ActionBar.ERR);
        else actionBar.status("Готов к запуску", Wv.ActionBar.OK);
    }

    private void refreshClient() {
        Client c = clients.get(clientIdx);
        Accounts.User u = accounts.session();
        String uuid = u == null ? null : accounts.uuid(u.login);
        infoGrid.set(0, uuid == null ? "—" : uuid, null);
        infoGrid.set(1, plan(), null);
        infoGrid.set(2, LAUNCHER_VERSION, null);
        infoGrid.set(3, "Активен", Wv.GREEN);
        clientGrid.set(0, c.mc, null);
        clientGrid.set(1, "Fabric", null);
        String prot = launcherConfig("protection");
        clientGrid.set(2, prot == null || prot.isBlank() ? "BullProtection" : prot, null);
        clientGrid.set(3, ram() * 1024 + " MB", null);
        if (buildLabel != null) refreshBuild();
    }

    /** Копирует UUID аккаунта в буфер обмена и на секунду показывает «Скопировано». */
    private void copyUuid() {
        Accounts.User u = accounts.session();
        String uuid = u == null ? null : accounts.uuid(u.login);
        if (uuid == null) return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(uuid), null);
        } catch (Exception ignored) { return; }
        infoGrid.set(0, "Скопировано", Wv.GREEN);
        javax.swing.Timer t = new javax.swing.Timer(1200, e -> infoGrid.set(0, uuid, null));
        t.setRepeats(false);
        t.start();
    }

    /** Тариф доступа (из launcher.json, по умолчанию LIFETIME). */
    private String plan() {
        String p = launcherConfig("plan");
        return p == null || p.isBlank() ? "LIFETIME" : p.toUpperCase();
    }

    // ----- настройки -----
    private void buildSettings() {
        JLabel t = Ui.label("Настройки", Ui.font(Font.BOLD, 26f), Ui.TEXT, SwingConstants.LEFT);
        t.setBounds(CX, 26, CWD, 38);
        JLabel nl = Ui.label("Игровой ник", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        nl.setBounds(CX, 82, CWD, 20);
        nickField = new Ui.Field("Ник в игре", false);
        nickField.setBounds(CX, 106, CWD, 46);

        JLabel rl = Ui.label("Оперативная память", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        rl.setBounds(CX, 170, CWD, 20);
        Ui.IconBtn minus = new Ui.IconBtn(Ui.Icon.LEFT, () -> { settings.put("ram", Math.max(2, ram() - 1)); refreshRam(); });
        Ui.IconBtn plus = new Ui.IconBtn(Ui.Icon.RIGHT, () -> { settings.put("ram", Math.min(16, ram() + 1)); refreshRam(); });
        minus.setBounds(CX, 196, 30, 30);
        plus.setBounds(CX + CWD - 30, 196, 30, 30);
        ramLabel = Ui.label("", Ui.font(Font.BOLD, 16f), Ui.TEXT, SwingConstants.CENTER);
        ramLabel.setBounds(CX + 36, 196, CWD - 72, 30);

        JLabel pl = Ui.label("Частицы на фоне", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        pl.setBounds(CX, 244, CWD, 20);
        settingsScreen.add(pl);
        Particles.Kind[] kinds = Particles.Kind.values();
        partChips = new Ui.Chip[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            Particles.Kind k = kinds[i];
            partChips[i] = new Ui.Chip(k.title, () -> setParticles(k));
            partChips[i].setBounds(CX + i * 132, 268, 120, 40);
            settingsScreen.add(partChips[i]);
            partChips[i].snap(k == particles.kind());
        }
        JLabel dl = Ui.label("Статус в Discord", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        dl.setBounds(CX, 324, CWD, 20);
        settingsScreen.add(dl);
        boolean rpcOn = !"off".equals(Json.str(settings, "discord"));
        rpcChips = new Ui.Chip[]{
                new Ui.Chip("Вкл", () -> setRpc(true)),
                new Ui.Chip("Выкл", () -> setRpc(false))};
        for (int i = 0; i < 2; i++) {
            rpcChips[i].setBounds(CX + i * 132, 348, 120, 40);
            rpcChips[i].snap(i == 0 ? rpcOn : !rpcOn);
            settingsScreen.add(rpcChips[i]);
        }
        JLabel fl = Ui.label("FPS в игре", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        fl.setBounds(CX, 404, CWD, 20);
        settingsScreen.add(fl);
        boolean unl = unlimitedFps();
        fpsChips = new Ui.Chip[]{
                new Ui.Chip("Без лимита", () -> setFps(true)),
                new Ui.Chip("Как в игре", () -> setFps(false))};
        for (int i = 0; i < 2; i++) {
            fpsChips[i].setBounds(CX + i * 152, 428, 140, 40);
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
            refreshClient();
            show("home");
        });
        save.setBounds(CX, 500, CWD, 52);

        Ui.Link folder = new Ui.Link("Открыть папку игры", () -> {
            try {
                Path d = root.resolve("instances").resolve(clients.get(clientIdx).id);
                Files.createDirectories(d);
                Desktop.getDesktop().open(d.toFile());
            } catch (Exception ignored) { }
        });
        folder.setBounds(CX, 572, 180, 22);
        Ui.Link version = new Ui.Link("Сменить версию", () -> show("versions"));
        version.setBounds(CX + 200, 572, 180, 22);
        for (Component c : new Component[]{t, nl, nickField, rl, minus, plus, ramLabel, save, folder, version}) settingsScreen.add(c);
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
        if (n.equals("auth")) { discord.set("В лаунчере WiosVisuals", "Входит в аккаунт"); return; }
        String st;
        switch (n) {
            case "versions": st = "Выбирает версию"; break;
            case "mods": st = "Подбирает модификаторы"; break;
            case "packs": st = "Выбирает ресурспаки"; break;
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
        JLabel t = Ui.label("Аккаунт", Ui.font(Font.BOLD, 26f), Ui.TEXT, SwingConstants.LEFT);
        t.setBounds(CX, 26, CWD, 38);
        accAvatar = new Ui.Avatar();
        accAvatar.setBounds(CX, 86, 84, 84);
        accName = Ui.label("", Ui.font(Font.BOLD, 20f), Ui.TEXT, SwingConstants.LEFT);
        accName.setBounds(CX + 104, 96, CWD - 104, 28);
        accRole = Ui.label("", Ui.font(Font.BOLD, 14f), Ui.ORANGE, SwingConstants.LEFT);
        accRole.setBounds(CX + 104, 128, CWD - 104, 22);
        accLogin = Ui.label("", Ui.font(Font.PLAIN, 13f), Ui.MUTED, SwingConstants.LEFT);
        accLogin.setBounds(CX + 104, 150, CWD - 104, 20);
        JLabel al = Ui.label("Аватар", Ui.font(Font.BOLD, 15f), Ui.TEXT, SwingConstants.LEFT);
        al.setBounds(CX, 200, CWD, 22);
        accountScreen.add(al);
        choices = new Ui.AvatarChoice[Avatars.IDS.length];
        for (int i = 0; i < choices.length; i++) {
            String id = Avatars.IDS[i];
            choices[i] = new Ui.AvatarChoice(id, () -> chooseAvatar(id));
            choices[i].setBounds(CX + i * 82 - 6, 228, 76, 76);
            accountScreen.add(choices[i]);
        }
        Ui.GradButton upload = new Ui.GradButton("Загрузить своё фото", false, this::uploadAvatar).ghost();
        upload.setBounds(CX, 318, 220, 40);
        Ui.GradButton reset = new Ui.GradButton("Сбросить", false, () -> chooseAvatar(null)).ghost();
        reset.setBounds(CX + 232, 318, 120, 40);
        Ui.GradButton out = new Ui.GradButton("Выйти из аккаунта", false, () -> { accounts.logout(); show("auth"); });
        out.setBounds(CX, BODY_H - 28 - 52, CWD, 52);
        JLabel ml = Ui.label("Лицензия Minecraft", Ui.font(Font.BOLD, 15f), Ui.TEXT, SwingConstants.LEFT);
        ml.setBounds(CX, 382, CWD, 22);
        msStatus = Ui.label("", Ui.font(Font.PLAIN, 14f), Ui.MUTED, SwingConstants.LEFT);
        msStatus.setBounds(CX, 408, CWD, 20);
        msCode = Ui.label("", Ui.font(Font.BOLD, 28f), Color.WHITE, SwingConstants.LEFT);
        msCode.setBounds(CX, 432, CWD, 38);
        msBtn = new Ui.GradButton("Войти через Microsoft", false, this::msClick).ghost();
        msBtn.setBounds(CX, 478, 280, 44);
        msOpen = new Ui.Link("Открыть страницу входа", () -> openUrl(msDc != null ? msDc.url : "https://www.microsoft.com/link"));
        msOpen.setBounds(CX + 300, 489, 260, 22);
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
        profile.name = n;
        profile.sub = "WiosVisuals";
        profile.avatar.letter = letter;
        subCard.plan = plan();
        profile.repaint();
        subCard.repaint();
        accName.setText(n);
        accRole.setText(u.role);
        accLogin.setText("Логин: " + u.login);
        accAvatar.letter = letter;
        applyAvatar(profile.avatar);
        applyAvatar(accAvatar);
        if (choices != null) for (Ui.AvatarChoice c : choices) c.selected = c.id.equals(Json.str(settings, "avatar"));
        appScreen.repaint();
    }

    private Ui.Screen contentOf(String name) {
        switch (name) {
            case "mods": return modsScreen;
            case "packs": return packsScreen;
            case "settings": return settingsScreen;
            case "account": return accountScreen;
            default: return homeScreen;
        }
    }

    private Ui.Screen[] contents() { return new Ui.Screen[]{homeScreen, modsScreen, packsScreen, settingsScreen, accountScreen}; }

    private Ui.Screen visibleContent() {
        for (Ui.Screen s : contents()) if (s.isVisible()) return s;
        return null;
    }

    /** Режим окна: авторизация, выбор версии или основное окно с меню. */
    private static String modeOf(String name) {
        return name.equals("auth") || name.equals("versions") ? name : "app";
    }

    private Ui.Screen modeScreen(String mode) {
        return mode.equals("auth") ? authScreen : mode.equals("versions") ? versionScreen : appScreen;
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
        String mode = modeOf(name);
        boolean sameMode = mode.equals("app") && appScreen.isVisible();
        finishTransition();
        Ui.Screen from = sameMode ? visibleContent() : null;
        Ui.Screen to = sameMode ? contentOf(name) : modeScreen(mode);
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
        String mode = modeOf(name);
        applySize(!mode.equals("auth"));
        authScreen.setVisible(mode.equals("auth"));
        versionScreen.setVisible(mode.equals("versions"));
        appScreen.setVisible(mode.equals("app"));
        if (mode.equals("app")) for (Ui.Screen sc : contents()) sc.setVisible(sc == contentOf(name));
        for (int i = 0; i < NAV.length; i++) nav[i].setSelected(NAV[i][0].equals(name));
        if (mode.equals("app")) { refreshUser(); refreshClient(); }
        if (name.equals("mods")) modsScreen.onShow();
        if (name.equals("packs")) packsScreen.onShow();
        if (name.equals("settings")) nickField.input.setText(Json.str(settings, "nick") == null ? "" : Json.str(settings, "nick"));
        screenName = name;
        if (!busy) presence();
        rootPanel.revalidate();
        rootPanel.repaint();
        if (frame != null) frame.repaint();
    }

    private void start() {
        build();
        frame = new JFrame("WiosVisuals");
        try { frame.setIconImage(javax.imageio.ImageIO.read(Main.class.getResource("/icon.png"))); } catch (Exception ignored) { }
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(rootPanel);
        frame.setSize(AUTH_W, AUTH_H);
        frame.setLocationRelativeTo(null);
        if (System.getProperty("gl.noshape") == null) frame.setShape(new RoundRectangle2D.Float(0, 0, AUTH_W, AUTH_H, 18, 18));
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
        doShow(accounts.session() != null ? "versions" : "auth");
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
        for (Ui.Screen sc : contents()) {
            for (Ui.Screen o : contents()) o.setVisible(o == sc);
            for (int i = 0; i < 3; i++) {
                Graphics2D g = img.createGraphics();
                appScreen.paint(g);
                g.dispose();
            }
        }
        for (Ui.Screen o : contents()) o.setVisible(o == shown);
        appScreen.setVisible(appWas);
        Graphics2D vg = img.createGraphics();
        versionScreen.paint(vg);
        vg.dispose();
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
            actionBar.status(text, Wv.ActionBar.BUSY);
            actionBar.bar.setValue(f);
            discord.set("В лаунчере • Ник: " + nick(), "Загружает игру: " + (int) Math.round(f * 100) + "%");
        });
    }

    private void launch() {
        if (busy) return;
        busy = true;
        lastError = null;
        launchBtn.setEnabled(false);
        launchBtn.set("Загрузка...", Wv.Glyph.DOWNLOAD);
        actionBar.status("Подготовка...", Wv.ActionBar.BUSY);
        actionBar.progress(true);
        Client c = clients.get(clientIdx);
        String nick = nick();
        int ram = ram();
        new Thread(() -> {
            try {
                Path jar = c.resolveJar(appDir, root);
                if (jar != null) {
                    long bt = Files.getLastModifiedTime(jar).toMillis();
                    SwingUtilities.invokeLater(() -> { settings.put("lastBuild", bt); saveSettings(); });
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
                SwingUtilities.invokeLater(() -> {
                    launchBtn.set("Игра запущена", Wv.Glyph.PLAY);
                    actionBar.status("Игра запущена", Wv.ActionBar.OK);
                    actionBar.progress(false);
                    frame.setVisible(false);
                });
                int code = p.waitFor();
                SwingUtilities.invokeLater(() -> {
                    frame.setVisible(true);
                    startParticles();
                    discord.setPaused(false);
                    if (code != 0) lastError = "Игра завершилась с кодом " + code + ". Лог: " + log;
                });
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> lastError = String.valueOf(t.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    busy = false;
                    discord.setPaused(false);
                    presence();
                    launchBtn.setEnabled(true);
                    actionBar.progress(false);
                    refreshBuild();
                });
            }
        }, "launch").start();
    }

    // ---------- скриншоты экранов (для проверки вёрстки) ----------
    private void shots(Path dir) {
        try {
            Files.createDirectories(dir);
            build();
            for (String s : new String[]{"auth", "versions", "home", "packs", "settings", "account"}) {
                show(s);
                if (!s.equals("auth") && !s.equals("versions")) {
                    profile.name = "Wios"; profile.sub = "WiosVisuals";
                    accName.setText("Wios"); accRole.setText("Administrator"); accLogin.setText("Логин: wios"); accAvatar.letter = "W";
                }
                BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setColor(new Color(0x303030));
                g.fillRect(0, 0, W, H);
                rootPanel.validate();
                if (s.equals("packs")) packsScreen.reload();
                rootPanel.validate();
                rootPanel.printAll(g);
                g.dispose();
                javax.imageio.ImageIO.write(img, "png", dir.resolve(s + ".png").toFile());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
