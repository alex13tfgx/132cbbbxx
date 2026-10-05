package code.wiosvisual.utils.client;

import code.wiosvisual.features.impl.render.Fullbright;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

/** Английские названия эффектов (независимо от языка игры) и фильтр эффектов для HUD. */
public final class EffectNames {
    /** Где английское название не совпадает с id эффекта. */
    private static final Map<String, String> OVERRIDES = Map.of(
            "unluck", "Bad Luck",
            "dolphins_grace", "Dolphin's Grace",
            "hero_of_the_village", "Hero of the Village");
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    /** Длительность больше часа считаем бесконечной (Fullbright выдаёт Integer.MAX_VALUE). */
    private static final int INFINITE_TICKS = 72000;

    private EffectNames() { }

    public static String of(RegistryEntry<StatusEffect> effect) {
        return CACHE.computeIfAbsent(effect.getIdAsString(), EffectNames::fromId);
    }

    private static String fromId(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        String o = OVERRIDES.get(path);
        if (o != null) return o;
        StringBuilder sb = new StringBuilder(path.length());
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    public static boolean isInfinite(StatusEffectInstance effect) {
        return effect.isInfinite() || effect.getDuration() > INFINITE_TICKS;
    }

    /** Бесконечное ночное зрение при включённом Fullbright в HUD не показываем. */
    public static boolean hidden(StatusEffectInstance effect) {
        if (!effect.getEffectType().getIdAsString().equals(StatusEffects.NIGHT_VISION.getIdAsString()) || !isInfinite(effect)) return false;
        Fullbright fb = Fullbright.getInstance();
        return fb != null && fb.isState();
    }
}
