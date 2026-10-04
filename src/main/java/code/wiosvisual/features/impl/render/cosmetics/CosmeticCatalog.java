package code.wiosvisual.features.impl.render.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Список встроенной косметики из assets/mre/cosmetics/models. Файлы читаются один раз при
 * первом обращении; геометрия и анимация разбираются лениво — только для надетых предметов.
 */
public final class CosmeticCatalog {
    private static final int MAX_SCAN_INDEX = 256;
    private static final long CAPE_FRAME_MS = 100L;
    /** Названия предметов по номеру файла cosmetic_N.json; в файлах только «Pet 3», «Backpack 5» и т.п. */
    private static final String[] TITLES = {
            // Накидки 0–13
            "Ягуар", "Деревянная фигура", "Пульс", "Костюм с галстуком", "Тату", "Розовый свитер", "Тюльпаны",
            "Дразнилка", "Крик", "Летний день", "Зелье", "Алмазный меч", "Глитч", "Кресты",
            // Крылья 14–26
            "Осиные крылья", "Тёмные крылья", "Крылья гуля", "Крылья рокера", "Ледяные крылья", "Костяные крылья",
            "Трезубец", "Пасхальные крылья", "Ангельские крылья", "Крылья дракончика", "Синие драконьи крылья",
            "Паучьи крылья", "Вулканические крылья",
            // Спина 27–37
            "Катана", "Рюкзак-лягушка", "Рюкзак Supreme", "Рюкзак Vlone", "Рюкзак BAPE", "Полосатый рюкзак",
            "Рюкзак Nike", "Рюкзак Chrome Hearts", "Рюкзак Adidas", "Рюкзак Gucci", "Рюкзак Louis Vuitton",
            // Питомцы 38–49
            "Крипер", "Белый котик", "Чёрный котик", "Оцелот", "Тортик", "Пчёлка", "Голубь", "Призрак",
            "Алмаз", "Редиска", "Демон", "Ангелочек",
            // Шапки 50–62
            "Тёмная шляпа", "Нимб", "Шапка с ростком", "Голова лягушки", "Маска с очками", "Пингвин",
            "Розово-чёрная шапка", "Голова медведя", "Броненосец", "Верблюд", "Курица", "Лягушка", "Овечка"};
    private static final Map<Category, List<Entry>> ENTRIES = CosmeticCatalog.scan();

    private CosmeticCatalog() {
    }

    public enum Category {
        CAPE("Плащ", "плащ"),
        WINGS("Крылья", "крыл"),
        BACK("Спина", "рюкзак"),
        PET("Питомец", "питом"),
        HAT("Шапка", "шап", "шляп", "маск", "нимб", "голова"),
        EARS("Уши", "уши", "уш"),
        TAILS("Хвост", "хвост"),
        HEADS("Голова зверя", "голова"),
        MASKS("Маска", "маск"),
        PAWS("Лапы", "лап"),
        STOCKINGS("Чулки", "чулк");

        public final String title;
        /** Если название предмета уже содержит одно из этих слов, тип к нему не дописывается. */
        private final String[] typeWords;

        Category(String title, String ... typeWords) {
            this.title = title;
            this.typeWords = typeWords;
        }

        /** «Ягуар» → «Плащ: Ягуар», а «Рюкзак Nike» остаётся как есть. */
        String label(String name) {
            String lower = name.toLowerCase();
            for (String word : this.typeWords) {
                if (lower.contains(word)) {
                    return name;
                }
            }
            return this.title + ": " + name;
        }
    }

    /** Точка крепления: к какой части тела привязан предмет. */
    public enum Position {
        FREE,
        BODY,
        HEAD,
        /** Обе руки: предмет рисуется дважды, у правого и левого плеча. */
        ARMS,
        /** Обе ноги. */
        LEGS
    }

