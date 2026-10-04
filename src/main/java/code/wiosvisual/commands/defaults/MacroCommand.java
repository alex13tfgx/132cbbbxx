/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 */
package code.wiosvisual.commands.defaults;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.repository.macro.Macro;
import code.wiosvisual.common.repository.macro.MacroRepository;
import code.wiosvisual.utils.client.chat.StringHelper;
import code.wiosvisual.utils.client.managers.api.command.Command;
import code.wiosvisual.utils.client.managers.api.command.IBaritoneChatControl;
import code.wiosvisual.utils.client.managers.api.command.argument.IArgConsumer;
import code.wiosvisual.utils.client.managers.api.command.datatypes.KeyDataType;
import code.wiosvisual.utils.client.managers.api.command.datatypes.MacroDataType;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.Paginator;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MacroCommand
extends Command {
    private final MacroRepository macroRepository;

    public MacroCommand(Wiosvisual main) {
        super("macro", "macros");
        this.macroRepository = main.getMacroRepository();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action;
        switch (action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list") {
            case "add": {
                this.handleAddMacro(args);
                break;
            }
            case "remove": {
                this.handleRemoveMacro(args);
                break;
            }
            case "list": {
                this.handleListMacros(args, label);
                break;
            }
            case "clear": {
                this.handleClearMacros(args);
            }
        }
    }

    private void handleAddMacro(IArgConsumer args) throws CommandException {
        args.requireMin(3);
        int key = (Integer)((Map.Entry)args.getDatatypeFor(KeyDataType.INSTANCE)).getValue();
        String name = args.getString();
        String command = args.rawRest();
        if (this.macroRepository.hasMacro(name)) {
            this.logDirect("\u041c\u0430\u043a\u0440\u043e\u0441 \u0441 \u0442\u0430\u043a\u0438\u043c \u0438\u043c\u0435\u043d\u0435\u043c \u0443\u0436\u0435 \u0435\u0441\u0442\u044c \u0432 \u0441\u043f\u0438\u0441\u043a\u0435!", Formatting.RED);
            return;
        }
        this.macroRepository.addMacro(name, command, key);
        this.logDirect(String.valueOf(Formatting.WHITE) + "\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d \u043c\u0430\u043a\u0440\u043e\u0441 \u0441 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435\u043c " + String.valueOf(Formatting.RED) + name + String.valueOf(Formatting.WHITE) + " \u0441 \u043a\u043d\u043e\u043f\u043a\u043e\u0439 " + String.valueOf(Formatting.RED) + StringHelper.getBindName(key).toLowerCase() + String.valueOf(Formatting.WHITE) + " \u0441 \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439 " + String.valueOf(Formatting.RED) + command);
    }

    private void handleRemoveMacro(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String name = args.getString();
        if (this.macroRepository.hasMacro(name)) {
            this.macroRepository.deleteMacro(name);
            this.logDirect(String.valueOf(Formatting.WHITE) + "\u041c\u0430\u043a\u0440\u043e\u0441 " + String.valueOf(Formatting.RED) + name + String.valueOf(Formatting.WHITE) + " \u0431\u044b\u043b \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0443\u0434\u0430\u043b\u0435\u043d!");
        } else {
            this.logDirect("\u041c\u0430\u043a\u0440\u043e\u0441 \u0441 \u0442\u0430\u043a\u0438\u043c \u0438\u043c\u0435\u043d\u0435\u043c \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!", Formatting.RED);
        }
    }

    private void handleListMacros(IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);
        Paginator.paginate(args, new Paginator<Macro>(this.macroRepository.macroList), () -> this.logDirect("\u0421\u043f\u0438\u0441\u043e\u043a \u043c\u0430\u043a\u0440\u043e\u0441\u043e\u0432:"), macro -> {
            String names = macro.name();
            String keys = StringHelper.getBindName(macro.key()).toLowerCase();
            String command = macro.message();
            return Text.literal((String)(String.valueOf(Formatting.GRAY) + "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435: " + String.valueOf(Formatting.WHITE) + names)).append((Text)Text.literal((String)(String.valueOf(Formatting.GRAY) + " \u041a\u043b\u0430\u0432\u0438\u0448\u0430: " + String.valueOf(Formatting.WHITE) + keys))).append((Text)Text.literal((String)(String.valueOf(Formatting.GRAY) + " \u041a\u043e\u043c\u0430\u043d\u0434\u0430: " + String.valueOf(Formatting.WHITE) + command)));
        }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
    }

    private void handleClearMacros(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        this.macroRepository.clearList();
        this.logDirect("\u0412\u0441\u0435 \u043c\u0430\u043a\u0440\u043e\u0441\u044b \u0431\u044b\u043b\u0438 \u0443\u0434\u0430\u043b\u0435\u043d\u044b.", Formatting.WHITE);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny() && args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
        }
        if (args.hasAny()) {
            String arg = args.getString();
            if (arg.equalsIgnoreCase("add") && args.hasExactlyOne()) {
                return args.tabCompleteDatatype(KeyDataType.INSTANCE);
            }
            if (arg.equalsIgnoreCase("remove") && args.hasExactlyOne()) {
                return args.tabCompleteDatatype(MacroDataType.INSTANCE);
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "\u041f\u043e\u0437\u0432\u043e\u043b\u044f\u0435\u0442 \u0443\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c \u043c\u0430\u043a\u0440\u043e\u0441\u0430\u043c\u0438";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("\u042d\u0442\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u0430 \u043f\u043e\u0437\u0432\u043e\u043b\u044f\u0435\u0442 \u0443\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c \u043c\u0430\u043a\u0440\u043e\u0441\u0430\u043c\u0438, \u043a\u043e\u0442\u043e\u0440\u044b\u0435 \u0430\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0438 \u0432\u0432\u043e\u0434\u044f\u0442 \u0437\u0430\u0434\u0430\u043d\u043d\u044b\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u044b \u0432 \u0447\u0430\u0442.", "", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435:", "> macro add <key> <name> <message> - \u0414\u043e\u0431\u0430\u0432\u043b\u044f\u0435\u0442 \u043d\u043e\u0432\u044b\u0439 \u043c\u0430\u043a\u0440\u043e\u0441, \u043a\u043e\u0442\u043e\u0440\u044b\u0439 \u0431\u0443\u0434\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u0442\u044c\u0441\u044f \u043f\u0440\u0438 \u043d\u0430\u0436\u0430\u0442\u0438\u0438 \u043d\u0430 \u0443\u043a\u0430\u0437\u0430\u043d\u043d\u0443\u044e \u043a\u043b\u0430\u0432\u0438\u0448\u0443 \u0438 \u0432\u0432\u043e\u0434\u0438\u0442\u044c \u0443\u043a\u0430\u0437\u0430\u043d\u043d\u043e\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435.", "> macro remove <name> - \u0423\u0434\u0430\u043b\u044f\u0435\u0442 \u043c\u0430\u043a\u0440\u043e\u0441 \u0441 \u0443\u043a\u0430\u0437\u0430\u043d\u043d\u044b\u043c \u0438\u043c\u0435\u043d\u0435\u043c.", "> macro list - \u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0435\u0442 \u0441\u043f\u0438\u0441\u043e\u043a \u0432\u0441\u0435\u0445 \u0442\u0435\u043a\u0443\u0449\u0438\u0445 \u043c\u0430\u043a\u0440\u043e\u0441\u043e\u0432.", "> macro clear - \u0423\u0434\u0430\u043b\u044f\u0435\u0442 \u0432\u0441\u0435 \u043c\u0430\u043a\u0440\u043e\u0441\u044b \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430.");
    }
}
