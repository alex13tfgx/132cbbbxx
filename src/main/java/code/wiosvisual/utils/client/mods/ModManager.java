package code.wiosvisual.utils.client.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

/**
 * Управление модами из папки {@code mods} прямо из игры: установка .jar, включение/выключение
 * (переименование в {@code .jar.disabled}) и удаление. Fabric не умеет подгружать моды на ходу,
 * поэтому всё применяется после перезапуска.
 * <p>
 * На Windows загруженный jar заблокирован процессом игры — такие операции запоминаются и
 * выполняются отдельным скриптом сразу после закрытия игры.
 */
public final class ModManager {
    private static final String DISABLED_SUFFIX = ".disabled";
    /** id самого клиента: его нельзя выключить или удалить из списка. */
    private static final String SELF_ID = "renderextras";

    private static final Map<Path, Path> pendingRenames = new LinkedHashMap<>();
    private static final Set<Path> pendingDeletes = new LinkedHashSet<>();
    private static boolean shutdownHookRegistered;
    private static boolean changed;

    private ModManager() {
    }

    public record Result(String message, boolean error) {
    }

    public static final class ModEntry {
        public final Path path;
        public final String fileName;
        public final String id;
        public final String name;
        public final String version;
        public final long size;
        public final boolean loaded;
        public final boolean self;
        /** Включён ли мод с учётом отложенного переименования. */
        public final boolean enabled;
        public final boolean pendingDelete;

        ModEntry(Path path, String id, String name, String version, long size, boolean loaded, boolean self, boolean enabled, boolean pendingDelete) {
            this.path = path;
            this.fileName = path.getFileName().toString();
            this.id = id;
            this.name = name;
            this.version = version;
            this.size = size;
            this.loaded = loaded;
            this.self = self;
            this.enabled = enabled;
            this.pendingDelete = pendingDelete;
        }
    }

    private record Metadata(String id, String name, String version, JsonElement minecraft) {
    }

    public static Path modsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    /** Нужно ли перезапустить игру, чтобы изменения вступили в силу. */
    public static boolean restartRequired() {
        return changed || !pendingRenames.isEmpty() || !pendingDeletes.isEmpty();
    }

