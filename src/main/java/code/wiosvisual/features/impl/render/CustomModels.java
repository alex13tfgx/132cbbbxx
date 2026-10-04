package code.wiosvisual.features.impl.render;

import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.impl.render.custommodels.BbModel;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BooleanSetting;
import code.wiosvisual.features.module.setting.implement.ButtonSetting;
import code.wiosvisual.features.module.setting.implement.SelectSetting;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;

/**
 * Надевает Figura-аватар из папки .minecraft/wiosvisual/custom_models на игрока (и друзей).
 * Модель — это папка с avatar.json или .zip-архив такой папки.
 */
public class CustomModels extends Module {
    private static final String NO_MODELS = "Нет моделей";
    /** Встроенные модели: {отображаемое имя, папка в assets/wiosvisual/custom_models}. */
    private static final String[][] BUILTIN = {
            {"Pixelar Tung Sahur", "pixelar_tung_sahur"},
            {"Peter Griffin", "peter_griffin"},
            {"Repo V2", "repo_v2"},
            {"Bacon", "bacon"},
            {"67 Brainrot", "six_seven"},
    };

    private final Map<String, Path> models = new LinkedHashMap<>();
    private final SelectSetting model = new SelectSetting("Модель", "Модель, применяемая к вашему игроку").value(NO_MODELS);
    private final BooleanSetting onFriends = new BooleanSetting("На друзей", "Также применять модель к друзьям");
    private final ButtonSetting refresh = new ButtonSetting("Обновить список", "Пересканировать папку custom_models")
            .setButtonName("Refresh").setRunnable(this::rescan);

    private String loadedModel;

    public static CustomModels getInstance() {
        return Instance.get(CustomModels.class);
    }

    public CustomModels() {
        super("CustomModels", "Custom Models", ModuleCategory.COSMETICS);
        this.setup(this.model, this.onFriends, this.refresh);
        this.rescan();
    }

