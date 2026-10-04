package code.wiosvisual.features.impl.render.custommodels;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Проигрывает анимации Blockbench по состоянию игрока — так же, как это делают скрипты
 * Figura-аватаров: стоит → idle, идёт → walk, присел → crouch, удар → attack,
 * постоянные позы (keypose, armrot) — всегда. Переходы плавно смешиваются.
 */
public final class ModelAnimator {
    /** Сумма анимаций для группы: поворот (градусы), позиция (пиксели), масштаб. */
    public static final class Pose {
        public final float[] rot = new float[3];
        public final float[] pos = new float[3];
        public final float[] scale = {1f, 1f, 1f};
    }

    private static final class Playing {
        float time;
        float weight;
        boolean wanted;
    }

    private static final class EntityState {
        BbModel model;
        final Map<BbModel.Animation, Playing> playing = new HashMap<>();
        long last;
        boolean swinging;
        boolean attack;
    }

    private static final String[] ALWAYS = {"keypose", "armrot"};
    private static final String[] IDLE = {"idle"};
    private static final String[] WALK = {"walk", "walking", "run"};
    private static final String[] CROUCH = {"crouch", "crouching", "sneak", "sneaking"};
    private static final String[] ATTACK = {"attack", "attackr", "punch", "swing"};
    /** Скорость смешивания (1/с): ~0.15 с на переход. */
    private static final float BLEND = 14f;

    private static final Map<Integer, EntityState> STATES = new HashMap<>();

    private ModelAnimator() {
    }

    /**
     * Продвигает анимации игрока на прошедшее время и возвращает итоговые позы групп (по uuid).
     */
    public static Map<String, Pose> update(BbModel model, int entityId, boolean moving, boolean sneaking, boolean swinging) {
        long now = System.nanoTime();
        cleanup(now);
        EntityState st = STATES.computeIfAbsent(entityId, id -> new EntityState());
        if (st.model != model) {
            st.model = model;
            st.playing.clear();
            st.last = 0;
        }
        float dt = st.last == 0 ? 0f : Math.min(0.1f, (now - st.last) / 1e9f);
        st.last = now;

        // удар запускает attack с начала — по фронту взмаха руки
        if (swinging && !st.swinging && find(model, ATTACK) != null) {
            st.attack = true;
            Playing p = st.playing.computeIfAbsent(find(model, ATTACK), a -> new Playing());
            p.time = 0;
        }
        st.swinging = swinging;

        for (BbModel.Animation anim : model.animations) {
            Playing p = st.playing.computeIfAbsent(anim, a -> new Playing());
            String name = anim.name.toLowerCase(Locale.ROOT).trim();
            boolean wanted;
            if (is(name, ATTACK)) {
                wanted = st.attack;
            } else if (is(name, ALWAYS)) {
                wanted = true;
            } else if (is(name, CROUCH)) {
                wanted = sneaking;
            } else if (is(name, WALK)) {
                wanted = moving;
            } else if (is(name, IDLE)) {
                wanted = !moving;
            } else {
                wanted = false;
            }
            if (wanted && !p.wanted && p.weight < 0.01f) {
                p.time = 0;
            }
            p.wanted = wanted;
            if (wanted) {
                p.time += dt;
                if (is(name, ATTACK) && anim.length > 0 && p.time >= anim.length) {
                    st.attack = false; // удар доигран — плавно возвращаемся к ходьбе/стойке
                    p.wanted = false;
                }
            }
            float target = p.wanted ? 1f : 0f;
            p.weight += (target - p.weight) * (1f - (float) Math.exp(-BLEND * dt));
            if (dt == 0f) {
                p.weight = target; // первый кадр — сразу в позу
            }
            if (!p.wanted && p.weight < 0.01f) {
                p.weight = 0f;
                p.time = 0f;
            }
        }

        Map<String, Pose> poses = new HashMap<>();
        for (Map.Entry<BbModel.Animation, Playing> e : st.playing.entrySet()) {
            Playing p = e.getValue();
            if (p.weight <= 0f) {
                continue;
            }
            BbModel.Animation anim = e.getKey();
            float t = localTime(anim, p.time);
            for (Map.Entry<String, BbModel.Track[]> tr : anim.tracks.entrySet()) {
                Pose pose = poses.computeIfAbsent(tr.getKey(), k -> new Pose());
                BbModel.Track[] tracks = tr.getValue();
                if (tracks[BbModel.ROT] != null) {
                    float[] v = sample(tracks[BbModel.ROT], t);
                    for (int i = 0; i < 3; i++) pose.rot[i] += v[i] * p.weight;
                }
                if (tracks[BbModel.POS] != null) {
                    float[] v = sample(tracks[BbModel.POS], t);
                    for (int i = 0; i < 3; i++) pose.pos[i] += v[i] * p.weight;
                }
                if (tracks[BbModel.SCALE] != null) {
                    float[] v = sample(tracks[BbModel.SCALE], t);
                    for (int i = 0; i < 3; i++) pose.scale[i] *= 1f + (v[i] - 1f) * p.weight;
                }
            }
        }
        return poses;
    }

