/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_2583
 *  net.minecraft.class_303
 *  net.minecraft.class_310
 *  net.minecraft.class_338
 *  net.minecraft.class_5250
 *  net.minecraft.class_7591
 */
package code.wiosvisual.utils.client.chat;

import code.wiosvisual.mixins.acc.InsertChatMixin;
import code.wiosvisual.utils.client.text.TextHelper;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatMessage {
    /**
     * Префикс сообщений клиента. Цвета градиента темы — лишь запасной вариант: при отрисовке
     * чата ChatBrandShimmer по метке во вставке перекрашивает эти символы каждый кадр,
     * поэтому префикс переливается, как ватермарка, и следует смене темы даже в старых сообщениях.
     */
    private static MutableText prefix() {
        Text gradient = TextHelper.applyGradient(ChatBrandShimmer.BRAND_MARKER, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.getHudColor(), ColorAssist.getHudColor2(), true);
        return Text.literal("").append(gradient).styled(style -> style.withInsertion(ChatBrandShimmer.BRAND_MARKER));
    }

    public static MutableText brandmessage() {
        MutableText Wiosvisual = ChatMessage.prefix();
        MutableText arrow = Text.literal((String)" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
        return Wiosvisual.append((Text)arrow);
    }

    public static void send(Text message) {
        if (QuickImports.mc.inGameHud == null || QuickImports.mc.inGameHud.getChatHud() == null) {
            return;
        }
        Text prefix = ChatMessage.prefix();
        MutableText arrow = Text.literal((String)" >").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
        MutableText fullMessage = prefix.copy().append((Text)arrow).append(" ").append(message);
        int tick = QuickImports.mc.inGameHud.getTicks();
        ChatHudLine line = new ChatHudLine(tick, (Text)fullMessage, null, MessageIndicator.system());
        ChatHud chatHud = QuickImports.mc.inGameHud.getChatHud();
        ((InsertChatMixin)chatHud).invokeAddMessage(line);
        ((InsertChatMixin)chatHud).invokeAddVisibleMessage(line);
    }

    public static void brandmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MutableText Wiosvisual = ChatMessage.prefix();
            MutableText arrow = Text.literal((String)" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
            MutableText formattedMessage = Wiosvisual.append((Text)arrow).append((Text)Text.literal((String)message));
            MinecraftClient.getInstance().player.sendMessage((Text)formattedMessage, false);
        }
    }

    public static void brandmessage(Text message) {
        if (MinecraftClient.getInstance().player != null) {
            MutableText Wiosvisual = ChatMessage.prefix();
            MutableText arrow = Text.literal((String)" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
            MutableText formattedMessage = Wiosvisual.append((Text)arrow).append(message);
            MinecraftClient.getInstance().player.sendMessage((Text)formattedMessage, false);
        }
    }
}
