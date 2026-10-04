/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package code.wiosvisual.common.logger.implement;

import code.wiosvisual.common.logger.Logger;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;

public class ConsoleLogger
implements Logger {
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger((String)"Wiosvisual");

    @Override
    public void log(Object message) {
        this.logger.info("[Wiosvisual] {}", message);
    }

    @Override
    public void minecraftLog(Text ... components) {
    }
}
