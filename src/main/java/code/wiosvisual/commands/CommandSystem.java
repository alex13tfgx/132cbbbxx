/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.commands;

import code.wiosvisual.commands.argparser.ArgParserManager;
import code.wiosvisual.utils.client.managers.api.command.ICommandSystem;
import code.wiosvisual.utils.client.managers.api.command.argparser.IArgParserManager;

public enum CommandSystem implements ICommandSystem
{
    INSTANCE;


    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
