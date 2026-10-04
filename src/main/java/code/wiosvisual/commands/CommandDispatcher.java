/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3545
 */
package code.wiosvisual.commands;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.commands.argument.ArgConsumer;
import code.wiosvisual.commands.argument.CommandArguments;
import code.wiosvisual.commands.manager.CommandRepository;
import code.wiosvisual.events.chat.ChatEvent;
import code.wiosvisual.events.chat.TabCompleteEvent;
import code.wiosvisual.utils.client.managers.api.command.IBaritoneChatControl;
import code.wiosvisual.utils.client.managers.api.command.argument.ICommandArgument;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandNotEnoughArgumentsException;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandNotFoundException;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.wiosvisual.utils.client.managers.api.command.manager.ICommandManager;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import code.wiosvisual.utils.interactions.path.BaritoneHelper;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Pair;

public class CommandDispatcher
implements QuickLogger {
    private final ICommandManager manager = Wiosvisual.getInstance().getCommandRepository();
    public static String prefix = ".";

    public CommandDispatcher(EventManager eventManager) {
        EventManager.register(this);
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        String msg = event.getMessage();
        if (msg.startsWith("/")) {
            return;
        }
        if (BaritoneHelper.handleChatCommand(msg)) {
            event.cancel();
            return;
        }
        boolean forceRun = msg.startsWith(IBaritoneChatControl.FORCE_COMMAND_PREFIX);
        if (msg.startsWith(prefix) || forceRun) {
            event.cancel();
            String commandStr = msg.substring(forceRun ? IBaritoneChatControl.FORCE_COMMAND_PREFIX.length() : prefix.length());
            if (!this.runCommand(commandStr) && !commandStr.trim().isEmpty()) {
                new CommandNotFoundException((String)CommandRepository.expand(commandStr).getLeft()).handle(null, null);
            }
        }
    }

    public boolean runCommand(String msg) {
        if (msg.isEmpty()) {
            return this.runCommand("help");
        }
        Pair<String, List<ICommandArgument>> pair = CommandRepository.expand(msg);
        String command = (String)pair.getLeft();
        String rest = msg.substring(((String)pair.getLeft()).length());
        ArgConsumer argc = new ArgConsumer(this.manager, (List)pair.getRight());
        return this.manager.execute(pair);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String eventPrefix = event.prefix;
        if (!eventPrefix.startsWith(prefix)) {
            return;
        }
        String msg = eventPrefix.substring(prefix.length());
        List<ICommandArgument> args = CommandArguments.from(msg, true);
        Stream<String> stream = this.tabComplete(msg);
        if (args.size() == 1) {
            stream = stream.map(x -> prefix + x);
        }
        event.completions = (String[])stream.toArray(String[]::new);
    }

    public Stream<String> tabComplete(String msg) {
        try {
            List<ICommandArgument> args = CommandArguments.from(msg, true);
            ArgConsumer argc = new ArgConsumer(this.manager, args);
            if (argc.hasAtMost(2) && argc.hasExactly(1)) {
                return new TabCompleteHelper().addCommands(this.manager).filterPrefix(argc.getString()).stream();
            }
            return this.manager.tabComplete(msg);
        }
        catch (CommandNotEnoughArgumentsException ignored) {
            return Stream.empty();
        }
    }
}