    public static synchronized List<ModEntry> scan() {
        List<ModEntry> result = new ArrayList<>();
        Path dir = modsDir();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        Set<Path> loadedPaths = new HashSet<>();
        Set<Path> selfPaths = new HashSet<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            try {
                if (container.getOrigin().getKind() != ModOrigin.Kind.PATH) continue;
                for (Path p : container.getOrigin().getPaths()) {
                    Path norm = normalize(p);
                    loadedPaths.add(norm);
                    if (SELF_ID.equals(container.getMetadata().getId())) {
                        selfPaths.add(norm);
                    }
                }
            } catch (Throwable ignored) {
                // Источник мода недоступен — просто не считаем его загруженным.
            }
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).filter(ModManager::isModFile).forEach(file -> {
                Path path = normalize(file);
                Metadata meta = readMetadata(path);
                boolean onDiskEnabled = !path.getFileName().toString().endsWith(DISABLED_SUFFIX);
                Path renameTarget = pendingRenames.get(path);
                boolean enabled = renameTarget == null ? onDiskEnabled : !renameTarget.getFileName().toString().endsWith(DISABLED_SUFFIX);
                long size = 0L;
                try {
                    size = Files.size(path);
                } catch (IOException ignored) {
                    // Размер только для подписи.
                }
                String name = meta != null && meta.name() != null ? meta.name() : stripExtension(path.getFileName().toString());
                result.add(new ModEntry(path, meta == null ? null : meta.id(), name, meta == null ? null : meta.version(), size,
                        loadedPaths.contains(path), selfPaths.contains(path) || (meta != null && SELF_ID.equals(meta.id())),
                        enabled, pendingDeletes.contains(path)));
            });
        } catch (IOException ignored) {
            // Папка пропала во время чтения — показываем то, что успели.
        }
        result.sort(Comparator.comparing((ModEntry e) -> e.name.toLowerCase(Locale.ROOT)));
        return result;
    }

    /** Копирует выбранные файлы в папку модов, проверяя, что это совместимые моды Fabric. */
    public static synchronized List<Result> install(List<Path> files) {
        List<Result> results = new ArrayList<>();
        Path dir = modsDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            results.add(new Result("Не удалось создать папку mods: " + e.getMessage(), true));
            return results;
        }
        for (Path source : files) {
            results.add(installOne(dir, source));
        }
        return results;
    }

    private static Result installOne(Path dir, Path source) {
        String fileName = source.getFileName().toString();
        if (!Files.isRegularFile(source) || !fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return new Result(fileName + ": это не .jar файл", true);
        }
        String problem = detectForeignLoader(source);
        if (problem != null) {
            return new Result(fileName + ": " + problem, true);
        }
        Metadata meta = readMetadata(source);
        if (meta == null) {
            return new Result(fileName + ": не похоже на мод Fabric", true);
        }
        String display = meta.name() == null ? fileName : meta.name();
        if (SELF_ID.equals(meta.id())) {
            return new Result(display + ": клиент уже установлен", true);
        }
        String incompatible = checkMinecraft(meta.minecraft());
        if (incompatible != null) {
            return new Result(display + ": " + incompatible, true);
        }
        Path target = dir.resolve(fileName);
        if (normalize(source).equals(normalize(target))) {
            return new Result(display + ": уже в папке mods", true);
        }
        if (Files.exists(target) || Files.exists(dir.resolve(fileName + DISABLED_SUFFIX))) {
            return new Result(display + ": файл с таким именем уже установлен", true);
        }
        // Два мода с одним id не дадут игре запуститься — старую версию выключаем.
        boolean replaced = false;
        for (ModEntry existing : scan()) {
            if (meta.id() == null || !meta.id().equals(existing.id) || existing.pendingDelete || !existing.enabled) continue;
            setEnabled(existing, false);
            replaced = true;
        }
        try {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            return new Result(display + ": не удалось скопировать (" + e.getMessage() + ")", true);
        }
        changed = true;
        String version = meta.version() == null ? "" : " " + meta.version();
        return new Result("Установлен " + display + version + (replaced ? " (старая версия выключена)" : ""), false);
    }

    public static synchronized Result setEnabled(ModEntry entry, boolean enable) {
        if (entry.self) {
            return new Result("Клиент нельзя выключить", true);
        }
        if (entry.pendingDelete || entry.enabled == enable) {
            return null;
        }
        Path path = entry.path;
        if (pendingRenames.remove(path) != null) {
            // Повторное нажатие отменяет ещё не выполненное переименование.
            return null;
        }
        String name = path.getFileName().toString();
        Path target = enable
                ? path.resolveSibling(name.substring(0, name.length() - DISABLED_SUFFIX.length()))
                : path.resolveSibling(name + DISABLED_SUFFIX);
        if (Files.exists(target)) {
            return new Result("Уже есть файл " + target.getFileName(), true);
        }
        try {
            Files.move(path, target);
            changed = true;
        } catch (IOException e) {
            pendingRenames.put(path, normalize(target));
            registerShutdownHook();
        }
        return null;
    }

    public static synchronized Result delete(ModEntry entry) {
        if (entry.self) {
            return new Result("Клиент нельзя удалить", true);
        }
        pendingRenames.remove(entry.path);
        try {
            Files.deleteIfExists(entry.path);
            changed = true;
            return new Result("Удалён " + entry.name, false);
        } catch (IOException e) {
            pendingDeletes.add(entry.path);
            registerShutdownHook();
            return new Result(entry.name + " удалится после выхода из игры", false);
        }
    }

    public static synchronized void cancelDelete(ModEntry entry) {
        pendingDeletes.remove(entry.path);
    }

    private static boolean isModFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jar") || name.endsWith(".jar" + DISABLED_SUFFIX);
    }

    private static Metadata readMetadata(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if (entry == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonElement minecraft = null;
                if (json.has("depends") && json.get("depends").isJsonObject()) {
                    minecraft = json.getAsJsonObject("depends").get("minecraft");
                }
                return new Metadata(string(json, "id"), string(json, "name"), string(json, "version"), minecraft);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String detectForeignLoader(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            if (zip.getEntry("fabric.mod.json") != null) {
                return null;
            }
            if (zip.getEntry("META-INF/mods.toml") != null || zip.getEntry("META-INF/neoforge.mods.toml") != null) {
                return "это мод для Forge/NeoForge, нужен Fabric";
            }
            if (zip.getEntry("quilt.mod.json") != null) {
                return "это мод для Quilt, нужен Fabric";
            }
            return null;
        } catch (IOException e) {
            return "файл повреждён или это не jar";
        }
    }

    /** Возвращает текст ошибки, если мод не рассчитан на текущую версию Minecraft. */
    private static String checkMinecraft(JsonElement requirement) {
        if (requirement == null) {
            return null;
        }
        try {
            ModContainer minecraft = FabricLoader.getInstance().getModContainer("minecraft").orElse(null);
            if (minecraft == null) {
                return null;
            }
            Version current = minecraft.getMetadata().getVersion();
            List<String> predicates = new ArrayList<>();
            if (requirement.isJsonArray()) {
                for (JsonElement el : (JsonArray)requirement) {
                    predicates.add(el.getAsString());
                }
            } else {
                predicates.add(requirement.getAsString());
            }
            for (String predicate : predicates) {
                if (VersionPredicate.parse(predicate).test(current)) {
                    return null;
                }
            }
            return "мод для Minecraft " + String.join(" / ", predicates) + ", а у вас " + current.getFriendlyString();
        } catch (Exception e) {
            // Нестандартная запись версии — не мешаем установке.
            return null;
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement el = json.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    private static String stripExtension(String fileName) {
        String name = fileName.endsWith(DISABLED_SUFFIX) ? fileName.substring(0, fileName.length() - DISABLED_SUFFIX.length()) : fileName;
        return name.toLowerCase(Locale.ROOT).endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(ModManager::runPendingAfterExit, "wios-mods-pending"));
    }

    /**
     * Файлы загруженных модов заняты процессом игры, пока он жив. Запускаем отдельный скрипт,
     * который дожидается выхода игры и уже тогда переименовывает/удаляет их.
     */
    private static synchronized void runPendingAfterExit() {
        if (pendingRenames.isEmpty() && pendingDeletes.isEmpty()) {
            return;
        }
        long pid = ProcessHandle.current().pid();
        try {
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            StringBuilder sb = new StringBuilder();
            Path script;
            if (windows) {
                script = Files.createTempFile("wios-mods-", ".bat");
                sb.append("@echo off\r\nchcp 65001 >nul\r\n");
                sb.append(":wait\r\n");
                sb.append("tasklist /FI \"PID eq ").append(pid).append("\" 2>nul | find \"").append(pid).append("\" >nul\r\n");
                sb.append("if not errorlevel 1 (\r\n  timeout /t 1 /nobreak >nul\r\n  goto wait\r\n)\r\n");
                for (Map.Entry<Path, Path> rename : pendingRenames.entrySet()) {
                    sb.append("move /Y \"").append(rename.getKey()).append("\" \"").append(rename.getValue()).append("\" >nul\r\n");
                }
                for (Path path : pendingDeletes) {
                    sb.append("del /F /Q \"").append(path).append("\" >nul\r\n");
                }
                sb.append("del \"%~f0\"\r\n");
                Files.writeString(script, sb.toString(), StandardCharsets.UTF_8);
                new ProcessBuilder("cmd.exe", "/c", "start", "/min", "cmd.exe", "/c", script.toString()).start();
            } else {
                script = Files.createTempFile("wios-mods-", ".sh");
                sb.append("while kill -0 ").append(pid).append(" 2>/dev/null; do sleep 1; done\n");
                for (Map.Entry<Path, Path> rename : pendingRenames.entrySet()) {
                    sb.append("mv -f ").append(shellQuote(rename.getKey())).append(' ').append(shellQuote(rename.getValue())).append('\n');
                }
                for (Path path : pendingDeletes) {
                    sb.append("rm -f ").append(shellQuote(path)).append('\n');
                }
                sb.append("rm -f \"$0\"\n");
                Files.writeString(script, sb.toString(), StandardCharsets.UTF_8);
                new ProcessBuilder("nohup", "sh", script.toString()).start();
            }
        } catch (Exception ignored) {
            // Если скрипт не запустился, файлы просто останутся как были.
        }
    }

    private static String shellQuote(Path path) {
        return "'" + path.toString().replace("'", "'\\''") + "'";
    }
}
