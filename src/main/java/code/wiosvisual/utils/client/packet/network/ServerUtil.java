/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1308
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_266
 *  net.minecraft.class_269
 *  net.minecraft.class_3532
 *  net.minecraft.class_5250
 *  net.minecraft.class_8646
 *  net.minecraft.class_9013
 *  net.minecraft.class_9015
 *  net.minecraft.class_9022
 *  net.minecraft.class_9025
 */
package code.wiosvisual.utils.client.packet.network;

import code.wiosvisual.utils.client.packet.network.Network;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.MathHelper;

public final class ServerUtil
implements QuickImports {
    private static final Map<UUID, Float> lastValidHealth = new WeakHashMap<UUID, Float>();
    private static long scoreboardCacheTick = -1L;
    private static final Map<UUID, Float> scoreboardCache = new WeakHashMap<UUID, Float>();

    public static float getHealthFraction(LivingEntity target) {
        if (target == null) {
            return 0.0f;
        }
        if (target instanceof MobEntity) {
            return ServerUtil.safeVanillaFraction(target);
        }
        if (mc.getCurrentServerEntry() == null || mc.isConnectedToLocalServer()) {
            return ServerUtil.safeVanillaFraction(target);
        }
        float maxHp = ServerUtil.getMaxHealth(target);
        if (maxHp <= 0.0f) {
            return 0.0f;
        }
        return MathHelper.clamp((float)(ServerUtil.getHealthValue(target) / maxHp), (float)0.0f, (float)1.0f);
    }

    public static float getHealthValue(LivingEntity target) {
        Float lastKnown;
        if (target == null) {
            return 0.0f;
        }
        float vanillaHp = target.getHealth();
        if (!(target instanceof PlayerEntity)) {
            return MathHelper.clamp((float)vanillaHp, (float)0.0f, (float)target.getMaxHealth());
        }
        PlayerEntity player = (PlayerEntity)target;
        if (!player.isAlive()) {
            lastValidHealth.remove(player.getUuid());
            return 0.0f;
        }
        float scoreboardHp = ServerUtil.getCachedScoreboardHealth(player);
        if (scoreboardHp > 0.0f) {
            lastValidHealth.put(player.getUuid(), Float.valueOf(scoreboardHp));
            return MathHelper.clamp((float)scoreboardHp, (float)0.0f, (float)ServerUtil.getMaxHealth(target));
        }
        if (ServerUtil.usesExtendedHealthScale() && mc.getCurrentServerEntry() != null && !mc.isConnectedToLocalServer() && (lastKnown = lastValidHealth.get(player.getUuid())) != null && lastKnown.floatValue() > 0.0f) {
            return MathHelper.clamp((float)lastKnown.floatValue(), (float)0.0f, (float)ServerUtil.getMaxHealth(target));
        }
        return MathHelper.clamp((float)vanillaHp, (float)0.0f, (float)ServerUtil.getMaxHealth(target));
    }

    public static float getMaxHealth(LivingEntity target) {
        if (target == null) {
            return 20.0f;
        }
        if (target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)target;
            if (ServerUtil.usesExtendedHealthScale()) {
                Float lastKnown;
                float scoreboardHp = ServerUtil.getCachedScoreboardHealth(player);
                if (scoreboardHp <= 0.0f && (lastKnown = lastValidHealth.get(player.getUuid())) != null && lastKnown.floatValue() > 0.0f) {
                    scoreboardHp = lastKnown.floatValue();
                }
                if (scoreboardHp > 0.0f) {
                    float vanillaMax = target.getMaxHealth();
                    return scoreboardHp <= vanillaMax ? vanillaMax : ServerUtil.getExtendedMaxHealth();
                }
            }
        }
        return target.getMaxHealth();
    }

    public static float getDisplayHealth(LivingEntity target, float healthFraction) {
        if (target == null) {
            return 0.0f;
        }
        if (target instanceof PlayerEntity && ServerUtil.usesExtendedHealthScale() && mc.getCurrentServerEntry() != null && !mc.isConnectedToLocalServer()) {
            return ServerUtil.getHealthValue(target);
        }
        return healthFraction * ServerUtil.getMaxHealth(target);
    }

    private static float getCachedScoreboardHealth(PlayerEntity player) {
        long tick;
        long l = tick = ServerUtil.mc.world != null ? ServerUtil.mc.world.getTimeOfDay() : 0L;
        if (scoreboardCacheTick != tick) {
            scoreboardCache.clear();
            scoreboardCacheTick = tick;
        }
        return scoreboardCache.computeIfAbsent(player.getUuid(), uuid -> Float.valueOf(ServerUtil.readScoreboardHealth(player))).floatValue();
    }

    private static float readScoreboardHealth(PlayerEntity player) {
        Scoreboard scoreboard = player.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (objective == null) {
            return -1.0f;
        }
        if (ServerUtil.usesReallyWorldScoreboard()) {
            try {
                int score = scoreboard.getOrCreateScore(ScoreHolder.fromName((String)player.getNameForScoreboard()), objective).getScore();
                if (score >= 0) {
                    if (ServerUtil.usesExtendedHealthScale()) {
                        return score > 0 ? (float)score : -1.0f;
                    }
                    if ((float)score <= player.getMaxHealth()) {
                        return score;
                    }
                }
            }
            catch (RuntimeException score) {
                // empty catch block
            }
        }
        if (ServerUtil.usesFormattedScoreboardHealth()) {
            try {
                MutableText formatted = ReadableScoreboardScore.getFormattedScore((ReadableScoreboardScore)scoreboard.getScore((ScoreHolder)player, objective), (NumberFormat)objective.getNumberFormatOr((NumberFormat)StyledNumberFormat.EMPTY));
                float parsed = Float.parseFloat(ColorAssist.removeFormatting(formatted.getString()));
                if (parsed > 0.0f) {
                    return parsed;
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return -1.0f;
    }

    private static float getExtendedMaxHealth() {
        return 1000.0f;
    }

    private static boolean usesReallyWorldScoreboard() {
        if (ServerUtil.usesExtendedHealthScale()) {
            return true;
        }
        String address = ServerUtil.getServerAddress();
        return address.contains("reallyworld") || address.contains("playrw") || address.contains("saturn-x") || address.contains("skytime") || address.contains("space-times");
    }

    private static boolean usesFormattedScoreboardHealth() {
        return ServerUtil.usesExtendedHealthScale() || ServerUtil.usesReallyWorldScoreboard();
    }

    private static boolean usesExtendedHealthScale() {
        return switch (Network.server) {
            case "FunTime", "ReallyWorld", "GulPvP", "HolyWorld", "HopLite", "CopyTime" -> true;
            default -> false;
        };
    }

    private static String getServerAddress() {
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) {
            return "";
        }
        String address = ServerUtil.mc.getNetworkHandler().getServerInfo().address;
        return address == null ? "" : address.toLowerCase(Locale.ROOT);
    }

    private static float safeVanillaFraction(LivingEntity target) {
        float max = target.getMaxHealth();
        return max > 0.0f ? MathHelper.clamp((float)(target.getHealth() / max), (float)0.0f, (float)1.0f) : 0.0f;
    }

    private ServerUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
