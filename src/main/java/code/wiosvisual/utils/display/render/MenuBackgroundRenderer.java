/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1921
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 */
package code.wiosvisual.utils.display.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class MenuBackgroundRenderer {
    public static final Identifier MENU_BACKGROUND_TEXTURE = Identifier.of((String)"minecraft", (String)"textures/mainmenu/background.png");

    private MenuBackgroundRenderer() {
    }

    public static boolean hasBackgroundTexture() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getResourceManager().getResource(MENU_BACKGROUND_TEXTURE).isPresent();
    }

    public static boolean renderFullscreen(DrawContext context, int width, int height) {
        if (!MenuBackgroundRenderer.hasBackgroundTexture()) {
            return false;
        }
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, MENU_BACKGROUND_TEXTURE, 0, 0, 0.0f, 0.0f, width, height, width, height);
        return true;
    }
}
