/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 */
package code.wiosvisual.commands.defaults;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleProvider;
import code.wiosvisual.features.module.ModuleRepository;
import code.wiosvisual.utils.client.chat.StringHelper;
import code.wiosvisual.utils.client.managers.api.command.Command;
import code.wiosvisual.utils.client.managers.api.command.IBaritoneChatControl;
import code.wiosvisual.utils.client.managers.api.command.argument.IArgConsumer;
import code.wiosvisual.utils.client.managers.api.command.datatypes.KeyDataType;
import code.wiosvisual.utils.client.managers.api.command.datatypes.ModuleDataType;
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

public class BindCommand
extends Command {
    private final ModuleProvider moduleProvider;
    private final ModuleRepository moduleRepository;

    public BindCommand(Wiosvisual main) {
        super("bind");
        this.moduleRepository = main.getModuleRepository();
        this.moduleProvider = main.getModuleProvider();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action;
        switch (action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list") {
            case "add": {
                this.handleAddBind(args);
                break;
            }
            case "remove": {
                this.handleRemoveBind(args);
                break;
            }
            case "list": {
                this.handleListBinds(args, label);
                break;
            }
            case "clear": {
                this.handleClearBinds(args);
            }
        }
    }

    private void handleAddBind(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        String moduleName = args.getString();
        int key = (Integer)((Map.Entry)args.getDatatypeFor(KeyDataType.INSTANCE)).getValue();
        Object module = this.moduleProvider.get(moduleName);
        ((Module)module).setKey(key);
        this.logDirect(String.valueOf(Formatting.WHITE) + "\u041c\u043e\u0434\u0443\u043b\u044c " + String.valueOf(Formatting.RED) + moduleName + String.valueOf(Formatting.WHITE) + " \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u043d \u043a \u043a\u043d\u043e\u043f\u043a\u0435 " + String.valueOf(Formatting.RED) + StringHelper.getBindName(key).toLowerCase());
    }

    private void handleRemoveBind(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String moduleName = args.getString();
        Object module = this.moduleProvider.get(moduleName);
        ((Module)module).setKey(-1);
        this.logDirect(String.valueOf(Formatting.WHITE) + "\u0411\u0438\u043d\u0434 \u0434\u043b\u044f \u043c\u043e\u0434\u0443\u043b\u044f " + String.valueOf(Formatting.RED) + moduleName + String.valueOf(Formatting.WHITE) + " \u0431\u044b\u043b \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0443\u0434\u0430\u043b\u0435\u043d!");
    }

    private void handleListBinds(IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);
        List<Module> filtredList = this.moduleRepository.modules().stream().filter(module -> module.getKey() != -1 && !module.isHidden()).toList();
        Paginator.paginate(args, new Paginator<Module>(filtredList), () -> this.logDirect("\u0421\u043f\u0438\u0441\u043e\u043a \u043c\u043e\u0434\u0443\u043b\u0435\u0439:"), module -> {
            String names = module.getName();
            String keys = StringHelper.getBindName(module.getKey()).toLowerCase();
            return Text.literal((String)(String.valueOf(Formatting.GRAY) + "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435: " + String.valueOf(Formatting.WHITE) + names)).append((Text)Text.literal((String)(String.valueOf(Formatting.GRAY) + " \u041a\u043b\u0430\u0432\u0438\u0448\u0430: " + String.valueOf(Formatting.WHITE) + keys)));
        }, IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
    }

    private void handleClearBinds(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        this.moduleRepository.modules().forEach(function -> function.setKey(-1));
        this.logDirect("\u0412\u0441\u0435 \u0431\u0438\u043d\u0434\u044b \u043c\u043e\u0434\u0443\u043b\u0435\u0439 \u0431\u044b\u043b\u0438 \u0443\u0434\u0430\u043b\u0435\u043d\u044b.", Formatting.WHITE);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
        }
        String arg = args.getString();
        if (arg.equalsIgnoreCase("add")) {
            if (args.hasExactly(1)) {
                return args.tabCompleteDatatype(ModuleDataType.INSTANCE);
            }
            if (args.hasExactly(2)) {
                return args.tabCompleteDatatype(KeyDataType.INSTANCE);
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "\u041f\u043e\u0437\u0432\u043e\u043b\u044f\u0435\u0442 \u0443\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c \u0431\u0438\u043d\u0434\u0430\u043c\u0438 \u043c\u043e\u0434\u0443\u043b\u0435\u0439";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("\u042d\u0442\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u0430 \u043f\u043e\u0437\u0432\u043e\u043b\u044f\u0435\u0442 \u0443\u043f\u0440\u0430\u0432\u043b\u044f\u0442\u044c \u0431\u0438\u043d\u0434\u0430\u043c\u0438 \u0434\u043b\u044f \u043c\u043e\u0434\u0443\u043b\u0435\u0439, \u043a\u043e\u0442\u043e\u0440\u044b\u0435 \u0431\u0443\u0434\u0443\u0442 \u0430\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u0442\u044c\u0441\u044f \u043f\u0440\u0438 \u043d\u0430\u0436\u0430\u0442\u0438\u0438 \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0451\u043d\u043d\u044b\u0445 \u043a\u043b\u0430\u0432\u0438\u0448.", "", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435:", "> bind add <module> <key> - \u041f\u0440\u0438\u0432\u044f\u0437\u044b\u0432\u0430\u0435\u0442 \u043c\u043e\u0434\u0443\u043b\u044c \u043a \u0443\u043a\u0430\u0437\u0430\u043d\u043d\u043e\u0439 \u043a\u043b\u0430\u0432\u0438\u0448\u0435.", "> bind remove <module> - \u0423\u0434\u0430\u043b\u044f\u0435\u0442 \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u0443 \u043c\u043e\u0434\u0443\u043b\u044f.", "> bind list - \u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0441\u043f\u0438\u0441\u043e\u043a \u0432\u0441\u0435\u0445 \u0442\u0435\u043a\u0443\u0449\u0438\u0445 \u0431\u0438\u043d\u0434\u043e\u0432 \u043c\u043e\u0434\u0443\u043b\u0435\u0439.", "> bind clear - \u0423\u0434\u0430\u043b\u044f\u0435\u0442 \u0432\u0441\u0435 \u0431\u0438\u043d\u0434\u044b \u043c\u043e\u0434\u0443\u043b\u0435\u0439.");
    }
}
