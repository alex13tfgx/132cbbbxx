/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.command.datatypes;

import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeContext;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.stream.Stream;

public interface IDatatype
extends QuickImports {
    public Stream<String> tabComplete(IDatatypeContext var1) throws CommandException;
}