    public static final class Entry {
        public final int index;
        public final String name;
        public final Category category;
        public final Identifier texture;
        private boolean loaded;
        private Position position = Position.BODY;
        private float x;
        private float y;
        private float z;
        private float yaw;
        private float pitch;
        private float roll;
        private float scale = 1.0f;
        private float headOffset;
        private GeoModel model;
        private CosmeticAnimation animation;
        /** Анимация движения питомца (walk/run), если в файле она есть. */
        private CosmeticAnimation moveAnimation;
        /** Покадровая текстура: кадры столбиком, UV модели указывают на первый. */
        /** Кадры анимированной накидки; пустой массив — накидка обычная. */
        private Identifier[] capeFrames;
        private int frameCount = 1;
        private int frameTimeMs = 150;
        private float frameStep;
        /** Папка внешней косметики (assets/dimasik/cosmetics/категория/имя) или null для встроенной. */
        private final String dir;

        private Entry(int index, String name, Category category) {
            this.index = index;
            this.name = name;
            this.category = category;
            this.dir = null;
            this.texture = Identifier.of("mre", "textures/cosmetics/cosmetic_" + index + ".png");
        }

        /** Косметика из каталога dimasik: модель model.geo.json, анимация animation.json, текстура texture.png. */
        private Entry(String name, Category category, String folder, Position position, float scale) {
            this.index = -1;
            this.name = name;
            this.category = category;
            this.dir = folder;
            this.texture = Identifier.of("dimasik", "cosmetics/" + folder + "/texture.png");
            this.position = position;
            this.scale = scale;
            // Модели авторские, в координатах игрока (ноги на y=0): сдвигаем от точки крепления к земле.
            this.y = position == Position.ARMS || position == Position.LEGS ? -0.75f : -1.5f;
            if (position == Position.FREE) {
                this.x = 0.9f;
                this.z = 0.3f;
            }
        }

        /** Внешняя косметика уже в координатах игрока: служебные подъёмы не нужны. */
        public boolean external() {
            return this.dir != null;
        }

        public Position position() {
            this.ensureLoaded();
            return this.position;
        }

        public float x() {
            return this.x;
        }

        public float y() {
            return this.y;
        }

        public float z() {
            return this.z;
        }

        public float yaw() {
            return this.yaw;
        }

        public float pitch() {
            return this.pitch;
        }

        public float roll() {
            return this.roll;
        }

        public float scale() {
            return this.scale;
        }

        /**
         * Подъём предмета над шеей для крепления к голове. Шапки Pulse (категория 1) уже
         * отсчитывают высоту от шеи, а шапки-звери (категория 3) — от макушки, им нужен +0.5.
         */
        public float headOffset() {
            return this.headOffset;
        }

        /**
         * Текстура плаща в раскладке 64x32. Обычные накидки готовы как есть; у анимированных кадры
         * лежат столбиком и каждый покрывает только область плаща (22x17), поэтому при первом
         * обращении кадры раскладываются в отдельные текстуры полного размера.
         */
        public synchronized Identifier capeTexture() {
            if (this.capeFrames == null) {
                // Текстуры регистрируются только из потока рендера.
                if (!RenderSystem.isOnRenderThread()) {
                    return this.texture;
                }
                this.capeFrames = CosmeticCatalog.buildCapeFrames(this);
            }
            if (this.capeFrames.length == 0) {
                return this.texture;
            }
            return this.capeFrames[(int)(System.currentTimeMillis() / CAPE_FRAME_MS % this.capeFrames.length)];
        }

        /** Сдвиг V до текущего кадра анимированной текстуры (0, если текстура статичная). */
        public float frameOffsetV() {
            if (this.frameCount <= 1) {
                return 0.0f;
            }
            long frame = System.currentTimeMillis() / this.frameTimeMs % this.frameCount;
            return (float)frame * this.frameStep;
        }

        public GeoModel model() {
            this.ensureLoaded();
            return this.model;
        }

        public CosmeticAnimation animation() {
            this.ensureLoaded();
            return this.animation;
        }

