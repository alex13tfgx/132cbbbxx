/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.command.datatypes;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeContext;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeFor;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.List;
import java.util.stream.Stream;

public enum ModuleDataType implements IDatatypeFor<Module>
{
    INSTANCE;


    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> source = this.getModules().stream().map(Module::getName);
        String context = datatypeContext.getConsumer().getString();
        return new TabCompleteHelper().append(source).filterPrefix(context).sortAlphabetically().stream();
    }

    @Override
    public Module get(IDatatypeContext datatypeContext) throws CommandException {
        String name = datatypeContext.getConsumer().getString();
        return this.getModules().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    private List<? extends Module> getModules() {
        return Wiosvisual.getInstance().getModuleRepository().modules().stream().filter(m -> !m.isHidden()).toList();
    }
}
