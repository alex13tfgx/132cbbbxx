/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.commands.defaults;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.commands.defaults.BindCommand;
import code.wiosvisual.commands.defaults.ChatCommand;
import code.wiosvisual.commands.defaults.ConfigCommand;
import code.wiosvisual.commands.defaults.FriendCommand;
import code.wiosvisual.commands.defaults.GpsCommand;
import code.wiosvisual.commands.defaults.MacroCommand;
import code.wiosvisual.commands.defaults.WayCommand;
import code.wiosvisual.utils.client.managers.api.command.Command;
import code.wiosvisual.utils.client.managers.api.command.ICommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {
    public static List<ICommand> createAll() {
        Wiosvisual main = Wiosvisual.getInstance();
        ArrayList<Command> commands = new ArrayList<Command>(Arrays.asList(new ConfigCommand(main), new MacroCommand(main), new BindCommand(main), new ChatCommand(), new FriendCommand(), new GpsCommand(), new WayCommand(main)));
        return Collections.unmodifiableList(commands);
    }
}
