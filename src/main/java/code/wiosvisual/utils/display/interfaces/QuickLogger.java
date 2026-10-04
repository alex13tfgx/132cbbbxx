/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_5250
 */
package code.wiosvisual.utils.display.interfaces;

import code.wiosvisual.utils.client.chat.ChatMessage;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public interface QuickLogger {
    public static Text getPrefix() {
        MutableText text = ChatMessage.brandmessage();
        text.setStyle(text.getStyle().withColor(Formatting.GRAY));
        return text;
    }

    default public void logDirect(Text ... components) {
        MutableText component = Text.literal((String)"");
        component.append(QuickLogger.getPrefix());
        component.append((Text)Text.literal((String)" "));
        Arrays.asList(components).forEach(arg_0 -> ((MutableText)component).append(arg_0));
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage((Text)component);
        }
    }

    default public void logDirect(String message, Formatting color) {
        Stream.of(message.split("\n")).forEach(line -> {
            MutableText component = Text.literal((String)line.replace("\t", "    "));
            component.setStyle(component.getStyle().withColor(color));
            this.logDirect(new Text[]{component});
        });
    }

    default public void logDirect(String message) {
        this.logDirect(message, Formatting.WHITE);
    }
}
