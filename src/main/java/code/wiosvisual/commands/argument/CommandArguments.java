/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.commands.argument;

import code.wiosvisual.commands.argument.CommandArgument;
import code.wiosvisual.utils.client.managers.api.command.argument.ICommandArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandArguments {
    private static final Pattern ARG_PATTERN = Pattern.compile("\\S+");

    private CommandArguments() {
    }

    public static List<ICommandArgument> from(String string, boolean preserveEmptyLast) {
        ArrayList<ICommandArgument> args = new ArrayList<ICommandArgument>();
        Matcher argMatcher = ARG_PATTERN.matcher(string);
        int lastEnd = -1;
        while (argMatcher.find()) {
            args.add(new CommandArgument(args.size(), argMatcher.group(), string.substring(argMatcher.start())));
            lastEnd = argMatcher.end();
        }
        if (preserveEmptyLast && lastEnd < string.length()) {
            args.add(new CommandArgument(args.size(), "", ""));
        }
        return args;
    }

    public static List<ICommandArgument> from(String string) {
        return CommandArguments.from(string, false);
    }

    public static CommandArgument unknown() {
        return new CommandArgument(-1, "<unknown>", "");
    }
}
