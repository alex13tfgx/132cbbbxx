/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap$Entry
 *  net.minecraft.class_3675$class_307
 */
package code.wiosvisual.utils.client.managers.api.command.datatypes;

import code.wiosvisual.utils.client.chat.StringHelper;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeContext;
import code.wiosvisual.utils.client.managers.api.command.datatypes.IDatatypeFor;
import code.wiosvisual.utils.client.managers.api.command.exception.CommandException;
import code.wiosvisual.utils.client.managers.api.command.helpers.TabCompleteHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.util.InputUtil;

public enum KeyDataType implements IDatatypeFor<Map.Entry<String, Integer>>
{
    INSTANCE;


    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> keys = KeyDataType.getKeys().keySet().stream();
        String context = datatypeContext.getConsumer().getString();
        return new TabCompleteHelper().append(keys).filterPrefix(context).sortAlphabetically().stream();
    }

    @Override
    public Map.Entry<String, Integer> get(IDatatypeContext datatypeContext) throws CommandException {
        String key = datatypeContext.getConsumer().getString();
        return KeyDataType.getKeys().entrySet().stream().filter(s -> ((String)s.getKey()).equalsIgnoreCase(key)).findFirst().orElse(null);
    }

    private static Map<String, Integer> getKeys() {
        HashMap<String, Integer> keys = new HashMap<String, Integer>();
        for (Int2ObjectMap.Entry entry : InputUtil.Type.KEYSYM.map.int2ObjectEntrySet()) {
            int keyCode = entry.getIntKey();
            String bindName = StringHelper.getBindName(keyCode).toLowerCase();
            keys.put(bindName, keyCode);
        }
        return keys;
    }
}
