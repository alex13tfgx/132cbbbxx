/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3545
 */
package code.wiosvisual.utils.client.managers.api.command.manager;

import code.wiosvisual.utils.client.managers.api.command.ICommand;
import code.wiosvisual.utils.client.managers.api.command.argument.ICommandArgument;
import code.wiosvisual.utils.client.managers.api.command.registry.Registry;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Pair;

public interface ICommandManager {
    public Registry<ICommand> getRegistry();

    public ICommand getCommand(String var1);

    public boolean execute(String var1);

    public boolean execute(Pair<String, List<ICommandArgument>> var1);

    public Stream<String> tabComplete(Pair<String, List<ICommandArgument>> var1);

    public Stream<String> tabComplete(String var1);
}
