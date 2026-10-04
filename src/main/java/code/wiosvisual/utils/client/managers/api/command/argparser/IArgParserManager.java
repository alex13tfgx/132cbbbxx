/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.command.argparser;

import code.wiosvisual.utils.client.managers.api.command.argparser.IArgParser;
import code.wiosvisual.utils.client.managers.api.command.argument.ICommandArgument;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandInvalidTypeException;
import code.wiosvisual.utils.client.managers.api.command.registry.Registry;

public interface IArgParserManager {
    public <T> IArgParser.Stateless<T> getParserStateless(Class<T> var1);

    public <T, S> IArgParser.Stated<T, S> getParserStated(Class<T> var1, Class<S> var2);

    public <T> T parseStateless(Class<T> var1, ICommandArgument var2) throws CommandInvalidTypeException;

    public <T, S> T parseStated(Class<T> var1, Class<S> var2, ICommandArgument var3, S var4) throws CommandInvalidTypeException;

    public Registry<IArgParser> getRegistry();
}
