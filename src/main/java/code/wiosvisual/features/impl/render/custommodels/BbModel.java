package code.wiosvisual.features.impl.render.custommodels;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Модель Blockbench (.bbmodel): дерево групп с кубами и mesh-элементами, плюс анимации.
 * Координаты в пикселях модели, Y вверх, лицо модели смотрит на север (−Z).
 * Lua-скрипты Figura не выполняются: из них берётся только начальная видимость частей.
 */
public final class BbModel {
    /** Порядок граней: north, east, south, west, up, down. */
    public static final String[] FACE_NAMES = {"north", "east", "south", "west", "up", "down"};
    private static final AtomicInteger COUNTER = new AtomicInteger();
    /** Части Figura, которые рисуются не на теле (череп, иконки, мир, первое лицо и т.п.). */
    private static final String[] SPECIAL_PREFIXES = {"skull", "portrait", "gui", "hud", "camera", "world", "firstperson", "arrow", "trident"};

    /** Имя папки модели (для встроенных — их id): по нему подключается поведение из скрипта. */
    public String id = "";
    /** Группы уже привязаны к частям тела (делается один раз при первом рендере). */
    public boolean partsAssigned;
    public final List<Group> roots = new ArrayList<>();
    public final List<Identifier> textures = new ArrayList<>();
    public final List<Animation> animations = new ArrayList<>();

    public static final class Group {
        public String name = "";
        public String uuid = "";
        public boolean visible = true;
        /** Часть тела игрока, за которой следует группа (индекс CustomModelFeatureRenderer.Part), −1 — никакая. */
        public int part = -1;
        public final float[] origin = new float[3];
        public final float[] rotation = new float[3];
        public final List<Group> groups = new ArrayList<>();
        public final List<Cube> cubes = new ArrayList<>();
        public final List<Mesh> meshes = new ArrayList<>();
    }

    public static final class Cube {
        public final float[] from = new float[3];
        public final float[] to = new float[3];
        public final float[] origin = new float[3];
        public final float[] rotation = new float[3];
        public float inflate;
        /** Нормализованные UV граней: {u1, v1, u2, v2}; null — грань не рисуется. */
        public final float[][] uv = new float[6][];
        public final int[] texture = new int[6];
    }

    /** Произвольная сетка: вершины уже в пикселях относительно origin. */
    public static final class Mesh {
        public final float[] origin = new float[3];
        public final float[] rotation = new float[3];
        public final List<MeshFace> faces = new ArrayList<>();
    }

    public static final class MeshFace {
        public int texture = -1;
        /** 3 или 4 вершины в порядке обхода: {x, y, z, u, v} (UV нормализованы). */
        public final List<float[]> vertices = new ArrayList<>();
    }

    /** Анимация Blockbench: дорожки ключевых кадров по группам. */
    public static final class Animation {
        public String name = "";
        /** loop | once | hold */
        public String loop = "once";
        public float length;
        /** uuid группы → дорожки rotation/position/scale. */
        public final Map<String, Track[]> tracks = new HashMap<>();
    }

    public static final int ROT = 0, POS = 1, SCALE = 2;

    public static final class Track {
        public final List<Keyframe> keys = new ArrayList<>();
    }

    public static final class Keyframe {
        public float time;
        public final float[] value = new float[3];
        /** linear | catmullrom | step */
        public String interpolation = "linear";
    }

    public static BbModel load(Path file) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        BbModel model = new BbModel();
        Path folder = file.getParent();
        model.id = folder != null && folder.getFileName() != null ? folder.getFileName().toString() : "";

        float resW = 16;
        float resH = 16;
        if (root.has("resolution") && root.get("resolution").isJsonObject()) {
            JsonObject res = root.getAsJsonObject("resolution");
            resW = number(res, "width", 16);
            resH = number(res, "height", 16);
        }

        List<float[]> textureUvSize = new ArrayList<>();
        if (root.has("textures")) {
            for (JsonElement element : root.getAsJsonArray("textures")) {
                JsonObject tex = element.getAsJsonObject();
                Identifier id = null;
                float uvW = number(tex, "uv_width", resW);
                float uvH = number(tex, "uv_height", resH);
                byte[] bytes = readTexture(tex, folder);
                if (bytes != null) {
                    try {
                        NativeImage image = NativeImage.read(bytes);
                        id = Identifier.of("wiosvisual", "custom_model/" + COUNTER.incrementAndGet());
                        Identifier registered = id;
                        MinecraftClient.getInstance().getTextureManager().registerTexture(registered, new NativeImageBackedTexture(() -> registered.toString(), image));
                    } catch (Exception error) {
                        id = null;
                    }
                }
                model.textures.add(id);
                textureUvSize.add(new float[]{uvW, uvH});
            }
        }

