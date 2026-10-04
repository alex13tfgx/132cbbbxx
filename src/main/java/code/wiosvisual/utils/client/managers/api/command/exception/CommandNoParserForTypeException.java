/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.command.exception;

import code.wiosvisual.utils.client.managers.api.command.exception.CommandUnhandledException;

public class CommandNoParserForTypeException
extends CommandUnhandledException {
    public CommandNoParserForTypeException(Class<?> klass) {
        super(String.format("Could not find a handler for type %s", klass.getSimpleName()));
    }
}