        public CosmeticAnimation moveAnimation() {
            this.ensureLoaded();
            return this.moveAnimation;
        }

        private synchronized void ensureLoaded() {
            if (this.loaded) {
                return;
            }
            this.loaded = true;
            if (this.dir != null) {
                try {
                    JsonObject geo = CosmeticCatalog.readFile("/assets/dimasik/cosmetics/" + this.dir + "/model.geo.json");
                    this.model = geo == null ? null : GeoModelParser.parse(geo);
                    JsonObject anim = CosmeticCatalog.readFile("/assets/dimasik/cosmetics/" + this.dir + "/animation.json");
                    if (this.model != null && anim != null) {
                        this.animation = CosmeticAnimation.parse(anim, this.model);
                        this.moveAnimation = CosmeticAnimation.parse(anim, this.model, "run");
                        if (this.moveAnimation == null) {
                            this.moveAnimation = CosmeticAnimation.parse(anim, this.model, "walk");
                        }
                    }
                }
                catch (Exception ignored) {
                    this.model = null;
                    this.animation = null;
                }
                return;
            }
            JsonObject json = CosmeticCatalog.read(this.index);
            if (json == null) {
                return;
            }
            if (json.has("pos")) {
                int pos = json.get("pos").getAsInt();
                Position[] values = Position.values();
                this.position = pos >= 0 && pos < values.length ? values[pos] : Position.BODY;
            }
            this.x = CosmeticCatalog.number(json, "x", 0.0f);
            this.y = CosmeticCatalog.number(json, "y", 0.0f);
            this.z = CosmeticCatalog.number(json, "z", 0.0f);
            this.yaw = CosmeticCatalog.number(json, "yaw", 0.0f);
            this.pitch = CosmeticCatalog.number(json, "pitch", 0.0f);
            this.roll = CosmeticCatalog.number(json, "roll", 0.0f);
            this.scale = CosmeticCatalog.number(json, "scale", 1.0f);
            this.headOffset = CosmeticCatalog.number(json, "category", 1.0f) >= 3.0f ? 0.5f : 0.0f;
            if (json.has("textureAnimation") && json.get("textureAnimation").isJsonObject()) {
                JsonObject frames = json.getAsJsonObject("textureAnimation");
                int count = (int)CosmeticCatalog.number(frames, "frameCount", 1.0f);
                float frameHeight = CosmeticCatalog.number(frames, "frameHeight", 0.0f);
                float totalHeight = CosmeticCatalog.number(frames, "totalHeight", 0.0f);
                if (count > 1 && frameHeight > 0.0f && totalHeight > 0.0f) {
                    this.frameCount = count;
                    this.frameStep = frameHeight / totalHeight;
                    // frameTime задан в тиках игры (50 мс).
                    this.frameTimeMs = Math.max(1, Math.round(CosmeticCatalog.number(frames, "frameTime", 3.0f) * 50.0f));
                }
            }
            try {
                if (json.has("model") && json.get("model").isJsonObject()) {
                    this.model = GeoModelParser.parse(json.getAsJsonObject("model"));
                }
                if (this.model != null && json.has("animation") && json.get("animation").isJsonObject()) {
                    this.animation = CosmeticAnimation.parse(json.getAsJsonObject("animation"), this.model);
                }
            }
            catch (Exception ignored) {
                // Битая модель просто не рисуется.
                this.model = null;
                this.animation = null;
            }
        }
    }

    public static List<Entry> entries(Category category) {
        return ENTRIES.get(category);
    }

    private static final Map<Category, Map<String, Entry>> BY_NAME = new EnumMap<Category, Map<String, Entry>>(Category.class);

    public static synchronized Entry find(Category category, String name) {
        if (name == null) {
            return null;
        }
        Map<String, Entry> index = BY_NAME.get(category);
        if (index == null) {
            index = new java.util.HashMap<String, Entry>();
            for (Entry entry : ENTRIES.get(category)) {
                index.put(entry.name, entry);
            }
            BY_NAME.put(category, index);
        }
        return index.get(name);
    }