    public static Path modelsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("wiosvisual").resolve("custom_models");
    }

    private void rescan() {
        this.models.clear();
        Path dir = modelsDir();
        try {
            Files.createDirectories(dir);
            for (String[] builtin : BUILTIN) {
                Path folder = extractBuiltin(builtin[1]);
                if (folder != null) {
                    this.models.put(builtin[0], folder);
                }
            }
            try (Stream<Path> entries = Files.list(dir)) {
                entries.sorted().forEach(entry -> {
                    String file = entry.getFileName().toString();
                    if (file.startsWith(".")) {
                        return;
                    }
                    if (Files.isDirectory(entry) && Files.exists(entry.resolve("avatar.json"))) {
                        this.models.put(file, entry);
                    } else if (file.toLowerCase().endsWith(".zip")) {
                        this.models.put(file.substring(0, file.length() - 4), entry);
                    }
                });
            }
        } catch (IOException error) {
            LogManager.getLogger("Wiosvisual").error("Failed to scan {}", dir, error);
        }
        String previous = this.model.getSelected();
        this.model.values(this.models.isEmpty() ? List.of(NO_MODELS) : new ArrayList<>(this.models.keySet()));
        if (previous != null) {
            this.model.selected(previous);
        }
        this.loadedModel = null;
    }

    private static BbModel current;

    /** Модель, которую нужно рисовать вместо игрока, или null, если модуль выключен / модель не загружена. */
    public static BbModel currentModel() {
        CustomModels module = getInstance();
        return module != null && module.isState() ? current : null;
    }

    /** Заменять ли ванильное тело этого игрока: свой всегда, друзей — если включено «На друзей». */
    public static boolean shouldReplace(Entity entity) {
        CustomModels module = getInstance();
        if (module == null || !module.isState() || current == null || !(entity instanceof PlayerEntity player)) {
            return false;
        }
        return player == mc.player || module.onFriends.isValue() && FriendUtils.isFriend(player);
    }

    @Override
    public void activate() {
        super.activate();
        this.loadedModel = null;
    }

    @Override
    public void deactivate() {
        this.unload();
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) {
            return;
        }
        String selected = this.model.getSelected();
        if (selected.equals(this.loadedModel)) {
            return;
        }
        // Что бы ни случилось при загрузке, повторно её в каждом тике не пробуем.
        this.loadedModel = selected;
        this.unload();
        Path source = this.models.get(selected);
        if (source == null) {
            return;
        }
        Path folder = this.resolveFolder(selected, source);
        Path file = folder == null ? null : findModelFile(folder);
        if (file == null) {
            this.notify("Custom Models: в «" + selected + "» нет файла .bbmodel");
            return;
        }
        try {
            current = BbModel.load(file);
        } catch (Exception error) {
            LogManager.getLogger("Wiosvisual").error("Failed to load model {}", file, error);
            this.notify("Custom Models: не удалось загрузить «" + selected + "»");
        }
    }

    private void unload() {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    private void notify(String text) {
        code.wiosvisual.display.hud.Notifications.getInstance().addList(text, 4000, true);
    }

    /** Предпочитает model.bbmodel; иначе берёт первый .bbmodel в папке. */
    private static Path findModelFile(Path folder) {
        Path preferred = folder.resolve("model.bbmodel");
        if (Files.isRegularFile(preferred)) {
            return preferred;
        }
        try (Stream<Path> entries = Files.list(folder)) {
            return entries.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".bbmodel")).sorted().findFirst().orElse(null);
        } catch (IOException error) {
            return null;
        }
    }

    /**
     * Figura грузит аватар только с диска — копируем встроенную модель из jar в .cache/builtin.
     * Список файлов модели (текстуры, lua-библиотеки, подпапки) лежит в её files.txt.
     */
    private static Path extractBuiltin(String id) throws IOException {
        String base = "/assets/wiosvisual/custom_models/" + id + "/";
        List<String> files;
        try (InputStream in = CustomModels.class.getResourceAsStream(base + "files.txt")) {
            if (in == null) {
                return null;
            }
            files = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
        }
        Path target = modelsDir().resolve(".cache").resolve("builtin").resolve(id).normalize();
        // Старые файлы прошлой версии модели убираем: лишний .bbmodel Figura тоже загрузила бы.
        if (Files.exists(target)) {
            try (Stream<Path> walk = Files.walk(target)) {
                walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(target);
        for (String file : files) {
            Path out = target.resolve(file).normalize();
            if (!out.startsWith(target)) {
                continue;
            }
            try (InputStream in = CustomModels.class.getResourceAsStream(base + file)) {
                if (in == null) {
                    return null;
                }
                Files.createDirectories(out.getParent());
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return target;
    }

    /** Папка отдаётся как есть, zip распаковывается в .cache (Figura грузит аватар из папки). */
    private Path resolveFolder(String name, Path source) {
        if (Files.isDirectory(source)) {
            return source;
        }
        Path target = modelsDir().resolve(".cache").resolve(name);
        try {
            if (!Files.exists(target) || Files.getLastModifiedTime(source).compareTo(Files.getLastModifiedTime(target)) > 0) {
                unzip(source, target);
            }
            if (Files.exists(target.resolve("avatar.json"))) {
                return target;
            }
            // Архив мог содержать папку аватара внутри.
            try (Stream<Path> entries = Files.list(target)) {
                return entries.filter(p -> Files.exists(p.resolve("avatar.json"))).findFirst().orElse(null);
            }
        } catch (IOException error) {
            LogManager.getLogger("Wiosvisual").error("Failed to unpack model {}", source, error);
            return null;
        }
    }

    private static void unzip(Path zip, Path target) throws IOException {
        if (Files.exists(target)) {
            try (Stream<Path> walk = Files.walk(target)) {
                walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(target);
        Path root = target.normalize();
        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (ZipEntry entry : java.util.Collections.list(file.entries())) {
                Path out = root.resolve(entry.getName()).normalize();
                if (!out.startsWith(root)) {
                    continue; // zip-slip
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                    continue;
                }
                Files.createDirectories(out.getParent());
                try (InputStream in = file.getInputStream(entry)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