    private static float localTime(BbModel.Animation anim, float time) {
        if (anim.length <= 0f) {
            return 0f;
        }
        if ("loop".equals(anim.loop)) {
            return time % anim.length;
        }
        return Math.min(time, anim.length); // once и hold держат последний кадр
    }

    /** Значение дорожки в момент t (linear / catmullrom / step, как в Blockbench). */
    static float[] sample(BbModel.Track track, float t) {
        java.util.List<BbModel.Keyframe> keys = track.keys;
        if (keys.size() == 1 || t <= keys.get(0).time) {
            return keys.get(0).value;
        }
        BbModel.Keyframe lastKey = keys.get(keys.size() - 1);
        if (t >= lastKey.time) {
            return lastKey.value;
        }
        int i = 0;
        while (i < keys.size() - 2 && keys.get(i + 1).time <= t) {
            i++;
        }
        BbModel.Keyframe a = keys.get(i);
        BbModel.Keyframe b = keys.get(i + 1);
        float span = b.time - a.time;
        float alpha = span <= 0f ? 1f : (t - a.time) / span;
        float[] out = new float[3];
        if ("step".equals(a.interpolation)) {
            return a.value;
        }
        if ("catmullrom".equals(a.interpolation) || "catmullrom".equals(b.interpolation)) {
            float[] p0 = i > 0 ? keys.get(i - 1).value : a.value;
            float[] p3 = i + 2 < keys.size() ? keys.get(i + 2).value : b.value;
            for (int k = 0; k < 3; k++) {
                out[k] = catmull(p0[k], a.value[k], b.value[k], p3[k], alpha);
            }
            return out;
        }
        for (int k = 0; k < 3; k++) {
            out[k] = a.value[k] + (b.value[k] - a.value[k]) * alpha;
        }
        return out;
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * ((2f * p1) + (-p0 + p2) * t + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }

    private static boolean is(String name, String[] names) {
        for (String n : names) {
            if (n.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static BbModel.Animation find(BbModel model, String[] names) {
        for (String n : names) {
            BbModel.Animation a = model.animation(n);
            if (a != null) {
                return a;
            }
        }
        return null;
    }

    /** Игроки, которых давно не рисовали, забываются. */
    private static void cleanup(long now) {
        if (STATES.size() < 32) {
            return;
        }
        Iterator<EntityState> it = STATES.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().last > 5_000_000_000L) {
                it.remove();
            }
        }
    }

    /**
     * Поведение из Lua-скрипта модели «67»: руки вытянуты вперёд и по очереди качаются,
     * цифры 6 и 7 покачиваются и вращаются над головой. Возвращает {rot xyz, pos xyz} или null.
     */
    public static float[] scripted(BbModel model, String group, float ageTicks, boolean moving) {
        if (!"six_seven".equals(model.id)) {
            return null;
        }
        float speed = moving ? 0.55f : 0.4f;
        float swing = (float) Math.sin(ageTicks * speed) * 22f;
        switch (group) {
            case "ArmR":
                return new float[]{75f + swing, 0f, -6f, 0f, 0f, 0f};
            case "ArmL":
                return new float[]{75f - swing, 0f, 6f, 0f, 0f, 0f};
            case "Digits":
                return new float[]{0f, (ageTicks * 3f) % 360f, 0f, 0f, (float) Math.sin(ageTicks * 0.12f) * 1.5f, 0f};
            case "Digit6":
                return new float[]{0f, 0f, 0f, 0f, Math.max(0f, (float) Math.sin(ageTicks * speed)) * 1.5f, 0f};
            case "Digit7":
                return new float[]{0f, 0f, 0f, 0f, Math.max(0f, (float) -Math.sin(ageTicks * speed)) * 1.5f, 0f};
            default:
                return null;
        }
    }
}
