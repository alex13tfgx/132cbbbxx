package code.wiosvisual.features.impl.render.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Первая анимация из Bedrock-файла: ключевые кадры поворота, смещения и масштаба костей
 * с линейной интерполяцией. Время идёт от первого показа предмета.
 */
public final class CosmeticAnimation {
    private final boolean loop;
    private final float length;
    private final List<Track> tracks = new ArrayList<Track>();
    /** Исходная поза костей — от неё отсчитываются смещения анимации. */
    private final Map<GeoModel.Bone, float[]> restPose = new HashMap<GeoModel.Bone, float[]>();
    private long startTime;

    private CosmeticAnimation(boolean loop, float length) {
        this.loop = loop;
        this.length = length;
    }

    public static CosmeticAnimation parse(JsonObject root, GeoModel model) {
        return CosmeticAnimation.parse(root, model, null);
    }

    /** Анимация, в названии которой есть keyword (например walk или run); null — выбор по умолчанию. */
    public static CosmeticAnimation parse(JsonObject root, GeoModel model, String keyword) {
        if (!root.has("animations")) {
            return null;
        }
        JsonObject animations = root.getAsJsonObject("animations");
        JsonObject animation = null;
        if (keyword != null) {
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                if (entry.getValue().isJsonObject() && entry.getKey().toLowerCase().contains(keyword)) {
                    animation = entry.getValue().getAsJsonObject();
                    break;
                }
            }
            if (animation == null) {
                return null;
            }
        } else {
            animation = CosmeticAnimation.pickAnimation(animations);
        }
        if (animation == null) {
            return null;
        }
        boolean loop = animation.has("loop") && animation.get("loop").isJsonPrimitive() && animation.get("loop").getAsJsonPrimitive().isBoolean() && animation.get("loop").getAsBoolean();
        float length = animation.has("animation_length") ? animation.get("animation_length").getAsFloat() : 1.0f;
        CosmeticAnimation result = new CosmeticAnimation(loop, length);
        if (animation.has("bones")) {
            for (Map.Entry<String, JsonElement> entry : animation.getAsJsonObject("bones").entrySet()) {
                GeoModel.Bone bone = model.findBone(entry.getKey());
                if (bone == null || !entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject data = entry.getValue().getAsJsonObject();
                Track track = new Track(bone, CosmeticAnimation.keyframes(data.get("rotation")), CosmeticAnimation.keyframes(data.get("position")), CosmeticAnimation.keyframes(data.get("scale")));
                if (track.rotation.isEmpty() && track.position.isEmpty() && track.scale.isEmpty()) {
                    continue;
                }
                result.tracks.add(track);
                result.restPose.put(bone, new float[]{bone.rotationX, bone.rotationY, bone.rotationZ, bone.positionX, bone.positionY, bone.positionZ, bone.scaleX, bone.scaleY, bone.scaleZ});
            }
        }
        return result.tracks.isEmpty() ? null : result;
    }

    /**
     * В файле бывает несколько анимаций: «gui» уменьшает и сдвигает модель для превью в меню,
     * «moving»/«fly» — для полёта. На игроке нужна обычная: сначала ищем «idle», затем любую, кроме «gui».
     */
    private static JsonObject pickAnimation(JsonObject animations) {
        JsonObject fallback = null;
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            String name = entry.getKey().toLowerCase();
            if (name.contains("idle")) {
                return entry.getValue().getAsJsonObject();
            }
            if (fallback == null && !name.contains("gui")) {
                fallback = entry.getValue().getAsJsonObject();
            }
        }
        return fallback;
    }

    /** Выставляет позу костей на текущий момент времени. */
    public void apply() {
        long now = System.currentTimeMillis();
        if (this.startTime == 0L) {
            this.startTime = now;
        }
        this.applyAt((float)(now - this.startTime) / 1000.0f);
    }

    /** Ставит позу на заданную секунду анимации (для питомцев, у которых скорость зависит от бега). */
    public void applyAt(float seconds) {
        float time = this.loop && this.length > 0.0f ? seconds % this.length : Math.min(seconds, this.length);
        for (Track track : this.tracks) {
            GeoModel.Bone bone = track.bone;
            float[] rest = this.restPose.get(bone);
            if (!track.rotation.isEmpty()) {
                float[] r = CosmeticAnimation.sample(track.rotation, time);
                bone.rotationX = rest[0] + (float)Math.toRadians(-r[0]);
                bone.rotationY = rest[1] + (float)Math.toRadians(-r[1]);
                bone.rotationZ = rest[2] + (float)Math.toRadians(r[2]);
            }
            if (!track.position.isEmpty()) {
                float[] p = CosmeticAnimation.sample(track.position, time);
                bone.positionX = rest[3] + p[0];
                bone.positionY = rest[4] + p[1];
                bone.positionZ = rest[5] + p[2];
            }
            if (!track.scale.isEmpty()) {
                float[] s = CosmeticAnimation.sample(track.scale, time);
                bone.scaleX = rest[6] * s[0];
                bone.scaleY = rest[7] * s[1];
                bone.scaleZ = rest[8] * s[2];
            }
        }
    }

    private static TreeMap<Float, float[]> keyframes(JsonElement element) {
        TreeMap<Float, float[]> frames = new TreeMap<Float, float[]>();
        if (element == null) {
            return frames;
        }
        if (element.isJsonArray()) {
            // Постоянное значение без ключевых кадров.
            float[] value = CosmeticAnimation.vector(element);
            if (value != null) {
                frames.put(0.0f, value);
            }
            return frames;
        }
        if (!element.isJsonObject()) {
            return frames;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                JsonElement value = entry.getValue();
                if (value.isJsonObject()) {
                    JsonObject object = value.getAsJsonObject();
                    value = object.has("vector") ? object.get("vector") : object.get("post");
                }
                float[] vector = CosmeticAnimation.vector(value);
                if (vector != null) {
                    frames.put(time, vector);
                }
            }
            catch (NumberFormatException ignored) {
                // Ключи-выражения Molang не поддерживаются.
            }
        }
        return frames;
    }

    private static float[] vector(JsonElement element) {
        if (element == null) {
            return null;
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("vector")) {
            element = element.getAsJsonObject().get("vector");
        }
        if (!element.isJsonArray()) {
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

    private static float[] sample(TreeMap<Float, float[]> frames, float time) {
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

    private static final class Track {
        final GeoModel.Bone bone;
        final TreeMap<Float, float[]> rotation;
        final TreeMap<Float, float[]> position;
        final TreeMap<Float, float[]> scale;

        Track(GeoModel.Bone bone, TreeMap<Float, float[]> rotation, TreeMap<Float, float[]> position, TreeMap<Float, float[]> scale) {
            this.bone = bone;
            this.rotation = rotation;
            this.position = position;
            this.scale = scale;
        }
    }
}