    private static Map<Category, List<Entry>> scan() {
        Map<Category, List<Entry>> result = new EnumMap<Category, List<Entry>>(Category.class);
        Map<Category, Set<String>> usedNames = new EnumMap<Category, Set<String>>(Category.class);
        for (Category category : Category.values()) {
            result.put(category, new ArrayList<Entry>());
            usedNames.put(category, new HashSet<String>());
        }
        for (int i = 0; i < MAX_SCAN_INDEX; ++i) {
            JsonObject json = CosmeticCatalog.read(i);
            if (json == null) {
                continue;
            }
            String rawName = json.has("name") ? json.get("name").getAsString() : "";
            String rawType = json.has("type") ? json.get("type").getAsString() : "";
            int pos = json.has("pos") ? json.get("pos").getAsInt() : -1;
            Category category = CosmeticCatalog.classify(i, rawName, rawType, pos);
            // Весь встроенный набор Pulse убран, кроме плащей; остальная косметика берётся из каталога dimasik.
            if (category != Category.CAPE) {
                continue;
            }
            String name = category.label(i < TITLES.length ? TITLES[i] : CosmeticCatalog.displayName(rawName, i));
            // У разных авторов встречаются одинаковые названия — различаем номером.
            Set<String> used = usedNames.get(category);
            String unique = name;
            for (int n = 2; used.contains(unique); ++n) {
                unique = name + " #" + n;
            }
            used.add(unique);
            result.get(category).add(new Entry(i, unique, category));
        }
        CosmeticCatalog.scanExternal(result, usedNames);
        for (Category category : Category.values()) {
            result.put(category, Collections.unmodifiableList(result.get(category)));
        }
        return result;
    }

    /** Каталог dimasik: catalog.json перечисляет предметы по папкам категорий. */
    private static void scanExternal(Map<Category, List<Entry>> result, Map<Category, Set<String>> usedNames) {
        JsonArray catalog;
        try (InputStream in = CosmeticCatalog.class.getResourceAsStream("/assets/dimasik/cosmetics/catalog.json")) {
            if (in == null) {
                return;
            }
            catalog = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray();
        }
        catch (Exception ignored) {
            return;
        }
        for (JsonElement element : catalog) {
            JsonObject item = element.getAsJsonObject();
            String folderCategory = item.get("category").getAsString();
            String id = item.get("name").getAsString();
            if (id.toLowerCase().startsWith("pulse_")) {
                continue;
            }
            Category category;
            Position position;
            switch (folderCategory) {
                case "wings" -> { category = Category.WINGS; position = Position.BODY; }
                case "backpacks" -> { category = Category.BACK; position = Position.BODY; }
                case "tails" -> { category = Category.TAILS; position = Position.BODY; }
                case "hats" -> { category = Category.HAT; position = Position.HEAD; }
                case "ears" -> { category = Category.EARS; position = Position.HEAD; }
                case "heads" -> { category = Category.HEADS; position = Position.HEAD; }
                case "masks" -> { category = Category.MASKS; position = Position.HEAD; }
                case "pets" -> { category = Category.PET; position = Position.FREE; }
                case "paws" -> { category = Category.PAWS; position = Position.ARMS; }
                case "stockings" -> { category = Category.STOCKINGS; position = Position.LEGS; }
                default -> { continue; }
            }
            String display = item.has("displayName") ? item.get("displayName").getAsString() : id;
            float scale = number(item, "scale", 1.0f);
            String name = category.label(display);
            Set<String> used = usedNames.get(category);
            String unique = name;
            for (int n = 2; used.contains(unique); ++n) {
                unique = name + " #" + n;
            }
            used.add(unique);
            result.get(category).add(new Entry(unique, category, folderCategory + "/" + id, position, scale));
        }
    }

