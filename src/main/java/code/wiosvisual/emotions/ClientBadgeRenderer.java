/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 */
package code.wiosvisual.emotions;

import code.wiosvisual.emotions.EmotionWheelManager;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.render.geometry.Render2D;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public final class ClientBadgeRenderer {
    private static final int CLIENT_ICON_TEXTURE_SIZE = 2100;
    private static Identifier clientIcon;

    private ClientBadgeRenderer() {
    }

    private static Identifier getClientIcon() {
        if (clientIcon == null) {
            clientIcon = Identifier.of((String)"mre", (String)"wiosvisual_icon.png");
        }
        return clientIcon;
    }

    public static boolean hasCompatibleClient(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (uuid != null && mc.player != null && uuid.equals(mc.player.getUuid())) {
            return true;
        }
        EmotionWheelManager manager = EmotionWheelManager.getInstance();
        return manager != null && manager.hasCompatibleClient(uuid);
    }

    public static boolean hasCompatibleClient(PlayerEntity player) {
        return player != null && ClientBadgeRenderer.hasCompatibleClient(player.getUuid());
    }

    public static void drawRoundedBadge(DrawContext context, float x, float y, float size) {
        try {
            Render2D.drawTexture(context, ClientBadgeRenderer.getClientIcon(), x, y, size, size / 2.0f, 2100, 2100, 2100, ColorAssist.rgba(255, 255, 255, 255), ColorAssist.rgba(255, 255, 255, 255));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}