        Map<String, JsonObject> elements = new HashMap<>();
        if (root.has("elements")) {
            for (JsonElement element : root.getAsJsonArray("elements")) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("uuid")) {
                    elements.put(object.get("uuid").getAsString(), object);
                }
            }
        }
        if (root.has("outliner")) {
            for (JsonElement item : root.getAsJsonArray("outliner")) {
                if (item.isJsonObject()) {
                    model.roots.add(parseGroup(item.getAsJsonObject(), elements, textureUvSize));
                } else {
                    // Элемент прямо в корне: кладём в безымянную группу.
                    Group loose = new Group();
                    addElement(loose, item.getAsString(), elements, textureUvSize);
                    model.roots.add(loose);
                }
            }
        }
        if (root.has("animations")) {
            for (JsonElement element : root.getAsJsonArray("animations")) {
                if (element.isJsonObject()) {
                    model.animations.add(parseAnimation(element.getAsJsonObject()));
                }
            }
        }
        if (folder != null) {
            model.applyScriptVisibility(folder.resolve("script.lua"));
        }
        model.hideSpecialParts(model.roots);
        return model;
    }

    // ------------------------------------------------------------------ группы и элементы

    private static Group parseGroup(JsonObject object, Map<String, JsonObject> elements, List<float[]> uvSizes) {
        Group group = new Group();
        group.name = object.has("name") ? object.get("name").getAsString() : "";
        group.uuid = object.has("uuid") ? object.get("uuid").getAsString() : "";
        group.visible = !object.has("visibility") || object.get("visibility").getAsBoolean();
        readVec(object, "origin", group.origin);
        readVec(object, "rotation", group.rotation);
        if (object.has("children")) {
            for (JsonElement child : object.getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    group.groups.add(parseGroup(child.getAsJsonObject(), elements, uvSizes));
                } else {
                    addElement(group, child.getAsString(), elements, uvSizes);
                }
            }
        }
        return group;
    }

    private static void addElement(Group group, String uuid, Map<String, JsonObject> elements, List<float[]> uvSizes) {
        JsonObject object = elements.get(uuid);
        if (object == null) {
            return;
        }
        if (object.has("export") && !object.get("export").getAsBoolean()) {
            return;
        }
        if (object.has("visibility") && !object.get("visibility").getAsBoolean()) {
            return;
        }
        String type = object.has("type") ? object.get("type").getAsString() : "cube";
        if ("mesh".equals(type)) {
            Mesh mesh = parseMesh(object, uvSizes);
            if (mesh != null) {
                group.meshes.add(mesh);
            }
            return;
        }
        if (!"cube".equals(type)) {
            return;
        }
        Cube cube = new Cube();
        readVec(object, "from", cube.from);
        readVec(object, "to", cube.to);
        readVec(object, "origin", cube.origin);
        readVec(object, "rotation", cube.rotation);
        cube.inflate = number(object, "inflate", 0);
        JsonObject faces = object.has("faces") ? object.getAsJsonObject("faces") : new JsonObject();
        for (int i = 0; i < 6; i++) {
            cube.texture[i] = -1;
            String name = FACE_NAMES[i];
            if (!faces.has(name) || !faces.get(name).isJsonObject()) {
                continue;
            }
            JsonObject face = faces.getAsJsonObject(name);
            if (!face.has("texture") || face.get("texture").isJsonNull() || !face.has("uv")) {
                continue;
            }
            int texture = face.get("texture").getAsInt();
            JsonArray uv = face.getAsJsonArray("uv");
            float[] size = uvSize(uvSizes, texture);
            cube.texture[i] = texture;
            cube.uv[i] = new float[]{
                    uv.get(0).getAsFloat() / size[0], uv.get(1).getAsFloat() / size[1],
                    uv.get(2).getAsFloat() / size[0], uv.get(3).getAsFloat() / size[1]};
        }
        group.cubes.add(cube);
    }

    private static Mesh parseMesh(JsonObject object, List<float[]> uvSizes) {
        if (!object.has("vertices") || !object.has("faces")) {
            return null;
        }
        Mesh mesh = new Mesh();
        readVec(object, "origin", mesh.origin);
        readVec(object, "rotation", mesh.rotation);
        JsonObject vertices = object.getAsJsonObject("vertices");
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("faces").entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject face = entry.getValue().getAsJsonObject();
            if (!face.has("vertices") || !face.has("texture") || face.get("texture").isJsonNull()) {
                continue;
            }
            JsonArray keys = face.getAsJsonArray("vertices");
            if (keys.size() < 3 || keys.size() > 4) {
                continue;
            }
            MeshFace out = new MeshFace();
            out.texture = face.get("texture").getAsInt();
            float[] size = uvSize(uvSizes, out.texture);
            JsonObject uv = face.has("uv") && face.get("uv").isJsonObject() ? face.getAsJsonObject("uv") : new JsonObject();
            for (JsonElement key : keys) {
                String k = key.getAsString();
                if (!vertices.has(k)) {
                    out.vertices.clear();
                    break;
                }
                JsonArray p = vertices.getAsJsonArray(k);
                JsonArray t = uv.has(k) ? uv.getAsJsonArray(k) : null;
                out.vertices.add(new float[]{
                        p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat(),
                        t != null ? t.get(0).getAsFloat() / size[0] : 0f,
                        t != null ? t.get(1).getAsFloat() / size[1] : 0f});
            }
            if (out.vertices.size() == 4) {
                sortQuad(out.vertices);
            }
            if (out.vertices.size() >= 3) {
                mesh.faces.add(out);
            }
        }
        return mesh;
    }

    /**
     * Blockbench хранит вершины грани в произвольном порядке. Переставляем их так, чтобы
     * 0–2 была диагональю (иначе четырёхугольник «перекрутится»).
     */
    private static void sortQuad(List<float[]> v) {
        float[] a = v.get(0), b = v.get(1), c = v.get(2), d = v.get(3);
        if (onOppositeSides(a, c, b, d)) {
            return; // 0–2 уже диагональ
        }
        if (onOppositeSides(a, b, c, d)) {
            v.set(1, c);
            v.set(2, b); // диагональ была 0–1
            return;
        }
        v.set(2, d);
        v.set(3, c); // диагональ была 0–3
    }

    /** Лежат ли p и q по разные стороны прямой a–b (в плоскости грани). */
    private static boolean onOppositeSides(float[] a, float[] b, float[] p, float[] q) {
        float[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        float[] cp = cross(ab, new float[]{p[0] - a[0], p[1] - a[1], p[2] - a[2]});
        float[] cq = cross(ab, new float[]{q[0] - a[0], q[1] - a[1], q[2] - a[2]});
        return cp[0] * cq[0] + cp[1] * cq[1] + cp[2] * cq[2] < 0;
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float[] uvSize(List<float[]> uvSizes, int texture) {
        return texture >= 0 && texture < uvSizes.size() ? uvSizes.get(texture) : new float[]{16, 16};
    }

    // ------------------------------------------------------------------ анимации

    private static Animation parseAnimation(JsonObject object) {
        Animation anim = new Animation();
        anim.name = object.has("name") ? object.get("name").getAsString() : "";
        anim.loop = object.has("loop") ? object.get("loop").getAsString() : "once";
        anim.length = number(object, "length", 0);
        if (!object.has("animators") || !object.get("animators").isJsonObject()) {
            return anim;
        }
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("animators").entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject animator = entry.getValue().getAsJsonObject();
            if (animator.has("type") && !"bone".equals(animator.get("type").getAsString())) {
                continue;
            }
            if (!animator.has("keyframes")) {
                continue;
            }
            Track[] tracks = new Track[3];
            for (JsonElement kfElement : animator.getAsJsonArray("keyframes")) {
                JsonObject kf = kfElement.getAsJsonObject();
                String channel = kf.has("channel") ? kf.get("channel").getAsString() : "";
                int index = switch (channel) {
                    case "rotation" -> ROT;
                    case "position" -> POS;
                    case "scale" -> SCALE;
                    default -> -1;
                };
                if (index < 0 || !kf.has("data_points") || kf.getAsJsonArray("data_points").isEmpty()) {
                    continue;
                }
                JsonObject point = kf.getAsJsonArray("data_points").get(0).getAsJsonObject();
                Keyframe key = new Keyframe();
                key.time = number(kf, "time", 0);
                key.interpolation = kf.has("interpolation") ? kf.get("interpolation").getAsString() : "linear";
                float fallback = index == SCALE ? 1f : 0f;
                key.value[0] = value(point, "x", fallback);
                key.value[1] = value(point, "y", fallback);
                key.value[2] = value(point, "z", fallback);
                if (tracks[index] == null) {
                    tracks[index] = new Track();
                }
                tracks[index].keys.add(key);
            }
            for (Track track : tracks) {
                if (track != null) {
                    track.keys.sort((a, b) -> Float.compare(a.time, b.time));
                }
            }
            anim.tracks.put(entry.getKey(), tracks);
        }
        return anim;
    }

    /** Значение ключа: число или простое Molang-выражение без переменных; иначе запасное. */
    private static float value(JsonObject point, String axis, float fallback) {
        if (!point.has(axis) || point.get(axis).isJsonNull()) {
            return fallback;
        }
        String raw = point.get(axis).getAsString().trim();
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public Animation animation(String name) {
        for (Animation anim : this.animations) {
            if (anim.name.equalsIgnoreCase(name)) {
                return anim;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ видимость

    /**
     * Figura-скрипты прячут часть деталей при загрузке (`models.model.a.b:setVisible(false)`
     * без отступа). Повторяем это: такие группы скрываются.
     */
    private void applyScriptVisibility(Path script) {
        if (!Files.isRegularFile(script)) {
            return;
        }
        Pattern pattern = Pattern.compile("^models\\.[A-Za-z0-9_]+((?:\\.[A-Za-z0-9_]+)+)\\s*:\\s*setVisible\\(\\s*false\\s*\\)");
        Set<String> hidden = new HashSet<>();
        try {
            for (String line : Files.readAllLines(script, StandardCharsets.UTF_8)) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    hidden.add(m.group(1).substring(1));
                }
            }
        } catch (IOException ignored) {
            return;
        }
        for (String path : hidden) {
            Group group = find(this.roots, path.split("\\."), 0);
            if (group != null) {
                group.visible = false;
            }
        }
    }

    private static Group find(List<Group> groups, String[] path, int index) {
        for (Group group : groups) {
            if (group.name.equals(path[index])) {
                return index == path.length - 1 ? group : find(group.groups, path, index + 1);
            }
        }
        return null;
    }

    /** Служебные корневые группы Figura не рисуются на теле игрока. */
    private void hideSpecialParts(List<Group> groups) {
        for (Group group : groups) {
            String lower = group.name.toLowerCase(Locale.ROOT);
            for (String prefix : SPECIAL_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    group.visible = false;
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ утилиты

    private static byte[] readTexture(JsonObject tex, Path folder) {
        try {
            if (tex.has("source") && !tex.get("source").isJsonNull()) {
                String source = tex.get("source").getAsString();
                int comma = source.indexOf(',');
                if (source.startsWith("data:") && comma > 0) {
                    return Base64.getDecoder().decode(source.substring(comma + 1));
                }
            }
            if (folder != null) {
                for (String key : new String[]{"relative_path", "name", "path"}) {
                    if (!tex.has(key) || tex.get(key).isJsonNull()) {
                        continue;
                    }
                    String value = tex.get(key).getAsString().replace('\\', '/');
                    Path direct = folder.resolve(value).normalize();
                    if (direct.startsWith(folder) && Files.isRegularFile(direct)) {
                        return Files.readAllBytes(direct);
                    }
                    Path byName = folder.resolve(Path.of(value).getFileName().toString());
                    if (Files.isRegularFile(byName)) {
                        return Files.readAllBytes(byName);
                    }
                }
            }
        } catch (Exception ignored) {
            // Текстура не прочиталась — грани с ней просто не рисуются.
        }
        return null;
    }

    private static void readVec(JsonObject object, String key, float[] out) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return;
        }
        JsonArray array = object.getAsJsonArray(key);
        for (int i = 0; i < 3 && i < array.size(); i++) {
            out[i] = array.get(i).getAsFloat();
        }
    }

    private static float number(JsonObject object, String key, float fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsFloat() : fallback;
    }

    /** Освобождает текстуры модели. */
    public void close() {
        for (Identifier id : this.textures) {
            if (id != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
            }
        }
        this.textures.clear();
    }
}
