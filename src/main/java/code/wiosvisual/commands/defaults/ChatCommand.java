package code.wiosvisual.commands.defaults;

import code.wiosvisual.utils.client.managers.api.command.Command;
import code.wiosvisual.utils.client.managers.api.command.argument.IArgConsumer;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.net.BackendConfig;
import code.wiosvisual.utils.client.net.ClientRole;
import code.wiosvisual.utils.client.net.OnlineUser;
import code.wiosvisual.utils.client.net.PresenceService;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;

/**
 * .chat <текст>   — написать в общий чат клиента
 * .chat online    — список тех, кто сейчас в клиенте, с ролями
 * .chat toggle    — показывать ли сообщения общего чата в игровом чате
 */
public class ChatCommand extends Command {
    public ChatCommand() {
        super("chat");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        PresenceService presence = PresenceService.getInstance();
        if (!BackendConfig.isConfigured()) {
            this.logDirect("Адрес бэкенда не настроен: заполните Wiosvisual/files/" + BackendConfig.FILE_NAME, Formatting.RED);
            return;
        }
        if (!args.hasAny()) {
            this.logDirect("Использование: .chat <текст> | .chat online | .chat toggle", Formatting.YELLOW);
            return;
        }
        String rest = args.rawRest().trim();
        String first = rest.split("\\s+")[0].toLowerCase(Locale.ROOT);

        if ("online".equals(first)) {
            List<OnlineUser> users = presence.getOnlineUsers();
            if (users.isEmpty()) {
                this.logDirect("Сейчас никого нет в сети.", Formatting.GRAY);
                return;
            }
            this.logDirect("В сети: " + users.size(), Formatting.AQUA);
            for (OnlineUser user : users) {
                ClientRole role = user.role();
                this.logDirect(user.nameForList() + " — " + role.label()
                        + (user.server() == null || user.server().isBlank() ? "" : " (" + user.server() + ")"), Formatting.WHITE);
            }
            return;
        }

        if ("toggle".equals(first)) {
            presence.setChatInGame(!presence.isChatInGame());
            this.logDirect("Общий чат в игре: " + (presence.isChatInGame() ? "включён" : "выключен"), Formatting.YELLOW);
            return;
        }

        if (!presence.sendChat(rest)) {
            this.logDirect("Нет связи с бэкендом — сообщение не отправлено.", Formatting.RED);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Общий чат клиента и список онлайна";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Общий чат клиента: сообщения видны всем, кто сейчас в сети, и на сайте.",
                "",
                "Использование:",
                "> chat <текст> - отправить сообщение",
                "> chat online - кто сейчас в сети и с какими ролями",
                "> chat toggle - показывать сообщения в игровом чате или нет");
    }
}
