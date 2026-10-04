/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_2596
 *  net.minecraft.class_266
 *  net.minecraft.class_268
 *  net.minecraft.class_269
 *  net.minecraft.class_270
 *  net.minecraft.class_2761
 *  net.minecraft.class_3532
 *  net.minecraft.class_8646
 *  net.minecraft.class_9011
 *  org.apache.commons.lang3.StringUtils
 */
package code.wiosvisual.utils.client.packet.network;

import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.interactions.interact.PlayerInteractionHelper;
import code.wiosvisual.utils.math.time.StopWatch;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

public final class Network
implements QuickImports {
    private static final StopWatch pvpWatch = new StopWatch();
    public static String server = "Vanilla";
    public static float TPS = 20.0f;
    public static long timestamp;
    public static int anarchy;
    public static boolean pvpEnd;

    public static void tick() {
        anarchy = Network.getAnarchyMode();
        server = Network.getServer();
        pvpEnd = Network.inPvpEnd();
        if (Network.inPvp()) {
            pvpWatch.reset();
        }
    }

    public static void packet(PacketEvent e) {
        Packet<?> packet2 = e.getPacket();
        Objects.requireNonNull(packet2);
        Packet<?> packet3 = packet2;
        int n = 0;
        switch (code.wiosvisual.utils.TypeSwitch.typeSwitch(new Object[]{WorldTimeUpdateS2CPacket.class}, packet3, n)) {
            case 0: {
                WorldTimeUpdateS2CPacket time = (WorldTimeUpdateS2CPacket)packet3;
                long nanoTime = System.nanoTime();
                float maxTPS = 20.0f;
                float rawTPS = maxTPS * (1.0E9f / (float)(nanoTime - timestamp));
                TPS = MathHelper.clamp((float)rawTPS, (float)0.0f, (float)maxTPS);
                timestamp = nanoTime;
                break;
            }
        }
    }

    public static String getServer() {
        String title;
        ScoreboardObjective objective;
        String brand;
        if (PlayerInteractionHelper.nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) {
            return "Vanilla";
        }
        String serverIp = Network.mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String string = brand = mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand().toLowerCase() : "";
        if (brand.contains("botfilter")) {
            return "FunTime";
        }
        if (brand.contains("\u00a76spooky\u00a7ccore")) {
            return "SpookyTime";
        }
        if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) {
            return "CopyTime";
        }
        if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) {
            return "HolyWorld";
        }
        if (serverIp.contains("reallyworld")) {
            return "ReallyWorld";
        }
        if (serverIp.contains("gulpvp")) {
            return "GulPvP";
        }
        if (Network.mc.world != null && Network.mc.world.getScoreboard() != null && (objective = Network.mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)) != null && (title = objective.getDisplayName().getString()) != null && (title.equals("HolyWorld.me") || title.equals("HolyWorld") || title.toLowerCase().contains("holyworld"))) {
            return "HolyWorld";
        }
        return "Vanilla";
    }

    private static int getAnarchyMode() {
        Scoreboard scoreboard = Network.mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return -1;
        }
        switch (server) {
            case "FunTime": 
            case "SpookyTime": {
                String[] parts;
                String title = objective.getDisplayName().getString();
                if (title.contains("\u0410\u043d\u0430\u0440\u0445\u0438\u044f-") && (parts = title.split("\u0410\u043d\u0430\u0440\u0445\u0438\u044f-")).length > 1) {
                    try {
                        String numberStr = parts[1].trim().split(" ")[0];
                        return Integer.parseInt(numberStr);
                    }
                    catch (NumberFormatException numberStr) {
                        // empty catch block
                    }
                }
                List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective).stream().sorted((e1, e2) -> Integer.compare(e2.value(), e1.value())).toList();
                for (ScoreboardEntry entry : entries) {
                    String[] parts2;
                    String text = Team.decorateName((AbstractTeam)scoreboard.getScoreHolderTeam(entry.owner()), (Text)entry.name()).getString();
                    if (!text.contains("\u0410\u043d\u0430\u0440\u0445\u0438\u044f-") || (parts2 = text.split("\u0410\u043d\u0430\u0440\u0445\u0438\u044f-")).length <= 1) continue;
                    try {
                        String numberStr = parts2[1].trim().split(" ")[0];
                        return Integer.parseInt(numberStr);
                    }
                    catch (NumberFormatException numberStr) {
                    }
                }
                break;
            }
            case "HolyWorld": {
                String numberAfterHash;
                ScoreboardEntry lastEntry;
                String lastText;
                String title = objective.getDisplayName().getString();
                List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective).stream().sorted((e1, e2) -> Integer.compare(e2.value(), e1.value())).toList();
                if (!entries.isEmpty() && !(lastText = Team.decorateName((AbstractTeam)scoreboard.getScoreHolderTeam((lastEntry = entries.get(entries.size() - 1)).owner()), (Text)lastEntry.name()).getString()).isEmpty() && lastText.contains("#") && (numberAfterHash = StringUtils.substringAfter((String)lastText, (String)"#")) != null && !numberAfterHash.isEmpty()) {
                    try {
                        StringBuilder numBuilder = new StringBuilder();
                        for (char c : numberAfterHash.trim().toCharArray()) {
                            if (!Character.isDigit(c)) break;
                            numBuilder.append(c);
                        }
                        if (numBuilder.length() > 0) {
                            return Integer.parseInt(numBuilder.toString());
                        }
                    }
                    catch (NumberFormatException numBuilder) {
                        // empty catch block
                    }
                }
                for (ScoreboardEntry scoreboardEntry : entries) {
                    String numberAfterHash2;
                    String text = Team.decorateName((AbstractTeam)scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), (Text)scoreboardEntry.name()).getString();
                    if (text.isEmpty() || text.contains("\u041a\u043b\u0430\u043d:") || text.contains("\u0422\u0440\u0438\u043e:") || !text.contains("#") || (numberAfterHash2 = StringUtils.substringAfter((String)text, (String)"#")) == null || numberAfterHash2.isEmpty()) continue;
                    try {
                        StringBuilder numBuilder = new StringBuilder();
                        for (char c : numberAfterHash2.trim().toCharArray()) {
                            if (!Character.isDigit(c)) break;
                            numBuilder.append(c);
                        }
                        if (numBuilder.length() <= 0) continue;
                        return Integer.parseInt(numBuilder.toString());
                    }
                    catch (NumberFormatException numberFormatException) {
                    }
                }
                if (title == null || !title.equals("HolyWorld.me") && !title.equals("HolyWorld")) break;
                return -1;
            }
            case "ReallyWorld": {
                String title = objective.getDisplayName().getString();
                String number = StringUtils.substringAfter((String)title, (String)"#");
                if (number == null || number.isEmpty()) break;
                try {
                    return Integer.parseInt(number.trim().split(" ")[0]);
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
        }
        return -1;
    }

    public static boolean isPvp() {
        return !pvpWatch.finished(500.0);
    }

    private static boolean inPvp() {
        return Network.mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> s.contains("pvp") || s.contains("\u043f\u0432\u043f"));
    }

    private static boolean inPvpEnd() {
        return Network.mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> !(!s.contains("pvp") && !s.contains("\u043f\u0432\u043f") || !s.contains("0") && !s.contains("1")));
    }

    public static String getWorldType() {
        return Network.mc.world.getRegistryKey().getValue().getPath();
    }

    public static boolean isCopyTime() {
        return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");
    }

    public static boolean isFunTime() {
        return server.equals("FunTime");
    }

    public static boolean isReallyWorld() {
        return server.equals("ReallyWorld");
    }

    public static boolean isGulPvP() {
        return server.equals("GulPvP");
    }

    public static boolean isHolyWorld() {
        return server.equals("HolyWorld");
    }

    public static boolean isSpookyTime() {
        return server.equals("SpookyTime");
    }

    public static boolean isAresMine() {
        return server.equals("aresmine");
    }

    public static boolean isVanilla() {
        return server.equals("Vanilla");
    }

    private Network() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static int getAnarchy() {
        return anarchy;
    }

    public static boolean isPvpEnd() {
        return pvpEnd;
    }
}
