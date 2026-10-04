/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_746
 */
package code.wiosvisual.utils.client.chat;

import net.minecraft.client.network.ClientPlayerEntity;

public final class ChatCommandSender {
    private ChatCommandSender() {
    }

    public static void sendCommand(ClientPlayerEntity player, String command) {
        if (player == null || player.networkHandler == null || command == null) {
            return;
        }
        if (command.startsWith("/")) {
            player.networkHandler.sendChatCommand(command.substring(1));
        } else {
            player.networkHandler.sendChatMessage(command);
        }
    }
}