    private static JsonObject readFile(String path) {
        try (InputStream in = CosmeticCatalog.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static Identifier[] buildCapeFrames(Entry entry) {
        try (InputStream in = CosmeticCatalog.class.getResourceAsStream("/assets/mre/textures/cosmetics/cosmetic_" + entry.index + ".png")) {
            if (in == null) {
                return new Identifier[0];
            }
            try (NativeImage strip = NativeImage.read(in)) {
                int width = strip.getWidth();
                int height = strip.getHeight();
                if (width == height * 2) {
                    // Обычная раскладка 64x32 (любого масштаба).
                    return new Identifier[0];
                }
                int frameHeight = Math.max(1, Math.round(width * 17.0f / 22.0f));
                int frames = Math.max(1, height / frameHeight);
                int canvasWidth = Math.round(width * 64.0f / 22.0f);
                int canvasHeight = canvasWidth / 2;
                Identifier[] ids = new Identifier[frames];
                for (int f = 0; f < frames; ++f) {
                    NativeImage canvas = new NativeImage(canvasWidth, canvasHeight, true);
                    for (int y = 0; y < frameHeight && y < canvasHeight; ++y) {
                        for (int x = 0; x < width && x < canvasWidth; ++x) {
                            canvas.setColorArgb(x, y, strip.getColorArgb(x, f * frameHeight + y));
                        }
                    }
                    ids[f] = Identifier.of("mre", "cosmetics/cape_" + entry.index + "_frame_" + f);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(ids[f], new NativeImageBackedTexture(() -> "wios", canvas));
                }
                return ids;
            }
        }
        catch (Exception ignored) {
            return new Identifier[0];
        }
    }

    private static JsonObject read(int index) {
        try (InputStream in = CosmeticCatalog.class.getResourceAsStream("/assets/mre/cosmetics/models/cosmetic_" + index + ".json")) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    /** Убирает префикс автора вида «pulse_» и подчёркивания. */
    private static String displayName(String rawName, int index) {
        String name = rawName == null ? "" : rawName.trim();
        int underscore = name.indexOf('_');
        if (underscore > 0 && underscore + 1 < name.length() && name.substring(0, underscore).matches("[a-z0-9]+") && Character.isUpperCase(name.charAt(underscore + 1))) {
            name = name.substring(underscore + 1);
        }
        name = name.replace('_', ' ').trim();
        return name.isEmpty() ? "Cosmetic " + (index + 1) : name;
    }

    private static Category classify(int index, String name, String rawType, int pos) {
        String type = rawType == null ? "" : rawType.trim().toLowerCase();
        switch (type) {
            case "cape":
                return Category.CAPE;
            case "wings":
                return Category.WINGS;
            case "pet":
                return Category.PET;
            case "hat":
                return Category.HAT;
            case "bodywear":
                return Category.BACK;
            default:
                break;
        }
        String lower = name == null ? "" : name.toLowerCase();
        if (lower.contains("cape")) {
            return Category.CAPE;
        }
        if (lower.contains("wing")) {
            return Category.WINGS;
        }
        if (lower.contains("pet") || lower.contains("bee") || lower.contains("radish")) {
            return Category.PET;
        }
        if (lower.contains("hat") || lower.contains("nimb") || pos == 2) {
            return Category.HAT;
        }
        if (index <= 13) {
            return Category.CAPE;
        }
        if (index <= 26) {
            return Category.WINGS;
        }
        if (index <= 37) {
            return Category.BACK;
        }
        if (index <= 49) {
            return Category.PET;
        }
        return Category.BACK;
    }

    private static float number(JsonObject json, String key, float fallback) {
        try {
            return json.has(key) ? json.get(key).getAsFloat() : fallback;
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    /** Названия для выпадающего списка: «Нет» и все предметы категории. */
    public static String[] options(Category category) {
        List<Entry> list = ENTRIES.get(category);
        String[] options = new String[list.size() + 1];
        options[0] = "Нет";
        for (int i = 0; i < list.size(); ++i) {
            options[i + 1] = list.get(i).name;
        }
        return options;
    }
}
