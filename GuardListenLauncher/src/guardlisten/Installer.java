package guardlisten;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Скачивает Minecraft + Fabric + моды и собирает команду запуска. */
final class Installer {
    interface Listener { void update(String text, double fraction); }

    static final class Plan {
        List<String> command = new ArrayList<>();
        Path gameDir;
    }

    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    private final Path root;
    private final Path libs, assets, versions;

    Installer(Path root) {
        this.root = root;
        libs = root.resolve("libraries");
        assets = root.resolve("assets");
        versions = root.resolve("versions");
    }

    private static final class Task {
        final String url; final Path path; final long size;
        Task(String url, Path path, long size) { this.url = url; this.path = path; this.size = size; }
    }

    static String get(String url) throws Exception {
        return new String(getBytes(url), StandardCharsets.UTF_8);
    }

    static byte[] getBytes(String url) throws Exception {
        Exception last = null;
        for (int i = 0; i < 3; i++) {
            try {
                HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "GuardListen-Launcher/1.0").build(), HttpResponse.BodyHandlers.ofByteArray());
                if (r.statusCode() == 200) return r.body();
                last = new IOException("HTTP " + r.statusCode() + " " + url);
            } catch (Exception e) { last = e; }
        }
        throw last;
    }

    private static void download(Task t) throws Exception {
        if (Files.isRegularFile(t.path) && (t.size <= 0 || Files.size(t.path) == t.size)) return;
        Files.createDirectories(t.path.getParent());
        Path tmp = t.path.resolveSibling(t.path.getFileName() + ".part");
        Files.write(tmp, getBytes(t.url));
        Files.move(tmp, t.path, StandardCopyOption.REPLACE_EXISTING);
    }

    // ---- правила Mojang ----
    private static String arch() {
        String a = System.getProperty("os.arch").toLowerCase();
        if (a.contains("aarch64") || a.contains("arm64")) return "arm64";
        if (a.equals("x86") || a.equals("i386")) return "x86";
        return "x64";
    }

    private static boolean allowed(Object rules) {
        if (rules == null) return true;
        boolean ok = false;
        for (Object r : Json.list(rules)) {
            Map<String, Object> m = Json.obj(r);
            boolean match = !m.containsKey("features");
            if (m.containsKey("os")) {
                Map<String, Object> os = Json.obj(m.get("os"));
                String n = Json.str(os, "name");
                if (n != null && !n.equals("windows")) match = false;
                String a = Json.str(os, "arch");
                if (a != null && !a.equals(arch())) match = false;
            }
            if (match) ok = "allow".equals(Json.str(m, "action"));
        }
        return ok;
    }

    private static List<String> args(Object arr) {
        List<String> out = new ArrayList<>();
        for (Object o : Json.list(arr)) {
            if (o instanceof String) { out.add((String) o); continue; }
            Map<String, Object> m = Json.obj(o);
            if (!allowed(m.get("rules"))) continue;
            Object v = m.get("value");
            if (v instanceof String) out.add((String) v);
            else for (Object x : Json.list(v)) out.add(String.valueOf(x));
        }
        return out;
    }

    private static String mavenPath(String name) {
        String[] p = name.split(":");
        String ext = "jar";
        if (p.length > 3 && p[3].contains("@")) { ext = p[3].split("@")[1]; p[3] = p[3].split("@")[0]; }
        String file = p[1] + "-" + p[2] + (p.length > 3 ? "-" + p[3] : "") + "." + ext;
        return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + file;
    }

    private static String libKey(String name) {
        String[] p = name.split(":");
        return p[0] + ":" + p[1] + (p.length > 3 ? ":" + p[3] : "");
    }

    Plan prepare(Client c, String nick, int ramGb, Path modJar, Listener l) throws Exception {
        return prepare(c, nick, ramGb, modJar, null, l);
    }

    /** @param ses сессия Microsoft (лицензия); null — офлайн-запуск */
    Plan prepare(Client c, String nick, int ramGb, Path modJar, MicrosoftAuth.Session ses, Listener l) throws Exception {
        if (ses != null) nick = ses.name;
        Path gameDir = root.resolve("instances").resolve(c.id);
        Files.createDirectories(gameDir.resolve("mods"));

        l.update("Получение списка версий...", 0.02);
        Path vdir = versions.resolve(c.mc);
        Path vjson = vdir.resolve(c.mc + ".json");
        Map<String, Object> vanilla;
        if (Files.isRegularFile(vjson)) vanilla = Json.obj(Json.parse(Files.readString(vjson)));
        else {
            Map<String, Object> manifest = Json.obj(Json.parse(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")));
            String vurl = null;
            for (Object o : Json.list(manifest.get("versions"))) {
                Map<String, Object> m = Json.obj(o);
                if (c.mc.equals(Json.str(m, "id"))) vurl = Json.str(m, "url");
            }
            if (vurl == null) throw new IOException("Версия Minecraft " + c.mc + " не найдена");
            String txt = get(vurl);
            Files.createDirectories(vdir);
            Files.writeString(vjson, txt);
            vanilla = Json.obj(Json.parse(txt));
        }

        l.update("Получение Fabric Loader " + c.loader + "...", 0.04);
        String fid = "fabric-loader-" + c.loader + "-" + c.mc;
        Path fjson = versions.resolve(fid).resolve(fid + ".json");
        Map<String, Object> fabric;
        if (Files.isRegularFile(fjson)) fabric = Json.obj(Json.parse(Files.readString(fjson)));
        else {
            String txt = get("https://meta.fabricmc.net/v2/versions/loader/" + c.mc + "/" + c.loader + "/profile/json");
            Files.createDirectories(fjson.getParent());
            Files.writeString(fjson, txt);
            fabric = Json.obj(Json.parse(txt));
        }

        List<Task> tasks = new ArrayList<>();
        LinkedHashMap<String, Path> cp = new LinkedHashMap<>();

        for (Map<String, Object> src : List.of(vanilla, fabric)) {
            for (Object o : Json.list(src.get("libraries"))) {
                Map<String, Object> lib = Json.obj(o);
                if (!allowed(lib.get("rules"))) continue;
                String name = Json.str(lib, "name");
                Map<String, Object> art = Json.obj(Json.obj(lib.get("downloads")).get("artifact"));
                String path, url; long size = 0;
                if (!art.isEmpty()) {
                    path = Json.str(art, "path"); url = Json.str(art, "url"); size = Json.num(art, "size");
                } else {
                    path = mavenPath(name);
                    String base = Json.str(lib, "url") != null ? Json.str(lib, "url") : "https://libraries.minecraft.net/";
                    url = base + (base.endsWith("/") ? "" : "/") + path;
                }
                Path p = libs.resolve(path);
                tasks.add(new Task(url, p, size));
                cp.remove(libKey(name));
                cp.put(libKey(name), p);
            }
        }

        Path clientJar = vdir.resolve(c.mc + ".jar");
        Map<String, Object> cl = Json.obj(Json.obj(vanilla.get("downloads")).get("client"));
        tasks.add(new Task(Json.str(cl, "url"), clientJar, Json.num(cl, "size")));
        cp.put("client", clientJar);

        // ассеты
        Map<String, Object> ai = Json.obj(vanilla.get("assetIndex"));
        String aid = Json.str(ai, "id");
        Path aidx = assets.resolve("indexes").resolve(aid + ".json");
        l.update("Получение индекса ресурсов...", 0.06);
        download(new Task(Json.str(ai, "url"), aidx, Json.num(ai, "size")));
        Map<String, Object> objs = Json.obj(Json.obj(Json.parse(Files.readString(aidx))).get("objects"));
        for (Object o : objs.values()) {
            Map<String, Object> m = Json.obj(o);
            String h = Json.str(m, "hash");
            String sub = h.substring(0, 2);
            tasks.add(new Task("https://resources.download.minecraft.net/" + sub + "/" + h,
                    assets.resolve("objects").resolve(sub).resolve(h), Json.num(m, "size")));
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger done = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        int total = tasks.size();
        for (Task t : tasks) fs.add(pool.submit(() -> {
            download(t);
            int d = done.incrementAndGet();
            if (d % 25 == 0 || d == total) l.update("Загрузка файлов игры: " + d + " / " + total, 0.08 + 0.82 * d / total);
            return null;
        }));
        try {
            for (Future<?> f : fs) f.get();
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        } finally { pool.shutdownNow(); }

        // моды
        l.update("Подготовка модов...", 0.92);
        installFabricApi(gameDir.resolve("mods"), c.mc);
        if (modJar != null) {
            Path dst = gameDir.resolve("mods").resolve("wiosvisual.jar");
            if (!Files.exists(dst) || Files.size(dst) != Files.size(modJar)
                    || Files.getLastModifiedTime(dst).compareTo(Files.getLastModifiedTime(modJar)) < 0)
                Files.copy(modJar, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }

        // аргументы
        Path natives = root.resolve("natives").resolve(c.mc);
        Files.createDirectories(natives);
        StringBuilder cps = new StringBuilder();
        for (Path p : cp.values()) { if (cps.length() > 0) cps.append(File.pathSeparator); cps.append(p.toAbsolutePath()); }

        Map<String, String> vars = new HashMap<>();
        vars.put("natives_directory", natives.toAbsolutePath().toString());
        vars.put("launcher_name", "GuardListen");
        vars.put("launcher_version", "1.0");
        vars.put("classpath", cps.toString());
        vars.put("auth_player_name", nick);
        vars.put("version_name", fid);
        vars.put("game_directory", gameDir.toAbsolutePath().toString());
        vars.put("assets_root", assets.toAbsolutePath().toString());
        vars.put("assets_index_name", aid);
        if (ses != null) {
            vars.put("auth_uuid", ses.uuid);
            vars.put("auth_access_token", ses.token);
            vars.put("auth_xuid", ses.xuid);
            vars.put("user_type", "msa");
        } else {
            vars.put("auth_uuid", UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""));
            vars.put("auth_access_token", "0");
            vars.put("auth_xuid", "0");
            vars.put("user_type", "legacy");
        }
        vars.put("clientid", "0");
        vars.put("version_type", "release");

        Plan plan = new Plan();
        plan.gameDir = gameDir;
        plan.command.add(javaBin());
        plan.command.add("-Xmx" + ramGb + "G");
        plan.command.add("-Xms512M");
        Map<String, Object> vargs = Json.obj(vanilla.get("arguments")), fargs = Json.obj(fabric.get("arguments"));
        List<String> jvm = new ArrayList<>(args(vargs.get("jvm")));
        jvm.addAll(args(fargs.get("jvm")));
        List<String> game = new ArrayList<>(args(vargs.get("game")));
        game.addAll(args(fargs.get("game")));
        for (String a : jvm) plan.command.add(subst(a, vars));
        plan.command.add(Json.str(fabric, "mainClass"));
        for (String a : game) plan.command.add(subst(a, vars));
        l.update("Запуск игры...", 1.0);
        return plan;
    }

    private static String subst(String a, Map<String, String> vars) {
        for (Map.Entry<String, String> e : vars.entrySet()) a = a.replace("${" + e.getKey() + "}", e.getValue());
        return a;
    }

    static String javaBin() {
        Path w = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
        if (Files.exists(w)) return w.toString();
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private void installFabricApi(Path mods, String mc) throws Exception {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(mods, "fabric-api*.jar")) {
            if (ds.iterator().hasNext()) return;
        }
        String q = "https://api.modrinth.com/v2/project/fabric-api/version?loaders="
                + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8)
                + "&game_versions=" + URLEncoder.encode("[\"" + mc + "\"]", StandardCharsets.UTF_8);
        List<Object> vs = Json.list(Json.parse(get(q)));
        if (vs.isEmpty()) throw new IOException("Fabric API для " + mc + " не найден на Modrinth");
        List<Object> files = Json.list(Json.obj(vs.get(0)).get("files"));
        for (Object f : files) {
            Map<String, Object> m = Json.obj(f);
            if (Boolean.TRUE.equals(m.get("primary")) || files.size() == 1) {
                download(new Task(Json.str(m, "url"), mods.resolve(Json.str(m, "filename")), Json.num(m, "size")));
                return;
            }
        }
    }
}
