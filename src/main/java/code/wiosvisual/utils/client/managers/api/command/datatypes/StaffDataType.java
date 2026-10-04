/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.client.managers.api.command.datatypes;

import code.wiosvisual.common.repository.staff.StaffRepository;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeContext;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypePost;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import java.util.stream.Stream;

public enum StaffDataType implements IDatatypePost<String, Void>
{
    INSTANCE;


    @Override
    public String apply(IDatatypeContext context, Void original) throws CommandException {
        return context.getConsumer().getString();
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext context) throws CommandException {
        TabCompleteHelper helper = new TabCompleteHelper();
        StaffRepository.getStaff().forEach(staff -> helper.append(staff.getName()));
        String prefix = context.getConsumer().hasAny() ? context.getConsumer().peekString() : "";
        return helper.filterPrefix(prefix).stream();
    }
}
