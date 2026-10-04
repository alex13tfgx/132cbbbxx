/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.command.exception;

import code.wiosvisual.utils.client.managers.api.command.exception.CommandErrorMessageException;

public class CommandTooManyArgumentsException
extends CommandErrorMessageException {
    public CommandTooManyArgumentsException(int maxArgs) {
        super(String.format("Too many arguments (expected at most %d)", maxArgs));
    }
}
