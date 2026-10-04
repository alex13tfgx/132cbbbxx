package code.wiosvisual.utils.client.net;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

/**
 * Приводит адрес текущего сервера к человекочитаемому виду для нашего API.
 * Раньше в презенс уходила голая строка подключения, поэтому один и тот же
 * HolyWorld выглядел в онлайн-листе как «mc.holyworld.ru», «play.holyworld.ru»
 * и так далее — сгруппировать игроков по серверу было нельзя.
 */
public final class ServerIdentity {
    public static final String SINGLEPLAYER = "Одиночная игра";
    public static final String HOLYWORLD = "HolyWorld";
    private static final String HOLYWORLD_DOMAIN = "holyworld";

    /** Ключевые слова режимов HolyWorld в том виде, в каком они попадаются на табло. */
    private static final Map<String, String> HOLYWORLD_MODES = new LinkedHashMap<>();

    static {
        HOLYWORLD_MODES.put("анарх", "Анархия");
        HOLYWORLD_MODES.put("anarch", "Анархия");
        HOLYWORLD_MODES.put("гриф", "Гриф");
        HOLYWORLD_MODES.put("grief", "Гриф");
        HOLYWORLD_MODES.put("магия", "Магия");
        HOLYWORLD_MODES.put("magic", "Магия");
        HOLYWORLD_MODES.put("техно", "Техно");
        HOLYWORLD_MODES.put("techno", "Техно");
        HOLYWORLD_MODES.put("ванил", "Ванилла");
        HOLYWORLD_MODES.put("vanilla", "Ванилла");
        HOLYWORLD_MODES.put("хардкор", "Хардкор");
        HOLYWORLD_MODES.put("hardcore", "Хардкор");
        HOLYWORLD_MODES.put("скайблок", "СкайБлок");
        HOLYWORLD_MODES.put("skyblock", "СкайБлок");
        HOLYWORLD_MODES.put("приват", "Приват");
        HOLYWORLD_MODES.put("лобби", "Лобби");
        HOLYWORLD_MODES.put("lobby", "Лобби");
        HOLYWORLD_MODES.put("hub", "Лобби");
    }

    private ServerIdentity() {
    }

    /** Название сервера для презенса: «HolyWorld · Анархия», «Одиночная игра» или сам адрес. */
    public static String describeCurrent() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return "";
        }
        if (mc.isIntegratedServerRunning()) {
            return SINGLEPLAYER;
        }
        String address = currentAddress(mc);
        if (address.isEmpty()) {
            return "";
        }
        if (!isHolyWorld(address)) {
            return address;
        }
        String mode = currentHolyWorldMode(mc);
        return mode.isEmpty() ? HOLYWORLD : HOLYWORLD + " · " + mode;
    }

    public static boolean isHolyWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && !mc.isIntegratedServerRunning() && isHolyWorld(currentAddress(mc));
    }

    private static boolean isHolyWorld(String address) {
        // Адрес бывает и с портом, и с любым поддоменом, поэтому смотрим на само имя домена.
        return host(address).contains(HOLYWORLD_DOMAIN);
    }

    private static String currentAddress(MinecraftClient mc) {
        ServerInfo server = mc.getCurrentServerEntry();
        if (server == null || server.address == null || server.address.isBlank()) {
            return "";
        }
        return server.address.trim();
    }

    private static String host(String address) {
        String host = address.toLowerCase(Locale.ROOT);
        int port = host.indexOf(58);
        if (port >= 0) {
            host = host.substring(0, port);
        }
        return host;
    }

    /**
     * Режим берём с заголовка бокового табло: у HolyWorld там висит название режима,
     * а отдельного пакета с ним сервер не присылает.
     */
    private static String currentHolyWorldMode(MinecraftClient mc) {
        if (mc.world == null) {
            return "";
        }
        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) {
            return "";
        }
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null || sidebar.getDisplayName() == null) {
            return "";
        }
        String title = sidebar.getDisplayName().getString().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> mode : HOLYWORLD_MODES.entrySet()) {
            if (title.contains(mode.getKey())) {
                return mode.getValue();
            }
        }
        return "";
    }
}
