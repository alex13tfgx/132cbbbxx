package code.wiosvisual.emotions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.model.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Эмоции из файлов assets/dimasik/player_animation/*.json (Bedrock-анимации костей игрока:
 * body, torso, head, left_arm, right_arm, left_leg, right_leg). Углы в градусах задают
 * pitch/yaw/roll частей модели напрямую, смещения — в пикселях модели (Y у Bedrock направлен вверх).
 */
public final class JsonEmotes {
    private static final Map<String, Anim> CACHE = new HashMap<String, Anim>();
    private static final Vector3f WAIST = new Vector3f(0.0f, 12.0f, 0.0f);

    private JsonEmotes() {
    }

    /** Длительность в миллисекундах: длина анимации, а у зацикленных — фиксированные 8 секунд. */
    public static long durationOf(String file) {
        Anim anim = get(file);
        if (anim == null) {
            return 3000L;
        }
        return anim.loop ? 8000L : Math.max(1000L, (long)(anim.length * 1000.0f) + 400L);
    }

    public static boolean exists(String file) {
        return get(file) != null;
    }

    private static synchronized Anim get(String file) {
        if (CACHE.containsKey(file)) {
            return CACHE.get(file);
        }
        Anim anim = null;
        try (InputStream in = JsonEmotes.class.getResourceAsStream("/assets/dimasik/player_animation/" + file + ".json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                anim = Anim.parse(root);
            }
        }
        catch (Exception ignored) {
            anim = null;
        }
        CACHE.put(file, anim);
        return anim;
    }

    /** Ставит позу частей модели по анимации; alpha — плавное включение и выключение эмоции. */
    public static void apply(String file, long elapsedMs, float alpha, ModelPart head, ModelPart hat, ModelPart body,
                             ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        Anim anim = get(file);
        if (anim == null) {
            return;
        }
        float seconds = elapsedMs / 1000.0f;
        float time = anim.loop && anim.length > 0.0f ? seconds % anim.length : Math.min(seconds, anim.length);
        Pose root = anim.sample("body", time);
        Pose torso = anim.sample("torso", time);
        place(head, anim.sample("head", time), torso, root, alpha, true);
        place(hat, anim.sample("head", time), torso, root, alpha, true);
        place(body, null, torso, root, alpha, true);
        place(rightArm, anim.sample("right_arm", time), torso, root, alpha, true);
        place(leftArm, anim.sample("left_arm", time), torso, root, alpha, true);
        place(rightLeg, anim.sample("right_leg", time), null, root, alpha, false);
        place(leftLeg, anim.sample("left_leg", time), null, root, alpha, false);
    }

    /** Собственная поза части, затем поворот торса (для верхней части тела) и всего тела вокруг талии. */
    private static void place(ModelPart part, Pose own, Pose torso, Pose root, float alpha, boolean underTorso) {
        Vector3f position = new Vector3f(part.originX, part.originY, part.originZ);
        Quaternionf rotation = own != null ? own.rotation() : new Quaternionf().rotationZYX(part.roll, part.yaw, part.pitch);
        if (own != null) {
            position.add(own.offset());
        }
        if (underTorso && torso != null) {
            Quaternionf rt = torso.rotation();
            position.sub(WAIST).rotate(rt).add(WAIST).add(torso.offset());
            rotation = new Quaternionf(rt).mul(rotation);
        }
        if (root != null) {
            Quaternionf rr = root.rotation();
            position.sub(WAIST).rotate(rr).add(WAIST).add(root.offset());
            rotation = new Quaternionf(rr).mul(rotation);
        }
        Vector3f euler = rotation.getEulerAnglesZYX(new Vector3f());
        part.originX += (position.x - part.originX) * alpha;
        part.originY += (position.y - part.originY) * alpha;
        part.originZ += (position.z - part.originZ) * alpha;
        part.pitch += (euler.x - part.pitch) * alpha;
        part.yaw += (euler.y - part.yaw) * alpha;
        part.roll += (euler.z - part.roll) * alpha;
    }

    /** Поза кости: поворот в радианах (pitch, yaw, roll) и смещение в пикселях модели. */
    private record Pose(float rx, float ry, float rz, float ox, float oy, float oz) {
        Quaternionf rotation() {
            return new Quaternionf().rotationZYX(this.rz, this.ry, this.rx);
        }

        Vector3f offset() {
            return new Vector3f(this.ox, this.oy, this.oz);
        }
    }

    private static final class Anim {
        final boolean loop;
        final float length;
        final Map<String, TreeMap<Float, float[]>> rotations = new HashMap<String, TreeMap<Float, float[]>>();
        final Map<String, TreeMap<Float, float[]>> positions = new HashMap<String, TreeMap<Float, float[]>>();

