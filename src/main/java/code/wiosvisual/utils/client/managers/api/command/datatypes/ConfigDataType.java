/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.command.datatypes;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeContext;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeFor;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.wiosvisual.utils.client.managers.file.ClientFile;
import code.wiosvisual.utils.client.managers.file.impl.ModuleFile;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ConfigDataType implements IDatatypeFor<ClientFile>
{
    INSTANCE;


    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        Stream<String> friends = this.getList().stream().map(ClientFile::getName);
        String context = ctx.getConsumer().getString();
        return new TabCompleteHelper().append(friends).filterPrefix(context).sortAlphabetically().stream();
    }

    @Override
    public ClientFile get(IDatatypeContext datatypeContext) throws CommandException {
        String username = datatypeContext.getConsumer().getString();
        return this.getList().stream().filter(s -> s.getName().equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    public List<? extends ModuleFile> getList() {
        return Wiosvisual.getInstance().getFileRepository().getClientFiles().stream().filter(clientFile -> clientFile instanceof ModuleFile).map(clientFile -> (ModuleFile)clientFile).collect(Collectors.toList());
    }
}
