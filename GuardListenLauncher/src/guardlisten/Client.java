package guardlisten;

import java.nio.file.*;
import java.util.*;

/** Описание запускаемого клиента (записи из clients.json). */
final class Client {
    final String id, name, description, mc, loader;
    final String jar; // путь к jar мода (может быть null)
    /** Можно ли выбрать версию (false — карточка «Скоро»). */
    final boolean available;
    /** Подпись под номером версии на экране выбора. */
    final String subtitle;

    Client(String id, String name, String description, String mc, String loader, String jar, boolean available, String subtitle) {
        this.id = id; this.name = name; this.description = description;
        this.mc = mc; this.loader = loader; this.jar = jar;
        this.available = available; this.subtitle = subtitle;
    }

    static List<Client> load(Path appDir) {
        List<Client> out = new ArrayList<>();
        Path f = appDir.resolve("clients.json");
        try {
            if (Files.exists(f)) {
                for (Object o : Json.list(Json.parse(Files.readString(f)))) {
                    Map<String, Object> m = Json.obj(o);
                    String name = Json.str(m, "name");
                    if (name == null) continue;
                    String id = Json.str(m, "id") != null ? Json.str(m, "id") : name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                    boolean av = !Boolean.FALSE.equals(m.get("available"));
                    String sub = Json.str(m, "subtitle") != null ? Json.str(m, "subtitle")
                            : av ? "Fabric · стабильная версия" : "Версия находится в разработке";
                    out.add(new Client(id, name, Json.str(m, "description") != null ? Json.str(m, "description") : "",
                            Json.str(m, "mc") != null ? Json.str(m, "mc") : "1.21.11",
                            Json.str(m, "loader") != null ? Json.str(m, "loader") : "0.19.5", Json.str(m, "jar"), av, sub));
                }
            }
        } catch (Exception ignored) { }
        if (out.stream().noneMatch(c -> c.available)) {
            out.add(0, new Client("wios", "WiosVisuals",
                    "WiosVisuals — приватный клиент для Minecraft 1.21.11 на Fabric. Визуальные модули, HUD, "
                            + "улучшенный интерфейс и удобные утилиты. Клиент регулярно обновляется и дополняется, "
                            + "чтобы вы в полной мере ощутили комфорт в игре.",
                    "1.21.11", "0.19.5", null, true, "Fabric · стабильная версия"));
            if (out.size() == 1)
                out.add(new Client("wios-1214", "WiosVisuals", "", "1.21.4", "0.19.5", null, false, "Версия находится в разработке"));
        }
        return out;
    }

    /**
     * Ищет jar мода и берёт САМЫЙ СВЕЖИЙ из всех мест:
     * %APPDATA%\GuardListen\clients (туда сборка мода выкладывает билд сама),
     * папка clients рядом с лаунчером, вшитый в exe wios.jar, build/libs проекта.
     */
    Path resolveJar(Path appDir, Path root) {
        List<Path> c = new ArrayList<>();
        if (jar != null) c.add(appDir.resolve(jar));
        c.add(root.resolve("clients").resolve(id + ".jar"));
        c.add(appDir.resolve("clients").resolve(id + ".jar"));
        c.add(appDir.resolve("wios.jar"));
        c.add(appDir.resolve("..").resolve("build").resolve("libs").resolve("wiosvisual-1.0.0.jar"));
        Path best = null;
        long bestTime = Long.MIN_VALUE;
        for (Path p : c) {
            try {
                if (!Files.isRegularFile(p)) continue;
                long t = Files.getLastModifiedTime(p).toMillis();
                if (t > bestTime) { bestTime = t; best = p.normalize(); }
            } catch (Exception ignored) { }
        }
        return best;
    }

    /** Игра и мод уже скачаны — можно сразу играть. */
    boolean installed(Path root) {
        return Files.isRegularFile(root.resolve("versions").resolve(mc).resolve(mc + ".jar"))
                && Files.isRegularFile(root.resolve("instances").resolve(id).resolve("mods").resolve("wiosvisual.jar"));
    }
}