        Anim(boolean loop, float length) {
            this.loop = loop;
            this.length = length;
        }

        static Anim parse(JsonObject root) {
            JsonObject animations = root.getAsJsonObject("animations");
            if (animations == null || animations.size() == 0) {
                return null;
            }
            JsonObject animation = animations.entrySet().iterator().next().getValue().getAsJsonObject();
            boolean loop = animation.has("loop") && animation.get("loop").isJsonPrimitive() && animation.get("loop").getAsJsonPrimitive().isBoolean() && animation.get("loop").getAsBoolean();
            float length = animation.has("animation_length") ? animation.get("animation_length").getAsFloat() : 0.0f;
            JsonObject bones = animation.getAsJsonObject("bones");
            if (bones == null) {
                return null;
            }
            Anim result = new Anim(loop, length);
            float maxKey = 0.0f;
            for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject data = entry.getValue().getAsJsonObject();
                TreeMap<Float, float[]> rotation = keyframes(data.get("rotation"));
                TreeMap<Float, float[]> position = keyframes(data.get("position"));
                if (!rotation.isEmpty()) {
                    result.rotations.put(entry.getKey(), rotation);
                    maxKey = Math.max(maxKey, rotation.lastKey());
                }
                if (!position.isEmpty()) {
                    result.positions.put(entry.getKey(), position);
                    maxKey = Math.max(maxKey, position.lastKey());
                }
            }
            Anim finished = length > 0.0f ? result : new Anim(loop, Math.max(maxKey, 1.0f));
            if (finished != result) {
                finished.rotations.putAll(result.rotations);
                finished.positions.putAll(result.positions);
            }
            return finished;
        }

        Pose sample(String bone, float time) {
            TreeMap<Float, float[]> rotation = this.rotations.get(bone);
            TreeMap<Float, float[]> position = this.positions.get(bone);
            if (rotation == null && position == null) {
                return null;
            }
            float[] r = rotation == null ? new float[3] : interpolate(rotation, time);
            float[] p = position == null ? new float[3] : interpolate(position, time);
            // Углы Bedrock в градусах; Y смещения у Bedrock направлен вверх, у модели игрока — вниз.
            return new Pose((float)Math.toRadians(r[0]), (float)Math.toRadians(r[1]), (float)Math.toRadians(r[2]), p[0], -p[1], p[2]);
        }

        private static float[] interpolate(TreeMap<Float, float[]> frames, float time) {
            Map.Entry<Float, float[]> before = frames.floorEntry(time);
            Map.Entry<Float, float[]> after = frames.ceilingEntry(time);
            if (before == null) {
                return after.getValue();
            }
            if (after == null || after.getKey().equals(before.getKey())) {
                return before.getValue();
            }
            float t = (time - before.getKey()) / (after.getKey() - before.getKey());
            float[] a = before.getValue();
            float[] b = after.getValue();
            return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
        }

        private static TreeMap<Float, float[]> keyframes(JsonElement element) {
            TreeMap<Float, float[]> frames = new TreeMap<Float, float[]>();
            if (element == null) {
                return frames;
            }
            if (element.isJsonArray()) {
                float[] value = vector(element);
                if (value != null) frames.put(0.0f, value);
                return frames;
            }
            if (!element.isJsonObject()) {
                return frames;
            }
            JsonObject object = element.getAsJsonObject();
            if (object.has("vector")) {
                float[] value = vector(object.get("vector"));
                if (value != null) frames.put(0.0f, value);
                return frames;
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                try {
                    float time = Float.parseFloat(entry.getKey());
                    JsonElement value = entry.getValue();
                    if (value.isJsonObject()) {
                        JsonObject frame = value.getAsJsonObject();
                        value = frame.has("vector") ? frame.get("vector") : frame.get("post");
                        if (value != null && value.isJsonObject() && value.getAsJsonObject().has("vector")) {
                            value = value.getAsJsonObject().get("vector");
                        }
                    }
                    float[] vector = vector(value);
                    if (vector != null) frames.put(time, vector);
                }
                catch (NumberFormatException ignored) {
                    // Ключи-выражения Molang не поддерживаются.
                }
            }
            return frames;
        }

        private static float[] vector(JsonElement element) {
            if (element == null || !element.isJsonArray()) {
                return null;
            }
            JsonArray array = element.getAsJsonArray();
            if (array.size() < 3) {
                return null;
            }
            try {
                return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
            }
            catch (Exception ignored) {
                return null;
            }
        }
    }
}
