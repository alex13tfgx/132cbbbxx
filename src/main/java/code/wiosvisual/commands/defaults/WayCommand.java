/*
 * .way add <name> [x y z] | .way remove <name> | .way gps <name> | .way list | .way clear
 * Сохранённые точки привязаны к серверу и рисуются в мире (WayRepository).
 */
package code.wiosvisual.commands.defaults;

import code.wiosvisual.Wiosvisual;
import code.wiosvisual.common.repository.way.Way;
import code.wiosvisual.common.repository.way.WayRepository;
import code.wiosvisual.utils.client.managers.api.command.Command;
import code.wiosvisual.utils.client.managers.api.command.IBaritoneChatControl;
import code.wiosvisual.utils.client.managers.api.command.argument.IArgConsumer;
import code.wiosvisual.utils.client.managers.api.command.datatypes.WayDataType;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.Paginator;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WayCommand
extends Command {
    private final WayRepository wayRepository;

    public WayCommand(Wiosvisual main) {
        super("way", "waypoint", "wp");
        this.wayRepository = main.getWayRepository();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add", "set" -> this.handleAdd(args);
            case "remove", "del", "delete" -> this.handleRemove(args);
            case "gps", "go" -> this.handleGps(args);
            case "list" -> this.handleList(args, label);
            case "clear" -> this.handleClear(args);
            default -> this.logDirect("Использование: .way add <имя> [x y z] | remove <имя> | gps <имя> | list | clear", Formatting.YELLOW);
        }
    }

    private void handleAdd(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();
        BlockPos pos;
        if (args.hasAny()) {
            args.requireExactly(3);
            double[] c = new double[3];
            for (int i = 0; i < 3; ++i) {
                String raw = args.getString();
                try {
                    c[i] = this.parseCoord(raw, i);
                } catch (NumberFormatException ex) {
                    this.logDirect("Не число: " + raw, Formatting.RED);
                    return;
                }
            }
            pos = BlockPos.ofFloored(c[0], c[1], c[2]);
        } else {
            if (mc.player == null) {
                this.logDirect("Нужно быть в мире, чтобы поставить точку на своей позиции.", Formatting.RED);
                return;
            }
            pos = mc.player.getBlockPos();
        }
        boolean existed = this.wayRepository.hasWay(name);
        this.wayRepository.upsertWay(name, pos, WayRepository.currentServer());
        this.logDirect(String.valueOf(Formatting.WHITE) + (existed ? "Точка " : "Добавлена точка ") + String.valueOf(Formatting.RED) + name + String.valueOf(Formatting.WHITE) + (existed ? " перенесена на " : ": ") + String.valueOf(Formatting.RED) + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    private double parseCoord(String raw, int index) {
        if (!raw.startsWith("~")) {
            return Double.parseDouble(raw);
        }
        double offset = raw.length() == 1 ? 0.0 : Double.parseDouble(raw.substring(1));
        if (mc.player == null) {
            return offset;
        }
        Vec3d p = new net.minecraft.util.math.Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        return (index == 0 ? p.x : index == 1 ? p.y : p.z) + offset;
    }

    private void handleRemove(IArgConsumer args) throws CommandException {
        args.requireExactly(1);
        String name = args.getString();
        if (!this.wayRepository.hasWay(name)) {
            this.logDirect("Точка с таким именем не найдена!", Formatting.RED);
            return;
        }
        this.wayRepository.deleteWay(name);
        this.logDirect(String.valueOf(Formatting.WHITE) + "Точка " + String.valueOf(Formatting.RED) + name + String.valueOf(Formatting.WHITE) + " удалена.");
    }

    private void handleGps(IArgConsumer args) throws CommandException {
        args.requireExactly(1);
        Way way = args.getDatatypeFor(WayDataType.INSTANCE);
        if (way == null) {
            this.logDirect("Точка с таким именем не найдена!", Formatting.RED);
            return;
        }
        GpsCommand gps = GpsCommand.getInstance();
        if (gps == null) {
            return;
        }
        gps.navigate(way.pos().toCenterPos());
        this.logDirect(String.valueOf(Formatting.WHITE) + "GPS ведёт к точке " + String.valueOf(Formatting.RED) + way.name());
    }

    private void handleList(IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);
        Paginator.paginate(args, new Paginator<Way>(this.wayRepository.wayList), () -> this.logDirect("Список точек:"), way -> Text.literal(String.valueOf(Formatting.GRAY) + "Название: " + String.valueOf(Formatting.WHITE) + way.name() + String.valueOf(Formatting.GRAY) + " Координаты: " + String.valueOf(Formatting.WHITE) + way.pos().getX() + " " + way.pos().getY() + " " + way.pos().getZ() + String.valueOf(Formatting.GRAY) + " Сервер: " + String.valueOf(Formatting.WHITE) + way.server()), IBaritoneChatControl.FORCE_COMMAND_PREFIX + label);
    }

    private void handleClear(IArgConsumer args) throws CommandException {
        args.requireMax(0);
        this.wayRepository.clearList();
        this.logDirect("Все точки удалены.", Formatting.WHITE);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().prepend("add", "remove", "gps", "list", "clear").filterPrefix(args.getString()).stream();
        }
        if (args.hasAny()) {
            String arg = args.getString();
            if ((arg.equalsIgnoreCase("remove") || arg.equalsIgnoreCase("gps")) && args.hasExactlyOne()) {
                return args.tabCompleteDatatype(WayDataType.INSTANCE);
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Сохранённые точки (waypoints)";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Сохраняет точки для текущего сервера и показывает их в мире с дистанцией.",
                "",
                "Использование:",
                "> way add <name> - поставить точку на своей позиции",
                "> way add <name> <x> <y> <z> - поставить точку по координатам (поддерживается ~)",
                "> way remove <name> - удалить точку",
                "> way gps <name> - включить GPS до точки",
                "> way list - список точек",
                "> way clear - удалить все точки");
    }
}
