/*
 * .friend — управление списком друзей. Добавленные игроки подсвечиваются
 * в HitboxCustomizer отдельным цветом (по умолчанию зелёным).
 */
package code.wiosvisual.commands.defaults;

import code.wiosvisual.common.repository.friend.Friend;
import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.utils.client.managers.api.command.Command;
import code.wiosvisual.utils.client.managers.api.command.IBaritoneChatControl;
import code.wiosvisual.utils.client.managers.api.command.argument.IArgConsumer;
import code.wiosvisual.utils.client.managers.api.command.datatypes.FriendDataType;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.Paginator;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FriendCommand
extends Command {
    public FriendCommand() {
        super("friend");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add": {
                this.handleAdd(args);
                break;
            }
            case "remove": {
                this.handleRemove(args);
                break;
            }
            case "clear": {
                this.handleClear(args);
                break;
            }
            default: {
                this.handleList(args, label);
            }
        }
    }

    private void handleAdd(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();
        if (FriendUtils.isFriend(name)) {
            this.logDirect(String.valueOf(Formatting.RED) + name + String.valueOf(Formatting.WHITE) + " уже в списке друзей.");
            return;
        }
        FriendUtils.addFriend(name);
        this.logDirect(String.valueOf(Formatting.WHITE) + "Игрок " + String.valueOf(Formatting.GREEN) + name + String.valueOf(Formatting.WHITE) + " добавлен в друзья — теперь его хитбокс подсвечивается.");
    }

    private void handleRemove(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();
        if (!FriendUtils.isFriend(name)) {
            this.logDirect(String.valueOf(Formatting.RED) + name + String.valueOf(Formatting.WHITE) + " не найден в списке друзей.");
            return;
        }
        FriendUtils.removeFriend(name);
        this.logDirect(String.valueOf(Formatting.WHITE) + "Игрок " + String.valueOf(Formatting.RED) + name + String.valueOf(Formatting.WHITE) + " удалён из друзей.");
    }

    private void handleClear(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        FriendUtils.clear();
        this.logDirect("Список друзей очищен.", Formatting.WHITE);
    }

    private void handleList(IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);
        List<Friend> friends = List.copyOf(FriendUtils.getFriends());
        if (friends.isEmpty()) {
            this.logDirect("Список друзей пуст. Добавить: .friend add <ник>", Formatting.WHITE);
            return;
        }
        Paginator.paginate(args, new Paginator<Friend>(friends), () -> this.logDirect("Друзья:"), friend -> Text.literal((String)(String.valueOf(Formatting.GRAY) + "Ник: " + String.valueOf(Formatting.GREEN) + friend.getName())), IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
        }
        String arg = args.getString();
        if (arg.equalsIgnoreCase("remove") && args.hasExactly(1)) {
            return args.tabCompleteDatatype(FriendDataType.INSTANCE);
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление списком друзей";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("Друзья подсвечиваются отдельным цветом в модуле HitboxCustomizer.", "", "Использование:", "> friend add <ник> - Добавляет игрока в друзья.", "> friend remove <ник> - Убирает игрока из друзей.", "> friend list - Показывает список друзей.", "> friend clear - Очищает список друзей.");
    }
}
