/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_5250
 */
package code.wiosvisual.common.logger.implement;

import code.wiosvisual.common.logger.Logger;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import java.util.Arrays;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class MinecraftLogger
implements Logger,
QuickImports {
    @Override
    public void log(Object message) {
    }

    @Override
    public void minecraftLog(Text ... components) {
        if (MinecraftLogger.mc.player != null) {
            MutableText component = Text.literal((String)"");
            Arrays.asList(components).forEach(arg_0 -> ((MutableText)component).append(arg_0));
            MinecraftLogger.mc.inGameHud.getChatHud().addMessage((Text)component);
        }
    }
}
