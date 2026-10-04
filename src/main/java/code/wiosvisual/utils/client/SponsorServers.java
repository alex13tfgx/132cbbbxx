package code.wiosvisual.utils.client;

import java.util.List;
import net.minecraft.client.network.ServerInfo;

/**
 * Список рекламных серверов, которые не должны попадать в servers.dat.
 *
 * Живёт вне миксина намеренно: инициализаторы полей миксина не доезжают
 * до целевого класса в обфусцированной сборке, и поле оказывается null.
 */
public final class SponsorServers {
    private static final List<ServerInfo> LIST = List.of(new ServerInfo("Лучший HvH сервер!", "dddd", ServerInfo.ServerType.REALM), new ServerInfo("Много режимов + подарки!", "ddd", ServerInfo.ServerType.REALM));

    private SponsorServers() {
    }

    public static List<ServerInfo> list() {
        return LIST;
    }

    public static boolean contains(ServerInfo info) {
        return info != null && LIST.contains(info);
    }
}
